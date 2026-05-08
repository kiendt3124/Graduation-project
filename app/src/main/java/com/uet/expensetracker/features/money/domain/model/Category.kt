package com.uet.expensetracker.features.money.domain.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.uet.expensetracker.features.money.data.data_source.local.model.CategoryEntity
import java.io.Serializable

// Represents a category group and its subcategories for UI display
data class CategoryGroup(
    val parentName: String, // Unique identifier for the parent group (from json "name")
    val parentTitleResName: String, // Resolved string resource ID for parent title
    val parentIconResName: String, // Resolved drawable resource ID for parent icon
    val type: CategoryType,
    val subCategories: List<Category>
) : Serializable

// Represents an individual selectable category (mostly subcategories from json)
data class Category(
    val id: Int = 0,
    val metaData: String,
    val title: String,
    val icon: String,
    val type: CategoryType,
    val parentName: String? = null
) : Serializable {
    // Convert from domain model to entity
    fun toCategoryEntity(): CategoryEntity {
        val typeValue = when (type) {
            CategoryType.EXPENSE -> 2
            CategoryType.INCOME -> 1
            CategoryType.DEBT_LOAN -> 3
            CategoryType.UNKNOWN -> 0
        }

        return CategoryEntity(
            id = id,
            metaData = metaData,
            title = title,
            icon = icon,
            type = typeValue,
            parentName = parentName
        )
    }
}

enum class CategoryType {
    EXPENSE, INCOME, DEBT_LOAN, UNKNOWN
}