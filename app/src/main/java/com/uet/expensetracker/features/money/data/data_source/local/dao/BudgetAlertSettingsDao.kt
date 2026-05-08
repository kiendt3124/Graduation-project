package com.uet.expensetracker.features.money.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetAlertSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetAlertSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: BudgetAlertSettingsEntity): Long

    @Query("SELECT * FROM budget_alert_settings WHERE settingsId = 0 LIMIT 1")
    suspend fun getGlobalSettings(): BudgetAlertSettingsEntity?

    @Query("SELECT * FROM budget_alert_settings WHERE budgetId = :budgetId LIMIT 1")
    suspend fun getSettingsForBudget(budgetId: Int): BudgetAlertSettingsEntity?

    @Query("SELECT * FROM budget_alert_settings WHERE budgetId = :budgetId OR budgetId IS NULL ORDER BY budgetId DESC LIMIT 1")
    suspend fun getEffectiveSettings(budgetId: Int?): BudgetAlertSettingsEntity?

    @Query("SELECT * FROM budget_alert_settings")
    fun getAllSettings(): Flow<List<BudgetAlertSettingsEntity>>

    @Query("DELETE FROM budget_alert_settings WHERE budgetId = :budgetId")
    suspend fun deleteSettingsForBudget(budgetId: Int)
}

