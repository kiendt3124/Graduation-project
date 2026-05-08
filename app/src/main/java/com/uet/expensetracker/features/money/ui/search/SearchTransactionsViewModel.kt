package com.uet.expensetracker.features.money.ui.search

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveWalletByIdUseCase
import com.uet.expensetracker.features.money.domain.usecases.SearchTransactionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.QuickSearchTransactionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetSearchSuggestionsUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetUsedCategoriesUseCase
import com.uet.expensetracker.features.money.domain.usecases.SearchFailure
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import com.uet.expensetracker.utils.CurrencyConverter
import com.uet.expensetracker.utils.createTotalWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SearchTransactionsViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val observeWalletByIdUseCase: ObserveWalletByIdUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val quickSearchTransactionsUseCase: QuickSearchTransactionsUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getUsedCategoriesUseCase: GetUsedCategoriesUseCase,
    private val currencyConverter: CurrencyConverter,
) : BaseViewModel<SearchTransactionsState, SearchTransactionsIntent, SearchTransactionsEvent>() {

    // Current wallet context
    private var currentWalletId = 1 // Default wallet ID
    private val TOTAL_WALLET_ID = -1 // Special ID for total wallet
    
    // Job tracking
    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null

    @SuppressLint("NewApi")
    override val _viewState = MutableStateFlow(
        SearchTransactionsState(
            currentWallet = getDefaultWallet(),
            isTotalWallet = false,
            isLoading = false
        )
    )

    init {
        processIntent(SearchTransactionsIntent.InitializeSearch)
    }

    override fun processIntent(intent: SearchTransactionsIntent) {
        when (intent) {
            // Search mode management
            is SearchTransactionsIntent.InitializeSearch -> initializeSearch()
            is SearchTransactionsIntent.ExitSearch -> exitSearch()
            
            // Search text and suggestions
            is SearchTransactionsIntent.UpdateSearchText -> updateSearchText(intent.text)
            is SearchTransactionsIntent.SelectSearchSuggestion -> selectSearchSuggestion(intent.suggestion)
            is SearchTransactionsIntent.LoadSearchSuggestions -> loadSearchSuggestions()
            is SearchTransactionsIntent.ClearSearchText -> clearSearchText()
            
            // Search filters
            is SearchTransactionsIntent.UpdateMinAmount -> updateMinAmount(intent.amount)
            is SearchTransactionsIntent.UpdateMaxAmount -> updateMaxAmount(intent.amount)
            is SearchTransactionsIntent.ToggleCategoryFilter -> toggleCategoryFilter(intent.categoryId)
            is SearchTransactionsIntent.UpdateTransactionTypeFilter -> updateTransactionTypeFilter(intent.type)
            is SearchTransactionsIntent.UpdateDateRange -> updateDateRange(intent.startDate, intent.endDate)
            
            // Search execution
            is SearchTransactionsIntent.ExecuteSearch -> executeSearch()
            is SearchTransactionsIntent.ExecuteQuickSearch -> executeQuickSearch()
            is SearchTransactionsIntent.ClearSearch -> clearSearch()
            is SearchTransactionsIntent.ClearAllFilters -> clearAllFilters()
            
            // UI actions
            is SearchTransactionsIntent.ToggleFiltersPanel -> toggleFiltersPanel()
            is SearchTransactionsIntent.LoadAvailableCategories -> loadAvailableCategories()
            
            // Navigation and interaction
            is SearchTransactionsIntent.NavigateToTransactionDetail -> navigateToTransactionDetail(intent.transactionId)
            is SearchTransactionsIntent.NavigateBack -> navigateBack()
            
            // Quick search actions
            is SearchTransactionsIntent.UseQuickSearchTerm -> useQuickSearchTerm(intent.term)
            is SearchTransactionsIntent.SaveToRecentSearches -> saveToRecentSearches(intent.searchText)
            is SearchTransactionsIntent.ClearRecentSearches -> clearRecentSearches()
            
            // Date range shortcuts
            is SearchTransactionsIntent.SetLast7Days -> setLast7Days()
            is SearchTransactionsIntent.SetLast30Days -> setLast30Days()
            is SearchTransactionsIntent.SetLast3Months -> setLast3Months()
            is SearchTransactionsIntent.SetThisMonth -> setThisMonth()
            is SearchTransactionsIntent.SetLastMonth -> setLastMonth()
        }
    }

    // ==================== SEARCH MODE MANAGEMENT ====================

    /**
     * Initialize search mode and load initial data
     */
    private fun initializeSearch() {
        _viewState.value = _viewState.value.copy(
            searchCriteria = SearchCriteria(),
            searchResults = emptyList(),
            searchError = null,
            isLoading = false
        )
        
        // Load available categories for filter dropdown
        loadAvailableCategories()
        emitEvent(SearchTransactionsEvent.ShowFiltersPanel)
    }

    /**
     * Exit search mode and emit navigation event
     */
    private fun exitSearch() {
        // Cancel any ongoing search operations
        cancelSearchOperations()
        
        _viewState.value = _viewState.value.copy(
            searchCriteria = SearchCriteria(),
            searchResults = emptyList(),
            searchSuggestions = emptyList(),
            searchError = null,
            isSearching = false,
            showFiltersPanel = false
        )
        
        emitEvent(SearchTransactionsEvent.SearchCleared)
        emitEvent(SearchTransactionsEvent.NavigateBack)
    }

    // ==================== SEARCH TEXT AND SUGGESTIONS ====================

    /**
     * Update search text and trigger appropriate actions
     */
    private fun updateSearchText(text: String) {
        val newCriteria = _viewState.value.searchCriteria.copy(searchText = text)
        _viewState.value = _viewState.value.copy(searchCriteria = newCriteria)
        
        // Cancel previous suggestions job
        suggestionsJob?.cancel()
        
        // Trigger appropriate action based on text length
        when {
            text.isBlank() -> {
                _viewState.value = _viewState.value.copy(
                    searchSuggestions = emptyList(),
                    searchResults = emptyList()
                )
                emitEvent(SearchTransactionsEvent.HideSearchSuggestions)
            }
            text.length >= 2 -> {
                // Execute quick search for real-time results and load suggestions
                executeQuickSearchInternal(text)
                loadSearchSuggestions()
                emitEvent(SearchTransactionsEvent.ShowSearchSuggestions(_viewState.value.searchSuggestions))
            }
            text.length == 1 -> {
                // Only load suggestions for single character
                loadSearchSuggestions()
                emitEvent(SearchTransactionsEvent.ShowSearchSuggestions(_viewState.value.searchSuggestions))
            }
        }
    }

    /**
     * Select a search suggestion and execute search
     */
    private fun selectSearchSuggestion(suggestion: String) {
        updateSearchText(suggestion)
        executeQuickSearchInternal(suggestion)
        saveToRecentSearches(suggestion)
        emitEvent(SearchTransactionsEvent.HideSearchSuggestions)
    }

    /**
     * Load search suggestions based on current search text
     */
    private fun loadSearchSuggestions() {
        val searchText = _viewState.value.searchCriteria.searchText
        if (searchText.isBlank()) {
            _viewState.value = _viewState.value.copy(searchSuggestions = emptyList())
            return
        }
        
        val walletId = if (_viewState.value.isTotalWallet) null else currentWalletId
        
        suggestionsJob = viewModelScope.launch {
            getSearchSuggestionsUseCase(
                params = GetSearchSuggestionsUseCase.Params(
                    searchText = searchText,
                    walletId = walletId,
                    limit = 5
                ),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            android.util.Log.w("SearchTransactionsViewModel", "Failed to load suggestions: $failure")
                        },
                        { suggestionsFlow ->
                            viewModelScope.launch {
                                suggestionsFlow.collect { suggestions ->
                                    _viewState.value = _viewState.value.copy(searchSuggestions = suggestions)
                                }
                            }
                        }
                    )
                }
            )
        }
    }

    /**
     * Clear search text
     */
    private fun clearSearchText() {
        _viewState.value = _viewState.value.copy(
            searchCriteria = _viewState.value.searchCriteria.copy(searchText = ""),
            searchSuggestions = emptyList(),
            searchResults = emptyList()
        )
        emitEvent(SearchTransactionsEvent.HideSearchSuggestions)
    }

    // ==================== SEARCH FILTERS ====================

    /**
     * Update minimum amount filter
     */
    private fun updateMinAmount(amount: Double?) {
        val newCriteria = _viewState.value.searchCriteria.copy(minAmount = amount)
        _viewState.value = _viewState.value.copy(searchCriteria = newCriteria)
    }

    /**
     * Update maximum amount filter
     */
    private fun updateMaxAmount(amount: Double?) {
        val newCriteria = _viewState.value.searchCriteria.copy(maxAmount = amount)
        _viewState.value = _viewState.value.copy(searchCriteria = newCriteria)
    }

    /**
     * Toggle category filter selection
     */
    private fun toggleCategoryFilter(categoryId: Int) {
        val currentIds = _viewState.value.searchCriteria.selectedCategoryIds.toMutableList()
        if (currentIds.contains(categoryId)) {
            currentIds.remove(categoryId)
        } else {
            currentIds.add(categoryId)
        }
        
        val newCriteria = _viewState.value.searchCriteria.copy(selectedCategoryIds = currentIds)
        _viewState.value = _viewState.value.copy(searchCriteria = newCriteria)
    }

    /**
     * Update transaction type filter
     */
    private fun updateTransactionTypeFilter(type: TransactionType?) {
        val newCriteria = _viewState.value.searchCriteria.copy(selectedTransactionType = type)
        _viewState.value = _viewState.value.copy(searchCriteria = newCriteria)
    }

    /**
     * Update date range filter
     */
    private fun updateDateRange(startDate: Long?, endDate: Long?) {
        val newCriteria = _viewState.value.searchCriteria.copy(
            startDate = startDate,
            endDate = endDate
        )
        _viewState.value = _viewState.value.copy(searchCriteria = newCriteria)
    }

    // ==================== SEARCH EXECUTION ====================

    /**
     * Execute comprehensive search with all filters
     */
    private fun executeSearch() {
        val criteria = _viewState.value.searchCriteria
        
        // Validate search criteria
        if (!criteria.hasActiveFilters) {
            emitEvent(SearchTransactionsEvent.ShowEmptySearchCriteria("Please enter keywords or select filters"))
            return
        }
        
        _viewState.value = _viewState.value.copy(isSearching = true, searchError = null)
        emitEvent(SearchTransactionsEvent.ShowLoading)
        
        val walletId = if (_viewState.value.isTotalWallet) null else currentWalletId
        
        // Cancel previous search job
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            searchTransactionsUseCase(
                params = SearchTransactionsUseCase.Params(
                    searchText = criteria.searchText.takeIf { it.isNotBlank() },
                    minAmount = criteria.minAmount,
                    maxAmount = criteria.maxAmount,
                    categoryIds = criteria.selectedCategoryIds.takeIf { it.isNotEmpty() },
                    walletId = walletId,
                    transactionType = criteria.selectedTransactionType,
                    startDate = criteria.startDate,
                    endDate = criteria.endDate
                ),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure -> handleSearchFailure(failure) },
                        { transactionsFlow ->
                            viewModelScope.launch {
                                transactionsFlow.collect { transactions ->
                                    // Group transactions by date
                                    val groupedResults = groupTransactionsByDate(transactions)
                                    _viewState.value = _viewState.value.copy(
                                        searchResults = groupedResults,
                                        isSearching = false
                                    )
                                    
                                    // Save successful search to recent searches
                                    if (criteria.searchText.isNotBlank()) {
                                        saveToRecentSearches(criteria.searchText)
                                    }
                                    
                                    emitEvent(SearchTransactionsEvent.HideLoading)
                                    emitEvent(SearchTransactionsEvent.SearchSuccessful)
                                }
                            }
                        }
                    )
                }
            )
        }
    }

    /**
     * Execute quick search for real-time results (public interface)
     */
    private fun executeQuickSearch() {
        val searchText = _viewState.value.searchCriteria.searchText
        if (searchText.length >= 2) {
            executeQuickSearchInternal(searchText)
        } else {
            emitEvent(SearchTransactionsEvent.SearchTextTooShort)
        }
    }

    /**
     * Execute quick search internal implementation
     */
    private fun executeQuickSearchInternal(searchText: String) {
        if (searchText.length < 2) return
        
        val walletId = if (_viewState.value.isTotalWallet) null else currentWalletId
        
        // Cancel previous search job
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            quickSearchTransactionsUseCase(
                params = QuickSearchTransactionsUseCase.Params(
                    searchText = searchText,
                    walletId = walletId,
                    limit = 20 // Limit for quick search
                ),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure -> 
                            // Silently handle quick search failures
                            android.util.Log.w("SearchTransactionsViewModel", "Quick search failed: $failure")
                        },
                        { transactionsFlow ->
                            viewModelScope.launch {
                                transactionsFlow.collect { transactions ->
                                    val groupedResults = groupTransactionsByDate(transactions)
                                    _viewState.value = _viewState.value.copy(
                                        searchResults = groupedResults
                                    )
                                }
                            }
                        }
                    )
                }
            )
        }
    }

    /**
     * Clear search results but keep filters
     */
    private fun clearSearch() {
        _viewState.value = _viewState.value.copy(
            searchResults = emptyList(),
            searchError = null,
            isSearching = false
        )
        emitEvent(SearchTransactionsEvent.SearchCleared)
        emitEvent(SearchTransactionsEvent.HideLoading)
    }

    /**
     * Clear all search filters and results
     */
    private fun clearAllFilters() {
        _viewState.value = _viewState.value.copy(
            searchCriteria = SearchCriteria(),
            searchResults = emptyList(),
            searchError = null,
            isSearching = false,
            searchSuggestions = emptyList()
        )
        emitEvent(SearchTransactionsEvent.SearchCleared)
        emitEvent(SearchTransactionsEvent.HideLoading)
        emitEvent(SearchTransactionsEvent.HideSearchSuggestions)
    }

    // ==================== UI ACTIONS ====================

    /**
     * Toggle filters panel visibility
     */
    private fun toggleFiltersPanel() {
        val newShowState = !_viewState.value.showFiltersPanel
        _viewState.value = _viewState.value.copy(showFiltersPanel = newShowState)
        
        if (newShowState) {
            emitEvent(SearchTransactionsEvent.ShowFiltersPanel)
        } else {
            emitEvent(SearchTransactionsEvent.HideFiltersPanel)
        }
    }

    /**
     * Load available categories for filter dropdown
     */
    private fun loadAvailableCategories() {
        val walletId = if (_viewState.value.isTotalWallet) null else currentWalletId
        
        viewModelScope.launch {
            getUsedCategoriesUseCase(
                params = GetUsedCategoriesUseCase.Params(walletId = walletId),
                scope = viewModelScope,
                onResult = { result ->
                    result.fold(
                        { failure ->
                            android.util.Log.w("SearchTransactionsViewModel", "Failed to load categories: $failure")
                        },
                        { categoriesFlow ->
                            viewModelScope.launch {
                                categoriesFlow.collect { categoryIds ->
                                    _viewState.value = _viewState.value.copy(availableCategoryIds = categoryIds)
                                }
                            }
                        }
                    )
                }
            )
        }
    }

    // ==================== NAVIGATION AND INTERACTION ====================

    /**
     * Navigate to transaction detail
     */
    private fun navigateToTransactionDetail(transactionId: Int) {
        emitEvent(SearchTransactionsEvent.NavigateToTransactionDetail(transactionId))
    }

    /**
     * Navigate back
     */
    private fun navigateBack() {
        exitSearch()
    }

    // ==================== QUICK SEARCH ACTIONS ====================

    /**
     * Use a quick search term
     */
    private fun useQuickSearchTerm(term: String) {
        updateSearchText(term)
        executeQuickSearchInternal(term)
    }

    /**
     * Save search text to recent searches
     */
    private fun saveToRecentSearches(searchText: String) {
        if (searchText.isBlank()) return
        
        val currentRecent = _viewState.value.recentSearches.toMutableList()
        
        // Remove if already exists
        currentRecent.remove(searchText)
        
        // Add to beginning
        currentRecent.add(0, searchText)
        
        // Keep only last 10 searches
        if (currentRecent.size > 10) {
            currentRecent.removeAt(currentRecent.size - 1)
        }
        
        _viewState.value = _viewState.value.copy(recentSearches = currentRecent)
        emitEvent(SearchTransactionsEvent.SearchSavedToHistory)
    }

    /**
     * Clear recent searches
     */
    private fun clearRecentSearches() {
        _viewState.value = _viewState.value.copy(recentSearches = emptyList())
        emitEvent(SearchTransactionsEvent.RecentSearchesCleared)
    }

    // ==================== DATE RANGE SHORTCUTS ====================

    /**
     * Set date range to last 7 days
     */
    private fun setLast7Days() {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (7 * 24 * 60 * 60 * 1000L)
        updateDateRange(startDate, endDate)
    }

    /**
     * Set date range to last 30 days
     */
    private fun setLast30Days() {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (30 * 24 * 60 * 60 * 1000L)
        updateDateRange(startDate, endDate)
    }

    /**
     * Set date range to last 3 months
     */
    private fun setLast3Months() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, -3)
        val startDate = calendar.timeInMillis
        
        updateDateRange(startDate, endDate)
    }

    /**
     * Set date range to this month
     */
    private fun setThisMonth() {
        val calendar = Calendar.getInstance()
        
        // End of today
        val endDate = calendar.timeInMillis
        
        // Start of this month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis
        
        updateDateRange(startDate, endDate)
    }

    /**
     * Set date range to last month
     */
    private fun setLastMonth() {
        val calendar = Calendar.getInstance()
        
        // Start of this month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // End of last month (one millisecond before start of this month)
        val endDate = calendar.timeInMillis - 1
        
        // Start of last month
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.timeInMillis
        
        updateDateRange(startDate, endDate)
    }

    // ==================== HELPER METHODS ====================

    /**
     * Handle search failure and emit appropriate events
     */
    private fun handleSearchFailure(failure: com.uet.expensetracker.core.failure.Failure) {
        _viewState.value = _viewState.value.copy(isSearching = false)
        emitEvent(SearchTransactionsEvent.HideLoading)
        
        when (failure) {
            is SearchFailure.InvalidAmountRange -> {
                emitEvent(SearchTransactionsEvent.ShowInvalidAmountRange("Minimum amount must be less than maximum amount"))
            }
            is SearchFailure.InvalidDateRange -> {
                emitEvent(SearchTransactionsEvent.ShowInvalidDateRange("Start date must be before end date"))
            }
            is SearchFailure.EmptySearchQuery -> {
                emitEvent(SearchTransactionsEvent.ShowEmptySearchCriteria("Please enter search keywords"))
            }
            is SearchFailure.SearchTextTooShort -> {
                emitEvent(SearchTransactionsEvent.SearchTextTooShort)
            }
            else -> {
                emitEvent(SearchTransactionsEvent.ShowSearchError("An error occurred while searching"))
            }
        }
    }

    /**
     * Group transactions by date for display
     */
    private fun groupTransactionsByDate(transactions: List<Transaction>): List<DailyTransactions> {
        val calendar = Calendar.getInstance()
        
        return transactions.groupBy { transaction ->
            calendar.time = transaction.transactionDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.map { (dayMillis, txns) ->
            val date = java.util.Date(dayMillis)
            
            var total = 0.0
            txns.forEach { transaction ->
                total += if (transaction.transactionType == TransactionType.INFLOW) transaction.amount else -transaction.amount
            }
            
            DailyTransactions.create(date, txns, total)
        }.sortedByDescending { it.date.time }
    }

    /**
     * Cancel ongoing search operations
     */
    private fun cancelSearchOperations() {
        searchJob?.cancel()
        suggestionsJob?.cancel()
        searchJob = null
        suggestionsJob = null
    }

    /**
     * Get default wallet helper
     */
    private fun getDefaultWallet(): Wallet {
        return Wallet(
            id = 1,
            walletName = "Default Wallet",
            currentBalance = 0.0,
            currency = com.uet.expensetracker.features.money.domain.model.Currency(
                id = 1,
                currencyName = "Vietnamese Dong",
                currencyCode = "VND",
                symbol = "₫"
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing search operations to prevent memory leaks
        cancelSearchOperations()
        android.util.Log.d("SearchTransactionsViewModel", "ViewModel cleared, all search operations cancelled")
    }
}
