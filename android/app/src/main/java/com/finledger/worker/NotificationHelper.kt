package com.finledger.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.finledger.R

object NotificationHelper {
    const val CHANNEL_REMINDERS = "finledger_reminders"
    const val CHANNEL_ALERTS = "finledger_alerts"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Bills & Loan Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Upcoming bills, subscriptions, and loan repayment reminders"
        }
        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Budgets & Alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Budget limits and unusual spending alerts"
        }
        manager.createNotificationChannel(reminders)
        manager.createNotificationChannel(alerts)
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

