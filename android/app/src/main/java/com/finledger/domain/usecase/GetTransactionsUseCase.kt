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
            filterTransactions(list, filter)
        }
    }

    companion object {
        fun filterTransactions(
            list: List<TransactionWithDetails>,
            filter: TransactionFilter,
        ): List<TransactionWithDetails> {
            val trimmedQuery = filter.searchQuery.trim()
            return list.filter { item ->
                val tx = item.transaction
                val matchesQuery = trimmedQuery.isEmpty() || listOf(
                    tx.merchantOrPayee,
                    tx.description,
                    tx.notes,
                    tx.location,
                    item.accountName,
                    item.category?.name.orEmpty(),
                ).any { it.contains(trimmedQuery, ignoreCase = true) }

                val matchesType = filter.type == null || tx.type == filter.type
                val matchesCategory = filter.categoryId == null || tx.categoryId == filter.categoryId
                val matchesAccount = filter.accountId == null ||
                    tx.accountId == filter.accountId ||
                    tx.toAccountId == filter.accountId
                val matchesStart = filter.startDateMs == null || tx.date >= filter.startDateMs
                val matchesEnd = filter.endDateMs == null || tx.date <= filter.endDateMs
                val matchesShared = !filter.onlyShared || !tx.isPersonal || tx.sharedExpenseId != null

                matchesQuery &&
                    matchesType &&
                    matchesCategory &&
                    matchesAccount &&
                    matchesStart &&
                    matchesEnd &&
                    matchesShared
            }
        }
    }
}
