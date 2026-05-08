package com.uet.expensetracker.features.ai.data.remote

import com.uet.expensetracker.features.ai.data.remote.dto.ChatRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.ChatResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiAnswerRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiAnswerResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiPlanRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiPlanResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.ChatHistoryDto
import com.uet.expensetracker.features.ai.data.remote.dto.InsightsRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.InsightsResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.ParseTransactionRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.ParsedTransactionDto
import com.uet.expensetracker.features.ai.data.remote.dto.context.FinancialContextDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AiApiService {

    @POST("/api/ai/chat")
    suspend fun chat(@Body body: ChatRequestDto): ChatResponseDto

    @POST("/api/ai/parse-transaction")
    suspend fun parseTransaction(@Body body: ParseTransactionRequestDto): ParsedTransactionDto

    @POST("/api/ai/insights")
    suspend fun insights(@Body body: InsightsRequestDto): InsightsResponseDto

    @POST("/api/ai/plan")
    suspend fun plan(@Body body: AiPlanRequestDto): AiPlanResponseDto

    @POST("/api/ai/answer")
    suspend fun answer(@Body body: AiAnswerRequestDto): AiAnswerResponseDto

    @GET("/api/ai/history")
    suspend fun history(): List<ChatHistoryDto>

    @DELETE("/api/ai/history")
    suspend fun clearHistory()

    // Context sync
    @POST("/api/ai/sync-context")
    suspend fun syncContext(@Body body: FinancialContextDto)

    @GET("/api/ai/context/{userId}")
    suspend fun getContext(@Path("userId") userId: String): FinancialContextDto
}


