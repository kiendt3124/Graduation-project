package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.CurrencyRepository
import javax.inject.Inject

class CheckCurrenciesExistUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) : UseCase<Boolean, UseCase.None>() {

    override suspend fun run(params: None): Either<Failure, Boolean> {
        return try {
            val count = currencyRepository.getCurrenciesCount()
            Either.Right(count > 0)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }
}