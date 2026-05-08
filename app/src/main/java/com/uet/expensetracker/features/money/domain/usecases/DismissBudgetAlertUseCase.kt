package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import javax.inject.Inject

class DismissBudgetAlertUseCase @Inject constructor(
    private val budgetAlertRepository: BudgetAlertRepository
) : UseCase<Unit, DismissBudgetAlertUseCase.Params>() {
    
    override suspend fun run(params: Params): Either<Failure, Unit> {
        return try {
            when (params) {
                is Params.ByAlertId -> budgetAlertRepository.dismissAlert(params.alertId)
                is Params.AllForBudget -> budgetAlertRepository.dismissAllAlertsForBudget(params.budgetId)
            }
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
    
    sealed class Params {
        data class ByAlertId(val alertId: String) : Params()
        data class AllForBudget(val budgetId: Int) : Params()
    }
}

