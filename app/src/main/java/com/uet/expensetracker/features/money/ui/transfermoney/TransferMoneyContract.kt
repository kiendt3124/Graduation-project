package com.uet.expensetracker.features.money.ui.transfermoney

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Wallet
import java.util.Date

/**
 * Transfer Money screen state
 */
data class TransferMoneyState(
    val isLoading: Boolean = false,
    val wallets: List<Wallet> = emptyList(),
    val fromWallet: Wallet? = null,
    val toWallet: Wallet? = null,
    val amount: Double = 0.0,
    val formattedAmount: String = "",
    val toAmount: Double = 0.0,
    val formattedToAmount: String = "",
    val note: String = "",
    val date: Date = Date(),
    val excludeFromReport: Boolean = false,
    val addTransferFee: Boolean = false,
    val transferFee: Double = 0.0,
    val error: String = "",
    val exchangeRate: String = "",
    val lastWalletSelectionMode: String = "" // "from" or "to"
) : MviStateBase

/**
 * Transfer Money intents that user can trigger
 */
sealed class TransferMoneyIntent : MviIntentBase {
    data class LoadWallets(val selectedWalletId: Int? = null) : TransferMoneyIntent()
    data class SelectFromWallet(val walletId: Int) : TransferMoneyIntent()
    data class SelectToWallet(val walletId: Int) : TransferMoneyIntent()
    data class UpdateAmount(val amount: Double) : TransferMoneyIntent()
    data class UpdateFormattedAmount(val formattedAmount: String) : TransferMoneyIntent()
    data class UpdateNote(val note: String) : TransferMoneyIntent()
    data class UpdateDate(val date: Date) : TransferMoneyIntent()
    data class UpdateExcludeFromReport(val exclude: Boolean) : TransferMoneyIntent()
    data class UpdateAddTransferFee(val addFee: Boolean) : TransferMoneyIntent()
    data class UpdateTransferFee(val fee: Double) : TransferMoneyIntent()
    data class SetWalletSelectionMode(val mode: String) : TransferMoneyIntent() // "from" or "to"
    object SaveTransfer : TransferMoneyIntent()
    object Cancel : TransferMoneyIntent()
    object ConvertCurrency : TransferMoneyIntent()
    object OpenEnterAmountScreen : TransferMoneyIntent()
}

/**
 * One-time events emitted by the ViewModel
 */
sealed class TransferMoneyEvent : MviEventBase {
    object NavigateBack : TransferMoneyEvent()
    object TransferCompleted : TransferMoneyEvent()
    data class ShowError(val message: String) : TransferMoneyEvent()
    object NavigateToEnterAmount : TransferMoneyEvent()
} 