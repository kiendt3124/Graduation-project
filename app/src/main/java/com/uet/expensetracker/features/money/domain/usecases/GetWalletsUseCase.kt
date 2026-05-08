package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.uet.expensetracker.features.money.domain.model.Wallet

class GetWalletsUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) : UseCase<Flow<List<Wallet>>, UseCase.None>() {

    override suspend fun run(params: None): Either<Failure, Flow<List<Wallet>>> {
        return try {
            val wallets = walletRepository.getWallets()
                .map { walletEntities ->
                    walletEntities.map { Wallet.fromEntity(it) }
                }
            Either.Right(wallets)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }
}