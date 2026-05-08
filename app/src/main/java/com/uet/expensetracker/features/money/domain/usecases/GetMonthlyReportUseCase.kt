package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

/**
 * Use case to get monthly report data including:
 * - Opening and closing balance
 * - Net income
 * - Income and expense breakdown by category
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : UseCase<GetMonthlyReportUseCase.MonthlyReportData, GetMonthlyReportUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, MonthlyReportData> {
        return try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, params.year)
                set(Calendar.MONTH, params.month - 1) // Calendar month is 0-based
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // Start of selected month
            val monthStart = calendar.timeInMillis
            
            // End of selected month
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val monthEnd = calendar.timeInMillis
            
            // Start of previous month (for opening balance)
            calendar.set(Calendar.YEAR, params.year)
            calendar.set(Calendar.MONTH, params.month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val previousMonthEnd = calendar.timeInMillis
            
            // Get transactions for the selected month
            val transactions = transactionRepository.getTransactionsByMonth(
                year = params.year,
                month = params.month,
                walletId = params.walletId
            ).first()
            
            // Get all transactions before selected month (for opening balance)
            val allTransactionsBeforeMonth = transactionRepository.searchTransactionsByDateRange(
                startDate = 0,
                endDate = previousMonthEnd,
                walletId = params.walletId
            ).first()
            
            // Get all wallets
            val walletEntities = walletRepository.getWallets().first()
            val walletsToUse = if (params.walletId != null) {
                walletEntities.filter { it.wallet.id == params.walletId }
            } else {
                walletEntities
            }
            
            // Calculate opening balance (sum of all wallet balances at end of previous month)
            // This is the sum of current balances minus transactions in selected month and after
            // For simplicity, we calculate: current balance - (transactions in selected month + transactions after)
            val allTransactionsAfterMonth = transactionRepository.searchTransactionsByDateRange(
                startDate = monthEnd + 1,
                endDate = Long.MAX_VALUE,
                walletId = params.walletId
            ).first()
            
            val openingBalance = walletsToUse.sumOf { walletEntity ->
                val wallet = walletEntity.wallet
                val walletTransactionsInMonth = transactions.filter { it.wallet.id == wallet.id }
                val walletTransactionsAfter = allTransactionsAfterMonth.filter { it.wallet.id == wallet.id }
                
                // Calculate balance change from transactions in selected month and after
                val balanceChangeInMonth = walletTransactionsInMonth.sumOf { tx ->
                    when (tx.transactionType) {
                        TransactionType.INFLOW -> tx.amount
                        TransactionType.OUTFLOW -> -tx.amount
                    }
                }
                val balanceChangeAfter = walletTransactionsAfter.sumOf { tx ->
                    when (tx.transactionType) {
                        TransactionType.INFLOW -> tx.amount
                        TransactionType.OUTFLOW -> -tx.amount
                    }
                }
                
                // Opening balance = current balance - (change in month + change after)
                wallet.currentBalance - balanceChangeInMonth - balanceChangeAfter
            }
            
            // Calculate closing balance (opening + net change in month)
            val monthIncome = transactions
                .filter { it.transactionType == TransactionType.INFLOW }
                .sumOf { it.amount }
            val monthExpense = transactions
                .filter { it.transactionType == TransactionType.OUTFLOW }
                .sumOf { it.amount }
            val netChange = monthIncome - monthExpense
            val closingBalance = openingBalance + netChange
            
            // Group transactions by category for pie charts
            val incomeByCategory = transactions
                .filter { it.transactionType == TransactionType.INFLOW }
                .groupBy { it.category.metaData }
                .map { (metadata, txs) ->
                    CategoryAmount(
                        categoryMetadata = metadata,
                        categoryDisplayName = txs.first().category.title,
                        amount = txs.sumOf { it.amount }
                    )
                }
            
            val expenseByCategory = transactions
                .filter { it.transactionType == TransactionType.OUTFLOW }
                .groupBy { it.category.metaData }
                .map { (metadata, txs) ->
                    CategoryAmount(
                        categoryMetadata = metadata,
                        categoryDisplayName = txs.first().category.title,
                        amount = txs.sumOf { it.amount }
                    )
                }
            
            val reportData = MonthlyReportData(
                openingBalance = openingBalance,
                closingBalance = closingBalance,
                netIncome = monthIncome - monthExpense,
                totalIncome = monthIncome,
                totalExpense = monthExpense,
                incomeByCategory = incomeByCategory,
                expenseByCategory = expenseByCategory
            )
            
            Either.Right(reportData)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }
    
    data class Params(
        val year: Int,
        val month: Int,
        val walletId: Int? = null
    )
    
    data class MonthlyReportData(
        val openingBalance: Double,
        val closingBalance: Double,
        val netIncome: Double,
        val totalIncome: Double,
        val totalExpense: Double,
        val incomeByCategory: List<CategoryAmount>,
        val expenseByCategory: List<CategoryAmount>
    )
    
    data class CategoryAmount(
        val categoryMetadata: String,
        val categoryDisplayName: String,
        val amount: Double
    )
}

