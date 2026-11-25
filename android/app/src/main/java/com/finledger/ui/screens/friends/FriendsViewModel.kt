package com.finledger.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.PersonEntity
import com.finledger.data.local.entity.SettlementDirection
import com.finledger.data.local.entity.SettlementEntity
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.usecase.AddPersonUseCase
import com.finledger.domain.usecase.ComputePersonBalanceUseCase
import com.finledger.domain.usecase.GetAccountsUseCase
import com.finledger.domain.usecase.PersonBalanceSummary
import com.finledger.domain.usecase.PersonLedgerEntry
import com.finledger.domain.usecase.RecordSettlementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val balances: List<PersonBalanceSummary> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalToReceive: Long = 0L,
    val totalToPay: Long = 0L,
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val computePersonBalanceUseCase: ComputePersonBalanceUseCase,
    private val addPersonUseCase: AddPersonUseCase,
    private val recordSettlementUseCase: RecordSettlementUseCase,
    private val socialRepository: SocialRepository,
    getAccountsUseCase: GetAccountsUseCase,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FriendsUiState> = combine(
        computePersonBalanceUseCase(),
        getAccountsUseCase(),
        userPreferencesRepository.preferences,
        _error,
    ) { balances, accounts, prefs, err ->
        val receive = balances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise }
        val pay = balances.filter { it.netBalancePaise < 0 }.sumOf { -it.netBalancePaise }
        FriendsUiState(
            balances = balances,
            accounts = accounts,
            totalToReceive = receive,
            totalToPay = pay,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    fun observePerson(personId: Long): StateFlow<PersonEntity?> =
        socialRepository.observePerson(personId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observeLedger(personId: Long): StateFlow<List<PersonLedgerEntry>> =
        computePersonBalanceUseCase.observeLedgerForPerson(personId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeSettlements(personId: Long): StateFlow<List<SettlementEntity>> =
        socialRepository.observeSettlementsByPerson(personId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun savePerson(
        existing: PersonEntity? = null,
        name: String,
        phone: String,
        email: String,
        relationship: String,
        notes: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            addPersonUseCase(existing, name, phone, email, relationship, notes)
                .onSuccess {
                    _error.value = null
                    onSaved()
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun recordSettlement(
        personId: Long,
        amountPaise: Long,
        direction: SettlementDirection,
        dateMs: Long,
        accountId: Long?,
        paymentMethod: String,
        referenceNumber: String,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            recordSettlementUseCase(
                personId = personId,
                amountPaise = amountPaise,
                direction = direction,
                dateMs = dateMs,
                accountId = accountId,
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber,
                notes = notes,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }
}

