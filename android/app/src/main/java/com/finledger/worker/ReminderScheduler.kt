package com.finledger.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finledger.domain.model.UserPreferences
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val BILL_WORK = "finledger_bill_reminders"
    private const val LOAN_WORK = "finledger_loan_reminders"
    private const val BUDGET_WORK = "finledger_budget_alerts"
    private const val BACKUP_WORK = "finledger_auto_backup"

    fun scheduleAll(context: Context) {
        syncWithPreferences(context, UserPreferences())
    }

    fun syncWithPreferences(context: Context, prefs: UserPreferences) {
        val wm = WorkManager.getInstance(context)

        if (prefs.notificationsEnabled && prefs.billRemindersEnabled) {
            val billRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(BILL_WORK, ExistingPeriodicWorkPolicy.KEEP, billRequest)
        } else {
            wm.cancelUniqueWork(BILL_WORK)
        }

        if (prefs.notificationsEnabled && prefs.loanRemindersEnabled) {
            val loanRequest = PeriodicWorkRequestBuilder<LoanReminderWorker>(24, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(LOAN_WORK, ExistingPeriodicWorkPolicy.KEEP, loanRequest)
        } else {
            wm.cancelUniqueWork(LOAN_WORK)
        }

        if (prefs.notificationsEnabled && prefs.budgetAlertsEnabled) {
            val budgetRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(12, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(BUDGET_WORK, ExistingPeriodicWorkPolicy.KEEP, budgetRequest)
        } else {
            wm.cancelUniqueWork(BUDGET_WORK)
        }

        if (prefs.autoBackupEnabled) {
            val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(BACKUP_WORK, ExistingPeriodicWorkPolicy.KEEP, backupRequest)
        } else {
            wm.cancelUniqueWork(BACKUP_WORK)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleAll(context)
        }
    }
}
