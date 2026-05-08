package com.uet.expensetracker.features.money.data.mapper

import android.util.Log
import com.uet.expensetracker.features.money.domain.model.DebtCategoryMetadata
import com.uet.expensetracker.features.money.domain.model.DebtInfo
import com.uet.expensetracker.features.money.domain.model.DebtPerson
import com.uet.expensetracker.features.money.domain.model.DebtSummary
import com.uet.expensetracker.features.money.domain.model.DebtType
import com.uet.expensetracker.features.money.domain.model.PaymentRecord
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.ui.debtmanagement.components.DebtSortBy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Mapper class for debt-related data transformations
 * Handles JSON parsing, debt grouping, and other debt-specific operations
 */
object DebtMapper {
    
    private val gson = Gson()
    private const val TAG = "DebtMapper"

    /**
     * Parse withPerson JSON string to list of DebtPerson objects
     * 
     * @param withPersonJson JSON string from transaction
     * @return List of DebtPerson objects, empty list if parsing fails
     */
    fun parseWithPersonJson(withPersonJson: String?): List<DebtPerson> {
        if (withPersonJson.isNullOrBlank()) return emptyList()
        
        return try {
            val listType = object : TypeToken<List<DebtPerson>>() {}.type
            gson.fromJson(withPersonJson, listType) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse withPerson JSON: $withPersonJson", e)
            emptyList()
        }
    }

    /**
     * Convert list of DebtPerson objects to JSON string
     * 
     * @param debtPersons List of DebtPerson objects
     * @return JSON string representation
     */
    fun debtPersonListToJson(debtPersons: List<DebtPerson>): String {
        return try {
            gson.toJson(debtPersons)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert debt persons to JSON", e)
            "[]"
        }
    }

