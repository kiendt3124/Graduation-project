package com.uet.expensetracker.features.money.di

import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.domain.usecases.ProcessRecurringBudgetsUseCase
import com.uet.expensetracker.features.money.domain.usecases.SaveBudgetUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecurringBudgetWorkerModule {

    @Provides
    @Singleton
    fun provideProcessRecurringBudgetsUseCase(
        budgetRepository: BudgetRepository,
        saveBudgetUseCase: SaveBudgetUseCase
    ): ProcessRecurringBudgetsUseCase =
        ProcessRecurringBudgetsUseCase(budgetRepository, saveBudgetUseCase)
} 