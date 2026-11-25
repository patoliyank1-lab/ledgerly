package com.finledger.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.EventEntity
import com.finledger.data.local.entity.GroupEntity
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class FriendsUiState(
    val balances: List<PersonBalanceSummary> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
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
    private val personFlows = ConcurrentHashMap<Long, StateFlow<PersonEntity?>>()
    private val ledgerFlows = ConcurrentHashMap<Long, StateFlow<List<PersonLedgerEntry>>>()
    private val settlementFlows = ConcurrentHashMap<Long, StateFlow<List<SettlementEntity>>>()

    val uiState: StateFlow<FriendsUiState> = combine<Any?, FriendsUiState>(
        computePersonBalanceUseCase(),
        getAccountsUseCase(),
        socialRepository.observeActiveGroups(),
        socialRepository.observeAllEvents(),
        userPreferencesRepository.preferences,
        _error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val balances = values[0] as List<PersonBalanceSummary>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[1] as List<AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val groups = values[2] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val events = values[3] as List<EventEntity>
        val prefs = values[4] as com.finledger.domain.model.UserPreferences
        val err = values[5] as String?
        val receive = balances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise }
        val pay = balances.filter { it.netBalancePaise < 0 }.sumOf { -it.netBalancePaise }
        FriendsUiState(
            balances = balances,
            accounts = accounts,
            groups = groups,
            events = events,
            totalToReceive = receive,
            totalToPay = pay,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    fun observePerson(personId: Long): StateFlow<PersonEntity?> = personFlows.getOrPut(personId) {
        socialRepository.observePerson(personId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    fun observeLedger(personId: Long): StateFlow<List<PersonLedgerEntry>> = ledgerFlows.getOrPut(personId) {
        computePersonBalanceUseCase.observeLedgerForPerson(personId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun observeSettlements(personId: Long): StateFlow<List<SettlementEntity>> = settlementFlows.getOrPut(personId) {
        socialRepository.observeSettlementsByPerson(personId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

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
        groupId: Long? = null,
        eventId: Long? = null,
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
                groupId = groupId,
                eventId = eventId,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun archivePerson(personId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            socialRepository.archivePerson(personId, true)
            onDone()
        }
    }

    fun deletePerson(personId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            socialRepository.deletePerson(personId)
            onDone()
        }
    }

    suspend fun restorePersonDirect(personId: Long) {
        socialRepository.restorePerson(personId)
    }

    fun deleteSettlement(settlement: SettlementEntity) {
        viewModelScope.launch {
            socialRepository.deleteSettlement(settlement)
        }
    }

    suspend fun restoreSettlementDirect(settlement: SettlementEntity) {
        socialRepository.restoreSettlement(settlement)
    }
}

