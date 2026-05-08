package com.uet.expensetracker.features.ai.di

import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.domain.usecase.AiAnswerUseCase
import com.uet.expensetracker.features.ai.domain.usecase.AiClearHistoryUseCase
import com.uet.expensetracker.features.ai.domain.usecase.AiHistoryUseCase
import com.uet.expensetracker.features.ai.domain.usecase.AiPlanUseCase
import com.uet.expensetracker.features.ai.domain.usecase.ChatAiUseCase
import com.uet.expensetracker.features.ai.domain.usecase.GetInsightsAiUseCase
import com.uet.expensetracker.features.ai.domain.usecase.ParseTransactionAiUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AiUseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideChatAiUseCase(repo: AiRepository) = ChatAiUseCase(repo)

    @Provides
    @ViewModelScoped
    fun provideParseTransactionAiUseCase(repo: AiRepository) = ParseTransactionAiUseCase(repo)

    @Provides
    @ViewModelScoped
    fun provideGetInsightsAiUseCase(repo: AiRepository) = GetInsightsAiUseCase(repo)

    @Provides
    @ViewModelScoped
    fun provideAiPlanUseCase(repo: AiRepository) = AiPlanUseCase(repo)

    @Provides
    @ViewModelScoped
    fun provideAiAnswerUseCase(repo: AiRepository) = AiAnswerUseCase(repo)

    @Provides
    @ViewModelScoped
    fun provideAiHistoryUseCase(repo: AiRepository) = AiHistoryUseCase(repo)

    @Provides
    @ViewModelScoped
    fun provideAiClearHistoryUseCase(repo: AiRepository) = AiClearHistoryUseCase(repo)
}


