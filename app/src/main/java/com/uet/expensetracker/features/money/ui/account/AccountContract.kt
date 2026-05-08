package com.uet.expensetracker.features.money.ui.account

import android.content.Context
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviEventBase

/**
 * Defines the state, intents, and events for the Account screen following the MVI pattern.
 */

data class AccountState(
    val isSignedIn: Boolean = false,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isSigningIn: Boolean = false,
    val isSigningOut: Boolean = false,
    val isBackupLoading: Boolean = false,
    val isRestoreLoading: Boolean = false,
    val error: String? = null
) : MviStateBase

// User actions or intents on the Account screen
sealed interface AccountIntent : MviIntentBase {
    object LoadProfile : AccountIntent
    object SignIn : AccountIntent
    data class GoogleSignIn(val idToken: String?) : AccountIntent
    data class SignInError(val message: String) : AccountIntent
    object SignOut : AccountIntent
    data class SignOutWithBackup(val context: Context) : AccountIntent
    data class BackupData(val context: Context) : AccountIntent
    data class RestoreData(val context: Context) : AccountIntent
}

// One-time events emitted from the Account ViewModel
sealed interface AccountEvent : MviEventBase {
    object LaunchSignInFlow : AccountEvent
    object ShowPostLoginRestorePrompt : AccountEvent
    object SignOutSuccess : AccountEvent
    object BackupSuccess : AccountEvent
    object RestoreSuccess : AccountEvent
    data class ShowError(val message: String) : AccountEvent
}
