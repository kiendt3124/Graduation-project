@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.lifecycle.SavedStateHandle
import com.uet.expensetracker.BuildConfig
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.features.money.ui.budgets.components.BudgetOverviewCard
import com.uet.expensetracker.features.money.ui.budgets.components.BudgetItem
import com.uet.expensetracker.features.money.ui.budgets.components.EmptyBudgetContent
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import com.uet.expensetracker.utils.getDrawableResId
import com.uet.expensetracker.utils.getStringResId
import kotlinx.coroutines.flow.collectLatest

/**
 * Budget Screen UI
 */
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle,
    navController: NavController,
    onCreateBudgetClick: () -> Unit = { viewModel.processIntent(BudgetScreenIntent.CreateBudget) },
    onHowToUseClick: () -> Unit = {}
) {
    // Collect view state
    val viewState by viewModel.viewState.collectAsState()

    // Observe for selected wallet from ChooseWalletScreen
    val selectedWallet =
        navController.currentBackStackEntry?.savedStateHandle?.get<Wallet>("selected_wallet")
    val isTotal =
        navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("is_total_wallet")
            ?: false

    LaunchedEffect(selectedWallet, isTotal) {
        // If a wallet is selected, update the view model with the new wallet
        // -1 for total wallet
        selectedWallet?.let {
            viewModel.processIntent(BudgetScreenIntent.ChangeWallet(selectedWallet?.id ?: -1, isTotal))


            // Remove the values to avoid processing them multiple times
            navController.currentBackStackEntry?.savedStateHandle?.remove<Wallet>(
                "selected_wallet"
            )
            navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("is_total_wallet")
        }


    }

    // Handle events
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(key1 = viewModel, key2 = lifecycleOwner) {
        viewModel.event
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest { event ->
                when (event) {
                    is BudgetScreenEvent.NavigateToCreateBudget -> {
                        navController.navigate(Screen.AddBudget.createRoute())
                    }
                    is BudgetScreenEvent.NavigateToChooseWallet -> {
                        val currentWalletId = viewState.currentWallet?.id ?: -1
                        navController.navigate(Screen.ChooseWallet.createRoute(walletId = currentWalletId))
                    }
                    is BudgetScreenEvent.ShowError -> {
                        // Show error toast or snackbar
                    }
                    is BudgetScreenEvent.WalletChanged -> {
                        // Handle wallet changed event if needed (e.g. scroll to top)
                    }
                    is BudgetScreenEvent.BudgetCreated -> {
                        // Optionally refresh or show confirmation
                    }
                    is BudgetScreenEvent.BudgetDeleted -> {
                        // Optionally show confirmation
                    }
                    null -> { /* Ignore null events */ }
                }
            }
    }

    // Render the screen
    BudgetScreenContent(
        state = viewState,
        onWalletSelectorClick = {
            viewModel.processIntent(BudgetScreenIntent.SelectWalletClicked)
        },
        onCreateBudgetClick = onCreateBudgetClick,
        onRefreshBudgets = {
            viewModel.processIntent(BudgetScreenIntent.RefreshBudgets)
        },
        onDeleteBudget = { budgetId ->
            viewModel.processIntent(BudgetScreenIntent.DeleteBudget(budgetId))
        },
        onBudgetClick = { budgetId ->
            navController.navigate(Screen.BudgetDetails.createRoute(budgetId))
        },
        onHowToUseClick = onHowToUseClick,
        modifier = modifier
    )
}

/**
 * Budget Screen Content - Revamped UI
 */