    /**
     * Parse debtMetadata JSON to DebtInfo object
     * 
     * @param debtMetadataJson JSON string from transaction
     * @return DebtInfo object or null if parsing fails
     */
    fun parseDebtMetadata(debtMetadataJson: String?): DebtInfo? {
        if (debtMetadataJson.isNullOrBlank()) return null
        
        return try {
            gson.fromJson(debtMetadataJson, DebtInfo::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse debt metadata JSON: $debtMetadataJson", e)
            null
        }
    }

    /**
     * Convert DebtInfo to JSON string
     * 
     * @param debtInfo DebtInfo object
     * @return JSON string representation
     */
    fun debtInfoToJson(debtInfo: DebtInfo): String {
        return try {
            gson.toJson(debtInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert debt info to JSON", e)
            "{}"
        }
    }

    /**
     * Group debt transactions by person and debt type
     * 
     * @param transactions List of debt transactions
     * @return Map grouped by person ID and debt type
     */
    fun groupDebtTransactionsByPersonAndType(
        transactions: List<Transaction>
    ): Map<Pair<String, DebtType>, List<Transaction>> {
        return transactions
            .flatMap { transaction ->
                val debtPersons = parseWithPersonJson(transaction.withPerson)
                val debtType = determineDebtType(transaction.category.metaData)
                
                debtPersons.map { person ->
                    Triple(person.id, debtType, transaction)
                }
            }
            .groupBy { (personId, debtType, _) -> 
                Pair(personId, debtType) 
            }
            .mapValues { (_, triples) -> 
                triples.map { it.third }
            }
    }

    /**
     * Determine debt type from category metadata
     * 
     * @param categoryMetadata Category metadata string
     * @return DebtType enum value
     */
    fun determineDebtType(categoryMetadata: String): DebtType {
        return when (categoryMetadata) {
            DebtCategoryMetadata.DEBT -> DebtType.RECEIVABLE
            DebtCategoryMetadata.DEBT_COLLECTION -> DebtType.RECEIVABLE
            DebtCategoryMetadata.COLLECT_INTEREST -> DebtType.RECEIVABLE
            DebtCategoryMetadata.LOAN -> DebtType.PAYABLE
            DebtCategoryMetadata.REPAYMENT -> DebtType.PAYABLE
            DebtCategoryMetadata.PAY_INTEREST -> DebtType.PAYABLE
            else -> DebtType.RECEIVABLE // Default fallback
        }
    }

    /**
     * Calculate total debt amount from transactions
     * 
     * @param transactions List of debt transactions
     * @return Total debt amount
     */
    fun calculateTotalDebtAmount(transactions: List<Transaction>): Double {
        return transactions.sumOf { transaction ->
            val categoryMetadata = transaction.category.metaData
            when (categoryMetadata) {
                DebtCategoryMetadata.DEBT -> transaction.amount // Money lent out
                DebtCategoryMetadata.LOAN -> transaction.amount // Money borrowed
                DebtCategoryMetadata.DEBT_COLLECTION -> -transaction.amount // Money received back
                DebtCategoryMetadata.REPAYMENT -> -transaction.amount // Money paid back
                DebtCategoryMetadata.COLLECT_INTEREST -> transaction.amount // Interest collected
                DebtCategoryMetadata.PAY_INTEREST -> -transaction.amount // Interest paid
                else -> 0.0
            }
        }
    }

    /**
     * Calculate remaining debt amount after payments
     * 
     * @param originalAmount Original debt amount
     * @param paymentTransactions List of payment transactions
     * @return Remaining debt amount
     */
    fun calculateRemainingDebtAmount(
        originalAmount: Double,
        paymentTransactions: List<Transaction>
    ): Double {
        val totalPayments = paymentTransactions.sumOf { it.amount }
        return originalAmount - totalPayments
    }

    /**
     * Create payment records from transactions
     * 
     * @param paymentTransactions List of payment transactions
     * @return List of PaymentRecord objects
     */
    fun createPaymentRecords(paymentTransactions: List<Transaction>): List<PaymentRecord> {
        return paymentTransactions.map { transaction ->
            PaymentRecord(
                date = transaction.transactionDate,
                amount = transaction.amount,
                description = transaction.description,
                transactionId = transaction.id
            )
        }
    }

    /**
     * Generate unique debt reference
     * 
     * @param personId Person ID involved in debt
     * @param timestamp Timestamp for uniqueness
     * @return Unique debt reference string
     */
    fun generateDebtReference(personId: String, timestamp: Long): String {
        return "DEBT_${personId}_${timestamp}_${UUID.randomUUID().toString().take(8)}"
    }

    /**
     * Create DebtSummary from grouped transactions
     * 
     * @param personId Person ID
     * @param personName Person name
     * @param transactions List of related transactions
     * @return DebtSummary object
     */
    fun createDebtSummary(
        personId: String,
        personName: String,
        transactions: List<Transaction>
    ): DebtSummary {
        val originalTransactions = transactions.filter { isOriginalDebtTransaction(it) }
        val paymentTransactions = transactions.filter { !isOriginalDebtTransaction(it) }
        
        val originalAmount = originalTransactions.sumOf { it.amount }
        val paidAmount = paymentTransactions.sumOf { it.amount }
        
        // Find the original transaction (first one chronologically)
        val originalTransaction = originalTransactions.minByOrNull { it.transactionDate }
            ?: transactions.first() // Fallback to first transaction if no original found
        
        // Get currency from the wallet
        val currency = originalTransaction.wallet.currency.currencyCode
        
        return DebtSummary(
            debtId = originalTransaction.debtReference ?: generateDebtReference(personId, originalTransaction.transactionDate.time),
            personName = personName,
            personId = personId,
            originalTransaction = originalTransaction,
            repaymentTransactions = paymentTransactions,
            totalAmount = originalAmount,
            paidAmount = paidAmount,
            currency = currency
        )
    }

    /**
     * Check if transaction is an original debt transaction
     * 
     * @param transaction Transaction to check
     * @return True if it's an original debt transaction
     */
    private fun isOriginalDebtTransaction(transaction: Transaction): Boolean {
        val categoryMetadata = transaction.category.metaData
        return categoryMetadata == DebtCategoryMetadata.DEBT ||
               categoryMetadata == DebtCategoryMetadata.LOAN
    }

    /**
     * Filter transactions by debt status (paid/unpaid)
     * 
     * @param debtSummaries List of debt summaries
     * @param isPaid Filter for paid/unpaid debts
     * @return Filtered list of debt summaries
     */
    fun filterDebtsByStatus(
        debtSummaries: List<DebtSummary>,
        isPaid: Boolean
    ): List<DebtSummary> {
        return debtSummaries.filter { it.isPaid == isPaid }
    }

    /**
     * Sort debt summaries by various criteria
     * 
     * @param debtSummaries List of debt summaries
     * @param sortBy Sort criteria
     * @return Sorted list of debt summaries
     */
    fun sortDebtSummaries(
        debtSummaries: List<DebtSummary>,
        sortBy: DebtSortBy = DebtSortBy.AMOUNT_DESC
    ): List<DebtSummary> {
        return when (sortBy) {
            DebtSortBy.AMOUNT_ASC -> debtSummaries.sortedBy { it.remainingAmount }
            DebtSortBy.AMOUNT_DESC -> debtSummaries.sortedByDescending { it.remainingAmount }
            DebtSortBy.NAME_ASC -> debtSummaries.sortedBy { it.personName }
            DebtSortBy.NAME_DESC -> debtSummaries.sortedByDescending { it.personName }
            DebtSortBy.DATE_ASC -> debtSummaries.sortedBy { 
                it.originalTransaction.transactionDate
            }
            DebtSortBy.DATE_DESC -> debtSummaries.sortedByDescending { 
                it.originalTransaction.transactionDate
            }
        }
    }
} 