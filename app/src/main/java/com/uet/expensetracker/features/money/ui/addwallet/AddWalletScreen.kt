package com.uet.expensetracker.features.money.ui.addwallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import com.uet.expensetracker.features.money.ui.addtransaction.components.TransactionNumpad
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.utils.Constants
import com.uet.expensetracker.utils.getDrawableResId
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddWalletScreen(
    viewModel: AddWalletViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToCurrencyScreen: () -> Unit = {},
    onNavigateToIconPicker: () -> Unit = {},
    navController: NavController? = null
) {
    val state by viewModel.viewState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // If editing, load existing wallet


    val context = LocalContext.current
    // Get selected currency from navigation result
    navController?.let { nav ->
        LaunchedEffect(key1 = Unit) {
            // Listen for currency result from CurrencyScreen
            nav.currentBackStackEntry?.savedStateHandle?.getStateFlow<Currency?>(
                "selected_currency",
                null
            )
                ?.collect { currency ->
                    currency?.let { result ->
                        // Handle the received currency
                        viewModel.processIntent(AddWalletIntent.SelectCurrency(result))
                        // Remove the consumed result
                        nav.currentBackStackEntry?.savedStateHandle?.remove<Currency>("selected_currency")
                    }
                }
        }

        LaunchedEffect(key1 = Unit) {
            // Listen for icon selection from IconPickerScreen
            nav.currentBackStackEntry?.savedStateHandle?.getStateFlow<Int?>(
                Constants.SELECTED_ICON_KEY,
                null
            ).let { flow ->
                flow?.collect { iconRes ->
                    iconRes?.let {
                        val drawableName: String =
                            context.resources.getResourceEntryName(it)
                        viewModel.processIntent(AddWalletIntent.SelectIcon(drawableName))
                        nav.currentBackStackEntry?.savedStateHandle?.remove<Int>(Constants.SELECTED_ICON_KEY)
                    }
                }
            }
        }
    }

    
    // Handle events and show snackbars
    LaunchedEffect(key1 = true) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is AddWalletEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AddWalletEvent.ShowSuccess -> {
                    val msg = if (state.isEditMode) context.getString(R.string.add_wallet_updated_success) else context.getString(R.string.add_wallet_created_success)
                    snackbarHostState.showSnackbar(msg)
                    onNavigateBack()
                }
                is AddWalletEvent.NavigateToCurrencyScreen -> {
                    if (navController != null) navController.navigate(Screen.Currency.route)
                    else onNavigateToCurrencyScreen()
                }
                is AddWalletEvent.NavigateToIconPicker -> {
                    if (navController != null) navController.navigate(Screen.IconPicker.route)
                    else onNavigateToIconPicker()
                }
                is AddWalletEvent.NavigateBack -> onNavigateBack()
                else -> {}
            }
        }
    }

    AddWalletScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onWalletNameChange = { viewModel.processIntent(AddWalletIntent.UpdateWalletName(it)) },
        onNavigateBack = { viewModel.processIntent(AddWalletIntent.NavigateBack) },
        onIconClick = { viewModel.processIntent(AddWalletIntent.NavigateToIconPicker) },
        onSaveWallet = { viewModel.processIntent(AddWalletIntent.SaveWallet) },
        onCurrencyClick = { viewModel.processIntent(AddWalletIntent.NavigateToCurrencyScreen) },
        onToggleNumpad = { viewModel.processIntent(AddWalletIntent.ToggleNumpad) },
        onNumpadButtonClick = { viewModel.processIntent(AddWalletIntent.NumpadButtonPressed(it)) },
        onNotificationToggle = { viewModel.processIntent(AddWalletIntent.ToggleNotification(it)) },
        onExcludedFromTotalToggle = { viewModel.processIntent(AddWalletIntent.ToggleExcludedFromTotal(it)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletScreenContent(
    state: AddWalletState,
    snackbarHostState: SnackbarHostState,
    onWalletNameChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onIconClick: () -> Unit,
    onSaveWallet: () -> Unit,
    onCurrencyClick: () -> Unit,
    onToggleNumpad: () -> Unit,
    onNumpadButtonClick: (NumpadButton) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onExcludedFromTotalToggle: (Boolean) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) stringResource(R.string.add_wallet_edit_title) else stringResource(R.string.add_wallet_title), color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.add_wallet_close_cd), tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColor.Light.PrimaryColor.containerColor,
                    titleContentColor = AppColor.Light.PrimaryColor.contentColor,
                    navigationIconContentColor = AppColor.Light.PrimaryColor.contentColor
                )
            )
        },
        bottomBar = {
            Button(
                onClick = onSaveWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    contentColor = AppColor.Light.PrimaryColor.contentColor
                )
            ) {
                Text(
                    if (state.isEditMode) stringResource(R.string.add_wallet_update_button) else stringResource(R.string.add_wallet_save_button), 
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor,
    ) { paddingValues ->
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColor.Light.PrimaryColor.containerColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Icon/Name/Currency
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = getDrawableResId(LocalContext.current, state.iconString)),
                                contentDescription = "Wallet Icon",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable(onClick = onIconClick)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                                    .padding(16.dp)
                            ) {
                                BasicTextField(
                                    value = state.walletName,
                                    onValueChange = onWalletNameChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 20.sp,
                                        color = AppColor.Light.PrimaryColor.contentColor
                                    ),
                                    cursorBrush = SolidColor(AppColor.Light.PrimaryColor.TextButtonColor),
                                    decorationBox = { innerTextField ->
                                        if (state.walletName.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.add_wallet_name_label),
                                                style = TextStyle(
                                                    fontSize = 20.sp,
                                                    color = AppColor.Light.PrimaryColor.disabledContentColor
                                                )
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onCurrencyClick)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_currency),
                                contentDescription = stringResource(R.string.add_wallet_currency_icon_cd),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(text = state.currency?.currencyName ?: stringResource(R.string.add_wallet_currency_label), fontSize = 16.sp, color = TextMain)
                        }
                    }
                }

                // Section 2: Initial Balance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.add_wallet_initial_balance_label), style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .clickable(onClick = onToggleNumpad)
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

                // Section 3: Toggles
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = Color.White
//                    ),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                ) {
//                    Column(
//                        modifier = Modifier.padding(12.dp),
//                        verticalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text(stringResource(R.string.add_wallet_enable_notification_title), fontWeight = FontWeight.Bold)
//                                Text(
//                                    stringResource(R.string.add_wallet_enable_notification_description),
//                                    style = MaterialTheme.typography.bodySmall,
//                                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
//                                )
//                            }
//                            Switch(
//                                checked = state.enableNotification,
//                                onCheckedChange = onNotificationToggle,
//                                colors = SwitchDefaults.colors(
//                                    checkedThumbColor = Color.White,
//                                    checkedTrackColor = AppColor.Light.PrimaryColor.TextButtonColor
//                                )
//                            )
//                        }
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text(stringResource(R.string.add_wallet_excluded_from_total_title), fontWeight = FontWeight.Bold)
//                                Text(
//                                    stringResource(R.string.add_wallet_excluded_from_total_description),
//                                    style = MaterialTheme.typography.bodySmall,
//                                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
//
//                                )
//                            }
//                            Switch(
//                                checked = state.excludedFromTotal,
//                                onCheckedChange = onExcludedFromTotalToggle,
//                                colors = SwitchDefaults.colors(
//                                    checkedThumbColor = Color.White,
//                                    checkedTrackColor = AppColor.Light.PrimaryColor.TextButtonColor
//                                )
//                            )
//                        }
//                    }
//                }
            }
        }

        // Bottom sheet numpad
        if (state.showNumpad) {
            ModalBottomSheet(
                onDismissRequest = onToggleNumpad,
                sheetState = bottomSheetState,
                containerColor = Color.Transparent
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
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

@Preview
@Composable
fun AddWalletScreenContentPreview() {
    val previewState = AddWalletState(
        walletName = "My Wallet",
        initialBalance = "1000",
        enableNotification = true,
        excludedFromTotal = false
    )
    
    AddWalletScreenContent(
        state = previewState,
        onWalletNameChange = {},
        onNavigateBack = {},
        onIconClick = {},
        onSaveWallet = {},
        onCurrencyClick = {},
        onToggleNumpad = {},
        onNumpadButtonClick = {},
        onNotificationToggle = {},
        onExcludedFromTotalToggle = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}
