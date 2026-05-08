package com.uet.expensetracker.utils

import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet

val TOTAL_WALLET_ID = -1 // Special ID for total wallet
suspend fun createTotalWallet(wallets: List<Wallet>, currencyConverter: CurrencyConverter): Wallet {
    // Initialize currency converter
    currencyConverter.initialize()

    // Find the main wallet and its currency, or use VND as default
    val mainWallet = wallets.find { it.isMainWallet }
    val mainCurrency = mainWallet?.currency ?: Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫",
        displayType = "suffix"
    )

    // Calculate total balance by converting all amounts to main currency
    var totalBalance = 0.0
    wallets.forEach { wallet ->
        val convertedAmount = if (wallet.currency.currencyCode == mainCurrency.currencyCode) {
            wallet.currentBalance
        } else {
            currencyConverter.convert(
                amount = wallet.currentBalance,
                fromCurrency = wallet.currency.currencyCode,
                toCurrency = mainCurrency.currencyCode
            ) ?: 0.0
        }
        totalBalance += convertedAmount
    }

    return Wallet(
        id = TOTAL_WALLET_ID,
        walletName = "All Wallets",
        currentBalance = totalBalance,
        currency = mainCurrency,
        icon = "ic_category_all",
        isMainWallet = false
    )
}