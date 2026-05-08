package com.uet.expensetracker.features.money.ui.addtransaction

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.Contact
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import java.util.Date
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Currency

/**
 * Defines the state, intents, and events for the Add Transaction screen following the MVI pattern.
 */

// Represents the state of the Add Transaction screen
data class AddTransactionState(
    // Transaction basic info
    val amount: Double = 0.0,
    val amountInput: String = "0",
    val formattedAmount: String = "0",
    val displayExpression: String = "0",
    val wallet: Wallet = Wallet(
        id = -1,
        walletName = "",
        currentBalance = 0.0,
        currency = Currency(
            id = -1,
            currencyName = "",
            currencyCode = "",
            symbol = "",
            displayType = "",
            image = null
        ),
        isMainWallet = false
    ),
    val category: Category? = null,
    val transactionType: TransactionType = TransactionType.OUTFLOW,
    val description: String = "",
    val transactionDate: Date = Date(), // Using java.util.Date for compatibility

    // UI states
    val showMoreDetails: Boolean = false,
    val isValidAmount: Boolean = true,
    val isValidCategory: Boolean = false,
    val isValidWallet: Boolean = false,
    val showCategories: Boolean = false,
    val showWallets: Boolean = false,
    val showDatePicker: Boolean = false,
    val showNumpad: Boolean = false,
    val showImageSourceDialog: Boolean = false,

    // Suggested amounts
    val suggestedAmounts: List<String> = listOf(
        "500000.0",
        "2000000.0",
        "30000.0",
        "100000.0",
        "5000.0"
    ),

    // Status flags
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: Throwable? = null,

    // Edit mode properties
    val isEditMode: Boolean = false,
    val editingTransactionId: Int? = null,
    val isLoadingTransaction: Boolean = false,

    // Additional details
    val withContacts: List<Contact> = emptyList(),
    val eventName: String = "",
    val reminderSet: Boolean = false,
    val excludeFromReport: Boolean = false,
    val photoUri: String? = null,
    
    // Track if category was manually selected by user
    val isCategoryManuallySelected: Boolean = false
) : MviStateBase

// Represents user actions or intents on the Add Transaction screen
sealed interface AddTransactionIntent : MviIntentBase {
    data class UpdateAmount(val amount: String) : AddTransactionIntent
    data class SelectWallet(val wallet: Wallet) : AddTransactionIntent
    data class SelectCategory(val category: Category) : AddTransactionIntent
    data class UpdateDescription(val description: String) : AddTransactionIntent
    data class SelectTransactionType(val type: TransactionType) : AddTransactionIntent
    data class SelectDate(val date: Date) : AddTransactionIntent
    data class NumpadPressed(val button: NumpadButton) : AddTransactionIntent
    data object ToggleMoreDetails : AddTransactionIntent
    data object ShowCategorySelector : AddTransactionIntent
    data object ShowWalletSelector : AddTransactionIntent
    data object ShowDatePicker : AddTransactionIntent
    data object Save : AddTransactionIntent
    
    // Contact-related intents
    data class UpdateContacts(val contacts: List<Contact>) : AddTransactionIntent
    data class RemoveContact(val contact: Contact) : AddTransactionIntent
    data object ShowContactSelector : AddTransactionIntent
    
    data class UpdateEventName(val event: String) : AddTransactionIntent
    data class SetReminder(val hasReminder: Boolean) : AddTransactionIntent
    data class UpdateExcludeFromReport(val exclude: Boolean) : AddTransactionIntent
    data class UpdatePhotoUri(val uri: String?) : AddTransactionIntent
    
    // New intents for handling Google Assistant data
    data class InitializeCategory(val categoryName: String) : AddTransactionIntent
    data class InitializeDate(val dateString: String) : AddTransactionIntent
    data class InitializeDescription(val description: String) : AddTransactionIntent
    
    // New intents for numpad visibility control
    data object ShowNumpad : AddTransactionIntent
    data object HideNumpad : AddTransactionIntent
    
    // New intents for image selection
    data object ShowImageSourceDialog : AddTransactionIntent
    data object HideImageSourceDialog : AddTransactionIntent
    data object SelectFromCamera : AddTransactionIntent
    data object SelectFromGallery : AddTransactionIntent
    data object RemovePhoto : AddTransactionIntent
    
    // Edit mode intents
    data class LoadTransactionForEdit(val transactionId: Int) : AddTransactionIntent
    data object CancelEdit : AddTransactionIntent
    
    // Initialization intent
    data object InitializeForAddMode : AddTransactionIntent
}

// Represents one-time events that can occur on the Add Transaction screen
sealed interface AddTransactionEvent : MviEventBase {
    data object NavigateBack : AddTransactionEvent
    data class ShowError(val message: String) : AddTransactionEvent
    data object TransactionSaved : AddTransactionEvent
    data object LaunchCamera : AddTransactionEvent
    data object LaunchGallery : AddTransactionEvent
    data class ShowImagePermissionError(val message: String) : AddTransactionEvent
    data class NavigateToContacts(val currentContacts: List<Contact>) : AddTransactionEvent
    
    // Edit mode events
    data object TransactionUpdated : AddTransactionEvent
    data class FailedToLoadTransaction(val error: String) : AddTransactionEvent
}

// Extension properties for computed values
val AddTransactionState.isEditing: Boolean 
    get() = isEditMode && editingTransactionId != null