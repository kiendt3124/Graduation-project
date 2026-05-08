package com.uet.expensetracker.features.money.ui.onboarding.splash

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : BaseViewModel<SplashState, SplashIntent, SplashEvent>() {

    override val _viewState = MutableStateFlow(SplashState())

    override fun processIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.CheckStatus -> checkOnboardingStatus()
        }
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val completed = onboardingRepository.isOnboardingCompleted().first()
            delay(2000) // Simulate loading delay
            emitEvent(
                if (completed) SplashEvent.GoToHome
                else SplashEvent.GoToOnboarding
            )
        }
    }
} 