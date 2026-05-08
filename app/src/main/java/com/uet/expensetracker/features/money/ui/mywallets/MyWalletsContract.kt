package com.uet.expensetracker.features.money.ui.mywallets

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Wallet


data class MyWalletsState(
    val isLoading: Boolean = false,
    val wallets: List<Wallet> = emptyList(),
    val totalBalance: Double = 0.0,
    val error: String? = null
) : MviStateBase

sealed interface MyWalletsIntent : MviIntentBase {
    object LoadWallets : MyWalletsIntent
    data class AddWallet(val walletType: WalletType) : MyWalletsIntent
    data class DeleteWallet(val walletId: Int) : MyWalletsIntent
    data class SetMainWallet(val walletId: Int) : MyWalletsIntent
    data class EditWallet(val walletId: Int) : MyWalletsIntent
    data class TransferMoney(val walletId: Int) : MyWalletsIntent
}

sealed interface MyWalletsEvent : MviEventBase {
    data class ShowError(val message: String) : MyWalletsEvent
    data class NavigateToAddWallet(val walletType: WalletType) : MyWalletsEvent
    data class NavigateToWalletDetail(val walletId: Int) : MyWalletsEvent
    object WalletDeleted : MyWalletsEvent
    object MainWalletSet : MyWalletsEvent
    data class NavigateToEditWallet(val walletId: Int) : MyWalletsEvent
    data class NavigateToTransferMoney(val walletId: Int) : MyWalletsEvent
}

enum class WalletType(val displayName: String) {
    BASIC("Basic Wallet"),
    LINKED("Linked Wallet"),
    CREDIT("Credit Wallet"),
    GOAL("Goal Wallet")
}