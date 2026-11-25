package com.finledger.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finledger.domain.model.ThemeMode
import com.finledger.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val PRIMARY_CURRENCY = stringPreferencesKey("primary_currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FY_START_MONTH = intPreferencesKey("fy_start_month")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "Personal",
            primaryCurrency = prefs[Keys.PRIMARY_CURRENCY] ?: "INR",
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            financialYearStartMonth = prefs[Keys.FY_START_MONTH] ?: 4,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: false,
        )
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    suspend fun setPrimaryCurrency(currency: String) {
        dataStore.edit { it[Keys.PRIMARY_CURRENCY] = currency.uppercase() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFinancialYearStartMonth(month: Int) {
        dataStore.edit { it[Keys.FY_START_MONTH] = month.coerceIn(1, 12) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPin(pinHash: String?) {
        dataStore.edit {
            if (pinHash == null) {
                it[Keys.PIN_ENABLED] = false
                it.remove(Keys.PIN_HASH)
            } else {
                it[Keys.PIN_ENABLED] = true
                it[Keys.PIN_HASH] = pinHash
            }
        }
    }

    val pinHash: Flow<String?> = dataStore.data.map { it[Keys.PIN_HASH] }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }
}

