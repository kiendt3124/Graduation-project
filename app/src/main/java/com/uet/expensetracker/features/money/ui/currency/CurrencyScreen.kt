package com.uet.expensetracker.features.money.ui.currency

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.data.data_source.local.CurrencyDataSource
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    onBackClick: () -> Unit = {},
    onCurrencySelected: (Currency) -> Unit = {}
) {
    val viewModel: CurrencyViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.processIntent(CurrencyIntent.LoadCurrencies)
    }
    LaunchedEffect(viewModel.event) {
        viewModel.event.collect { event ->
            when (event) {
                is CurrencyEvent.CurrencySelected -> onCurrencySelected(event.currency)
                is CurrencyEvent.ShowError -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            CurrencyAppBar(
                onBackClick = onBackClick,
                onSearchClick = { /* TODO: Implement search */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CurrencyList(
                modifier = Modifier.padding(paddingValues),
                currencies = state.currencies,
                selectedCurrencyCode = state.selectedCurrencyCode ?: "",
                onCurrencyClick = { currency ->
                    viewModel.processIntent(CurrencyIntent.SelectCurrency(currency))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyAppBar(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.add_account_currency_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            titleContentColor = TextMain,
            navigationIconContentColor = TextMain,
            actionIconContentColor = TextMain
        )
    )
}

@Composable
fun CurrencyList(
    modifier: Modifier = Modifier,
    currencies: List<Currency>,
    selectedCurrencyCode: String,
    onCurrencyClick: (Currency) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(currencies, key = { it.id }) { currency ->
            CurrencyItem(
                currency = currency,
                isSelected = currency.currencyCode == selectedCurrencyCode,
                onClick = { onCurrencyClick(currency) }
            )
            HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun CurrencyItem(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val drawableName = "ic_currency_${currency.currencyCode.lowercase()}"
    val drawableId = remember(drawableName) {
        val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        if (id == 0) R.drawable.ic_currency else id
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = "${currency.currencyName} flag",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.currencyName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currency.symbol.ifEmpty { currency.currencyCode },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
fun CurrencyScreenPreview() {
    ExpenseTrackerTheme(darkTheme = true) {
        val previewCurrencies = listOf(
            Currency(1, "United States Dollar", "USD", "$", "0"),
            Currency(2, "Pound", "GBP", "£", "0"),
            Currency(3, "Euro", "EUR", "€", "0"),
            Currency(4, "Việt Nam Đồng", "VND", "₫", "1"),
            Currency(5, "Yuan Renminbi", "CNY", "¥", "0"),
            Currency(81, "Afghan afghani", "AFN", "AFN", "0"),
            Currency(82, "Albanian lek", "ALL", "ALL", "0"),
            Currency(19, "Algerian Dinar", "DZD", "DZD", "0"),
            Currency(83, "Angolan kwanza", "AOA", "AOA", "0"),
            Currency(22, "Argentina Pesos", "ARS", "$", "0"),
            Currency(85, "Armenian dram", "AMD", "AMD", "0")
        )

        var selectedCode by remember { mutableStateOf("VND") }

        Scaffold(
            topBar = {
                CurrencyAppBar(
                    onBackClick = {},
                    onSearchClick = {}
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            CurrencyList(
                modifier = Modifier.padding(paddingValues),
                currencies = previewCurrencies,
                selectedCurrencyCode = selectedCode,
                onCurrencyClick = { selectedCode = it.currencyCode }
            )
        }
    }
}

@Preview()
@Composable
fun CurrencyItemPreview() {
    ExpenseTrackerTheme {
        val currency = Currency(1, "United States Dollar", "USD", "$", "0")
        Column {
            CurrencyItem(currency = currency, isSelected = false, onClick = {})
            HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray.copy(alpha = 0.5f))
            CurrencyItem(
                currency = Currency(4, "Việt Nam Đồng", "VND", "₫", "1"),
                isSelected = true,
                onClick = {})
            HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray.copy(alpha = 0.5f))
            CurrencyItem(
                currency = Currency(81, "Afghan afghani", "AFN", "AFN", "0"),
                isSelected = false,
                onClick = {})
        }
    }
}