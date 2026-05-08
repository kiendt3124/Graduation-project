package com.uet.expensetracker.features.money.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.AlertType
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.BudgetAlert
import com.uet.expensetracker.features.money.ui.navigation.MainActivity
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import java.util.UUID

class BudgetNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_WARNING_ID = "budget_warning_channel"
        private const val CHANNEL_EXCEEDED_ID = "budget_exceeded_channel"
        private const val CHANNEL_EXPIRING_ID = "budget_expiring_channel"
        private const val CHANNEL_DAILY_RATE_ID = "budget_daily_rate_channel"

        private const val NOTIFICATION_ID_WARNING = 2000
        private const val NOTIFICATION_ID_EXCEEDED = 2001
        private const val NOTIFICATION_ID_EXPIRING = 2002
        private const val NOTIFICATION_ID_DAILY_RATE = 2003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Warning channel
            val warningChannel = NotificationChannel(
                CHANNEL_WARNING_ID,
                context.getString(R.string.budget_alert_channel_warning_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.budget_alert_channel_warning_description)
            }

            // Exceeded channel
            val exceededChannel = NotificationChannel(
                CHANNEL_EXCEEDED_ID,
                context.getString(R.string.budget_alert_channel_exceeded_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.budget_alert_channel_exceeded_description)
            }

            // Expiring channel
            val expiringChannel = NotificationChannel(
                CHANNEL_EXPIRING_ID,
                context.getString(R.string.budget_alert_channel_expiring_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.budget_alert_channel_expiring_description)
            }

            // Daily rate channel
            val dailyRateChannel = NotificationChannel(
                CHANNEL_DAILY_RATE_ID,
                context.getString(R.string.budget_alert_channel_daily_rate_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.budget_alert_channel_daily_rate_description)
            }

            notificationManager.createNotificationChannel(warningChannel)
            notificationManager.createNotificationChannel(exceededChannel)
            notificationManager.createNotificationChannel(expiringChannel)
            notificationManager.createNotificationChannel(dailyRateChannel)
        }
    }

    fun showBudgetWarning(budget: Budget, alert: BudgetAlert) {
        if (!hasNotificationPermission()) return

        val channelId = CHANNEL_WARNING_ID
        val notificationId = NOTIFICATION_ID_WARNING + budget.budgetId

        val title = context.getString(R.string.budget_alert_warning_title, budget.category.title)
        val message = alert.message.ifEmpty { 
            context.getString(R.string.budget_alert_warning_message_default)
        }

        val intent = createBudgetDetailsIntent(budget.budgetId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo_2)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showBudgetExceeded(budget: Budget, alert: BudgetAlert) {
        if (!hasNotificationPermission()) return

        val channelId = CHANNEL_EXCEEDED_ID
        val notificationId = NOTIFICATION_ID_EXCEEDED + budget.budgetId

        val title = context.getString(R.string.budget_alert_exceeded_title, budget.category.title)
        val message = alert.message.ifEmpty {
            context.getString(R.string.budget_alert_exceeded_message_default)
        }

        val intent = createBudgetDetailsIntent(budget.budgetId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo_2)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showBudgetExpiring(budget: Budget, alert: BudgetAlert) {
        if (!hasNotificationPermission()) return

        val channelId = CHANNEL_EXPIRING_ID
        val notificationId = NOTIFICATION_ID_EXPIRING + budget.budgetId

        val title = context.getString(R.string.budget_alert_expiring_title, budget.category.title)
        val message = alert.message.ifEmpty {
            context.getString(R.string.budget_alert_expiring_message_default)
        }

        val intent = createBudgetDetailsIntent(budget.budgetId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo_2)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showDailyRateAlert(budget: Budget, alert: BudgetAlert) {
        if (!hasNotificationPermission()) return

        val channelId = CHANNEL_DAILY_RATE_ID
        val notificationId = NOTIFICATION_ID_DAILY_RATE + budget.budgetId

        val title = context.getString(R.string.budget_alert_daily_rate_title, budget.category.title)
        val message = alert.message.ifEmpty {
            context.getString(R.string.budget_alert_daily_rate_message_default)
        }

        val intent = createBudgetDetailsIntent(budget.budgetId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            UUID.randomUUID().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo_2)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun createBudgetDetailsIntent(budgetId: Int): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("feature", "show_budget_details")
            putExtra("budgetId", budgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}

