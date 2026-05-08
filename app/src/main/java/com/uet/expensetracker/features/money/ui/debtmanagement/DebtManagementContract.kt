package com.uet.expensetracker.features.money.ui.debtmanagement

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.DebtSummary
import com.uet.expensetracker.features.money.domain.model.DebtType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.ui.debtmanagement.components.DebtFilterOptions
import com.uet.expensetracker.features.money.ui.debtmanagement.components.DebtTab

/**
 * Defines the state properties for the DebtManagement UI.
 * Follows MVI architecture pattern for predictable state management.
 */
data class DebtManagementState(
    // Wallet selection
    val availableWallets: List<Wallet> = emptyList(),
    val selectedWallet: Wallet? = null,
    
    // Raw debt data (chưa filter) để áp dụng filter nhiều lần không mất dữ liệu gốc
    val allPayableDebts: List<DebtSummary> = emptyList(),
    val allReceivableDebts: List<DebtSummary> = emptyList(),
    
    // Tab management
    val selectedTab: DebtTab = DebtTab.PAYABLE,
    
    // Debt data
    val payableDebts: List<DebtSummary> = emptyList(),
    val receivableDebts: List<DebtSummary> = emptyList(),
    val unpaidPayableDebts: List<DebtSummary> = emptyList(),
    val paidPayableDebts: List<DebtSummary> = emptyList(),
    val unpaidReceivableDebts: List<DebtSummary> = emptyList(),
    val paidReceivableDebts: List<DebtSummary> = emptyList(),
    
    // Payment sheet state
    val showPaymentSheet: Boolean = false,
    val paymentTarget: DebtSummary? = null,
    val paymentAmountInput: String = "",
    val paymentNoteInput: String = "",
    val paymentSubmitting: Boolean = false,
    
    // Summary statistics
    val totalPayableAmount: Double = 0.0,
    val totalReceivableAmount: Double = 0.0,
    val totalUnpaidPayable: Double = 0.0,
    val totalUnpaidReceivable: Double = 0.0,
    val currencySymbol: String = "đ",
    
    // Filter options
    val filterOptions: DebtFilterOptions = DebtFilterOptions(),
    
    // UI state
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showWalletSelector: Boolean = false,
    val showFilterDialog: Boolean = false,

    // Payment history bottom sheet
    val showHistorySheet: Boolean = false,
    val historyItems: List<com.uet.expensetracker.features.money.domain.model.PaymentRecord> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val historyTitle: String? = null,
    val historyDebtType: DebtType? = null
) : MviStateBase

/**
 * Defines the user intents that can be triggered from the DebtManagement UI.
 * Following MVI architecture pattern for clear unidirectional data flow.
 */
sealed interface DebtManagementIntent : MviIntentBase {
    // Data loading
    data object LoadInitialData : DebtManagementIntent
    data object RefreshData : DebtManagementIntent
    
    // Wallet selection
    data object ShowWalletSelector : DebtManagementIntent
    data object HideWalletSelector : DebtManagementIntent
    data class SelectWallet(val wallet: Wallet?) : DebtManagementIntent
    
    // Tab management
    data class SelectTab(val tab: DebtTab) : DebtManagementIntent
    
    // Debt management
    data class ViewDebtDetails(val debtSummary: DebtSummary) : DebtManagementIntent
    data class AddPartialPayment(val debtSummary: DebtSummary) : DebtManagementIntent
    data class ViewPaymentHistory(val debtSummary: DebtSummary) : DebtManagementIntent
    data object ClosePaymentHistory : DebtManagementIntent
    data class UpdatePaymentAmount(val amountText: String) : DebtManagementIntent
    data class UpdatePaymentNote(val note: String) : DebtManagementIntent
    data object ConfirmPayment : DebtManagementIntent
    data object DismissPaymentSheet : DebtManagementIntent
    
    // Filter management
    data object ShowFilterDialog : DebtManagementIntent
    data object HideFilterDialog : DebtManagementIntent
    data class ApplyFilter(val filterOptions: DebtFilterOptions) : DebtManagementIntent
    data object ClearFilter : DebtManagementIntent
    
    // Error handling
    data object ClearError : DebtManagementIntent
}

/**
 * Defines events that can be emitted from the DebtManagementViewModel to the UI
 * for one-time actions (e.g., showing a toast, navigation).
 */
sealed interface DebtManagementEvent : MviEventBase {
    // Navigation events
    data class NavigateToDebtDetails(val debtSummary: DebtSummary) : DebtManagementEvent
    data class NavigateToAddPartialPayment(val debtSummary: DebtSummary) : DebtManagementEvent
    data class NavigateToPaymentHistory(val debtSummary: DebtSummary) : DebtManagementEvent
    
    // UI events
    data class ShowToast(val message: String) : DebtManagementEvent
    data class ShowError(val message: String) : DebtManagementEvent
    data class ShowSuccessMessage(val message: String) : DebtManagementEvent
    
    // Share/Export events
    data class ShareDebtSummary(val summary: String) : DebtManagementEvent
    data class ExportDebtReport(val filePath: String) : DebtManagementEvent
} 