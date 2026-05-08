package com.uet.expensetracker.core.di

import android.content.Context
import androidx.room.Room
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertSettingsDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.CategoryDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.CurrencyDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.TransactionDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.WalletDao
import com.uet.expensetracker.features.money.data.data_source.local.db.LocalDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase {
        return LocalDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWalletDao(database: LocalDatabase): WalletDao {
        return database.walletDao
    }

    @Provides
    @Singleton
    fun provideCurrencyDao(database: LocalDatabase): CurrencyDao {
        return database.currencyDao
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: LocalDatabase): TransactionDao {
        return database.transactionDao
    }
    
    @Provides
    @Singleton
    fun provideCategoryDao(database: LocalDatabase): CategoryDao {
        return database.categoryDao
    }
    
    @Provides
    @Singleton
    fun provideBudgetDao(database: LocalDatabase): BudgetDao {
        return database.budgetDao
    }
    
    @Provides
    @Singleton
    fun provideBudgetAlertDao(database: LocalDatabase): BudgetAlertDao {
        return database.budgetAlertDao
    }
    
    @Provides
    @Singleton
    fun provideBudgetAlertSettingsDao(database: LocalDatabase): BudgetAlertSettingsDao {
        return database.budgetAlertSettingsDao
    }
}