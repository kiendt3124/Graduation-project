package com.uet.expensetracker.features.ai.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class FinancialContextSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated, skipping sync")
            return Result.success()
        }

        return try {
            val context = FinancialContextSyncHelper.syncContext()
            if (context == null) {
                Log.w(TAG, "Sync helper not initialized")
                return Result.failure()
            }
            
            if (context.wallets.isEmpty() && context.recentTransactions.isEmpty()) {
                Log.d(TAG, "No financial data to sync")
                return Result.success()
            }
            
            Log.d(TAG, "Financial context synced successfully")
            Result.success()
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error ${e.code()}: ${e.message()}")
            when (e.code()) {
                401, 403 -> Result.failure()
                in 500..599 -> if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
                else -> Result.failure()
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "FinancialContextSync"
        private const val PERIODIC_WORK_NAME = "FinancialContextSyncPeriodic"
        private const val ONE_TIME_WORK_NAME = "FinancialContextSyncOneTime"
        private const val MAX_RETRY_ATTEMPTS = 3

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<FinancialContextSyncWorker>(
                6, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<FinancialContextSyncWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

