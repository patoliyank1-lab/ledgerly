package com.finledger.domain.usecase

import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.TransactionRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        amountPaise: Long,
        accountId: Long,
        categoryId: Long?,
        dateMs: Long,
        merchantOrPayee: String = "",
        location: String = "",
        paymentMethod: String = "",
        description: String = "",
        notes: String = "",
        isRefund: Boolean = false,
        parentTransactionId: Long? = null,
        tagIds: List<Long> = emptyList(),
    ): Result<Long> {
        if (amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        return runCatching {
            val tx = TransactionEntity(
                type = if (isRefund) TransactionType.REFUND else TransactionType.EXPENSE,
                amount = amountPaise,
                personalAmount = amountPaise,
                amountInPrimary = amountPaise,
                date = dateMs,
                accountId = accountId,
                categoryId = categoryId,
                merchantOrPayee = merchantOrPayee.trim(),
                location = location.trim(),
                paymentMethod = paymentMethod.trim(),
                description = description.trim(),
                notes = notes.trim(),
                parentTransactionId = parentTransactionId,
                isPersonal = true,
            )
            transactionRepository.insert(tx, tagIds)
        }
    }
}

