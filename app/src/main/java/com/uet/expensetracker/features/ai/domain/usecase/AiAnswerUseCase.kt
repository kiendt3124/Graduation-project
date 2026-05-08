package com.uet.expensetracker.features.ai.domain.usecase

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.functional.toLeft
import com.uet.expensetracker.core.functional.toRight
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.data.remote.dto.AiAnswerRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.AiAnswerResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.DataRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.DataResponseDto
import com.uet.expensetracker.features.ai.domain.AiFailure
import javax.inject.Inject

class AiAnswerUseCase @Inject constructor(
    private val repository: AiRepository
) : UseCase<AiAnswerResponseDto, AiAnswerUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, AiAnswerResponseDto> {
        return try {
            repository.answer(
                AiAnswerRequestDto(
                    prompt = params.prompt,
                    locale = params.locale,
                    timezone = params.timezone,
                    currencyCode = params.currencyCode,
                    dataRequest = params.dataRequest,
                    dataResponse = params.dataResponse
                )
            ).toRight()
        } catch (e: Exception) {
            AiFailure.Unknown(e.message ?: "AI answer error").toLeft()
        }
    }

    data class Params(
        val prompt: String,
        val dataRequest: DataRequestDto,
        val dataResponse: DataResponseDto,
        val locale: String? = "vi-VN",
        val timezone: String? = null,
        val currencyCode: String? = "VND",
    )
}



