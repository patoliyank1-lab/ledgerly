package com.finledger.domain.usecase

import com.finledger.data.local.entity.TransactionEntity
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
        val existing = transactionRepository.getById(updated.id)
            ?: return Result.failure(IllegalStateException("Transaction not found"))

        return runCatching {
            val normalized = if (existing.isPersonal) {
                updated.copy(personalAmount = updated.amount, amountInPrimary = updated.amount)
            } else {
                updated.copy(amountInPrimary = updated.amount)
            }
            transactionRepository.update(existing, normalized, tagIds)
        }
    }
}

