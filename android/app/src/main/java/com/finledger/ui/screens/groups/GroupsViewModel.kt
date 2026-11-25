package com.finledger.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.*
import com.finledger.data.repository.SocialRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.usecase.CreateGroupUseCase
import com.finledger.domain.usecase.SimplifySettlementsUseCase
import com.finledger.domain.usecase.SplitCalculator
import com.finledger.domain.usecase.SuggestedSettlement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class GroupWithStats(
    val group: GroupEntity,
    val memberIds: List<Long>,
    val totalSpentPaise: Long,
)

data class GroupsUiState(
    val groups: List<GroupWithStats> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

data class GroupDetailUiState(
    val group: GroupEntity? = null,
    val members: List<PersonEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val expenses: List<SharedExpenseEntity> = emptyList(),
    val payersByExpense: Map<Long, List<ExpensePayerEntity>> = emptyMap(),
    val splitsByExpense: Map<Long, List<ExpenseSplitEntity>> = emptyMap(),
    val memberNets: Map<Long, Long> = emptyMap(),
    val suggestions: List<SuggestedSettlement> = emptyList(),
    val totalSpent: Long = 0L,
    val primaryCurrency: String = "INR",
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val createGroupUseCase: CreateGroupUseCase,
    private val simplifySettlementsUseCase: SimplifySettlementsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val detailFlows = ConcurrentHashMap<Long, StateFlow<GroupDetailUiState>>()

    val uiState: StateFlow<GroupsUiState> = combine<Any?, GroupsUiState>(
        socialRepository.observeActiveGroups(),
        socialRepository.observeAllGroupMembers(),
        socialRepository.observeAllSharedExpenses(),
        socialRepository.observeActivePersons(),
        userPreferencesRepository.preferences,
        _error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val groups = values[0] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val members = values[1] as List<GroupMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[2] as List<SharedExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val persons = values[3] as List<PersonEntity>
        val prefs = values[4] as com.finledger.domain.model.UserPreferences
        val err = values[5] as String?

        val membersByGroup = members.groupBy { it.groupId }
        val expByGroup = expenses.groupBy { it.groupId }

        GroupsUiState(
            groups = groups.map { g ->
                GroupWithStats(
                    group = g,
                    memberIds = membersByGroup[g.id].orEmpty().map { it.personId },
                    totalSpentPaise = expByGroup[g.id].orEmpty().sumOf { it.totalAmount },
                )
            },
            persons = persons,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    fun observeGroupDetail(groupId: Long): StateFlow<GroupDetailUiState> = detailFlows.getOrPut(groupId) {
        combine<Any?, GroupDetailUiState>(
            socialRepository.observeGroup(groupId),
            socialRepository.observeGroupMembers(groupId),
            socialRepository.observeActivePersons(),
            socialRepository.observeEventsByGroup(groupId),
            socialRepository.observeSharedExpensesByGroup(groupId),
            socialRepository.observeAllPayers(),
            socialRepository.observeAllSplits(),
            socialRepository.observeAllSettlements(),
            userPreferencesRepository.preferences,
        ) { values ->
            val group = values[0] as GroupEntity?
            @Suppress("UNCHECKED_CAST")
            val groupMembers = values[1] as List<GroupMemberEntity>
            @Suppress("UNCHECKED_CAST")
            val allPersons = values[2] as List<PersonEntity>
            @Suppress("UNCHECKED_CAST")
            val events = values[3] as List<EventEntity>
            @Suppress("UNCHECKED_CAST")
            val expenses = values[4] as List<SharedExpenseEntity>
            @Suppress("UNCHECKED_CAST")
            val allPayers = values[5] as List<ExpensePayerEntity>
            @Suppress("UNCHECKED_CAST")
            val allSplits = values[6] as List<ExpenseSplitEntity>
            @Suppress("UNCHECKED_CAST")
            val allSettlements = values[7] as List<SettlementEntity>
            val prefs = values[8] as com.finledger.domain.model.UserPreferences

            val memberIdSet = groupMembers.map { it.personId }.toSet()
            val memberPersons = allPersons.filter { it.id in memberIdSet }
            val payersByExp = allPayers.groupBy { it.sharedExpenseId }
            val splitsByExp = allSplits.groupBy { it.sharedExpenseId }
            val groupSettlements = allSettlements.filter { it.groupId == groupId }

            val netMap = simplifySettlementsUseCase.computeNetMap(expenses, payersByExp, splitsByExp, groupSettlements)
            val suggestions = com.finledger.domain.usecase.BalanceCalculator.simplifyDebts(netMap)

            GroupDetailUiState(
                group = group,
                members = memberPersons,
                events = events,
                expenses = expenses,
                payersByExpense = payersByExp,
                splitsByExpense = splitsByExp,
                memberNets = netMap,
                suggestions = suggestions,
                totalSpent = expenses.sumOf { it.totalAmount },
                primaryCurrency = group?.currency ?: prefs.primaryCurrency,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())
    }

    fun saveGroup(
        existing: GroupEntity?,
        name: String,
        description: String,
        currency: String,
        spendingLimitPaise: Long?,
        memberPersonIds: List<Long>,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            createGroupUseCase(
                existing = existing,
                name = name,
                description = description,
                currency = currency,
                spendingLimitPaise = spendingLimitPaise,
                memberPersonIds = memberPersonIds,
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun archiveGroup(groupId: Long) {
        viewModelScope.launch {
            socialRepository.updateGroupStatus(groupId, GroupStatus.ARCHIVED)
        }
    }

    fun deleteSharedExpense(expenseId: Long) {
        viewModelScope.launch {
            socialRepository.deleteSharedExpense(expenseId)
        }
    }

    suspend fun restoreSharedExpenseDirect(expenseId: Long) {
        socialRepository.restoreSharedExpense(expenseId)
    }

    fun personName(id: Long, members: List<PersonEntity>): String {
        if (id == SplitCalculator.USER_PERSON_ID) return "You"
        return members.firstOrNull { it.id == id }?.name ?: "Person #$id"
    }
}
