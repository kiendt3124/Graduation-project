package com.uet.expensetracker.features.money.ui.mywallets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import com.uet.expensetracker.ui.theme.greenIndicator
import com.uet.expensetracker.ui.theme.onDarkSurface
import com.uet.expensetracker.utils.getDrawableResId
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWalletsScreen(
    viewModel: MyWalletsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAddWallet: () -> Unit = {},
    onNavigateToWalletDetail: (Int) -> Unit = {},
    onNavigateToEditWallet: (Int) -> Unit = {},
    onNavigateToTransferMoney: (Int) -> Unit = {}
) {
    val state by viewModel.viewState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedWalletForOptions by remember { mutableStateOf<Wallet?>(null) }
    val context = LocalContext.current
    
    // Collect events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            // Handle success events: reload list and show snackbar
            when (event) {
                is MyWalletsEvent.WalletDeleted -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.my_wallets_delete_success))
                }
                is MyWalletsEvent.MainWalletSet -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.my_wallets_set_main_success))
                }
                else -> {}
            }

            when (event) {
                is MyWalletsEvent.NavigateToAddWallet -> {
                    onNavigateToAddWallet()
                }
                is MyWalletsEvent.NavigateToWalletDetail -> {
                    onNavigateToWalletDetail(event.walletId)
                }
                is MyWalletsEvent.NavigateToEditWallet -> {
                    onNavigateToEditWallet(event.walletId)
                }
                is MyWalletsEvent.NavigateToTransferMoney -> {
                    onNavigateToTransferMoney(event.walletId)
                }
                is MyWalletsEvent.ShowError -> {
                    // Show error message
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }
    
    // Define callbacks
    val onBackClick = {
        onNavigateBack()
    }
    
    val onInfoClick = {
        // Handle info button click
    }

    val onEditClick = {
        // Handle edit button click
    }
    
    val onAddWalletClick = {
        showBottomSheet = true
    }
    
    val onWalletClick = { walletId: Int ->
        onNavigateToWalletDetail(walletId)
    }
    
    val onWalletMoreClick = { wallet: Wallet ->
        selectedWalletForOptions = wallet
        showOptionsSheet = true
    }
    
    val onWalletTypeSelected = { walletType: WalletType ->
        viewModel.processIntent(MyWalletsIntent.AddWallet(walletType))
        showBottomSheet = false
    }
    
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    MyWalletsScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBackClick,
        onInfoClick = onInfoClick,
        onEditClick = onEditClick,
        onAddWalletClick = onAddWalletClick,
        onWalletClick = onWalletClick,
        onWalletMoreClick = onWalletMoreClick
    )
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            containerColor = Color.White
        ) {
            AddWalletBottomSheetContent(onWalletTypeClick = onWalletTypeSelected)
        }
    }

    if (showOptionsSheet && selectedWalletForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppColor.Light.PrimaryColor.cardColor
        ) {
            OptionsBottomSheetContent(
                onSetDefault = {
                    viewModel.processIntent(MyWalletsIntent.SetMainWallet(selectedWalletForOptions!!.id))
                    showOptionsSheet = false
                },
                onEdit = {
                    viewModel.processIntent(MyWalletsIntent.EditWallet(selectedWalletForOptions!!.id))
                    showOptionsSheet = false
                },
                onDelete = {
                    showOptionsSheet = false
                    showConfirmDeleteDialog = true
                },
                onTransfer = {
                    viewModel.processIntent(MyWalletsIntent.TransferMoney(selectedWalletForOptions!!.id))
                    showOptionsSheet = false
                }
            )
        }
    }

    // Confirmation dialog before delete
    if (showConfirmDeleteDialog && selectedWalletForOptions != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text(stringResource(R.string.my_wallets_delete_confirm_title)) },
            text = { Text(stringResource(R.string.my_wallets_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.processIntent(MyWalletsIntent.DeleteWallet(selectedWalletForOptions!!.id))
                    showConfirmDeleteDialog = false
                }) {
                    Text(stringResource(R.string.my_wallets_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text(stringResource(R.string.my_wallets_cancel_button))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWalletsScreenContent(
    state: MyWalletsState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onInfoClick: () -> Unit,
    onEditClick: () -> Unit,
    onAddWalletClick: () -> Unit,
    onWalletClick: (Int) -> Unit,
    onWalletMoreClick: (Wallet) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.my_wallets_title),
                        color = TextMain,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.my_wallets_back_cd),
                            tint = TextMain
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = stringResource(R.string.my_wallets_info_cd),
                            tint = TextMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColor.Light.PrimaryColor.containerColor,
                    titleContentColor = AppColor.Light.PrimaryColor.contentColor,
                    navigationIconContentColor = AppColor.Light.PrimaryColor.contentColor,
                    actionIconContentColor = AppColor.Light.PrimaryColor.contentColor
                )
            )
        },
        bottomBar = {
            Button(
                onClick = onAddWalletClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    contentColor = AppColor.Light.PrimaryColor.contentColor
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.my_wallets_add_wallet_button), fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = greenIndicator)
            }
        } else {
            val scrollState = rememberScrollState()
            Card(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColor.Light.PrimaryColor.cardColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Error message
                        state.error?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                color = Color.Red,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Placeholder info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.my_wallets_empty_message),
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        // Total Balance
                        TotalBalanceCard(
                            icon = painterResource(id = R.drawable.ic_category_all),
                            totalBalance = "≈ ${formatCurrency(state.totalBalance)}"
                        )

                        // Wallet list
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                IncludedInTotalHeader()
                                state.wallets.forEach { wallet ->
                                    WalletItem(
                                        wallet = wallet,
                                        onClick = { onWalletClick(wallet.id) },
                                        onMoreClick = { onWalletMoreClick(wallet) }
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
}

@Composable
fun TotalBalanceCard(icon: Painter, totalBalance: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = icon,
                contentDescription = stringResource(R.string.my_wallets_total_balance_icon_cd),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.my_wallets_total_label),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    totalBalance,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
            }
        }
    }
}

