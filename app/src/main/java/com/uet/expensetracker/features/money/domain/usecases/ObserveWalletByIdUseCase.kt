package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class ObserveWalletByIdUseCase(
    private val walletRepository: WalletRepository
) : UseCase<Flow<Wallet>, ObserveWalletByIdUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Flow<Wallet>> {
        return try {
            val walletId = when (params) {
                is Params.ByWalletId -> params.walletId
            }
            val walletFlow = walletRepository.getWalletById(walletId)
                .filterNotNull()
                .map { Wallet.fromEntity(it) }
            Either.Right(walletFlow)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }

    sealed class Params {
        data class ByWalletId(val walletId: Int) : Params()
    }
}