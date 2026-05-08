package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import javax.inject.Inject

class SaveBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    /**
     * Saves or updates a budget
     * @param budget The budget to save or update
     * @return The ID of the saved budget
     */
    suspend operator fun invoke(budget: Budget): Long {
        return budgetRepository.saveBudget(budget)
    }
} 