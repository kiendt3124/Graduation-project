package com.uet.expensetracker.features.money.data.data_source.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "wallets",
    foreignKeys = [
        ForeignKey (
            entity = CurrencyEntity::class,
            parentColumns = ["cur_id"],
            childColumns = ["currencyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("currencyId")]
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val walletName: String,
    val currentBalance: Double,
    val currencyId: Int,
    val icon: String = "img_wallet_default_widget",
    val isMainWallet: Boolean = false
)

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    @ColumnInfo(name = "cur_id")
    val id: Int,
    @ColumnInfo(name = "cur_name")
    val currencyName: String,
    @ColumnInfo(name = "cur_code")
    val currencyCode: String,
    @ColumnInfo(name = "cur_symbol")
    val symbol: String = "",
    @ColumnInfo(name = "cur_display_type")
    val displayType: String = "",
    val image: ByteArray? = null
)

data class WalletWithCurrencyEntity(
    @Embedded val wallet: WalletEntity,
    @Relation(
        parentColumn = "currencyId",
        entityColumn = "cur_id"
    )
    val currency: CurrencyEntity
)