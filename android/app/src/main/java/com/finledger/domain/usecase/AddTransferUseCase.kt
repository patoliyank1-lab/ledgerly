package com.finledger.domain.usecase

import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * Transfers move funds between user's own accounts (or pay off credit card liability)
 * without counting as income or expense (FINANCIAL_RULES.md §2 & §7).
 */
class AddTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        amountPaise: Long,
        fromAccountId: Long,
        toAccountId: Long,
        dateMs: Long,
        notes: String = "",
    ): Result<Long> {
        if (amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Transfer amount must be greater than zero"))
        }
        if (fromAccountId == toAccountId) {
            return Result.failure(IllegalArgumentException("Source and destination accounts must differ"))
        }
        return runCatching {
            val tx = TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = amountPaise,
                personalAmount = 0L,
                amountInPrimary = amountPaise,
                date = dateMs,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                description = "Account Transfer",
                notes = notes.trim(),
                isPersonal = true,
            )
            transactionRepository.insert(tx)
        }
    }
}

