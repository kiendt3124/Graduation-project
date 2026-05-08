package com.uet.expensetracker.features.ai.data.remote.dto

data class ChatRequestDto(
    val message: String,
    val locale: String? = null,
    val context: String? = null,
)

data class ChatResponseDto(
    val reply: String,
    val suggestions: List<String>? = emptyList(),
    val data: Map<String, Any>? = null, // Structured data for charts, tables, etc.
)

