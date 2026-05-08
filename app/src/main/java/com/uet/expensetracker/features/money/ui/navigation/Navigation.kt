package com.uet.expensetracker.features.money.ui.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.account.AccountScreen
import com.uet.expensetracker.features.money.ui.addwallet.AddWalletScreen
import com.uet.expensetracker.features.money.ui.budgets.BudgetScreen
import com.uet.expensetracker.features.money.ui.currency.CurrencyScreen
import com.uet.expensetracker.features.money.ui.enteramount.EnterAmountScreen
import com.uet.expensetracker.features.money.ui.home.HomeScreen
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.features.money.ui.transactions.TransactionScreen
import com.uet.expensetracker.features.money.ui.addtransaction.AddTransactionScreen
import com.uet.expensetracker.features.money.ui.category.CategoryScreen
import com.uet.expensetracker.features.money.ui.contacts.ContactsScreen
import com.uet.expensetracker.features.money.ui.detailtransaction.DetailTransactionScreen

import com.uet.expensetracker.features.money.ui.mywallets.MyWalletsScreen
import com.uet.expensetracker.features.money.ui.transfermoney.TransferMoneyScreen
import com.uet.expensetracker.features.money.ui.choosewallet.ChooseWalletScreen
import com.uet.expensetracker.features.money.ui.addbudget.AddBudgetScreen
import com.uet.expensetracker.features.money.ui.addbudget.AddBudgetViewModel
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.Wallet
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navDeepLink
import com.uet.expensetracker.features.money.ui.addbudget.AddBudgetIntent
import com.uet.expensetracker.ui.theme.AppColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.uet.expensetracker.features.ai.ui.chat.ChatAiScreen
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.ui.addwallet.AddWalletViewModel
import com.uet.expensetracker.ui.theme.AppColor.Light.SecondaryColor.color1
import com.uet.expensetracker.features.money.ui.onboarding.splash.SplashScreen
import com.uet.expensetracker.features.money.ui.onboarding.onboard.OnboardingScreen
import com.uet.expensetracker.features.money.ui.onboarding.walletsetup.WalletSetupScreen
import com.uet.expensetracker.features.money.ui.onboarding.iconpicker.IconPickerScreen
import com.uet.expensetracker.features.money.ui.onboarding.googlelogin.GoogleLoginScreen
import com.uet.expensetracker.features.money.ui.budgetdetails.BudgetDetailsScreen
import com.uet.expensetracker.features.money.ui.budgets.transactions.BudgetTransactionsScreen
import com.uet.expensetracker.features.money.ui.search.SearchTransactionsScreen
import com.uet.expensetracker.features.money.ui.debtmanagement.DebtManagementScreen
import com.uet.expensetracker.features.money.ui.monthlyreport.MonthlyReportScreen

// 1. Define Navigation Destinations

// List of items for the bottom bar (excluding Add, handled by FAB)
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Budget,
    Screen.Account,
)

