package com.uet.expensetracker.features.ai.data.remote.dto

data class ChatHistoryDto(
    val role: String,
    val content: String,
    val createdAt: String? = null,
)


