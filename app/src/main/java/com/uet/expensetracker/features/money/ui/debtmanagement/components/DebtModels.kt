package com.uet.expensetracker.features.money.ui.debtmanagement.components

import androidx.annotation.StringRes
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.DebtType

/**
 * Represents the main tabs in debt management screen
 */
enum class DebtTab(@StringRes val displayNameResId: Int) {
    PAYABLE(R.string.debt_tab_payable),
    RECEIVABLE(R.string.debt_tab_receivable)
}

/**
 * Represents sub-sections within each debt tab
 */
enum class DebtSection(@StringRes val displayNameResId: Int) {
    UNPAID(R.string.debt_section_unpaid),
    PAID(R.string.debt_section_paid)
}

/**
 * Filter options for debt management
 */
data class DebtFilterOptions(
    val sortBy: DebtSortBy = DebtSortBy.AMOUNT_DESC,
    val showPaidDebts: Boolean = true,
    val showUnpaidDebts: Boolean = true,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val searchQuery: String = ""
) {
    fun isDefault(): Boolean {
        return sortBy == DebtSortBy.AMOUNT_DESC &&
               showPaidDebts &&
               showUnpaidDebts &&
               minAmount == null &&
               maxAmount == null &&
               searchQuery.isEmpty()
    }
}

/**
 * Sorting options for debt summaries
 */
enum class DebtSortBy(@StringRes val displayNameResId: Int) {
    AMOUNT_ASC(R.string.debt_sort_amount_asc),
    AMOUNT_DESC(R.string.debt_sort_amount_desc),
    NAME_ASC(R.string.debt_sort_name_asc),
    NAME_DESC(R.string.debt_sort_name_desc),
    DATE_ASC(R.string.debt_sort_date_asc),
    DATE_DESC(R.string.debt_sort_date_desc)
}

/**
 * UI model for debt summary cards
 */
data class DebtCardUiModel(
    val personName: String,
    val personInitial: String,
    val originalAmount: String,
    val remainingAmount: String,
    val paidAmount: String,
    val progressPercentage: Float,
    val isFullyPaid: Boolean,
    val lastPaymentDate: String?,
    val currencySymbol: String,
    val debtType: DebtType,
    val paymentCount: Int,
    val daysSinceLastPayment: Int?
)

/**
 * UI model for debt statistics
 */
data class DebtStatsUiModel(
    val totalAmount: String,
    val unpaidAmount: String,
    val paidAmount: String,
    val totalCount: Int,
    val unpaidCount: Int,
    val paidCount: Int,
    val currencySymbol: String,
    val debtType: DebtType
)

/**
 * UI model for payment history items
 */
data class PaymentHistoryUiModel(
    val date: String,
    val amount: String,
    val description: String?,
    val isLatest: Boolean,
    val currencySymbol: String
)

/**
 * UI state for wallet selector
 */
data class WalletSelectorUiModel(
    val walletName: String,
    val balance: String,
    val currencySymbol: String,
    val isSelected: Boolean,
    val isAllWallets: Boolean = false
) 