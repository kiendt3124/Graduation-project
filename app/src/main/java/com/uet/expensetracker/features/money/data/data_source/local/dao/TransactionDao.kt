package com.uet.expensetracker.features.money.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.uet.expensetracker.features.money.data.data_source.local.model.TransactionEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.TransactionWithDetails
import com.uet.expensetracker.features.money.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionWithDetails?

    @Transaction
    @Query("SELECT * FROM transactions WHERE walletId = :walletId")
    fun getTransactionsByWalletId(walletId: Int): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<TransactionWithDetails>>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Int)

    /**
     * Get transactions filtered by category and wallet
     * 
     * @param categoryId Category ID to filter by, or null for all categories
     * @param walletId Wallet ID to filter by, or null for all wallets
     * @return Flow of filtered transactions
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:walletId IS NULL OR walletId = :walletId)
    """)
    fun getFilteredTransactions(categoryId: Int?, walletId: Int?): Flow<List<TransactionWithDetails>>

    /**
     * Get transactions for a specific month and year
     * 
     * @param year The year to filter by (e.g., "2024")
     * @param monthFormatted The month to filter by in 2-digit format (e.g., "01", "12")
     * @param walletId Optional wallet ID to filter by, or null for all wallets
     * @return Flow of transactions in the specified month, ordered by date descending
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE strftime('%Y', transactionDate / 1000, 'unixepoch') = :year
        AND strftime('%m', transactionDate / 1000, 'unixepoch') = :monthFormatted
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
    """)
    fun getTransactionsByMonth(
        year: String, 
        monthFormatted: String, 
        walletId: Int? = null
    ): Flow<List<TransactionWithDetails>>

    /**
     * Get future transactions (transactions with date greater than current time)
     * 
     * @param walletId Optional wallet ID to filter by, or null for all wallets
     * @return Flow of future transactions, ordered by date ascending
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE transactionDate > strftime('%s', 'now') * 1000
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate ASC
    """)
    fun getFutureTransactions(walletId: Int? = null): Flow<List<TransactionWithDetails>>

    /**
     * Search transactions with text query and comprehensive filters
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
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (:searchText IS NULL OR :searchText = '' OR 
               LOWER(COALESCE(description, '')) LIKE '%' || LOWER(:searchText) || '%' OR
               LOWER(COALESCE(eventName, '')) LIKE '%' || LOWER(:searchText) || '%' OR
               LOWER(COALESCE(withPerson, '')) LIKE '%' || LOWER(:searchText) || '%')
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (:categoryIds IS NULL OR categoryId IN (:categoryIds))
        AND (:walletId IS NULL OR walletId = :walletId)
        AND (:transactionType IS NULL OR transactionType = :transactionType)
        AND (:startDate IS NULL OR transactionDate >= :startDate)
        AND (:endDate IS NULL OR transactionDate <= :endDate)
        ORDER BY transactionDate DESC
    """)
    fun searchTransactions(
        searchText: String? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        categoryIds: List<Int>? = null,
        walletId: Int? = null,
        transactionType: TransactionType? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<TransactionWithDetails>>

    /**
     * Quick text-only search in transaction descriptions
     * Optimized for real-time search suggestions
     * 
     * @param searchText Text to search in description field
     * @param walletId Optional wallet ID to filter by
     * @param limit Maximum number of results to return
     * @return Flow of matching transactions limited by count
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (:searchText IS NULL OR :searchText = '' OR 
               LOWER(COALESCE(description, '')) LIKE '%' || LOWER(:searchText) || '%')
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
        LIMIT :limit
    """)
    fun quickSearchTransactions(
        searchText: String,
        walletId: Int? = null,
        limit: Int = 50
    ): Flow<List<TransactionWithDetails>>

    /**
     * Search transactions by exact amount
     * 
     * @param amount Exact amount to search for
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions with exact amount match
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
    """)
    fun searchTransactionsByAmount(
        amount: Double,
        walletId: Int? = null
    ): Flow<List<TransactionWithDetails>>

    /**
     * Search transactions within amount range
     * 
     * @param minAmount Minimum amount (inclusive)
     * @param maxAmount Maximum amount (inclusive)
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions within amount range
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE amount BETWEEN :minAmount AND :maxAmount
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
    """)
    fun searchTransactionsByAmountRange(
        minAmount: Double,
        maxAmount: Double,
        walletId: Int? = null
    ): Flow<List<TransactionWithDetails>>

    /**
     * Search transactions by date range
     * 
     * @param startDate Start date in milliseconds (inclusive)
     * @param endDate End date in milliseconds (inclusive)
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions within date range
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE transactionDate BETWEEN :startDate AND :endDate
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
    """)
    fun searchTransactionsByDateRange(
        startDate: Long,
        endDate: Long,
        walletId: Int? = null
    ): Flow<List<TransactionWithDetails>>

    /**
     * Get distinct categories from transactions (for filter suggestions)
     * 
     * @param walletId Optional wallet ID to filter by
     * @return Flow of category IDs that have transactions
     */
    @Query("""
        SELECT DISTINCT categoryId FROM transactions 
        WHERE (:walletId IS NULL OR walletId = :walletId)
        ORDER BY categoryId
    """)
    fun getUsedCategoryIds(walletId: Int? = null): Flow<List<Int>>

    /**
     * Get search suggestions based on recent transaction descriptions
     * 
     * @param searchText Partial text to match
     * @param walletId Optional wallet ID to filter by
     * @param limit Maximum number of suggestions
     * @return Flow of distinct description suggestions
     */
    @Query("""
        SELECT DISTINCT TRIM(description) as suggestion FROM transactions 
        WHERE description IS NOT NULL 
        AND description != ''
        AND LOWER(description) LIKE '%' || LOWER(:searchText) || '%'
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
        LIMIT :limit
    """)
    fun getSearchSuggestions(
        searchText: String,
        walletId: Int? = null,
        limit: Int = 10
    ): Flow<List<String>>

    // ==================== DEBT MANAGEMENT QUERIES ====================

    /**
     * Get all debt-related transactions filtered by category metadata
     * 
     * @param walletId Optional wallet ID to filter by (null for all wallets)
     * @param categoryMetadata List of category metadata to filter by (e.g., IS_DEBT, IS_LOAN)
     * @return Flow of debt transactions ordered by date descending
     */
    @Transaction
    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE (:walletId IS NULL OR t.walletId = :walletId)
        AND c.metaData IN (:categoryMetadata)
        ORDER BY t.transactionDate DESC
    """)
    fun getDebtTransactionsByCategories(
        walletId: Int? = null,
        categoryMetadata: List<String>
    ): Flow<List<TransactionWithDetails>>

    /**
     * Get transactions by debt reference for tracking payment history
     * 
     * @param debtReference The debt reference to search for
     * @return Flow of transactions linked to the debt reference, ordered by date
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE debtReference = :debtReference
        ORDER BY transactionDate ASC
    """)
    fun getTransactionsByDebtReference(debtReference: String): Flow<List<TransactionWithDetails>>

    /**
     * Get transactions by parent debt ID
     * 
     * @param parentDebtId The parent debt transaction ID
     * @return Flow of child transactions linked to the parent debt
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE parentDebtId = :parentDebtId
        ORDER BY transactionDate ASC
    """)
    fun getTransactionsByParentDebtId(parentDebtId: Int): Flow<List<TransactionWithDetails>>

    /**
     * Get debt transactions grouped by person for analytics
     * This is a helper query to get all debt transactions that can be grouped by person
     * 
     * @param walletId Optional wallet ID to filter by
     * @param categoryMetadata List of category metadata for debt categories
     * @return Flow of debt transactions with person information
     */
    @Transaction
    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE (:walletId IS NULL OR t.walletId = :walletId)
        AND c.metaData IN (:categoryMetadata)
        AND t.withPerson IS NOT NULL
        AND t.withPerson != ''
        ORDER BY t.transactionDate DESC
    """)
    fun getDebtTransactionsWithPerson(
        walletId: Int? = null,
        categoryMetadata: List<String>
    ): Flow<List<TransactionWithDetails>>

    /**
     * Get all transactions that have debt references (new debt tracking system)
     * 
     * @param walletId Optional wallet ID to filter by
     * @return Flow of transactions with debt references
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE debtReference IS NOT NULL
        AND debtReference != ''
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY transactionDate DESC
    """)
    fun getTransactionsWithDebtReference(walletId: Int? = null): Flow<List<TransactionWithDetails>>

    /**
     * Get distinct debt references for a wallet
     * 
     * @param walletId Optional wallet ID to filter by
     * @return Flow of distinct debt reference strings
     */
    @Query("""
        SELECT DISTINCT debtReference FROM transactions 
        WHERE debtReference IS NOT NULL
        AND debtReference != ''
        AND (:walletId IS NULL OR walletId = :walletId)
        ORDER BY debtReference
    """)
    fun getDistinctDebtReferences(walletId: Int? = null): Flow<List<String>>

    /**
     * Count transactions by debt reference
     * Useful for determining if a debt has multiple payment transactions
     * 
     * @param debtReference The debt reference to count
     * @return Number of transactions with this debt reference
     */
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE debtReference = :debtReference
    """)
    suspend fun countTransactionsByDebtReference(debtReference: String): Int

    /**
     * Get the original debt transaction by debt reference
     * Finds the earliest transaction with the given debt reference
     * 
     * @param debtReference The debt reference to search for
     * @return The original debt transaction, or null if not found
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE debtReference = :debtReference
        ORDER BY transactionDate ASC
        LIMIT 1
    """)
    suspend fun getOriginalDebtTransaction(debtReference: String): TransactionWithDetails?

    /**
     * Get payment transactions for a debt reference (excluding the original)
     * 
     * @param debtReference The debt reference to search for
     * @param originalTransactionId The original transaction ID to exclude
     * @return Flow of payment transactions
     */
    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE debtReference = :debtReference
        AND id != :originalTransactionId
        ORDER BY transactionDate ASC
    """)
    fun getPaymentTransactionsByDebtReference(
        debtReference: String,
        originalTransactionId: Int
    ): Flow<List<TransactionWithDetails>>
}