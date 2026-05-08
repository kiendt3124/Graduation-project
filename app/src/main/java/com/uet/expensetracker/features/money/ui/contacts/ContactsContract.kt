package com.uet.expensetracker.features.money.ui.contacts

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Contact

/**
 * Defines the state, intents, and events for the Contacts screen following the MVI pattern.
 */

// Represents the state of the Contacts screen
data class ContactsState(
    // Contacts data
    val contacts: List<Contact> = emptyList(),
    val selectedContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val filteredContacts: List<Contact> = emptyList(),

    // UI states
    val isLoading: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val showPermissionBottomSheet: Boolean = false,
    val isSearchMode: Boolean = false,
    val doNotAskAgain: Boolean = false,

    // Phone number input
    val phoneNumberInput: String = "",
    val isValidPhoneNumber: Boolean = false,

    // Status flags
    val error: Throwable? = null,
    val isSuccess: Boolean = false
) : MviStateBase

// Represents user actions or intents on the Contacts screen
sealed interface ContactsIntent : MviIntentBase {
    // Contact selection
    data class ToggleContactSelection(val contact: Contact) : ContactsIntent
    data object ClearSelection : ContactsIntent
    data object SelectAllVisible : ContactsIntent
    data class UpdateInitialContacts(val contacts: List<Contact>) : ContactsIntent

    // Search and filtering
    data class UpdateSearchQuery(val query: String) : ContactsIntent
    data object ToggleSearchMode : ContactsIntent
    data object ClearSearch : ContactsIntent

    // Phone number input
    data class UpdatePhoneNumber(val phoneNumber: String) : ContactsIntent
    data object AddPhoneNumberAsContact : ContactsIntent

    // Permissions
    data object RequestContactsPermission : ContactsIntent
    data object PermissionGranted : ContactsIntent
    data object PermissionDenied : ContactsIntent
    data class UpdateDoNotAskAgain(val doNotAsk: Boolean) : ContactsIntent
    data object ShowPermissionBottomSheet : ContactsIntent
    data object HidePermissionBottomSheet : ContactsIntent

    // Data loading
    data object LoadContacts : ContactsIntent
    data object RefreshContacts : ContactsIntent

    // Navigation
    data object NavigateBack : ContactsIntent
    data object Done : ContactsIntent
}

// Represents one-time events that can occur on the Contacts screen
sealed interface ContactsEvent : MviEventBase {
    data object NavigateBack : ContactsEvent
    data class ShowError(val message: String) : ContactsEvent
    data class ContactsSelected(val contacts: List<Contact>) : ContactsEvent
    data object RequestContactsPermission : ContactsEvent
    data object OpenAppSettings : ContactsEvent
    data class ShowToast(val message: String) : ContactsEvent
} 