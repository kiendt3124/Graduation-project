package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.functional.toLeft
import com.uet.expensetracker.core.functional.toRight
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to retrieve wallet information from the repository
 */
class GetWallet @Inject constructor(
    private val repository: WalletRepository
) : UseCase<Flow<List<WalletWithCurrencyEntity>>, UseCase.None>() {
    override suspend fun run(params: None): Either<Failure, Flow<List<WalletWithCurrencyEntity>>> {
        return try {
            repository.getWallets().toRight()
        } catch (exception: Exception) {
            DatabaseFailure(exception.message ?: "Unknown error").toLeft()
        }
    }

    /**
     * Database specific failure
     */
    data class DatabaseFailure(val message: String) : Failure.FeatureFailure()
}