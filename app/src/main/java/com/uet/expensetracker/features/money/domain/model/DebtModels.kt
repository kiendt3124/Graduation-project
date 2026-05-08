package com.uet.expensetracker.features.money.domain.model

import java.util.Date

/**
 * Represents a person involved in debt transactions
 * Used for parsing and storing person information in withPerson JSON field
 */
data class DebtPerson(
    val id: String,
    val name: String,
    val initial: String,
    val debtId: String? = null,
    val originalAmount: Double? = null,
    val notes: String? = null
)

/**
 * Represents a record of debt payment
 */
data class PaymentRecord(
    val date: Date,
    val amount: Double,
    val description: String?,
    val transactionId: Int
)

/**
 * Comprehensive debt summary for a specific person and debt relationship
 */
data class DebtSummary(
    val debtId: String,
    val personName: String,
    val personId: String,
    val originalTransaction: Transaction,
    val repaymentTransactions: List<Transaction>,
    val totalAmount: Double,
    val paidAmount: Double,
    val currency: String
) {
    val remainingAmount: Double get() = totalAmount - paidAmount
    val isPaid: Boolean get() = remainingAmount <= 0.0
    val progressPercentage: Float get() = if (totalAmount > 0) (paidAmount / totalAmount).toFloat() else 0f
    
    val paymentHistory: List<PaymentRecord> get() = repaymentTransactions.map { transaction ->
        PaymentRecord(
            date = transaction.transactionDate,
            amount = transaction.amount,
            description = transaction.description,
            transactionId = transaction.id
        )
    }.sortedByDescending { it.date }
    
    /**
     * Check if this debt has recent activity (within last 30 days)
     */
    val hasRecentActivity: Boolean get() {
        val thirtyDaysAgo = Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        return repaymentTransactions.any { it.transactionDate.after(thirtyDaysAgo) }
    }
    
    /**
     * Get the last payment date, null if no payments made
     */
    val lastPaymentDate: Date? get() = repaymentTransactions
        .maxByOrNull { it.transactionDate }?.transactionDate
}

/**
 * Complete debt information for a specific wallet or all wallets
 */
data class DebtInfo(
    val wallet: Wallet?,
    val payableDebts: List<DebtSummary>,
    val receivableDebts: List<DebtSummary>
) {
    val totalPayableAmount: Double get() = payableDebts.sumOf { it.remainingAmount }
    val totalReceivableAmount: Double get() = receivableDebts.sumOf { it.remainingAmount }
    
    val totalPayableCount: Int get() = payableDebts.count { !it.isPaid }
    val totalReceivableCount: Int get() = receivableDebts.count { !it.isPaid }
    
    val hasAnyDebts: Boolean get() = payableDebts.isNotEmpty() || receivableDebts.isNotEmpty()
}

/**
 * Type of debt relationship
 */
enum class DebtType {
    /**
     * Tab "Phải trả": Khoản nợ tôi phải trả lại cho người khác
     * - IS_DEBT: Tôi đi vay người khác → Tôi phải trả lại
     * - IS_REPAYMENT: Tôi trả nợ lại cho người khác
     */
    PAYABLE,
    
    /**
     * Tab "Được nhận": Khoản nợ người khác phải trả lại cho tôi
     * - IS_LOAN: Tôi cho người khác vay → Người khác nợ tôi
     * - IS_DEBT_COLLECTION: Tôi thu nợ từ người khác
     */
    RECEIVABLE
}

/**
 * UI tab types for debt management screen
 */
enum class DebtTab {
    PAYABLE,
    RECEIVABLE
}

/**
 * Status of debt payment
 */
enum class DebtStatus {
    UNPAID,     // No payments made yet
    PARTIAL,    // Some payments made but not fully paid
    PAID        // Fully paid
}

/**
 * Debt category metadata constants for easy reference
 * 
 * ========================================
 * LOGIC PHÂN LOẠI NỢ THEO QUAN ĐIỂM:
 * ========================================
 * 
 * Tab "Phải trả" (Payable): Khoản nợ tôi phải trả lại cho người khác
 * ┌─────────────────────────────────────────────────────────────┐
 * │ IS_DEBT: Tôi đi vay người khác                              │
 * │   → Khi vay: INFLOW (tiền vào, màu xanh)                    │
 * │   → Tôi phải trả lại → Hiển thị ở Tab "Phải trả"           │
 * │                                                              │
 * │ IS_REPAYMENT: Tôi trả nợ lại cho người khác                 │
 * │   → OUTFLOW (tiền ra, màu đỏ)                               │
 * │   → Giao dịch thanh toán cho khoản nợ IS_DEBT              │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Tab "Được nhận" (Receivable): Khoản nợ người khác phải trả lại cho tôi
 * ┌─────────────────────────────────────────────────────────────┐
 * │ IS_LOAN: Tôi cho người khác vay                             │
 * │   → Khi cho vay: OUTFLOW (tiền ra, màu đỏ)                 │
 * │   → Người khác nợ tôi → Hiển thị ở Tab "Được nhận"        │
 * │                                                              │
 * │ IS_DEBT_COLLECTION: Tôi thu nợ từ người khác                │
 * │   → INFLOW (tiền vào, màu xanh)                             │
 * │   → Giao dịch thu nợ cho khoản cho vay IS_LOAN             │
 * └─────────────────────────────────────────────────────────────┘
 */
