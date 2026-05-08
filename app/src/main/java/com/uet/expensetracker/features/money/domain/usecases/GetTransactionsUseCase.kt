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

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<List<DailyTransactions>, GetTransactionsUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, List<DailyTransactions>> {
        return try {
            val transactions = when (params) {
                is Params.ByWalletId -> transactionRepository.getTransactionsByWalletId(params.walletId).first()
                is Params.All -> transactionRepository.getAllTransactions().first()
            }
            
            val currentMonthStart = Calendar.getInstance().apply {
                time = Date()
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val nextMonthStart = Calendar.getInstance().apply {
                time = Date()
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val currentMonthTransactions = transactions.filter { transaction ->
                val transactionTime = transaction.transactionDate.time
                transactionTime >= currentMonthStart && transactionTime < nextMonthStart
            }
            
            val calendar = Calendar.getInstance()
            
            val groupedByDate = currentMonthTransactions.groupBy { transaction ->
                calendar.time = transaction.transactionDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis 
            }.map { (dayMillis, txns) ->
                val date = Date(dayMillis)
                
                var total = 0.0
                txns.forEach { 
                    total += if (it.transactionType == TransactionType.INFLOW) it.amount else -it.amount
                }
                
                DailyTransactions(date, txns, total)
            }.sortedByDescending { it.date.time } 
            
            Either.Right(groupedByDate)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    sealed class Params {
        object All : Params()
        data class ByWalletId(val walletId: Int) : Params()
    }
}