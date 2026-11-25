package com.finledger.domain.usecase

import com.finledger.data.local.entity.*
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class LoanWithBalance(
    val loan: LoanEntity,
    val principalRepaidPaise: Long,
    val interestPaidPaise: Long,
    val feePaidPaise: Long,
    val outstandingPrincipalPaise: Long,
)

/**
 * Records lending or borrowing without fake expense/income (FINANCIAL_RULES.md §4 & §5).
 */
class CreateLoanUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        type: LoanType,
        personOrOrg: String,
        personId: Long?,
        principalPaise: Long,
        dateGivenMs: Long,
        expectedRepaymentDateMs: Long?,
        interestRate: Double = 0.0,
        interestType: String = "NONE",
        repaymentFrequency: String = "NONE",
        guarantorName: String = "",
        accountId: Long?,
        notes: String = "",
    ): Result<Long> {
        if (personOrOrg.isBlank()) {
            return Result.failure(IllegalArgumentException("Person or lender name is required"))
        }
        if (principalPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Principal amount must be greater than zero"))
        }
        return runCatching {
            val loanId = commitmentRepository.insertLoan(
                LoanEntity(
                    type = type,
                    personOrOrg = personOrOrg.trim(),
                    personId = personId,
                    principalAmount = principalPaise,
                    dateGiven = dateGivenMs,
                    expectedRepaymentDate = expectedRepaymentDateMs,
                    interestRate = interestRate,
                    interestType = interestType,
                    repaymentFrequency = repaymentFrequency,
                    guarantorName = guarantorName.trim(),
                    accountId = accountId,
                    notes = notes.trim(),
                )
            )
            if (accountId != null) {
                transactionRepository.insert(
                    TransactionEntity(
                        type = if (type == LoanType.LENT) TransactionType.LENDING else TransactionType.BORROWING,
                        amount = principalPaise,
                        personalAmount = 0L,
                        amountInPrimary = principalPaise,
                        date = dateGivenMs,
                        accountId = accountId,
                        personId = personId,
                        merchantOrPayee = personOrOrg.trim(),
                        description = if (type == LoanType.LENT) "Lent to ${personOrOrg.trim()}" else "Borrowed from ${personOrOrg.trim()}",
                        notes = notes.trim(),
                    )
                )
            }
            loanId
        }
    }
}

/**
 * Records a partial or full loan repayment, separating principal from interest/fees (FINANCIAL_RULES.md §6).
 */
class RecordRepaymentUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        loanId: Long,
        principalPaise: Long,
        interestPaise: Long = 0L,
        feePaise: Long = 0L,
        dateMs: Long,
        accountId: Long?,
        notes: String = "",
    ): Result<Long> {
        if (principalPaise <= 0L && interestPaise <= 0L && feePaise <= 0L) {
            return Result.failure(IllegalArgumentException("Enter a repayment amount greater than zero"))
        }
        val loan = commitmentRepository.getLoanById(loanId)
            ?: return Result.failure(IllegalStateException("Loan not found"))

        return runCatching {
            val repId = commitmentRepository.insertRepayment(
                LoanRepaymentEntity(
                    loanId = loanId,
                    date = dateMs,
                    principalAmount = principalPaise,
                    interestAmount = interestPaise,
                    feeAmount = feePaise,
                    accountId = accountId,
                    notes = notes.trim(),
                )
            )

            if (accountId != null) {
                if (principalPaise > 0L) {
                    transactionRepository.insert(
                        TransactionEntity(
                            type = if (loan.type == LoanType.LENT) {
                                TransactionType.REPAYMENT_RECEIVED
                            } else {
                                TransactionType.REPAYMENT_MADE
                            },
                            amount = principalPaise,
                            personalAmount = 0L,
                            amountInPrimary = principalPaise,
                            date = dateMs,
                            accountId = accountId,
                            personId = loan.personId,
                            merchantOrPayee = loan.personOrOrg,
                            description = "Loan Principal Repayment",
                            notes = notes.trim(),
                        )
                    )
                }
                val interestAndFee = interestPaise + feePaise
                if (interestAndFee > 0L) {
                    transactionRepository.insert(
                        TransactionEntity(
                            type = if (loan.type == LoanType.LENT) TransactionType.INCOME else TransactionType.EXPENSE,
                            amount = interestAndFee,
                            personalAmount = interestAndFee,
                            amountInPrimary = interestAndFee,
                            date = dateMs,
                            accountId = accountId,
                            personId = loan.personId,
                            merchantOrPayee = loan.personOrOrg,
                            description = if (loan.type == LoanType.LENT) "Loan Interest Received" else "Loan Interest & Fees",
                            notes = notes.trim(),
                        )
                    )
                }
            }

            // Recalculate loan status
            val allRepayments = commitmentRepository.getRepaymentsForLoanOnce(loanId)
            val totalRepaidPrincipal = allRepayments.sumOf { it.principalAmount }
            val newStatus = when {
                totalRepaidPrincipal >= loan.principalAmount -> LoanStatus.CLOSED
                totalRepaidPrincipal > 0L -> LoanStatus.PARTIALLY_REPAID
                else -> LoanStatus.ACTIVE
            }
            commitmentRepository.updateLoanStatus(loanId, newStatus)
            repId
        }
    }
}

class GetLoanBalanceUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
) {
    operator fun invoke(): Flow<List<LoanWithBalance>> {
        return combine(
            commitmentRepository.observeAllLoans(),
            commitmentRepository.observeAllRepayments(),
        ) { loans, repayments ->
            val repByLoan = repayments.groupBy { it.loanId }
            loans.map { loan ->
                val list = repByLoan[loan.id].orEmpty()
                val principalPaid = list.sumOf { it.principalAmount }
                LoanWithBalance(
                    loan = loan,
                    principalRepaidPaise = principalPaid,
                    interestPaidPaise = list.sumOf { it.interestAmount },
                    feePaidPaise = list.sumOf { it.feeAmount },
                    outstandingPrincipalPaise = (loan.principalAmount - principalPaid).coerceAtLeast(0L),
                )
            }
        }
    }
}

