package com.finledger.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.AuditLogEntity
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.TagEntity
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.AuditLogRepository
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.TagRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.QuickTransactionTemplate
import com.finledger.domain.model.TransactionFilter
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.AddExpenseUseCase
import com.finledger.domain.usecase.AddIncomeUseCase
import com.finledger.domain.usecase.AddTransferUseCase
import com.finledger.domain.usecase.DeleteTransactionUseCase
import com.finledger.domain.usecase.EditTransactionUseCase
import com.finledger.domain.usecase.GetAccountsUseCase
import com.finledger.domain.usecase.GetTransactionsUseCase
import com.finledger.domain.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MerchantHistoryStats(
    val topExpenseMerchants: List<String> = emptyList(),
    val topIncomeSources: List<String> = emptyList(),
    val expenseCategoryByMerchant: Map<String, Long> = emptyMap(),
    val incomeCategoryBySource: Map<String, Long> = emptyMap(),
)

data class TransactionsUiState(
    val groupedTransactions: Map<String, List<TransactionWithDetails>> = emptyMap(),
    val filter: TransactionFilter = TransactionFilter(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val topExpenseCategoryIds: List<Long> = emptyList(),
    val topIncomeCategoryIds: List<Long> = emptyList(),
    val merchantHistory: MerchantHistoryStats = MerchantHistoryStats(),
    val lastTransaction: TransactionWithDetails? = null,
    val quickTemplates: List<QuickTransactionTemplate> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val lastUsedAccountId: Long? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val auditLogRepository: AuditLogRepository,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val addTransferUseCase: AddTransferUseCase,
    private val editTransactionUseCase: EditTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _error = MutableStateFlow<String?>(null)

    private val filteredTransactions = _filter.flatMapLatest { filter ->
        getTransactionsUseCase(filter)
    }

    val uiState: StateFlow<TransactionsUiState> = combine<Any?, TransactionsUiState>(
        filteredTransactions,
        transactionRepository.observeAllTransactionsWithDetails(),
        _filter,
        getAccountsUseCase(),
        categoryRepository.observeAll(),
        tagRepository.observeAll(),
        userPreferencesRepository.preferences,
        _error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txs = values[0] as List<TransactionWithDetails>
        @Suppress("UNCHECKED_CAST")
        val allWithDetails = values[1] as List<TransactionWithDetails>
        val allTxs = allWithDetails.map { it.transaction }
        val filter = values[2] as TransactionFilter
        @Suppress("UNCHECKED_CAST")
        val accounts = values[3] as List<AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val categories = values[4] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val tags = values[5] as List<TagEntity>
        val prefs = values[6] as com.finledger.domain.model.UserPreferences
        val error = values[7] as String?

        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
        val incomeCats = categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH }

        TransactionsUiState(
            groupedTransactions = txs.groupBy { DateFormatter.formatDateHeader(it.transaction.date) },
            filter = filter,
            accounts = accounts,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            topExpenseCategoryIds = computeTopCategoryIds(allTxs, expenseCats, TransactionType.EXPENSE),
            topIncomeCategoryIds = computeTopCategoryIds(allTxs, incomeCats, TransactionType.INCOME),
            merchantHistory = computeMerchantHistoryStats(allTxs, expenseCats, incomeCats),
            lastTransaction = findLastRepeatableTransaction(allWithDetails),
            quickTemplates = prefs.quickTemplates,
            allCategories = categories,
            tags = tags,
            primaryCurrency = prefs.primaryCurrency,
            lastUsedAccountId = prefs.lastUsedAccountId,
            errorMessage = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun updateSearchQuery(query: String) {
        _filter.update { it.copy(searchQuery = query) }
    }

    fun updateFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun clearFilter() {
        _filter.value = TransactionFilter()
    }

    private val txFlows = ConcurrentHashMap<Long, StateFlow<TransactionEntity?>>()
    private val auditFlows = ConcurrentHashMap<Long, StateFlow<List<AuditLogEntity>>>()

    fun observeTransaction(id: Long): StateFlow<TransactionEntity?> = txFlows.getOrPut(id) {
        transactionRepository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    fun observeAuditHistory(id: Long): StateFlow<List<AuditLogEntity>> = auditFlows.getOrPut(id) {
        auditLogRepository.observeForEntity("transaction", id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun addExpense(
        amountPaise: Long,
        accountId: Long,
        categoryId: Long?,
        dateMs: Long,
        merchant: String,
        location: String,
        paymentMethod: String,
        description: String,
        notes: String,
        isRefund: Boolean,
        tagIds: List<Long>,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            addExpenseUseCase(
                amountPaise = amountPaise,
                accountId = accountId,
                categoryId = categoryId,
                dateMs = dateMs,
                merchantOrPayee = merchant,
                location = location,
                paymentMethod = paymentMethod,
                description = description,
                notes = notes,
                isRefund = isRefund,
                tagIds = tagIds,
            ).onSuccess {
                _error.value = null
                userPreferencesRepository.setLastUsedAccountId(accountId)
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun addIncome(
        amountPaise: Long,
        accountId: Long,
        categoryId: Long?,
        dateMs: Long,
        source: String,
        description: String,
        notes: String,
        tagIds: List<Long>,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            addIncomeUseCase(
                amountPaise = amountPaise,
                accountId = accountId,
                categoryId = categoryId,
                dateMs = dateMs,
                source = source,
                description = description,
                notes = notes,
                tagIds = tagIds,
            ).onSuccess {
                _error.value = null
                userPreferencesRepository.setLastUsedAccountId(accountId)
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun addTransfer(
        amountPaise: Long,
        fromAccountId: Long,
        toAccountId: Long,
        dateMs: Long,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            addTransferUseCase(
                amountPaise = amountPaise,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                dateMs = dateMs,
                notes = notes,
            ).onSuccess {
                _error.value = null
                userPreferencesRepository.setLastUsedAccountId(fromAccountId)
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun updateTransaction(
        existing: TransactionEntity,
        amountPaise: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        dateMs: Long,
        merchantOrPayee: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val updated = existing.copy(
                amount = amountPaise,
                accountId = accountId,
                toAccountId = if (existing.type == TransactionType.TRANSFER) toAccountId else existing.toAccountId,
                categoryId = categoryId,
                date = dateMs,
                merchantOrPayee = merchantOrPayee.trim(),
                description = description.trim(),
                notes = notes.trim(),
            )
            editTransactionUseCase(updated)
                .onSuccess {
                    _error.value = null
                    onSuccess()
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteTransaction(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteTransactionUseCase(id).onSuccess { onDeleted() }
        }
    }

    suspend fun restoreTransactionDirect(id: Long) {
        deleteTransactionUseCase.restore(id)
    }

    fun saveQuickTemplate(template: QuickTransactionTemplate) {
        viewModelScope.launch {
            userPreferencesRepository.saveQuickTemplate(template)
        }
    }

    fun deleteQuickTemplate(templateId: String) {
        viewModelScope.launch {
            userPreferencesRepository.deleteQuickTemplate(templateId)
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        const val TOP_CATEGORY_LIMIT = 6
        const val RECENT_CATEGORY_POOL_SIZE = 200

        fun resolveDefaultAccountId(
            accounts: List<AccountWithBalance>,
            preferredId: Long?,
        ): Long? {
            if (preferredId != null && accounts.any { it.account.id == preferredId }) {
                return preferredId
            }
            return accounts.firstOrNull()?.account?.id
        }

        /**
         * Returns up to [limit] category IDs ordered by recent transaction frequency (then recency,
         * then default category order when counts tie or no matching transactions exist).
         */
        fun computeTopCategoryIds(
            transactions: List<TransactionEntity>,
            categories: List<CategoryEntity>,
            targetType: TransactionType,
            limit: Int = TOP_CATEGORY_LIMIT,
            recentPoolSize: Int = RECENT_CATEGORY_POOL_SIZE,
        ): List<Long> {
            val activeCategories = categories.filter { !it.isDeleted }
            if (activeCategories.isEmpty() || limit <= 0) return emptyList()

            val defaultOrderIndex = activeCategories.withIndex().associate { (index, cat) -> cat.id to index }
            val recentMatching = transactions
                .asSequence()
                .filter { !it.isDeleted && it.type == targetType && it.categoryId != null }
                .sortedByDescending { it.date }
                .take(recentPoolSize)
                .toList()

            val counts = mutableMapOf<Long, Int>()
            val latestDate = mutableMapOf<Long, Long>()
            for (tx in recentMatching) {
                val catId = tx.categoryId ?: continue
                if (catId !in defaultOrderIndex) continue
                counts[catId] = (counts[catId] ?: 0) + 1
                val prev = latestDate[catId] ?: Long.MIN_VALUE
                if (tx.date > prev) latestDate[catId] = tx.date
            }

            return activeCategories
                .sortedWith(
                    compareByDescending<CategoryEntity> { counts[it.id] ?: 0 }
                        .thenByDescending { latestDate[it.id] ?: Long.MIN_VALUE }
                        .thenBy { defaultOrderIndex[it.id] ?: Int.MAX_VALUE }
                )
                .take(limit)
                .map { it.id }
        }

        private class MerchantAggregate(
            var totalCount: Int = 0,
            var latestDate: Long = Long.MIN_VALUE,
            var displayLabel: String = "",
            val categoryCounts: MutableMap<Long, Int> = mutableMapOf(),
            val categoryLatestDate: MutableMap<Long, Long> = mutableMapOf(),
        )

        private fun buildMerchantIndex(
            transactions: List<TransactionEntity>,
            categories: List<CategoryEntity>,
            targetType: TransactionType,
        ): Pair<List<String>, Map<String, Long>> {
            val activeCategoryIds = categories.filter { !it.isDeleted }.mapTo(mutableSetOf()) { it.id }
            val aggregates = mutableMapOf<String, MerchantAggregate>()

            for (tx in transactions) {
                if (tx.isDeleted || tx.type != targetType) continue
                val trimmed = tx.merchantOrPayee.trim()
                if (trimmed.isEmpty()) continue

                val key = trimmed.lowercase()
                val agg = aggregates.getOrPut(key) { MerchantAggregate() }
                agg.totalCount += 1
                if (tx.date >= agg.latestDate) {
                    agg.latestDate = tx.date
                    agg.displayLabel = trimmed
                }

                val catId = tx.categoryId
                if (catId != null && catId in activeCategoryIds) {
                    agg.categoryCounts[catId] = (agg.categoryCounts[catId] ?: 0) + 1
                    val prevCatDate = agg.categoryLatestDate[catId] ?: Long.MIN_VALUE
                    if (tx.date > prevCatDate) agg.categoryLatestDate[catId] = tx.date
                }
            }

            val topMerchants = aggregates.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, MerchantAggregate>> { it.value.totalCount }
                        .thenByDescending { it.value.latestDate }
                        .thenBy { it.value.displayLabel.lowercase() }
                )
                .map { it.value.displayLabel }

            val categoryMap = mutableMapOf<String, Long>()
            for ((key, agg) in aggregates) {
                val bestCatId = agg.categoryCounts.entries
                    .sortedWith(
                        compareByDescending<Map.Entry<Long, Int>> { it.value }
                            .thenByDescending { agg.categoryLatestDate[it.key] ?: Long.MIN_VALUE }
                    )
                    .firstOrNull()
                    ?.key
                if (bestCatId != null) {
                    categoryMap[key] = bestCatId
                }
            }
            return topMerchants to categoryMap
        }

        fun computeMerchantHistoryStats(
            transactions: List<TransactionEntity>,
            expenseCategories: List<CategoryEntity>,
            incomeCategories: List<CategoryEntity>,
        ): MerchantHistoryStats {
            val (topMerchants, expenseCatsByMerchant) = buildMerchantIndex(
                transactions = transactions,
                categories = expenseCategories,
                targetType = TransactionType.EXPENSE,
            )
            val (topSources, incomeCatsBySource) = buildMerchantIndex(
                transactions = transactions,
                categories = incomeCategories,
                targetType = TransactionType.INCOME,
            )
            return MerchantHistoryStats(
                topExpenseMerchants = topMerchants,
                topIncomeSources = topSources,
                expenseCategoryByMerchant = expenseCatsByMerchant,
                incomeCategoryBySource = incomeCatsBySource,
            )
        }

        fun filterMerchantSuggestions(
            merchants: List<String>,
            query: String,
            limit: Int = 5,
        ): List<String> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return merchants.take(limit)

            val nonExact = merchants.filter {
                it.contains(trimmed, ignoreCase = true) && !it.equals(trimmed, ignoreCase = true)
            }
            val (prefixMatches, substringMatches) = nonExact.partition {
                it.startsWith(trimmed, ignoreCase = true)
            }
            return (prefixMatches + substringMatches).take(limit)
        }

        fun suggestCategoryForMerchant(
            merchantInput: String,
            categoryMap: Map<String, Long>,
        ): Long? {
            val key = merchantInput.trim().lowercase()
            if (key.isEmpty()) return null
            return categoryMap[key]
        }

        private val REPEATABLE_TYPES = setOf(
            TransactionType.EXPENSE,
            TransactionType.REFUND,
            TransactionType.INCOME,
            TransactionType.TRANSFER,
        )

        fun findLastRepeatableTransaction(
            transactions: List<TransactionWithDetails>,
        ): TransactionWithDetails? =
            transactions
                .asSequence()
                .filter { !it.transaction.isDeleted && it.transaction.type in REPEATABLE_TYPES }
                .maxWithOrNull(
                    compareBy<TransactionWithDetails> { it.transaction.date }
                        .thenBy { it.transaction.createdAt }
                        .thenBy { it.transaction.id }
                )
    }
}
