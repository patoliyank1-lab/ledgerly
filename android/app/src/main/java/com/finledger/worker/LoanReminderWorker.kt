package com.finledger.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finledger.data.local.dao.LoanDao
import com.finledger.data.local.entity.LoanStatus
import com.finledger.data.local.entity.LoanType
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.util.DateFormatter
import com.finledger.domain.util.MoneyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class LoanReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val loanDao: LoanDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.preferences.first()
        if (!prefs.notificationsEnabled) return Result.success()

        val now = System.currentTimeMillis()
        val threeDaysMs = 3 * 86_400_000L
        val loans = loanDao.getAllOnce().filter { it.status != LoanStatus.CLOSED && it.expectedRepaymentDate != null }
        for (loan in loans) {
            val due = loan.expectedRepaymentDate ?: continue
            if (due <= now + threeDaysMs) {
                val title = if (loan.type == LoanType.LENT) {
                    "Loan Collection Due: ${loan.personOrOrg}"
                } else {
                    "Loan Repayment Due: ${loan.personOrOrg}"
                }
                val body = "Principal ${MoneyFormatter.format(loan.principalAmount, loan.currency)} due ${DateFormatter.formatDate(due)}"
                NotificationHelper.showNotification(
                    context = context,
                    channelId = NotificationHelper.CHANNEL_REMINDERS,
                    notificationId = 20_000 + loan.id.toInt(),
                    title = title,
                    body = body,
                )
            }
        }
        return Result.success()
    }
}

