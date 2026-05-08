package com.uet.expensetracker.features.money.ui.addtransaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import java.util.Date
import com.uet.expensetracker.features.money.domain.model.Contact
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import androidx.core.net.toUri
import java.io.FileOutputStream
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R

@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel = hiltViewModel(),
    onCloseClick: () -> Unit = {},
    navController: NavController = rememberNavController(),
    transactionId: Int? = null, // For edit mode
    initialAmount: String? = null,
    initialCategory: String? = null,
    initialDate: String? = null,
    initialDescription: String? = null
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Camera image URI state
    val cameraImageUri = remember { mutableStateOf<android.net.Uri?>(null) }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            // Persist read permission
            coroutineScope.launch {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(selectedUri, flags)
                // Update state with persisted URI
                viewModel.processIntent(AddTransactionIntent.UpdatePhotoUri(selectedUri.toString()))
            }
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let { srcUri ->
                // Copy captured image to persistent storage
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
                        val destFile = File(imagesDir, "tx_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(srcUri)?.use { input ->
                            FileOutputStream(destFile).use { output -> input.copyTo(output) }
                        }
                        val persistedUri = destFile.toUri().toString()
                        withContext(Dispatchers.Main) {
                            viewModel.processIntent(AddTransactionIntent.UpdatePhotoUri(persistedUri))
                        }
                    }
                }
            }
        }
    }
    
    // Create temp file for camera
    val createTempImageFile = {
        val timeStamp = System.currentTimeMillis()
        File(context.cacheDir, "temp_image_$timeStamp.jpg").apply {
            createNewFile()
        }
    }
    
    // Load transaction for edit if transactionId is provided, otherwise initialize for add mode
    // Use a key to ensure this only runs once per transactionId
    var hasLoadedTransaction by remember { mutableStateOf(false) }
    var loadedTransactionId by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            // Only load if we haven't loaded this transaction yet
            if (!hasLoadedTransaction || loadedTransactionId != transactionId) {
                hasLoadedTransaction = true
                loadedTransactionId = transactionId
                viewModel.processIntent(AddTransactionIntent.LoadTransactionForEdit(transactionId))
            }
        } else {
            if (!hasLoadedTransaction) {
                viewModel.processIntent(AddTransactionIntent.InitializeForAddMode)
                hasLoadedTransaction = true
            }
        }
    }
    
    // Process intent data from Google Assistant if available
    LaunchedEffect(initialAmount, initialCategory, initialDate) {
        if (initialAmount != null || initialCategory != null || initialDate != null) {
            initialAmount?.let { amount -> 
                viewModel.processIntent(AddTransactionIntent.UpdateAmount(amount)) 
            }
            
            initialCategory?.let { categoryName ->
                // Process the category - will need to find category by name in ViewModel
                viewModel.processIntent(AddTransactionIntent.InitializeCategory(categoryName))
            }
            
            initialDate?.let { dateString ->
                // Process the date - convert string to Date in ViewModel
                viewModel.processIntent(AddTransactionIntent.InitializeDate(dateString))
            }

            initialDescription?.let { desc ->
                viewModel.processIntent(AddTransactionIntent.InitializeDescription(desc))
            }
        }
    }

    // Observe the selected category when returning from CategoryScreen
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    
    // Use lifecycle observer to detect when returning from CategoryScreen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check for selected category when screen resumes
                savedStateHandle?.get<Category>("selected_category")?.let { category ->
                    savedStateHandle.remove<Category>("selected_category")
                    viewModel.processIntent(AddTransactionIntent.SelectCategory(category))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Observe selected wallet when returning from WalletScreen
    val selectedWallet = savedStateHandle?.get<Wallet>("selected_wallet")
    val isTotalWallet = savedStateHandle?.get<Boolean>("is_total_wallet") ?: false
    
    LaunchedEffect(selectedWallet) {
        selectedWallet?.let { wallet ->
            savedStateHandle?.remove<Wallet>("selected_wallet")
            viewModel.processIntent(AddTransactionIntent.SelectWallet(wallet))
            // Also remove is_total_wallet flag if exists
            savedStateHandle?.remove<Boolean>("is_total_wallet")
        }
    }

    // Observe selected contacts when returning from ContactsScreen
    val selectedContacts = savedStateHandle?.get<List<Contact>>("selected_contacts")
    
    LaunchedEffect(selectedContacts) {
        selectedContacts?.let { contacts ->
            savedStateHandle?.remove<List<Contact>>("selected_contacts")
            viewModel.processIntent(AddTransactionIntent.UpdateContacts(contacts))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                AddTransactionEvent.NavigateBack -> {
                    navController.navigateUp()
                }
                is AddTransactionEvent.ShowError -> {
                    // Handle error display
                }
                AddTransactionEvent.TransactionSaved -> {
                    // Handle transaction saved
                }
                AddTransactionEvent.LaunchCamera -> {
                    try {
                        val tempFile = createTempImageFile()
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        cameraImageUri.value = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        viewModel.processIntent(AddTransactionIntent.HideImageSourceDialog)
                        // Show error to user
                    }
                }
                AddTransactionEvent.LaunchGallery -> {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                is AddTransactionEvent.ShowImagePermissionError -> {
                    // Handle permission error
                }
                is AddTransactionEvent.NavigateToContacts -> {
                    // Navigate to contacts screen with current contacts
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "current_contacts",
                        event.currentContacts
                    )
                    navController.navigate(Screen.Contacts.route)
                }
                AddTransactionEvent.TransactionUpdated -> {
                    // Handle transaction updated - could show success message
                }
                is AddTransactionEvent.FailedToLoadTransaction -> {
                    // Handle load transaction error
                    android.util.Log.e("AddTransactionScreen", "Failed to load transaction: ${event.error}")
                }
                null -> {}
            }
        }
    }

    // Show loading for edit mode
    if (state.isLoadingTransaction) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    AddTransactionContent(
        state = state,
        onIntent = { intent -> 
            when (intent) {
                is AddTransactionIntent.ShowWalletSelector -> {
                    // Pass current wallet ID and hide Total Wallet for AddTransaction
                    val currentWalletId = state.wallet.id
                    navController.navigate(Screen.ChooseWallet.createRoute(currentWalletId))
                }
                is AddTransactionIntent.ShowContactSelector -> {
                    viewModel.processIntent(intent) // This will emit NavigateToContacts event
                }
                else -> viewModel.processIntent(intent)
            }
        },
        onCloseClick = onCloseClick,
        onCategoryClick = { navController.navigate(Screen.Category.route) },
        title = if (state.isEditMode) {
            stringResource(R.string.add_transaction_title_edit)
        } else {
            stringResource(R.string.add_transaction_title_add)
        },
        saveButtonText = if (state.isEditMode) {
            stringResource(R.string.add_transaction_button_update)
        } else {
            stringResource(R.string.add_transaction_button_save)
        }
    )
}

