package com.uet.expensetracker.features.money.ui.detailtransaction

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Contact
import com.uet.expensetracker.features.money.domain.usecases.DeleteTransactionUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.SaveTransactionUseCase
import com.uet.expensetracker.features.money.ui.detailtransaction.components.TransactionImageCard
import com.uet.expensetracker.features.ai.data.worker.FinancialContextSyncWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.drawToBitmap

@HiltViewModel
class DetailTransactionViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : BaseViewModel<DetailTransactionState, DetailTransactionIntent, DetailTransactionEvent>() {

    override val _viewState = MutableStateFlow(DetailTransactionState(isLoading = true))

    override fun processIntent(intent: DetailTransactionIntent) {
        when (intent) {
            is DetailTransactionIntent.LoadTransaction -> loadTransaction(intent.id)
            is DetailTransactionIntent.DeleteTransaction -> deleteTransaction()
            is DetailTransactionIntent.ConfirmDeleteTransaction -> confirmDeleteTransaction()
            is DetailTransactionIntent.DismissDeleteConfirmation -> dismissDeleteConfirmation()
            is DetailTransactionIntent.CopyTransaction -> copyTransaction()
            is DetailTransactionIntent.ShareTransaction -> shareTransaction()
            is DetailTransactionIntent.OpenTransactionImage -> openTransactionImage()
            is DetailTransactionIntent.ShareTransactionImage -> shareTransactionImage()
            is DetailTransactionIntent.DismissShareDialog -> dismissShareDialog()
            is DetailTransactionIntent.EditTransaction -> editTransaction()
        }
    }

    private fun loadTransaction(id: Int) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(isLoading = true)

            getTransactionByIdUseCase(
                params = id,
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            _viewState.value = _viewState.value.copy(
                                isLoading = false,
                                error = Exception(failure.toString())
                            )
                            emitEvent(DetailTransactionEvent.ShowError("Failed to load transaction details"))
                        },
                        { transaction ->
                            // Parse contacts from withPerson JSON string
                            val contactsList = parseContactsFromJson(transaction.withPerson)
                            
                            _viewState.value = _viewState.value.copy(
                                transaction = transaction,
                                isLoading = false,
                                listContacts = contactsList
                            )
                        }
                    )
                }
            )
        }
    }

    private fun parseContactsFromJson(withPersonJson: String?): List<Contact> {
        return try {
            if (withPersonJson.isNullOrEmpty()) {
                emptyList()
            } else {
                val gson = Gson()
                val listType = object : TypeToken<List<Contact>>() {}.type
                gson.fromJson(withPersonJson, listType) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error parsing contacts from JSON: $withPersonJson", e)
            emptyList()
        }
    }

    private fun deleteTransaction() {
        // Show delete confirmation dialog
        _viewState.value = _viewState.value.copy(showDeleteConfirmation = true)
    }
    
    private fun dismissDeleteConfirmation() {
        // Simply hide the dialog
        _viewState.value = _viewState.value.copy(showDeleteConfirmation = false)
    }

    private fun confirmDeleteTransaction() {
        val transaction = _viewState.value.transaction ?: return
        
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(
                isLoading = true,
                showDeleteConfirmation = false
            )

            deleteTransactionUseCase(
                params = DeleteTransactionUseCase.Params(transaction.id),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            _viewState.value = _viewState.value.copy(isLoading = false)
                            emitEvent(DetailTransactionEvent.ShowError("Failed to delete transaction"))
                        },
                        {
                            _viewState.value = _viewState.value.copy(isLoading = false)
                            
                            // Sync financial context so AI sees latest data
                            FinancialContextSyncWorker.enqueueOneTime(appContext)
                            
                            emitEvent(DetailTransactionEvent.TransactionDeleted)
                            emitEvent(DetailTransactionEvent.NavigateBack)
                        }
                    )
                }
            )
        }
    }

    private fun copyTransaction() {
        val transaction = _viewState.value.transaction ?: return
        
        // Set loading state
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            // Create a duplicate transaction with current date
            val currentDate = Date()
            
            // Use SaveTransactionUseCase to create a new transaction with copied details
            val params = SaveTransactionUseCase.Params(
                // Use 0 for transactionId to create a new transaction
                transactionId = 0,
                wallet = transaction.wallet,
                amount = transaction.amount,
                transactionType = transaction.transactionType,
                transactionDate = currentDate, // Use current date for the copy
                description = transaction.description?.let { "Copy of: $it" } ?: "Copy of transaction",
                category = transaction.category,
                withPerson = transaction.withPerson,
                eventName = transaction.eventName,
                hasReminder = transaction.hasReminder,
                excludeFromReport = transaction.excludeFromReport,
                photoUri = transaction.photoUri
            )
            
            saveTransactionUseCase(
                params = params,
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            _viewState.value = _viewState.value.copy(isLoading = false)
                            emitEvent(DetailTransactionEvent.ShowError("Failed to duplicate transaction"))
                        },
                        { newId ->
                            _viewState.value = _viewState.value.copy(isLoading = false)
                            emitEvent(DetailTransactionEvent.TransactionCopied)
                            // Navigate to the new transaction details
//                            emitEvent(DetailTransactionEvent.NavigateToEditTransaction(newId.toInt()))
                        }
                    )
                }
            )
        }
    }

    private fun shareTransaction() {
        val transaction = _viewState.value.transaction ?: return
        
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // Create temporary file for the transaction image
                val file = withContext(Dispatchers.IO) {
                    val tempFile = createImageFileInCache("transaction_${transaction.id}.png")
                    // Note: In a real app, you'd render the TransactionImageCard to a bitmap here
                    // Since we can't directly do that in this implementation, we'll need the UI to handle this
                    tempFile
                }
                
                // Update state to show share dialog with image file
                _viewState.value = _viewState.value.copy(
                    showShareDialog = true,
                    transactionImageFile = file,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error creating transaction image", e)
                _viewState.value = _viewState.value.copy(isLoading = false)
                emitEvent(DetailTransactionEvent.ShowError("Failed to create transaction image"))
            }
        }
    }
    
    private fun dismissShareDialog() {
        _viewState.value = _viewState.value.copy(showShareDialog = false)
    }
    
    private fun openTransactionImage() {
        val imageFile = _viewState.value.transactionImageFile ?: return
        emitEvent(DetailTransactionEvent.OpenImage(imageFile))
    }
    
    private fun shareTransactionImage() {
        val imageFile = _viewState.value.transactionImageFile ?: return
        emitEvent(DetailTransactionEvent.ShareImage(imageFile))
    }

    private fun editTransaction() {
        val transaction = _viewState.value.transaction ?: return
        emitEvent(DetailTransactionEvent.NavigateToEditTransaction(transaction.id))
    }
    
    // Helper method to create an image file in the app's cache directory
    private suspend fun createImageFileInCache(fileName: String): File {
        return withContext(Dispatchers.IO) {
            val cacheDir = appContext.cacheDir
            val file = File(cacheDir, fileName)
            
            if (file.exists()) {
                file.delete()
            }
            
            file.createNewFile()
            file
        }
    }
}