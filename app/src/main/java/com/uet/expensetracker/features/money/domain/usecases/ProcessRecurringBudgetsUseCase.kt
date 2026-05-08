package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.domain.usecases.SaveBudgetUseCase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

/**
 * Use case to process recurring budgets: for each budget with isRepeating=true and endDate < today,
 * compute the next period (monthly) and update via SaveBudgetUseCase.
 */
class ProcessRecurringBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val saveBudgetUseCase: SaveBudgetUseCase
) {
    suspend operator fun invoke() {
        // Load current budgets
        val budgets = budgetRepository.getAllBudgets().first()
        val now = Calendar.getInstance().time
        budgets.filter { it.isRepeating && it.endDate.before(now) }
            .forEach { budget ->
                // Compute next period (monthly)
                val calendar = Calendar.getInstance().apply { time = budget.fromDate }
                calendar.add(Calendar.MONTH, 1)
                // First day of next month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val newFrom = calendar.time
                // Last day of next month
                val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, lastDay)
                val newTo = calendar.time

                // Create updated budget preserving ID and repeating flag
                val updated = Budget(
                    budgetId = budget.budgetId,
                    category = budget.category,
                    wallet = budget.wallet,
                    amount = budget.amount,
                    fromDate = newFrom,
                    endDate = newTo,
                    isRepeating = true
                )
                // Save (Room REPLACE) to update dates
                saveBudgetUseCase(updated)
            }
    }
} 