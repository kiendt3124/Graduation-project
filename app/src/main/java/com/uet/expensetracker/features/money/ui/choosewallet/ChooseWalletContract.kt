package com.uet.expensetracker.features.money.ui.choosewallet

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Wallet

data class ChooseWalletState(
    val isLoading: Boolean = true,
    val wallets: List<Wallet> = emptyList(),
    val selectedWalletId: Int? = null,
    val totalWallet: Wallet? = null,
    val error: String? = null,
    val showTotalWallet: Boolean = true // Control whether to show Total Wallet option
) : MviStateBase

sealed class ChooseWalletIntent : MviIntentBase {
    data object LoadWallets : ChooseWalletIntent()
    data class SelectWallet(val walletId: Int) : ChooseWalletIntent()
    data object SelectTotalWallet : ChooseWalletIntent()
    data object ConfirmSelection : ChooseWalletIntent()
    data object Cancel : ChooseWalletIntent()
    data class SetShowTotalWallet(val show: Boolean) : ChooseWalletIntent()
}

sealed class ChooseWalletEvent : MviEventBase {
    data class WalletSelected(val wallet: Wallet, val isTotalWallet: Boolean = false) : ChooseWalletEvent()
    data object NavigateBack : ChooseWalletEvent()
    data class ShowError(val message: String) : ChooseWalletEvent()
} 