object DebtCategoryMetadata {
    // ==================== TAB "PHẢI TRẢ" (PAYABLE) ====================
    // Khoản nợ tôi phải trả lại cho người khác
    
    /**
     * IS_DEBT: Tôi đi vay người khác
     * - Transaction Type: INFLOW (tiền vào khi vay)
     * - Tab: "Phải trả" (Payable)
     * - Ý nghĩa: Tôi vay tiền → Tôi phải trả lại
     */
    const val DEBT = "IS_DEBT"
    
    /**
     * IS_REPAYMENT: Tôi trả nợ lại cho người khác
     * - Transaction Type: OUTFLOW (tiền ra khi trả)
     * - Tab: "Phải trả" (Payable)
     * - Ý nghĩa: Giao dịch thanh toán cho khoản nợ IS_DEBT
     */
    const val REPAYMENT = "IS_REPAYMENT"
    
    /**
     * IS_PAY_INTEREST: Tôi trả lãi
     * - Transaction Type: OUTFLOW (tiền ra)
     * - Tab: "Phải trả" (Payable)
     */
    const val PAY_INTEREST = "IS_PAY_INTEREST"
    
    // ==================== TAB "ĐƯỢC NHẬN" (RECEIVABLE) ====================
    // Khoản nợ người khác phải trả lại cho tôi
    
    /**
     * IS_LOAN: Tôi cho người khác vay
     * - Transaction Type: OUTFLOW (tiền ra khi cho vay)
     * - Tab: "Được nhận" (Receivable)
     * - Ý nghĩa: Tôi cho vay → Người khác nợ tôi → Tôi sẽ thu lại
     */
    const val LOAN = "IS_LOAN"
    
    /**
     * IS_DEBT_COLLECTION: Tôi thu nợ từ người khác
     * - Transaction Type: INFLOW (tiền vào khi thu)
     * - Tab: "Được nhận" (Receivable)
     * - Ý nghĩa: Giao dịch thu nợ cho khoản cho vay IS_LOAN
     */
    const val DEBT_COLLECTION = "IS_DEBT_COLLECTION"
    
    /**
     * IS_COLLECT_INTEREST: Tôi thu lãi
     * - Transaction Type: INFLOW (tiền vào)
     * - Tab: "Được nhận" (Receivable)
     */
    const val COLLECT_INTEREST = "IS_COLLECT_INTEREST"
    
    // ==================== MAPPING CHO TAB "PHẢI TRẢ" ====================
    
    /**
     * Danh sách category gốc cho Tab "Phải trả"
     * IS_DEBT: Tôi đi vay người khác → Tôi phải trả lại
     */
    val PAYABLE_ORIGINAL = listOf(DEBT)
    
    /**
     * Danh sách category thanh toán cho Tab "Phải trả"
     * IS_REPAYMENT: Tôi trả nợ lại cho người khác
     */
    val PAYABLE_REPAYMENT = listOf(REPAYMENT)
    
    // ==================== MAPPING CHO TAB "ĐƯỢC NHẬN" ====================
    
    /**
     * Danh sách category gốc cho Tab "Được nhận"
     * IS_LOAN: Tôi cho người khác vay → Người khác nợ tôi
     */
    val RECEIVABLE_ORIGINAL = listOf(LOAN)
    
    /**
     * Danh sách category thu nợ cho Tab "Được nhận"
     * IS_DEBT_COLLECTION: Tôi thu nợ từ người khác
     */
    val RECEIVABLE_REPAYMENT = listOf(DEBT_COLLECTION)
    
    // ==================== DANH SÁCH TỔNG HỢP ====================
    
    /**
     * Tất cả các category liên quan đến nợ
     */
    val ALL_DEBT_CATEGORIES = listOf(LOAN, REPAYMENT, PAY_INTEREST, DEBT, DEBT_COLLECTION, COLLECT_INTEREST)

    /**
     * Fallback an toàn cho danh mục thu nợ nếu metadata chính không có
     * @return IS_DEBT_COLLECTION
     */
    fun DEPT_COLLECTION_SAFE_FALLBACK(): String = DEBT_COLLECTION
} 