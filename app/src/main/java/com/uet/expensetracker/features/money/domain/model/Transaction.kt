package com.uet.expensetracker.features.money.domain.model

import java.util.Date

// Represents a single financial transaction
data class Transaction(
    val id: Int,
    val wallet: Wallet,
    val transactionType: TransactionType,
    val amount: Double,
    val transactionDate: Date,
    val description: String?,
    val category: Category,
    val withPerson: String? = null,
    val eventName: String? = null,
    val hasReminder: Boolean = false,
    val excludeFromReport: Boolean = false,
    val photoUri: String? = null,
    val parentDebtId: Int? = null,  // Link to original debt transaction
    val debtReference: String? = null, // Unique identifier for debt relationship
    val debtMetadata: String? = null // Additional debt-related metadata as JSON
) {
    // For compatibility with repositories and other code still using walletId
    val walletId: Int
        get() = wallet.id
        
    /**
     * Check if this transaction is debt-related
     */
    val isDebtRelated: Boolean get() = DebtCategoryMetadata.ALL_DEBT_CATEGORIES.contains(category.metaData)
    
    /**
     * Xác định loại nợ nếu đây là giao dịch nợ
     * 
     * Logic phân loại:
     * - PAYABLE (Tab "Phải trả"): 
     *   + IS_DEBT (Tôi đi vay người khác → Tôi phải trả lại)
     *   + IS_REPAYMENT (Tôi trả nợ lại cho người khác)
     * 
     * - RECEIVABLE (Tab "Được nhận"): 
     *   + IS_LOAN (Tôi cho người khác vay → Người khác nợ tôi)
     *   + IS_DEBT_COLLECTION (Tôi thu nợ từ người khác)
     */
    val debtType: DebtType? get() = when (category.metaData) {
        in DebtCategoryMetadata.PAYABLE_ORIGINAL,
        in DebtCategoryMetadata.PAYABLE_REPAYMENT -> DebtType.PAYABLE
        in DebtCategoryMetadata.RECEIVABLE_ORIGINAL,
        in DebtCategoryMetadata.RECEIVABLE_REPAYMENT -> DebtType.RECEIVABLE
        else -> null
    }
    
    /**
     * Kiểm tra xem đây có phải là giao dịch nợ gốc (không phải thanh toán)
     * 
     * Giao dịch nợ gốc:
     * - IS_DEBT: Tôi đi vay người khác (Tab "Phải trả")
     * - IS_LOAN: Tôi cho người khác vay (Tab "Được nhận")
     * 
     * Giao dịch thanh toán:
     * - IS_REPAYMENT: Tôi trả nợ (Tab "Phải trả")
     * - IS_DEBT_COLLECTION: Tôi thu nợ (Tab "Được nhận")
     */
    val isOriginalDebt: Boolean get() = category.metaData in 
        (DebtCategoryMetadata.PAYABLE_ORIGINAL + DebtCategoryMetadata.RECEIVABLE_ORIGINAL)
    
    /**
     * Kiểm tra xem đây có phải là giao dịch thanh toán/thu nợ
     * 
     * Giao dịch thanh toán:
     * - IS_REPAYMENT: Tôi trả nợ lại cho người khác (Tab "Phải trả")
     * - IS_DEBT_COLLECTION: Tôi thu nợ từ người khác (Tab "Được nhận")
     */
    val isRepayment: Boolean get() = category.metaData in 
        (DebtCategoryMetadata.PAYABLE_REPAYMENT + DebtCategoryMetadata.RECEIVABLE_REPAYMENT)
}

// Enum to define the type of transaction more clearly
enum class TransactionType {
    INFLOW,
    OUTFLOW
}
