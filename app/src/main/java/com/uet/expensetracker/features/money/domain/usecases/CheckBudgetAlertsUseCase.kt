package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.AlertType
import com.uet.expensetracker.features.money.domain.model.AlertSeverity
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.BudgetAlert
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CheckBudgetAlertsUseCase @Inject constructor(
    private val budgetAlertRepository: BudgetAlertRepository
) {
    suspend operator fun invoke(
        budget: Budget,
        transactions: List<Transaction>,
        settings: com.uet.expensetracker.features.money.domain.model.BudgetAlertSettings
    ): List<BudgetAlert> {
        val alerts = mutableListOf<BudgetAlert>()
        
        // Calculate spent amount
        val spentAmount = transactions
            .filter { it.category.id == budget.category.id && it.transactionType == TransactionType.OUTFLOW }
            .sumOf { it.amount }
        
        val progress = if (budget.amount > 0) (spentAmount / budget.amount) else 0.0
        val progressPercent = (progress * 100).toInt()
        
        // Check warning thresholds
        if (settings.enableWarningAlerts) {
            settings.warningThresholds.forEach { threshold ->
                if (progressPercent >= threshold) {
                    val alertType = when (threshold) {
                        80 -> AlertType.WARNING_THRESHOLD_80
                        90 -> AlertType.WARNING_THRESHOLD_90
                        95 -> AlertType.WARNING_THRESHOLD_95
                        else -> null
                    }
                    
                    if (alertType != null) {
                        // Check if alert already sent today
                        val existingAlert = budgetAlertRepository.getAlertByTypeAndDate(
                            budget.budgetId,
                            alertType,
                            Date()
                        )
                        
                        if (existingAlert == null || settings.alertFrequency == com.uet.expensetracker.features.money.domain.model.AlertFrequency.EVERY_TIME) {
                            val severity = when (threshold) {
                                80 -> AlertSeverity.MEDIUM
                                90 -> AlertSeverity.HIGH
                                95 -> AlertSeverity.HIGH
                                else -> AlertSeverity.MEDIUM
                            }
                            
                            alerts.add(
                                BudgetAlert(
                                    alertId = UUID.randomUUID().toString(),
                                    budgetId = budget.budgetId,
                                    alertType = alertType,
                                    severity = severity,
                                    message = "", // Will be set by UI
                                    timestamp = Date()
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Check budget exceeded
        if (settings.enableExceededAlerts && spentAmount > budget.amount) {
            val existingAlert = budgetAlertRepository.getAlertByTypeAndDate(
                budget.budgetId,
                AlertType.BUDGET_EXCEEDED,
                Date()
            )
            
            if (existingAlert == null || settings.alertFrequency == com.uet.expensetracker.features.money.domain.model.AlertFrequency.EVERY_TIME) {
                alerts.add(
                    BudgetAlert(
                        alertId = UUID.randomUUID().toString(),
                        budgetId = budget.budgetId,
                        alertType = AlertType.BUDGET_EXCEEDED,
                        severity = AlertSeverity.URGENT,
                        message = "",
                        timestamp = Date()
                    )
                )
            }
        }
        
        // Check expiring soon
        if (settings.enableExpiringAlerts) {
            val now = Date()
            val daysUntilExpiry = ((budget.endDate.time - now.time) / (1000 * 60 * 60 * 24)).toInt()
            
            if (daysUntilExpiry <= settings.expiringDaysBefore && daysUntilExpiry >= 0) {
                val alertType = if (daysUntilExpiry == 0) AlertType.EXPIRED else AlertType.EXPIRING_SOON
                val existingAlert = budgetAlertRepository.getAlertByTypeAndDate(
                    budget.budgetId,
                    alertType,
                    Date()
                )
                
                if (existingAlert == null || settings.alertFrequency == com.uet.expensetracker.features.money.domain.model.AlertFrequency.EVERY_TIME) {
                    alerts.add(
                        BudgetAlert(
                            alertId = UUID.randomUUID().toString(),
                            budgetId = budget.budgetId,
                            alertType = alertType,
                            severity = if (daysUntilExpiry == 0) AlertSeverity.HIGH else AlertSeverity.MEDIUM,
                            message = "",
                            timestamp = Date()
                        )
                    )
                }
            }
        }
        
        return alerts
    }
}

