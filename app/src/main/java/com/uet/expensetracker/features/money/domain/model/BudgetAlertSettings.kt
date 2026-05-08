package com.uet.expensetracker.features.money.domain.model

data class BudgetAlertSettings(
    val settingsId: Int = 0,
    val budgetId: Int? = null, // null = global settings, >0 = budget-specific
    val enableWarningAlerts: Boolean = true,
    val warningThresholds: List<Int> = listOf(80, 90, 95), // percentages
    val enableExceededAlerts: Boolean = true,
    val enableExpiringAlerts: Boolean = true,
    val expiringDaysBefore: Int = 3,
    val enableDailyRateAlerts: Boolean = true,
    val dailyRateThreshold: Double = 1.5, // 150% of recommended
    val enablePushNotifications: Boolean = true,
    val enableInAppAlerts: Boolean = true,
    val quietHoursStart: Int? = null, // e.g., 22 (10 PM)
    val quietHoursEnd: Int? = null, // e.g., 7 (7 AM)
    val alertFrequency: AlertFrequency = AlertFrequency.ONCE_PER_DAY
) {
    companion object {
        fun default(): BudgetAlertSettings {
            return BudgetAlertSettings()
        }
    }
}

