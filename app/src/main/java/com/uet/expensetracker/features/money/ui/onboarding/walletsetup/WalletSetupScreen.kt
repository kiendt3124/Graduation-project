package com.uet.expensetracker.features.money.ui.onboarding.walletsetup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.ui.theme.AppColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.uet.expensetracker.features.money.ui.addtransaction.components.TransactionNumpad
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import com.uet.expensetracker.ui.theme.TextMain

@Composable
fun WalletSetupScreen(
    navController: NavController,
    allowSkip: Boolean = false,
    viewModel: WalletSetupViewModel = hiltViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val savedStateHandle = navBackStackEntry?.savedStateHandle

    // Handle returned icon
    savedStateHandle?.getLiveData<Int>("selected_icon")
        ?.observe(lifecycleOwner) { iconRes ->
            viewModel.processIntent(WalletSetupIntent.IconPicked(iconRes))
            savedStateHandle.remove<Int>("selected_icon")
        }

    // Handle returned currency
    savedStateHandle?.getLiveData<com.uet.expensetracker.features.money.domain.model.Currency>("selected_currency")
        ?.observe(lifecycleOwner) { currency ->
            viewModel.processIntent(WalletSetupIntent.CurrencySelected(currency))
            savedStateHandle.remove<com.uet.expensetracker.features.money.domain.model.Currency>("selected_currency")
        }

    // Handle returned balance
    savedStateHandle?.getLiveData<String>("entered_amount")
        ?.observe(lifecycleOwner) { amount ->
            viewModel.processIntent(WalletSetupIntent.BalanceEntered(amount))
            savedStateHandle.remove<String>("entered_amount")
        }

    // Handle navigation events
    LaunchedEffect(viewModel.event) {
        viewModel.event.collect { event ->
            when (event) {
                is WalletSetupEvent.NavigateToIconPicker -> navController.navigate(Screen.IconPicker.route)
                is WalletSetupEvent.NavigateToCurrencyPicker -> navController.navigate(Screen.Currency.route)
                is WalletSetupEvent.NavigateToEnterAmount -> {
                    val currency = state.selectedCurrency
                    val route = Screen.EnterAmount.createRoute(
                        currencyId = currency?.id ?: 0,
                        currencyCode = currency?.currencyCode ?: "USD",
                        currencySymbol = currency?.symbol ?: "$"
                    )
                    navController.navigate(route)
                }
                is WalletSetupEvent.NavigateToHome -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.WalletSetup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                null -> Unit
            }
        }
    }

    WalletSetupScreenContent(
        state = state,
        allowSkip = allowSkip,
        onSkipSetup = { viewModel.processIntent(WalletSetupIntent.SkipSetup) },
        onChangeIcon = { viewModel.processIntent(WalletSetupIntent.ChangeIcon) },
        onUpdateName = { viewModel.processIntent(WalletSetupIntent.UpdateName(it)) },
        onChangeCurrency = { viewModel.processIntent(WalletSetupIntent.ChangeCurrency) },
        onChangeBalance = { viewModel.processIntent(WalletSetupIntent.ChangeBalance) },
        onConfirmSetup = { viewModel.processIntent(WalletSetupIntent.ConfirmSetup) },
        onNumpadButtonClick = { button ->
            viewModel.processIntent(
               WalletSetupIntent.NumpadButtonPressed(
                    button = button
               )
            )
        }
    )

    state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSetupScreenContent(
    state: WalletSetupState,
    allowSkip: Boolean,
    onSkipSetup: () -> Unit,
    onChangeIcon: () -> Unit,
    onUpdateName: (String) -> Unit,
    onChangeCurrency: () -> Unit,
    onChangeBalance: () -> Unit,
    onConfirmSetup: () -> Unit,
    onNumpadButtonClick: (NumpadButton) -> Unit
) {
    // Bottom-sheet state for numpad
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Scaffold(
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        topBar = {
            if (allowSkip) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    TextButton(
                        onClick = onSkipSetup,
                        enabled = !state.isRestoreLoading,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        if (state.isRestoreLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.common_skip),
                                color = AppColor.Light.PrimaryColor.contentColor
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Confirm button
            Button(
                onClick = onConfirmSetup,
                enabled = !state.isRestoreLoading &&
                    state.walletName.isNotBlank() &&
                    (state.formattedAmount.isNotBlank() && !state.formattedAmount.contentEquals(",.;+-x/")) &&
                    state.selectedCurrency != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    contentColor = Color.White,
                    disabledContainerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    disabledContentColor = AppColor.Light.PrimaryColor.TextButtonColor
                )
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = stringResource(R.string.wallet_setup_create_button), fontSize = 16.sp, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: title and description
            Column {
                Text(
                    text = stringResource(R.string.wallet_setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColor.Light.PrimaryColor.contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(R.string.wallet_setup_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }

            // Root Card containing icon, name, currency, and balance
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Icon section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    AppColor.Light.PrimaryColor.containerColor,
                                    CircleShape
                                )
                                .clickable { onChangeIcon() },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = state.selectedIcon),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.wallet_setup_change_icon),
                            color = TextMain,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Name section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.containerColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.wallet_setup_wallet_name_label),
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColor.Light.PrimaryColor.contentColor
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                                    .padding(16.dp)
                            ) {
                                BasicTextField(
                                    value = state.walletName,
                                    onValueChange = onUpdateName,
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.Light.PrimaryColor.contentColor),
                                    cursorBrush = SolidColor(AppColor.Light.PrimaryColor.TextButtonColor),
                                    decorationBox = { inner ->
                                        if (state.walletName.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.wallet_setup_wallet_name_placeholder),
                                                style = TextStyle(
                                                    fontSize = 20.sp,
                                                    color = AppColor.Light.PrimaryColor.disabledContentColor
                                                )
                                            )
                                        }
                                        inner()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Currency section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.containerColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.wallet_setup_currency_label),
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColor.Light.PrimaryColor.contentColor
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChangeCurrency() }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.selectedCurrency?.currencyName ?: stringResource(R.string.wallet_setup_choose_currency),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppColor.Light.PrimaryColor.contentColor
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AppColor.Light.PrimaryColor.contentColor,
                                    modifier = Modifier.rotate(-90f)
                                )
                            }
                        }
                    }

                    // Balance section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.containerColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.wallet_setup_initial_balance_label),
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColor.Light.PrimaryColor.contentColor
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                                    .clickable { onChangeBalance() }
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = state.formattedAmount,
                                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                    color = AppColor.Light.PrimaryColor.TextButtonColor
                                )
                            }
                        }
                    }
                }
            }


        }
    }
    // Render calculator numpad in a bottom sheet
    if (state.showNumpad) {
        ModalBottomSheet(
            onDismissRequest = onChangeBalance,
            sheetState = bottomSheetState,
            containerColor = Color.White
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.cardColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.wallet_setup_initial_balance_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColor.Light.PrimaryColor.contentColor
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .clickable { onChangeBalance() }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = state.formattedAmount,
                                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                color = AppColor.Light.PrimaryColor.TextButtonColor
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            AppColor.Light.NumpadColors.ButtonBackgroundColor,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .padding(8.dp)
                ) {
                    TransactionNumpad(
                        onButtonClick = onNumpadButtonClick,
                        showSaveButton = false
                    )
                }
            }

        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun WalletSetupScreenContentPreview() {
    ExpenseTrackerTheme {
        val sampleCurrency = com.uet.expensetracker.features.money.domain.model.Currency(
            id = 1,
            currencyName = "United States Dollar",
            currencyCode = "USD",
            symbol = "$",
            displayType = "",
            image = null
        )
        WalletSetupScreenContent(
            state = WalletSetupState(
                title = "",
                description = "",
                selectedIcon = R.drawable.ic_wallet,
                walletName = "",
                selectedCurrency = sampleCurrency,
                enteredAmount = "",
                isCreating = false,
                error = null
            ),
            allowSkip = false,
            onSkipSetup = {},
            onChangeIcon = {},
            onUpdateName = {},
            onChangeCurrency = {},
            onChangeBalance = {},
            onConfirmSetup = {},
            onNumpadButtonClick = {}
        )
    }
} 