package com.uet.expensetracker.features.money.ui.search

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import java.util.Date

/**
 * Contract for Search Transactions feature following MVI pattern
 */

/**
 * Search criteria data class with computed properties
 */
data class SearchCriteria(
    val searchText: String = "",
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val selectedCategoryIds: List<Int> = emptyList(),
    val selectedTransactionType: TransactionType? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
) {
    /**
     * Computed property to check if any filters are active
     */
    val hasActiveFilters: Boolean
        get() = searchText.isNotBlank() ||
                minAmount != null ||
                maxAmount != null ||
                selectedCategoryIds.isNotEmpty() ||
                selectedTransactionType != null ||
                startDate != null ||
                endDate != null
                
    /**
     * Check if only text search is active (no other filters)
     */
    val isTextOnlySearch: Boolean
        get() = searchText.isNotBlank() && 
                minAmount == null &&
                maxAmount == null &&
                selectedCategoryIds.isEmpty() &&
                selectedTransactionType == null &&
                startDate == null &&
                endDate == null
}

/**
 * State for Search Transactions screen
 */
data class SearchTransactionsState(
    // Core search state
    val searchCriteria: SearchCriteria = SearchCriteria(),
    val searchResults: List<DailyTransactions> = emptyList(),
    val isSearching: Boolean = false,
    val searchSuggestions: List<String> = emptyList(),
    val availableCategoryIds: List<Int> = emptyList(),
    val searchError: String? = null,
    
    // Wallet context
    val currentWallet: Wallet = getDefaultWallet(),
    val isTotalWallet: Boolean = false,
    
    // UI state
    val isLoading: Boolean = false,
    val showFiltersPanel: Boolean = false,
    
    // Search history and quick filters
    val recentSearches: List<String> = emptyList(),
    val quickSearchTerms: List<String> = listOf(
        "Cafe", "Ăn uống", "Xăng xe", "Mua sắm", 
        "Điện thoại", "Internet", "Tiền nhà", "Y tế"
    )
) : MviStateBase

/**
 * Intents for Search Transactions screen
 */
sealed class SearchTransactionsIntent : MviIntentBase {
    
    // Search mode management
    object InitializeSearch : SearchTransactionsIntent()
    object ExitSearch : SearchTransactionsIntent()
    
    // Search text and suggestions
    data class UpdateSearchText(val text: String) : SearchTransactionsIntent()
    data class SelectSearchSuggestion(val suggestion: String) : SearchTransactionsIntent()
    object LoadSearchSuggestions : SearchTransactionsIntent()
    object ClearSearchText : SearchTransactionsIntent()
    
    // Search filters
    data class UpdateMinAmount(val amount: Double?) : SearchTransactionsIntent()
    data class UpdateMaxAmount(val amount: Double?) : SearchTransactionsIntent()
    data class ToggleCategoryFilter(val categoryId: Int) : SearchTransactionsIntent()
    data class UpdateTransactionTypeFilter(val type: TransactionType?) : SearchTransactionsIntent()
    data class UpdateDateRange(val startDate: Long?, val endDate: Long?) : SearchTransactionsIntent()
    
    // Search execution
    object ExecuteSearch : SearchTransactionsIntent()
    object ExecuteQuickSearch : SearchTransactionsIntent()
    object ClearSearch : SearchTransactionsIntent()
    object ClearAllFilters : SearchTransactionsIntent()
    
    // UI actions
    object ToggleFiltersPanel : SearchTransactionsIntent()
    object LoadAvailableCategories : SearchTransactionsIntent()
    
    // Navigation and interaction
    data class NavigateToTransactionDetail(val transactionId: Int) : SearchTransactionsIntent()
    object NavigateBack : SearchTransactionsIntent()
    
    // Quick search actions
    data class UseQuickSearchTerm(val term: String) : SearchTransactionsIntent()
    data class SaveToRecentSearches(val searchText: String) : SearchTransactionsIntent()
    object ClearRecentSearches : SearchTransactionsIntent()
    
    // Date range shortcuts
    object SetLast7Days : SearchTransactionsIntent()
    object SetLast30Days : SearchTransactionsIntent()
    object SetLast3Months : SearchTransactionsIntent()
    object SetThisMonth : SearchTransactionsIntent()
    object SetLastMonth : SearchTransactionsIntent()
}

/**
 * Events for Search Transactions screen
 */
sealed class SearchTransactionsEvent : MviEventBase {
    
    // Navigation events
    data class NavigateToTransactionDetail(val transactionId: Int) : SearchTransactionsEvent()
    object NavigateBack : SearchTransactionsEvent()
    
    // Search result events
    object SearchSuccessful : SearchTransactionsEvent()
    object SearchCleared : SearchTransactionsEvent()
    data class ShowSearchError(val message: String) : SearchTransactionsEvent()
    
    // Validation events
    data class ShowInvalidAmountRange(val message: String) : SearchTransactionsEvent()
    data class ShowInvalidDateRange(val message: String) : SearchTransactionsEvent()
    object SearchTextTooShort : SearchTransactionsEvent()
    data class ShowEmptySearchCriteria(val message: String) : SearchTransactionsEvent()
    
    // UI events
    object ShowFiltersPanel : SearchTransactionsEvent()
    object HideFiltersPanel : SearchTransactionsEvent()
    data class ShowSearchSuggestions(val suggestions: List<String>) : SearchTransactionsEvent()
    object HideSearchSuggestions : SearchTransactionsEvent()
    
    // Success messages
    data class ShowSuccessMessage(val message: String) : SearchTransactionsEvent()
    data class ShowInfoMessage(val message: String) : SearchTransactionsEvent()
    
    // Loading states
    object ShowLoading : SearchTransactionsEvent()
    object HideLoading : SearchTransactionsEvent()
    
    // Search history events
    object SearchSavedToHistory : SearchTransactionsEvent()
    object RecentSearchesCleared : SearchTransactionsEvent()
}

/**
 * Helper function to create default wallet
 */
private fun getDefaultWallet(): Wallet {
    return Wallet(
        id = 1,
        walletName = "Default Wallet",
        currentBalance = 0.0,
        currency = Currency(
            id = 1,
            currencyName = "Vietnamese Dong",
            currencyCode = "VND",
            symbol = "₫"
        )
    )
} 