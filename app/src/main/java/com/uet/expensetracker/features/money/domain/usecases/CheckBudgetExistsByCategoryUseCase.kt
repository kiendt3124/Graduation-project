package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class CheckBudgetExistsByCategoryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) : UseCase<CheckBudgetExistsByCategoryUseCase.Result, CheckBudgetExistsByCategoryUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Result> {
        return try {
            val budgets = budgetRepository.getBudgetsByCategory(params.walletId, params.categoryId).firstOrNull()
            val exists = budgets != null && budgets.isNotEmpty()
            val existingBudgetId = if (exists) budgets?.first()?.budgetId else null
            Either.Right(Result(exists, existingBudgetId))
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }

    data class Params(
        val categoryId: Int,
        val walletId: Int
    )

    data class Result(
        val exists: Boolean,
        val existingBudgetId: Int?
    )
}