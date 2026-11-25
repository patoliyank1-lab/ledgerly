package com.finledger.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val BILL_WORK = "finledger_bill_reminders"
    private const val LOAN_WORK = "finledger_loan_reminders"
    private const val BACKUP_WORK = "finledger_auto_backup"

    fun scheduleAll(context: Context) {
        val wm = WorkManager.getInstance(context)

        val billRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(BILL_WORK, ExistingPeriodicWorkPolicy.KEEP, billRequest)

        val loanRequest = PeriodicWorkRequestBuilder<LoanReminderWorker>(24, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(LOAN_WORK, ExistingPeriodicWorkPolicy.KEEP, loanRequest)

        val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(BACKUP_WORK, ExistingPeriodicWorkPolicy.KEEP, backupRequest)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleAll(context)
        }
    }
}
