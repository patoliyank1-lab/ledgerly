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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val groupedTransactions: Map<String, List<TransactionWithDetails>> = emptyMap(),
    val filter: TransactionFilter = TransactionFilter(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val primaryCurrency: String = "INR",
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
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _error = MutableStateFlow<String?>(null)

    private val filteredTransactions = _filter.flatMapLatest { filter ->
        getTransactionsUseCase(filter)
    }

    val uiState: StateFlow<TransactionsUiState> = combine<Any?, TransactionsUiState>(
        filteredTransactions,
        _filter,
        getAccountsUseCase(),
        categoryRepository.observeAll(),
        tagRepository.observeAll(),
        userPreferencesRepository.preferences,
        _error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txs = values[0] as List<TransactionWithDetails>
        val filter = values[1] as TransactionFilter
        @Suppress("UNCHECKED_CAST")
        val accounts = values[2] as List<AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val tags = values[4] as List<TagEntity>
        val prefs = values[5] as com.finledger.domain.model.UserPreferences
        val error = values[6] as String?

        TransactionsUiState(
            groupedTransactions = txs.groupBy { DateFormatter.formatDateHeader(it.transaction.date) },
            filter = filter,
            accounts = accounts,
            expenseCategories = categories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH },
            incomeCategories = categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH },
            allCategories = categories,
            tags = tags,
            primaryCurrency = prefs.primaryCurrency,
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

    fun observeTransaction(id: Long): StateFlow<TransactionEntity?> =
        transactionRepository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observeAuditHistory(id: Long): StateFlow<List<AuditLogEntity>> =
        auditLogRepository.observeForEntity("transaction", id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun clearError() {
        _error.value = null
    }
}
