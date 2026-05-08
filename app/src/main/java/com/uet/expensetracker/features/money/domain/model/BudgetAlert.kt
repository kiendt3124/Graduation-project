package com.uet.expensetracker.features.money.domain.model

import java.util.Date

data class BudgetAlert(
    val alertId: String,
    val budgetId: Int,
    val alertType: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Date,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false
)

enum class AlertType(val value: Int) {
    WARNING_THRESHOLD_80(0),      // 80% warning
    WARNING_THRESHOLD_90(1),      // 90% warning
    WARNING_THRESHOLD_95(2),      // 95% warning
    BUDGET_EXCEEDED(3),
    EXPIRING_SOON(4),
    EXPIRED(5),
    DAILY_RATE_HIGH(6),
    DAILY_RATE_CRITICAL(7);

    companion object {
        fun fromInt(value: Int): AlertType {
            return values().find { it.value == value } ?: WARNING_THRESHOLD_80
        }
    }
}

enum class AlertSeverity(val value: Int) {
    LOW(0),        // Info
    MEDIUM(1),     // Warning
    HIGH(2),       // Critical
    URGENT(3);     // Exceeded

    companion object {
        fun fromInt(value: Int): AlertSeverity {
            return values().find { it.value == value } ?: LOW
        }
    }
}

enum class AlertFrequency(val value: Int) {
    ONCE_PER_DAY(0),      // Chỉ alert 1 lần/ngày cho mỗi loại
    ONCE_PER_WEEK(1),     // Chỉ alert 1 lần/tuần
    EVERY_TIME(2),        // Alert mỗi khi điều kiện thỏa mãn
    NEVER(3);             // Tắt hoàn toàn

    companion object {
        fun fromInt(value: Int): AlertFrequency {
            return values().find { it.value == value } ?: ONCE_PER_DAY
        }
    }
}