@Composable
fun IncludedInTotalHeader() {
    Text(
        text = stringResource(R.string.my_wallets_included_in_total),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun WalletList(
    wallets: List<Wallet>,
    onWalletClick: (Int) -> Unit,
    onWalletMoreClick: (Wallet) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        items(wallets) { wallet ->
            WalletItem(
                wallet = wallet,
                onClick = { onWalletClick(wallet.id) },
                onMoreClick = { onWalletMoreClick(wallet) }
            )
            Divider(color = Color.DarkGray, thickness = 0.5.dp)
        }
    }
}

@Composable
fun WalletItem(
    wallet: Wallet,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val balanceText = formatCurrency(wallet.currentBalance, wallet.currency.symbol)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Remove main wallet indicator here; will display next to the wallet name

        // Get wallet icon resource ID dynamically
        val iconResId = getWalletIconResId(wallet.icon)
        
        Image(
            painter = painterResource(id = getDrawableResId(LocalContext.current, wallet.icon)), // Fallback icon
            contentDescription = wallet.walletName,
            modifier = Modifier.size(36.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    wallet.walletName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMain
                )
                if (wallet.isMainWallet) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box( modifier = Modifier
                        .border(BorderStroke(1.dp, greenIndicator), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.my_wallets_main_label),
                            fontSize = 10.sp,
                            color = greenIndicator,
                        )
                    }

                }
            }
            Text(
                text = balanceText,
                fontSize = 14.sp,
                color = if (wallet.currentBalance < 0) Color.Red else TextSecondary
            )
        }
        IconButton(onClick = onMoreClick) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.my_wallets_more_options_cd),
                tint = TextSecondary
            )
        }
    }
}

// Utility function to format currency
private fun formatCurrency(amount: Double, symbol: String = "₫"): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    numberFormat.minimumFractionDigits = 0
    numberFormat.maximumFractionDigits = 2
    
    return if (amount < 0) {
        "-${numberFormat.format(Math.abs(amount))} $symbol"
    } else {
        "${numberFormat.format(amount)} $symbol"
    }
}

// Utility function to get wallet icon resource ID
private fun getWalletIconResId(iconName: String): Int {
    // In a real app, you would map icon names to resource IDs
    // For now, just return a default icon
    return R.drawable.icon_10 // Using the same icon from TotalBalanceCard
}

