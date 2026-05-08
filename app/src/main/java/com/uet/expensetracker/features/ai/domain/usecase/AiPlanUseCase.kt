package com.uet.expensetracker.features.ai.domain.usecase

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.functional.toLeft
import com.uet.expensetracker.core.functional.toRight
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.data.remote.dto.AiPlanRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiPlanResponseDto
import com.uet.expensetracker.features.ai.domain.AiFailure
import javax.inject.Inject

class AiPlanUseCase @Inject constructor(
    private val repository: AiRepository
) : UseCase<AiPlanResponseDto, AiPlanUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, AiPlanResponseDto> {
        return try {
            repository.plan(
                AiPlanRequestDto(
                    prompt = params.prompt,
                    locale = params.locale,
                    timezone = params.timezone,
                    currencyCode = params.currencyCode
                )
            ).toRight()
        } catch (e: Exception) {
            AiFailure.Unknown(e.message ?: "AI plan error").toLeft()
        }
    }

    data class Params(
        val prompt: String,
        val locale: String? = "vi-VN",
        val timezone: String? = null,
        val currencyCode: String? = "VND"
    )
}



