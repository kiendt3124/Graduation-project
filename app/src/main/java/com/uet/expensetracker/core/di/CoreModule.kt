package com.uet.expensetracker.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Aggregate module for core app functionality
 */
@Module(
    includes = [
        DatabaseModule::class,
        FirebaseModule::class,
        NetworkModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
object CoreModule