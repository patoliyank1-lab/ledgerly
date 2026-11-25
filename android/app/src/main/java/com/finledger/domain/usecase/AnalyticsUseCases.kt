package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.InsightRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.util.DateFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

data class NetWorthBreakdown(
    val cashAndBankAssets: Long,
    val investmentAssets: Long,
    val receivablesAssets: Long,
    val totalAssets: Long,
    val creditCardLiabilities: Long,
    val borrowedLoanLiabilities: Long,
    val payablesLiabilities: Long,
    val totalLiabilities: Long,
    val netWorth: Long,
)

data class MonthlyCashFlowPoint(
    val label: String,
    val incomePaise: Long,
    val expensePaise: Long,
    val netSavingsPaise: Long,
)

data class CategorySpendingItem(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val totalPaise: Long,
    val percent: Double,
    val transactionCount: Int,
)

data class PeriodComparison(
    val currentIncome: Long,
    val previousIncome: Long,
    val currentExpense: Long,
    val previousExpense: Long,
)

data class AgingBucketPerson(
    val person: PersonEntity,
    val outstandingPaise: Long,
    val daysOutstanding: Int,
    val bucketLabel: String, // "0-30 days", "31-60 days", "60+ days"
)

class GetNetWorthUseCase @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getPortfolioValueUseCase: GetPortfolioValueUseCase,
    private val computePersonBalanceUseCase: ComputePersonBalanceUseCase,
    private val getLoanBalanceUseCase: GetLoanBalanceUseCase,
    private val insightRepository: InsightRepository,
) {
    operator fun invoke(): Flow<NetWorthBreakdown> {
        return combine(
            getAccountsUseCase.observeSummary(),
            getPortfolioValueUseCase(),
            computePersonBalanceUseCase(),
            getLoanBalanceUseCase(),
        ) { accSummary, portfolio, personBalances, loans ->
            val cashAndBank = accSummary.totalAvailableCashAndBank
            val investments = accSummary.investmentAccountBalance + portfolio.totalCurrentValuePaise

            // Consolidated person receivables / payables (already includes person-linked loans)
            val personReceivables = personBalances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise }
            val personPayables = personBalances.filter { it.netBalancePaise < 0 }.sumOf { -it.netBalancePaise }

            // Non-person institutional loans (personId == null)
            val orgReceivables = loans.filter { it.loan.personId == null && it.loan.type == LoanType.LENT }
                .sumOf { it.outstandingPrincipalPaise }
            val orgBorrowed = loans.filter { it.loan.personId == null && it.loan.type == LoanType.BORROWED }
                .sumOf { it.outstandingPrincipalPaise }

            val receivables = personReceivables + orgReceivables
            val totalAssets = cashAndBank + investments + receivables
            val totalLiabilities = accSummary.creditCardOutstanding + orgBorrowed + personPayables

            NetWorthBreakdown(
                cashAndBankAssets = cashAndBank,
                investmentAssets = investments,
                receivablesAssets = receivables,
                totalAssets = totalAssets,
                creditCardLiabilities = accSummary.creditCardOutstanding,
                borrowedLoanLiabilities = orgBorrowed,
                payablesLiabilities = personPayables,
                totalLiabilities = totalLiabilities,
                netWorth = totalAssets - totalLiabilities,
            )
        }
    }

    fun observeSnapshots(): Flow<List<NetWorthSnapshotEntity>> = insightRepository.observeNetWorthSnapshots()

    suspend fun recordSnapshot(breakdown: NetWorthBreakdown) {
        insightRepository.saveSnapshot(
            NetWorthSnapshotEntity(
                snapshotDateMs = System.currentTimeMillis(),
                totalAssetsPaise = breakdown.totalAssets,
                totalLiabilitiesPaise = breakdown.totalLiabilities,
                netWorthPaise = breakdown.netWorth,
            )
        )
    }
}

class GetCashFlowUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
) {
    operator fun invoke(monthsBack: Int = 6): Flow<List<MonthlyCashFlowPoint>> {
        return combine(
            transactionRepository.observeAllTransactionsWithDetails(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllSplits(),
        ) { list, sharedExpenses, splits ->
            val friendPaidShares = FriendPaidSharedExpenseHelper.resolveFriendPaidShares(list, sharedExpenses, splits)
            val cal = Calendar.getInstance()
            val points = mutableListOf<MonthlyCashFlowPoint>()
            for (offset in (monthsBack - 1) downTo 0) {
                val monthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
                val (start, end) = GetMonthlySummaryUseCase.monthBounds(
                    monthCal.get(Calendar.YEAR),
                    monthCal.get(Calendar.MONTH),
                )
                var inc = 0L
                var exp = 0L
                for (item in list) {
                    val tx = item.transaction
                    if (tx.isDeleted || tx.status == TransactionStatus.CANCELLED || tx.date !in start..end) continue
                    when (tx.type) {
                        TransactionType.INCOME -> inc += tx.amountInPrimary
                        TransactionType.EXPENSE -> exp += tx.personalAmount
                        TransactionType.REFUND -> exp -= tx.personalAmount
                        else -> Unit
                    }
                }
                for (entry in friendPaidShares) {
                    if (entry.expense.date in start..end) {
                        exp += entry.userSharePaise
                    }
                }
                val normExp = exp.coerceAtLeast(0L)
                points.add(
                    MonthlyCashFlowPoint(
                        label = DateFormatter.formatMonthYear(start).take(3),
                        incomePaise = inc,
                        expensePaise = normExp,
                        netSavingsPaise = inc - normExp,
                    )
                )
            }
            points
        }
    }
}

class GetCategoryBreakdownUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(startMs: Long, endMs: Long): Flow<List<CategorySpendingItem>> {
        return combine(
            transactionRepository.observeAllTransactionsWithDetails(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllSplits(),
            categoryRepository.observeAll(),
        ) { list, sharedExpenses, splits, categories ->
            val categoryMap = categories.associateBy { it.id }
            val filtered = list.filter {
                val tx = it.transaction
                !tx.isDeleted && tx.status != TransactionStatus.CANCELLED &&
                    tx.date in startMs..endMs &&
                    (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.REFUND)
            }
            val friendPaid = FriendPaidSharedExpenseHelper.resolveFriendPaidShares(list, sharedExpenses, splits)
                .filter { it.expense.date in startMs..endMs }

            val totalsByCat = mutableMapOf<Long?, Long>()
            val countsByCat = mutableMapOf<Long?, Int>()
            for (item in filtered) {
                val catId = item.category?.id
                val delta = if (item.transaction.type == TransactionType.EXPENSE) item.transaction.personalAmount
                else -item.transaction.personalAmount
                totalsByCat[catId] = (totalsByCat[catId] ?: 0L) + delta
                countsByCat[catId] = (countsByCat[catId] ?: 0) + 1
            }
            for (entry in friendPaid) {
                val catId = entry.expense.categoryId
                totalsByCat[catId] = (totalsByCat[catId] ?: 0L) + entry.userSharePaise
                countsByCat[catId] = (countsByCat[catId] ?: 0) + 1
            }

            val rawItems = totalsByCat.mapNotNull { (catId, rawTotal) ->
                val net = rawTotal.coerceAtLeast(0L)
                if (net <= 0L) return@mapNotNull null
                val cat = catId?.let { categoryMap[it] }
                CategorySpendingItem(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Uncategorized",
                    colorHex = cat?.colorHex ?: "#7A7A7A",
                    totalPaise = net,
                    percent = 0.0,
                    transactionCount = countsByCat[catId] ?: 0,
                )
            }.sortedByDescending { it.totalPaise }

            val totalAll = rawItems.sumOf { it.totalPaise }.coerceAtLeast(1L)
            rawItems.map { it.copy(percent = (it.totalPaise * 100.0) / totalAll) }
        }
    }

    fun observeTransactionsInCategory(
        categoryId: Long?,
        startMs: Long,
        endMs: Long,
    ): Flow<List<TransactionWithDetails>> {
        return transactionRepository.observeAllTransactionsWithDetails().map { list ->
            list.filter {
                val tx = it.transaction
                !tx.isDeleted && tx.status != TransactionStatus.CANCELLED &&
                    tx.date in startMs..endMs &&
                    tx.categoryId == categoryId &&
                    (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.REFUND)
            }
        }
    }
}

class GetPeriodComparisonUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val socialRepository: SocialRepository,
) {
    operator fun invoke(): Flow<PeriodComparison> {
        return combine(
            transactionRepository.observeAllTransactionsWithDetails(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllSplits(),
        ) { list, sharedExpenses, splits ->
            val friendPaidShares = FriendPaidSharedExpenseHelper.resolveFriendPaidShares(list, sharedExpenses, splits)
            val cal = Calendar.getInstance()
            val (curStart, curEnd) = GetMonthlySummaryUseCase.monthBounds(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
            )
            cal.add(Calendar.MONTH, -1)
            val (prevStart, prevEnd) = GetMonthlySummaryUseCase.monthBounds(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
            )

            fun totalsIn(s: Long, e: Long): Pair<Long, Long> {
                var inc = 0L
                var exp = 0L
                for (item in list) {
                    val tx = item.transaction
                    if (tx.isDeleted || tx.status == TransactionStatus.CANCELLED || tx.date !in s..e) continue
                    when (tx.type) {
                        TransactionType.INCOME -> inc += tx.amountInPrimary
                        TransactionType.EXPENSE -> exp += tx.personalAmount
                        TransactionType.REFUND -> exp -= tx.personalAmount
                        else -> Unit
                    }
                }
                for (entry in friendPaidShares) {
                    if (entry.expense.date in s..e) {
                        exp += entry.userSharePaise
                    }
                }
                return inc to exp.coerceAtLeast(0L)
            }

            val (curInc, curExp) = totalsIn(curStart, curEnd)
            val (prevInc, prevExp) = totalsIn(prevStart, prevEnd)
            PeriodComparison(curInc, prevInc, curExp, prevExp)
        }
    }
}

class GetAgingReportUseCase @Inject constructor(
    private val computePersonBalanceUseCase: ComputePersonBalanceUseCase,
    private val socialRepository: SocialRepository,
) {
    operator fun invoke(): Flow<List<AgingBucketPerson>> {
        return combine(
            computePersonBalanceUseCase(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllPayers(),
            socialRepository.observeAllSplits(),
        ) { balances, expenses, payers, splits ->
            val now = System.currentTimeMillis()
            val payersByExp = payers.groupBy { it.sharedExpenseId }
            val splitsByExp = splits.groupBy { it.sharedExpenseId }

            // Build per-person positive receivable entries sorted oldest-first for FIFO aging
            val receivablesByPerson = mutableMapOf<Long, MutableList<Pair<Long, Long>>>()
            for (exp in expenses) {
                if (exp.isDeleted) continue
                val deltas = BalanceCalculator.bilateralDeltasForExpense(
                    payersByExp[exp.id].orEmpty(),
                    splitsByExp[exp.id].orEmpty(),
                )
                for ((personId, delta) in deltas) {
                    if (delta > 0L) {
                        receivablesByPerson.getOrPut(personId) { mutableListOf() }.add(exp.date to delta)
                    }
                }
            }

            balances.filter { it.netBalancePaise > 0L }.map { b ->
                val entries = receivablesByPerson[b.person.id].orEmpty().sortedBy { it.first }
                // FIFO: match current outstanding against newest receivables backward (equivalent to paying oldest first)
                var remainingToCover = b.netBalancePaise
                var oldestUnpaidDate = now
                for ((dateMs, amount) in entries.asReversed()) {
                    oldestUnpaidDate = dateMs
                    remainingToCover -= amount
                    if (remainingToCover <= 0L) break
                }
                val days = ((now - oldestUnpaidDate) / 86_400_000L).toInt().coerceAtLeast(0)
                val bucket = when {
                    days <= 30 -> "0–30 days"
                    days <= 60 -> "31–60 days"
                    else -> "60+ days"
                }
                AgingBucketPerson(
                    person = b.person,
                    outstandingPaise = b.netBalancePaise,
                    daysOutstanding = days,
                    bucketLabel = bucket,
                )
            }
        }
    }
}

