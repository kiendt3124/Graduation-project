package com.uet.expensetracker.features.money.domain.repository

import com.uet.expensetracker.features.money.domain.model.BudgetAlert
import com.uet.expensetracker.features.money.domain.model.BudgetAlertSettings
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface BudgetAlertRepository {
    suspend fun saveAlert(alert: BudgetAlert)
    suspend fun getActiveAlertsByBudget(budgetId: Int): Flow<List<BudgetAlert>>
    suspend fun getAllActiveAlerts(): Flow<List<BudgetAlert>>
    suspend fun getLatestAlertByType(budgetId: Int, alertType: com.uet.expensetracker.features.money.domain.model.AlertType): BudgetAlert?
    suspend fun getAlertByTypeAndDate(budgetId: Int, alertType: com.uet.expensetracker.features.money.domain.model.AlertType, date: Date): BudgetAlert?
    suspend fun markAsRead(alertId: String)
    suspend fun dismissAlert(alertId: String)
    suspend fun dismissAllAlertsForBudget(budgetId: Int)
    suspend fun deleteOldDismissedAlerts(beforeDate: Date)
    suspend fun getUnreadAlertCount(): Flow<Int>

    suspend fun saveSettings(settings: BudgetAlertSettings): Long
    suspend fun getGlobalSettings(): BudgetAlertSettings?
    suspend fun getSettingsForBudget(budgetId: Int): BudgetAlertSettings?
    suspend fun getEffectiveSettings(budgetId: Int?): BudgetAlertSettings
    suspend fun getAllSettings(): Flow<List<BudgetAlertSettings>>
    suspend fun deleteSettingsForBudget(budgetId: Int)
}

