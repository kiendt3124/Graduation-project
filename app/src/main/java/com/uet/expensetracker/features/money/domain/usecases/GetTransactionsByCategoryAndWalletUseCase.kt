package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe transactions by category and wallet
 */
class GetTransactionsByCategoryAndWalletUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<Transaction>>, GetTransactionsByCategoryAndWalletUseCase.Params>() {

    /**
     * Run the use case with the provided parameters
     *
     * @param params Parameters containing categoryId and walletId
     * @return Either a Failure or a Flow of transactions that match the filter criteria
     */
    override suspend fun run(params: Params): Either<Failure, Flow<List<Transaction>>> {
        return try {
            Either.Right(transactionRepository.observeFilteredTransactions(params.categoryId, params.walletId))
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    /**
     * Parameters for the use case
     *
     * @property categoryId The ID of the category to filter by, or null for all categories
     * @property walletId The ID of the wallet to filter by, or null for all wallets
     */
    data class Params(
        val categoryId: Int? = null,
        val walletId: Int? = null
    )
} 