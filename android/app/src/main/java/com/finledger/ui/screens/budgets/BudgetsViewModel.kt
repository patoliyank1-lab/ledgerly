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
    val goals: List<SavingsGoalEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val primaryCurrency: String = "INR",
    val errorMessage: String? = null,
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val createSavingsGoalUseCase: CreateSavingsGoalUseCase,
    private val commitmentRepository: CommitmentRepository,
    categoryRepository: CategoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        getBudgetProgressUseCase(),
        commitmentRepository.observeSavingsGoals(),
        categoryRepository.observeByType(CategoryType.EXPENSE),
        userPreferencesRepository.preferences,
        _error,
    ) { budgets, goals, categories, prefs, err ->
        BudgetsUiState(
            budgets = budgets,
            goals = goals,
            categories = categories,
            primaryCurrency = prefs.primaryCurrency,
            errorMessage = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun saveBudget(
        name: String,
        limitPaise: Long,
        period: BudgetPeriod,
        categoryId: Long?,
        alertThreshold: Int,
        isRollover: Boolean,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            createBudgetUseCase(
                BudgetEntity(
                    name = name,
                    amount = limitPaise,
                    period = period,
                    startDate = System.currentTimeMillis(),
                    categoryId = categoryId,
                    alertThreshold = alertThreshold,
                    isRollover = isRollover,
                )
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun saveGoal(
        name: String,
        description: String,
        targetPaise: Long,
        currentPaise: Long,
        targetDateMs: Long?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            createSavingsGoalUseCase(
                SavingsGoalEntity(
                    name = name,
                    description = description.trim(),
                    targetAmount = targetPaise,
                    currentAmount = currentPaise,
                    targetDate = targetDateMs,
                )
            ).onSuccess {
                _error.value = null
                onSuccess()
            }.onFailure { _error.value = it.message }
        }
    }

    fun contributeToGoal(goalId: Long, deltaPaise: Long) {
        viewModelScope.launch { createSavingsGoalUseCase.contribute(goalId, deltaPaise) }
    }
}

