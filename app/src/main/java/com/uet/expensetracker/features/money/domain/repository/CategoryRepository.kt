package com.uet.expensetracker.features.money.domain.repository

import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun insertCategories(categories: List<Category>): Result<Unit>
    suspend fun insertCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
    suspend fun checkCategoriesExist(): Boolean
    suspend fun getCategoryByName(name: String): Category?
    fun getCategoryGroupsByType(type: CategoryType): Flow<List<CategoryGroup>>
    fun getAllCategoryGroups(): Flow<Map<CategoryType, List<CategoryGroup>>>
}