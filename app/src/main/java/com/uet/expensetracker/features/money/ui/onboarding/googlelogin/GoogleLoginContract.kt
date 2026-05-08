package com.uet.expensetracker.features.money.ui.onboarding.googlelogin

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase

sealed interface GoogleLoginIntent : MviIntentBase {
    data object SignIn : GoogleLoginIntent
    data class GoogleSignIn(val idToken: String?) : GoogleLoginIntent
    data object Back : GoogleLoginIntent
}

data class GoogleLoginState(
    val isLoading: Boolean = false,
    val error: String? = null
) : MviStateBase

sealed interface GoogleLoginEvent : MviEventBase {
    data object LaunchSignInFlow : GoogleLoginEvent
    data object SignInSuccess : GoogleLoginEvent
    data class ShowError(val message: String) : GoogleLoginEvent
    data object NavigateBack : GoogleLoginEvent
}


