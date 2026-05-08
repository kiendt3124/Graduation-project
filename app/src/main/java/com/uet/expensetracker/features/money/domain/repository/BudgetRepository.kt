package com.uet.expensetracker.features.money.domain.repository

import com.uet.expensetracker.features.money.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface BudgetRepository {
    suspend fun saveBudget(budget: Budget): Long
    suspend fun getBudgetById(budgetId: Int): Budget?
    suspend fun getBudgetsByCategory(categoryId: Int): Flow<List<Budget>>
    suspend fun getBudgetsByWallet(walletId: Int): Flow<List<Budget>>
    suspend fun getBudgetsByCategory(walletId: Int, categoryId: Int): Flow<List<Budget>>
    suspend fun getActiveBudgetsForDate(date: Date): Flow<List<Budget>>
    suspend fun getAllBudgets(): Flow<List<Budget>>
    suspend fun getBudgetsFromTotalWallet(): Flow<List<Budget>>
    suspend fun deleteBudget(budgetId: Int)
} 