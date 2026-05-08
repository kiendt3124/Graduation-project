package com.uet.expensetracker.features.money.ui.category

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.usecases.GetCategoriesByTypeUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase
) : BaseViewModel<CategoryState, CategoryIntent, CategoryEvent>() {

    override val _viewState = MutableStateFlow(CategoryState())

    init {
        processIntent(CategoryIntent.LoadCategories)
    }

    override fun processIntent(intent: CategoryIntent) {
        when (intent) {
            CategoryIntent.LoadCategories -> loadCategories()
            is CategoryIntent.SelectTab -> selectTab(intent.type)
            is CategoryIntent.SelectCategory -> selectCategory(intent.categoryId)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true) }

            getCategoriesUseCase(UseCase.None()) { result ->
                result.fold(
                    { failure ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load categories"
                            )
                        }
                    },
                    { categoryGroups ->
                        _viewState.update { state ->
                            state.copy(
                                isLoading = false,
                                allCategoryGroups = categoryGroups,
                                categoryGroups = categoryGroups[state.selectedTab] ?: emptyList(),
                                error = null
                            )
                        }
                    }
                )
            }
        }
    }

    private fun selectTab(type: CategoryType) {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true) }

            getCategoriesByTypeUseCase(GetCategoriesByTypeUseCase.Params(type)) { result ->
                result.fold(
                    { failure ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load categories for type $type"
                            )
                        }
                    },
                    { categoryGroups ->
                        _viewState.update { state ->
                            state.copy(
                                isLoading = false,
                                selectedTab = type,
                                categoryGroups = categoryGroups,
                                error = null
                            )
                        }
                    }
                )
            }
        }
    }

    private fun selectCategory(categoryId: Int) {
        // Find the category by ID from the current state
        val category = _viewState.value.categoryGroups
            .flatMap { it.subCategories }
            .find { it.id == categoryId }

        // Only update if we found the category
        category?.let {
            _viewState.update { state -> state.copy(selectedCategory = category) }
            emitEvent(CategoryEvent.CategorySelected(category))
        }
    }
}