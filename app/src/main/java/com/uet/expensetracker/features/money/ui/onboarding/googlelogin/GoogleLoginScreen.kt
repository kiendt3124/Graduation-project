package com.uet.expensetracker.features.money.ui.onboarding.googlelogin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.utils.handleSignIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun GoogleLoginScreen(
    navController: NavController,
    viewModel: GoogleLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val request = remember {
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId(context.getString(R.string.default_web_client_id))
            // Allow picking any Google account on device.
            .setFilterByAuthorizedAccounts(false)
            .build()

        Log.d("google_login", "serverClientId=${context.getString(R.string.default_web_client_id)}")

        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                GoogleLoginEvent.LaunchSignInFlow -> {
                    scope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val result = credentialManager.getCredential(
                                context = context,
                                request = request
                            )

                            handleSignIn(
                                credential = result.credential,
                                firebaseAuthWithGoogle = { idToken ->
                                    viewModel.processIntent(GoogleLoginIntent.GoogleSignIn(idToken))
                                },
                                errorHandler = { msg ->
                                    viewModel.processIntent(GoogleLoginIntent.GoogleSignIn(null))
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                            Log.e("signin", "error: No credentials available", e)
                            Toast.makeText(
                                context,
                                "Vui lòng thêm tài khoản Google vào thiết bị của bạn trong Cài đặt > Tài khoản",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: GetCredentialException) {
                            Log.e("google_login", "Credential error: ${e.message}", e)
                            Toast.makeText(
                                context,
                                e.message ?: context.getString(R.string.google_login_sign_in_canceled),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e("google_login", "Sign-in error: ${e.message}", e)
                            Toast.makeText(
                                context,
                                e.message ?: context.getString(R.string.google_login_sign_in_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                GoogleLoginEvent.SignInSuccess -> {
                    navController.navigate(Screen.WalletSetup.createRoute(allowSkip = true)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                is GoogleLoginEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                GoogleLoginEvent.NavigateBack -> navController.popBackStack()
                null -> Unit
            }
        }
    }

    GoogleLoginContent(
        isLoading = state.isLoading,
        onBack = { viewModel.processIntent(GoogleLoginIntent.Back) },
        onGoogleSignIn = { viewModel.processIntent(GoogleLoginIntent.SignIn) }
    )
}

@Composable
private fun GoogleLoginContent(
    isLoading: Boolean,
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    Scaffold(
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = AppColor.Light.PrimaryColor.contentColor
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = AppColor.Light.PrimaryColor.containerColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColor.Light.PrimaryColor.TextButtonColor
                        )
                    }

                    Text(
                        text = stringResource(R.string.google_login_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColor.Light.PrimaryColor.contentColor
                    )

                    Text(
                        text = stringResource(R.string.google_login_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onGoogleSignIn,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = stringResource(R.string.google_login_button), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(onClick = onBack, enabled = !isLoading) {
                        Text(text = stringResource(R.string.common_back), color = AppColor.Light.PrimaryColor.contentColor)
                    }
                }
            }
        }
    }
}


