package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

data class BudgetProgress(
    val budget: BudgetEntity,
    val spentPaise: Long,
    val remainingPaise: Long,
    val percentUsed: Int,
    val isAlertTriggered: Boolean,
    val isExceeded: Boolean,
)

class CreateBillUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
) {
    suspend operator fun invoke(bill: BillEntity): Result<Long> {
        if (bill.name.isBlank()) return Result.failure(IllegalArgumentException("Bill name cannot be empty"))
        if (bill.expectedAmount <= 0L) return Result.failure(IllegalArgumentException("Amount must be positive"))
        return runCatching { commitmentRepository.saveBill(bill.copy(name = bill.name.trim())) }
    }
}

data class BillPaymentReceipt(
    val billId: Long,
    val paymentId: Long,
    val transactionId: Long,
    val previousDueDateMs: Long,
)

class MarkBillPaidUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        bill: BillEntity,
        actualAmountPaise: Long,
        accountId: Long,
        paidDateMs: Long,
    ): Result<Long> = markPaidWithReceipt(bill, actualAmountPaise, accountId, paidDateMs).map { it.paymentId }

    suspend fun markPaidWithReceipt(
        bill: BillEntity,
        actualAmountPaise: Long,
        accountId: Long,
        paidDateMs: Long,
    ): Result<BillPaymentReceipt> {
        if (actualAmountPaise <= 0L) return Result.failure(IllegalArgumentException("Amount must be positive"))
        return runCatching {
            val previousDue = bill.nextDueDateMs
            val txId = transactionRepository.insert(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = actualAmountPaise,
                    personalAmount = actualAmountPaise,
                    amountInPrimary = actualAmountPaise,
                    date = paidDateMs,
                    accountId = accountId,
                    categoryId = bill.categoryId,
                    merchantOrPayee = bill.provider.ifBlank { bill.name },
                    description = "Bill payment: ${bill.name}",
                    isRecurring = true,
                    recurringId = bill.id,
                )
            )
            val paymentId = commitmentRepository.insertBillPayment(
                BillPaymentEntity(
                    billId = bill.id,
                    dueDate = previousDue,
                    paidDate = paidDateMs,
                    amount = actualAmountPaise,
                    isPaid = true,
                    transactionId = txId,
                )
            )
            // Advance next due date anchored to original dueDay (prevents month-end drift after Feb)
            val nextDue = advanceDueDate(previousDue, bill.frequency, bill.dueDay)
            commitmentRepository.saveBill(bill.copy(nextDueDateMs = nextDue))
            BillPaymentReceipt(
                billId = bill.id,
                paymentId = paymentId,
                transactionId = txId,
                previousDueDateMs = previousDue,
            )
        }
    }

    suspend fun undoBillPayment(receipt: BillPaymentReceipt): Result<Unit> = runCatching {
        commitmentRepository.softDeleteBillPayment(receipt.paymentId)
        transactionRepository.softDelete(receipt.transactionId)
        commitmentRepository.getBillById(receipt.billId)?.let { currentBill ->
            commitmentRepository.saveBill(currentBill.copy(nextDueDateMs = receipt.previousDueDateMs))
        }
    }

    private fun advanceDueDate(currentMs: Long, frequency: BillFrequency, dueDay: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMs }
        when (frequency) {
            BillFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BillFrequency.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            BillFrequency.QUARTERLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 3)
                cal.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            BillFrequency.YEARLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.YEAR, 1)
                cal.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            BillFrequency.CUSTOM -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
        }
        return cal.timeInMillis
    }
}

class CreateBudgetUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
) {
    suspend operator fun invoke(budget: BudgetEntity): Result<Long> {
        if (budget.name.isBlank()) return Result.failure(IllegalArgumentException("Budget name cannot be empty"))
        if (budget.amount <= 0L) return Result.failure(IllegalArgumentException("Budget limit must be positive"))
        return runCatching { commitmentRepository.saveBudget(budget.copy(name = budget.name.trim())) }
    }
}

class GetBudgetProgressUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<List<BudgetProgress>> {
        return combine(
            commitmentRepository.observeActiveBudgets(),
            transactionRepository.observeAllTransactionsWithDetails(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllSplits(),
            userPreferencesRepository.preferences,
        ) { budgets, transactions, sharedExpenses, splits, prefs ->
            val friendPaidShares = FriendPaidSharedExpenseHelper.resolveFriendPaidShares(
                transactions,
                sharedExpenses,
                splits,
            )

            fun computeSpent(budget: BudgetEntity, startMs: Long, endMs: Long): Long {
                var spent = 0L
                for (item in transactions) {
                    val tx = item.transaction
                    if (tx.isDeleted || tx.status == TransactionStatus.CANCELLED) continue
                    if (tx.date !in startMs..endMs) continue
                    if (budget.categoryId != null && tx.categoryId != budget.categoryId) continue
                    if (budget.accountId != null && tx.accountId != budget.accountId) continue
                    if (budget.groupId != null && tx.groupId != budget.groupId) continue
                    if (budget.eventId != null && tx.eventId != budget.eventId) continue

                    if (tx.type == TransactionType.EXPENSE) spent += tx.personalAmount
                    if (tx.type == TransactionType.REFUND) spent -= tx.personalAmount
                }
                if (budget.accountId == null) {
                    for (entry in friendPaidShares) {
                        val exp = entry.expense
                        if (exp.date !in startMs..endMs) continue
                        if (budget.categoryId != null && exp.categoryId != budget.categoryId) continue
                        if (budget.groupId != null && exp.groupId != budget.groupId) continue
                        if (budget.eventId != null && exp.eventId != budget.eventId) continue
                        spent += entry.userSharePaise
                    }
                }
                return spent.coerceAtLeast(0L)
            }

            budgets.map { budget ->
                val (startMs, endMs) = periodBounds(budget, prefs.financialYearStartMonth)
                val normSpent = computeSpent(budget, startMs, endMs)
                val carryOver = if (budget.isRollover) {
                    previousPeriodBounds(budget, prefs.financialYearStartMonth)?.let { (prevS, prevE) ->
                        (budget.amount - computeSpent(budget, prevS, prevE)).coerceAtLeast(0L)
                    } ?: 0L
                } else 0L
                val effectiveLimit = (budget.amount + carryOver).coerceAtLeast(1L)
                val remaining = effectiveLimit - normSpent
                val pct = ((normSpent * 100) / effectiveLimit).toInt()
                BudgetProgress(
                    budget = budget.copy(amount = effectiveLimit),
                    spentPaise = normSpent,
                    remainingPaise = remaining,
                    percentUsed = pct,
                    isAlertTriggered = pct >= budget.alertThreshold,
                    isExceeded = normSpent > effectiveLimit,
                )
            }
        }
    }

    private fun periodBounds(budget: BudgetEntity, fyStartMonth: Int): Pair<Long, Long> {
        return when (budget.period) {
            BudgetPeriod.MONTHLY -> GetMonthlySummaryUseCase.monthBounds()
            BudgetPeriod.YEARLY -> financialYearBounds(fyStartMonth, yearOffset = 0)
            BudgetPeriod.CUSTOM -> budget.startDate to (budget.endDate ?: Long.MAX_VALUE)
        }
    }

    private fun previousPeriodBounds(budget: BudgetEntity, fyStartMonth: Int): Pair<Long, Long>? {
        return when (budget.period) {
            BudgetPeriod.MONTHLY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -1)
                }
                GetMonthlySummaryUseCase.monthBounds(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            }
            BudgetPeriod.YEARLY -> financialYearBounds(fyStartMonth, yearOffset = -1)
            BudgetPeriod.CUSTOM -> null
        }
    }

    private fun financialYearBounds(fyStartMonthOneBased: Int, yearOffset: Int): Pair<Long, Long> {
        val startMonthZero = (fyStartMonthOneBased.coerceIn(1, 12)) - 1
        val now = Calendar.getInstance()
        val currentMonthZero = now.get(Calendar.MONTH)
        val fyStartYear = (if (currentMonthZero >= startMonthZero) now.get(Calendar.YEAR) else now.get(Calendar.YEAR) - 1) + yearOffset

        val startCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, fyStartYear)
            set(Calendar.MONTH, startMonthZero)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 11)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }
}

class CreateSavingsGoalUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
) {
    suspend operator fun invoke(goal: SavingsGoalEntity): Result<Long> {
        if (goal.name.isBlank()) return Result.failure(IllegalArgumentException("Goal name cannot be empty"))
        if (goal.targetAmount <= 0L) return Result.failure(IllegalArgumentException("Target amount must be positive"))
        val updatedStatus = if (goal.currentAmount >= goal.targetAmount) GoalStatus.COMPLETED else GoalStatus.IN_PROGRESS
        return runCatching {
            commitmentRepository.saveSavingsGoal(goal.copy(name = goal.name.trim(), status = updatedStatus))
        }
    }

    suspend fun contribute(goalId: Long, deltaPaise: Long): Result<Unit> {
        val existing = commitmentRepository.getSavingsGoalById(goalId)
            ?: return Result.failure(IllegalStateException("Goal not found"))
        val next = (existing.currentAmount + deltaPaise).coerceAtLeast(0L)
        val status = if (next >= existing.targetAmount) GoalStatus.COMPLETED else GoalStatus.IN_PROGRESS
        return runCatching {
            commitmentRepository.saveSavingsGoal(existing.copy(currentAmount = next, status = status))
            Unit
        }
    }
}

