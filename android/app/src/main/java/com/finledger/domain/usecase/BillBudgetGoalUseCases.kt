package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
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

class MarkBillPaidUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        bill: BillEntity,
        actualAmountPaise: Long,
        accountId: Long,
        paidDateMs: Long,
    ): Result<Long> {
        if (actualAmountPaise <= 0L) return Result.failure(IllegalArgumentException("Amount must be positive"))
        return runCatching {
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
                    dueDate = bill.nextDueDateMs,
                    paidDate = paidDateMs,
                    amount = actualAmountPaise,
                    isPaid = true,
                    transactionId = txId,
                )
            )
            // Advance next due date anchored to original dueDay (prevents month-end drift after Feb)
            val nextDue = advanceDueDate(bill.nextDueDateMs, bill.frequency, bill.dueDay)
            commitmentRepository.saveBill(bill.copy(nextDueDateMs = nextDue))
            paymentId
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

