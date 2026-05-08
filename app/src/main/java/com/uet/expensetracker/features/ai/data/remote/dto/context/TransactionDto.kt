package com.uet.expensetracker.features.ai.data.remote.dto.context

data class TransactionDto(
    val id: Int?,
    val amount: Double?,
    val type: String?, // INFLOW / OUTFLOW
    val date: String?, // ISO-8601
    val description: String?,
    val categoryMetadata: String?,
    val walletId: Int?
)

