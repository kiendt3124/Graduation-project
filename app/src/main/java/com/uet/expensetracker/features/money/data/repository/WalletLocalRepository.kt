package com.uet.expensetracker.features.money.data.repository

import com.uet.expensetracker.features.money.data.data_source.local.dao.WalletDao
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WalletLocalRepository @Inject constructor(private val walletDao: WalletDao) : WalletRepository {
    override suspend fun getWallets(): Flow<List<WalletWithCurrencyEntity>> {
        return walletDao.getWallets()
    }

    override suspend fun insertWallet(wallet: Wallet) {
        val walletEntity = WalletEntity(
            id = wallet.id,
            walletName = wallet.walletName,
            currentBalance = wallet.currentBalance,
            currencyId = wallet.currency.id,
            icon = wallet.icon,
            isMainWallet = wallet.isMainWallet
        )
        walletDao.insertWallet(walletEntity)
    }

    override suspend fun getWalletById(walletId: Int): Flow<WalletWithCurrencyEntity?> {
        return walletDao.getWalletById(walletId)
    }
    
    override suspend fun updateWalletBalance(walletId: Int, newBalance: Double) {
        walletDao.updateWalletBalance(walletId, newBalance)
    }

    override suspend fun updateWallet(walletEntity: WalletEntity) {

        walletDao.updateWallet(walletEntity)
    }

    override suspend fun deleteWallet(walletId: Int) {
        walletDao.deleteWallet(walletId)
    }

    override suspend fun getDefaultWallet(): Flow<WalletWithCurrencyEntity?> {
        return walletDao.getDefaultWallet()
    }
}