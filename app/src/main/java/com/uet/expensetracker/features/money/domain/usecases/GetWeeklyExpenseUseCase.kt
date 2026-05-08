package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class GetWeeklyExpenseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Double, UseCase.None>() {

    override suspend fun run(params: None): Either<Failure, Double> {
        return try {
            val allTxns = transactionRepository.getAllTransactions().first()
            val now = Calendar.getInstance().timeInMillis
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val total = allTxns.filter { txn ->
                txn.transactionType == TransactionType.OUTFLOW &&
                txn.transactionDate.time >= weekStart
            }.sumOf { it.amount }

            Either.Right(total)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
} 