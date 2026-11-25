package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.TransactionRepository
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
            // Advance next due date according to frequency
            val nextDue = advanceDueDate(bill.nextDueDateMs, bill.frequency)
            commitmentRepository.saveBill(bill.copy(nextDueDateMs = nextDue))
            paymentId
        }
    }

    private fun advanceDueDate(currentMs: Long, frequency: BillFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMs }
        when (frequency) {
            BillFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BillFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            BillFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
            BillFrequency.CUSTOM -> cal.add(Calendar.MONTH, 1)
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
) {
    operator fun invoke(): Flow<List<BudgetProgress>> {
        return combine(
            commitmentRepository.observeActiveBudgets(),
            transactionRepository.observeAllTransactionsWithDetails(),
        ) { budgets, transactions ->
            budgets.map { budget ->
                val (startMs, endMs) = periodBounds(budget)
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
                val normSpent = spent.coerceAtLeast(0L)
                val remaining = budget.amount - normSpent
                val pct = if (budget.amount > 0) ((normSpent * 100) / budget.amount).toInt() else 0
                BudgetProgress(
                    budget = budget,
                    spentPaise = normSpent,
                    remainingPaise = remaining,
                    percentUsed = pct,
                    isAlertTriggered = pct >= budget.alertThreshold,
                    isExceeded = normSpent > budget.amount,
                )
            }
        }
    }

    private fun periodBounds(budget: BudgetEntity): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (budget.period) {
            BudgetPeriod.MONTHLY -> GetMonthlySummaryUseCase.monthBounds()
            BudgetPeriod.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
            BudgetPeriod.CUSTOM -> budget.startDate to (budget.endDate ?: Long.MAX_VALUE)
        }
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

