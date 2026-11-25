package com.finledger.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AccountType
import com.finledger.data.local.entity.TransactionEntity
import com.finledger.data.local.entity.TransactionType
import com.finledger.data.repository.AccountRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountSummary
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.usecase.AddAccountUseCase
import com.finledger.domain.usecase.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val summary: AccountSummary = AccountSummary(0, 0, 0, 0, 0, 0, 0),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        getAccountsUseCase(),
        getAccountsUseCase.observeSummary(),
        userPreferencesRepository.preferences,
        _error,
    ) { accounts, summary, prefs, err ->
        AccountsUiState(
            accounts = accounts,
            summary = summary,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    private val accountFlows = ConcurrentHashMap<Long, StateFlow<AccountWithBalance?>>()
    private val accountTxFlows = ConcurrentHashMap<Long, StateFlow<List<TransactionWithDetails>>>()

    fun observeAccount(accountId: Long): StateFlow<AccountWithBalance?> = accountFlows.getOrPut(accountId) {
        accountRepository.observeAccountWithBalance(accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    fun observeTransactionsForAccount(accountId: Long): StateFlow<List<TransactionWithDetails>> = accountTxFlows.getOrPut(accountId) {
        transactionRepository.observeTransactionsByAccount(accountId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun saveAccount(
        existing: AccountEntity? = null,
        name: String,
        type: AccountType,
        bankOrProvider: String,
        maskedIdentifier: String,
        currency: String,
        openingBalancePaise: Long,
        creditLimitPaise: Long?,
        billingCycleDay: Int?,
        dueDay: Int?,
        notes: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val entity = (existing ?: AccountEntity(name = name, type = type)).copy(
                name = name,
                type = type,
                bankOrProvider = bankOrProvider.trim(),
                maskedIdentifier = maskedIdentifier.trim(),
                currency = currency,
                openingBalance = openingBalancePaise,
                creditLimit = if (type == AccountType.CREDIT_CARD) creditLimitPaise else null,
                billingCycleDay = if (type == AccountType.CREDIT_CARD) billingCycleDay else null,
                dueDay = if (type == AccountType.CREDIT_CARD) dueDay else null,
                notes = notes.trim(),
            )
            runCatching { addAccountUseCase(entity).getOrThrow() }
                .onSuccess {
                    _error.value = null
                    onSaved()
                }
                .onFailure { _error.value = it.message ?: "Failed to save account" }
        }
    }

    fun reconcileAccount(accountId: Long, currentBalance: Long, targetBalance: Long, notes: String) {
        val diff = targetBalance - currentBalance
        if (diff == 0L) return
        viewModelScope.launch {
            val tx = TransactionEntity(
                type = TransactionType.ACCOUNT_ADJUSTMENT,
                amount = kotlin.math.abs(diff),
                personalAmount = 0L,
                adjustmentSign = if (diff > 0) 1 else -1,
                date = System.currentTimeMillis(),
                accountId = accountId,
                description = "Balance Reconciliation",
                notes = notes.ifBlank { "Manual statement reconciliation" },
            )
            transactionRepository.insert(tx)
        }
    }

    fun archiveAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.setArchived(accountId, true)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.softDelete(accountId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}

