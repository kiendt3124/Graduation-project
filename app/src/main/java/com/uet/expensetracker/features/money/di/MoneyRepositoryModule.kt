package com.uet.expensetracker.features.money.di

import com.google.gson.Gson
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetAlertSettingsDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.CategoryDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.WalletDao
import com.uet.expensetracker.features.money.data.repository.WalletLocalRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import com.uet.expensetracker.features.money.data.data_source.local.dao.CurrencyDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.TransactionDao
import com.uet.expensetracker.features.money.data.repository.BudgetRepositoryImpl
import com.uet.expensetracker.features.money.data.repository.BudgetAlertRepositoryImpl
import com.uet.expensetracker.features.money.data.repository.CategoryRepositoryImpl
import com.uet.expensetracker.features.money.data.repository.CurrencyLocalRepository
import com.uet.expensetracker.features.money.data.repository.TransactionRepositoryImpl
import com.uet.expensetracker.features.money.data.repository.OnboardingRepositoryImpl
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import com.uet.expensetracker.features.money.domain.repository.CategoryRepository
import com.uet.expensetracker.features.money.domain.repository.CurrencyRepository
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.OnboardingRepository
import com.uet.expensetracker.utils.CurrencyConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MoneyRepositoryModule {

    @Provides
    @Singleton
    fun provideWalletRepository(walletDao: WalletDao): WalletRepository {
        return WalletLocalRepository(walletDao)
    }

    @Provides
    @Singleton
    fun provideCurrencyRepository(currencyDao: CurrencyDao): CurrencyRepository {
        return CurrencyLocalRepository(currencyDao)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(transactionDao: TransactionDao): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao)
    }
    
    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository {
        return CategoryRepositoryImpl(categoryDao)
    }
    
    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        categoryDao: CategoryDao,
        walletDao: WalletDao,
        currencyConverter: CurrencyConverter
    ): BudgetRepository {
        return BudgetRepositoryImpl(budgetDao, categoryDao, walletDao, currencyConverter)
    }

    @Provides
    @Singleton
    fun provideOnboardingRepository(
        onboardingRepositoryImpl: OnboardingRepositoryImpl
    ): OnboardingRepository {
        return onboardingRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideBudgetAlertRepository(
        budgetAlertDao: BudgetAlertDao,
        budgetAlertSettingsDao: BudgetAlertSettingsDao,
        gson: Gson
    ): BudgetAlertRepository {
        return BudgetAlertRepositoryImpl(budgetAlertDao, budgetAlertSettingsDao, gson)
    }
}