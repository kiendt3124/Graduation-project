package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import javax.inject.Inject

class GetBudgetByIdUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) : UseCase<Budget, Int>() {

    override suspend fun run(params: Int): Either<Failure, Budget> {
        return try {
            val budget = budgetRepository.getBudgetById(params)
            if (budget != null) {
                Either.Right(budget)
            } else {
                Either.Left(Failure.NotFound)
            }
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
} 