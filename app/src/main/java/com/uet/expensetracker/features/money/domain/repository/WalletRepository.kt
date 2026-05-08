package com.uet.expensetracker.features.money.domain.repository

import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    suspend fun getWallets(): Flow<List<WalletWithCurrencyEntity>>
    suspend fun insertWallet(wallet: Wallet)
    suspend fun getWalletById(walletId: Int): Flow<WalletWithCurrencyEntity?>
    suspend fun updateWalletBalance(walletId: Int, newBalance: Double)
    suspend fun updateWallet(wallet: WalletEntity)
    suspend fun deleteWallet(walletId: Int)
    suspend fun getDefaultWallet(): Flow<WalletWithCurrencyEntity?>
}