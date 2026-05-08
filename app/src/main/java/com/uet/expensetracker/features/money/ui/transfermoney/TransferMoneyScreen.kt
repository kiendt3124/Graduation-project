package com.uet.expensetracker.features.money.ui.transfermoney

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.features.money.ui.transfermoney.components.*
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import java.util.*

@Composable
fun TransferMoneyScreen(
    viewModel: TransferMoneyViewModel = hiltViewModel(),
    navController: NavController = rememberNavController(),
    walletId: Int? = null
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    
    // Load wallets when screen is first displayed
    LaunchedEffect(key1 = Unit) {
        viewModel.processIntent(TransferMoneyIntent.LoadWallets(walletId))
    }

    // Handle events from ViewModel
    LaunchedEffect(key1 = Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is TransferMoneyEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                is TransferMoneyEvent.TransferCompleted -> {
                    Toast.makeText(context, context.getString(R.string.transfer_money_success), Toast.LENGTH_SHORT).show()
                }
                is TransferMoneyEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is TransferMoneyEvent.NavigateToEnterAmount -> {
                    // Navigate to EnterAmount screen with the currency from the FromWallet
                    state.fromWallet?.let { wallet ->
                        val route = Screen.EnterAmount.createRoute(
                            currencyId = wallet.currency.id,
                            currencyCode = wallet.currency.currencyCode,
                            currencySymbol = wallet.currency.symbol
                        )
                        navController.navigate(route)
                    }
                }
                null -> { /* Ignore null events */ }
            }
        }
    }

    // Handle navigation result from EnterAmountScreen
    LaunchedEffect(key1 = true) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.get<String>("entered_amount")?.let { amount ->
            // Parse the amount string to double
            try {
                val amountDouble = amount.toDouble()
                viewModel.processIntent(TransferMoneyIntent.UpdateAmount(amountDouble))
            } catch (e: NumberFormatException) {
                Toast.makeText(context, context.getString(R.string.transfer_money_error_invalid_amount), Toast.LENGTH_SHORT).show()
            }
            // Clear the saved state to avoid processing it again
            savedStateHandle.remove<String>("entered_amount")
        }
        
        savedStateHandle?.get<String>("formatted_amount")?.let { formattedAmount ->
            viewModel.processIntent(TransferMoneyIntent.UpdateFormattedAmount(formattedAmount))
            // Clear the saved state to avoid processing it again
            savedStateHandle.remove<String>("formatted_amount")
        }
        
        // Handle wallet selection result from ChooseWalletScreen
        savedStateHandle?.get<Wallet>("selected_wallet")?.let { wallet ->
            // Check if we were selecting 'from' or 'to' wallet
            viewModel.processIntent(
                if (state.lastWalletSelectionMode == "from") {
                    TransferMoneyIntent.SelectFromWallet(wallet.id)
                } else {
                    TransferMoneyIntent.SelectToWallet(wallet.id)
                }
            )
            // Clear the saved state to avoid processing it again
            savedStateHandle.remove<Wallet>("selected_wallet")
        }
    }

    TransferMoneyContent(
        state = state,
        onAmountClick = { viewModel.processIntent(TransferMoneyIntent.OpenEnterAmountScreen) },
        onNoteChange = { viewModel.processIntent(TransferMoneyIntent.UpdateNote(it)) },
        onFromWalletSelect = { 
            viewModel.processIntent(TransferMoneyIntent.SetWalletSelectionMode("from"))
            navController.navigate(Screen.ChooseWallet.createRoute(state.fromWallet?.id))
        },
        onToWalletSelect = { 
            viewModel.processIntent(TransferMoneyIntent.SetWalletSelectionMode("to"))
            navController.navigate(Screen.ChooseWallet.createRoute(state.toWallet?.id))
        },
        onDateChange = { viewModel.processIntent(TransferMoneyIntent.UpdateDate(it)) },
        onExcludeFromReportChange = { viewModel.processIntent(TransferMoneyIntent.UpdateExcludeFromReport(it)) },
        onAddTransferFeeChange = { viewModel.processIntent(TransferMoneyIntent.UpdateAddTransferFee(it)) },
        onTransferFeeChange = { viewModel.processIntent(TransferMoneyIntent.UpdateTransferFee(it)) },
        onSaveClick = { viewModel.processIntent(TransferMoneyIntent.SaveTransfer) },
        onCloseClick = { viewModel.processIntent(TransferMoneyIntent.Cancel) }
    )
}

