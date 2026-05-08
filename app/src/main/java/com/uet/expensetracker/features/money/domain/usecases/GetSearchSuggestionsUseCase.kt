package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting search suggestions based on recent transaction descriptions
 * 
 * This use case provides autocomplete suggestions to help users quickly find
 * transactions based on previously used descriptions.
 * 
 * @param transactionRepository Repository for accessing transaction data
 */
class GetSearchSuggestionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<String>>, GetSearchSuggestionsUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<List<String>>> {
        return try {
            // Validate search text
            if (params.searchText.isBlank()) {
                return Either.Left(SearchFailure.EmptySearchQuery)
            }

            // Minimum search length for meaningful suggestions
            if (params.searchText.length < MIN_SEARCH_LENGTH) {
                return Either.Left(SearchFailure.SearchTextTooShort)
            }

            val suggestionsFlow = transactionRepository.getSearchSuggestions(
                searchText = params.searchText.trim(),
                walletId = params.walletId,
                limit = params.limit
            ).map { suggestions ->
                // Filter and clean suggestions
                suggestions
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(params.limit)
            }

            Either.Right(suggestionsFlow)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    /**
     * Parameters for search suggestions
     * 
     * @param searchText Partial text to match against transaction descriptions
     * @param walletId Optional wallet ID to filter suggestions by (null for all wallets)
     * @param limit Maximum number of suggestions to return (default 10)
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
        private const val MIN_SEARCH_LENGTH = 1
        private const val DEFAULT_LIMIT = 10
        private const val MAX_LIMIT = 20
    }
}

/**
 * Data class representing a search suggestion with metadata
 */
data class SearchSuggestion(
    val text: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
) 