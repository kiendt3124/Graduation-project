package com.uet.expensetracker.features.ai.data.remote.dto

data class AiPlanRequestDto(
    val prompt: String,
    val locale: String? = null,
    val timezone: String? = null,
    val currencyCode: String? = null,
)

data class AiPlanResponseDto(
    val dataRequest: DataRequestDto
)

enum class DatasetTypeDto {
    MONTHLY_TOTALS,
    TOP_CATEGORIES
}

data class DataRequestDto(
    val analysisType: String,
    val timeRange: String,
    val requiredDatasets: List<DatasetTypeDto>,
    val topK: Int? = 5,
)

data class DataResponseDto(
    val monthlyTotals: List<MonthlyTotalDto>? = null,
    val topCategories: List<CategoryTotalDto>? = null,
) {
    data class MonthlyTotalDto(
        val month: String,
        val income: Double,
        val expense: Double
    )

    data class CategoryTotalDto(
        val categoryMetadata: String,
        val amount: Double
    )
}

data class AiAnswerRequestDto(
    val prompt: String,
    val locale: String? = null,
    val timezone: String? = null,
    val currencyCode: String? = null,
    val dataRequest: DataRequestDto,
    val dataResponse: DataResponseDto,
)

data class AiAnswerResponseDto(
    val answerMarkdown: String,
    val actions: List<ActionDto>? = emptyList(),
) {
    data class ActionDto(
        val type: String,
        val label: String? = null,
        val payloadJson: String? = null,
    )
}



