package com.uet.expensetracker.features.money.data.data_source.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_alert_settings",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["budgetId"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("budgetId")
    ]
)
data class BudgetAlertSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val settingsId: Int = 0,
    val budgetId: Int? = null, // null = global settings, >0 = budget-specific
    val enableWarningAlerts: Boolean = true,
    val warningThresholds: String = "[80,90,95]", // JSON array as string
    val enableExceededAlerts: Boolean = true,
    val enableExpiringAlerts: Boolean = true,
    val expiringDaysBefore: Int = 3,
    val enableDailyRateAlerts: Boolean = true,
    val dailyRateThreshold: Double = 1.5,
    val enablePushNotifications: Boolean = true,
    val enableInAppAlerts: Boolean = true,
    val quietHoursStart: Int? = null,
    val quietHoursEnd: Int? = null,
    val alertFrequency: Int = 0 // AlertFrequency enum as int (0 = ONCE_PER_DAY)
)

