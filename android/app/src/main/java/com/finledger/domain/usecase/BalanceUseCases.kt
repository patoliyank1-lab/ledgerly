package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToLong

data class PersonBalanceSummary(
    val person: PersonEntity,
    val netBalancePaise: Long, // > 0 person owes user; < 0 user owes person; 0 settled
    val fromSharedExpensesPaise: Long,
    val fromSettlementsPaise: Long,
    val fromLoansPaise: Long = 0L,
)

data class PersonLedgerEntry(
    val id: String,
    val dateMs: Long,
    val title: String,
    val subtitle: String,
    val deltaPaise: Long, // + increases what person owes user; - increases what user owes person
)

data class SuggestedSettlement(
    val fromPersonId: Long,
    val toPersonId: Long,
    val amountPaise: Long,
)

/**
 * Pure bilateral and group balance calculator implementing FINANCIAL_RULES.md §10 & §12.
 */
object BalanceCalculator {

    fun bilateralDeltasForExpense(
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
    ): Map<Long, Long> {
        val participantIds = (payers.map { it.personId } + splits.filter { !it.isExcluded }.map { it.personId }).distinct()
        val netMap = participantIds.associateWith { id ->
            val paid = payers.filter { it.personId == id }.sumOf { it.amountPaid }
            val share = splits.filter { it.personId == id && !it.isExcluded }.sumOf { it.shareAmount }
            paid - share
        }

        val userNet = netMap[SplitCalculator.USER_PERSON_ID] ?: 0L
        if (userNet == 0L) return emptyMap()

        val result = LinkedHashMap<Long, Long>()
        if (userNet > 0L) {
            val debtors = netMap.filter { it.key != SplitCalculator.USER_PERSON_ID && it.value < 0L }
            val totalDeficit = debtors.values.sumOf { -it }
            if (totalDeficit <= 0L) return emptyMap()

            val totalSurplus = netMap.values.filter { it > 0L }.sum()
            val userShareOfSurplus = if (totalSurplus > 0L) userNet.toDouble() / totalSurplus.toDouble() else 1.0

            val entries = debtors.entries.toList()
            var allocated = 0L
            entries.forEachIndexed { index, (personId, deficitNeg) ->
                val rawOwedByPerson = if (index == entries.lastIndex) {
                    (userNet - allocated).coerceAtLeast(0L)
                } else {
                    ((-deficitNeg) * userShareOfSurplus).roundToLong()
                }
                result[personId] = rawOwedByPerson
                allocated += rawOwedByPerson
            }
        } else {
            val userOwesTotal = -userNet
            val creditors = netMap.filter { it.key != SplitCalculator.USER_PERSON_ID && it.value > 0L }
            val totalSurplus = creditors.values.sum()
            if (totalSurplus <= 0L) return emptyMap()

            val entries = creditors.entries.toList()
            var allocated = 0L
            entries.forEachIndexed { index, (personId, surplus) ->
                val rawOwedToPerson = if (index == entries.lastIndex) {
                    (userOwesTotal - allocated).coerceAtLeast(0L)
                } else {
                    ((userOwesTotal.toDouble() * surplus.toDouble()) / totalSurplus.toDouble()).roundToLong()
                }
                result[personId] = -rawOwedToPerson
                allocated += rawOwedToPerson
            }
        }
        return result
    }

