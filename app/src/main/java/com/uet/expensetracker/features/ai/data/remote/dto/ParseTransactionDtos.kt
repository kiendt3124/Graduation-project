package com.uet.expensetracker.features.ai.data.remote.dto

data class ParseTransactionRequestDto(
    val text: String,
    val locale: String? = null,
)

data class ParsedTransactionDto(
    val amount: Double? = null,
    val currencyCode: String? = null,
    val categoryName: String? = null,
    val description: String? = null,
    val date: String? = null,
    val walletName: String? = null,
)

