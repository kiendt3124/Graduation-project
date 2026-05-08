package com.uet.expensetracker.features.ai.data.remote.dto

data class InsightsRequestDto(
    val totalIncome: Double,
    val totalExpense: Double,
    val totalDebt: Double,
    val recentSpendingPattern: String? = null,
    val timeRange: String? = null,
)

data class InsightsResponseDto(
    val alerts: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
)

