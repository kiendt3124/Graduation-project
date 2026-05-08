package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.repository.CategoryRepository
import javax.inject.Inject

class GetCategoryByIdUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) : UseCase<Category, GetCategoryByIdUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Category> {
        return try {
            val category = categoryRepository.getCategoryById(params.id)
                ?: return Either.Left(Failure.NotFound)
            Either.Right(category)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }

    data class Params(val id: String)
}