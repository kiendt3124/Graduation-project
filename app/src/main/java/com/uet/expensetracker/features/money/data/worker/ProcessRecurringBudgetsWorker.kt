package com.uet.expensetracker.features.money.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uet.expensetracker.features.money.domain.usecases.ProcessRecurringBudgetsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProcessRecurringBudgetsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val processRecurringUseCase: ProcessRecurringBudgetsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        processRecurringUseCase()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
} 