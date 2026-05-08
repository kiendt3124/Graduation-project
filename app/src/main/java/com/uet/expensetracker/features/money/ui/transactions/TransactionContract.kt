package com.uet.expensetracker.features.money.ui.transactions

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Defines the state, intents, and events for the Transaction screen following the MVI pattern.
 */

/**
 * Represents a month tab item in the transaction screen
 */
data class MonthItem(
    val label: String,           // "This month", "01/2024", "Future"
    val year: Int,
    val month: Int,
    val isFuture: Boolean = false
) {
    companion object {
        /**
         * Builds a list of MonthItem for the past months, current month, and future
         * @param monthsBack Number of months to go back (default 18 months = 1.5 years)
         * @return List of MonthItem sorted from oldest to newest, with "Future" at the end
         */
        fun buildMonthItems(monthsBack: Int = 18): List<MonthItem> {
            val now = LocalDate.now()
            val months = mutableListOf<MonthItem>()
            
            // Add past months (1.5 years back)
            for (i in monthsBack downTo 1) {
                val date = now.minusMonths(i.toLong())
                months.add(
                    MonthItem(
                        label = date.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                        year = date.year,
                        month = date.monthValue
                    )
                )
            }
            
            // Add current month
            months.add(
                MonthItem(
                    label = "This month",
                    year = now.year,
                    month = now.monthValue
                )
            )
            
            // Add future tab
            months.add(
                MonthItem(
                    label = "Future",
                    year = now.year,
                    month = now.monthValue,
                    isFuture = true
                )
            )
            
            return months
        }
        
        /**
         * Find the index of "This month" tab
         */
        fun findThisMonthIndex(months: List<MonthItem>): Int {
            return months.indexOfFirst { it.label == "This month" }.takeIf { it >= 0 } ?: 0
        }
    }
}



// Represents the state of the Transaction screen
data class TransactionState(
    val selectedTabIndex: Int = 1, // Will be updated to default to "This month"
    val months: List<MonthItem> = MonthItem.buildMonthItems(), // List of month tabs
    val currentBalance: Double = 0.0,
    val currentWalletName: String = "",
    val currentWallet: Wallet = Wallet(
        id = 0,
        walletName = "Default Wallet",
        currentBalance = 0.0,
        currency = Currency(
            id = 1,
            currencyName = "Vietnamese Dong",
            currencyCode = "VND",
            symbol = "₫"
        )
    ),
    val inflow: Double = 0.0,
    val outflow: Double = 0.0,
    val groupedTransactions: List<DailyTransactions> = emptyList(),
    val isLoading: Boolean = true,
    val error: Throwable? = null,
    val isTotalWallet: Boolean = false
) : MviStateBase {
    init {
        // Auto-set selected tab to "This month" if not explicitly set
        if (selectedTabIndex == 1 && months.isNotEmpty()) {
            val thisMonthIndex = MonthItem.findThisMonthIndex(months)
            if (thisMonthIndex != selectedTabIndex) {
                // Note: This should be handled in ViewModel init, not here
                // This is just for documentation of the intended behavior
            }
        }
    }
}

// Represents user actions or intents on the Transaction screen
sealed interface TransactionIntent : MviIntentBase {
    data class SelectTab(val index: Int) : TransactionIntent
    data class SelectWallet(val walletId: Int, val isTotalWallet: Boolean = false) : TransactionIntent
    data object LoadTransactions : TransactionIntent
    data class NavigateToTransactionDetail(val transactionId: Int) : TransactionIntent
    data object OpenChooseWallet : TransactionIntent
}

// Represents one-time events that can occur on the Transaction screen (e.g., navigation, showing toasts)
sealed interface TransactionEvent : MviEventBase {
    data class NavigateToTransactionDetail(val transactionId: Int) : TransactionEvent
    data class NavigateToChooseWallet(val currentWalletId: Int) : TransactionEvent
}

data class DailyTransactions(
    val date: Date,
    val transactions: List<Transaction>,
    val dailyTotal: Double
) {
    companion object {
        // Factory method that creates DailyTransactions with sorted transactions (newest first)
        fun create(
            date: Date,
            unsortedTransactions: List<Transaction>,
            dailyTotal: Double
        ): DailyTransactions {
            return DailyTransactions(
                date = date,
                transactions = unsortedTransactions.sortedByDescending { it.id },
                dailyTotal = dailyTotal
            )
        }
    }
}
