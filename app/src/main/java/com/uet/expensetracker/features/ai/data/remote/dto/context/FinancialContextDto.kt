package com.uet.expensetracker.features.ai.data.remote.dto.context

data class FinancialContextDto(
    val wallets: List<WalletDto> = emptyList(),
    val recentTransactions: List<TransactionDto> = emptyList(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val monthlyTotals: List<MonthlyTotalDto> = emptyList(),
    val analyticsCache: Map<String, Any?> = emptyMap(),
    val totalBalance: Double? = null,
    val monthlyAvgIncome: Double? = null,
    val monthlyAvgExpense: Double? = null,
    val savingsRate: Double? = null
)

