package com.uet.expensetracker.features.money.data.data_source.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val metaData: String,
    val title: String,
    val icon: String,
    val type: Int, // 1=INCOME, 2=EXPENSE, 3=DEBT_LOAN
    val parentName: String? // null for top-level categories
) {
    fun toCategory(): Category {
        val categoryType = when (type) {
            1 -> CategoryType.INCOME
            2 -> CategoryType.EXPENSE
            3 -> CategoryType.DEBT_LOAN
            else -> CategoryType.UNKNOWN
        }

        return Category(
            id = id,
            metaData = metaData,
            title = title,
            icon = icon,
            type = categoryType,
            parentName = parentName
        )
    }
}