package com.uet.expensetracker.features.money.ui.transactions

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveTransactionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveWalletByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetTransactionsByMonthUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetFutureTransactionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveTransactionsByMonthUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveFutureTransactionsUseCase

import com.uet.expensetracker.utils.CurrencyConverter
import com.uet.expensetracker.utils.createTotalWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val observeWalletByIdUseCase: ObserveWalletByIdUseCase,
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val getFutureTransactionsUseCase: GetFutureTransactionsUseCase,
    private val observeTransactionsByMonthUseCase: ObserveTransactionsByMonthUseCase,
    private val observeFutureTransactionsUseCase: ObserveFutureTransactionsUseCase,
    private val currencyConverter: CurrencyConverter,
) : BaseViewModel<TransactionState, TransactionIntent, TransactionEvent>() {

    // Track if a data load is in progress to avoid multiple simultaneous loads
    private var isLoading = false
    private var currentWalletId = -1 // Default wallet ID
    private val TOTAL_WALLET_ID = -1 // Special ID for total wallet
    
    // Job to track current transaction observation - cancel when switching tabs
    private var currentTransactionObservationJob: Job? = null

    @SuppressLint("NewApi") 
    override val _viewState = MutableStateFlow(
        TransactionState(
            selectedTabIndex = MonthItem.findThisMonthIndex(MonthItem.buildMonthItems()), // Default to "This month" tab
            months = MonthItem.buildMonthItems(), // Initialize month tabs
            currentBalance = 0.0,
            currentWalletName = "",
            currentWallet = Wallet(
                id = currentWalletId,
                walletName = "Default Wallet",
                currentBalance = 0.0,
                currency = Currency(
                    id = 1,
                    currencyName = "Vietnamese Dong",
                    currencyCode = "VND",
                    symbol = "₫"
                )
            ),
            inflow = 0.0,
            outflow = 0.0,
            groupedTransactions = getPlaceholderTransactions(),
            isLoading = true,
            isTotalWallet = false // Default to not total wallet
        )
    )

    init {
        // Observe wallets to auto-select the main wallet initially or when the current wallet is deleted
        viewModelScope.launch {
            getWalletsUseCase(UseCase.None()) { result ->
                result.fold({}, { walletsFlow ->
                    viewModelScope.launch {
                        walletsFlow.collect { wallets ->
                            if (wallets.isEmpty()) return@collect
                            val currentWalletExists = wallets.any { it.id == currentWalletId }
                            if (!currentWalletExists && !_viewState.value.isTotalWallet) {
                                val mainWallet = wallets.find { it.isMainWallet } ?: wallets.first()
                                selectWallet(mainWallet.id, isTotalWallet = false)
                            }
                        }
                    }
                })
            }
        }
    }

    override fun processIntent(intent: TransactionIntent) {
        when (intent) {
            is TransactionIntent.SelectTab -> selectTab(intent.index)
            is TransactionIntent.SelectWallet -> selectWallet(intent.walletId, intent.isTotalWallet)
            is TransactionIntent.LoadTransactions -> loadTransactions()
            is TransactionIntent.NavigateToTransactionDetail -> navigateToTransactionDetail(intent.transactionId)
            is TransactionIntent.OpenChooseWallet -> navigateToChooseWallet()
        }
    }

    private fun selectTab(index: Int) {
        _viewState.value = _viewState.value.copy(selectedTabIndex = index)
        
        // Cancel previous observation before starting new one
        cancelCurrentTransactionObservation()
        observeTransactionsByTab(index)
    }

    /**
     * Observe transactions based on the selected tab (month or future) for real-time updates
     */
    private fun observeTransactionsByTab(tabIndex: Int) {
        val months = _viewState.value.months
        if (tabIndex < 0 || tabIndex >= months.size) {
            android.util.Log.e("TransactionViewModel", "Invalid tab index: $tabIndex")
            return
        }
        
        val selectedMonth = months[tabIndex]
        val walletId = if (_viewState.value.isTotalWallet) null else currentWalletId
        
        if (isLoading) {
            android.util.Log.d("TransactionViewModel", "Already observing transactions, skipping")
            return
        }
        
        isLoading = true
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        android.util.Log.d("TransactionViewModel", "Starting new transaction observation for tab $tabIndex")
        
        viewModelScope.launch {
            try {
                if (selectedMonth.isFuture) {
                    // Observe future transactions for real-time updates
                    observeFutureTransactionsUseCase(
                        params = ObserveFutureTransactionsUseCase.Params(walletId = walletId),
                    scope = viewModelScope,
                    onResult = { result ->
                            handleReactiveTransactionResult(result)
                        }
                    )
                } else {
                    // Observe transactions for specific month for real-time updates
                    observeTransactionsByMonthUseCase(
                        params = ObserveTransactionsByMonthUseCase.Params(
                            year = selectedMonth.year,
                            month = selectedMonth.month,
                            walletId = walletId
                        ),
                        scope = viewModelScope,
                        onResult = { result ->
                            handleReactiveTransactionResult(result)
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("TransactionViewModel", "Exception observing transactions by tab: ${e.message}", e)
                _viewState.value = _viewState.value.copy(
                    error = e,
                    isLoading = false
                )
                isLoading = false
            }
        }
    }
    
    /**
     * Handle the reactive result from either ObserveTransactionsByMonthUseCase or ObserveFutureTransactionsUseCase
     */
    private fun handleReactiveTransactionResult(result: com.uet.expensetracker.core.functional.Either<com.uet.expensetracker.core.failure.Failure, kotlinx.coroutines.flow.Flow<List<DailyTransactions>>>) {
                        result.fold(
                            { failure ->
                android.util.Log.e("TransactionViewModel", "Failed to observe transactions: $failure")
                                _viewState.value = _viewState.value.copy(
                                    isLoading = false,
                                    error = Exception(failure.toString())
                                )
                                isLoading = false
                            },
            { dailyTransactionsFlow ->
                android.util.Log.d("TransactionViewModel", "Successfully started observing transactions")
                
                // Collect the flow for real-time updates - store Job for cancellation
                currentTransactionObservationJob = viewModelScope.launch {
                    try {
                        dailyTransactionsFlow.collect { dailyTransactions ->
                            android.util.Log.d("TransactionViewModel", "Received real-time update: ${dailyTransactions.size} daily transaction groups")
                            
                            // Calculate inflow and outflow with currency conversion if needed
                            val inflow = calculateInflowFromDailyTransactions(dailyTransactions)
                            val outflow = calculateOutflowFromDailyTransactions(dailyTransactions)
                            
                            // Recalculate daily totals for Total Wallet if needed
                            val processedDailyTransactions = if (_viewState.value.isTotalWallet) {
                                recalculateDailyTotalsWithCurrencyConversion(dailyTransactions)
                            } else {
                                dailyTransactions
                            }

                            _viewState.value = _viewState.value.copy(
                                groupedTransactions = processedDailyTransactions,
                                inflow = inflow,
                                outflow = outflow,
                                isLoading = false
                            )
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionViewModel", "Error collecting transaction flow: ${e.message}", e)
                        _viewState.value = _viewState.value.copy(
                            isLoading = false,
                            error = e
                        )
                        isLoading = false
                    }
                }
            }
        )
    }

    /**
     * Cancel current transaction observation to prevent memory leaks when switching tabs
     */
    private fun cancelCurrentTransactionObservation() {
        currentTransactionObservationJob?.let { job ->
            if (job.isActive) {
                android.util.Log.d("TransactionViewModel", "Cancelling previous transaction observation")
                job.cancel()
            }
        }
        currentTransactionObservationJob = null
        isLoading = false
    }

    /**
     * Handle the result from either GetTransactionsByMonthUseCase or GetFutureTransactionsUseCase
     */
    private fun handleTransactionResult(result: com.uet.expensetracker.core.functional.Either<com.uet.expensetracker.core.failure.Failure, List<DailyTransactions>>) {
                                                result.fold(
                                                    { failure ->
                android.util.Log.e("TransactionViewModel", "Failed to load transactions: $failure")
                                                        _viewState.value = _viewState.value.copy(
                                                            isLoading = false,
                                                            error = Exception(failure.toString())
                                                        )
                                                        isLoading = false
                                                    },
            { dailyTransactions ->
                android.util.Log.d("TransactionViewModel", "Successfully loaded ${dailyTransactions.size} daily transaction groups")
                
                // Calculate inflow and outflow with currency conversion if needed
                                                        viewModelScope.launch {
                    try {
                        val inflow = calculateInflowFromDailyTransactions(dailyTransactions)
                        val outflow = calculateOutflowFromDailyTransactions(dailyTransactions)
                        
                        // Recalculate daily totals for Total Wallet if needed
                        val processedDailyTransactions = if (_viewState.value.isTotalWallet) {
                            recalculateDailyTotalsWithCurrencyConversion(dailyTransactions)
                        } else {
                            dailyTransactions
                        }

                                                                _viewState.value = _viewState.value.copy(
                            groupedTransactions = processedDailyTransactions,
                                                                    inflow = inflow,
                                                                    outflow = outflow,
                            isLoading = false
                        )
                        isLoading = false
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionViewModel", "Error calculating inflow/outflow: ${e.message}", e)
                        _viewState.value = _viewState.value.copy(
                                                                    isLoading = false,
                            error = e
                                                                )
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                                )
                                            }

    /**
     * Calculate total inflow from DailyTransactions list with currency conversion for Total Wallet
     */
    private suspend fun calculateInflowFromDailyTransactions(dailyTransactions: List<DailyTransactions>): Double {
        return if (_viewState.value.isTotalWallet) {
            // For Total Wallet, convert all transactions to the main currency
            val targetCurrency = _viewState.value.currentWallet.currency.currencyCode
            currencyConverter.initialize()
            
            dailyTransactions.sumOf { dailyData ->
                dailyData.transactions
                    .filter { it.transactionType == TransactionType.INFLOW }
                    .sumOf { transaction ->
                        if (transaction.wallet.currency.currencyCode == targetCurrency) {
                            transaction.amount
                        } else {
                            currencyConverter.convert(
                                amount = transaction.amount,
                                fromCurrency = transaction.wallet.currency.currencyCode,
                                toCurrency = targetCurrency
                            ) ?: transaction.amount
                        }
                    }
            }
        } else {
            // For individual wallet, no conversion needed
            dailyTransactions.sumOf { dailyData ->
                dailyData.transactions
                    .filter { it.transactionType == TransactionType.INFLOW }
                    .sumOf { it.amount }
            }
        }
    }

    /**
     * Calculate total outflow from DailyTransactions list with currency conversion for Total Wallet
     */
    private suspend fun calculateOutflowFromDailyTransactions(dailyTransactions: List<DailyTransactions>): Double {
        return if (_viewState.value.isTotalWallet) {
            // For Total Wallet, convert all transactions to the main currency
            val targetCurrency = _viewState.value.currentWallet.currency.currencyCode
            currencyConverter.initialize()
            
            dailyTransactions.sumOf { dailyData ->
                dailyData.transactions
                    .filter { it.transactionType == TransactionType.OUTFLOW }
                    .sumOf { transaction ->
                        if (transaction.wallet.currency.currencyCode == targetCurrency) {
                            transaction.amount
                        } else {
                            currencyConverter.convert(
                                amount = transaction.amount,
                                fromCurrency = transaction.wallet.currency.currencyCode,
                                toCurrency = targetCurrency
                            ) ?: transaction.amount
                        }
                    }
            }
        } else {
            // For individual wallet, no conversion needed
            dailyTransactions.sumOf { dailyData ->
                dailyData.transactions
                    .filter { it.transactionType == TransactionType.OUTFLOW }
                    .sumOf { it.amount }
            }
        }
    }

    /**
     * Recalculate daily totals with currency conversion for Total Wallet
     */
    private suspend fun recalculateDailyTotalsWithCurrencyConversion(dailyTransactions: List<DailyTransactions>): List<DailyTransactions> {
        val targetCurrency = _viewState.value.currentWallet.currency.currencyCode
        currencyConverter.initialize()
        
        return dailyTransactions.map { dailyData ->
            // Recalculate daily total with currency conversion
            val convertedDailyTotal = dailyData.transactions.sumOf { transaction ->
                val convertedAmount = if (transaction.wallet.currency.currencyCode == targetCurrency) {
                    transaction.amount
                } else {
                    currencyConverter.convert(
                        amount = transaction.amount,
                        fromCurrency = transaction.wallet.currency.currencyCode,
                        toCurrency = targetCurrency
                    ) ?: transaction.amount
                }
                
                when (transaction.transactionType) {
                    TransactionType.INFLOW -> convertedAmount
                    TransactionType.OUTFLOW -> -convertedAmount
                }
            }
            
            // Create new DailyTransactions with updated daily total
            DailyTransactions.create(
                date = dailyData.date,
                unsortedTransactions = dailyData.transactions,
                dailyTotal = convertedDailyTotal
            )
        }
    }
    
    private fun selectWallet(walletId: Int, isTotalWallet: Boolean) {
        android.util.Log.d("TransactionViewModel", "Selecting wallet with ID: $walletId, isTotalWallet: $isTotalWallet")
        
        if (currentWalletId == walletId && _viewState.value.isTotalWallet == isTotalWallet) {
            android.util.Log.d("TransactionViewModel", "Wallet already selected, skipping")
            return
        }
        
        // Cancel previous observation before switching wallets
        cancelCurrentTransactionObservation()
        
        currentWalletId = walletId
        _viewState.value = _viewState.value.copy(isTotalWallet = isTotalWallet)
        
        if (isTotalWallet) {
            // Create total wallet and observe transactions for current tab
            createAndSetTotalWallet()
            observeTransactionsByTab(_viewState.value.selectedTabIndex)
        } else {
            // For regular wallet, observe the wallet for updates and observe transactions
            observeCurrentWallet(walletId)
            observeTransactionsByTab(_viewState.value.selectedTabIndex)
        }
    }

    /**
     * Create total wallet by combining all wallets
     */
    private fun createAndSetTotalWallet() {
        viewModelScope.launch {
            getWalletsUseCase(
                params = UseCase.None(),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            android.util.Log.e("TransactionViewModel", "Failed to get wallets for total: $failure")
                        },
                        { walletsFlow ->
                            viewModelScope.launch {
                                walletsFlow.collect { wallets ->
                                    val totalWallet = createTotalWallet(wallets, currencyConverter)
                                    _viewState.value = _viewState.value.copy(
                                        currentWallet = totalWallet,
                                        currentWalletName = totalWallet.walletName,
                                        currentBalance = totalWallet.currentBalance
                                    )
                                }
                            }
                        }
                    )
                }
            )
        }
    }

    private fun loadTransactions() {
        // Cancel previous observation before loading new data
        cancelCurrentTransactionObservation()
        // Use the new tab-based observing instead of the old method
        observeTransactionsByTab(_viewState.value.selectedTabIndex)
    }

    private fun observeCurrentWallet(walletId: Int) {
        viewModelScope.launch {
            observeWalletByIdUseCase(
                params = ObserveWalletByIdUseCase.Params.ByWalletId(walletId),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            _viewState.value = _viewState.value.copy(
                                error = Exception(failure.toString())
                            )
                        },
                        { walletFlow ->
                            viewModelScope.launch {
                                walletFlow.collect { wallet ->
                                    _viewState.value = _viewState.value.copy(
                                        currentBalance = wallet.currentBalance,
                                        currentWalletName = wallet.walletName,
                                        currentWallet = wallet,
                                    )
                                }
                            }
                        }
                    )
                }
            )
        }
    }

    private fun navigateToTransactionDetail(transactionId: Int) {
        // Emit navigation event to be handled by the UI
        emitEvent(TransactionEvent.NavigateToTransactionDetail(transactionId))
    }

    private fun navigateToChooseWallet() {
        android.util.Log.d("TransactionViewModel", "Navigating to choose wallet screen with current wallet ID: $currentWalletId")
        emitEvent(TransactionEvent.NavigateToChooseWallet(currentWalletId))
    }

    private fun getPlaceholderTransactions(): List<DailyTransactions> {
        // Return empty list since we now load real data based on selected month tab
        return emptyList()
    }




    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing transaction observation to prevent memory leaks
        cancelCurrentTransactionObservation()
        android.util.Log.d("TransactionViewModel", "ViewModel cleared, all observations cancelled")
    }
}