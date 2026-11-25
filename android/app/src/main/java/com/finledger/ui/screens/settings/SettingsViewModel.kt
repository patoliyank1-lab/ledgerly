package com.finledger.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.CategoryEntity
import com.finledger.data.local.entity.CategoryType
import com.finledger.data.local.entity.TagEntity
import com.finledger.data.repository.CategoryRepository
import com.finledger.data.repository.TagRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.ThemeMode
import com.finledger.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferences,
        categoryRepository.observeAll(),
        tagRepository.observeAll(),
    ) { prefs, categories, tags ->
        SettingsUiState(
            preferences = prefs,
            categories = categories,
            tags = tags,
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
        viewModelScope.launch { userPreferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setAutoBackupEnabled(enabled) }
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
}

