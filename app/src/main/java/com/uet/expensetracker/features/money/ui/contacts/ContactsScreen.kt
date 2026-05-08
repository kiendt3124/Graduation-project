package com.uet.expensetracker.features.money.ui.contacts

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.uet.expensetracker.features.money.domain.model.Contact
import kotlinx.coroutines.launch
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateBack: () -> Unit,
    onDone: (List<Contact>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController? = null
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Bottom sheet state for permission dialog
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.processIntent(ContactsIntent.PermissionGranted)
        } else {
            viewModel.processIntent(ContactsIntent.PermissionDenied)
        }
    }

    // Get initial contacts from navigation if coming from AddTransactionScreen
    LaunchedEffect(Unit) {
        navController?.previousBackStackEntry?.savedStateHandle?.get<List<Contact>>("current_contacts")?.let { currentContacts ->
            // Initialize the contacts screen with existing selected contacts
            viewModel.processIntent(ContactsIntent.UpdateInitialContacts(currentContacts))
        }
    }

    // Handle events
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is ContactsEvent.NavigateBack -> onNavigateBack()
                is ContactsEvent.ContactsSelected -> onDone(event.contacts)
                is ContactsEvent.RequestContactsPermission -> {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
                is ContactsEvent.ShowError -> {
                    // Handle error display (could show snackbar, etc.)
                }
                is ContactsEvent.ShowToast -> {
                    // Handle toast display
                }
                is ContactsEvent.OpenAppSettings -> {
                    // Handle opening app settings
                }

                null -> {}
            }
        }
    }

    // Handle bottom sheet visibility
    LaunchedEffect(state.showPermissionBottomSheet) {
        if (state.showPermissionBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main content
        ContactsScreenContent(
            state = state,
            onNavigateBack = {
                viewModel.processIntent(ContactsIntent.NavigateBack)
            },
            onDone = {
                viewModel.processIntent(ContactsIntent.Done)
            },
            onContactClick = { contact ->
                viewModel.processIntent(ContactsIntent.ToggleContactSelection(contact))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Permission bottom sheet
        if (state.showPermissionBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.processIntent(ContactsIntent.HidePermissionBottomSheet)
                },
                sheetState = bottomSheetState
            ) {
                ContactPermissionBottomSheet(
                    onAllowAccess = {
                        viewModel.processIntent(ContactsIntent.RequestContactsPermission)
                        viewModel.processIntent(ContactsIntent.HidePermissionBottomSheet)
                    },
                    onNoThanks = {
                        viewModel.processIntent(ContactsIntent.HidePermissionBottomSheet)
                        onNavigateBack()
                    },
                    onDoNotAskAgainChanged = { doNotAsk ->
                        viewModel.processIntent(ContactsIntent.UpdateDoNotAskAgain(doNotAsk))
                    }
                )
            }
        }
    }
} 