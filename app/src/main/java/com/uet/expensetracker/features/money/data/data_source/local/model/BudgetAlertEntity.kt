package com.uet.expensetracker.features.money.data.data_source.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "budget_alert",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["budgetId"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("budgetId"),
        Index("alertType"),
        Index("isDismissed"),
        Index("timestamp")
    ]
)
data class BudgetAlertEntity(
    @PrimaryKey
    val alertId: String,
    val budgetId: Int,
    val alertType: Int, // AlertType enum as int
    val severity: Int, // AlertSeverity enum as int
    val message: String,
    val timestamp: Date,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false
)

