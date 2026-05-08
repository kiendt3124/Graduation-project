package com.uet.expensetracker.features.money.data.data_source.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index("walletId")
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val budgetId: Int = 0,
    val categoryId: Int,
    // nullable walletId: null indicates budget for all wallets
    val walletId: Int? = null,
    val amount: Double,
    val fromDate: Date,
    val endDate: Date,
    val isRepeating: Boolean = false
)

