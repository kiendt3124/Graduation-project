package com.uet.expensetracker.features.money.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.uet.expensetracker.features.money.ui.account.components.AccountContent
import kotlinx.coroutines.flow.collectLatest
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.uet.expensetracker.R
import com.uet.expensetracker.utils.handleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToDebts: () -> Unit = {},
    onNavigateToWallets: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {}
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val showPostLoginRestorePrompt = remember { mutableStateOf(false) }
    val request = remember {
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId(context.getString(R.string.default_web_client_id))
            // Only show accounts previously used to sign in.
            .setFilterByAuthorizedAccounts(false)
            .build()

        Log.d("signin", "serverClientId=${context.getString(R.string.default_web_client_id)}")

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        request

    }

    // Load profile when screen appears
    LaunchedEffect(Unit) {
        viewModel.processIntent(AccountIntent.LoadProfile)
    }

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                AccountEvent.LaunchSignInFlow -> {
                    launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val result = credentialManager.getCredential(
                                context = context,
                                request = request
                            )

                        handleSignIn(
                            credential = result.credential,
                            firebaseAuthWithGoogle = {
                                viewModel.processIntent(AccountIntent.GoogleSignIn(it))
                            },
                            errorHandler = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                        } catch (e: GetCredentialException) {
                            Log.e("signin", "error: ${e.message}")
                            e.printStackTrace()
                        }

                    }


                }

                is AccountEvent.ShowError -> {
                    // TODO: Show error message to user
                    Toast.makeText(
                        context,
                        context.getString(R.string.common_error_format, event.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                AccountEvent.ShowPostLoginRestorePrompt -> {
                    showPostLoginRestorePrompt.value = true
                }

                AccountEvent.SignOutSuccess -> {
                    viewModel.processIntent(AccountIntent.LoadProfile)
                }

                AccountEvent.BackupSuccess -> {
                    // TODO: Inform user of backup success
                    Toast.makeText(context, context.getString(R.string.account_backup_success), Toast.LENGTH_SHORT).show()
                }

                AccountEvent.RestoreSuccess -> {
                    // TODO: Inform user of restore success
                    Toast.makeText(context, context.getString(R.string.account_restore_success), Toast.LENGTH_SHORT).show()
                    if (showPostLoginRestorePrompt.value) {
                        showPostLoginRestorePrompt.value = false
                    }
                }

                null -> {}
            }
        }
    }

    if (showPostLoginRestorePrompt.value) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isRestoreLoading) showPostLoginRestorePrompt.value = false
            },
            title = { Text(stringResource(R.string.account_restore_prompt_title)) },
            text = {
                if (state.isRestoreLoading) {
                    Text(stringResource(R.string.account_restore_prompt_restoring))
                } else {
                    Text(stringResource(R.string.account_restore_prompt_body))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isRestoreLoading,
                    onClick = { viewModel.processIntent(AccountIntent.RestoreData(context)) }
                ) {
                    if (state.isRestoreLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.account_restore_action))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isRestoreLoading,
                    onClick = { showPostLoginRestorePrompt.value = false }
                ) {
                    Text(stringResource(R.string.common_skip))
                }
            }
        )
    }

    AccountContent(
        state = state,
        onIntent = { intent -> viewModel.processIntent(intent) },
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToDebts = onNavigateToDebts,
        onNavigateToWallet = onNavigateToWallets,
        onNavigateToAiChat = onNavigateToAiChat,
        modifier = modifier
    )
}
