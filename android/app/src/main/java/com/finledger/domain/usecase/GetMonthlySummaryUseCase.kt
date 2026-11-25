package com.finledger.domain.usecase

import com.finledger.data.local.entity.TransactionStatus
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.domain.model.MonthlySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

/**
 * Computes monthly income, personal expense, and savings using FINANCIAL_RULES.md:
 * - Only EXPENSE.personalAmount counts as personal spending (shared expense user share only).
 * - Friend-paid shared expenses (userPaid == 0) include the user's non-excluded split share.
 * - REFUND reduces personal spending.
 * - TRANSFER, LENDING, BORROWING, REPAYMENT_RECEIVED, REPAYMENT_MADE do not count as income/expense.
 */
class GetMonthlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
) {
    operator fun invoke(year: Int? = null, monthZeroBased: Int? = null): Flow<MonthlySummary> {
        val (startMs, endMs) = monthBounds(year, monthZeroBased)
        return combine(
            transactionRepository.observeAllTransactionsWithDetails(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllSplits(),
        ) { list, sharedExpenses, splits ->
            var income = 0L
            var expense = 0L

            for (item in list) {
                val tx = item.transaction
                if (tx.isDeleted || tx.status == TransactionStatus.CANCELLED) continue
                if (tx.date !in startMs..endMs) continue

                when (tx.type) {
                    TransactionType.INCOME -> income += tx.amountInPrimary
                    TransactionType.EXPENSE -> expense += tx.personalAmount
                    TransactionType.REFUND -> expense -= tx.personalAmount
                    else -> Unit
                }
            }

            val friendPaidShares = FriendPaidSharedExpenseHelper.resolveFriendPaidShares(list, sharedExpenses, splits)
            for (entry in friendPaidShares) {
                if (entry.expense.date in startMs..endMs) {
                    expense += entry.userSharePaise
                }
            }

            val normalizedExpense = expense.coerceAtLeast(0L)
            val savings = income - normalizedExpense
            val rate = if (income > 0) ((savings * 100) / income).toInt() else 0

            MonthlySummary(
                monthStartMs = startMs,
                monthEndMs = endMs,
                totalIncome = income,
                totalPersonalExpense = normalizedExpense,
                savings = savings,
                savingsRatePercent = rate,
            )
        }
    }

    companion object {
        fun monthBounds(year: Int? = null, monthZeroBased: Int? = null): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            if (year != null) cal.set(Calendar.YEAR, year)
            if (monthZeroBased != null) cal.set(Calendar.MONTH, monthZeroBased)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            return start to end
        }
    }
}

