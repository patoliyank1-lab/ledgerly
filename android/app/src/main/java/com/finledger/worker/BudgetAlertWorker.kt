package com.finledger.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.usecase.GetBudgetProgressUseCase
import com.finledger.domain.util.MoneyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.preferences.first()
        if (!prefs.notificationsEnabled || !prefs.budgetAlertsEnabled) return Result.success()

        val progressList = getBudgetProgressUseCase().first()
        for (item in progressList) {
            if (!item.isAlertTriggered && !item.isExceeded) continue
            val budget = item.budget
            val title = if (item.isExceeded) {
                "Budget Exceeded: ${budget.name}"
            } else {
                "Budget Alert (${item.percentUsed}%): ${budget.name}"
            }
            val body = "Spent ${MoneyFormatter.format(item.spentPaise, prefs.primaryCurrency)} of ${MoneyFormatter.format(budget.amount, prefs.primaryCurrency)}"
            NotificationHelper.showNotification(
                context = context,
                channelId = NotificationHelper.CHANNEL_ALERTS,
                notificationId = 30_000 + budget.id.toInt(),
                title = title,
                body = body,
            )
        }
        return Result.success()
    }
}

