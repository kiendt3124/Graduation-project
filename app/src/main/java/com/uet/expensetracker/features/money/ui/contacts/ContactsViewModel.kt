package com.uet.expensetracker.features.money.ui.contacts

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Contact
import com.uet.expensetracker.utils.ContactsUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.uet.expensetracker.R

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseViewModel<ContactsState, ContactsIntent, ContactsEvent>() {

    override val _viewState = MutableStateFlow(ContactsState())
    private var _loadContactsJob: Job? = null
    init {
        checkPermissionAndLoadContacts()
    }

    override fun processIntent(intent: ContactsIntent) {
        when (intent) {
            is ContactsIntent.ToggleContactSelection -> toggleContactSelection(intent.contact)
            is ContactsIntent.ClearSelection -> clearSelection()
            is ContactsIntent.SelectAllVisible -> selectAllVisible()
            is ContactsIntent.UpdateInitialContacts -> updateInitialContacts(intent.contacts)
            is ContactsIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is ContactsIntent.ToggleSearchMode -> toggleSearchMode()
            is ContactsIntent.ClearSearch -> clearSearch()
            is ContactsIntent.UpdatePhoneNumber -> updatePhoneNumber(intent.phoneNumber)
            is ContactsIntent.AddPhoneNumberAsContact -> addPhoneNumberAsContact()
            is ContactsIntent.RequestContactsPermission -> requestContactsPermission()
            is ContactsIntent.PermissionGranted -> onPermissionGranted()
            is ContactsIntent.PermissionDenied -> onPermissionDenied()
            is ContactsIntent.UpdateDoNotAskAgain -> updateDoNotAskAgain(intent.doNotAsk)
            is ContactsIntent.ShowPermissionBottomSheet -> showPermissionBottomSheet()
            is ContactsIntent.HidePermissionBottomSheet -> hidePermissionBottomSheet()
            is ContactsIntent.LoadContacts -> loadContacts()
            is ContactsIntent.RefreshContacts -> refreshContacts()
            is ContactsIntent.NavigateBack -> navigateBack()
            is ContactsIntent.Done -> done()
        }
    }

    private fun checkPermissionAndLoadContacts() {
        viewModelScope.launch {
            val hasPermission = ContactsUtil.hasContactsPermission(context)
            _viewState.value = _viewState.value.copy(hasContactsPermission = hasPermission)

            if (hasPermission) {
                loadContacts()
            } else {
                showPermissionBottomSheet()
            }
        }
    }

    private fun loadContacts() {
        _loadContactsJob = viewModelScope.launch {
            try {
                _viewState.value = _viewState.value.copy(isLoading = true, error = null)

                val contacts = ContactsUtil.getAllContacts(context)
                val filteredContacts = if (_viewState.value.searchQuery.isNotEmpty()) {
                    ContactsUtil.searchContacts(contacts, _viewState.value.searchQuery)
                } else {
                    contacts
                }

                _viewState.value = _viewState.value.copy(
                    contacts = contacts,
                    filteredContacts = filteredContacts,
                    isLoading = false
                )

                Log.d("ContactsViewModel", "Loaded ${contacts.size} contacts")

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    isLoading = false,
                    error = e
                )
                emitEvent(ContactsEvent.ShowError(context.getString(R.string.contacts_error_load_contacts, e.message ?: "")))
                Log.e("ContactsViewModel", "Error loading contacts", e)
            }
        }
    }

    private fun refreshContacts() {
        if (_viewState.value.hasContactsPermission) {
            loadContacts()
        } else {
            checkPermissionAndLoadContacts()
        }
    }

    private fun toggleContactSelection(contact: Contact) {
        val currentState = _viewState.value
        val updatedContacts = currentState.filteredContacts.map { existingContact ->
            if (existingContact.id == contact.id) {
                existingContact.copy(isSelected = !existingContact.isSelected)
            } else {
                existingContact
            }
        }

        val selectedContacts = updatedContacts.filter { it.isSelected }

        _viewState.value = currentState.copy(
            filteredContacts = updatedContacts,
            selectedContacts = selectedContacts,
            contacts = currentState.contacts.map { existingContact ->
                if (existingContact.id == contact.id) {
                    existingContact.copy(isSelected = !existingContact.isSelected)
                } else {
                    existingContact
                }
            }
        )
    }

    private fun clearSelection() {
        val currentState = _viewState.value
        val clearedContacts = currentState.contacts.map { it.copy(isSelected = false) }
        val clearedFilteredContacts =
            currentState.filteredContacts.map { it.copy(isSelected = false) }

        _viewState.value = currentState.copy(
            contacts = clearedContacts,
            filteredContacts = clearedFilteredContacts,
            selectedContacts = emptyList()
        )
    }

    private fun selectAllVisible() {
        val currentState = _viewState.value
        val updatedFilteredContacts =
            currentState.filteredContacts.map { it.copy(isSelected = true) }
        val selectedIds = updatedFilteredContacts.map { it.id }.toSet()

        val updatedContacts = currentState.contacts.map { contact ->
            if (selectedIds.contains(contact.id)) {
                contact.copy(isSelected = true)
            } else {
                contact
            }
        }

        _viewState.value = currentState.copy(
            contacts = updatedContacts,
            filteredContacts = updatedFilteredContacts,
            selectedContacts = updatedFilteredContacts
        )
    }

    private fun updateInitialContacts(initialContacts: List<Contact>) {
        _loadContactsJob?.invokeOnCompletion {
            val currentState = _viewState.value

            val initialContactIds = initialContacts.map { it.id }.toSet()

            // Update existing contacts to mark initial ones as selected
            val updatedContacts = currentState.contacts.map { contact ->
                contact.copy(isSelected = initialContactIds.contains(contact.id))
            }

            val updatedFilteredContacts = if (currentState.searchQuery.isNotEmpty()) {
                ContactsUtil.searchContacts(updatedContacts, currentState.searchQuery)
            } else {
                updatedContacts
            }

            // Include initial contacts that might not be in device contacts (like phone numbers)
            val selectedContacts =
                updatedContacts.filter { it.isSelected } + initialContacts.filter { initial ->
                    !updatedContacts.any { device -> device.id == initial.id }
                }

            _viewState.value = currentState.copy(
                contacts = updatedContacts,
                filteredContacts = updatedFilteredContacts,
                selectedContacts = selectedContacts
            )
        }
    }

    private fun updateSearchQuery(query: String) {
        val currentState = _viewState.value
        val filteredContacts = if (query.isNotEmpty()) {
            ContactsUtil.searchContacts(currentState.contacts, query)
        } else {
            currentState.contacts
        }

        _viewState.value = currentState.copy(
            searchQuery = query,
            filteredContacts = filteredContacts,
            isValidPhoneNumber = ContactsUtil.isValidPhoneNumber(query)
        )
    }

    private fun toggleSearchMode() {
        val currentState = _viewState.value
        _viewState.value = currentState.copy(
            isSearchMode = !currentState.isSearchMode,
            searchQuery = if (!currentState.isSearchMode) "" else currentState.searchQuery,
            filteredContacts = if (!currentState.isSearchMode) currentState.contacts else currentState.filteredContacts
        )
    }

    private fun clearSearch() {
        val currentState = _viewState.value
        _viewState.value = currentState.copy(
            searchQuery = "",
            filteredContacts = currentState.contacts,
            isSearchMode = false,
            isValidPhoneNumber = false
        )
    }

    private fun updatePhoneNumber(phoneNumber: String) {
        _viewState.value = _viewState.value.copy(
            phoneNumberInput = phoneNumber,
            isValidPhoneNumber = ContactsUtil.isValidPhoneNumber(phoneNumber)
        )
    }

    private fun addPhoneNumberAsContact() {
        val currentState = _viewState.value
        if (currentState.isValidPhoneNumber && currentState.phoneNumberInput.isNotEmpty()) {
            val newContact =
                ContactsUtil.createContactFromPhoneNumber(currentState.phoneNumberInput)
            val updatedSelectedContacts = currentState.selectedContacts + newContact

            _viewState.value = currentState.copy(
                selectedContacts = updatedSelectedContacts,
                phoneNumberInput = ""
            )

            emitEvent(ContactsEvent.ShowToast(context.getString(R.string.contacts_phone_number_added)))
        }
    }

    private fun requestContactsPermission() {
        emitEvent(ContactsEvent.RequestContactsPermission)
    }

    private fun onPermissionGranted() {
        _viewState.value = _viewState.value.copy(
            hasContactsPermission = true,
            showPermissionBottomSheet = false
        )
        loadContacts()
    }

    private fun onPermissionDenied() {
        val currentState = _viewState.value
        _viewState.value = currentState.copy(
            hasContactsPermission = false,
            showPermissionBottomSheet = if (currentState.doNotAskAgain) false else true
        )

        if (currentState.doNotAskAgain) {
            emitEvent(ContactsEvent.OpenAppSettings)
        }
    }

    private fun updateDoNotAskAgain(doNotAsk: Boolean) {
        _viewState.value = _viewState.value.copy(doNotAskAgain = doNotAsk)
    }

    private fun showPermissionBottomSheet() {
        _viewState.value = _viewState.value.copy(showPermissionBottomSheet = true)
    }

    private fun hidePermissionBottomSheet() {
        _viewState.value = _viewState.value.copy(showPermissionBottomSheet = false)
    }

    private fun navigateBack() {
        emitEvent(ContactsEvent.NavigateBack)
    }

    private fun done() {
        val selectedContacts = _viewState.value.selectedContacts
        if (selectedContacts.isNotEmpty()) {
            emitEvent(ContactsEvent.ContactsSelected(selectedContacts))
        }
        emitEvent(ContactsEvent.NavigateBack)
    }
} 