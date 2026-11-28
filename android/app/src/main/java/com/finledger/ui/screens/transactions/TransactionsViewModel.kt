package com.finledger.ui.screens.transactions

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.AuditLogEntity
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.DocumentEntity
import com.finledger.data.local.entity.DocumentType
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
import com.finledger.domain.usecase.GetMonthlySummaryUseCase
import com.finledger.domain.usecase.GetTransactionsUseCase
import com.finledger.domain.usecase.SaveDocumentUseCase
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
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

data class YearMonthKey(
    val year: Int,
    val monthZeroBased: Int,
)

data class FilteredTransactionSummary(
    val count: Int = 0,
    val spentPaise: Long = 0L,
    val incomePaise: Long = 0L,
    val transferPaise: Long = 0L,
    val netTotalPaise: Long = 0L,
)

data class TransactionsUiState(
    val groupedTransactions: Map<String, List<TransactionWithDetails>> = emptyMap(),
    val filteredCount: Int = 0,
    val filteredNetTotalPaise: Long = 0L,
    val filteredSpentPaise: Long = 0L,
    val filteredIncomePaise: Long = 0L,
    val filteredTransferPaise: Long = 0L,
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
    val vaultDocuments: List<DocumentEntity> = emptyList(),
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
    private val saveDocumentUseCase: SaveDocumentUseCase,
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
        saveDocumentUseCase.observeAll(),
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
        @Suppress("UNCHECKED_CAST")
        val docs = values[6] as List<DocumentEntity>
        val prefs = values[7] as com.finledger.domain.model.UserPreferences
        val error = values[8] as String?

        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
        val incomeCats = categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH }
        val summary = computeFilteredSummary(txs)

        TransactionsUiState(
            groupedTransactions = txs.groupBy { DateFormatter.formatDateHeader(it.transaction.date) },
            filteredCount = summary.count,
            filteredNetTotalPaise = summary.netTotalPaise,
            filteredSpentPaise = summary.spentPaise,
            filteredIncomePaise = summary.incomePaise,
            filteredTransferPaise = summary.transferPaise,
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
            vaultDocuments = docs,
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

    fun selectCurrentMonth() {
        val current = currentYearMonth()
        val (startMs, endMs) = monthBoundsFor(current)
        _filter.update { it.copy(startDateMs = startMs, endDateMs = endMs) }
    }

    fun stepMonth(deltaMonths: Int) {
        val base = resolveYearMonthFromBounds(_filter.value.startDateMs, _filter.value.endDateMs)
            ?: currentYearMonth()
        val target = shiftMonth(base, deltaMonths)
        val (startMs, endMs) = monthBoundsFor(target)
        _filter.update { it.copy(startDateMs = startMs, endDateMs = endMs) }
    }

    fun clearDateRange() {
        _filter.update { it.copy(startDateMs = null, endDateMs = null) }
    }

    fun toggleCurrentMonthFilter() {
        val current = currentYearMonth()
        val active = resolveYearMonthFromBounds(_filter.value.startDateMs, _filter.value.endDateMs)
        if (active == current) {
            clearDateRange()
        } else {
            selectCurrentMonth()
        }
    }

    fun toggleTypeFilter(type: TransactionType) {
        _filter.update { current ->
            current.copy(type = if (current.type == type) null else type)
        }
    }

    fun toggleSharedFilter() {
        _filter.update { current ->
            current.copy(onlyShared = !current.onlyShared)
        }
    }

    private val txFlows = ConcurrentHashMap<Long, StateFlow<TransactionEntity?>>()
    private val auditFlows = ConcurrentHashMap<Long, StateFlow<List<AuditLogEntity>>>()
    private val receiptFlows = ConcurrentHashMap<Long, StateFlow<List<DocumentEntity>>>()

    fun observeTransaction(id: Long): StateFlow<TransactionEntity?> = txFlows.getOrPut(id) {
        transactionRepository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    fun observeAuditHistory(id: Long): StateFlow<List<AuditLogEntity>> = auditFlows.getOrPut(id) {
        auditLogRepository.observeForEntity("transaction", id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun observeTransactionReceipts(id: Long): StateFlow<List<DocumentEntity>> = receiptFlows.getOrPut(id) {
        saveDocumentUseCase.observeForTransaction(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun attachReceiptUri(transactionId: Long, uri: Uri, name: String = "", type: DocumentType = DocumentType.RECEIPT) {
        viewModelScope.launch {
            saveDocumentUseCase.linkExistingOrSaveNew(
                transactionId = transactionId,
                uri = uri,
                name = name,
                type = type,
            ).onFailure { _error.value = it.message }
        }
    }

    fun attachExistingVaultDocument(transactionId: Long, documentId: Long) {
        viewModelScope.launch {
            saveDocumentUseCase.linkExistingOrSaveNew(
                transactionId = transactionId,
                existingDocumentId = documentId,
            ).onFailure { _error.value = it.message }
        }
    }

    fun detachReceipt(documentId: Long) {
        viewModelScope.launch { saveDocumentUseCase.unlink(documentId) }
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
        pendingReceiptUri: Uri? = null,
        pendingVaultDocumentId: Long? = null,
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
            ).onSuccess { txId ->
                if (pendingReceiptUri != null || pendingVaultDocumentId != null) {
                    saveDocumentUseCase.linkExistingOrSaveNew(
                        transactionId = txId,
                        existingDocumentId = pendingVaultDocumentId,
                        uri = pendingReceiptUri,
                        name = merchant.ifBlank { description },
                    )
                }
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
        pendingReceiptUri: Uri? = null,
        pendingVaultDocumentId: Long? = null,
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
            ).onSuccess { txId ->
                if (pendingReceiptUri != null || pendingVaultDocumentId != null) {
                    saveDocumentUseCase.linkExistingOrSaveNew(
                        transactionId = txId,
                        existingDocumentId = pendingVaultDocumentId,
                        uri = pendingReceiptUri,
                        name = source.ifBlank { description },
                    )
                }
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
        pendingReceiptUri: Uri? = null,
        pendingVaultDocumentId: Long? = null,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            addTransferUseCase(
                amountPaise = amountPaise,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                dateMs = dateMs,
                notes = notes,
            ).onSuccess { txId ->
                if (pendingReceiptUri != null || pendingVaultDocumentId != null) {
                    saveDocumentUseCase.linkExistingOrSaveNew(
                        transactionId = txId,
                        existingDocumentId = pendingVaultDocumentId,
                        uri = pendingReceiptUri,
                        name = "Transfer Receipt #$txId",
                    )
                }
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

    fun bulkDelete(ids: Set<Long>, onDeleted: (List<Long>) -> Unit) {
        val targetIds = ids.toList()
        if (targetIds.isEmpty()) return
        viewModelScope.launch {
            val deleted = executeBulkDelete(targetIds, deleteTransactionUseCase)
            if (deleted.isNotEmpty()) {
                onDeleted(deleted)
            }
        }
    }

    suspend fun bulkRestoreDirect(ids: List<Long>) {
        executeBulkRestore(ids, deleteTransactionUseCase)
    }

    fun bulkUpdateCategory(ids: Set<Long>, categoryId: Long, onDone: () -> Unit = {}) {
        val targetIds = ids.toList()
        if (targetIds.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            executeBulkUpdateCategory(targetIds, categoryId, transactionRepository)
            onDone()
        }
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

        fun computeFilteredSummary(
            transactions: List<TransactionWithDetails>,
        ): FilteredTransactionSummary {
            var spent = 0L
            var income = 0L
            var transfer = 0L
            var net = 0L

            for (item in transactions) {
                val tx = item.transaction
                when (tx.type) {
                    TransactionType.EXPENSE -> {
                        spent += tx.amount
                        net -= tx.amount
                    }
                    TransactionType.INCOME -> {
                        income += tx.amount
                        net += tx.amount
                    }
                    TransactionType.REFUND -> {
                        net += tx.amount
                    }
                    TransactionType.TRANSFER -> {
                        transfer += tx.amount
                    }
                    TransactionType.LENDING,
                    TransactionType.REPAYMENT_MADE,
                    TransactionType.INVESTMENT_BUY -> {
                        spent += tx.amount
                        net -= tx.amount
                    }
                    TransactionType.BORROWING,
                    TransactionType.REPAYMENT_RECEIVED,
                    TransactionType.INVESTMENT_SELL -> {
                        income += tx.amount
                        net += tx.amount
                    }
                    TransactionType.ACCOUNT_ADJUSTMENT -> {
                        net += tx.amount * tx.adjustmentSign
                    }
                }
            }

            return FilteredTransactionSummary(
                count = transactions.size,
                spentPaise = spent,
                incomePaise = income,
                transferPaise = transfer,
                netTotalPaise = net,
            )
        }

        fun currentYearMonth(nowMs: Long = System.currentTimeMillis()): YearMonthKey {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            return YearMonthKey(
                year = cal.get(Calendar.YEAR),
                monthZeroBased = cal.get(Calendar.MONTH),
            )
        }

        fun shiftMonth(base: YearMonthKey, deltaMonths: Int): YearMonthKey {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.YEAR, base.year)
                set(Calendar.MONTH, base.monthZeroBased)
                add(Calendar.MONTH, deltaMonths)
            }
            return YearMonthKey(
                year = cal.get(Calendar.YEAR),
                monthZeroBased = cal.get(Calendar.MONTH),
            )
        }

        fun monthBoundsFor(key: YearMonthKey): Pair<Long, Long> =
            GetMonthlySummaryUseCase.monthBounds(year = key.year, monthZeroBased = key.monthZeroBased)

        fun resolveYearMonthFromBounds(startDateMs: Long?, endDateMs: Long?): YearMonthKey? {
            if (startDateMs == null || endDateMs == null) return null
            val cal = Calendar.getInstance().apply { timeInMillis = startDateMs }
            val candidate = YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            val (expectedStart, expectedEnd) = monthBoundsFor(candidate)
            return if (startDateMs == expectedStart && endDateMs == expectedEnd) candidate else null
        }

        fun formatFilteredSummaryText(state: TransactionsUiState): String? {
            if (state.filteredCount <= 0) return null

            val filter = state.filter
            val labels = buildList {
                filter.categoryId?.let { cid ->
                    state.allCategories.firstOrNull { it.id == cid }?.name?.let { add(it) }
                }
                filter.accountId?.let { aid ->
                    state.accounts.firstOrNull { it.account.id == aid }?.account?.name?.let { add(it) }
                }
                filter.type?.let { add(it.displayName) }
                if (filter.onlyShared) add("Shared")
            }
            val activeLabel = if (labels.isNotEmpty()) {
                labels.joinToString(" · ")
            } else {
                val monthStart = if (resolveYearMonthFromBounds(filter.startDateMs, filter.endDateMs) != null) {
                    filter.startDateMs
                } else {
                    null
                }
                monthStart?.let { DateFormatter.formatMonthYear(it) } ?: "All"
            }

            val currency = state.primaryCurrency
            val amountLabel = when {
                filter.type == TransactionType.EXPENSE -> MoneyFormatter.format(state.filteredSpentPaise, currency)
                filter.type == TransactionType.INCOME -> MoneyFormatter.format(state.filteredIncomePaise, currency)
                filter.type == TransactionType.TRANSFER -> MoneyFormatter.format(state.filteredTransferPaise, currency)
                state.filteredSpentPaise > 0L && state.filteredIncomePaise == 0L && state.filteredTransferPaise == 0L ->
                    MoneyFormatter.format(state.filteredSpentPaise, currency)
                state.filteredIncomePaise > 0L && state.filteredSpentPaise == 0L && state.filteredTransferPaise == 0L ->
                    MoneyFormatter.format(state.filteredIncomePaise, currency)
                state.filteredTransferPaise > 0L && state.filteredSpentPaise == 0L && state.filteredIncomePaise == 0L ->
                    MoneyFormatter.format(state.filteredTransferPaise, currency)
                else -> "Net ${MoneyFormatter.format(state.filteredNetTotalPaise, currency, showPlus = true)}"
            }

            val countLabel = if (state.filteredCount == 1) "1 item" else "${state.filteredCount} items"
            return "$activeLabel · $amountLabel · $countLabel"
        }

        private val CATEGORY_ELIGIBLE_TYPES = setOf(
            TransactionType.EXPENSE,
            TransactionType.REFUND,
            TransactionType.INCOME,
        )

        suspend fun executeBulkDelete(
            ids: List<Long>,
            deleteUseCase: DeleteTransactionUseCase,
        ): List<Long> {
            val deleted = mutableListOf<Long>()
            for (id in ids.distinct()) {
                if (deleteUseCase(id).isSuccess) {
                    deleted += id
                }
            }
            return deleted
        }

        suspend fun executeBulkRestore(
            ids: List<Long>,
            deleteUseCase: DeleteTransactionUseCase,
        ): Int {
            var restored = 0
            for (id in ids.distinct()) {
                if (deleteUseCase.restore(id).isSuccess) {
                    restored += 1
                }
            }
            return restored
        }

        suspend fun executeBulkUpdateCategory(
            ids: List<Long>,
            categoryId: Long,
            transactionRepository: TransactionRepository,
        ): Int {
            var updatedCount = 0
            for (id in ids.distinct()) {
                val existing = transactionRepository.getById(id) ?: continue
                if (existing.isDeleted || existing.type !in CATEGORY_ELIGIBLE_TYPES) continue
                if (existing.categoryId == categoryId) continue
                transactionRepository.update(existing, existing.copy(categoryId = categoryId))
                updatedCount += 1
            }
            return updatedCount
        }
    }
}