@Composable
fun BudgetScreenContent(
    modifier: Modifier = Modifier,
    state: BudgetScreenState,
    onWalletSelectorClick: () -> Unit,
    onCreateBudgetClick: () -> Unit,
    onRefreshBudgets: () -> Unit,
    onDeleteBudget: (Int) -> Unit,
    onBudgetClick: (Int) -> Unit,
    onHowToUseClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(1) }
    val tabs = listOf(
        stringResource(R.string.budgets_last_month),
        stringResource(R.string.budgets_this_month),
        stringResource(R.string.budgets_future)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.budgets_running_budgets), color = TextMain, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColor.Light.PrimaryColor.containerColor,
                    titleContentColor = AppColor.Light.PrimaryColor.contentColor,
                ),
                actions = {
                    // Wallet selector icon (similar to image)
                    IconButton(onClick = onWalletSelectorClick) {
                        // Replace with your actual globe/wallet icon if available
                        Row {
                            Image(
                                painter = painterResource(id = getDrawableResId(LocalContext.current, state.currentWallet?.icon ?: "0")), // Placeholder, replace with actual icon
                                contentDescription = stringResource(R.string.budgets_select_wallet_cd),
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_tooltip_home_up), // Placeholder, replace with actual icon
                                    contentDescription = stringResource(R.string.budgets_select_wallet_cd),
                                    modifier = Modifier.size(12.dp),
                                    tint = TextMain
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_tooltip_home_down), // Placeholder, replace with actual icon
                                    contentDescription = stringResource(R.string.budgets_select_wallet_cd),
                                    modifier = Modifier.size(12.dp),
                                    tint = TextMain
                                )
                            }
                        }

                    }
                    // More options icon (if needed, similar to image)
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.budgets_more_options_cd),
                            tint = TextMain
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budgets_refresh)) },
                            onClick = {
                                onRefreshBudgets()
                                showMenu = false
                            }
                        )
                        // Add other menu items if needed
                    }
                }
            )
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // TabRow for time periods
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = AppColor.Light.PrimaryColor.containerColor,
                contentColor = Color.White,
                indicator = {
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(it[selectedTabIndex]),
                        color = TextSecondary // Or your accent color
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTabIndex == index) TextMain else TextSecondary
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                } else if (state.isBudgetEmpty && state.displayableBudgets.isEmpty()) { // Check displayableBudgets as well
                    EmptyBudgetContent(
                        paddingValues = PaddingValues(), // Already handled by parent
                        onCreateBudgetClick = onCreateBudgetClick,
                        onHowToUseClick = onHowToUseClick
                    )
                } else {
                    // Main content with BudgetOverviewCard and list of BudgetItem
                    LazyColumn(
                        modifier = Modifier.background(color = AppColor.Light.PrimaryColor.containerColor),
                        contentPadding = PaddingValues(vertical = 8.dp), // Reduced from 16.dp to 8.dp
                        verticalArrangement = Arrangement.spacedBy(4.dp) // Reduced from 8.dp to 4.dp
                    ) {
                        // Budget Overview Card
                        item {
                            BudgetOverviewCard(
                                totalAmount = state.overviewTotalAmount,
                                spentAmount = state.overviewSpentAmount,
                                progress = state.overviewProgress,
                                totalBudgetsText = state.overviewTotalBudgetsText,
                                totalSpentText = state.overviewTotalSpentText,
                                daysLeftText = state.overviewDaysLeftText,
                                onCreateBudgetClick = onCreateBudgetClick
                            )
                        }

                        // List of Budgets
                        items(state.displayableBudgets, key = { it.id }) { budget ->
                            BudgetItem(
                                modifier = Modifier.clickable { onBudgetClick(budget.id) },
                                categoryIcon = getDrawableResId(LocalContext.current, budget.categoryIconResName),
                                categoryName = stringResource(getStringResId(LocalContext.current, budget.categoryName)),
                                amount = budget.amountFormatted,
                                leftAmount = budget.leftAmountFormatted,
                                progress = budget.progress,
                                alertType = budget.alertType
                            )
                        }
                    }
                }

                if (BuildConfig.DEBUG) {
                    // Error message if needed
                    state.error?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(
                                    Color.White.copy(alpha = 0.8f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp) // Reduced from default padding
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun BudgetScreenEmptyPreview() {
    BudgetScreenContent(
        state = BudgetScreenState(isBudgetEmpty = true, displayableBudgets = emptyList(), currentWallet = Wallet(
            id = 1,
            walletName = "Main Wallet",
            currentBalance = 5000000.0,
            currency = Currency(
                id = 1,
                currencyName = "Vietnamese Dong",
                currencyCode = "VND",
                symbol = "₫",
                displayType = "symbol",
                image = null
            ),
            icon = "img_wallet_default_widget",
        ),),
        onWalletSelectorClick = {},
        onCreateBudgetClick = {},
        onRefreshBudgets = {},
        onDeleteBudget = {},
        onBudgetClick = {},
        onHowToUseClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun BudgetScreenWithBudgetsPreview() {
    val previewBudgets = listOf(
        DisplayableBudget(1, "Food & Beverage", "ic_category_foodndrink", "1,000,000", "Left 1,000,000", 0.0f, "$"),
        DisplayableBudget(2, "Transportation", "ic_category_foodndrink", "1,000,000", "Left 900,000", 0.1f, "$")
    )
    BudgetScreenContent(
        state = BudgetScreenState(
            isBudgetEmpty = false,
            displayableBudgets = previewBudgets,
            overviewTotalAmount = "1,900,000",
            overviewSpentAmount = "100,000",
            overviewProgress = 0.05f,
            overviewTotalBudgetsText = "2",
            overviewTotalSpentText = "100 K",
            overviewDaysLeftText = "18 days",
            currentWallet = Wallet(
                id = 1,
                walletName = "Main Wallet",
                currentBalance = 5000000.0,
                currency = Currency(
                    id = 1,
                    currencyName = "Vietnamese Dong",
                    currencyCode = "VND",
                    symbol = "₫",
                    displayType = "symbol",
                    image = null
                ),
                icon = "ic_category_all",
            ),
        ),
        onWalletSelectorClick = {},
        onCreateBudgetClick = {},
        onRefreshBudgets = {},
        onDeleteBudget = {},
        onBudgetClick = {},
        onHowToUseClick = {}
    )
} 