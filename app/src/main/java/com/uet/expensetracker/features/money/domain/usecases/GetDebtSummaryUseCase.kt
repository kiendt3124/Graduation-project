package com.uet.expensetracker.features.money.domain.usecases

import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.DebtCategoryMetadata
import com.uet.expensetracker.features.money.domain.model.DebtInfo
import com.uet.expensetracker.features.money.domain.model.DebtPerson
import com.uet.expensetracker.features.money.domain.model.DebtSummary
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetDebtSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : UseCase<DebtInfo, GetDebtSummaryUseCase.Params>() {

    companion object {
        private const val TAG = "GetDebtSummaryUseCase"
    }

    override suspend fun run(params: Params): Either<Failure, DebtInfo> {
        return try {
            // Get the selected wallet; khi walletId null sẽ lấy tất cả ví và trả về DebtInfo không gắn ví
            val walletWithCurrencyEntity = if (params.walletId != null) {
                walletRepository.getWalletById(params.walletId).first()
            } else null
            if (params.walletId != null && walletWithCurrencyEntity == null) {
                Log.w(TAG, "No wallet found for ID: ${params.walletId}")
                return Either.Left(Failure.DatabaseError)
            }

            // Get all debt-related transactions for the wallet
            val allTransactions = if (params.walletId != null) {
                transactionRepository.getTransactionsByWalletId(params.walletId).first()
            } else {
                transactionRepository.getAllTransactions().first()
            }

            // Filter debt-related transactions
            val debtTransactions = allTransactions.filter { transaction ->
                DebtCategoryMetadata.ALL_DEBT_CATEGORIES.contains(transaction.category.metaData)
            }

            Log.d(TAG, "Found ${debtTransactions.size} debt transactions")

            /**
             * Tính toán khoản nợ cho Tab "Phải trả" (Payable)
             * 
             * Logic:
             * - originalCategories: IS_DEBT (Tôi đi vay người khác → Tôi phải trả lại)
             * - repaymentCategories: IS_REPAYMENT (Tôi trả nợ lại cho người khác)
             * 
             * Ví dụ:
             * - Giao dịch gốc: IS_DEBT 1,000,000đ (Tôi vay) → INFLOW
             * - Giao dịch trả: IS_REPAYMENT 300,000đ (Tôi trả) → OUTFLOW
             * - Còn lại: 700,000đ (Tôi còn nợ)
             */
            val payableDebts = calculateDebtSummaries(
                transactions = debtTransactions,
                originalCategories = DebtCategoryMetadata.PAYABLE_ORIGINAL,  // IS_DEBT
                repaymentCategories = DebtCategoryMetadata.PAYABLE_REPAYMENT  // IS_REPAYMENT
            )

            /**
             * Tính toán khoản nợ cho Tab "Được nhận" (Receivable)
             * 
             * Logic:
             * - originalCategories: IS_LOAN (Tôi cho người khác vay → Người khác nợ tôi)
             * - repaymentCategories: IS_DEBT_COLLECTION (Tôi thu nợ từ người khác)
             * 
             * Ví dụ:
             * - Giao dịch gốc: IS_LOAN 2,000,000đ (Tôi cho vay) → OUTFLOW
             * - Giao dịch thu: IS_DEBT_COLLECTION 500,000đ (Tôi thu) → INFLOW
             * - Còn lại: 1,500,000đ (Người khác còn nợ tôi)
             */
            val receivableDebts = calculateDebtSummaries(
                transactions = debtTransactions,
                originalCategories = DebtCategoryMetadata.RECEIVABLE_ORIGINAL,  // IS_LOAN
                repaymentCategories = DebtCategoryMetadata.RECEIVABLE_REPAYMENT  // IS_DEBT_COLLECTION
            )

            val debtInfo = DebtInfo(
                wallet = walletWithCurrencyEntity?.let { Wallet.fromEntity(it) },
                payableDebts = payableDebts,
                receivableDebts = receivableDebts
            )

            Log.d(TAG, "Calculated debt info: ${payableDebts.size} payable, ${receivableDebts.size} receivable")

            Either.Right(debtInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating debt summary", e)
            Either.Left(Failure.ServerError)
        }
    }

    private suspend fun calculateDebtSummaries(
        transactions: List<Transaction>,
        originalCategories: List<String>,
        repaymentCategories: List<String>
    ): List<DebtSummary> {
        
        // Group transactions by debtReference first, then fallback to person-based grouping
        val groupedTransactions = groupTransactionsByDebt(transactions, originalCategories, repaymentCategories)
        
        return groupedTransactions.mapNotNull { (debtKey, debtTransactions) ->
            createDebtSummary(debtKey, debtTransactions, originalCategories, repaymentCategories)
        }
    }

    private fun groupTransactionsByDebt(
        transactions: List<Transaction>,
        originalCategories: List<String>,
        repaymentCategories: List<String>
    ): Map<String, List<Transaction>> {
        
        val relevantTransactions = transactions.filter { tx ->
            originalCategories.contains(tx.category.metaData) || 
            repaymentCategories.contains(tx.category.metaData)
        }
        
        // Tạo map để track transaction gốc theo ID
        // Điều này giúp match transaction thanh toán với transaction gốc qua parentDebtId
        val originalTransactionMap = relevantTransactions
            .filter { originalCategories.contains(it.category.metaData) }
            .associateBy { it.id }
        
        // Group transactions với logic cải thiện
        val grouped = mutableMapOf<String, MutableList<Transaction>>()
        
        relevantTransactions.forEach { tx ->
            val groupKey = when {
                // Ưu tiên 1: Nếu có debtReference, dùng debtReference
                tx.debtReference != null -> tx.debtReference!!
                
                // Ưu tiên 2: Nếu là transaction thanh toán và có parentDebtId, tìm transaction gốc
                tx.parentDebtId != null -> {
                    val original = originalTransactionMap[tx.parentDebtId]
                    if (original != null) {
                        // Nếu transaction gốc có debtReference, dùng nó
                        // Nếu không, tạo key từ person hoặc ID của transaction gốc
                        original.debtReference ?: run {
                            val persons = parseDebtPersonFromJson(original.withPerson)
                            persons?.firstOrNull()?.let { person ->
                                "${person.id}_${person.name}"
                            } ?: "legacy_${tx.parentDebtId}"
                        }
                    } else {
                        // Transaction gốc không tìm thấy, tạo key từ parentDebtId
                        "legacy_${tx.parentDebtId}"
                    }
                }
                
                // Ưu tiên 3: Nếu là transaction gốc, tạo key từ person hoặc ID
                originalCategories.contains(tx.category.metaData) -> {
                    val persons = parseDebtPersonFromJson(tx.withPerson)
                    persons?.firstOrNull()?.let { person ->
                        "${person.id}_${person.name}"
                    } ?: "legacy_${tx.id}"
                }
                
                // Fallback: Tạo key từ ID
                else -> "unknown_${tx.id}"
            }
            
            grouped.getOrPut(groupKey) { mutableListOf() }.add(tx)
        }
        
        return grouped
    }

    private fun createDebtSummary(
        debtKey: String,
        debtTransactions: List<Transaction>,
        originalCategories: List<String>,
        repaymentCategories: List<String>
    ): DebtSummary? {
        
        // Find the original debt transaction
        val originalTransaction = debtTransactions.find { tx ->
            originalCategories.contains(tx.category.metaData)
        } ?: return null
        
        // Find all repayment transactions
        val repaymentTransactions = debtTransactions.filter { tx ->
            repaymentCategories.contains(tx.category.metaData)
        }.sortedBy { it.transactionDate }
        
        // Parse person information; fallback để không loại bỏ khoản nợ thiếu contact
        val personInfo = parseDebtPersonFromJson(originalTransaction.withPerson)?.firstOrNull()
            ?: DebtPerson(
                id = "unknown_${originalTransaction.id}",
                name = originalTransaction.description ?: "Không rõ đối tác",
                initial = (originalTransaction.description?.firstOrNull() ?: '?').toString()
            )
        
        val totalAmount = originalTransaction.amount
        val paidAmount = repaymentTransactions.sumOf { it.amount }
        val currency = originalTransaction.wallet.currency.currencyCode
        
        return DebtSummary(
            debtId = originalTransaction.debtReference ?: "legacy_${originalTransaction.id}",
            personName = personInfo.name,
            personId = personInfo.id,
            originalTransaction = originalTransaction,
            repaymentTransactions = repaymentTransactions,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            currency = currency
        )
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
        val walletId: Int? = null  // null means all wallets
    )
} 