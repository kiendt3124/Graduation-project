package com.uet.expensetracker.features.money.ui.category

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType

data class CategoryState(
    val isLoading: Boolean = false,
    val allCategoryGroups: Map<CategoryType, List<CategoryGroup>> = emptyMap(),
    val categoryGroups: List<CategoryGroup> = emptyList(),
    val selectedTab: CategoryType = CategoryType.EXPENSE,
    val selectedCategory: Category? = null,
    val error: String? = null
): MviStateBase

sealed interface CategoryIntent: MviIntentBase {
    object LoadCategories : CategoryIntent
    data class SelectTab(val type: CategoryType) : CategoryIntent
    data class SelectCategory(val categoryId: Int) : CategoryIntent
}

sealed interface CategoryEvent: MviEventBase {
    data class CategorySelected(val category: Category) : CategoryEvent
}