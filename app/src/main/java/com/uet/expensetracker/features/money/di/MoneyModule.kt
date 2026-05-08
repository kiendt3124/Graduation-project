package com.uet.expensetracker.features.money.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Aggregate module for Money feature repositories
 *
 * Note: Use cases are provided by MoneyUseCaseModule with ViewModelComponent scope
 */
@Module(
    includes = [
        MoneyRepositoryModule::class,
        MoneyDataModule::class,
        RecurringBudgetWorkerModule::class,
        // AI use cases are provided separately via AiUseCaseModule (ViewModel scope)
    ]
)
@InstallIn(SingletonComponent::class)
object MoneyModule