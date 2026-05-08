package com.uet.expensetracker.features.money.ui.budgets

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.usecases.GetDefaultWalletUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionsByCategoryAndWalletUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveTransactionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveWalletByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.SaveBudgetUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetBudgetsByWalletUseCase
import com.uet.expensetracker.utils.CurrencyConverter
import com.uet.expensetracker.utils.createTotalWallet
import com.uet.expensetracker.utils.formatAmountWithCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getDefaultWalletUseCase: GetDefaultWalletUseCase,
    private val observeWalletByIdUseCase: ObserveWalletByIdUseCase,
    private val getTransactionsByCategoryAndWalletUseCase: GetTransactionsByCategoryAndWalletUseCase,
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val getBudgetsByWalletUseCase: GetBudgetsByWalletUseCase,
    private val currencyConverter: CurrencyConverter
) : BaseViewModel<BudgetScreenState, BudgetScreenIntent, BudgetScreenEvent>() {

    override val _viewState = MutableStateFlow(BudgetScreenState())

    init {
        processIntent(BudgetScreenIntent.LoadBudgets)
    }

    private fun loadInitialData() {
        _viewState.value = _viewState.value.copy(isLoading = true)

        getWalletsUseCase(UseCase.None()) { result ->
            result.fold(
                { failure ->
                    _viewState.value = _viewState.value.copy(
                        isLoading = false,
                        error = failure.toString()
                    )
                    emitEvent(BudgetScreenEvent.ShowError(failure.toString()))
                },
                { walletsFlow ->
                    viewModelScope.launch {
                        try {
                            val wallets = walletsFlow.first()

                            if (wallets.isNotEmpty()) {

                                getDefaultWalletUseCase(UseCase.None()) { defaultResult ->
                                    defaultResult.fold(
                                        { failure ->
                                            _viewState.value = _viewState.value.copy(
                                                isLoading = false,
                                                error = failure.toString()
                                            )
                                            emitEvent(BudgetScreenEvent.ShowError(failure.toString()))
                                        },
                                        { defaultWalletFlow ->
                                            viewModelScope.launch {
                                                try {
                                                    val defaultWalletEntity =
                                                        defaultWalletFlow.first()
                                                    val defaultWallet = defaultWalletEntity

                                                    if (defaultWallet != null) {
                                                        _viewState.value = _viewState.value.copy(
                                                            wallets = wallets,
                                                            currentWallet = defaultWallet,
                                                            // isLoading will be set to false in loadTransactionsForWallet
                                                        )
                                                        Log.i(
                                                            "BudgetViewModel",
                                                            "loadInitialData: defaultWallet: $defaultWallet"
                                                        )
                                                        loadTransactionsForWallet(
                                                            defaultWallet.id,
                                                            false
                                                        )
                                                    } else {
                                                        // Handle case where default wallet is null (e.g., show error or guide user to create a wallet)
                                                        _viewState.value = _viewState.value.copy(
                                                            isLoading = false,
                                                            isBudgetEmpty = true, // Or a specific error state
                                                            error = "Default wallet not found."
                                                        )
                                                        emitEvent(BudgetScreenEvent.ShowError("Default wallet not found. Please set up a wallet."))
                                                    }
                                                } catch (e: Exception) {
                                                    handleError(e)
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                _viewState.value = _viewState.value.copy(
                                    isLoading = false,
                                    isBudgetEmpty = true
                                )
                            }
                        } catch (e: Exception) {
                            handleError(e)
                        }
                    }
                }
            )
        }
    }

    private fun loadTransactionsForWallet(walletId: Int, isTotalWallet: Boolean) {
        _viewState.value = _viewState.value.copy(isLoading = true, isTotalWallet = isTotalWallet)

        if (isTotalWallet) {
            loadAllTransactionsAndBudgets()
        } else {
            loadWalletTransactionsAndBudgets(walletId)
        }
    }

    private fun loadWalletTransactionsAndBudgets(walletId: Int) {
        observeWalletByIdUseCase(ObserveWalletByIdUseCase.Params.ByWalletId(walletId)) { walletResult ->
            walletResult.fold(
                { failure -> handleError(failure.toString()) },
                { walletFlow ->
                    // Live-combine transactions and budgets flows for continuous updates
                    getTransactionsByCategoryAndWalletUseCase(
                        GetTransactionsByCategoryAndWalletUseCase.Params(walletId = walletId)
                    ) { transactionsResult ->
                        transactionsResult.fold(
                            { failure -> handleError(failure.toString()) },
                            { transactionsFlow ->
                                getBudgetsByWalletUseCase(
                                    GetBudgetsByWalletUseCase.Params.ByWalletId(walletId)
                                ) { budgetsResult ->
                                    budgetsResult.fold(
                                        { failure -> handleError(failure.toString()) },
                                        { budgetsFlow ->
                                            viewModelScope.launch {
                                                // Get wallet object once
                                                val walletObj = walletFlow.first()
                                                // Combine flows and collect continuously
                                                combine(transactionsFlow, budgetsFlow) { txs, buds ->
                                                    txs to buds
                                                }.collect { (txs, buds) ->
                                                    updateStateWithBudgetsAndTransactions(
                                                        currentWallet = walletObj,
                                                        budgets = buds,
                                                        allTransactionsForWallet = txs
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
            )
        }
    }

    /**
     * Loads transactions and budgets for TotalWallet
     */
    private fun loadAllTransactionsAndBudgets() {
        getWalletsUseCase(UseCase.None()) { result ->
            result.fold(
                { failure -> handleError(failure.toString()) },
                { walletsFlow ->
                    viewModelScope.launch {
                        // Get snapshot of wallets
                        val wallets = walletsFlow.first()
                        if (wallets.isEmpty()) {
//                            _viewState.value = _viewState.value.copy(
//                                isLoading = false,
//                                isBudgetEmpty = true,
//                                error = "No wallets found."
//                            )
                            return@launch
                        }
                        val totalWallet = createTotalWallet(wallets, currencyConverter)
                        observeTransactionsUseCase(ObserveTransactionsUseCase.Params.AllWallets) { transactionsResult ->
                            transactionsResult.fold(
                                { failure -> handleError(failure.toString()) },
                                { dailyTransactionsFlow ->
                                    getBudgetsByWalletUseCase(GetBudgetsByWalletUseCase.Params.TotalWallets) { budgetsResult ->
                                        budgetsResult.fold(
                                            { failure -> handleError(failure.toString()) },
                                            { budgetsFlow ->
                                                viewModelScope.launch {
                                                    // Combine daily transactions and budgets for live updates
                                                    combine(dailyTransactionsFlow, budgetsFlow) { dailyList, buds ->
                                                        dailyList.flatMap { it.transactions } to buds
                                                    }.collect { (allTransactions, budgets) ->
                                                        updateStateWithBudgetsAndTransactions(
                                                            totalWallet,
                                                            budgets,
                                                            allTransactions
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
            )
        }
    }

    private suspend fun updateStateWithBudgetsAndTransactions(
        currentWallet: Wallet,
        budgets: List<Budget>,
        allTransactionsForWallet: List<Transaction>
    ) {
        // First, convert all transaction amounts into the current wallet's currency
        val unifiedTransactions = allTransactionsForWallet.map { tx ->
            val fromCode = tx.wallet.currency.currencyCode
            val toCode = currentWallet.currency.currencyCode
            val convertedAmount = if (fromCode == toCode) {
                tx.amount
            } else {
                currencyConverter.convert(tx.amount, fromCode, toCode) ?: tx.amount
            }
            tx.copy(amount = convertedAmount)
        }
        // Only include transactions whose category is in the current budgets
        val budgetCategoryIds = budgets.map { it.category.id }.toSet()
        val relevantTransactions = unifiedTransactions.filter { it.category.id in budgetCategoryIds }

        val totalSpentOverall = relevantTransactions
            .filter { it.transactionType == TransactionType.OUTFLOW }
            .sumOf { it.amount }

        val totalBudgetTargetAmount = budgets.sumOf { it.amount }

        val displayableBudgets = budgets.map { budget ->
            val spentForThisBudget = relevantTransactions
                .filter { it.category.id == budget.category.id && it.transactionType == TransactionType.OUTFLOW }
                .sumOf { it.amount }
            val leftAmount = budget.amount - spentForThisBudget
            val progress = if (budget.amount > 0) (spentForThisBudget / budget.amount).toFloat()
                .coerceIn(0f, 1f) else 0f

            // Determine alert type based on progress
            val alertType = when {
                progress >= 1.0f -> com.uet.expensetracker.features.money.ui.budgets.components.BudgetAlertType.EXCEEDED
                progress >= 0.80f -> com.uet.expensetracker.features.money.ui.budgets.components.BudgetAlertType.WARNING
                else -> null
            }

            DisplayableBudget(
                id = budget.budgetId,
                categoryName = budget.category.title,
                categoryIconResName = budget.category.icon,
                amountFormatted = formatAmountWithCurrency(
                    budget.amount,
                    currentWallet.currency.symbol
                ),
                leftAmountFormatted = formatAmountWithCurrency(
                    leftAmount,
                    currentWallet.currency.symbol
                ),
                progress = progress,
                currencySymbol = currentWallet.currency.symbol,
                alertType = alertType
            )
        }

        val amountYouCanSpend = totalBudgetTargetAmount - totalSpentOverall
        val overviewProgress =
            if (totalBudgetTargetAmount > 0) (totalSpentOverall / totalBudgetTargetAmount).toFloat()
                .coerceIn(0f, 1f) else 0f

        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - today

        _viewState.value = _viewState.value.copy(
            isLoading = false,
            currentWallet = currentWallet,
            transactions = relevantTransactions,
            displayableBudgets = displayableBudgets,
            totalBudgetAmount = totalBudgetTargetAmount,
            totalSpentAmount = totalSpentOverall,
            isBudgetEmpty = budgets.isEmpty(),
            error = null,

            overviewTotalAmount = formatAmountWithCurrency(
                amountYouCanSpend.coerceAtLeast(0.0),
                currentWallet.currency.symbol
            ),
            overviewSpentAmount = formatAmountWithCurrency(
                totalSpentOverall, currentWallet.currency.symbol
            ),
            overviewProgress = overviewProgress,
            overviewTotalBudgetsText = formatOverviewNumber(budgets.size.toDouble()),
            overviewTotalSpentText = formatOverviewNumber(totalSpentOverall),
            overviewDaysLeftText = "$daysLeft days"
        )
    }

    private fun formatOverviewNumber(number: Double): String {
        return when {
            number >= 1_000_000 -> String.format(Locale.US, "%.0f M", number / 1_000_000)
            number >= 1_000 -> String.format(Locale.US, "%.0f K", number / 1_000)
            else -> String.format(Locale.US, "%.0f", number)
        }
    }

    private fun handleError(e: Exception) {
        Log.e("BudgetViewModel", "Error: ", e)
        _viewState.value = _viewState.value.copy(
            isLoading = false,
            error = e.message ?: "An error occurred"
        )
        emitEvent(BudgetScreenEvent.ShowError(e.message ?: "An error occurred"))
    }

    private fun handleError(message: String) {
        Log.e("BudgetViewModel", "Error: $message")
        _viewState.value = _viewState.value.copy(
            isLoading = false,
            error = message
        )
        emitEvent(BudgetScreenEvent.ShowError(message))
    }

    override fun processIntent(intent: BudgetScreenIntent) {
        when (intent) {
            is BudgetScreenIntent.LoadBudgets -> loadInitialData()

            is BudgetScreenIntent.ChangeWallet -> {
                val newWalletId = intent.walletId
                val newIsTotalWallet = intent.isTotalWallet
                _viewState.value = _viewState.value.copy(
                    isLoading = true,
                    isTotalWallet = newIsTotalWallet
                )

                if (newIsTotalWallet) {
                    loadTransactionsForWallet(newWalletId, true)
                } else {
                    observeWalletByIdUseCase(ObserveWalletByIdUseCase.Params.ByWalletId(newWalletId)) { result ->
                        result.fold(
                            { failure -> handleError(failure.toString()) },
                            { walletFlow ->
                                viewModelScope.launch {
                                    try {
                                        val wallet = walletFlow.first()
                                        _viewState.value = _viewState.value.copy(
                                            currentWallet = wallet
                                        )
                                        loadTransactionsForWallet(wallet.id, false)
//                                        emitEvent(BudgetScreenEvent.WalletChanged(wallet, false))
                                    } catch (e: Exception) {
                                        handleError(e)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            is BudgetScreenIntent.CreateBudget -> {
                emitEvent(BudgetScreenEvent.NavigateToCreateBudget)
            }

            is BudgetScreenIntent.DeleteBudget -> {
                _viewState.value = _viewState.value.copy(isLoading = true)
                emitEvent(BudgetScreenEvent.BudgetDeleted)
                val currentWalletId = _viewState.value.currentWallet?.id ?: -1
                val isTotal = _viewState.value.isTotalWallet
                if (currentWalletId != -1 || isTotal) {
                    loadTransactionsForWallet(currentWalletId, isTotal)
                } else {
                    loadInitialData()
                }
            }

            is BudgetScreenIntent.RefreshBudgets -> {
                val currentWalletId = _viewState.value.currentWallet?.id ?: -1
                val isTotal = _viewState.value.isTotalWallet
                if (currentWalletId != -1 || isTotal) {
                    loadTransactionsForWallet(currentWalletId, isTotal)
                } else {
                    loadInitialData()
                }
            }
            is BudgetScreenIntent.SelectWalletClicked -> {
                emitEvent(BudgetScreenEvent.NavigateToChooseWallet)
            }
        }
    }
}