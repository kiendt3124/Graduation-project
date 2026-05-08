package com.uet.expensetracker.features.money.data.data_source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Transaction
    @Query("SELECT * FROM wallets")
    fun getWallets(): Flow<List<WalletWithCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)
    
    @Query("UPDATE wallets SET currentBalance = :newBalance WHERE id = :walletId")
    suspend fun updateWalletBalance(walletId: Int, newBalance: Double)

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getWalletById(id: Int): Flow<WalletWithCurrencyEntity?>

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteWallet(id: Int)

    //update wallet
    @Update
    suspend fun updateWallet(wallet: WalletEntity)
    
    @Transaction
    @Query("SELECT * FROM wallets WHERE isMainWallet = 1 LIMIT 1")
    fun getDefaultWallet(): Flow<WalletWithCurrencyEntity?>
}