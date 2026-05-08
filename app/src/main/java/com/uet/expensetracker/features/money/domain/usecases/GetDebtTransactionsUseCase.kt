package com.uet.expensetracker.features.money.domain.usecases

import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.DebtCategoryMetadata
import com.uet.expensetracker.features.money.domain.model.DebtPerson
import com.uet.expensetracker.features.money.domain.model.DebtType
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetDebtTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) : UseCase<List<Transaction>, GetDebtTransactionsUseCase.Params>() {

    companion object {
        private const val TAG = "GetDebtTransactionsUseCase"
    }

    override suspend fun run(params: Params): Either<Failure, List<Transaction>> {
        return try {
            // Get all transactions for the wallet
            val allTransactions = if (params.walletId != null) {
                transactionRepository.getTransactionsByWalletId(params.walletId).first()
            } else {
                transactionRepository.getAllTransactions().first()
            }

            // Filter debt-related transactions
            val debtTransactions = allTransactions.filter { transaction ->
                DebtCategoryMetadata.ALL_DEBT_CATEGORIES.contains(transaction.category.metaData)
            }

            // Filter by debt type (payable or receivable)
            // PAYABLE: IS_DEBT (Tôi vay) + IS_REPAYMENT (Tôi trả nợ) - Tab "Phải trả"
            // RECEIVABLE: IS_LOAN (Tôi cho vay) + IS_DEBT_COLLECTION (Tôi thu nợ) - Tab "Được nhận"
            val categoryFilter = when (params.debtType) {
                DebtType.PAYABLE -> DebtCategoryMetadata.PAYABLE_ORIGINAL + DebtCategoryMetadata.PAYABLE_REPAYMENT
                DebtType.RECEIVABLE -> DebtCategoryMetadata.RECEIVABLE_ORIGINAL + DebtCategoryMetadata.RECEIVABLE_REPAYMENT
            }

            val filteredTransactions = debtTransactions.filter { transaction ->
                categoryFilter.contains(transaction.category.metaData)
            }

            // Filter by person if specified
            val personFilteredTransactions = if (params.personName.isNotBlank()) {
                filteredTransactions.filter { transaction ->
                    val persons = parseDebtPersonFromJson(transaction.withPerson)
                    persons?.any { person -> 
                        person.name.equals(params.personName, ignoreCase = true) ||
                        person.id == params.personId
                    } == true
                }
            } else {
                filteredTransactions
            }

            // Sort by date (newest first)
            val sortedTransactions = personFilteredTransactions.sortedByDescending { it.transactionDate }

            Log.d(TAG, "Found ${sortedTransactions.size} debt transactions for person: ${params.personName}")

            Either.Right(sortedTransactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting debt transactions", e)
            Either.Left(Failure.ServerError)
        }
    }

    private fun parseDebtPersonFromJson(withPersonJson: String?): List<DebtPerson>? {
        return try {
            if (withPersonJson.isNullOrEmpty()) return null
            
            val gson = Gson()
            val listType = object : TypeToken<List<DebtPerson>>() {}.type
            gson.fromJson(withPersonJson, listType)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse debt person JSON, trying fallback", e)
            // Fallback: try to parse as simple contact format
            try {
                val gson = Gson()
                val contactListType = object : TypeToken<List<Map<String, String>>>() {}.type
                val contacts: List<Map<String, String>> = gson.fromJson(withPersonJson, contactListType)
                
                contacts?.map { contact ->
                    DebtPerson(
                        id = contact["id"] ?: "unknown",
                        name = contact["name"] ?: "Unknown",
                        initial = contact["initial"] ?: contact["name"]?.firstOrNull()?.toString() ?: "?"
                    )
                }
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Failed to parse person JSON with fallback", fallbackException)
                null
            }
        }
    }

    data class Params(
        val walletId: Int? = null,
        val personName: String = "",
        val personId: String = "",
        val debtType: DebtType
    )
} 