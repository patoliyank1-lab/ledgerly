package com.finledger.domain.usecase

import com.finledger.data.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: Long): Result<Unit> = runCatching {
        transactionRepository.softDelete(transactionId)
    }

    suspend fun restore(transactionId: Long): Result<Unit> = runCatching {
        transactionRepository.restore(transactionId)
    }
}

