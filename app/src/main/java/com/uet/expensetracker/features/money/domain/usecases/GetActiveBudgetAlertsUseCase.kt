package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.BudgetAlert
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveBudgetAlertsUseCase @Inject constructor(
    private val budgetAlertRepository: BudgetAlertRepository
) : UseCase<Flow<List<BudgetAlert>>, GetActiveBudgetAlertsUseCase.Params>() {
    
    override suspend fun run(params: Params): Either<Failure, Flow<List<BudgetAlert>>> {
        return try {
            val result = when (params) {
                is Params.ByBudgetId -> budgetAlertRepository.getActiveAlertsByBudget(params.budgetId)
                is Params.All -> budgetAlertRepository.getAllActiveAlerts()
            }
            Either.Right(result)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
    
    sealed class Params {
        data class ByBudgetId(val budgetId: Int) : Params()
        data object All : Params()
    }
}

