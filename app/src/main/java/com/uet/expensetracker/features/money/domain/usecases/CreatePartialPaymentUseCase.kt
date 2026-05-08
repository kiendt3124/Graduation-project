package com.uet.expensetracker.features.money.domain.usecases

import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case for creating partial debt payments
 * This creates a payment transaction linked to an original debt transaction
 * and updates the wallet balance accordingly
 */
class CreatePartialPaymentUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : UseCase<Transaction, CreatePartialPaymentUseCase.Params>() {

    companion object {
        private const val TAG = "CreatePartialPaymentUseCase"
    }

    override suspend fun run(params: Params): Either<Failure, Transaction> {
        return try {
            Log.d(TAG, "Creating partial payment for debt: ${params.originalDebtId}")
            
            // Create the payment transaction with debt reference info
            val paymentTransaction = params.paymentTransaction.copy(
                parentDebtId = params.originalDebtId,
                debtReference = params.debtReference
            )
            
            // Save the payment transaction
            val transactionId = transactionRepository.saveTransaction(paymentTransaction)
            
            // Get the saved transaction with updated ID
            val savedTransaction = transactionRepository.getTransactionById(transactionId.toInt())
                ?: return Either.Left(Failure.DatabaseError)
            
            // Update wallet balance based on transaction type
            val wallet = savedTransaction.wallet
            val currentBalance = wallet.currentBalance
            val newBalance = when (savedTransaction.transactionType) {
                TransactionType.INFLOW -> currentBalance + savedTransaction.amount
                TransactionType.OUTFLOW -> currentBalance - savedTransaction.amount
            }
            
            // Update wallet balance in database
            walletRepository.updateWalletBalance(wallet.id, newBalance)
            
            Log.d(TAG, "Created partial payment transaction with ID: $transactionId")
            Log.d(TAG, "Updated wallet ${wallet.id} balance: $currentBalance -> $newBalance")
            
            Either.Right(savedTransaction)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating partial payment", e)
            Either.Left(Failure.ServerError)
        }
    }

    data class Params(
        val originalDebtId: Int,
        val debtReference: String,
        val paymentTransaction: Transaction
    )
} 