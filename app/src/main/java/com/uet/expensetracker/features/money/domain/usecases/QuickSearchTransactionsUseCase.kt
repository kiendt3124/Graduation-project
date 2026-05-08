package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for quick transaction search optimized for real-time search
 * 
 * This use case provides fast text-only search functionality optimized for
 * real-time search as users type. It limits results for better performance.
 * 
 * @param transactionRepository Repository for accessing transaction data
 */
class QuickSearchTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<Transaction>>, QuickSearchTransactionsUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<List<Transaction>>> {
        return try {
            // Validate search text
            if (params.searchText.isBlank()) {
                return Either.Left(SearchFailure.EmptySearchQuery)
            }

            // Minimum search length for performance
            if (params.searchText.length < MIN_SEARCH_LENGTH) {
                return Either.Left(SearchFailure.SearchTextTooShort)
            }

            val transactionsFlow = transactionRepository.quickSearchTransactions(
                searchText = params.searchText.trim(),
                walletId = params.walletId,
                limit = params.limit
            )

            Either.Right(transactionsFlow)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    /**
     * Parameters for quick search
     * 
     * @param searchText Text to search in transaction descriptions (minimum 2 characters)
     * @param walletId Optional wallet ID to filter by (null for all wallets)
     * @param limit Maximum number of results to return (default 50)
     */
    data class Params(
        val searchText: String,
        val walletId: Int? = null,
        val limit: Int = DEFAULT_LIMIT
    ) {
        init {
            require(limit > 0) { "Limit must be positive" }
            require(limit <= MAX_LIMIT) { "Limit cannot exceed $MAX_LIMIT" }
        }
    }

    companion object {
        private const val MIN_SEARCH_LENGTH = 2
        private const val DEFAULT_LIMIT = 50
        private const val MAX_LIMIT = 100
    }
}

 