package com.finledger.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.BudgetEntity
import com.finledger.data.local.entity.BudgetPeriod
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.SavingsGoalEntity
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.CommitmentRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.usecase.BudgetProgress
import com.finledger.domain.usecase.CreateBudgetUseCase
import com.finledger.domain.usecase.CreateSavingsGoalUseCase
import com.finledger.domain.usecase.GetBudgetProgressUseCase
import com.finledger.domain.usecase.SuggestBudgetsFromHistoryUseCase
import com.finledger.domain.usecase.SuggestedBudget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetsUiState(
    val budgets: List<BudgetProgress> = emptyList(),
    val suggestedBudgets: List<SuggestedBudget> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    getBudgetProgressUseCase: GetBudgetProgressUseCase,
    suggestBudgetsFromHistoryUseCase: SuggestBudgetsFromHistoryUseCase,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val createSavingsGoalUseCase: CreateSavingsGoalUseCase,
    private val commitmentRepository: CommitmentRepository,
    categoryRepository: CategoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        combine(getBudgetProgressUseCase(), suggestBudgetsFromHistoryUseCase()) { budgets, suggestions ->
            budgets to suggestions
        },
        commitmentRepository.observeSavingsGoals(),
        categoryRepository.observeByType(CategoryType.EXPENSE),
        userPreferencesRepository.preferences,
        _error,
    ) { (budgets, suggestions), goals, categories, prefs, err ->
        BudgetsUiState(
            budgets = budgets,
            suggestedBudgets = suggestions,
            goals = goals,
            categories = categories,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun saveBudget(
        existing: BudgetEntity? = null,
        name: String,
        limitPaise: Long,
        period: BudgetPeriod,
        categoryId: Long?,
        alertThreshold: Int,
        isRollover: Boolean,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val entity = (existing ?: BudgetEntity(
                name = name,
                amount = limitPaise,
                period = period,
                startDate = System.currentTimeMillis(),
            )).copy(
                name = name,
                amount = limitPaise,
                period = period,
                categoryId = categoryId,
                alertThreshold = alertThreshold.coerceIn(1, 100),
                isRollover = isRollover,
            )
            createBudgetUseCase(entity).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun createSuggestedBudgets(selectedCategoryIds: Set<Long>, onCreated: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val snapshot = uiState.value
            val targets = snapshot.suggestedBudgets.filter { it.categoryId in selectedCategoryIds }
            if (targets.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            var createdCount = 0
            var firstError: String? = null
            for (suggestion in targets) {
                val entity = BudgetEntity(
                    name = suggestion.categoryName,
                    amount = suggestion.suggestedLimitPaise,
                    currency = snapshot.primaryCurrency,
                    period = BudgetPeriod.MONTHLY,
                    startDate = now,
                    categoryId = suggestion.categoryId,
                    alertThreshold = 80,
                    isRollover = false,
                )
                createBudgetUseCase(entity)
                    .onSuccess { createdCount++ }
                    .onFailure { if (firstError == null) firstError = it.message }
            }
            _error.value = firstError
            if (createdCount > 0) onCreated(createdCount)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch { commitmentRepository.deleteBudget(id) }
    }

    suspend fun restoreBudgetDirect(id: Long) {
        commitmentRepository.restoreBudget(id)
    }

    fun saveGoal(
        existing: SavingsGoalEntity? = null,
        name: String,
        description: String,
        targetPaise: Long,
        currentPaise: Long,
        targetDateMs: Long?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val entity = (existing ?: SavingsGoalEntity(
                name = name,
                targetAmount = targetPaise,
            )).copy(
                name = name,
                description = description.trim(),
                targetAmount = targetPaise,
                currentAmount = currentPaise,
                targetDate = targetDateMs,
            )
            createSavingsGoalUseCase(entity).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch { commitmentRepository.deleteSavingsGoal(id) }
    }

    suspend fun restoreGoalDirect(id: Long) {
        commitmentRepository.restoreSavingsGoal(id)
    }

    fun contributeToGoal(goalId: Long, deltaPaise: Long) {
        viewModelScope.launch {
            createSavingsGoalUseCase.contribute(goalId, deltaPaise)
                .onSuccess { _error.value = null }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

