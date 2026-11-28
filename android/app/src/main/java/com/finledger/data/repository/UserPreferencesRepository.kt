package com.finledger.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finledger.domain.model.AppExperienceConfig
import com.finledger.domain.model.QuickTemplateCodec
import com.finledger.domain.model.QuickTransactionTemplate
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
        val LOCK_TIMEOUT_SECONDS = intPreferencesKey("lock_timeout_seconds")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BILL_REMINDERS_ENABLED = booleanPreferencesKey("bill_reminders_enabled")
        val LOAN_REMINDERS_ENABLED = booleanPreferencesKey("loan_reminders_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val CASH_WALLET_SEEDED = booleanPreferencesKey("cash_wallet_seeded")
        val LAST_USED_ACCOUNT_ID = longPreferencesKey("last_used_account_id")
        val QUICK_TEMPLATES = stringPreferencesKey("quick_templates_json")
        val IS_SIMPLE_MODE = booleanPreferencesKey("is_simple_mode")
        val TRACK_SHARED_AND_PEOPLE = booleanPreferencesKey("track_shared_and_people")
        val TRACK_LOANS = booleanPreferencesKey("track_loans")
        val TRACK_INVESTMENTS = booleanPreferencesKey("track_investments")
        val TRACK_PLANNING = booleanPreferencesKey("track_planning")
        val SHOW_DASHBOARD_NET_WORTH = booleanPreferencesKey("show_dashboard_net_worth")
        val SHOW_DASHBOARD_ALERTS = booleanPreferencesKey("show_dashboard_alerts")
        val CHECKLIST_DISMISSED = booleanPreferencesKey("checklist_dismissed")
        val BACKUP_REMINDER_SNOOZED_UNTIL_MS = longPreferencesKey("backup_reminder_snoozed_until_ms")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        val experience = AppExperienceConfig.resolve(
            onboardingCompleted = onboardingCompleted,
            isSimpleModeStored = prefs[Keys.IS_SIMPLE_MODE],
            trackSharedAndPeopleStored = prefs[Keys.TRACK_SHARED_AND_PEOPLE],
            trackLoansStored = prefs[Keys.TRACK_LOANS],
            trackInvestmentsStored = prefs[Keys.TRACK_INVESTMENTS],
            trackPlanningStored = prefs[Keys.TRACK_PLANNING],
            showDashboardNetWorthStored = prefs[Keys.SHOW_DASHBOARD_NET_WORTH],
            showDashboardAlertsStored = prefs[Keys.SHOW_DASHBOARD_ALERTS],
        )
        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "Personal",
            primaryCurrency = prefs[Keys.PRIMARY_CURRENCY] ?: "INR",
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            financialYearStartMonth = prefs[Keys.FY_START_MONTH] ?: 4,
            onboardingCompleted = onboardingCompleted,
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            lockTimeoutSeconds = prefs[Keys.LOCK_TIMEOUT_SECONDS] ?: 60,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            billRemindersEnabled = prefs[Keys.BILL_REMINDERS_ENABLED] ?: true,
            loanRemindersEnabled = prefs[Keys.LOAN_REMINDERS_ENABLED] ?: true,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: false,
            lastUsedAccountId = prefs[Keys.LAST_USED_ACCOUNT_ID],
            quickTemplates = QuickTemplateCodec.decode(prefs[Keys.QUICK_TEMPLATES]),
            isSimpleMode = experience.isSimpleMode,
            trackSharedAndPeople = experience.trackSharedAndPeople,
            trackLoans = experience.trackLoans,
            trackInvestments = experience.trackInvestments,
            trackPlanning = experience.trackPlanning,
            showDashboardNetWorth = experience.showDashboardNetWorth,
            showDashboardAlerts = experience.showDashboardAlerts,
            checklistDismissed = prefs[Keys.CHECKLIST_DISMISSED] ?: false,
            backupReminderSnoozedUntilMs = prefs[Keys.BACKUP_REMINDER_SNOOZED_UNTIL_MS] ?: 0L,
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

    suspend fun setLockTimeoutSeconds(seconds: Int) {
        dataStore.edit { it[Keys.LOCK_TIMEOUT_SECONDS] = seconds.coerceAtLeast(0) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBillRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BILL_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setLoanRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LOAN_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    val cashWalletSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.CASH_WALLET_SEEDED] ?: false }

    suspend fun setCashWalletSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.CASH_WALLET_SEEDED] = seeded }
    }

    suspend fun setLastUsedAccountId(accountId: Long) {
        dataStore.edit { it[Keys.LAST_USED_ACCOUNT_ID] = accountId }
    }

    suspend fun saveQuickTemplate(template: QuickTransactionTemplate) {
        dataStore.edit { prefs ->
            val current = QuickTemplateCodec.decode(prefs[Keys.QUICK_TEMPLATES])
            val updated = QuickTemplateCodec.addTemplate(current, template)
            prefs[Keys.QUICK_TEMPLATES] = QuickTemplateCodec.encode(updated)
        }
    }

    suspend fun deleteQuickTemplate(templateId: String) {
        dataStore.edit { prefs ->
            val current = QuickTemplateCodec.decode(prefs[Keys.QUICK_TEMPLATES])
            val updated = QuickTemplateCodec.removeTemplate(current, templateId)
            prefs[Keys.QUICK_TEMPLATES] = QuickTemplateCodec.encode(updated)
        }
    }

    suspend fun setSimpleMode(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_SIMPLE_MODE] = enabled }
    }

    suspend fun setTrackSharedAndPeople(enabled: Boolean) {
        dataStore.edit { it[Keys.TRACK_SHARED_AND_PEOPLE] = enabled }
    }

    suspend fun setTrackLoans(enabled: Boolean) {
        dataStore.edit { it[Keys.TRACK_LOANS] = enabled }
    }

    suspend fun setTrackInvestments(enabled: Boolean) {
        dataStore.edit { it[Keys.TRACK_INVESTMENTS] = enabled }
    }

    suspend fun setTrackPlanning(enabled: Boolean) {
        dataStore.edit { it[Keys.TRACK_PLANNING] = enabled }
    }

    suspend fun setShowDashboardNetWorth(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_DASHBOARD_NET_WORTH] = enabled }
    }

    suspend fun setShowDashboardAlerts(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_DASHBOARD_ALERTS] = enabled }
    }

    suspend fun setChecklistDismissed(dismissed: Boolean) {
        dataStore.edit { it[Keys.CHECKLIST_DISMISSED] = dismissed }
    }

    suspend fun snoozeBackupReminder(untilMs: Long) {
        dataStore.edit { it[Keys.BACKUP_REMINDER_SNOOZED_UNTIL_MS] = untilMs }
    }

    suspend fun applyExperienceConfig(
        isSimpleMode: Boolean,
        trackSharedAndPeople: Boolean,
        trackLoans: Boolean,
        trackInvestments: Boolean,
        trackPlanning: Boolean,
        showDashboardNetWorth: Boolean,
        showDashboardAlerts: Boolean,
    ) {
        dataStore.edit {
            it[Keys.IS_SIMPLE_MODE] = isSimpleMode
            it[Keys.TRACK_SHARED_AND_PEOPLE] = trackSharedAndPeople
            it[Keys.TRACK_LOANS] = trackLoans
            it[Keys.TRACK_INVESTMENTS] = trackInvestments
            it[Keys.TRACK_PLANNING] = trackPlanning
            it[Keys.SHOW_DASHBOARD_NET_WORTH] = showDashboardNetWorth
            it[Keys.SHOW_DASHBOARD_ALERTS] = showDashboardAlerts
        }
    }

    suspend fun applySimplePreset() {
        val target = AppExperienceConfig.applySimplePreset()
        applyExperienceConfig(
            isSimpleMode = target.isSimpleMode,
            trackSharedAndPeople = target.trackSharedAndPeople,
            trackLoans = target.trackLoans,
            trackInvestments = target.trackInvestments,
            trackPlanning = target.trackPlanning,
            showDashboardNetWorth = target.showDashboardNetWorth,
            showDashboardAlerts = target.showDashboardAlerts,
        )
    }

    suspend fun applyAdvancedPreset() {
        val target = AppExperienceConfig.applyAdvancedPreset()
        applyExperienceConfig(
            isSimpleMode = target.isSimpleMode,
            trackSharedAndPeople = target.trackSharedAndPeople,
            trackLoans = target.trackLoans,
            trackInvestments = target.trackInvestments,
            trackPlanning = target.trackPlanning,
            showDashboardNetWorth = target.showDashboardNetWorth,
            showDashboardAlerts = target.showDashboardAlerts,
        )
    }
}

