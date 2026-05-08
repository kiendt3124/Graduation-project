package com.uet.expensetracker.features.money.ui.onboarding.splash

import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.core.platform.MviEventBase

sealed interface SplashIntent : MviIntentBase {
    object CheckStatus : SplashIntent
}

/**
 * UI state for SplashScreen
 */
data class SplashState(
    val isLoading: Boolean = true
) : MviStateBase

sealed interface SplashEvent : MviEventBase {
    object GoToHome : SplashEvent
    object GoToOnboarding : SplashEvent
} 