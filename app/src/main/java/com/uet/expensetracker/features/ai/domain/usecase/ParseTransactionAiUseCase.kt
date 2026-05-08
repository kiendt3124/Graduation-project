package com.uet.expensetracker.features.ai.domain.usecase

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.functional.toLeft
import com.uet.expensetracker.core.functional.toRight
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.data.remote.dto.ParseTransactionRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.ParsedTransactionDto
import com.uet.expensetracker.features.ai.domain.AiFailure
import javax.inject.Inject

class ParseTransactionAiUseCase @Inject constructor(
    private val repository: AiRepository
) : UseCase<ParsedTransactionDto, ParseTransactionAiUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, ParsedTransactionDto> {
        return try {
            repository.parseTransaction(
                ParseTransactionRequestDto(
                    text = params.text,
                    locale = params.locale
                )
            ).toRight()
        } catch (e: Exception) {
            AiFailure.Unknown(e.message ?: "AI parse error").toLeft()
        }
    }

    data class Params(
        val text: String,
        val locale: String? = null
    )
}


