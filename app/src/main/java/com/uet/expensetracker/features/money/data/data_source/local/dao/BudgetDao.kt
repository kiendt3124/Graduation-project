package com.uet.expensetracker.features.money.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Query("SELECT * FROM budget WHERE budgetId = :budgetId")
    suspend fun getBudgetById(budgetId: Int): BudgetEntity?

    @Query("SELECT * FROM budget WHERE categoryId = :categoryId")
    fun getBudgetsByCategory(categoryId: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE walletId = :walletId")
    fun getBudgetsByWallet(walletId: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE walletId = :walletId AND categoryId = :categoryId")
    fun getBudgetsByCategory(walletId: Int, categoryId: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE :date BETWEEN fromDate AND endDate")
    fun getActiveBudgetsForDate(date: Date): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE walletId IS NULL")
    fun getBudgetsFromTotalWallet(): Flow<List<BudgetEntity>>


    @Query("DELETE FROM budget WHERE budgetId = :budgetId")
    suspend fun deleteBudget(budgetId: Int)
} 