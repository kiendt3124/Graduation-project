package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetsByWalletUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) : UseCase<Flow<List<Budget>>, GetBudgetsByWalletUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<List<Budget>>> {
        return try {
            when (params) {
                is Params.ByWalletId -> Either.Right(budgetRepository.getBudgetsByWallet(params.walletId))
                is Params.TotalWallets -> {

                    Either.Right(budgetRepository.getBudgetsFromTotalWallet())
                }
            }
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    sealed class Params {
        data class ByWalletId(val walletId: Int) : Params()
        data object TotalWallets : Params()
    }
}