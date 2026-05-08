package com.uet.expensetracker.features.money.ui.enteramount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import com.uet.expensetracker.features.money.ui.addtransaction.components.TransactionNumpad
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary

@Composable
fun EnterAmountScreen(
    onNavigateBack: (amount: String, formattedAmount: String) -> Unit = { _, _ -> },
    initialCurrency: Currency = Currency(
        id = 0,
        currencyName = "US Dollar",
        currencyCode = "USD",
        symbol = "$"
    )
) {
    // Create the ViewModel with Hilt
    val viewModel: EnterAmountViewModel = hiltViewModel()
    
    // Set currency from the parameter
    LaunchedEffect(initialCurrency) {
        viewModel.setCurrency(initialCurrency)
    }
    
    // Observe state
    val state by viewModel.viewState.collectAsState()
    
    // Create a SnackbarHostState to show errors
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe events
    LaunchedEffect(true) {
        viewModel.event.collect { event ->
            when (event) {
                is EnterAmountEvent.NavigateBack -> {
                    onNavigateBack(event.amount, event.formattedAmount)
                }
                is EnterAmountEvent.ShowError -> {
                    // Show error in Snackbar with appropriate color based on level
                    val message = event.message
                    snackbarHostState.showSnackbar(message)
                }
                null -> {} // Ignore null events
            }
        }
    }
    
    // Handle user intents
    val onSave = {
        viewModel.processIntent(EnterAmountIntent.SaveAmount)
    }
    
    val onNumpadClick = { button: NumpadButton ->
        viewModel.processIntent(EnterAmountIntent.NumpadButtonPressed(button))
    }
    
    // Render UI
    EnterAmountScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onSave = onSave,
        onNumpadButtonClick = onNumpadClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterAmountScreenContent(
    state: EnterAmountState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSave: () -> Unit = {},
    onNumpadButtonClick: (NumpadButton) -> Unit = {}
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.enter_amount_title), color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.enter_amount_save_cd),
                            tint = AppColor.Light.PrimaryColor.TextButtonColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColor.Light.NumpadColors.ButtonBackgroundColor
                )
            )
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColor.Light.PrimaryColor.containerColor),
            horizontalAlignment = Alignment.End
        ) {
            // Amount display area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Currency code indicator
                Text(
                    text = state.currency.currencyCode,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Currency Symbol
                    Text(
                        text = state.currency.symbol,
                        fontSize = 32.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = state.formattedAmount,
                        fontSize = 48.sp,
                        color = TextMain
                    )
                }
            }

            // Numpad
            TransactionNumpad(
                onButtonClick = { button -> onNumpadButtonClick(button) },
                showSaveButton = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6F6)
@Composable
fun EnterAmountScreenPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColor.Light.PrimaryColor.containerColor
    ) {
        EnterAmountScreenContent(
            state = EnterAmountState(
                amount = "500000",
                formattedAmount = "500,000",
                currency = Currency(
                    id = 0,
                    currencyName = "US Dollar",
                    currencyCode = "USD",
                    symbol = "$"
                )
            )
        )
    }
}
