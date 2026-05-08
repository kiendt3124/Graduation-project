package com.uet.expensetracker.features.money.data.worker

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.ui.navigation.MainActivity

/**
 * Worker hiển thị notification nhắc người dùng ghi lại giao dịch mỗi ngày.
 *
 * Worker này không phụ thuộc vào DI nên có thể được tạo trực tiếp bởi WorkManager.
 * Logic đơn giản: chỉ build notification với pending intent mở MainActivity
 * và điều hướng tới màn thêm giao dịch.
 */
class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            showDailyReminderNotification()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showDailyReminderNotification() {
        val context = applicationContext

        ensureNotificationChannel(context)

        // Android 13+ yêu cầu kiểm tra quyền POST_NOTIFICATIONS trước khi gọi notify()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                // Không có quyền -> không hiển thị notification, tránh crash/lint error
                return
            }
        }

        // Khi click → mở MainActivity và điều hướng đến AddTransactionScreen
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("feature", "show_add_transaction")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DAILY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_2)
            .setContentTitle(context.getString(R.string.daily_reminder_title))
            .setContentText(context.getString(R.string.daily_reminder_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(DAILY_REMINDER_NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                context.getString(R.string.daily_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.daily_reminder_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val DAILY_REMINDER_WORK_NAME = "DailyTransactionReminder"
        const val DAILY_REMINDER_CHANNEL_ID = "daily_reminder_channel"
        const val DAILY_REMINDER_NOTIFICATION_ID = 1001
    }
}


