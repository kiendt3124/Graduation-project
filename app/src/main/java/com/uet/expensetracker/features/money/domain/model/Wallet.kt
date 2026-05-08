package com.uet.expensetracker.features.money.domain.model

import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import java.io.Serializable

//data class Wallet(
//    val id: Int,
//    val walletName: String,
//    val currentBalance: Double,
//    val currency: Currency,
//    val icon: String = "img_wallet_default_widget",
//    val isMainWallet: Boolean = false
//) {
//    fun toWalletEntity(currencyId: Int): WalletEntity {
//        return WalletEntity(
//            id = id,
//            walletName = walletName,
//            currentBalance = currentBalance,
//            currencyId = currencyId,
//            icon = icon,
//            isMainWallet = isMainWallet
//        )
//    }
//}

data class Wallet(
    val id: Int,
    val walletName: String,
    val currentBalance: Double,
    val currency: Currency,
    val icon: String = "img_wallet_default_widget",
    val isMainWallet: Boolean = false
): Serializable {
    fun toWalletEntity(): WalletEntity {
        return WalletEntity(
            id = id,
            walletName = walletName,
            currentBalance = currentBalance,
            currencyId = currency.id,
            icon = icon,
            isMainWallet = isMainWallet
        )
    }

    companion object {
        fun fromEntity(entity: WalletWithCurrencyEntity): Wallet {
            return Wallet(
                id = entity.wallet.id,
                walletName = entity.wallet.walletName,
                currentBalance = entity.wallet.currentBalance,
                currency = Currency(
                    id = entity.currency.id,
                    currencyName = entity.currency.currencyName,
                    currencyCode = entity.currency.currencyCode,
                    symbol = entity.currency.symbol,
                    displayType = entity.currency.displayType,
                    image = entity.currency.image
                ),
                icon = entity.wallet.icon,
                isMainWallet = entity.wallet.isMainWallet
            )
        }
    }
}

data class Currency(
    val id: Int,
    val currencyName: String,
    val currencyCode: String,
    val symbol: String = "",
    val displayType: String = "",
    val image: ByteArray? = null
): Serializable {
    fun toCurrencyEntity(): CurrencyEntity {
        return CurrencyEntity(
            id = id,
            currencyName = currencyName,
            currencyCode = currencyCode,
            symbol = symbol,
            displayType = displayType,
            image = image
        )
    }
}