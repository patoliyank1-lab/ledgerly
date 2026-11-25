package com.finledger.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finledger.data.local.dao.BillDao
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val billDao: BillDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.preferences.first()
        if (!prefs.notificationsEnabled || !prefs.billRemindersEnabled) return Result.success()

        val now = System.currentTimeMillis()
        val bills = billDao.getAllOnce().filter { it.isActive }
        for (bill in bills) {
            val reminderWindowMs = bill.reminderDaysBefore * 86_400_000L
            if (bill.nextDueDateMs <= now + reminderWindowMs) {
                val overdue = bill.nextDueDateMs < now
                val title = if (overdue) "Overdue Bill: ${bill.name}" else "Upcoming Bill: ${bill.name}"
                val body = "${MoneyFormatter.format(bill.expectedAmount, bill.currency)} due on ${DateFormatter.formatDate(bill.nextDueDateMs)}"
                NotificationHelper.showNotification(
                    context = context,
                    channelId = NotificationHelper.CHANNEL_REMINDERS,
                    notificationId = 10_000 + bill.id.toInt(),
                    title = title,
                    body = body,
                )
            }
        }
        return Result.success()
    }
}

