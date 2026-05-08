package com.uet.expensetracker.features.ai.di

import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.data.AiRepositoryImpl
import com.uet.expensetracker.features.ai.data.remote.AiApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiApiService(retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides
    @Singleton
    fun provideAiRepository(api: AiApiService): AiRepository = AiRepositoryImpl(api)
}


