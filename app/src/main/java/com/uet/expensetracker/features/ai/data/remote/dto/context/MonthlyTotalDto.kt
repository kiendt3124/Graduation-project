package com.uet.expensetracker.features.ai.data.remote.dto.context

data class MonthlyTotalDto(
    val month: String?, // yyyy-MM
    val income: Double?,
    val expense: Double?
)

