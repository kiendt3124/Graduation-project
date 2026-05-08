package com.uet.expensetracker.features.money.domain.usecases

import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import javax.inject.Inject

class CreateWalletUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) : UseCase<Unit, CreateWalletUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Unit> {
        return try {
            walletRepository.insertWallet(
                Wallet(
                    id = params.walletWithCurrency.wallet.id,
                    walletName = params.walletWithCurrency.wallet.walletName,
                    currentBalance = params.walletWithCurrency.wallet.currentBalance,
                    currency = params.walletWithCurrency.currency.let {
                        com.uet.expensetracker.features.money.domain.model.Currency(
                            id = it.id,
                            currencyName = it.currencyName,
                            currencyCode = it.currencyCode,
                            symbol = it.symbol,
                            displayType = it.displayType,
                            image = it.image
                        )
                    },
                    isMainWallet = params.walletWithCurrency.wallet.isMainWallet,
                )
            )
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(Failure.DatabaseError)
        }
    }

    data class Params(val walletWithCurrency: WalletWithCurrencyEntity)
}