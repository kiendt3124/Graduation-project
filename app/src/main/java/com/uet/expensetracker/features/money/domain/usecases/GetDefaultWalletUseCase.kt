package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDefaultWalletUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) : UseCase<Flow<Wallet?>, UseCase.None>() {
    
    override suspend fun run(params: None): Either<Failure, Flow<Wallet?>> {
        return try {
            val defaultWalletFlow = walletRepository.getDefaultWallet().map { walletEntity ->
                walletEntity?.let {
                    Wallet.fromEntity(walletEntity)
                }
            }
            Either.Right(defaultWalletFlow)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }
    

} 