package com.uet.expensetracker.features.ai.data

import com.uet.expensetracker.features.ai.data.remote.dto.ChatRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.ChatResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiAnswerRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiAnswerResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiPlanRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiPlanResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.InsightsRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.InsightsResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.ParseTransactionRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.ParsedTransactionDto
import com.uet.expensetracker.features.ai.data.remote.dto.ChatHistoryDto
import com.uet.expensetracker.features.ai.data.remote.dto.context.FinancialContextDto

interface AiRepository {
    suspend fun chat(request: ChatRequestDto): ChatResponseDto
    suspend fun parseTransaction(request: ParseTransactionRequestDto): ParsedTransactionDto
    suspend fun insights(request: InsightsRequestDto): InsightsResponseDto
    suspend fun plan(request: AiPlanRequestDto): AiPlanResponseDto
    suspend fun answer(request: AiAnswerRequestDto): AiAnswerResponseDto
    suspend fun history(): List<ChatHistoryDto>
    suspend fun clearHistory()
    suspend fun syncContext(body: FinancialContextDto)
    suspend fun getContext(userId: String): FinancialContextDto
}


