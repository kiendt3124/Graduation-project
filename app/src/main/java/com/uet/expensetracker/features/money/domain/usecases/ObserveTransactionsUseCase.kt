package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase

class ObserveTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<DailyTransactions>>, ObserveTransactionsUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<List<DailyTransactions>>> {
        return try {
            val transactionsFlow = when (params) {
                is Params.ByWalletId -> transactionRepository.getTransactionsByWalletId(params.walletId)
                is Params.AllWallets -> transactionRepository.getAllTransactions()
            }

            val resultFlow = transactionsFlow.map { transactions ->
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

                currentMonthTransactions.groupBy { transaction ->
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
            }

            Either.Right(resultFlow)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    sealed class Params {
        object AllWallets : Params()
        data class ByWalletId(val walletId: Int) : Params()
    }
}