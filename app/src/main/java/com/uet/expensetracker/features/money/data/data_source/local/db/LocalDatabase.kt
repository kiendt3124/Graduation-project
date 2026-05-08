package com.uet.expensetracker.features.money.data.data_source.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.WalletDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.CurrencyDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.TransactionDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.CategoryDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertSettingsDao
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.CategoryEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.TransactionEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetAlertEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetAlertSettingsEntity

@Database(
    entities = [
        WalletEntity::class, 
        CurrencyEntity::class, 
        TransactionEntity::class, 
        CategoryEntity::class, 
        BudgetEntity::class,
        BudgetAlertEntity::class,
        BudgetAlertSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract val walletDao: WalletDao
    abstract val currencyDao: CurrencyDao
    abstract val transactionDao: TransactionDao
    abstract val categoryDao: CategoryDao
    abstract val budgetDao: BudgetDao
    abstract val budgetAlertDao: BudgetAlertDao
    abstract val budgetAlertSettingsDao: BudgetAlertSettingsDao

    companion object {
        const val DATABASE_NAME = "expense_tracker_database.db"

        @Volatile
        private var Instance: LocalDatabase? = null

        /**
         * Migration from version 1 to 2: Add debt tracking fields to transactions table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add debt tracking fields to transactions table
                database.execSQL("ALTER TABLE transactions ADD COLUMN parentDebtId INTEGER")
                database.execSQL("ALTER TABLE transactions ADD COLUMN debtReference TEXT")
                database.execSQL("ALTER TABLE transactions ADD COLUMN debtMetadata TEXT")
                
                // Create indexes for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_debtReference ON transactions(debtReference)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_parentDebtId ON transactions(parentDebtId)")
            }
        }

        /**
         * Migration from version 2 to 3: Add budget alert tables
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create budget_alert table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_alert (
                        alertId TEXT PRIMARY KEY NOT NULL,
                        budgetId INTEGER NOT NULL,
                        alertType INTEGER NOT NULL,
                        severity INTEGER NOT NULL,
                        message TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        isDismissed INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(budgetId) REFERENCES budget(budgetId) ON DELETE CASCADE
                    )
                """)
                
                // Create indexes for budget_alert
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alert_budgetId ON budget_alert(budgetId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alert_alertType ON budget_alert(alertType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alert_isDismissed ON budget_alert(isDismissed)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alert_timestamp ON budget_alert(timestamp)")
                
                // Create budget_alert_settings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budget_alert_settings (
                        settingsId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        budgetId INTEGER,
                        enableWarningAlerts INTEGER NOT NULL DEFAULT 1,
                        warningThresholds TEXT NOT NULL DEFAULT '[80,90,95]',
                        enableExceededAlerts INTEGER NOT NULL DEFAULT 1,
                        enableExpiringAlerts INTEGER NOT NULL DEFAULT 1,
                        expiringDaysBefore INTEGER NOT NULL DEFAULT 3,
                        enableDailyRateAlerts INTEGER NOT NULL DEFAULT 1,
                        dailyRateThreshold REAL NOT NULL DEFAULT 1.5,
                        enablePushNotifications INTEGER NOT NULL DEFAULT 1,
                        enableInAppAlerts INTEGER NOT NULL DEFAULT 1,
                        quietHoursStart INTEGER,
                        quietHoursEnd INTEGER,
                        alertFrequency INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(budgetId) REFERENCES budget(budgetId) ON DELETE CASCADE
                    )
                """)
                
                // Create index for budget_alert_settings
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budget_alert_settings_budgetId ON budget_alert_settings(budgetId)")
            }
        }

        fun getDatabase(context: Context): LocalDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, LocalDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    /**
                     * Setting this option in your app's database builder means that Room
                     * permanently deletes all data from the tables in your database when it
                     * attempts to perform a migration with no defined migration path.
                     */
                    .fallbackToDestructiveMigration()
//                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}