package com.finledger.domain.usecase

import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.TransactionRepository
import javax.inject.Inject

class AddIncomeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        amountPaise: Long,
        accountId: Long,
        categoryId: Long?,
        dateMs: Long,
        source: String = "",
        description: String = "",
        notes: String = "",
        tagIds: List<Long> = emptyList(),
    ): Result<Long> {
        if (amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Income amount must be greater than zero"))
        }
        return runCatching {
            val tx = TransactionEntity(
                type = TransactionType.INCOME,
                amount = amountPaise,
                personalAmount = amountPaise,
                amountInPrimary = amountPaise,
                date = dateMs,
                accountId = accountId,
                categoryId = categoryId,
                merchantOrPayee = source.trim(),
                description = description.trim(),
                notes = notes.trim(),
                isPersonal = true,
            )
            transactionRepository.insert(tx, tagIds)
        }
    }
}

