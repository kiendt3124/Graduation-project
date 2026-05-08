package com.uet.expensetracker.features.ai.domain.usecase

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.functional.toLeft
import com.uet.expensetracker.core.functional.toRight
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.ai.data.AiRepository
import com.uet.expensetracker.features.ai.data.remote.dto.ChatHistoryDto
import com.uet.expensetracker.features.ai.domain.AiFailure
import javax.inject.Inject

class AiHistoryUseCase @Inject constructor(
    private val repository: AiRepository
) : UseCase<List<ChatHistoryDto>, UseCase.None>() {

    override suspend fun run(params: None): Either<Failure, List<ChatHistoryDto>> {
        return try {
            repository.history().toRight()
        } catch (e: Exception) {
            AiFailure.Unknown(e.message ?: "Load history failed").toLeft()
        }
    }
}


