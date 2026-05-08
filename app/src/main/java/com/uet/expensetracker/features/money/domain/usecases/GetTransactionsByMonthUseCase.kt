package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Use case to get transactions for a specific month and year, grouped by date
 */
class GetTransactionsByMonthUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<List<DailyTransactions>, GetTransactionsByMonthUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, List<DailyTransactions>> {
        return try {
            // Get transactions for the specified month
            val transactions = transactionRepository.getTransactionsByMonth(
                year = params.year,
                month = params.month,
                walletId = params.walletId
            ).first()

            // Group transactions by date and create DailyTransactions
            val dailyTransactionsList = transactions
                .groupBy { transaction ->
                    // Normalize transaction date to start of day for grouping
                    val calendar = Calendar.getInstance()
                    calendar.time = transaction.transactionDate
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.time
                }
                .map { (date, transactionsForDate) ->
                    // Calculate daily total (inflow - outflow)
                    val dailyTotal = transactionsForDate.sumOf { transaction ->
                        when (transaction.transactionType) {
                            TransactionType.INFLOW -> transaction.amount
                            TransactionType.OUTFLOW -> -transaction.amount
                        }
                    }

                    DailyTransactions.create(
                        date = date,
                        unsortedTransactions = transactionsForDate,
                        dailyTotal = dailyTotal
                    )
                }
                .sortedByDescending { it.date } // Sort by date, newest first

            Either.Right(dailyTransactionsList)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }

    data class Params(
        val year: Int,
        val month: Int,
        val walletId: Int? = null
    )
} 