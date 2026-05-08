package com.uet.expensetracker.features.money.ui.budgets

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.Wallet

data class DisplayableBudget(
    val id: Int,
    val categoryName: String,
    val categoryIconResName: String,
    val amountFormatted: String,
    val leftAmountFormatted: String,
    val progress: Float,
    val currencySymbol: String,
    val alertType: com.uet.expensetracker.features.money.ui.budgets.components.BudgetAlertType? = null
)

data class BudgetScreenState(
    val isLoading: Boolean = false,
    val isBudgetEmpty: Boolean = true,
    val isTotalWallet: Boolean = false,
    val currentWallet: Wallet? = null,
    val wallets: List<Wallet> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val displayableBudgets: List<DisplayableBudget> = emptyList(),
    val totalBudgetAmount: Double = 0.0,
    val totalSpentAmount: Double = 0.0,
    val error: String? = null,

    val overviewTotalAmount: String = "0",
    val overviewSpentAmount: String = "0",
    val overviewProgress: Float = 0f,
    val overviewTotalBudgetsText: String = "0",
    val overviewTotalSpentText: String = "0",
    val overviewDaysLeftText: String = "N/A days"
): MviStateBase

sealed interface BudgetScreenIntent: MviIntentBase {
    object LoadBudgets : BudgetScreenIntent
    data class ChangeWallet(val walletId: Int, val isTotalWallet: Boolean = false) : BudgetScreenIntent
    object CreateBudget : BudgetScreenIntent
    data class DeleteBudget(val budgetId: Int) : BudgetScreenIntent
    object RefreshBudgets : BudgetScreenIntent
    object SelectWalletClicked : BudgetScreenIntent
}

sealed interface BudgetScreenEvent: MviEventBase {
    object NavigateToCreateBudget : BudgetScreenEvent
    object NavigateToChooseWallet : BudgetScreenEvent
    data class ShowError(val message: String) : BudgetScreenEvent
    data class WalletChanged(val wallet: Wallet, val isTotalWallet: Boolean) : BudgetScreenEvent
    object BudgetCreated : BudgetScreenEvent
    object BudgetDeleted : BudgetScreenEvent
}