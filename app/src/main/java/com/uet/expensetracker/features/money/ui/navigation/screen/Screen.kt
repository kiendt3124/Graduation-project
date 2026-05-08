package com.uet.expensetracker.features.money.ui.navigation.screen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.ui.graphics.vector.ImageVector
import com.uet.expensetracker.R

sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Splash       : Screen("splash",      R.string.nav_splash,       Icons.Default.Home)
    data object Onboarding   : Screen("onboarding",  R.string.nav_onboarding,   Icons.Default.Home)
    data object GoogleLogin  : Screen("google_login",R.string.nav_account, Icons.Default.AccountCircle)
    data object WalletSetup  : Screen("wallet_setup?allowSkip={allowSkip}",R.string.nav_setup_wallet,Icons.Default.AccountCircle) {
        fun createRoute(allowSkip: Boolean = false) = "wallet_setup?allowSkip=$allowSkip"
    }
    data object IconPicker    : Screen("icon_picker",R.string.nav_select_icon, Icons.Default.Add)
    data object Home        : Screen("home",        R.string.nav_home,         Icons.Default.Home)
    data object Transactions:
        Screen("transactions", R.string.nav_transactions, Icons.AutoMirrored.Filled.List)
    data object Budget      : Screen("budget",      R.string.nav_budget,       Icons.Default.ShoppingCart)
    data object Account     : Screen("account",     R.string.nav_account,      Icons.Default.AccountCircle)
    data object AddTransaction : Screen("add_transaction?transactionId={transactionId}", R.string.nav_add,      Icons.Default.Add) {
        fun createRoute(transactionId: Int? = null) =
            if (transactionId != null) "add_transaction?transactionId=$transactionId" else "add_transaction"
    }
    data object Category    : Screen("category",    R.string.nav_category,      Icons.Default.Add)
    data object MyWallets   : Screen("my_wallets",   R.string.nav_my_wallets,    Icons.Default.Add)
    data object AddWallet   : Screen("add_wallet?walletId={walletId}",   R.string.nav_add_wallet,     Icons.Default.Add) {
        fun createRoute(walletId: Int? = null) =
            if (walletId != null) "add_wallet?walletId=$walletId" else "add_wallet"
    }
    data object Currency    : Screen("currency",    R.string.nav_currency,      Icons.Default.Add)
    data object AddBudget   : Screen("add_budget?budgetId={budgetId}",   R.string.nav_add_budget,     Icons.Default.Add) {
        fun createRoute(budgetId: Int? = null) =
            if (budgetId != null) "add_budget?budgetId=$budgetId" else "add_budget"
    }
    data object Contacts    : Screen("contacts",    R.string.nav_contacts,      Icons.Default.AccountCircle)
    data object ChooseWallet: 
        Screen("choose_wallet?walletId={walletId}", R.string.nav_choose_wallet, Icons.Default.Add) {
        fun createRoute(walletId: Int? = null) = 
            if (walletId != null) "choose_wallet?walletId=$walletId" else "choose_wallet"
    }
    data object EnterAmount : 
        Screen("enter_amount?currencyCode={currencyCode}&currencySymbol={currencySymbol}&currencyId={currencyId}", 
              R.string.nav_enter_amount,
              Icons.Default.Add) {
        fun createRoute(currencyId: Int, currencyCode: String, currencySymbol: String) = 
            "enter_amount?currencyId=$currencyId&currencyCode=$currencyCode&currencySymbol=$currencySymbol"
    }
    data object DetailTransaction :
        Screen("detail_transaction/{transactionId}", R.string.nav_transaction_detail, Icons.Default.Add) {
        fun createRoute(transactionId: Int) = "detail_transaction/$transactionId"
    }

    data object TransferMoney :
        Screen("transfer_money?walletId={walletId}", R.string.nav_transfer_money, Icons.Default.Add) {
        fun createRoute(walletId: Int? = null) = if (walletId != null) "transfer_money?walletId=$walletId" else "transfer_money"
    }
    data object BudgetDetails :
        Screen("budget_details/{budgetId}", R.string.nav_budget_details, Icons.Default.ShoppingCart) {
        fun createRoute(budgetId: Int) = "budget_details/$budgetId"
    }
    data object BudgetTransactions :
        Screen("budget_transactions/{budgetId}", R.string.nav_budget_transactions, Icons.AutoMirrored.Filled.List) {
        fun createRoute(budgetId: Int) = "budget_transactions/$budgetId"
    }
    
    // Search Transaction Screen
    data object SearchTransaction : Screen("search_transaction", R.string.nav_search_transaction, Icons.Default.Search)
    
    // Debt Management Screen
    data object DebtManagement : Screen("debt_management", R.string.nav_debt_management, Icons.Default.Search)

    // Monthly Report Screen
    data object MonthlyReport : Screen("monthly_report", R.string.nav_reports, Icons.Default.ShoppingCart)
    data object AiChat : Screen("ai_chat", R.string.nav_account, Icons.Default.Chat)
}