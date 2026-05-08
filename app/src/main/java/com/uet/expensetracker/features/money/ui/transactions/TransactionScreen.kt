package com.uet.expensetracker.features.money.ui.transactions

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.transactions.components.DailyTransactionHeader
import com.uet.expensetracker.features.money.ui.transactions.components.EmptyTransactionsState
import com.uet.expensetracker.features.money.ui.transactions.components.MonthSelectionTabs
import com.uet.expensetracker.features.money.ui.transactions.components.SummarySection
import com.uet.expensetracker.features.money.ui.transactions.components.TransactionItem
import com.uet.expensetracker.features.money.ui.transactions.components.WalletSelector
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.utils.formatCurrency
import java.util.Calendar
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import androidx.compose.foundation.clickable
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsState()
    val currentRoute = navController?.currentBackStackEntry?.destination?.route

    android.util.Log.d("TransactionScreen", "Screen rendered with current route: $currentRoute")

    // Handle results from ChooseWalletScreen
    LaunchedEffect(navController?.currentBackStackEntry?.savedStateHandle) {
        // Debug the current back stack entry's saved state
        val savedStateHandle = navController?.currentBackStackEntry?.savedStateHandle
        val hasWalletId = savedStateHandle?.contains("selected_wallet_id") ?: false
        android.util.Log.d(
            "TransactionScreen",
            "Checking savedStateHandle for selected_wallet_id, contains: $hasWalletId"
        )

        // Check for wallet selection result
        savedStateHandle?.get<Int>("selected_wallet_id")?.let { walletId ->
            // Get the isTotalWallet flag
            val isTotalWallet = savedStateHandle.get<Boolean>("is_total_wallet") ?: false
            android.util.Log.d(
                "TransactionScreen",
                "Retrieved selected_wallet_id: $walletId, isTotalWallet: $isTotalWallet"
            )

            // Clear the results to avoid processing them multiple times
            savedStateHandle.remove<Int>("selected_wallet_id")
            savedStateHandle.remove<Boolean>("is_total_wallet")

            // Process the selected wallet
            if (walletId != 0) {
                android.util.Log.d(
                    "TransactionScreen",
                    "Processing wallet selection: $walletId with isTotalWallet: $isTotalWallet"
                )
                viewModel.processIntent(TransactionIntent.SelectWallet(walletId, isTotalWallet))
            }
        }
    }

    // Create callback functions to handle UI events
    val onTabSelected: (Int) -> Unit = { index ->
        android.util.Log.d("TransactionScreen", "Tab selected: $index")
        // Send intent to ViewModel
        viewModel.processIntent(TransactionIntent.SelectTab(index))
    }

    val onWalletSelected: () -> Unit = {
        android.util.Log.d(
            "TransactionScreen",
            "Wallet selector clicked, opening choose wallet screen"
        )
        // Open wallet selection screen
        viewModel.processIntent(TransactionIntent.OpenChooseWallet)
    }

    val onTransactionClick: (Transaction) -> Unit = { transaction ->
        android.util.Log.d("TransactionScreen", "Transaction clicked: ${transaction.id}")
        viewModel.processIntent(TransactionIntent.NavigateToTransactionDetail(transaction.id))
    }

    // Add callbacks for TopAppBar actions
    val onSearchClick: () -> Unit = {
        // Navigate to dedicated search screen
        navController?.navigate(Screen.SearchTransaction.route)
    }

    val onAdjustBalanceClick: () -> Unit = {
        // Handle adjust balance action
    }

    val onTransferMoneyClick: () -> Unit = {
        navController?.navigate(Screen.TransferMoney.createRoute())
    }

    val onEditWalletClick: () -> Unit = {
        // Handle edit wallet action
        navController?.navigate(Screen.AddWallet.createRoute(state.currentWallet.id))
    }

    val onAddTransactionClick: () -> Unit = {
        // Navigate to add transaction screen
        navController?.navigate(Screen.AddTransaction.createRoute())
    }

    // Handle navigation events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is TransactionEvent.NavigateToTransactionDetail -> {
                    Log.i("TransactionScreen", "TransactionScreen: navigate to detail")
                    navController?.navigate(Screen.DetailTransaction.createRoute(event.transactionId))
                }

                is TransactionEvent.NavigateToChooseWallet -> {
                    Log.i(
                        "TransactionScreen",
                        "TransactionScreen: navigate to choose wallet with ID: ${event.currentWalletId}"
                    )
                    // Pass the current wallet ID to the ChooseWalletScreen
                    navController?.navigate(Screen.ChooseWallet.createRoute(event.currentWalletId))
                }

                else -> {} // Handle future events
            }
        }
    }

    TransactionScreenContent(
        state = state,
        modifier = modifier,
        onTabSelected = onTabSelected,
        onWalletSelected = onWalletSelected,
        onTransactionClick = onTransactionClick,
        onSearchClick = onSearchClick,
        onAdjustBalanceClick = onAdjustBalanceClick,
        onTransferMoneyClick = onTransferMoneyClick,
        onEditWalletClick = onEditWalletClick,
        onAddTransactionClick = onAddTransactionClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreenContent(
    state: TransactionState,
    onTabSelected: (Int) -> Unit,
    onWalletSelected: () -> Unit,
    onTransactionClick: (Transaction) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAdjustBalanceClick: () -> Unit = {},
    onTransferMoneyClick: () -> Unit = {},
    onEditWalletClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    modifier: Modifier
) {
    val backgroundColor = Color(0xFFF6F6F6)
    val currentBalance = state.currentWallet.currentBalance
    val currentWalletName = state.currentWallet.walletName
    val inflow = state.inflow
    val outflow = state.outflow
    val selectedTabIndex = state.selectedTabIndex
    val groupedTransactions = state.groupedTransactions
    val isTotalWallet = state.isTotalWallet

    // Modal bottom sheet setup
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val isShowBottomSheet = remember { mutableStateOf(false) }

    if (isShowBottomSheet.value) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { isShowBottomSheet.value = false },
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            content = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isTotalWallet) {
                        MoreBottomSheetContent(
                            onTransferMoney = {
                                onTransferMoneyClick()
                                isShowBottomSheet.value = false
                            },
                            onEdit = {
                                onEditWalletClick()
                                isShowBottomSheet.value = false
                            }
                        )

                    } else {
                        Text(
                            text = stringResource(id = R.string.transaction_only_individual_wallets),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor,
        topBar = {
            TransactionTopAppBar(
                currentBalance = currentBalance,
                currentWalletName = currentWalletName,
                onSearchClick = onSearchClick,
                onMoreOptionsClick = { isShowBottomSheet.value = true  },
                onAdjustBalanceClick = onAdjustBalanceClick,
                onTransferMoneyClick = onTransferMoneyClick,
                onEditWalletClick = onEditWalletClick,
                isTotalWallet = isTotalWallet
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxWidth()
                .background(backgroundColor),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Normal navigation
            WalletSelector(
                walletName = currentWalletName,
                walletIcon = state.currentWallet.icon,
                onWalletSelected = onWalletSelected,
                isTotalWallet = isTotalWallet
            )

            MonthSelectionTabs(
                months = state.months,
                selectedIndex = selectedTabIndex,
                onTabSelected = onTabSelected
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else if (groupedTransactions.isEmpty()) {
                // Show empty state when no transactions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    // Still show summary section even when empty
                    SummarySection(inflow = inflow, outflow = outflow)
                    
                    // Empty state
                    EmptyTransactionsState(
                        monthLabel = if (selectedTabIndex < state.months.size) {
                            state.months[selectedTabIndex].label
                        } else {
                            stringResource(id = R.string.transaction_this_month)
                        },
                        onAddTransactionClick = onAddTransactionClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item(key = "summary-section") {
                        Column {
                            SummarySection(inflow = inflow, outflow = outflow)
                        }
                    }

                    groupedTransactions.forEach { dailyData ->
                        item(key = "daily-${dailyData.date.time}") {
                            DailyTransactionsCard(
                                dailyData = dailyData,
                                onTransactionClick = onTransactionClick,
                                showWalletIndicator = isTotalWallet,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreBottomSheetContent(
    onTransferMoney: () -> Unit,
    onEdit: () -> Unit,
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
            text = stringResource(id = R.string.transaction_more),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Set as main wallet
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
                    text = stringResource(id = R.string.transaction_edit_wallet),
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
                .clickable(onClick = onTransferMoney),
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
                    text = stringResource(id = R.string.transaction_transfer_money),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DailyTransactionsCard(
    dailyData: DailyTransactions,
    onTransactionClick: (Transaction) -> Unit,
    showWalletIndicator: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            DailyTransactionHeader(
                date = dailyData.date,
                dailyTotal = dailyData.dailyTotal
            )

            HorizontalDivider(
                color = AppColor.Light.DividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Transactions
            dailyData.transactions.forEach { transaction ->
                TransactionItem(
                    transaction = transaction,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = onTransactionClick,
                    showWalletIndicator = showWalletIndicator
                )
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTopAppBar(
    currentBalance: Double,
    currentWalletName: String,
    onSearchClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onAdjustBalanceClick: () -> Unit = {},
    onTransferMoneyClick: () -> Unit = {},
    onEditWalletClick: () -> Unit = {},
    isTotalWallet: Boolean = false
) {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(id = R.string.transaction_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF505D6D),
                    fontSize = 12.sp
                )
                Text(
                    text = "${formatCurrency(currentBalance)} ₫",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1E2A36)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        actions = {
//            IconButton(onClick = onSearchClick) {
//                Icon(Icons.Filled.Search, contentDescription = "Search", tint = IconTint)
//            }
            IconButton(onClick = onMoreOptionsClick) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = IconTint
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            titleContentColor = AppColor.Light.PrimaryColor.contentColor
        ),
    )
}

fun getPlaceholderTransactions(): List<DailyTransactions> {
    val calendar = Calendar.getInstance()

    // Create date for April 17, 2025
    calendar.set(2025, Calendar.APRIL, 17)
    val date1 = calendar.time

    // Create date for April 15, 2025
    calendar.set(2025, Calendar.APRIL, 15)
    val date2 = calendar.time

    // Create placeholder Category objects
    val loanCategory = Category(
        id = 1,
        metaData = "loan",
        title = "Loan",
        icon = "ic_category_foodndrink",
        type = CategoryType.DEBT_LOAN
    )

    val foodCategory = Category(
        id = 2,
        metaData = "food_beverage",
        title = "Food & Beverage",
        icon = "ic_category_foodndrink",
        type = CategoryType.EXPENSE
    )

    // Create a placeholder wallet with currency
    val currency = Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫"
    )

    val wallet = Wallet(
        id = 1,
        walletName = "Tiền Tài Khoản",
        currentBalance = 249188.0,
        currency = currency
    )

    val transactions1 = listOf(
        Transaction(
            1,
            wallet,
            TransactionType.OUTFLOW,
            20000.0,
            date1,
            "Cho ô toàn cty vay to s...",
            loanCategory
        ),
        Transaction(
            3,
            wallet,
            TransactionType.OUTFLOW,
            44000.0,
            date1,
            "Adjust Balance",
            foodCategory
        ),
        Transaction(4, wallet, TransactionType.OUTFLOW, 35000.0, date1, null, foodCategory)
    )
    val total1 =
        transactions1.sumOf { if (it.transactionType == TransactionType.INFLOW) it.amount else -it.amount }

    val transactions2 = listOf(
        Transaction(5, wallet, TransactionType.OUTFLOW, 20000.0, date2, null, foodCategory)
    )
    val total2 =
        transactions2.sumOf { if (it.transactionType == TransactionType.INFLOW) it.amount else -it.amount }

    return listOf(
        DailyTransactions.create(date1, transactions1, total1),
        DailyTransactions.create(date2, transactions2, total2)
    ).sortedByDescending { it.date }
}



@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun TransactionScreenPreview() {
    ExpenseTrackerTheme {
        // Use a mock ViewModel for preview with MonthItem
        val months = MonthItem.buildMonthItems(monthsBack = 6) // Shorter list for preview
        val thisMonthIndex = MonthItem.findThisMonthIndex(months)
        
        val previewState = TransactionState(
            selectedTabIndex = thisMonthIndex,
            months = months,
            currentBalance = 249188.0,
            currentWalletName = "Tiền Tài Khoản",
            inflow = 10000.0,
            outflow = 5000.0,
            groupedTransactions = getPlaceholderTransactions(),
            isLoading = false,
            isTotalWallet = false
        )

        TransactionScreenContent(
            state = previewState,
            onTabSelected = {},
            onWalletSelected = {},
            onTransactionClick = {},
            onSearchClick = {},
            onAdjustBalanceClick = {},
            onTransferMoneyClick = {},
            onEditWalletClick = {},
            onAddTransactionClick = {},
            modifier = Modifier
        )
    }
}