package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class SaveTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : UseCase<Long, SaveTransactionUseCase.Params>() {

    // Add this helper function to get a wallet synchronously
    private suspend fun getWalletById(walletId: Int): Wallet? {
        val walletFlow = walletRepository.getWalletById(walletId)
        val walletEntity = walletFlow.firstOrNull()
        return walletEntity?.let { Wallet.fromEntity(it) }
    }

    override suspend fun run(params: Params): Either<Failure, Long> {
        return try {
            // Check if this is an edit operation (transactionId > 0)
            val isEdit = params.transactionId > 0
            
            // If editing, get the original transaction to revert its effect on the wallet balance
            var originalAmount = 0.0
            var originalType: TransactionType? = null
            var originalWalletId = 0
            
            if (isEdit) {
                // Get the original transaction to revert its balance effect
                val originalTransaction = transactionRepository.getTransactionById(params.transactionId)
                if (originalTransaction != null) {
                    originalAmount = originalTransaction.amount
                    originalType = originalTransaction.transactionType
                    originalWalletId = originalTransaction.wallet.id
                    
                    android.util.Log.d("SaveTransaction", "Editing transaction ID: ${params.transactionId}")
                    android.util.Log.d("SaveTransaction", "Original amount: $originalAmount, Original type: $originalType")
                    android.util.Log.d("SaveTransaction", "Original wallet ID: $originalWalletId, New wallet ID: ${params.wallet.id}")
                    android.util.Log.d("SaveTransaction", "New amount: ${params.amount}, New type: ${params.transactionType}")
                }
            }
            
            // Create transaction object with updated details
            val transaction = Transaction(
                id = params.transactionId, 
                wallet = params.wallet,
                amount = params.amount,
                transactionType = params.transactionType,
                transactionDate = params.transactionDate,
                description = params.description,
                category = params.category,
                withPerson = params.withPerson,
                eventName = params.eventName,
                hasReminder = params.hasReminder,
                excludeFromReport = params.excludeFromReport,
                photoUri = params.photoUri
            )

            // Save the transaction
            val id = transactionRepository.saveTransaction(transaction)
            
            if (isEdit && originalType != null) {
                // Check if the wallet changed
                val isWalletChanged = originalWalletId != params.wallet.id
                
                if (isWalletChanged) {
                    // If wallet changed, we need to update both the old and new wallet
                    
                    // 1. Update the original wallet by reverting the original transaction
                    if (originalWalletId > 0) {
                        // Get the original wallet's current balance
                        val originalWallet = getWalletById(originalWalletId)
                        if (originalWallet != null) {
                            val originalWalletBalance = originalWallet.currentBalance
                            
                            // Revert the effect on the original wallet
                            val updatedOriginalWalletBalance = when (originalType) {
                                TransactionType.INFLOW -> originalWalletBalance - originalAmount
                                TransactionType.OUTFLOW -> originalWalletBalance + originalAmount
                            }
                            
                            android.util.Log.d("SaveTransaction", "Original wallet balance: $originalWalletBalance -> $updatedOriginalWalletBalance")
                            
                            // Update the original wallet's balance
                            walletRepository.updateWalletBalance(originalWalletId, updatedOriginalWalletBalance)
                        }
                    }
                    
                    // 2. Update the new wallet by applying only the new transaction
                    val currentBalance = params.wallet.currentBalance
                    val newBalance = when (params.transactionType) {
                        TransactionType.INFLOW -> currentBalance + params.amount
                        TransactionType.OUTFLOW -> currentBalance - params.amount
                    }
                    
                    android.util.Log.d("SaveTransaction", "New wallet balance: $currentBalance -> $newBalance")
                    
                    // Update the new wallet's balance
                    walletRepository.updateWalletBalance(params.wallet.id, newBalance)
                } else {
                    // Same wallet, just update it normally
                    // Update wallet balance
                    val currentBalance = params.wallet.currentBalance
                    var newBalance = currentBalance
                    
                    android.util.Log.d("SaveTransaction", "Current wallet balance: $currentBalance")
                    
                    // Step 1: Revert the effect of the original transaction
                    newBalance = when (originalType) {
                        TransactionType.INFLOW -> currentBalance - originalAmount
                        TransactionType.OUTFLOW -> currentBalance + originalAmount
                    }
                    
                    android.util.Log.d("SaveTransaction", "After reverting original transaction: $newBalance")
                    
                    // Step 2: Apply the effect of the new transaction
                    newBalance = when (params.transactionType) {
                        TransactionType.INFLOW -> newBalance + params.amount
                        TransactionType.OUTFLOW -> newBalance - params.amount
                    }
                    
                    android.util.Log.d("SaveTransaction", "After applying new transaction: $newBalance")
                    
                    // Update the wallet balance in the database
                    walletRepository.updateWalletBalance(params.wallet.id, newBalance)
                }
            } else {
                // For new transactions, simply apply the effect
                val currentBalance = params.wallet.currentBalance
                val newBalance = when (params.transactionType) {
                    TransactionType.INFLOW -> currentBalance + params.amount
                    TransactionType.OUTFLOW -> currentBalance - params.amount
                }
                
                android.util.Log.d("SaveTransaction", "New transaction balance: $newBalance")
                
                // Update the wallet balance in the database
                walletRepository.updateWalletBalance(params.wallet.id, newBalance)
            }
            
            Either.Right(id)
        } catch (e: Exception) {
            android.util.Log.e("SaveTransaction", "Error saving transaction", e)
            Either.Left(Failure.DatabaseError)
        }
    }

    data class Params(
        val transactionId: Int = 0, // Default 0 for new transactions
        val wallet: Wallet,
        val amount: Double,
        val transactionType: TransactionType,
        val transactionDate: Date,
        val description: String?,
        val category: Category,
        val withPerson: String? = null,
        val eventName: String? = null,
        val hasReminder: Boolean = false,
        val excludeFromReport: Boolean = false,
        val photoUri: String? = null
    )
}