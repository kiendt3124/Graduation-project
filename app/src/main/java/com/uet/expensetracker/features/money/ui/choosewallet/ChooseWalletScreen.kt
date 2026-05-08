package com.uet.expensetracker.features.money.ui.choosewallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.onDarkSurface
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.greenIndicator
import androidx.navigation.NavController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.ScreenBgMain
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.utils.getDrawableResId

@Composable
fun ChooseWalletScreen(
    viewModel: ChooseWalletViewModel = hiltViewModel(),
    initialWalletId: Int? = null,
    showTotalWallet: Boolean = true, // Control whether to show Total Wallet
    onWalletSelected: (Wallet, Boolean) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit,
    navController: NavController? = null
) {
    val state by viewModel.viewState.collectAsState()
    
    // Debugging log to check current route and destination
    val currentRoute = navController?.currentBackStackEntry?.destination?.route
    val previousRoute = navController?.previousBackStackEntry?.destination?.route
    
    android.util.Log.d("ChooseWalletScreen", "Current route: $currentRoute, Previous route: $previousRoute")
    
    // Set initial wallet selection and load wallets
    LaunchedEffect(Unit) {
        // Set whether to show Total Wallet
        viewModel.processIntent(ChooseWalletIntent.SetShowTotalWallet(showTotalWallet))
        
        // Pre-select total wallet (id = -1) or specific wallet if provided
        initialWalletId?.let { id ->
            if (id == -1 && showTotalWallet) {
                viewModel.processIntent(ChooseWalletIntent.SelectTotalWallet)
            } else {
                viewModel.processIntent(ChooseWalletIntent.SelectWallet(id))
            }
        }
        viewModel.processIntent(ChooseWalletIntent.LoadWallets)
    }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ChooseWalletEvent.WalletSelected -> {
                    val selectedWalletId = event.wallet.id
                    val isTotalWallet = event.isTotalWallet
                    
                    // Set the selected wallet ID in the previous backstack entry
                    navController?.previousBackStackEntry?.savedStateHandle?.set("selected_wallet_id", selectedWalletId)
                    android.util.Log.d("ChooseWalletScreen", "WalletSelected event - Setting selected_wallet_id to $selectedWalletId, isTotalWallet: $isTotalWallet")
                    
                    // Set the is_total_wallet flag
                    navController?.previousBackStackEntry?.savedStateHandle?.set("is_total_wallet", isTotalWallet)
                    
                    // Also set the whole wallet object for compatibility
                    navController?.previousBackStackEntry?.savedStateHandle?.set("selected_wallet", event.wallet)
                    
                    // Call the callback if provided
                    onWalletSelected(event.wallet, isTotalWallet)
                    
                    // Navigate back directly using popBackStack which should return to previous screen
                    android.util.Log.d("ChooseWalletScreen", "Popping back stack to return to previous screen")
                    navController?.popBackStack() ?: onNavigateBack()
                }
                is ChooseWalletEvent.NavigateBack -> {
                    android.util.Log.d("ChooseWalletScreen", "NavigateBack event - Popping back stack")
                    navController?.popBackStack() ?: onNavigateBack()
                }
                is ChooseWalletEvent.ShowError -> {
                    // Show error (could use a snackbar here)
                    android.util.Log.e("ChooseWalletScreen", "Error: ${event.message}")
                }
                null -> {}
            }
        }
    }
    
    // If we're on a bad route, handle it directly
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("ChooseWalletScreen", "ChooseWalletScreen disposed")
        }
    }
    
    ChooseWalletScreenContent(
        state = state,
        onWalletClick = { walletId ->
            viewModel.processIntent(ChooseWalletIntent.SelectWallet(walletId))
            viewModel.processIntent(ChooseWalletIntent.ConfirmSelection)
        },
        onTotalWalletClick = {
            viewModel.processIntent(ChooseWalletIntent.SelectTotalWallet)
            viewModel.processIntent(ChooseWalletIntent.ConfirmSelection)
        },
        onBackClick = {
            viewModel.processIntent(ChooseWalletIntent.Cancel)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseWalletScreenContent(
    state: ChooseWalletState,
    onWalletClick: (Int) -> Unit,
    onTotalWalletClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_wallet_title), color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.choose_wallet_back_cd), tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColor.Light.PrimaryColor.containerColor)
            )
        },
        containerColor = ScreenBgMain
    ) { paddingValues ->
        Card(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ScreenBgMain),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = greenIndicator)
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.error?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = Color.Red,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = stringResource(R.string.choose_wallet_select_wallet),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2B3B48),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    // Highlight wallets in a separate card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            // Total wallet item - only show if enabled
                            if (state.showTotalWallet) {
                                state.totalWallet?.let { totalWallet ->
                                    TotalWalletItem(
                                        wallet = totalWallet,
                                        isSelected = state.selectedWalletId == totalWallet.id,
                                        onClick = onTotalWalletClick
                                    )
                                    Divider(color = Color.DarkGray, thickness = 0.5.dp)
                                }
                            }
                            // Regular wallets
                            state.wallets.forEach { wallet ->
                                WalletItem(
                                    wallet = wallet,
                                    isSelected = wallet.id == state.selectedWalletId,
                                    onClick = { onWalletClick(wallet.id) }
                                )
                                Divider(color = Color.DarkGray, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletList(
    wallets: List<Wallet>,
    totalWallet: Wallet?,
    selectedWalletId: Int?,
    showTotalWallet: Boolean = true,
    onWalletClick: (Int) -> Unit,
    onTotalWalletClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Add Total Wallet item at the top - only if enabled
        if (showTotalWallet) {
            totalWallet?.let { wallet ->
                item(key = "total_wallet") {
                    TotalWalletItem(
                        wallet = wallet,
                        isSelected = selectedWalletId == wallet.id,
                        onClick = onTotalWalletClick
                    )
                    Divider(color = Color.DarkGray, thickness = 1.dp)
                }
            }
        }
        
        items(wallets) { wallet ->
            WalletItem(
                wallet = wallet,
                isSelected = wallet.id == selectedWalletId,
                onClick = { onWalletClick(wallet.id) }
            )
            Divider(color = Color.DarkGray, thickness = 0.5.dp)
        }
    }
}

@Composable
fun TotalWalletItem(
    wallet: Wallet,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wallet icon
        Image(
            painterResource(id = R.drawable.ic_category_all),
            contentDescription = stringResource(R.string.choose_wallet_all_wallets_cd),
            modifier = Modifier.size(36.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Wallet details
        Column(modifier = Modifier.weight(1f)) {
            Text(wallet.walletName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = onDarkSurface)
            Text(
                text = formatCurrency(wallet.currentBalance, wallet.currency.symbol),
                fontSize = 14.sp,
                color = if (wallet.currentBalance < 0) Color.Red else onDarkSurface
            )
        }
        
        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.choose_wallet_selected_cd),
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun WalletItem(
    wallet: Wallet,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wallet icon
        Image(
            painter = painterResource(id = getDrawableResId(LocalContext.current, wallet.icon)),
            contentDescription = wallet.walletName,
            modifier = Modifier.size(36.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Wallet details
        Column(modifier = Modifier.weight(1f)) {
            Text(wallet.walletName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = onDarkSurface)
            Text(
                text = formatCurrency(wallet.currentBalance, wallet.currency.symbol),
                fontSize = 14.sp,
                color = if (wallet.currentBalance < 0) Color.Red else onDarkSurface
            )
        }
        
        // Selection indicator
        if (isSelected) {
            Image(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = stringResource(R.string.choose_wallet_selected_cd),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Utility function to format currency (reused from MyWalletsScreen)
private fun formatCurrency(amount: Double, symbol: String = "₫"): String {
    val numberFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault())
    numberFormat.minimumFractionDigits = 0
    numberFormat.maximumFractionDigits = 2
    
    return if (amount < 0) {
        "-${numberFormat.format(Math.abs(amount))} $symbol"
    } else {
        "${numberFormat.format(amount)} $symbol"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ChooseWalletScreenContentPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Create sample state with mock data
        val sampleState = ChooseWalletState(
            isLoading = false,
            wallets = listOf(
                Wallet(
                    id = 1,
                    walletName = "Ví Vip",
                    currentBalance = 1000000.0,
                    currency = Currency(
                        id = 1,
                        currencyName = "Vietnamese Dong",
                        currencyCode = "VND",
                        symbol = "₫",
                        displayType = "suffix"
                    ),
                    icon = "img_wallet_default_widget",
                    isMainWallet = true
                ),
                Wallet(
                    id = 2,
                    walletName = "Crypto",
                    currentBalance = -53.07,
                    currency = Currency(
                        id = 2,
                        currencyName = "US Dollar",
                        currencyCode = "USD",
                        symbol = "$",
                        displayType = "prefix"
                    ),
                    icon = "img_wallet_crypto"
                ),
                Wallet(
                    id = 3,
                    walletName = "Basic",
                    currentBalance = -5000.0,
                    currency = Currency(
                        id = 1,
                        currencyName = "Vietnamese Dong",
                        currencyCode = "VND",
                        symbol = "₫",
                        displayType = "suffix"
                    ),
                    icon = "img_wallet_basic"
                )
            ),
            totalWallet = Wallet(
                id = -1,
                walletName = "All Wallets",
                currentBalance = 994946.93,
                currency = Currency(
                    id = 1,
                    currencyName = "Vietnamese Dong",
                    currencyCode = "VND",
                    symbol = "₫",
                    displayType = "suffix"
                ),
                icon = "img_total_wallet"
            ),
            selectedWalletId = 2
        )
        
        // Render the content with mock data
        ChooseWalletScreenContent(
            state = sampleState,
            onWalletClick = {},
            onTotalWalletClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ChooseWalletScreenLoadingPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Create loading state
        val loadingState = ChooseWalletState(
            isLoading = true
        )
        
        // Render the content with loading state
        ChooseWalletScreenContent(
            state = loadingState,
            onWalletClick = {},
            onTotalWalletClick = {},
            onBackClick = {}
        )
    }
} 