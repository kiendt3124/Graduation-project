package com.uet.expensetracker.features.money.domain.repository

import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun saveTransaction(transaction: Transaction): Long
    suspend fun getTransactionById(id: Int): Transaction?
    suspend fun getTransactionsByWalletId(walletId: Int): Flow<List<Transaction>>
    suspend fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun deleteTransaction(id: Int)
    fun observeFilteredTransactions(categoryId: Int? = null, walletId: Int? = null): Flow<List<Transaction>>
    
    /**
     * Get transactions for a specific month and year
     * 
     * @param year The year to filter by
     * @param month The month to filter by (1-12)
     * @param walletId Optional wallet ID to filter by, or null for all wallets
     * @return Flow of transactions in the specified month
     */
    fun getTransactionsByMonth(year: Int, month: Int, walletId: Int? = null): Flow<List<Transaction>>
    
    /**
     * Get future transactions (transactions with date greater than current time)
     * 
     * @param walletId Optional wallet ID to filter by, or null for all wallets
     * @return Flow of future transactions
     */
    fun getFutureTransactions(walletId: Int? = null): Flow<List<Transaction>>

    // ==================== SEARCH FUNCTIONALITY ====================

    /**
     * Search transactions with comprehensive filters
     * 
     * @param searchText Text to search in description, eventName, and withPerson fields
     * @param minAmount Minimum amount filter (inclusive)
     * @param maxAmount Maximum amount filter (inclusive)
     * @param categoryIds List of category IDs to filter by (null for all categories)
     * @param walletId Wallet ID to filter by (null for all wallets)
     * @param transactionType Transaction type filter (null for all types)
     * @param startDate Start date filter in milliseconds (inclusive)
     * @param endDate End date filter in milliseconds (inclusive)
     * @return Flow of filtered transactions ordered by date descending
     */
    fun searchTransactions(
        searchText: String? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        categoryIds: List<Int>? = null,
        walletId: Int? = null,
        transactionType: TransactionType? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<Transaction>>

    /**
     * Quick text-only search optimized for real-time search
     * 
     * @param searchText Text to search in transaction descriptions
     * @param walletId Optional wallet ID to filter by
     * @param limit Maximum number of results to return
     * @return Flow of matching transactions limited by count
     */
    fun quickSearchTransactions(
        searchText: String,
        walletId: Int? = null,
        limit: Int = 50
    ): Flow<List<Transaction>>

    /**
     * Search transactions by exact amount
     * 
     * @param amount Exact amount to search for
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions with exact amount match
     */
    fun searchTransactionsByAmount(
        amount: Double,
        walletId: Int? = null
    ): Flow<List<Transaction>>

    /**
     * Search transactions within amount range
     * 
     * @param minAmount Minimum amount (inclusive)
     * @param maxAmount Maximum amount (inclusive)
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions within amount range
     */
    fun searchTransactionsByAmountRange(
        minAmount: Double,
        maxAmount: Double,
        walletId: Int? = null
    ): Flow<List<Transaction>>

    /**
     * Search transactions by date range
     * 
     * @param startDate Start date in milliseconds (inclusive)
     * @param endDate End date in milliseconds (inclusive)
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions within date range
     */
    fun searchTransactionsByDateRange(
        startDate: Long,
        endDate: Long,
        walletId: Int? = null
    ): Flow<List<Transaction>>

    /**
     * Get distinct categories that have transactions (for filter suggestions)
     * 
     * @param walletId Optional wallet ID to filter by
     * @return Flow of category IDs that have transactions
     */
    fun getUsedCategoryIds(walletId: Int? = null): Flow<List<Int>>

    /**
     * Get search suggestions based on recent transaction descriptions
     * 
     * @param searchText Partial text to match
     * @param walletId Optional wallet ID to filter by
     * @param limit Maximum number of suggestions
     * @return Flow of distinct description suggestions
     */
    fun getSearchSuggestions(
        searchText: String,
        walletId: Int? = null,
        limit: Int = 10
    ): Flow<List<String>>

    // ==================== DEBT MANAGEMENT ====================

    /**
     * Get all debt-related transactions by category metadata
     */
    fun getDebtTransactionsByCategories(
        walletId: Int?,
        categoryMetadata: List<String>
    ): Flow<List<Transaction>>

    /**
     * Get transactions by debt reference
     */
    fun getTransactionsByDebtReference(debtReference: String): Flow<List<Transaction>>

    /**
     * Get transactions by parent debt ID
     */
    fun getTransactionsByParentDebtId(parentDebtId: Int): Flow<List<Transaction>>

    /**
     * Get debt transactions with person information
     */
    fun getDebtTransactionsWithPerson(
        walletId: Int?,
        categoryMetadata: List<String>
    ): Flow<List<Transaction>>

    /**
     * Get transactions with debt references
     */
    fun getTransactionsWithDebtReference(walletId: Int?): Flow<List<Transaction>>

    /**
     * Get distinct debt references
     */
    fun getDistinctDebtReferences(walletId: Int?): Flow<List<String>>

    /**
     * Count transactions by debt reference
     */
    suspend fun countTransactionsByDebtReference(debtReference: String): Int

    /**
     * Get the original debt transaction
     */
    suspend fun getOriginalDebtTransaction(debtReference: String): Transaction?

    /**
     * Get payment transactions for a debt reference
     */
    fun getPaymentTransactionsByDebtReference(
        debtReference: String,
        originalTransactionId: Int
    ): Flow<List<Transaction>>
}