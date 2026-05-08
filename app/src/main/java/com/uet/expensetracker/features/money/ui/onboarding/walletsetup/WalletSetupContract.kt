package com.uet.expensetracker.features.money.ui.onboarding.walletsetup

import androidx.annotation.DrawableRes
import com.uet.expensetracker.R
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton

sealed interface WalletSetupIntent : MviIntentBase {
    object ChangeIcon : WalletSetupIntent
    data class IconPicked(@DrawableRes val iconRes: Int) : WalletSetupIntent
    data class UpdateName(val name: String) : WalletSetupIntent
    object ChangeCurrency : WalletSetupIntent
    data class CurrencySelected(val currency: Currency) : WalletSetupIntent
    object ChangeBalance : WalletSetupIntent
    data class BalanceEntered(val amount: String) : WalletSetupIntent
    object ConfirmSetup : WalletSetupIntent
    object SkipSetup : WalletSetupIntent

    // Added for calculator numpad integration
    object ToggleNumpad : WalletSetupIntent
    data class NumpadButtonPressed(val button: NumpadButton) : WalletSetupIntent
}

/**
 * UI state for WalletSetupScreen
 */
data class WalletSetupState(
    val title: String = "",
    val description: String = "",
    @DrawableRes val selectedIcon: Int = R.drawable.img_wallet_default_widget,
    val walletName: String = "",
    val selectedCurrency: Currency? = null,
    val enteredAmount: String = "",
    val isCreating: Boolean = false,
    val isRestoreLoading: Boolean = false,
    val error: String? = null,

    // Added for calculator numpad integration
    val amountInput: String = "0",
    val formattedAmount: String = "0",
    val showNumpad: Boolean = false
) : MviStateBase

sealed interface WalletSetupEvent : MviEventBase {
    object NavigateToIconPicker : WalletSetupEvent
    object NavigateToCurrencyPicker : WalletSetupEvent
    object NavigateToEnterAmount : WalletSetupEvent
    object NavigateToHome : WalletSetupEvent
} 