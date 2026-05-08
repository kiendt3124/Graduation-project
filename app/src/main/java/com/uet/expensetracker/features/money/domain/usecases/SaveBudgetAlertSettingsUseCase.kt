package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.BudgetAlertSettings
import com.uet.expensetracker.features.money.domain.repository.BudgetAlertRepository
import javax.inject.Inject

class SaveBudgetAlertSettingsUseCase @Inject constructor(
    private val budgetAlertRepository: BudgetAlertRepository
) : UseCase<Long, SaveBudgetAlertSettingsUseCase.Params>() {
    
    override suspend fun run(params: Params): Either<Failure, Long> {
        return try {
            val result = budgetAlertRepository.saveSettings(params.settings)
            Either.Right(result)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }
    
    data class Params(val settings: BudgetAlertSettings)
}

