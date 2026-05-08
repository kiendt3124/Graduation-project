package com.uet.expensetracker.features.money.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uet.expensetracker.features.money.data.data_source.local.db.LocalDatabase
import com.uet.expensetracker.features.money.data.notification.BudgetNotificationManager
import com.uet.expensetracker.features.money.domain.model.AlertType
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.BudgetAlert
import com.uet.expensetracker.features.money.domain.model.BudgetAlertSettings
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.usecases.CheckBudgetAlertsUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetBudgetAlertSettingsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BudgetAlertCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetAlertRepository: BudgetAlertRepository,
    private val checkBudgetAlertsUseCase: CheckBudgetAlertsUseCase,
    private val getBudgetAlertSettingsUseCase: GetBudgetAlertSettingsUseCase
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = BudgetNotificationManager(appContext)

    override suspend fun doWork(): Result {
        return try {
            checkAndGenerateAlerts()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAndGenerateAlerts() {
        // Get all active budgets
        val allBudgets = budgetRepository.getAllBudgets().first()

        for (budgetEntity in allBudgets) {
            // Get effective settings for this budget
            val settingsResult = getBudgetAlertSettingsUseCase.run(
                GetBudgetAlertSettingsUseCase.Params.Effective(budgetEntity.budgetId)
            )

            val settings = when (settingsResult) {
                is com.uet.expensetracker.core.functional.Either.Right -> settingsResult.right
                else -> BudgetAlertSettings.default()
            }

            // Skip if alerts are disabled
            if (!settings.enableInAppAlerts && !settings.enablePushNotifications) {
                continue
            }

            // Get transactions for this budget
            val transactions = if (budgetEntity.wallet.id == -1) {
                // Total wallet - get all transactions for this category
                transactionRepository.observeFilteredTransactions(
                    categoryId = budgetEntity.category.id,
                    walletId = null
                ).first()
            } else {
                // Specific wallet
                transactionRepository.observeFilteredTransactions(
                    categoryId = budgetEntity.category.id,
                    walletId = budgetEntity.wallet.id
                ).first()
            }

            // Check and generate alerts
            val alerts = checkBudgetAlertsUseCase(
                budgetEntity,
                transactions,
                settings
            )

            // Save alerts and show notifications
            for (alert in alerts) {
                budgetAlertRepository.saveAlert(alert)

                // Show notification if enabled
                if (settings.enablePushNotifications) {
                    when (alert.alertType) {
                        AlertType.WARNING_THRESHOLD_80,
                        AlertType.WARNING_THRESHOLD_90,
                        AlertType.WARNING_THRESHOLD_95 -> {
                            notificationManager.showBudgetWarning(budgetEntity, alert)
                        }
                        AlertType.BUDGET_EXCEEDED -> {
                            notificationManager.showBudgetExceeded(budgetEntity, alert)
                        }
                        AlertType.EXPIRING_SOON,
                        AlertType.EXPIRED -> {
                            notificationManager.showBudgetExpiring(budgetEntity, alert)
                        }
                        AlertType.DAILY_RATE_HIGH,
                        AlertType.DAILY_RATE_CRITICAL -> {
                            notificationManager.showDailyRateAlert(budgetEntity, alert)
                        }
                    }
                }
            }
        }
    }
}