@Preview(showBackground = true, name = "My Wallets Screen Content Preview", backgroundColor = 0xFF000000)
@Composable
fun MyWalletsScreenContentPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Create sample state with mock data
        val sampleState = MyWalletsState(
            isLoading = false,
            wallets = listOf(
                Wallet(
                    id = 1,
                    walletName = "Ví Vip",
                    currentBalance = 1000000.0,
                    currency = com.uet.expensetracker.features.money.domain.model.Currency(
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
                    currency = com.uet.expensetracker.features.money.domain.model.Currency(
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
                    currency = com.uet.expensetracker.features.money.domain.model.Currency(
                        id = 1,
                        currencyName = "Vietnamese Dong",
                        currencyCode = "VND",
                        symbol = "₫",
                        displayType = "suffix"
                    ),
                    icon = "img_wallet_basic"
                )
            ),
            totalBalance = 994946.93,
            error = null
        )
        
        // Render the content with mock data and empty callbacks
        MyWalletsScreenContent(
            state = sampleState,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onInfoClick = {},
            onEditClick = {},
            onAddWalletClick = {},
            onWalletClick = {},
            onWalletMoreClick = {}
        )
    }
}

@Preview(showBackground = true, name = "My Wallets Screen Loading Preview", backgroundColor = 0xFF000000)
@Composable
fun MyWalletsScreenLoadingPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Create loading state
        val loadingState = MyWalletsState(
            isLoading = true
        )
        
        // Render the content with loading state
        MyWalletsScreenContent(
            state = loadingState,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onInfoClick = {},
            onEditClick = {},
            onAddWalletClick = {},
            onWalletClick = {},
            onWalletMoreClick = {}
        )
    }
}

@Preview(showBackground = true, name = "My Wallets Screen Error Preview", backgroundColor = 0xFF000000)
@Composable
fun MyWalletsScreenErrorPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Create error state
        val errorState = MyWalletsState(
            isLoading = false,
            error = "Failed to load wallets: Network Error"
        )
        
        // Render the content with error state
        MyWalletsScreenContent(
            state = errorState,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onInfoClick = {},
            onEditClick = {},
            onAddWalletClick = {},
            onWalletClick = {},
            onWalletMoreClick = {}
        )
    }
}

@Composable
fun AddWalletBottomSheetContent(onWalletTypeClick: (WalletType) -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        Text(
            text = stringResource(R.string.add_wallet_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Wallet options in a 2x2 grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            WalletTypeCard(
                title = WalletType.BASIC.displayName.replace(" ", "\n"),
                color = Color(0xFF4CAF50), // Green
                onClick = { onWalletTypeClick(WalletType.BASIC) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            WalletTypeCard(
                title = WalletType.LINKED.displayName.replace(" ", "\n"),
                color = Color(0xFF00ACC1), // Cyan
                onClick = { onWalletTypeClick(WalletType.LINKED) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            WalletTypeCard(
                title = "${WalletType.CREDIT.displayName.replace(" ", "\n")}\n(Beta)",
                color = Color(0xFFE91E63), // Pink
                onClick = { onWalletTypeClick(WalletType.CREDIT) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            WalletTypeCard(
                title = WalletType.GOAL.displayName.replace(" ", "\n"),
                color = Color(0xFFEF5350), // Red
                onClick = { onWalletTypeClick(WalletType.GOAL) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
    }
}

@Composable
fun WalletTypeCard(title: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(1.5f) // Adjust aspect ratio as needed
            .clip(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painterResource(id = R.drawable.ic_basic_wallet_v2), // Placeholder for question mark
                contentDescription = stringResource(R.string.my_wallets_info_cd),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp, // Adjust as needed
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

@Preview( name = "Add Wallet BottomSheet Preview")
@Composable
fun AddWalletBottomSheetContentPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) { // Ensuring dark theme for preview
        AddWalletBottomSheetContent(onWalletTypeClick = {})
    }
}

@Preview
@Composable
fun OptionsBottomSheetContentPreview() {
    OptionsBottomSheetContent(
        onSetDefault = {},
        onEdit = {},
        onDelete = {},
        onTransfer = {}
    )
}

@Composable
private fun OptionsBottomSheetContent(
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTransfer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.my_wallets_options_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Set as main wallet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSetDefault),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.my_wallets_set_as_main),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        // Edit wallet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.my_wallets_edit_wallet),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        // Delete wallet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDelete),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.my_wallets_delete_wallet),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        // Transfer money
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTransfer),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.my_wallets_transfer_money),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
