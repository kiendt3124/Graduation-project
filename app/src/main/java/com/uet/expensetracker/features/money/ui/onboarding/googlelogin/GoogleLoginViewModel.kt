package com.uet.expensetracker.features.money.ui.onboarding.googlelogin

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.uet.expensetracker.R
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.repository.OnboardingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleLoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val onboardingRepository: OnboardingRepository,
) : BaseViewModel<GoogleLoginState, GoogleLoginIntent, GoogleLoginEvent>() {

    override val _viewState = MutableStateFlow(GoogleLoginState())

    override fun processIntent(intent: GoogleLoginIntent) {
        when (intent) {
            GoogleLoginIntent.SignIn -> emitEvent(GoogleLoginEvent.LaunchSignInFlow)
            is GoogleLoginIntent.GoogleSignIn -> handleGoogleSignIn(intent.idToken)
            GoogleLoginIntent.Back -> emitEvent(GoogleLoginEvent.NavigateBack)
        }
    }

    private fun handleGoogleSignIn(idToken: String?) {
        viewModelScope.launch {
            android.util.Log.d("GoogleLoginVM", "handleGoogleSignIn called with token: ${idToken?.take(20)}...")
            if (idToken.isNullOrBlank()) {
                emitEvent(GoogleLoginEvent.ShowError(context.getString(R.string.google_login_error_no_token)))
                return@launch
            }

            _viewState.value = _viewState.value.copy(isLoading = true, error = null)

            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                android.util.Log.d("GoogleLoginVM", "Credential created, calling signInWithCredential")
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        android.util.Log.d("GoogleLoginVM", "Sign in successful")
                        viewModelScope.launch {
                            // Consider onboarding completed once user has logged in
                            onboardingRepository.setOnboardingCompleted(true)
                            _viewState.value = _viewState.value.copy(isLoading = false)
                            emitEvent(GoogleLoginEvent.SignInSuccess)
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("GoogleLoginVM", "Sign in failed", e)
                        _viewState.value = _viewState.value.copy(isLoading = false)
                        val msg = e.message ?: e.toString()
                        emitEvent(
                            GoogleLoginEvent.ShowError(
                                "Auth failed: $msg"
                            )
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("GoogleLoginVM", "Exception during sign in", e)
                _viewState.value = _viewState.value.copy(isLoading = false)
                emitEvent(
                    GoogleLoginEvent.ShowError(
                        e.message ?: context.getString(R.string.google_login_sign_in_error)
                    )
                )
            }
        }
    }
}


