package com.uet.expensetracker.features.money.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertSettingsDao
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetAlertEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetAlertSettingsEntity
import com.uet.expensetracker.features.money.domain.model.AlertFrequency
import com.uet.expensetracker.features.money.domain.model.AlertSeverity
import com.uet.expensetracker.features.money.domain.model.AlertType
import com.uet.expensetracker.features.money.domain.model.BudgetAlert
import com.uet.expensetracker.features.money.domain.model.BudgetAlertSettings
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class BudgetAlertRepositoryImpl @Inject constructor(
    private val budgetAlertDao: BudgetAlertDao,
    private val budgetAlertSettingsDao: BudgetAlertSettingsDao,
    private val gson: Gson
) : BudgetAlertRepository {

    override suspend fun saveAlert(alert: BudgetAlert) {
        val entity = alert.toEntity()
        budgetAlertDao.insertAlert(entity)
    }

    override suspend fun getActiveAlertsByBudget(budgetId: Int): Flow<List<BudgetAlert>> {
        return budgetAlertDao.getActiveAlertsByBudget(budgetId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllActiveAlerts(): Flow<List<BudgetAlert>> {
        return budgetAlertDao.getAllActiveAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLatestAlertByType(
        budgetId: Int,
        alertType: AlertType
    ): BudgetAlert? {
        val entity = budgetAlertDao.getLatestAlertByType(budgetId, alertType.value)
        return entity?.toDomain()
    }

    override suspend fun getAlertByTypeAndDate(
        budgetId: Int,
        alertType: AlertType,
        date: Date
    ): BudgetAlert? {
        val entity = budgetAlertDao.getAlertByTypeAndDate(budgetId, alertType.value, date)
        return entity?.toDomain()
    }

    override suspend fun markAsRead(alertId: String) {
        budgetAlertDao.markAsRead(alertId)
    }

    override suspend fun dismissAlert(alertId: String) {
        budgetAlertDao.dismissAlert(alertId)
    }

    override suspend fun dismissAllAlertsForBudget(budgetId: Int) {
        budgetAlertDao.dismissAllAlertsForBudget(budgetId)
    }

    override suspend fun deleteOldDismissedAlerts(beforeDate: Date) {
        budgetAlertDao.deleteOldDismissedAlerts(beforeDate)
    }

    override suspend fun getUnreadAlertCount(): Flow<Int> {
        return budgetAlertDao.getUnreadAlertCount()
    }

    override suspend fun saveSettings(settings: BudgetAlertSettings): Long {
        val entity = settings.toEntity(gson)
        return budgetAlertSettingsDao.insertSettings(entity)
    }

    override suspend fun getGlobalSettings(): BudgetAlertSettings? {
        val entity = budgetAlertSettingsDao.getGlobalSettings()
        return entity?.toDomain(gson)
    }

    override suspend fun getSettingsForBudget(budgetId: Int): BudgetAlertSettings? {
        val entity = budgetAlertSettingsDao.getSettingsForBudget(budgetId)
        return entity?.toDomain(gson)
    }

    override suspend fun getEffectiveSettings(budgetId: Int?): BudgetAlertSettings {
        val entity = budgetAlertSettingsDao.getEffectiveSettings(budgetId ?: -1)
        return entity?.toDomain(gson) ?: BudgetAlertSettings.default()
    }

    override suspend fun getAllSettings(): Flow<List<BudgetAlertSettings>> {
        return budgetAlertSettingsDao.getAllSettings().map { entities ->
            entities.map { it.toDomain(gson) }
        }
    }

    override suspend fun deleteSettingsForBudget(budgetId: Int) {
        budgetAlertSettingsDao.deleteSettingsForBudget(budgetId)
    }

    // Extension functions for conversion
    private fun BudgetAlert.toEntity(): BudgetAlertEntity {
        return BudgetAlertEntity(
            alertId = alertId,
            budgetId = budgetId,
            alertType = alertType.value,
            severity = severity.value,
            message = message,
            timestamp = timestamp,
            isRead = isRead,
            isDismissed = isDismissed
        )
    }

    private fun BudgetAlertEntity.toDomain(): BudgetAlert {
        return BudgetAlert(
            alertId = alertId,
            budgetId = budgetId,
            alertType = AlertType.fromInt(alertType),
            severity = AlertSeverity.fromInt(severity),
            message = message,
            timestamp = timestamp,
            isRead = isRead,
            isDismissed = isDismissed
        )
    }

    private fun BudgetAlertSettings.toEntity(gson: Gson): BudgetAlertSettingsEntity {
        return BudgetAlertSettingsEntity(
            settingsId = settingsId,
            budgetId = budgetId,
            enableWarningAlerts = enableWarningAlerts,
            warningThresholds = gson.toJson(warningThresholds),
            enableExceededAlerts = enableExceededAlerts,
            enableExpiringAlerts = enableExpiringAlerts,
            expiringDaysBefore = expiringDaysBefore,
            enableDailyRateAlerts = enableDailyRateAlerts,
            dailyRateThreshold = dailyRateThreshold,
            enablePushNotifications = enablePushNotifications,
            enableInAppAlerts = enableInAppAlerts,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            alertFrequency = alertFrequency.value
        )
    }

    private fun BudgetAlertSettingsEntity.toDomain(gson: Gson): BudgetAlertSettings {
        val thresholdsType = object : TypeToken<List<Int>>() {}.type
        val thresholds: List<Int> = try {
            gson.fromJson(warningThresholds, thresholdsType) ?: listOf(80, 90, 95)
        } catch (e: Exception) {
            listOf(80, 90, 95)
        }

        return BudgetAlertSettings(
            settingsId = settingsId,
            budgetId = budgetId,
            enableWarningAlerts = enableWarningAlerts,
            warningThresholds = thresholds,
            enableExceededAlerts = enableExceededAlerts,
            enableExpiringAlerts = enableExpiringAlerts,
            expiringDaysBefore = expiringDaysBefore,
            enableDailyRateAlerts = enableDailyRateAlerts,
            dailyRateThreshold = dailyRateThreshold,
            enablePushNotifications = enablePushNotifications,
            enableInAppAlerts = enableInAppAlerts,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            alertFrequency = AlertFrequency.fromInt(alertFrequency)
        )
    }
}

