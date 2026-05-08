package com.uet.expensetracker.features.money.data.repository

import com.uet.expensetracker.features.money.data.data_source.local.dao.TransactionDao
import com.uet.expensetracker.features.money.data.data_source.local.model.TransactionEntity
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun saveTransaction(transaction: Transaction): Long {
        val transactionEntity = TransactionEntity.fromTransaction(transaction)
        return transactionDao.insertTransaction(transactionEntity)
    }

    override suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)?.toTransaction()
    }

    override suspend fun getTransactionsByWalletId(walletId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByWalletId(walletId).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    override suspend fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    override suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransaction(id)
    }
    
    /**
     * Observe transactions filtered by category and wallet
     *
     * @param categoryId Optional category ID to filter by
     * @param walletId Optional wallet ID to filter by
     * @return Flow of filtered transactions
     */
    override fun observeFilteredTransactions(categoryId: Int?, walletId: Int?): Flow<List<Transaction>> {
        return transactionDao.getFilteredTransactions(categoryId, walletId).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get transactions for a specific month and year
     *
     * @param year The year to filter by
     * @param month The month to filter by (1-12)
     * @param walletId Optional wallet ID to filter by, or null for all wallets
     * @return Flow of transactions in the specified month
     */
    override fun getTransactionsByMonth(year: Int, month: Int, walletId: Int?): Flow<List<Transaction>> {
        // Format month to ensure it's 2 digits (01-12)
        val monthFormatted = String.format(java.util.Locale.US, "%02d", month)
        
        return transactionDao.getTransactionsByMonth(
            year.toString(), 
            monthFormatted, 
            walletId
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get future transactions (transactions with date greater than current time)
     *
     * @param walletId Optional wallet ID to filter by, or null for all wallets
     * @return Flow of future transactions
     */
    override fun getFutureTransactions(walletId: Int?): Flow<List<Transaction>> {
        return transactionDao.getFutureTransactions(walletId).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    // ==================== SEARCH FUNCTIONALITY IMPLEMENTATIONS ====================

    /**
     * Search transactions with comprehensive filters
     */
    override fun searchTransactions(
        searchText: String?,
        minAmount: Double?,
        maxAmount: Double?,
        categoryIds: List<Int>?,
        walletId: Int?,
        transactionType: TransactionType?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(
            searchText = searchText,
            minAmount = minAmount,
            maxAmount = maxAmount,
            categoryIds = categoryIds,
            walletId = walletId,
            transactionType = transactionType, // Pass enum directly
            startDate = startDate,
            endDate = endDate
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Quick text-only search optimized for real-time search
     */
    override fun quickSearchTransactions(
        searchText: String,
        walletId: Int?,
        limit: Int
    ): Flow<List<Transaction>> {
        return transactionDao.quickSearchTransactions(
            searchText = searchText,
            walletId = walletId,
            limit = limit
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Search transactions by exact amount
     */
    override fun searchTransactionsByAmount(
        amount: Double,
        walletId: Int?
    ): Flow<List<Transaction>> {
        return transactionDao.searchTransactionsByAmount(
            amount = amount,
            walletId = walletId
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Search transactions within amount range
     */
    override fun searchTransactionsByAmountRange(
        minAmount: Double,
        maxAmount: Double,
        walletId: Int?
    ): Flow<List<Transaction>> {
        return transactionDao.searchTransactionsByAmountRange(
            minAmount = minAmount,
            maxAmount = maxAmount,
            walletId = walletId
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Search transactions by date range
     */
    override fun searchTransactionsByDateRange(
        startDate: Long,
        endDate: Long,
        walletId: Int?
    ): Flow<List<Transaction>> {
        return transactionDao.searchTransactionsByDateRange(
            startDate = startDate,
            endDate = endDate,
            walletId = walletId
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get distinct categories that have transactions (for filter suggestions)
     */
    override fun getUsedCategoryIds(walletId: Int?): Flow<List<Int>> {
        return transactionDao.getUsedCategoryIds(walletId)
    }

    /**
     * Get search suggestions based on recent transaction descriptions
     */
    override fun getSearchSuggestions(
        searchText: String,
        walletId: Int?,
        limit: Int
    ): Flow<List<String>> {
        return transactionDao.getSearchSuggestions(
            searchText = searchText,
            walletId = walletId,
            limit = limit
        )
    }

    // ==================== DEBT MANAGEMENT IMPLEMENTATIONS ====================

    /**
     * Get all debt-related transactions by category metadata
     */
    override fun getDebtTransactionsByCategories(
        walletId: Int?,
        categoryMetadata: List<String>
    ): Flow<List<Transaction>> {
        return transactionDao.getDebtTransactionsByCategories(
            walletId = walletId,
            categoryMetadata = categoryMetadata
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get transactions by debt reference
     */
    override fun getTransactionsByDebtReference(debtReference: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDebtReference(debtReference).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get transactions by parent debt ID
     */
    override fun getTransactionsByParentDebtId(parentDebtId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByParentDebtId(parentDebtId).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get debt transactions with person information
     */
    override fun getDebtTransactionsWithPerson(
        walletId: Int?,
        categoryMetadata: List<String>
    ): Flow<List<Transaction>> {
        return transactionDao.getDebtTransactionsWithPerson(
            walletId = walletId,
            categoryMetadata = categoryMetadata
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get transactions with debt references
     */
    override fun getTransactionsWithDebtReference(walletId: Int?): Flow<List<Transaction>> {
        return transactionDao.getTransactionsWithDebtReference(walletId).map { entities ->
            entities.map { it.toTransaction() }
        }
    }

    /**
     * Get distinct debt references
     */
    override fun getDistinctDebtReferences(walletId: Int?): Flow<List<String>> {
        return transactionDao.getDistinctDebtReferences(walletId)
    }

    /**
     * Count transactions by debt reference
     */
    override suspend fun countTransactionsByDebtReference(debtReference: String): Int {
        return transactionDao.countTransactionsByDebtReference(debtReference)
    }

    /**
     * Get the original debt transaction
     */
    override suspend fun getOriginalDebtTransaction(debtReference: String): Transaction? {
        return transactionDao.getOriginalDebtTransaction(debtReference)?.toTransaction()
    }

    /**
     * Get payment transactions for a debt reference
     */
    override fun getPaymentTransactionsByDebtReference(
        debtReference: String,
        originalTransactionId: Int
    ): Flow<List<Transaction>> {
        return transactionDao.getPaymentTransactionsByDebtReference(
            debtReference = debtReference,
            originalTransactionId = originalTransactionId
        ).map { entities ->
            entities.map { it.toTransaction() }
        }
    }
}