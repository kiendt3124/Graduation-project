package com.uet.expensetracker.features.money.data.data_source.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import java.util.Date

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("walletId"), 
        Index("categoryId"),
        Index("parentDebtId"),
        Index("debtReference")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val walletId: Int,
    val amount: Double,
    val transactionType: TransactionType,
    val transactionDate: Date,
    val description: String?,
    val categoryId: Int,
    val withPerson: String? = null,
    val eventName: String? = null,
    val hasReminder: Boolean = false,
    val excludeFromReport: Boolean = false,
    val photoUri: String? = null,
    val parentDebtId: Int? = null,
    val debtReference: String? = null,
    val debtMetadata: String? = null
) {
    companion object {
        fun fromTransaction(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                walletId = transaction.wallet.id,
                amount = transaction.amount,
                transactionType = transaction.transactionType,
                transactionDate = transaction.transactionDate,
                description = transaction.description,
                categoryId = transaction.category.id,
                withPerson = transaction.withPerson,
                eventName = transaction.eventName,
                hasReminder = transaction.hasReminder,
                excludeFromReport = transaction.excludeFromReport,
                photoUri = transaction.photoUri,
                parentDebtId = transaction.parentDebtId,
                debtReference = transaction.debtReference,
                debtMetadata = transaction.debtMetadata
            )
        }
    }
}

data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "walletId",
        entityColumn = "id"
    )
    val wallet: WalletEntity,
    @Relation(
        parentColumn = "walletId",
        entityColumn = "cur_id",
        associateBy = androidx.room.Junction(
            value = WalletEntity::class,
            parentColumn = "id",
            entityColumn = "currencyId"
        )
    )
    val currency: CurrencyEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity
) {
    fun toTransaction(): Transaction {
        val categoryDomain = category.toCategory()

        val walletDomain = Wallet(
            id = wallet.id,
            walletName = wallet.walletName,
            currentBalance = wallet.currentBalance,
            currency = Currency(
                id = currency.id,
                currencyName = currency.currencyName,
                currencyCode = currency.currencyCode,
                symbol = currency.symbol,
                displayType = currency.displayType,
                image = currency.image
            )
        )

        return Transaction(
            id = transaction.id,
            wallet = walletDomain,
            amount = transaction.amount,
            transactionType = transaction.transactionType,
            transactionDate = transaction.transactionDate,
            description = transaction.description,
            category = categoryDomain,
            withPerson = transaction.withPerson,
            eventName = transaction.eventName,
            hasReminder = transaction.hasReminder,
            excludeFromReport = transaction.excludeFromReport,
            photoUri = transaction.photoUri,
            parentDebtId = transaction.parentDebtId,
            debtReference = transaction.debtReference,
            debtMetadata = transaction.debtMetadata
        )
    }
}