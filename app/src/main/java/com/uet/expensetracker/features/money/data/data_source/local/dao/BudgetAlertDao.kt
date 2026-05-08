package com.uet.expensetracker.features.money.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetAlertEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BudgetAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: BudgetAlertEntity)

    @Query("SELECT * FROM budget_alert WHERE budgetId = :budgetId AND isDismissed = 0 ORDER BY timestamp DESC")
    fun getActiveAlertsByBudget(budgetId: Int): Flow<List<BudgetAlertEntity>>

    @Query("SELECT * FROM budget_alert WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getAllActiveAlerts(): Flow<List<BudgetAlertEntity>>

    @Query("SELECT * FROM budget_alert WHERE budgetId = :budgetId AND alertType = :alertType AND isDismissed = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAlertByType(budgetId: Int, alertType: Int): BudgetAlertEntity?

    @Query("SELECT * FROM budget_alert WHERE budgetId = :budgetId AND alertType = :alertType AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch') AND isDismissed = 0")
    suspend fun getAlertByTypeAndDate(budgetId: Int, alertType: Int, date: Date): BudgetAlertEntity?

    @Query("UPDATE budget_alert SET isRead = 1 WHERE alertId = :alertId")
    suspend fun markAsRead(alertId: String)

    @Query("UPDATE budget_alert SET isDismissed = 1 WHERE alertId = :alertId")
    suspend fun dismissAlert(alertId: String)

    @Query("UPDATE budget_alert SET isDismissed = 1 WHERE budgetId = :budgetId")
    suspend fun dismissAllAlertsForBudget(budgetId: Int)

    @Query("DELETE FROM budget_alert WHERE isDismissed = 1 AND timestamp < :beforeDate")
    suspend fun deleteOldDismissedAlerts(beforeDate: Date)

    @Query("SELECT COUNT(*) FROM budget_alert WHERE isDismissed = 0 AND isRead = 0")
    fun getUnreadAlertCount(): Flow<Int>
}