@Composable
fun TransferMoneyContent(
    state: TransferMoneyState,
    onAmountClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onFromWalletSelect: (Int) -> Unit,
    onToWalletSelect: (Int) -> Unit,
    onDateChange: (Date) -> Unit,
    onExcludeFromReportChange: (Boolean) -> Unit,
    onAddTransferFeeChange: (Boolean) -> Unit,
    onTransferFeeChange: (Double) -> Unit,
    onSaveClick: () -> Unit,
    onCloseClick: () -> Unit,
    notePlaceholder: String = stringResource(R.string.transfer_money_note_placeholder)
) {
    val scrollState = rememberScrollState()
    
    // Common modifier for section containers
    val sectionModifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .fillMaxWidth()

    val insetTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val insetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TransferTopBar(
                onSaveClick = onSaveClick,
                onCloseClick = onCloseClick,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        },
        containerColor = Color(0xFFF6F6F6),
        modifier = Modifier.padding(top = insetTop)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                ) {
                    // From section
                    SectionHeader(title = stringResource(R.string.transfer_money_from_label))

                    // Group all "From" items in a single Surface
                    Surface(
                        modifier = sectionModifier,
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium.copy(CornerSize(12.dp))
                    ) {
                        Column(modifier = Modifier) {
                            // 1. Amount field - Replace with our custom AmountSelector
                            AmountSelector(
                                amount = state.formattedAmount.ifEmpty { 
                                    if (state.amount > 0) state.amount.toString() else "" 
                                },
                                currency = state.fromWallet?.currency,
                                onClick = onAmountClick
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

                            // 2. From wallet selector
                            WalletSelector(
                                wallets = state.wallets,
                                selectedWallet = state.fromWallet,
                                onWalletSelected = onFromWalletSelect
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

                            // 3. Category - Outgoing transfer item
                            TransferItem(
                                icon = Icons.Outlined.Email,
                                painter = painterResource(id = R.drawable.icon_142),
                                title = stringResource(R.string.transfer_money_outgoing_transfer)
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

                            // 4. Note field
                            TransferItem(
                                icon = Icons.Default.Email,
                                painter = painterResource(id = R.drawable.ic_description),
                                title = stringResource(R.string.transfer_money_note_label),
                                value = if (state.note.isNotEmpty()) state.note else null,
                                iconTint = Color.Black,
                                onClick = {
                                    // In a real app, show a dialog to enter note
                                    onNoteChange(notePlaceholder)
                                }
                            )
                        }
                    }

                    // To section
                    SectionHeader(title = stringResource(R.string.transfer_money_to_label))

                    // Group all "To" items in a single Surface
                    Surface(
                        modifier = sectionModifier,
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium.copy(CornerSize(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            // Check if to wallet is selected and has different category than from wallet
                            val showToAmount = state.toWallet != null &&
                                    state.fromWallet?.currency?.currencyCode != state.toWallet?.currency?.currencyCode
                            
                            // Show exchange rate if available
                            if (showToAmount && state.exchangeRate.isNotEmpty()) {
                                Text(
                                    text = state.exchangeRate,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                            }

                            // 1. Converted amount field (only if wallet is selected and currencies differ)
                            if (showToAmount) {
                                // Create a disabled amount selector for the destination amount
                                ToAmountDisplay(
                                    amount = state.formattedToAmount.ifEmpty { 
                                        if (state.toAmount > 0) state.toAmount.toString() else "" 
                                    },
                                    currency = state.toWallet?.currency
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                            }

                            // 2. To wallet selector
                            WalletSelector(
                                wallets = state.wallets.filter { it.id != state.fromWallet?.id },
                                selectedWallet = state.toWallet,
                                onWalletSelected = onToWalletSelect
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

                            // 3. Category - Incoming transfer item
                            TransferItem(
                                icon = Icons.Outlined.Email,
                                painter = painterResource(id = R.drawable.icon_143),
                                title = stringResource(R.string.transfer_money_incoming_transfer)
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

                            // 4. Note field (could be the same or different from "From" note)
                            TransferItem(
                                icon = Icons.Default.Email,
                                painter = painterResource(id = R.drawable.ic_description),
                                title = stringResource(R.string.transfer_money_note_label),
                                value = if (state.note.isNotEmpty()) state.note else null,
                                iconTint = Color.Black,
                                onClick = {
                                    // In a real app, show a dialog to enter note
                                    onNoteChange(notePlaceholder)
                                }
                            )
                        }
                    }

                    // Options section
//                    SectionHeader(title = stringResource(R.string.transfer_money_options_label))

                    // Group all "Options" items in a single Surface
//                    Surface(
//                        modifier = sectionModifier,
//                        color = Color.White,
//                        shape = MaterialTheme.shapes.medium.copy(CornerSize(12.dp))
//                    ) {
//                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
//                            // Date selector
//                            DateSelector(
//                                date = state.date,
//                                icon = painterResource(id = R.drawable.ic_calendar),
//                                iconTint = Color.Black,
//                                onDateChange = onDateChange
//                            )
//
//                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
//
//                            // Exclude from report toggle
//                            ToggleOption(
//                                title = stringResource(R.string.transfer_money_exclude_from_report_title),
//                                subtitle = stringResource(R.string.transfer_money_exclude_from_report_subtitle),
//                                checked = state.excludeFromReport,
//                                onCheckedChange = onExcludeFromReportChange
//                            )
//
//                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
//
//                            // Add transfer fee toggle
//                            ToggleOption(
//                                title = "Add transfer fee",
//                                checked = state.addTransferFee,
//                                onCheckedChange = onAddTransferFeeChange
//                            )
//
//                            // Transfer fee field (if enabled)
//                            if (state.addTransferFee) {
//                                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
//
//                                AmountTextField(
//                                    amount = state.transferFee,
//                                    currencySymbol = state.fromWallet?.currency?.symbol ?: "₫",
//                                    onAmountChange = onTransferFeeChange,
//                                    placeholder = stringResource(R.string.transfer_money_fee_amount_placeholder),
//                                    showOutline = false
//                                )
//                            }
//                        }
//                    }
                }
            }
        }
    }
}

@Composable
fun AmountSelector(
    amount: String,
    currency: Currency?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - Currency and amount label
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency image - could be an icon or flag
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF3A3A3C)),
                contentAlignment = Alignment.Center
            ) {
                currency?.image?.let { imageBytes ->
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = stringResource(R.string.transfer_money_currency_cd),
                        modifier = Modifier.size(24.dp)
                    )
                } ?: Text(
                    text = currency?.symbol ?: "$",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = stringResource(R.string.transfer_money_amount_label),
                color = TextMain,
                fontSize = 16.sp
            )
        }
        
        // Right side - Amount value and arrow
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (amount.isNotEmpty()) "${currency?.symbol ?: "$"} $amount" else stringResource(R.string.transfer_money_enter_amount_placeholder),
                color = if (amount.isNotEmpty()) TextMain else Color.Gray,
                fontSize = 16.sp,
                fontWeight = if (amount.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.transfer_money_select_amount_cd),
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun ToAmountDisplay(
    amount: String,
    currency: Currency?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - Currency and amount label
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency image - could be an icon or flag
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF3A3A3C)),
                contentAlignment = Alignment.Center
            ) {
                currency?.image?.let { imageBytes ->
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = stringResource(R.string.transfer_money_currency_cd),
                        modifier = Modifier.size(24.dp)
                    )
                } ?: Text(
                    text = currency?.symbol ?: "$",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = stringResource(R.string.transfer_money_converted_amount_label),
                color = TextMain,
                fontSize = 16.sp
            )
        }
        
        // Right side - Amount value
        Text(
            text = if (amount.isNotEmpty() && amount != "0.0") {
                // Check if it already contains a decimal point - if it does, assume it's already formatted
                if (amount.contains(".") || amount.contains(",")) {
                    "${currency?.symbol ?: "$"} $amount"
                } else {
                    try {
                        // Parse the amount and format it with decimal places
                        val amountValue = amount.toDouble()
                        "${currency?.symbol ?: "$"} ${com.uet.expensetracker.utils.formatAmount(amountValue, true)}"
                    } catch (e: NumberFormatException) {
                        "${currency?.symbol ?: "$"} $amount"
                    }
                }
            } else "-",
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransferMoneyContentPreview() {
    // Create preview data
    val currency = Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫"
    )
    
    val wallet1 = Wallet(
        id = 1,
        walletName = "Cash",
        currentBalance = 1000000.0,
        currency = currency
    )
    
    val wallet2 = Wallet(
        id = 2,
        walletName = "Bank Account",
        currentBalance = 5000000.0,
        currency = currency
    )
    
    val previewState = TransferMoneyState(
        fromWallet = wallet1,
        toWallet = wallet2,
        amount = 250000.0,
        formattedAmount = "250,000",
        wallets = listOf(wallet1, wallet2),
        excludeFromReport = true,
        addTransferFee = false
    )
    
    TransferMoneyContent(
        state = previewState,
        onAmountClick = {},
        onNoteChange = {},
        onFromWalletSelect = {},
        onToWalletSelect = {},
        onDateChange = {},
        onExcludeFromReportChange = {},
        onAddTransferFeeChange = {},
        onTransferFeeChange = {},
        onSaveClick = {},
        onCloseClick = {}
    )
}

// Update AmountTextField to include placeholder and outline control
@Composable
fun AmountTextField(
    amount: Double,
    currencySymbol: String,
    onAmountChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    placeholder: String = "",
    showOutline: Boolean = true
) {
    val textFieldValue = remember(amount) { 
        if (amount == 0.0) "" else amount.toString() 
    }
    
    val colors = if (showOutline) {
        OutlinedTextFieldDefaults.colors()
    } else {
        OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        )
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val newAmount = newValue.toDoubleOrNull() ?: 0.0
            onAmountChange(newAmount)
        },
        modifier = modifier.fillMaxWidth(),
        enabled = !readOnly,
        readOnly = readOnly,
        singleLine = true,
        placeholder = { 
            if (placeholder.isNotEmpty()) {
                Text(placeholder, color = Color.Gray)
            }
        },
        leadingIcon = { 
            Text(
                text = currencySymbol,
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        colors = colors,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
} 