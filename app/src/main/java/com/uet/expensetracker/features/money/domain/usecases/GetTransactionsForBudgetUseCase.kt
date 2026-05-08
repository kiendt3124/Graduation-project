package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetTransactionsForBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : UseCase<List<Transaction>, Int>() {

    override suspend fun run(params: Int): Either<Failure, List<Transaction>> {
        return try {
            val budget = budgetRepository.getBudgetById(params)
            if (budget == null) {
                Either.Left(Failure.NotFound)
            } else {
                val transactions = transactionRepository
                    .observeFilteredTransactions(
                        categoryId = budget.category.id,
                        walletId = budget.wallet.id
                    )
                    .first()
                Either.Right(transactions)
            }
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
} 