@Preview()
@Composable
fun AddTransactionPreview() {
    ExpenseTrackerTheme(darkTheme = true) {
        val previewCurrency = Currency(
            id = 1,
            currencyName = "US Dollar",
            currencyCode = "USD",
            symbol = "$",
            displayType = "prefix",
            image = null
        )
        
        val previewCategory = Category(
            id = 1,
            metaData = "food0",
            title = "Food & Beverage",
            icon = "ic_category_foodndrink",
            type = CategoryType.EXPENSE,
            parentName = "food"
        )
        
        val previewState = AddTransactionState(
            amount = 123.45,
            description = "Lunch with colleagues",
            transactionDate = Date(),
            category = previewCategory,
            showMoreDetails = true,
            showNumpad = false,
            wallet = Wallet(
                id = 1,
                walletName = "Cash",
                currentBalance = 1000.0,
                currency = previewCurrency,
                isMainWallet = true
            ),
            suggestedAmounts = listOf("10.00", "20.00", "50.00", "100.00"),
            withContacts = listOf<Contact>(),
            eventName = "",
            reminderSet = false,
            excludeFromReport = false
        )

        AddTransactionContent(
            state = previewState,
            onIntent = {},
            onCloseClick = {},
            onCategoryClick = {},
            title = "Add Transaction",
            saveButtonText = "Save"
        )
    }
}