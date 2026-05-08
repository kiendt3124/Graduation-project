package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) : UseCase<Map<CategoryType, List<CategoryGroup>>, UseCase.None>() {

    override suspend fun run(params: None): Either<Failure, Map<CategoryType, List<CategoryGroup>>> {
        return try {
            val categories = categoryRepository.getAllCategoryGroups().first()
            Either.Right(categories)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }
}