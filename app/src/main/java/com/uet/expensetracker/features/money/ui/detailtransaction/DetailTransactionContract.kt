package com.uet.expensetracker.features.money.ui.detailtransaction

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.Contact
import java.io.File

// Represents the state of the DetailTransaction screen
data class DetailTransactionState(
    val transaction: Transaction? = null,
    val isLoading: Boolean = true,
    val error: Throwable? = null,
    val showDeleteConfirmation: Boolean = false,
    val showShareDialog: Boolean = false,
    val transactionImageFile: File? = null,
    val listContacts: List<Contact> = emptyList()
) : MviStateBase

// Represents user actions on the DetailTransaction screen
sealed interface DetailTransactionIntent : MviIntentBase {
    data class LoadTransaction(val id: Int) : DetailTransactionIntent
    data object DeleteTransaction : DetailTransactionIntent
    data object ConfirmDeleteTransaction : DetailTransactionIntent
    data object DismissDeleteConfirmation : DetailTransactionIntent
    data object CopyTransaction : DetailTransactionIntent
    data object ShareTransaction : DetailTransactionIntent
    data object OpenTransactionImage : DetailTransactionIntent
    data object ShareTransactionImage : DetailTransactionIntent
    data object DismissShareDialog : DetailTransactionIntent
    data object EditTransaction : DetailTransactionIntent
}

// Represents one-time events that can occur on the DetailTransaction screen
sealed interface DetailTransactionEvent : MviEventBase {
    data object NavigateBack : DetailTransactionEvent
    data object TransactionDeleted : DetailTransactionEvent
    data object TransactionCopied : DetailTransactionEvent
    data class NavigateToEditTransaction(val transactionId: Int) : DetailTransactionEvent
    data class ShowError(val message: String) : DetailTransactionEvent
    data class OpenImage(val imageFile: File) : DetailTransactionEvent
    data class ShareImage(val imageFile: File) : DetailTransactionEvent
}