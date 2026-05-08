package com.uet.expensetracker.features.ai.domain.usecase

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.functional.toLeft
import com.uet.expensetracker.core.functional.toRight
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.data.remote.dto.InsightsRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.InsightsResponseDto
import com.uet.expensetracker.features.ai.domain.AiFailure
import javax.inject.Inject

class GetInsightsAiUseCase @Inject constructor(
    private val repository: AiRepository
) : UseCase<InsightsResponseDto, GetInsightsAiUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, InsightsResponseDto> {
        return try {
            repository.insights(
                InsightsRequestDto(
                    totalIncome = params.totalIncome,
                    totalExpense = params.totalExpense,
                    totalDebt = params.totalDebt,
                    recentSpendingPattern = params.recentSpendingPattern,
                    timeRange = params.timeRange
                )
            ).toRight()
        } catch (e: Exception) {
            AiFailure.Unknown(e.message ?: "AI insights error").toLeft()
        }
    }

    data class Params(
        val totalIncome: Double,
        val totalExpense: Double,
        val totalDebt: Double,
        val recentSpendingPattern: String? = null,
        val timeRange: String? = null
    )
}