    fun simplifyDebts(netBalances: Map<Long, Long>): List<SuggestedSettlement> {
        val creditors = netBalances.filter { it.value > 0L }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()
        val debtors = netBalances.filter { it.value < 0L }
            .map { it.key to -it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val result = mutableListOf<SuggestedSettlement>()
        var cIdx = 0
        var dIdx = 0
        while (cIdx < creditors.size && dIdx < debtors.size) {
            val (creditorId, creditAmt) = creditors[cIdx]
            val (debtorId, debtAmt) = debtors[dIdx]
            val pay = min(creditAmt, debtAmt)
            if (pay > 0L) {
                result.add(SuggestedSettlement(fromPersonId = debtorId, toPersonId = creditorId, amountPaise = pay))
            }
            val remCredit = creditAmt - pay
            val remDebt = debtAmt - pay
            if (remCredit == 0L) cIdx++ else creditors[cIdx] = creditorId to remCredit
            if (remDebt == 0L) dIdx++ else debtors[dIdx] = debtorId to remDebt
        }
        return result
    }
}

/**
 * Computes consolidated person-wise balances across shared expenses, settlements, AND personal loans (P2-16 + Phase 3).
 */
class ComputePersonBalanceUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
    private val commitmentRepository: CommitmentRepository,
) {
    operator fun invoke(): Flow<List<PersonBalanceSummary>> {
        return combine<Any?, List<PersonBalanceSummary>>(
            socialRepository.observeActivePersons(),
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllPayers(),
            socialRepository.observeAllSplits(),
            socialRepository.observeAllSettlements(),
            commitmentRepository.observeAllLoans(),
            commitmentRepository.observeAllRepayments(),
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val persons = values[0] as List<PersonEntity>
            @Suppress("UNCHECKED_CAST")
            val expenses = values[1] as List<SharedExpenseEntity>
            @Suppress("UNCHECKED_CAST")
            val allPayers = values[2] as List<ExpensePayerEntity>
            @Suppress("UNCHECKED_CAST")
            val allSplits = values[3] as List<ExpenseSplitEntity>
            @Suppress("UNCHECKED_CAST")
            val settlements = values[4] as List<SettlementEntity>
            @Suppress("UNCHECKED_CAST")
            val loans = values[5] as List<LoanEntity>
            @Suppress("UNCHECKED_CAST")
            val repayments = values[6] as List<LoanRepaymentEntity>

            val payersByExp = allPayers.groupBy { it.sharedExpenseId }
            val splitsByExp = allSplits.groupBy { it.sharedExpenseId }
            val repByLoan = repayments.groupBy { it.loanId }

            val sharedTotals = mutableMapOf<Long, Long>()
            for (expense in expenses) {
                val deltas = BalanceCalculator.bilateralDeltasForExpense(
                    payers = payersByExp[expense.id].orEmpty(),
                    splits = splitsByExp[expense.id].orEmpty(),
                )
                for ((personId, delta) in deltas) {
                    sharedTotals[personId] = (sharedTotals[personId] ?: 0L) + delta
                }
            }

            val settlementTotals = mutableMapOf<Long, Long>()
            for (s in settlements) {
                if (s.status == SettlementStatus.CANCELLED) continue
                val delta = when (s.direction) {
                    SettlementDirection.RECEIVED -> -s.amount
                    SettlementDirection.PAID -> s.amount
                }
                settlementTotals[s.personId] = (settlementTotals[s.personId] ?: 0L) + delta
            }

            val loanTotals = mutableMapOf<Long, Long>()
            for (loan in loans) {
                val pid = loan.personId ?: continue
                val repaid = repByLoan[loan.id].orEmpty().sumOf { it.principalAmount }
                val outstanding = (loan.principalAmount - repaid).coerceAtLeast(0L)
                val delta = if (loan.type == LoanType.LENT) outstanding else -outstanding
                loanTotals[pid] = (loanTotals[pid] ?: 0L) + delta
            }

            persons.map { person ->
                val shared = sharedTotals[person.id] ?: 0L
                val settled = settlementTotals[person.id] ?: 0L
                val loanNet = loanTotals[person.id] ?: 0L
                PersonBalanceSummary(
                    person = person,
                    netBalancePaise = shared + settled + loanNet,
                    fromSharedExpensesPaise = shared,
                    fromSettlementsPaise = settled,
                    fromLoansPaise = loanNet,
                )
            }.sortedByDescending { kotlin.math.abs(it.netBalancePaise) }
        }
    }

    fun observeLedgerForPerson(personId: Long): Flow<List<PersonLedgerEntry>> {
        return combine<Any?, List<PersonLedgerEntry>>(
            socialRepository.observeAllSharedExpenses(),
            socialRepository.observeAllPayers(),
            socialRepository.observeAllSplits(),
            socialRepository.observeSettlementsByPerson(personId),
            commitmentRepository.observeAllLoans(),
            commitmentRepository.observeAllRepayments(),
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val expenses = values[0] as List<SharedExpenseEntity>
            @Suppress("UNCHECKED_CAST")
            val allPayers = values[1] as List<ExpensePayerEntity>
            @Suppress("UNCHECKED_CAST")
            val allSplits = values[2] as List<ExpenseSplitEntity>
            @Suppress("UNCHECKED_CAST")
            val settlements = values[3] as List<SettlementEntity>
            @Suppress("UNCHECKED_CAST")
            val loans = values[4] as List<LoanEntity>
            @Suppress("UNCHECKED_CAST")
            val repayments = values[5] as List<LoanRepaymentEntity>

            val payersByExp = allPayers.groupBy { it.sharedExpenseId }
            val splitsByExp = allSplits.groupBy { it.sharedExpenseId }

            val entries = mutableListOf<PersonLedgerEntry>()
            for (expense in expenses) {
                val deltas = BalanceCalculator.bilateralDeltasForExpense(
                    payers = payersByExp[expense.id].orEmpty(),
                    splits = splitsByExp[expense.id].orEmpty(),
                )
                val delta = deltas[personId] ?: 0L
                if (delta != 0L) {
                    entries.add(
                        PersonLedgerEntry(
                            id = "exp_${expense.id}",
                            dateMs = expense.date,
                            title = expense.description,
                            subtitle = if (delta > 0) "Shared expense · owes you" else "Shared expense · you owe",
                            deltaPaise = delta,
                        )
                    )
                }
            }
            for (s in settlements) {
                if (s.status == SettlementStatus.CANCELLED) continue
                val delta = if (s.direction == SettlementDirection.RECEIVED) -s.amount else s.amount
                entries.add(
                    PersonLedgerEntry(
                        id = "set_${s.id}",
                        dateMs = s.date,
                        title = if (s.direction == SettlementDirection.RECEIVED) "Settlement received" else "Settlement paid",
                        subtitle = s.notes.ifBlank { s.paymentMethod.ifBlank { "Recorded settlement" } },
                        deltaPaise = delta,
                    )
                )
            }
            val personLoans = loans.filter { it.personId == personId }
            val loanMap = personLoans.associateBy { it.id }
            for (loan in personLoans) {
                val delta = if (loan.type == LoanType.LENT) loan.principalAmount else -loan.principalAmount
                entries.add(
                    PersonLedgerEntry(
                        id = "loan_${loan.id}",
                        dateMs = loan.dateGiven,
                        title = if (loan.type == LoanType.LENT) "Money Lent" else "Money Borrowed",
                        subtitle = loan.notes.ifBlank { "Loan principal" },
                        deltaPaise = delta,
                    )
                )
            }
            for (rep in repayments) {
                val loan = loanMap[rep.loanId] ?: continue
                val delta = if (loan.type == LoanType.LENT) -rep.principalAmount else rep.principalAmount
                entries.add(
                    PersonLedgerEntry(
                        id = "rep_${rep.id}",
                        dateMs = rep.date,
                        title = "Loan Repayment",
                        subtitle = rep.notes.ifBlank { "Principal repayment" },
                        deltaPaise = delta,
                    )
                )
            }
            entries.sortedByDescending { it.dateMs }
        }
    }
}

