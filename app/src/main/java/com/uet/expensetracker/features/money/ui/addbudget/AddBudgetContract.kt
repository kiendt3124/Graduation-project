package com.uet.expensetracker.features.money.ui.addbudget

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import java.util.Date

data class AddBudgetState(
    val selectedCategory: Category? = null,
    val amount: Double = 0.0,
    val amountInput: String = "0",
    val formattedAmount: String = "0",
    val displayExpression: String = "0",
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val isRepeating: Boolean = false,
    val isTotal: Boolean = false,
    val selectedWallet: Wallet? = null,
    val availableWallets: List<Wallet> = emptyList(),
    val isLoading: Boolean = false,
    val showNumpad: Boolean = false,
    val existingBudgetId: Int? = null,
    val isEditMode: Boolean = false
) : MviStateBase

sealed class AddBudgetIntent : MviIntentBase {
    data class SelectCategory(val category: Category) : AddBudgetIntent()
    data class UpdateAmount(val amount: Double) : AddBudgetIntent()
    data class ToggleRepeat(val isRepeating: Boolean) : AddBudgetIntent()
    data class SelectWallet(val wallet: Wallet) : AddBudgetIntent()
    data class ToggleTotal(val isTotal: Boolean) : AddBudgetIntent()
    object LoadWallets : AddBudgetIntent()
    object SaveBudget : AddBudgetIntent()
    object ConfirmOverride : AddBudgetIntent()
    object NavigateToSelectCategory : AddBudgetIntent()
    object NavigateToSelectDate : AddBudgetIntent()
    object NavigateToSelectWallet : AddBudgetIntent()

    // Numpad related intents
    object ToggleNumpad : AddBudgetIntent()
    data class NumpadButtonPressed(val button: NumpadButton) : AddBudgetIntent()

    // Date range selection intents
    data class SelectStartDate(val date: Date) : AddBudgetIntent()
    data class SelectEndDate(val date: Date) : AddBudgetIntent()
}

sealed class AddBudgetEvent : MviEventBase {
    object NavigateBack : AddBudgetEvent()
    object NavigateToSelectCategory : AddBudgetEvent()
    object NavigateToSelectDate : AddBudgetEvent()
    object NavigateToSelectWallet : AddBudgetEvent()
    data class ShowError(val message: String) : AddBudgetEvent()
    object BudgetSaved : AddBudgetEvent()
    data class ShowInfo(val message: String) : AddBudgetEvent()
    data class ShowConfirmOverride(val categoryId: Int) : AddBudgetEvent()
}