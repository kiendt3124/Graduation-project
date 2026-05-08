package com.uet.expensetracker.features.money.ui.budgetdetails

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.usecases.GetBudgetByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionsForBudgetUseCase
import com.uet.expensetracker.features.money.domain.usecases.DeleteBudgetUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionsByCategoryAndWalletUseCase
import com.uet.expensetracker.utils.CurrencyConverter
import com.uet.expensetracker.utils.TOTAL_WALLET_ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.util.Date
import kotlin.math.log

@HiltViewModel
class BudgetDetailsViewModel @Inject constructor(
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val getTransactionsForBudgetUseCase: GetTransactionsForBudgetUseCase,
    private val getTransactionsByCategoryAndWalletUseCase: GetTransactionsByCategoryAndWalletUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val currencyConverter: CurrencyConverter,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<BudgetDetailsState, BudgetDetailsIntent, BudgetDetailsEvent>() {

    override val _viewState = MutableStateFlow(BudgetDetailsState())

    private val budgetId: Int = savedStateHandle.get<Int>("budgetId")
        ?: throw IllegalArgumentException("budgetId is required")

    init {
        processIntent(BudgetDetailsIntent.LoadBudgetDetails)
    }

    override fun processIntent(intent: BudgetDetailsIntent) {
        when (intent) {
            BudgetDetailsIntent.LoadBudgetDetails -> loadBudgetDetails()
            BudgetDetailsIntent.EditBudget -> emitEvent(BudgetDetailsEvent.NavigateToEditBudget)
            BudgetDetailsIntent.DeleteBudget -> emitEvent(
                BudgetDetailsEvent.ShowDeleteConfirmation(budgetId.toString())
            )

            BudgetDetailsIntent.ShowTransactions -> emitEvent(BudgetDetailsEvent.NavigateToTransactions)
            BudgetDetailsIntent.NavigateBack -> emitEvent(BudgetDetailsEvent.NavigateBack)
        }
    }

    private fun loadBudgetDetails() {
        _viewState.value = _viewState.value.copy(isLoading = true, error = null)
        getBudgetByIdUseCase(budgetId, viewModelScope) { budgetResult ->
            budgetResult.fold(
                { failure ->
                    _viewState.value =
                        _viewState.value.copy(isLoading = false, error = failure.toString())
                    emitEvent(BudgetDetailsEvent.ShowError(failure.toString()))
                },
                { budget ->
                    if (budget.wallet.id == TOTAL_WALLET_ID) {
                        val params = GetTransactionsByCategoryAndWalletUseCase.Params(
                            categoryId = budget.category.id,
                            walletId = null // null for all wallets
                        )
                        getTransactionsByCategoryAndWalletUseCase(
                            params,
                            viewModelScope
                        ) { txResult ->
                            txResult.fold(
                                { failure ->
                                    _viewState.value = _viewState.value.copy(
                                        isLoading = false,
                                        error = failure.toString()
                                    )
                                    emitEvent(BudgetDetailsEvent.ShowError(failure.toString()))
                                },
                                { listFlow ->
                                    viewModelScope.launch {
                                        currencyConverter.initialize()

                                        val daysTotal = ChronoUnit.DAYS.between(
                                            budget.fromDate.toInstant()
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate(),
                                            budget.endDate.toInstant()
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        ).coerceAtLeast(1)

                                        val transactions = listFlow.firstOrNull() ?: return@launch
                                        val spent = transactions.filter { it.transactionType == TransactionType.OUTFLOW }
                                                .sumOf {
                                                    if (budget.wallet.currency.id != it.wallet.currency.id) {
                                                        currencyConverter.convert(
                                                            it.amount,
                                                            it.wallet.currency.currencyCode,
                                                            budget.wallet.currency.currencyCode
                                                        )
                                                            ?: it.amount
                                                    } else {
                                                        it.amount
                                                    }
                                                }


                                        val daysPassed = ChronoUnit.DAYS.between(
                                            budget.fromDate.toInstant()
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate(),
                                            LocalDate.now()
                                        ).coerceAtLeast(1)

                                        val recommended = budget.amount / daysTotal
                                        val actualDaily = spent / daysPassed
                                        val projected = recommended * daysTotal

                                        val remainingBudget = (budget.amount - spent).coerceAtLeast(0.0)

                                        val graphPoints = transactions
                                            .groupBy { tx ->
                                                tx.transactionDate
                                                    .toInstant()
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDate()
                                            }
                                            .map { (date, list) ->
                                                GraphPoint(
                                                    date = Date.from(
                                                        date.atStartOfDay(ZoneId.systemDefault())
                                                            .toInstant()
                                                    ),
                                                    value = list.sumOf {
                                                        if (budget.wallet.currency.id != it.wallet.currency.id) {
                                                            currencyConverter.convert(
                                                                it.amount,
                                                                it.wallet.currency.currencyCode,
                                                                budget.wallet.currency.currencyCode
                                                            )
                                                                ?: it.amount
                                                        } else {
                                                            it.amount
                                                        }
                                                    }
                                                )
                                            }

                                        _viewState.value = _viewState.value.copy(
                                            budget = budget,
                                            transactions = transactions,
                                            graphData = GraphData(points = graphPoints),
                                            recommendedDailySpending = recommended,
                                            actualDailySpending = actualDaily,
                                            projectedSpending = projected,
                                            spentAmount = spent,
                                            remainingBudget = remainingBudget,
                                            isLoading = false,
                                            error = null
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        getTransactionsForBudgetUseCase(budgetId, viewModelScope) { txResult ->
                            txResult.fold(
                                { failure ->
                                    _viewState.value = _viewState.value.copy(
                                        isLoading = false,
                                        error = failure.toString()
                                    )
                                    emitEvent(BudgetDetailsEvent.ShowError(failure.toString()))
                                },
                                { transactions ->
                                    val daysTotal = ChronoUnit.DAYS.between(
                                        budget.fromDate.toInstant().atZone(ZoneId.systemDefault())
                                            .toLocalDate(),
                                        budget.endDate.toInstant().atZone(ZoneId.systemDefault())
                                            .toLocalDate()
                                    ).coerceAtLeast(1)

                                    val spent =
                                        transactions.filter { it.transactionType == TransactionType.OUTFLOW }
                                            .sumOf { it.amount }

                                    val daysPassed = ChronoUnit.DAYS.between(
                                        budget.fromDate.toInstant().atZone(ZoneId.systemDefault())
                                            .toLocalDate(),
                                        LocalDate.now()
                                    ).coerceAtLeast(1)

                                    val recommended = budget.amount / daysTotal
                                    val actualDaily = spent / daysPassed
                                    val projected = recommended * daysTotal
                                    val remainingBudget = (budget.amount - spent).coerceAtLeast(0.0)

                                    val graphPoints = transactions
                                        .groupBy { tx ->
                                            tx.transactionDate
                                                .toInstant()
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        }
                                        .map { (date, list) ->
                                            GraphPoint(
                                                date = Date.from(
                                                    date.atStartOfDay(ZoneId.systemDefault())
                                                        .toInstant()
                                                ),
                                                value = list.sumOf { it.amount }
                                            )
                                        }

                                    _viewState.value = _viewState.value.copy(
                                        budget = budget,
                                        transactions = transactions,
                                        graphData = GraphData(points = graphPoints),
                                        recommendedDailySpending = recommended,
                                        actualDailySpending = actualDaily,
                                        projectedSpending = projected,
                                        spentAmount = spent,
                                        remainingBudget = remainingBudget,
                                        isLoading = false,
                                        error = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    fun confirmDelete() {
        _viewState.value = _viewState.value.copy(isLoading = true, error = null)
        deleteBudgetUseCase(budgetId, viewModelScope) { result ->
            result.fold(
                { failure ->
                    _viewState.value =
                        _viewState.value.copy(isLoading = false, error = failure.toString())
                    emitEvent(BudgetDetailsEvent.ShowError(failure.toString()))
                },
                {
                    _viewState.value = _viewState.value.copy(isLoading = false, error = null)
                    emitEvent(BudgetDetailsEvent.NavigateBack)
                }
            )
        }
    }
} 