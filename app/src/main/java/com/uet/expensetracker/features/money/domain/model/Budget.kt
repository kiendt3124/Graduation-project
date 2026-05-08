package com.uet.expensetracker.features.money.domain.model

import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetEntity
import java.util.Date

data class Budget(
    val budgetId: Int = 0,
    val category: Category,
    val wallet: Wallet,
    val amount: Double,
    val fromDate: Date,
    val endDate: Date,
    val isRepeating: Boolean
) {
    fun toBudgetEntity(): BudgetEntity {
        return BudgetEntity(
            budgetId = budgetId,
            categoryId = category.id,
            walletId = if (wallet.id == -1) null else wallet.id,
            amount = amount,
            fromDate = fromDate,
            endDate = endDate,
            isRepeating = isRepeating
        )
    }
    
    companion object {
        fun fromEntity(
            entity: BudgetEntity,
            category: Category,
            wallet: Wallet
        ): Budget {
            return Budget(
                budgetId = entity.budgetId,
                category = category,
                wallet = wallet,
                amount = entity.amount,
                fromDate = entity.fromDate,
                endDate = entity.endDate,
                isRepeating = entity.isRepeating
            )
        }
    }
}

