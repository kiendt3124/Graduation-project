package com.uet.expensetracker.features.money.ui.budgets.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.uet.expensetracker.features.money.domain.usecases.GetBudgetByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetBudgetTransactionsUseCase
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType

@HiltViewModel
class BudgetTransactionsViewModel @Inject constructor(
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<BudgetTransactionsState, BudgetTransactionsIntent, BudgetTransactionsEvent>() {

    private val budgetId: Int = savedStateHandle.get<Int>("budgetId")
        ?: throw IllegalArgumentException("budgetId is required")

    override val _viewState = MutableStateFlow(
        BudgetTransactionsState(
            budgetId = budgetId,
            category = Category(
                id = 0,
                metaData = "",
                title = "",
                icon = "",
                type = CategoryType.UNKNOWN
            ),
            groupedTransactions = emptyList(),
            isLoading = true,
            error = null
        )
    )

    init {
        processIntent(BudgetTransactionsIntent.LoadTransactions)
    }

    override fun processIntent(intent: BudgetTransactionsIntent) {
        when (intent) {
            BudgetTransactionsIntent.LoadTransactions,
            BudgetTransactionsIntent.RetryLoad -> loadTransactions()
            is BudgetTransactionsIntent.TransactionClicked -> emitEvent(
                BudgetTransactionsEvent.NavigateToTransactionDetail(intent.transactionId)
            )
            BudgetTransactionsIntent.BackClicked -> emitEvent(BudgetTransactionsEvent.NavigateBack)
        }
    }

    private fun loadTransactions() {
        _viewState.value = _viewState.value.copy(isLoading = true, error = null)

        // Fetch budget to get category
        getBudgetByIdUseCase(
            budgetId,
            viewModelScope
        ) { result ->
            result.fold(
                { failure ->
                    _viewState.value = _viewState.value.copy(isLoading = false, error = failure.toString())
                    emitEvent(BudgetTransactionsEvent.ShowError(failure.toString()))
                },
                { budget ->
                    // Update category in state
                    _viewState.value = _viewState.value.copy(category = budget.category)

                    // Fetch and group transactions
                    getBudgetTransactionsUseCase(
                        GetBudgetTransactionsUseCase.Params(budgetId),
                        viewModelScope
                    ) { ucResult ->
                        ucResult.fold(
                            { failure2 ->
                                _viewState.value = _viewState.value.copy(isLoading = false, error = failure2.toString())
                                emitEvent(BudgetTransactionsEvent.ShowError(failure2.toString()))
                            },
                            { flow ->
                                viewModelScope.launch {
                                    flow.collect { list ->
                                        _viewState.value = _viewState.value.copy(
                                            groupedTransactions = list,
                                            isLoading = false
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    }
} 