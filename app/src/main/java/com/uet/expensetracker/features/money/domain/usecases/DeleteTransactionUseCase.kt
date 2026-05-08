package com.uet.expensetracker.features.money.domain.usecases

import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case for deleting a transaction and updating wallet balance
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : UseCase<Unit, DeleteTransactionUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Unit> {
        return try {
            // 1. Get the transaction to be deleted
            val transaction = transactionRepository.getTransactionById(params.transactionId)
            if (transaction == null) {
                Log.e("DeleteTransaction", "Transaction not found with ID: ${params.transactionId}")
                return Either.Left(Failure.NotFound)
            }
            
            Log.d("DeleteTransaction", "Deleting transaction with ID: ${params.transactionId}")
            Log.d("DeleteTransaction", "Transaction amount: ${transaction.amount}, type: ${transaction.transactionType}")
            
            // 2. Update wallet balance by reversing the transaction's effect
            val walletId = transaction.wallet.id
            val walletFlow = walletRepository.getWalletById(walletId)
            val wallet = walletFlow.firstOrNull()?.let { Wallet.fromEntity(it) }
            
            if (wallet != null) {
                val currentBalance = wallet.currentBalance
                
                // Calculate new balance by reversing the transaction's effect
                val newBalance = when (transaction.transactionType) {
                    TransactionType.INFLOW -> currentBalance - transaction.amount
                    TransactionType.OUTFLOW -> currentBalance + transaction.amount
                }
                
                Log.d("DeleteTransaction", "Updating wallet balance from $currentBalance to $newBalance")
                
                // Update wallet balance
                walletRepository.updateWalletBalance(walletId, newBalance)
            } else {
                Log.e("DeleteTransaction", "Wallet not found with ID: $walletId")
                return Either.Left(Failure.NotFound)
            }
            
            // 3. Delete the transaction
            transactionRepository.deleteTransaction(params.transactionId)
            
            Either.Right(Unit)
        } catch (e: Exception) {
            Log.e("DeleteTransaction", "Error deleting transaction", e)
            Either.Left(Failure.DatabaseError)
        }
    }
    
    data class Params(val transactionId: Int)
} 