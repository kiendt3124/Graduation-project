package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting categories that have transactions
 * 
 * This use case provides a list of category IDs that have at least one transaction,
 * useful for populating category filter dropdowns in search functionality.
 * 
 * @param transactionRepository Repository for accessing transaction data
 */
class GetUsedCategoriesUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<Int>>, GetUsedCategoriesUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<List<Int>>> {
        return try {
            val categoriesFlow = transactionRepository.getUsedCategoryIds(
                walletId = params.walletId
            )

            Either.Right(categoriesFlow)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    /**
     * Parameters for getting used categories
     * 
     * @param walletId Optional wallet ID to filter categories by (null for all wallets)
     */
    data class Params(
        val walletId: Int? = null
    )
} 