data class SuggestedBudget(
    val categoryId: Long,
    val categoryName: String,
    val suggestedLimitPaise: Long,
    val averageMonthlySpendPaise: Long,
)

class SuggestBudgetsFromHistoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val commitmentRepository: CommitmentRepository,
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
) {
    operator fun invoke(): Flow<List<SuggestedBudget>> = combine(
        categoryRepository.observeByType(CategoryType.EXPENSE),
        commitmentRepository.observeActiveBudgets(),
        transactionRepository.observeAllTransactionsWithDetails(),
        socialRepository.observeAllSharedExpenses(),
        socialRepository.observeAllSplits(),
    ) { categories, budgets, transactions, sharedExpenses, splits ->
        computeSuggestions(
            categories = categories,
            existingBudgets = budgets,
            transactions = transactions,
            sharedExpenses = sharedExpenses,
            splits = splits,
            nowMs = System.currentTimeMillis(),
        )
    }

    companion object {
        private const val MONTHS_LOOKBACK = 3
        private const val MIN_MONTHLY_PAISE = 100L // < 1 major currency unit

        fun threeMonthWindowBounds(nowMs: Long = System.currentTimeMillis()): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(Calendar.MONTH, -MONTHS_LOOKBACK)
            }
            return startCal.timeInMillis to nowMs
        }

        fun roundSuggestedLimit(averageMonthlyPaise: Long): Long {
            val step = when {
                averageMonthlyPaise >= 1_000_000L -> 50_000L // >= 10,000 units -> nearest 500
                averageMonthlyPaise >= 100_000L -> 10_000L // >= 1,000 units -> nearest 100
                averageMonthlyPaise >= 10_000L -> 5_000L // >= 100 units -> nearest 50
                averageMonthlyPaise >= 1_000L -> 1_000L // >= 10 units -> nearest 10
                else -> 100L // nearest 1 unit
            }
            return (((averageMonthlyPaise + (step / 2L)) / step) * step).coerceAtLeast(100L)
        }

        fun computeSuggestions(
            categories: List<CategoryEntity>,
            existingBudgets: List<BudgetEntity>,
            transactions: List<TransactionWithDetails>,
            sharedExpenses: List<SharedExpenseEntity>,
            splits: List<ExpenseSplitEntity>,
            nowMs: Long = System.currentTimeMillis(),
        ): List<SuggestedBudget> {
            val (startMs, endMs) = threeMonthWindowBounds(nowMs)
            val activeExpenseCats = categories.associateBy { it.id }
            val budgetedCategoryIds = existingBudgets
                .filter { !it.isDeleted && it.isActive && it.period == BudgetPeriod.MONTHLY && it.categoryId != null }
                .mapNotNull { it.categoryId }
                .toSet()

            val totalsByCategory = mutableMapOf<Long, Long>()
            for (item in transactions) {
                val tx = item.transaction
                if (tx.isDeleted || tx.status == TransactionStatus.CANCELLED) continue
                if (tx.date !in startMs..endMs) continue
                val catId = tx.categoryId ?: continue
                if (catId !in activeExpenseCats) continue

                when (tx.type) {
                    TransactionType.EXPENSE -> {
                        totalsByCategory[catId] = (totalsByCategory[catId] ?: 0L) + tx.personalAmount
                    }
                    TransactionType.REFUND -> {
                        totalsByCategory[catId] = (totalsByCategory[catId] ?: 0L) - tx.personalAmount
                    }
                    else -> Unit
                }
            }

            val friendPaidShares = FriendPaidSharedExpenseHelper.resolveFriendPaidShares(
                transactions,
                sharedExpenses,
                splits,
            )
            for (entry in friendPaidShares) {
                val exp = entry.expense
                if (exp.date !in startMs..endMs) continue
                val catId = exp.categoryId ?: continue
                if (catId !in activeExpenseCats) continue
                totalsByCategory[catId] = (totalsByCategory[catId] ?: 0L) + entry.userSharePaise
            }

            return totalsByCategory.mapNotNull { (catId, rawTotal) ->
                if (catId in budgetedCategoryIds) return@mapNotNull null
                val normalizedTotal = rawTotal.coerceAtLeast(0L)
                val avgMonthly = normalizedTotal / MONTHS_LOOKBACK
                if (avgMonthly < MIN_MONTHLY_PAISE) return@mapNotNull null
                val cat = activeExpenseCats[catId] ?: return@mapNotNull null
                SuggestedBudget(
                    categoryId = catId,
                    categoryName = cat.name,
                    suggestedLimitPaise = roundSuggestedLimit(avgMonthly),
                    averageMonthlySpendPaise = avgMonthly,
                )
            }.sortedWith(
                compareByDescending<SuggestedBudget> { it.suggestedLimitPaise }
                    .thenBy { it.categoryName.lowercase() },
            )
        }
    }
}

