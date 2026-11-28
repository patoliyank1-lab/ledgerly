package com.finledger.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.TagEntity
import com.finledger.data.repository.AccountRepository
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.TagRepository
import com.finledger.data.repository.TransactionRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.AccountWithBalance
import com.finledger.domain.model.ThemeMode
import com.finledger.domain.model.TransactionWithDetails
import com.finledger.domain.model.UserPreferences
import com.finledger.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val hasExistingFinancialData: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferences,
        categoryRepository.observeAll(),
        tagRepository.observeAll(),
        transactionRepository.observeAllTransactionsWithDetails(),
        accountRepository.observeActiveAccountsWithBalances(),
    ) { prefs, categories, tags, transactions, accounts ->
        SettingsUiState(
            preferences = prefs,
            categories = categories,
            tags = tags,
            hasExistingFinancialData = hasExistingFinancialData(transactions, accounts),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateUserName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { userPreferencesRepository.setUserName(name) }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch { userPreferencesRepository.setPrimaryCurrency(currency) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
    }

    fun updateFinancialYearStart(month: Int) {
        viewModelScope.launch { userPreferencesRepository.setFinancialYearStartMonth(month) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
            ReminderScheduler.syncWithPreferences(context, userPreferencesRepository.preferences.first())
        }
    }

    fun setBillRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBillRemindersEnabled(enabled)
            ReminderScheduler.syncWithPreferences(context, userPreferencesRepository.preferences.first())
        }
    }

    fun setLoanRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setLoanRemindersEnabled(enabled)
            ReminderScheduler.syncWithPreferences(context, userPreferencesRepository.preferences.first())
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetAlertsEnabled(enabled)
            ReminderScheduler.syncWithPreferences(context, userPreferencesRepository.preferences.first())
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(enabled)
            ReminderScheduler.syncWithPreferences(context, userPreferencesRepository.preferences.first())
        }
    }

    fun setSimpleModePreset(simpleMode: Boolean) {
        viewModelScope.launch {
            if (simpleMode) {
                userPreferencesRepository.applySimplePreset()
            } else {
                userPreferencesRepository.applyAdvancedPreset()
            }
        }
    }

    fun setTrackSharedAndPeople(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setTrackSharedAndPeople(enabled) }
    }

    fun setTrackLoans(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setTrackLoans(enabled) }
    }

    fun setTrackInvestments(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setTrackInvestments(enabled) }
    }

    fun setTrackPlanning(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setTrackPlanning(enabled) }
    }

    fun setShowDashboardNetWorth(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowDashboardNetWorth(enabled) }
    }

    fun setShowDashboardAlerts(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowDashboardAlerts(enabled) }
    }

    fun addCategory(name: String, type: CategoryType, iconName: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.insert(
                CategoryEntity(
                    name = name.trim(),
                    iconName = iconName,
                    colorHex = colorHex,
                    type = type,
                    isDefault = false,
                )
            )
        }
    }

    fun deleteCustomCategory(id: Long) {
        viewModelScope.launch { categoryRepository.softDeleteCustom(id) }
    }

    fun addTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            tagRepository.insert(TagEntity(name = name.trim()))
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch { tagRepository.softDelete(id) }
    }

    companion object {
        fun hasExistingFinancialData(
            transactions: List<TransactionWithDetails>,
            accounts: List<AccountWithBalance>,
        ): Boolean = transactions.any { !it.transaction.isDeleted } ||
            accounts.any { it.currentBalance != 0L || it.account.openingBalance != 0L }
    }
}

