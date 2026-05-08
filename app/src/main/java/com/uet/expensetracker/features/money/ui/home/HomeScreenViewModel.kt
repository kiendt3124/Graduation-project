package com.uet.expensetracker.features.money.ui.home

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.uet.expensetracker.features.money.data.data_source.local.CurrencyDataSource
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.usecases.CheckCurrenciesExistUseCase
import com.uet.expensetracker.features.money.domain.usecases.CreateWalletUseCase
import com.uet.expensetracker.features.money.domain.usecases.SaveCurrenciesUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetCurrencyByCodeUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.CheckCategoriesExistUseCase
import com.uet.expensetracker.features.money.domain.usecases.InsertCategoriesUseCase
import com.uet.expensetracker.features.money.data.data_source.local.CategoryDataSource
import android.content.Context
import android.util.Log
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.interactor.UseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import com.uet.expensetracker.R
import com.uet.expensetracker.utils.CurrencyConverter
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.usecases.ObserveTransactionsUseCase
import java.util.*
import com.uet.expensetracker.features.money.domain.model.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.Job

/**
 * ViewModel for the HomeScreen.
 * Handles business logic and manages the UI state for HomeScreen.
 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val checkCurrenciesExistUseCase: CheckCurrenciesExistUseCase,
    private val saveCurrenciesUseCase: SaveCurrenciesUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val currencyDataSource: CurrencyDataSource,
    private val getCurrencyByCodeUseCase: GetCurrencyByCodeUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val checkCategoriesExistUseCase: CheckCategoriesExistUseCase,
    private val insertCategoriesUseCase: InsertCategoriesUseCase,
    private val categoryDataSource: CategoryDataSource,
    private val currencyConverter: CurrencyConverter,
    @ApplicationContext private val context: Context,
    private val observeTransactionsUseCase: ObserveTransactionsUseCase
) : BaseViewModel<HomeScreenState, HomeScreenIntent, HomeScreenEvent>() {

    // Override the viewState from BaseViewModel
    override val _viewState = MutableStateFlow(HomeScreenState())

    init {
        // Initial data loading when the ViewModel is created
        processIntent(HomeScreenIntent.LoadInitialData)
        // Combine wallets and transactions streams
        observerableTransactionReport()
    }

    private fun observerableTransactionReport() {
        combine(
            walletsStream().filter { it.isNotEmpty() },
            transactionsStream()
        ) { wallets, allTx -> wallets to allTx }
            .onEach { (wallets, allTx) ->
                // Determine main currency code
                val mainCurrencyCode =
                    wallets.find { it.isMainWallet }?.currency?.currencyCode ?: "VND"
                // Initialize converter
                currencyConverter.initialize()
                // Convert transactions to main currency
                val converted = allTx.map { tx ->
                    val amt = if (tx.wallet.currency.currencyCode == mainCurrencyCode) {
                        tx.amount
                    } else {
                        currencyConverter.convert(
                            tx.amount,
                            tx.wallet.currency.currencyCode,
                            mainCurrencyCode
                        ) ?: 0.0
                    }
                    tx to amt
                }
                // Calculate totals
                val totalSpent =
                    converted.filter { it.first.transactionType == TransactionType.OUTFLOW }
                        .sumOf { it.second }
                val totalIncome =
                    converted.filter { it.first.transactionType == TransactionType.INFLOW }
                        .sumOf { it.second }
                val now = System.currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L
                val weekStart = now - 6 * oneDay
                val lastWeekStart = now - 13 * oneDay
                val currentSpent =
                    converted.filter { (tx, _) -> tx.transactionType == TransactionType.OUTFLOW && tx.transactionDate.time >= weekStart }
                        .sumOf { it.second }
                val previousSpent =
                    converted.filter { (tx, _) -> tx.transactionType == TransactionType.OUTFLOW && tx.transactionDate.time in lastWeekStart until weekStart }
                        .sumOf { it.second }
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1); set(
                    Calendar.HOUR_OF_DAY,
                    0
                ); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val currentMonthStart = cal.timeInMillis
                val prevMonthCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1); set(
                    Calendar.DAY_OF_MONTH,
                    1
                ); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
                    Calendar.SECOND,
                    0
                ); set(Calendar.MILLISECOND, 0)
                }
                val prevMonthStart = prevMonthCal.timeInMillis
                val currentMonthSpent =
                    converted.filter { (tx, _) -> tx.transactionType == TransactionType.OUTFLOW && tx.transactionDate.time >= currentMonthStart }
                        .sumOf { it.second }
                val previousMonthSpent =
                    converted.filter { (tx, _) -> tx.transactionType == TransactionType.OUTFLOW && tx.transactionDate.time in prevMonthStart until currentMonthStart }
                        .sumOf { it.second }
                _viewState.update { state ->
                    state.copy(
                        transactions = allTx,
                        totalSpent = totalSpent,
                        totalIncome = totalIncome,
                        currentSpent = currentSpent,
                        previousSpent = previousSpent,
                        currentMonthSpent = currentMonthSpent,
                        previousMonthSpent = previousMonthSpent,
                        currencySymbol = wallets.find { it.isMainWallet }?.currency?.symbol ?: "đ",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handles intents triggered from the UI.
     */
    override fun processIntent(intent: HomeScreenIntent) {
        when (intent) {
            HomeScreenIntent.LoadInitialData -> loadData()
            HomeScreenIntent.ToggleBalanceVisibility -> toggleBalanceVisibility()
            is HomeScreenIntent.SelectMainTab -> _viewState.update { it.copy(selectedMainTab = intent.tab) }
            is HomeScreenIntent.SelectTrendingTab -> _viewState.update { it.copy(selectedTrendingTab = intent.tab) }
            is HomeScreenIntent.SelectSpendingTab -> _viewState.update { it.copy(selectedSpendingTab = intent.tab) }
            // Handle other intents as they are added
            HomeScreenIntent.LoadWeeklyExpense -> TODO()
            else -> {}
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Set loading state
                _viewState.update { it.copy(isLoading = true) }

                // First check if currencies exist in the database
                checkCurrenciesExistUseCase(UseCase.None()) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(isLoading = false, error = context.getString(R.string.home_error_check_currencies))
                            }
                        },
                        { currenciesExist ->
                            Log.i(HomeScreenViewModel::class.java.simpleName, "loadData: currenciesExist $currenciesExist")
                            if (!currenciesExist) {
                                // If currencies don't exist, load them from JSON and save to DB
                                loadCurrenciesFromJson()
                            } else {
                                // Check if categories exist
                                checkCategoriesExist()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.home_error_generic, e.message ?: ""))
                }
            }
        }
    }

    private fun loadCurrenciesFromJson() {
        viewModelScope.launch {
            try {
                // Load currencies from JSON file in assets
                val currencies = currencyDataSource.loadCurrencies(context)

                // Save currencies to database
                saveCurrenciesUseCase(SaveCurrenciesUseCase.Params(currencies)) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(isLoading = false, error = context.getString(R.string.home_error_save_currencies))
                            }
                        },
                        {
                            // After saving currencies, check if categories exist
                            checkCategoriesExist()
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.home_error_load_currencies, e.message ?: ""))
                }
            }
        }
    }

    private fun checkCategoriesExist() {
        viewModelScope.launch {
            try {
                checkCategoriesExistUseCase(UseCase.None()) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(isLoading = false, error = context.getString(R.string.home_error_check_categories))
                            }
                        },
                        { categoriesExist ->
                            Log.i(
                                HomeScreenViewModel::class.java.simpleName,
                                "loadData: categoriesExist $categoriesExist"
                            )
                            if (!categoriesExist) {
                                // If categories don't exist, load them from JSON and save to DB
                                loadCategoriesFromJson()
                            } else {
                                // Categories already exist, proceed to check for a default wallet
                                checkForDefaultWallet()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.home_error_check_categories))
                }
            }
        }
    }

    private fun loadCategoriesFromJson() {
        viewModelScope.launch {
            try {
                // Seed categories into DB using normalized CategoryEntity models.
                //
                // This path ensures that:
                // - Each parent category is stored as a top‑level row (parentName = null)
                //   with its own title/icon from JSON.
                // - Each subcategory is linked via parentName = parent title key.
                // Later, the repository groups them so that parent headers
                // always use the correct parent title & icon instead of
                // falling back to the first child.
                val entities = categoryDataSource.loadCategoryEntities(context)
                val allCategories = entities.map { it.toCategory() }

                insertCategoriesUseCase(InsertCategoriesUseCase.Params(allCategories)) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(isLoading = false, error = context.getString(R.string.home_error_save_categories))
                            }
                        },
                        {
                            // After saving categories, check for default wallet
                            checkForDefaultWallet()
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.home_error_load_categories, e.message ?: ""))
                }
            }
        }
    }

    private fun checkForDefaultWallet() {
        viewModelScope.launch {
            try {
                // Get wallets using UseCase
                getWalletsUseCase(UseCase.None()) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(isLoading = false, error = context.getString(R.string.home_error_load_wallets))
                            }
                        },
                        { walletsFlow ->
                            walletsFlow
                                .onEach { wallets ->
                                    Log.i(HomeScreenViewModel::class.java.simpleName, "checkForDefaultWallet: wallets: $wallets")
                                    if (wallets.isEmpty()) {
                                        // If no wallets exist, create a default wallet with VND
                                        createDefaultWallet()
                                    } else {
                                        // Wallets exist, load them and update the UI
                                        updateWalletsInUI(wallets)
                                    }
                                }
                                .catch { e ->
                                    _viewState.update {
                                        it.copy(isLoading = false, error = context.getString(R.string.home_error_load_wallets_with_message, e.message ?: ""))
                                    }
                                }
                                .launchIn(viewModelScope)
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.home_error_check_wallets_with_message, e.message ?: ""))
                }
            }
        }
    }

    private fun createDefaultWallet() {
        viewModelScope.launch {
            try {
                // Get VND currency by code using UseCase
                getCurrencyByCodeUseCase(GetCurrencyByCodeUseCase.Params("VND")) { result ->
                    result.fold(
                        { failure ->
                            when (failure) {
                                is Failure.NotFound -> {
                                    _viewState.update {
                                        it.copy(isLoading = false, error = context.getString(R.string.home_error_vnd_not_found))
                                    }
                                }
                                else -> {
                                    _viewState.update {
                                        it.copy(isLoading = false, error = context.getString(R.string.home_error_get_currency))
                                    }
                                }
                            }
                        },
                        { vndCurrency ->
                            // Create a default wallet with VND currency
                            val defaultWallet = WalletWithCurrencyEntity(
                                wallet = WalletEntity(
                                    id = 0, // Room will generate ID for new wallet
                                    walletName = context.getString(R.string.home_default_wallet_name),
                                    currentBalance = 0.0,
                                    currencyId = vndCurrency.id,
                                    isMainWallet = true
                                ),
                                currency = vndCurrency
                            )

                            // Save the default wallet
                            createWalletUseCase(CreateWalletUseCase.Params(defaultWallet)) { result ->
                                result.fold(
                                    { failure ->
                                        _viewState.update {
                                            it.copy(
                                                isLoading = false,
                                                error = context.getString(R.string.home_error_create_default_wallet)
                                            )
                                        }
                                    },
                                    {
                                        // Reload wallets to get the newly created wallet
                                        reloadWallets()
                                    }
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.home_error_create_default_wallet)
                    )
                }
            }
        }
    }

    private fun reloadWallets() {
        viewModelScope.launch {
            try {
                // Reload wallets using UseCase
                getWalletsUseCase(UseCase.None()) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(isLoading = false, error = context.getString(R.string.home_error_reload_wallets))
                            }
                        },
                        { walletsFlow ->
                            walletsFlow
                                .onEach { wallets ->
                                    updateWalletsInUI(wallets)
                                }
                                .catch { e ->
                                    _viewState.update {
                                        it.copy(isLoading = false, error = context.getString(R.string.home_error_reload_wallets))
                                    }
                                }
                                .launchIn(viewModelScope)
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(isLoading = false, error = context.getString(R.string.home_error_reload_wallets))
                }
            }
        }
    }

    private fun updateWalletsInUI(wallets: List<Wallet>) {
        viewModelScope.launch {
            try {
                // Initialize currency converter
                currencyConverter.initialize()

                // Find the main wallet and its currency
                val mainWallet = wallets.find { it.isMainWallet }
                val mainCurrency = mainWallet?.currency?.currencyCode ?: "VND"

                // Calculate total balance by converting all amounts to main currency
                var totalBalance = 0.0
                wallets.forEach { wallet ->
                    val convertedAmount = if (wallet.currency.currencyCode == mainCurrency) {
                        wallet.currentBalance
                    } else {
                        currencyConverter.convert(
                            amount = wallet.currentBalance,
                            fromCurrency = wallet.currency.currencyCode,
                            toCurrency = mainCurrency
                        ) ?: 0.0
                    }
                    totalBalance += convertedAmount
                }

                // Format the balance with the main currency symbol
                val currencySymbol = mainWallet?.currency?.symbol ?: "đ"
                val formattedBalance = String.format(java.util.Locale.US, "%,.0f %s", totalBalance, currencySymbol)

                // Convert domain models to entity models for UI state
                val walletEntities = wallets.map { wallet ->
                    WalletWithCurrencyEntity(
                        wallet = WalletEntity(
                            id = wallet.id,
                            walletName = wallet.walletName,
                            currentBalance = wallet.currentBalance,
                            currencyId = wallet.currency.id,
                            isMainWallet = wallet.isMainWallet
                        ),
                        currency = CurrencyEntity(
                            id = wallet.currency.id,
                            currencyName = wallet.currency.currencyName,
                            currencyCode = wallet.currency.currencyCode,
                            symbol = wallet.currency.symbol,
                            displayType = wallet.currency.displayType,
                            image = wallet.currency.image
                        )
                    )
                }

                _viewState.update {
                    it.copy(
                        isLoading = false,
                        totalBalance = formattedBalance,
                        wallets = walletEntities,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.home_error_calculate_balance, e.message ?: "")
                    )
                }
            }
        }
    }

    private fun toggleBalanceVisibility() {
        _viewState.update { currentState ->
            currentState.copy(isBalanceVisible = !currentState.isBalanceVisible)
        }
    }

    // Provides a Flow of wallet lists from GetWalletsUseCase.
    private fun walletsStream() = callbackFlow<List<Wallet>> {
        var job: Job? = null
        getWalletsUseCase(UseCase.None()) { result ->
            result.fold(
                { /* handle failure if needed */ },
                { walletFlow ->
                    // Launch collection in this callbackFlow scope
                    job = walletFlow.onEach { send(it) }.launchIn(this)
                }
            )
        }
        // Cancel the collection when the flow is closed
        awaitClose { job?.cancel() }
    }

    // Provides a Flow of all Transactions flattened from ObserveTransactionsUseCase.
    private fun transactionsStream() = callbackFlow<List<Transaction>> {
        var job: Job? = null
        observeTransactionsUseCase(ObserveTransactionsUseCase.Params.AllWallets, scope = viewModelScope) { result ->
            result.fold(
                { /* handle failure if needed */ },
                { txFlow ->
                    // Launch collection in this callbackFlow scope
                    job = txFlow.onEach { dailyList ->
                        send(dailyList.flatMap { it.transactions })
                    }.launchIn(this)
                }
            )
        }
        // Cancel the collection when the flow is closed
        awaitClose { job?.cancel() }
    }
}