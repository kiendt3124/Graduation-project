package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for comprehensive transaction search with multiple filters
 * 
 * This use case provides powerful search functionality allowing users to filter
 * transactions by text, amount range, categories, wallet, transaction type, and date range.
 * 
 * @param transactionRepository Repository for accessing transaction data
 */
class SearchTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<Flow<List<Transaction>>, SearchTransactionsUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<List<Transaction>>> {
        return try {
            // Validate parameters
            if (params.minAmount != null && params.maxAmount != null && params.minAmount > params.maxAmount) {
                return Either.Left(SearchFailure.InvalidAmountRange)
            }
            
            if (params.startDate != null && params.endDate != null && params.startDate > params.endDate) {
                return Either.Left(SearchFailure.InvalidDateRange)
            }

            val transactionsFlow = transactionRepository.searchTransactions(
                searchText = params.searchText?.takeIf { it.isNotBlank() },
                minAmount = params.minAmount,
                maxAmount = params.maxAmount,
                categoryIds = params.categoryIds?.takeIf { it.isNotEmpty() },
                walletId = params.walletId,
                transactionType = params.transactionType,
                startDate = params.startDate,
                endDate = params.endDate
            )

            Either.Right(transactionsFlow)
        } catch (e: Exception) {
            Either.Left(Failure.ServerError)
        }
    }

    /**
     * Parameters for comprehensive search
     * 
     * @param searchText Text to search in description, event name, and with person fields
     * @param minAmount Minimum amount filter (inclusive)
     * @param maxAmount Maximum amount filter (inclusive)
     * @param categoryIds List of category IDs to filter by
     * @param walletId Wallet ID to filter by (null for all wallets)
     * @param transactionType Transaction type filter (INFLOW/OUTFLOW)
     * @param startDate Start date filter in milliseconds (inclusive)
     * @param endDate End date filter in milliseconds (inclusive)
     */
    data class Params(
        val searchText: String? = null,
        val minAmount: Double? = null,
        val maxAmount: Double? = null,
        val categoryIds: List<Int>? = null,
        val walletId: Int? = null,
        val transactionType: TransactionType? = null,
        val startDate: Long? = null,
        val endDate: Long? = null
    )
}

 