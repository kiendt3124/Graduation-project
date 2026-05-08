package com.uet.expensetracker.features.money.domain.usecases

import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.CategoryRepository
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

/**
 * Params for the TransferMoneyUseCase
 */
data class TransferMoneyParams(
    val fromWallet: Wallet,
    val toWallet: Wallet,
    val amount: Double,
    val toAmount: Double? = null,
    val note: String?,
    val date: Date,
    val excludeFromReport: Boolean,
    val addTransferFee: Boolean = false,
    val transferFee: Double = 0.0
)

/**
 * Use case for transferring money between wallets
 */
class TransferMoneyUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository
) : UseCase<Boolean, TransferMoneyParams>() {

    override suspend fun run(params: TransferMoneyParams): Either<Failure, Boolean> {
        return try {
            // 1. Get transfer categories
            val outgoingTransferCategory = getTransferCategory(TransactionType.OUTFLOW)
            val incomingTransferCategory = getTransferCategory(TransactionType.INFLOW)
            
            // 2. Create the outgoing transaction (from source wallet)
            val outgoingTransaction = Transaction(
                id = 0, // Repository will assign ID
                wallet = params.fromWallet,
                transactionType = TransactionType.OUTFLOW,
                amount = params.amount,
                transactionDate = params.date,
                description = params.note ?: "Transfer to ${params.toWallet.walletName}",
                category = outgoingTransferCategory,
                excludeFromReport = params.excludeFromReport
            )
            
            // 3. Create the incoming transaction (to destination wallet)
            // Use toAmount if provided (different currency), otherwise use the original amount
            val incomingAmount = params.toAmount ?: params.amount
            
            val incomingTransaction = Transaction(
                id = 0, // Repository will assign ID
                wallet = params.toWallet,
                transactionType = TransactionType.INFLOW,
                amount = incomingAmount,
                transactionDate = params.date,
                description = params.note ?: "Transfer from ${params.fromWallet.walletName}",
                category = incomingTransferCategory,
                excludeFromReport = params.excludeFromReport
            )
            
            // 4. If transfer fee is enabled, create a fee transaction
            if (params.addTransferFee && params.transferFee > 0) {
                val feeTransaction = Transaction(
                    id = 0,
                    wallet = params.fromWallet,
                    transactionType = TransactionType.OUTFLOW,
                    amount = params.transferFee,
                    transactionDate = params.date,
                    description = "Transfer fee",
                    category = outgoingTransferCategory,
                    excludeFromReport = params.excludeFromReport
                )
                transactionRepository.saveTransaction(feeTransaction)
            }
            
            // 5. Save both transactions
            Log.d("TransferMoneyUseCase", "Saving outgoing transaction: $outgoingTransaction")
            transactionRepository.saveTransaction(outgoingTransaction)
            Log.d("TransferMoneyUseCase", "Saving incoming transaction: $incomingTransaction")
            transactionRepository.saveTransaction(incomingTransaction)
            
            // 6. Update wallet balances
            // Note: In a real app, you might want to do this in a transaction to ensure atomicity
            val fromWalletBalance = params.fromWallet.currentBalance - params.amount - 
                (if (params.addTransferFee) params.transferFee else 0.0)
            val toWalletBalance = params.toWallet.currentBalance + incomingAmount
            
            Log.d("TransferMoneyUseCase", "Updating from wallet (ID: ${params.fromWallet.id}) balance: $fromWalletBalance")
            Log.d("TransferMoneyUseCase", "Updating to wallet (ID: ${params.toWallet.id}) balance: $toWalletBalance")
            
            // 7. Update wallet balances
            walletRepository.updateWalletBalance(params.fromWallet.id, fromWalletBalance)
            walletRepository.updateWalletBalance(params.toWallet.id, toWalletBalance)

            Log.i("TransferMoneyUseCase", "run: Transfer completed successfully")
            Either.Right(true)
        } catch (e: Exception) {
            // Handle errors
            e.printStackTrace()
            Either.Left(Failure.ServerError)
        }
    }
    
    private suspend fun getTransferCategory(transactionType: TransactionType): Category {
        // Get the appropriate category type based on transaction type
        val categoryType = when (transactionType) {
            TransactionType.OUTFLOW -> CategoryType.EXPENSE
            TransactionType.INFLOW -> CategoryType.INCOME
            else -> CategoryType.EXPENSE // Default fallback
        }
        
        // Get the metadata identifier based on transaction type
        val metaData = when (transactionType) {
            TransactionType.OUTFLOW -> "IS_OUTGOING_TRANSFER"
            TransactionType.INFLOW -> "IS_INCOMING_TRANSFER"
            else -> "IS_OUTGOING_TRANSFER" // Default fallback
        }
        
        // Get categories of the specified type and collect the Flow
        val categories = categoryRepository.getCategoriesByType(categoryType).first()
        
        // Find the transfer category with matching metadata
        val transferCategory = categories.find { it.metaData == metaData }
        
        // Return the found category or throw an exception if not found
        return transferCategory ?: throw IllegalStateException(
            "Transfer category not found for type $transactionType with metadata $metaData. Please ensure categories are initialized properly."
        )
    }
} 