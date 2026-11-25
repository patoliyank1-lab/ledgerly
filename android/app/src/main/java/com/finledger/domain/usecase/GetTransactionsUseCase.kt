package com.finledger.domain.usecase

import com.finledger.data.repository.TransactionRepository
import com.finledger.domain.model.TransactionFilter
import com.finledger.domain.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(filter: TransactionFilter = TransactionFilter()): Flow<List<TransactionWithDetails>> {
        return transactionRepository.observeAllTransactionsWithDetails().map { list ->
            list.filter { item ->
                val tx = item.transaction
                val matchesQuery = filter.searchQuery.isBlank() || listOf(
                    tx.merchantOrPayee,
                    tx.description,
                    tx.notes,
                    tx.location,
                    item.accountName,
                    item.category?.name.orEmpty(),
                ).any { it.contains(filter.searchQuery.trim(), ignoreCase = true) }

                val matchesType = filter.type == null || tx.type == filter.type
                val matchesCategory = filter.categoryId == null || tx.categoryId == filter.categoryId
                val matchesAccount = filter.accountId == null ||
                    tx.accountId == filter.accountId ||
                    tx.toAccountId == filter.accountId
                val matchesStart = filter.startDateMs == null || tx.date >= filter.startDateMs
                val matchesEnd = filter.endDateMs == null || tx.date <= filter.endDateMs

                matchesQuery && matchesType && matchesCategory && matchesAccount && matchesStart && matchesEnd
            }
        }
    }
}

