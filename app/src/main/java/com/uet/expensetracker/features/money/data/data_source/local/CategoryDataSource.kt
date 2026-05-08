package com.uet.expensetracker.features.money.data.data_source.local

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.data.data_source.local.model.CategoryEntity
import org.json.JSONArray
import java.io.IOException
import java.nio.charset.Charset

class CategoryDataSource {

    companion object {
        private const val TAG = "CategoryDataSource"
    }

    fun loadCategories(context: Context): Map<CategoryType, List<CategoryGroup>> {
        val jsonString: String? = try {
            val inputStream = context.assets.open("moneylover_categories_v3.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ioException: IOException) {
            Log.e(TAG, "Error reading category JSON", ioException)
            null
        }

        val categoryGroups = mutableMapOf<CategoryType, MutableList<CategoryGroup>>()

        if (jsonString != null) {
            try {
                val rootArray = JSONArray(jsonString)
                for (i in 0 until rootArray.length()) {
                    val categoryJson = rootArray.getJSONObject(i)

                    val parentName = categoryJson.optString("name", "")
                    val parentTitleKey = categoryJson.optString("title", "")
                    val parentIconName = categoryJson.optString("icon", "")
                    val typeInt = categoryJson.optInt("type", -1)
                    val parentMetadata = categoryJson.optString(
                        "metadata",
                        parentName
                    )

                    val categoryType = mapIntToCategoryType(typeInt)
                    if (categoryType == CategoryType.UNKNOWN || parentName.isEmpty() || parentTitleKey.isEmpty() || parentIconName.isEmpty()) {
                        Log.w(TAG, "Skipping invalid parent category: $categoryJson")
                        continue
                    }

                    val subCategories = mutableListOf<Category>()
                    val subArray = categoryJson.optJSONArray("subcategories")

                    if (subArray == null) {
                        val parentCategory = Category(
                            id = 0,
                            metaData = parentMetadata,
                            title = parentTitleKey,
                            icon = parentIconName,
                            type = categoryType,
                            parentName = null
                        )
                        subCategories.add(parentCategory)
                    } else {
                        for (j in 0 until subArray.length()) {
                            val subCategoryJson = subArray.getJSONObject(j)
                            val subName = subCategoryJson.optString("name", "")
                            val subTitleKey = subCategoryJson.optString("title", "")
                            val subIconName = subCategoryJson.optString("icon", "")
                            val subTypeInt = subCategoryJson.optInt("type", typeInt)
                            val subMetadata = subCategoryJson.optString("metadata", subName)

                            val subCategoryType = mapIntToCategoryType(subTypeInt)
                            if (subCategoryType == CategoryType.UNKNOWN || subName.isEmpty() || subTitleKey.isEmpty() || subIconName.isEmpty()) {
                                Log.w(TAG, "Skipping invalid subcategory: $subCategoryJson")
                                continue
                            }

                            subCategories.add(
                                Category(
                                    id = 0,
                                    metaData = subMetadata,
                                    title = subTitleKey,
                                    icon = subIconName,
                                    type = subCategoryType,
                                    parentName = parentName
                                )
                            )
                        }
                    }

                    if (subCategories.isNotEmpty()) {
                        val group = CategoryGroup(
                            parentName = parentName,
                            parentTitleResName = parentTitleKey,
                            parentIconResName =parentIconName,
                            type = categoryType,
                            subCategories = subCategories
                        )
                        categoryGroups.getOrPut(categoryType) { mutableListOf() }.add(group)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing category JSON", e)
            }
        }

        return categoryGroups
    }

    // Creates CategoryEntity objects from JSON for database insertion
    fun loadCategoryEntities(context: Context): List<CategoryEntity> {
        val jsonString: String? = try {
            val inputStream = context.assets.open("moneylover_categories_v3.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ioException: IOException) {
            Log.e(TAG, "Error reading category JSON", ioException)
            null
        }

        val entities = mutableListOf<CategoryEntity>()

        if (jsonString != null) {
            try {
                val rootArray = JSONArray(jsonString)
                for (i in 0 until rootArray.length()) {
                    val categoryJson = rootArray.getJSONObject(i)

                    val parentName = categoryJson.optString("name", "")
                    val parentTitleKey = categoryJson.optString("title", "")
                    val parentIconName = categoryJson.optString("icon", "")
                    val typeInt = categoryJson.optInt("type", -1)
                    val parentMetadata = categoryJson.optString("metadata", parentName)

                    if (typeInt <= 0 || parentName.isEmpty() || parentTitleKey.isEmpty() || parentIconName.isEmpty()) {
                        Log.w(TAG, "Skipping invalid parent category: $categoryJson")
                        continue
                    }

                    val subArray = categoryJson.optJSONArray("subcategories")

                    if (subArray == null) {
                        // Add parent as a direct category
                        entities.add(
                            CategoryEntity(
                                id = 0, // Auto-generated
                                metaData = parentMetadata,
                                title = parentTitleKey,
                                icon = parentIconName,
                                type = typeInt,
                                parentName = null
                            )
                        )
                    } else {
                        // First, insert the parent category itself so we always have a proper group header.
                        // This ensures we have a Category with:
                        // - title = parentTitleKey (e.g. "cate_utilities")
                        // - icon  = parentIconName (e.g. "icon_135")
                        // - parentName = null (top-level)
                        entities.add(
                            CategoryEntity(
                                id = 0, // Auto-generated
                                metaData = parentMetadata,
                                title = parentTitleKey,
                                icon = parentIconName,
                                type = typeInt,
                                parentName = null
                            )
                        )

                        // Process subcategories and explicitly link them to the parent title key.
                        // This allows grouping logic to reliably find the correct header.
                        for (j in 0 until subArray.length()) {
                            val subCategoryJson = subArray.getJSONObject(j)
                            val subName = subCategoryJson.optString("name", "")
                            val subTitleKey = subCategoryJson.optString("title", "")
                            val subIconName = subCategoryJson.optString("icon", "")
                            val subTypeInt = subCategoryJson.optInt("type", typeInt)
                            val subMetadata = subCategoryJson.optString("metadata", subName)

                            if (subName.isEmpty() || subTitleKey.isEmpty() || subIconName.isEmpty()) {
                                Log.w(TAG, "Skipping invalid subcategory: $subCategoryJson")
                                continue
                            }

                            entities.add(
                                CategoryEntity(
                                    id = 0, // Auto-generated
                                    metaData = subMetadata,
                                    title = subTitleKey,
                                    icon = subIconName,
                                    type = subTypeInt,
                                    // IMPORTANT:
                                    // Use parentTitleKey (e.g. "cate_utilities") so that
                                    // grouping can match on the parent's title instead of raw JSON "name".
                                    parentName = parentTitleKey
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing category JSON for entities", e)
            }
        }

        return entities
    }

    private fun mapIntToCategoryType(typeInt: Int): CategoryType {
        return when (typeInt) {
            1 -> CategoryType.INCOME
            2 -> CategoryType.EXPENSE
            3 -> CategoryType.DEBT_LOAN // Assuming type 3 is Debt/Loan based on your request
            else -> CategoryType.UNKNOWN
        }
    }
}