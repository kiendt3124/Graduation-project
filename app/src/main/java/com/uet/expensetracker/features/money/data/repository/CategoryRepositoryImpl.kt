package com.uet.expensetracker.features.money.data.repository

import com.uet.expensetracker.features.money.data.data_source.local.dao.CategoryDao
import com.uet.expensetracker.features.money.data.data_source.local.model.CategoryEntity
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        val typeInt = when (type) {
            CategoryType.INCOME -> 1
            CategoryType.EXPENSE -> 2
            CategoryType.DEBT_LOAN -> 3
            CategoryType.UNKNOWN -> -1
        }
        return categoryDao.getCategoriesByType(typeInt).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(id)?.toCategory()
    }

    override suspend fun insertCategories(categories: List<Category>): Result<Unit> {
        return try {
            categoryDao.insertCategories(categories.map { toCategoryEntity(it) })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertCategory(category: Category): Result<Unit> {
        return try {
            categoryDao.insertCategory(toCategoryEntity(category))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            categoryDao.deleteCategory(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkCategoriesExist(): Boolean {
        // Check if any categories exist in the database
        return categoryDao.getAllCategories().map { it.isNotEmpty() }.first()
    }

    override suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.findCategoryByName(name)?.toCategory()
    }

    override fun getCategoryGroupsByType(type: CategoryType): Flow<List<CategoryGroup>> {
        return getCategoriesByType(type).map { categories ->
            groupCategoriesToCategoryGroups(categories)
        }
    }

    override fun getAllCategoryGroups(): Flow<Map<CategoryType, List<CategoryGroup>>> {
        return getAllCategories().map { allCategories ->
            val groupedByType = allCategories.groupBy { it.type }
            groupedByType.mapValues { (_, categories) ->
                groupCategoriesToCategoryGroups(categories)
            }
        }
    }

    // Helper function to convert domain model to entity
    private fun toCategoryEntity(category: Category): CategoryEntity {
        val typeInt = when (category.type) {
            CategoryType.INCOME -> 1
            CategoryType.EXPENSE -> 2
            CategoryType.DEBT_LOAN -> 3
            CategoryType.UNKNOWN -> -1
        }

        return CategoryEntity(
            id = category.id,
            title = category.title,
            icon = category.icon,
            type = typeInt,
            parentName = category.parentName,
            metaData = category.metaData
        )
    }

    // Helper function to group categories into CategoryGroup for UI.
    //
    // Expected invariants after seeding from CategoryDataSource:
    // - Parent categories (group headers) have parentName == null and title == JSON "title" (e.g. "cate_utilities").
    // - Subcategories have parentName == parent title key (e.g. "cate_utilities").
    //
    // This function:
    // 1. Treats each parent (parentName == null) as a group header.
    // 2. Attaches all children whose parentName == parent.title as subCategories.
    // 3. If a category has no children and is not referenced as a parent, it becomes a single-item group.
    private fun groupCategoriesToCategoryGroups(categories: List<Category>): List<CategoryGroup> {
        if (categories.isEmpty()) return emptyList()

        val result = mutableListOf<CategoryGroup>()

        // Split into parents (potential headers) and children.
        val parents = categories.filter { it.parentName == null }
        val children = categories.filter { it.parentName != null }

        // Index children by their parentName (which should be the parent's title key).
        val childrenByParentTitle: Map<String, List<Category>> = children
            .filter { !it.parentName.isNullOrEmpty() }
            .groupBy { it.parentName!! }

        // 1. Handle explicit parents: build groups with header + children.
        val processedTitles = mutableSetOf<String>()
        parents.forEach { parent ->
            val titleKey = parent.title
            if (processedTitles.add(titleKey)) {
                val subcategories = childrenByParentTitle[titleKey].orEmpty()

                val group = CategoryGroup(
                    parentName = titleKey,                // use title key as stable identifier
                    parentTitleResName = parent.title,    // e.g. "cate_utilities"
                    parentIconResName = parent.icon,      // e.g. "icon_135"
                    type = parent.type,
                    // If there are no children, treat the parent itself as a single subcategory.
                    subCategories = if (subcategories.isEmpty()) {
                        listOf(parent)
                    } else {
                        subcategories
                    }
                )
                result.add(group)
            }
        }

        // 2. Fallback: handle any "orphan" children (with parentName but no parent entry).
        // This covers legacy data where parent wasn't seeded; we group them by parentName
        // and use the first child as the header.
        childrenByParentTitle.forEach { (parentTitleKey, subcategories) ->
            if (processedTitles.contains(parentTitleKey)) return@forEach

            val first = subcategories.firstOrNull() ?: return@forEach
            val group = CategoryGroup(
                parentName = parentTitleKey,
                parentTitleResName = first.parentName ?: first.title,
                parentIconResName = first.icon,
                type = first.type,
                subCategories = subcategories
            )
            result.add(group)
        }

        // 3. Fallback: any parentless categories that haven't been grouped yet.
        // This ensures standalone categories (without children and not referenced as parentName)
        // still appear in the UI.
        parents.forEach { parent ->
            val titleKey = parent.title
            if (!processedTitles.contains(titleKey) && !childrenByParentTitle.containsKey(titleKey)) {
                val group = CategoryGroup(
                    parentName = titleKey,
                    parentTitleResName = parent.title,
                    parentIconResName = parent.icon,
                    type = parent.type,
                    subCategories = listOf(parent)
                )
                result.add(group)
                processedTitles.add(titleKey)
            }
        }

        return result
    }
}