class RecordSettlementUseCase @Inject constructor(
    private val socialRepository: SocialRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        personId: Long,
        amountPaise: Long,
        direction: SettlementDirection,
        dateMs: Long,
        accountId: Long?,
        paymentMethod: String = "",
        referenceNumber: String = "",
        notes: String = "",
        groupId: Long? = null,
        eventId: Long? = null,
    ): Result<Long> {
        if (amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Settlement amount must be greater than zero"))
        }
        return runCatching {
            val linkedTxId = if (accountId != null) {
                transactionRepository.insert(
                    TransactionEntity(
                        type = if (direction == SettlementDirection.RECEIVED) {
                            TransactionType.REPAYMENT_RECEIVED
                        } else {
                            TransactionType.REPAYMENT_MADE
                        },
                        amount = amountPaise,
                        personalAmount = 0L,
                        amountInPrimary = amountPaise,
                        date = dateMs,
                        accountId = accountId,
                        personId = personId,
                        groupId = groupId,
                        eventId = eventId,
                        paymentMethod = paymentMethod.trim(),
                        description = if (direction == SettlementDirection.RECEIVED) "Settlement Received" else "Settlement Paid",
                        notes = notes.trim(),
                    )
                )
            } else {
                null
            }

            socialRepository.insertSettlement(
                SettlementEntity(
                    personId = personId,
                    amount = amountPaise,
                    direction = direction,
                    date = dateMs,
                    accountId = accountId,
                    paymentMethod = paymentMethod.trim(),
                    referenceNumber = referenceNumber.trim(),
                    notes = notes.trim(),
                    groupId = groupId,
                    eventId = eventId,
                    linkedTransactionId = linkedTxId,
                )
            )
        }
    }
}

class SimplifySettlementsUseCase @Inject constructor() {
    operator fun invoke(
        expenses: List<SharedExpenseEntity>,
        payersByExpense: Map<Long, List<ExpensePayerEntity>>,
        splitsByExpense: Map<Long, List<ExpenseSplitEntity>>,
        settlements: List<SettlementEntity> = emptyList(),
    ): List<SuggestedSettlement> {
        val net = mutableMapOf<Long, Long>()
        for (exp in expenses) {
            for (p in payersByExpense[exp.id].orEmpty()) {
                net[p.personId] = (net[p.personId] ?: 0L) + p.amountPaid
            }
            for (s in splitsByExpense[exp.id].orEmpty()) {
                if (!s.isExcluded) {
                    net[s.personId] = (net[s.personId] ?: 0L) - s.shareAmount
                }
            }
        }
        for (st in settlements) {
            if (st.status == SettlementStatus.CANCELLED) continue
            if (st.direction == SettlementDirection.RECEIVED) {
                net[st.personId] = (net[st.personId] ?: 0L) + st.amount
                net[SplitCalculator.USER_PERSON_ID] = (net[SplitCalculator.USER_PERSON_ID] ?: 0L) - st.amount
            } else {
                net[SplitCalculator.USER_PERSON_ID] = (net[SplitCalculator.USER_PERSON_ID] ?: 0L) + st.amount
                net[st.personId] = (net[st.personId] ?: 0L) - st.amount
            }
        }
        return BalanceCalculator.simplifyDebts(net)
    }
}
