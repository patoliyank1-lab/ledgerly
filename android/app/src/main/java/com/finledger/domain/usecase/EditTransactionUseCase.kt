package com.finledger.domain.usecase

import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.TransactionRepository
import javax.inject.Inject

class EditTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        updated: TransactionEntity,
        tagIds: List<Long>? = null,
    ): Result<Unit> {
        if (updated.amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (updated.type == TransactionType.TRANSFER && updated.accountId == updated.toAccountId) {
            return Result.failure(IllegalArgumentException("Source and destination accounts must be different"))
        }
        val existing = transactionRepository.getById(updated.id)
            ?: return Result.failure(IllegalStateException("Transaction not found"))

        return runCatching {
            val isIncomeOrExpense = updated.type == TransactionType.EXPENSE ||
                updated.type == TransactionType.REFUND ||
                updated.type == TransactionType.INCOME
            val normalized = when {
                !isIncomeOrExpense -> updated.copy(personalAmount = 0L, amountInPrimary = updated.amount)
                existing.isPersonal -> updated.copy(personalAmount = updated.amount, amountInPrimary = updated.amount)
                else -> updated.copy(amountInPrimary = updated.amount)
            }
            transactionRepository.update(existing, normalized, tagIds)
        }
    }
}

