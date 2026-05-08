package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject

data class DailySpendingRateResult(
    val recommendedDaily: Double,
    val actualDaily: Double,
    val rate: Double, // actual / recommended
    val isHigh: Boolean,
    val isCritical: Boolean
)

class CalculateDailySpendingRateUseCase @Inject constructor() {
    operator fun invoke(
        budget: Budget,
        transactions: List<Transaction>
    ): DailySpendingRateResult {
        val now = Date()
        val daysTotal = ChronoUnit.DAYS.between(
            budget.fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
            budget.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ).coerceAtLeast(1)
        
        val daysPassed = ChronoUnit.DAYS.between(
            budget.fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
            now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ).coerceAtLeast(1)
        
        val recommendedDaily = budget.amount / daysTotal
        
        val spentAmount = transactions
            .filter { 
                it.category.id == budget.category.id && 
                it.transactionType == TransactionType.OUTFLOW &&
                it.transactionDate.before(now) &&
                it.transactionDate.after(budget.fromDate)
            }
            .sumOf { it.amount }
        
        val actualDaily = if (daysPassed > 0) spentAmount / daysPassed else 0.0
        
        val rate = if (recommendedDaily > 0) actualDaily / recommendedDaily else 0.0
        
        return DailySpendingRateResult(
            recommendedDaily = recommendedDaily,
            actualDaily = actualDaily,
            rate = rate,
            isHigh = rate >= 1.5, // 150%
            isCritical = rate >= 2.0 // 200%
        )
    }
}