// 3. Main App Navigation Composable
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    shouldNavigateToAddTransaction: MutableState<Boolean>,
    incomingIntent: MutableState<Intent?>,
    showBalanceSheet: MutableState<Boolean>,
    onDismissBalanceSheet: () -> Unit,
    showWeeklyExpenseSheet: MutableState<Boolean>,
    onDismissWeeklyExpenseSheet: () -> Unit
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Check if we're on a full-screen page that should hide bottom nav
    val isFullScreenPage =
        currentRoute == Screen.AddTransaction.route ||
        currentRoute == Screen.DetailTransaction.route ||
        currentRoute == Screen.EnterAmount.route ||
        currentRoute == Screen.TransferMoney.route ||
        currentRoute == Screen.MyWallets.route ||
        currentRoute == Screen.AddWallet.route ||
        currentRoute == Screen.Currency.route ||
        currentRoute == Screen.Category.route ||
        currentRoute == Screen.AddBudget.route ||
        currentRoute == Screen.Contacts.route ||
        currentRoute == Screen.ChooseWallet.route ||
        currentRoute == Screen.Splash.route ||
        currentRoute == Screen.GoogleLogin.route ||
        currentRoute == Screen.WalletSetup.route ||
        currentRoute == Screen.IconPicker.route ||
        currentRoute == Screen.BudgetDetails.route ||
        currentRoute == Screen.BudgetTransactions.route ||
        currentRoute == Screen.SearchTransaction.route ||
        currentRoute == Screen.DebtManagement.route ||
        currentRoute == Screen.MonthlyReport.route ||
        currentRoute == Screen.Onboarding.route ||
        currentRoute == Screen.AiChat.route

    LaunchedEffect(shouldNavigateToAddTransaction.value) {
        if (shouldNavigateToAddTransaction.value) {
            navController.navigate(Screen.AddTransaction.createRoute())
            shouldNavigateToAddTransaction.value = false
        }
    }

    LaunchedEffect(incomingIntent.value) {
        incomingIntent.value?.let { intent ->
            // Handle OPEN_APP_FEATURE intent
            if (intent.action == Intent.ACTION_VIEW && intent.hasExtra("feature")) {
                val feature = intent.getStringExtra("feature")

                // Navigate to Add Transaction screen through deep link
                val openFeatureRoute =
                    if (feature?.equals("show_add_transaction", ignoreCase = true) == true) {
//                    android.net.Uri.parse("expensetracker://add_transaction")
                        Screen.AddTransaction.route
                    } else if (feature?.equals("show_all_wallets", ignoreCase = true) == true) {
//                    android.net.Uri.parse("expensetracker://show_my_wallets")
                        Screen.MyWallets.route
                    } else if (feature?.equals("show_list_transaction", ignoreCase = true) == true) {
                        Screen.Transactions.route
                    } else if (feature?.equals("show_budget_screen", ignoreCase = true) == true) {
                        Screen.Budget.route
                    } else if (feature?.equals("show_account_screen", ignoreCase = true) == true) {
                        Screen.Account.route
                    } else if (feature?.equals("show_currency_screen", ignoreCase = true) == true) {
                        Screen.Currency.route
                    } else if (feature?.equals("show_debt_management_screen", ignoreCase = true) == true) {
                        Screen.DebtManagement.route
                    } else {
                        // Default to Home screen if no valid feature is provided
//                    android.net.Uri.parse("expensetracker://home")
                        Screen.Home.route
                    }
//
//                val deepLinkIntent = Intent(Intent.ACTION_VIEW, openFeatureUri)
//                navController.handleDeepLink(deepLinkIntent)

                navController.createDeepLink()
                    .setDestination(openFeatureRoute)
                    .createTaskStackBuilder()
                    .startActivities()
            }

            incomingIntent.value = null
        }
    }

    Scaffold(
        bottomBar = {
            // Only show the bottom bar when not on full-screen pages
            AnimatedVisibility(
                visible = !isFullScreenPage,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = color1
                    )
                    NavigationBar(
                        containerColor = AppColor.Light.PrimaryColor.containerColor,
                        contentColor = AppColor.Light.PrimaryColor.contentColor
                    ) {
                        val currentDestination = currentBackStackEntry?.destination

                        // Navigation items before center Add button
                        bottomNavItems.take(2).forEach { screen ->
                            AppBottomNavigationItem(
                                screen = screen,
                                currentDestination = currentDestination,
                                navController = navController,
                            )
                        }

                        // Center Add button
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { navController.navigate(Screen.AddTransaction.createRoute()) },
                                modifier = Modifier
                                    .background(
                                        color = AppColor.Light.PrimaryColor.TextButtonColor,
                                        shape = CircleShape
                                    )
                                    .padding(2.dp)
                            ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.nav_add_expense_cd),
                                tint = Color.White
                            )
                            }
                        }

                        // Navigation items after center Add button
                        bottomNavItems.drop(2).forEach { screen ->
                            AppBottomNavigationItem(
                                screen = screen,
                                currentDestination = currentDestination,
                                navController = navController
                            )
                        }
                    }
                }
            }
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
    ) { innerPadding ->
        // Apply padding only when not on full-screen pages
        val contentModifier = if (isFullScreenPage) {
            Modifier.fillMaxSize()
        } else {
            Modifier.padding(innerPadding)
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(navController = navController)
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }
            composable(Screen.GoogleLogin.route) {
                GoogleLoginScreen(navController = navController)
            }
            composable(
                route = Screen.WalletSetup.route,
                arguments = listOf(
                    navArgument("allowSkip") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val allowSkip = backStackEntry.arguments?.getBoolean("allowSkip") ?: false
                WalletSetupScreen(
                    navController = navController,
                    allowSkip = allowSkip
                )
            }
            composable(Screen.IconPicker.route) {
                IconPickerScreen(navController = navController)
            }
            composable(route = Screen.Home.route,
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "expensetracker://home"
                    }
                )
            ) {
                HomeScreen(
                    modifier = Modifier,
                    navController = navController,
                    showBalanceSheet = showBalanceSheet.value,
                    onDismissBalanceSheet = onDismissBalanceSheet,
                    showWeeklyExpenseSheet = showWeeklyExpenseSheet.value,
                    onDismissWeeklyExpenseSheet = onDismissWeeklyExpenseSheet
                )
            }
            composable(Screen.Transactions.route) {
                TransactionScreen(
                    navController = navController,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
            composable(Screen.Budget.route) { backStackEntry ->
                BudgetScreen(
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                    savedStateHandle = backStackEntry.savedStateHandle,
                    navController = navController,
                    onCreateBudgetClick = { navController.navigate(Screen.AddBudget.createRoute()) },
                    onHowToUseClick = { /* Handle how to use click */ }
                )
            }
            composable(
                route = Screen.BudgetDetails.route,
                arguments = listOf(navArgument("budgetId") { type = NavType.IntType })
            ) { backStackEntry ->
                val budgetId = backStackEntry.arguments?.getInt("budgetId") ?: return@composable
                BudgetDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditBudget = { navController.navigate(Screen.AddBudget.createRoute(budgetId)) },
                    onShowTransactions = { navController.navigate(Screen.BudgetTransactions.createRoute(budgetId)) },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
            composable(
                route = Screen.BudgetTransactions.route,
                arguments = listOf(navArgument("budgetId") { type = NavType.IntType })
            ) { backStackEntry ->
                val budgetId = backStackEntry.arguments?.getInt("budgetId") ?: return@composable
                BudgetTransactionsScreen(
                    budgetId = budgetId,
                    navController = navController,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
            composable(Screen.Account.route) { 
                AccountScreen(
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                    onNavigateToDebts = { navController.navigate(Screen.DebtManagement.route) },
                    onNavigateToWallets = { navController.navigate(Screen.MyWallets.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Category.route) },
                    onNavigateToAiChat = { navController.navigate(Screen.AiChat.route) }
                ) 
            }
            composable(Screen.AiChat.route) {
                ChatAiScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPrefillTransaction = { parsed ->
                        val amount = parsed.amount?.toString() ?: ""
                        val category = parsed.categoryName ?: ""
                        val date = parsed.date ?: ""
                        val desc = parsed.description ?: ""
                        // Store parsed values in back stack before navigation
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set("ai_amount", amount)
                            set("ai_category", category)
                            set("ai_date", date)
                            set("ai_description", desc)
                        }
                        navController.navigate(Screen.AddTransaction.createRoute())
                    },
                    onNavigate = { route, arguments ->
                        when (route) {
                            "add_budget" -> {
                                navController.navigate(Screen.AddBudget.createRoute())
                            }
                            "monthly_report" -> {
                                navController.navigate(Screen.MonthlyReport.route)
                            }
                            "home" -> {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                }
                            }
                            else -> {
                                // Try to navigate to the route directly
                                try {
                                    navController.navigate(route)
                                } catch (e: Exception) {
                                    // Route not found, ignore
                                }
                            }
                        }
                    },
                    modifier = contentModifier
                )
            }
            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern =
                            "expensetracker://addexpense?amount={amount}&category={category}&date={date}"
                    },
                    navDeepLink {
                        uriPattern = "expensetracker://add_transaction"
                    },
                )
            ) { backStackEntry ->
                val amountFromArgs = backStackEntry.arguments?.getString("amount")
                val categoryFromArgs = backStackEntry.arguments?.getString("category")
                val dateFromArgs = backStackEntry.arguments?.getString("date")
                val transactionId = backStackEntry.arguments?.getInt("transactionId")?.takeIf { it != -1 }

                // Values passed from AI chat via SavedStateHandle (if any)
                val previousHandle = navController.previousBackStackEntry?.savedStateHandle
                val aiAmount = previousHandle?.get<String>("ai_amount")
                val aiCategory = previousHandle?.get<String>("ai_category")
                val aiDate = previousHandle?.get<String>("ai_date")
                val aiDescription = previousHandle?.get<String>("ai_description")

                val amount = aiAmount?.takeIf { it.isNotBlank() } ?: amountFromArgs
                val category = aiCategory?.takeIf { it.isNotBlank() } ?: categoryFromArgs
                val date = aiDate?.takeIf { it.isNotBlank() } ?: dateFromArgs

                AddTransactionScreen(
                    onCloseClick = { navController.popBackStack() },
                    navController = navController,
                    transactionId = transactionId,
                    initialAmount = amount,
                    initialCategory = category,
                    initialDate = date,
                    initialDescription = aiDescription
                )
            }
            composable(Screen.Category.route) {
                CategoryScreen(
                    onBackClick = { navController.popBackStack() },
                    onCategorySelected = { selectedCategory ->
                        // Always set the selected category in the previous back stack entry
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_category",
                            selectedCategory
                        )
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.MyWallets.route,
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "expensetracker://show_my_wallets"
                    }
                )
            ) {
                MyWalletsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddWallet = { navController.navigate(Screen.AddWallet.createRoute()) },
                    onNavigateToWalletDetail = { walletId -> /* TODO: implement detail navigation */ },
                    onNavigateToEditWallet = { walletId ->
                        navController.navigate(Screen.AddWallet.createRoute(walletId))
                    },
                    onNavigateToTransferMoney = { walletId ->
                        navController.navigate(Screen.TransferMoney.createRoute(walletId))
                    }
                )
            }
            composable(
                route = Screen.AddWallet.route,
                arguments = listOf(navArgument("walletId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                // Obtain the ViewModel scoped to this backStackEntry so SavedStateHandle has walletId
                val viewModel: AddWalletViewModel = hiltViewModel(backStackEntry)
                AddWalletScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.navigateUp() },
                    navController = navController
                )
            }
            composable(Screen.Currency.route) {
                CurrencyScreen(
                    onBackClick = { navController.popBackStack() },
                    onCurrencySelected = { selectedCurrency ->
                        // Set the selected currency in the previous back stack entry
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_currency",
                            selectedCurrency
                        )
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDone = { selectedContacts ->
                        // Handle selected contacts - could set them in previous back stack entry
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_contacts",
                            selectedContacts
                        )
                    },
                    navController = navController
                )
            }
            composable(
                route = Screen.AddBudget.route,
                arguments = listOf(navArgument("budgetId") {
                    type = NavType.IntType
                    nullable = false
                    defaultValue = -1 // Using -1 as a marker for null
                })
            ) { backStackEntry ->
                val viewModel = hiltViewModel<AddBudgetViewModel>(backStackEntry)

                // Observe for selected category from CategoryScreen
                navController.currentBackStackEntry?.savedStateHandle?.get<Category>("selected_category")
                    ?.let { category ->
                        LaunchedEffect(category) {
                            viewModel.processIntent(AddBudgetIntent.SelectCategory(category))
                            // Remove the value to avoid processing it multiple times
                            navController.currentBackStackEntry?.savedStateHandle?.remove<Category>(
                                "selected_category"
                            )
                        }
                    }

                // Observe for selected wallet from ChooseWalletScreen
                val selectedWallet =
                    navController.currentBackStackEntry?.savedStateHandle?.get<Wallet>("selected_wallet")
                val isTotal =
                    navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("is_total_wallet")
                        ?: false

                LaunchedEffect(selectedWallet) {
                    selectedWallet?.let {
                        viewModel.processIntent(AddBudgetIntent.SelectWallet(it))
                        if (isTotal) {
                            viewModel.processIntent(AddBudgetIntent.ToggleTotal(true))
                        }
                        // Remove the values to avoid processing them multiple times
                        navController.currentBackStackEntry?.savedStateHandle?.remove<Wallet>(
                            "selected_wallet"
                        )
                        navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("is_total_wallet")
                    }
                }

                AddBudgetScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSelectCategory = { navController.navigate(Screen.Category.route) },
                    onNavigateToSelectDate = { /* TODO: Implement date picker navigation */ },
                    onNavigateToSelectWallet = {
                        // Pass current selection (wallet ID or -1 for total) to pre-select
                        val state = viewModel.viewState.value
                        val paramId = if (state.isTotal) -1 else state.selectedWallet?.id
                        navController.navigate(Screen.ChooseWallet.createRoute(paramId))
                    },
                    onShowError = { errorMessage ->
                        /* TODO: Implement error handling, such as showing a snackbar */
                    }
                )
            }
            composable(
                route = Screen.ChooseWallet.route,
                arguments = listOf(
                    navArgument("walletId") {
                        type = NavType.IntType
                        nullable = false
                        defaultValue = -1 // Using -1 as a marker for null
                    }
                )
            ) { backStackEntry ->
                val walletId = backStackEntry.arguments?.getInt("walletId")
                // Keep raw walletId (−1 indicates All Wallets)
                val initialWalletId = walletId

                android.util.Log.d(
                    "Navigation",
                    "Opening ChooseWalletScreen with initialWalletId: $initialWalletId"
                )
                // Get previous entry to determine where to navigate back to
                val previousRoute = navController.previousBackStackEntry?.destination?.route
                android.util.Log.d("Navigation", "Previous route: $previousRoute")

                ChooseWalletScreen(
                    initialWalletId = initialWalletId,
                    showTotalWallet = !isFromAddTransaction(previousRoute),
                    onWalletSelected = { selectedWallet, isTotalWallet ->
                        // Set both the wallet ID and the wallet object in the previous back stack entry
                        android.util.Log.d(
                            "Navigation",
                            "Wallet selected in Navigation: ${selectedWallet.id} - ${selectedWallet.walletName}, isTotalWallet: $isTotalWallet"
                        )
                        android.util.Log.d(
                            "Navigation",
                            "Setting wallet in previous entry: $previousRoute"
                        )

                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_wallet_id",
                            selectedWallet.id
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "is_total_wallet",
                            isTotalWallet
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_wallet",
                            selectedWallet
                        )

                        // Let the ChooseWalletScreen handle navigation back
                    },
                    onNavigateBack = {
                        // Navigate back - ensure we get back to the correct screen
                        if (previousRoute == Screen.Transactions.route) {
                            android.util.Log.d("Navigation", "Navigating back to Transactions")
                            navController.popBackStack()
                        } else if (previousRoute == Screen.AddBudget.route) {
                            android.util.Log.d("Navigation", "Navigating back to Add Budget")
                            navController.popBackStack()
                        } else {
                            android.util.Log.d(
                                "Navigation",
                                "Previous route is neither Transactions nor AddBudget, simple popBackStack"
                            )
                            navController.popBackStack()
                        }
                    },
                    navController = navController
                )
            }
            composable(
                route = Screen.EnterAmount.route,
                arguments = listOf(
                    navArgument("currencyId") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("currencyCode") {
                        type = NavType.StringType
                        defaultValue = "USD"
                    },
                    navArgument("currencySymbol") {
                        type = NavType.StringType
                        defaultValue = "$"
                    }
                )
            ) { backStackEntry ->
                val currencyId = backStackEntry.arguments?.getInt("currencyId") ?: 0
                val currencyCode = backStackEntry.arguments?.getString("currencyCode") ?: "USD"
                val currencySymbol = backStackEntry.arguments?.getString("currencySymbol") ?: "$"

                // Create Currency object from arguments
                val currency = Currency(
                    id = currencyId,
                    currencyName = "", // We don't need this for the amount screen
                    currencyCode = currencyCode,
                    symbol = currencySymbol
                )

                EnterAmountScreen(
                    onNavigateBack = { amount, formattedAmount ->
                        // Set both raw and formatted amounts in the previous back stack entry
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "entered_amount", amount
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "formatted_amount", formattedAmount
                        )
                        navController.popBackStack()
                    },
                    initialCurrency = currency
                )
            }
            composable(
                route = Screen.DetailTransaction.route,
                arguments = listOf(
                    navArgument("transactionId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getInt("transactionId") ?: 0
                DetailTransactionScreen(
                    transactionId = transactionId,
                    navController = navController
                )
            }

            composable(
                route = Screen.TransferMoney.route,
                arguments = listOf(
                    navArgument("walletId") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val walletId = backStackEntry.arguments?.getInt("walletId")
                TransferMoneyScreen(
                    navController = navController,
                    walletId = walletId
                )
            }
            composable(Screen.SearchTransaction.route) {
                SearchTransactionsScreen(
                    navController = navController
                )
            }
            composable(Screen.DebtManagement.route) {
                DebtManagementScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MonthlyReport.route) {
                MonthlyReportScreen(
                    navController = navController,
                    modifier = contentModifier
                )
            }
        }
    }
}

// Helper function to check if coming from AddTransaction screen
private fun isFromAddTransaction(previousRoute: String?): Boolean {
    return previousRoute?.startsWith("add_transaction") == true
}

// Helper composable for creating each Bottom Navigation Item
@Composable
fun RowScope.AppBottomNavigationItem(
    screen: Screen,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    NavigationBarItem(
        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
        label = {
            Text(
                text = stringResource(screen.labelRes),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
        onClick = {
            // Navigate to the selected screen
            navController.navigate(screen.route) {
                // Pop up to the start destination, avoiding building up a large back stack
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true // Save state of screens
                }
                // Avoid multiple copies of the same destination when reselecting
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF2B3B48),
            selectedTextColor = Color(0xFF2B3B48),
            unselectedIconColor = Color(0xFF94A1B1),
            unselectedTextColor = Color(0xFF94A1B1),
            indicatorColor = Color.Transparent
        )
    )
}