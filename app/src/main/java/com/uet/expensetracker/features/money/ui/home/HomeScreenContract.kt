package com.uet.expensetracker.features.money.ui.home

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.ui.home.components.MainTab
import com.uet.expensetracker.features.money.ui.home.components.TrendingSubTab
import com.uet.expensetracker.features.money.ui.home.components.SpendingSubTab

/**
 * Defines the state properties for the HomeScreen UI.
 */
data class HomeScreenState(
    val totalBalance: String = "0 đ", // Formatted total balance
    val isBalanceVisible: Boolean = true,
    val wallets: List<WalletWithCurrencyEntity> = emptyList(), // List of user's wallets
    // Data for report tabs
    val transactions: List<Transaction> = emptyList(),
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val currentSpent: Double = 0.0,
    val previousSpent: Double = 0.0,
    val currentMonthSpent: Double = 0.0,
    val previousMonthSpent: Double = 0.0,
    val currencySymbol: String = "",
    // Selected tabs for report UI
    val selectedMainTab: MainTab = MainTab.Trending,
    val selectedTrendingTab: TrendingSubTab = TrendingSubTab.TotalSpent,
    val selectedSpendingTab: SpendingSubTab = SpendingSubTab.Week,
    val isLoading: Boolean = false,
    val error: String? = null // Holds potential error messages
) : MviStateBase

/**
 * Defines the user intents that can be triggered from the HomeScreen UI.
 * Following MVI architecture pattern for clear unidirectional data flow.
 */
sealed interface HomeScreenIntent : MviIntentBase {
    data object LoadInitialData : HomeScreenIntent // Intent to load initial screen data
    data object ToggleBalanceVisibility : HomeScreenIntent // Intent to toggle balance visibility
    data class SelectMainTab(val tab: MainTab) : HomeScreenIntent
    data class SelectTrendingTab(val tab: TrendingSubTab) : HomeScreenIntent
    data class SelectSpendingTab(val tab: SpendingSubTab) : HomeScreenIntent
    data object LoadWeeklyExpense : HomeScreenIntent // Intent to load weekly expense
    // Add other intents like NavigateToAllWallets, NavigateToReports, SearchClicked etc. later
}

/**
 * Defines events that can be emitted from the HomeScreenViewModel to the UI
 * for one-time actions (e.g., showing a toast, navigation).
 */
sealed interface HomeScreenEvent : MviEventBase {
    data class ShowWeeklyExpense(val amount: String) : HomeScreenEvent // Event to show weekly expense in bottom sheet
    // Define specific events here later if needed, e.g.,
    // data class ShowError(val message: String) : HomeScreenEvent
}