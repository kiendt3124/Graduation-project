package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.domain.repository.CurrencyRepository
import javax.inject.Inject

class GetCurrencyByCodeUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) : UseCase<CurrencyEntity, GetCurrencyByCodeUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, CurrencyEntity> {
        return try {
            val currency = currencyRepository.getCurrencyByCode(params.code)
            if (currency != null) {
                Either.Right(currency)
            } else {
                Either.Left(Failure.NotFound)
            }
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }

    data class Params(val code: String)
}