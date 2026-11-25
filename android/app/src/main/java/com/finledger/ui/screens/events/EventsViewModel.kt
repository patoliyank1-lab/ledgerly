package com.finledger.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.*
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventWithMeta(
    val event: EventEntity,
    val groupName: String?,
    val participantCount: Int,
    val totalSpentPaise: Long,
)

data class EventsUiState(
    val events: List<EventWithMeta> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

data class EventDetailUiState(
    val event: EventEntity? = null,
    val groupName: String? = null,
    val participants: List<PersonEntity> = emptyList(),
    val expenses: List<SharedExpenseEntity> = emptyList(),
    val totalSpentPaise: Long = 0L,
    val userPersonalSpendPaise: Long = 0L,
    val memberNets: Map<Long, Long> = emptyMap(),
    val suggestions: List<SuggestedSettlement> = emptyList(),
    val primaryCurrency: String = "INR",
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val createEventUseCase: CreateEventUseCase,
    private val addMultiPayerExpenseUseCase: AddMultiPayerExpenseUseCase,
    private val simplifySettlementsUseCase: SimplifySettlementsUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    categoryRepository: CategoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EventsUiState> = combine<Any?, EventsUiState>(
        socialRepository.observeAllEvents(),
        socialRepository.observeActiveGroups(),
        socialRepository.observeAllEventParticipants(),
        socialRepository.observeAllSharedExpenses(),
        socialRepository.observeActivePersons(),
        getAccountsUseCase(),
        categoryRepository.observeByType(CategoryType.EXPENSE),
        userPreferencesRepository.preferences,
        _error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val events = values[0] as List<EventEntity>
        @Suppress("UNCHECKED_CAST")
        val groups = values[1] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val participants = values[2] as List<EventParticipantEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[3] as List<SharedExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val persons = values[4] as List<PersonEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[5] as List<AccountWithBalance>
        @Suppress("UNCHECKED_CAST")
        val categories = values[6] as List<CategoryEntity>
        val prefs = values[7] as com.finledger.domain.model.UserPreferences
        val err = values[8] as String?

        val groupMap = groups.associateBy { it.id }
        val partByEvent = participants.groupBy { it.eventId }
        val expByEvent = expenses.groupBy { it.eventId }

        EventsUiState(
            events = events.map { ev ->
                EventWithMeta(
                    event = ev,
                    groupName = ev.groupId?.let { groupMap[it]?.name },
                    participantCount = partByEvent[ev.id].orEmpty().size + 1,
                    totalSpentPaise = expByEvent[ev.id].orEmpty().sumOf { it.totalAmount },
                )
            },
            groups = groups,
            persons = persons,
            accounts = accounts,
            categories = categories,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    fun observeEventDetail(eventId: Long): StateFlow<EventDetailUiState> = combine<Any?, EventDetailUiState>(
        socialRepository.observeEvent(eventId),
        socialRepository.observeEventParticipants(eventId),
        socialRepository.observeActivePersons(),
        socialRepository.observeActiveGroups(),
        socialRepository.observeSharedExpensesByEvent(eventId),
        socialRepository.observeAllPayers(),
        socialRepository.observeAllSplits(),
        socialRepository.observeAllSettlements(),
        userPreferencesRepository.preferences,
    ) { values ->
        val ev = values[0] as EventEntity?
        @Suppress("UNCHECKED_CAST")
        val evParticipants = values[1] as List<EventParticipantEntity>
        @Suppress("UNCHECKED_CAST")
        val allPersons = values[2] as List<PersonEntity>
        @Suppress("UNCHECKED_CAST")
        val groups = values[3] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[4] as List<SharedExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val allPayers = values[5] as List<ExpensePayerEntity>
        @Suppress("UNCHECKED_CAST")
        val allSplits = values[6] as List<ExpenseSplitEntity>
        @Suppress("UNCHECKED_CAST")
        val allSettlements = values[7] as List<SettlementEntity>
        val prefs = values[8] as com.finledger.domain.model.UserPreferences

        val partSet = evParticipants.map { it.personId }.toSet()
        val participants = allPersons.filter { it.id in partSet }
        val payersByExp = allPayers.groupBy { it.sharedExpenseId }
        val splitsByExp = allSplits.groupBy { it.sharedExpenseId }
        val evSettlements = allSettlements.filter { it.eventId == eventId }

        val netMap = mutableMapOf<Long, Long>()
        var userPersonal = 0L
        for (exp in expenses) {
            payersByExp[exp.id].orEmpty().forEach { p ->
                netMap[p.personId] = (netMap[p.personId] ?: 0L) + p.amountPaid
            }
            splitsByExp[exp.id].orEmpty().filter { !it.isExcluded }.forEach { s ->
                netMap[s.personId] = (netMap[s.personId] ?: 0L) - s.shareAmount
                if (s.personId == SplitCalculator.USER_PERSON_ID) userPersonal += s.shareAmount
            }
        }

        EventDetailUiState(
            event = ev,
            groupName = ev?.groupId?.let { gid -> groups.firstOrNull { it.id == gid }?.name },
            participants = participants,
            expenses = expenses,
            totalSpentPaise = expenses.sumOf { it.totalAmount },
            userPersonalSpendPaise = userPersonal,
            memberNets = netMap,
            suggestions = simplifySettlementsUseCase(expenses, payersByExp, splitsByExp, evSettlements),
            primaryCurrency = prefs.primaryCurrency,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState())

    fun saveEvent(
        existing: EventEntity?,
        name: String,
        description: String,
        location: String,
        groupId: Long?,
        startDateMs: Long,
        budgetPaise: Long?,
        participantIds: List<Long>,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            createEventUseCase(
                existing = existing,
                name = name,
                description = description,
                location = location,
                groupId = groupId,
                startDateMs = startDateMs,
                budgetAmountPaise = budgetPaise,
                participantPersonIds = participantIds,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun closeEvent(eventId: Long) {
        viewModelScope.launch {
            socialRepository.updateEventStatus(eventId, EventStatus.COMPLETED)
        }
    }

    fun saveSharedExpense(
        totalAmountPaise: Long,
        description: String,
        dateMs: Long,
        categoryId: Long?,
        groupId: Long?,
        eventId: Long?,
        splitMethod: SplitMethod,
        payers: List<ExpensePayerEntity>,
        userAccountIdIfUserPaid: Long?,
        splits: List<ExpenseSplitEntity>,
        items: List<SharedExpenseItemEntity>,
        notes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            addMultiPayerExpenseUseCase(
                totalAmountPaise = totalAmountPaise,
                description = description,
                dateMs = dateMs,
                categoryId = categoryId,
                groupId = groupId,
                eventId = eventId,
                splitMethod = splitMethod,
                payers = payers,
                userAccountIdIfUserPaid = userAccountIdIfUserPaid,
                splits = splits,
                items = items,
                notes = notes,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun personName(id: Long, list: List<PersonEntity>): String {
        if (id == SplitCalculator.USER_PERSON_ID) return "You"
        return list.firstOrNull { it.id == id }?.name ?: "Person #$id"
    }
}
