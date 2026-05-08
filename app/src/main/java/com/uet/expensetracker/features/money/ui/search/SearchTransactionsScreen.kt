package com.uet.expensetracker.features.money.ui.search

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.transactions.components.DailyTransactionHeader
import com.uet.expensetracker.features.money.ui.search.components.SearchBar
import com.uet.expensetracker.features.money.ui.search.components.SearchFiltersPanel
import com.uet.expensetracker.features.money.ui.transactions.components.TransactionItem
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen

/**
 * Main SearchTransactionsScreen composable for dedicated search functionality
 */
@Composable
fun SearchTransactionsScreen(
    viewModel: SearchTransactionsViewModel = hiltViewModel(),
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsState()

    // Initialize search when screen loads
    LaunchedEffect(Unit) {
        viewModel.processIntent(SearchTransactionsIntent.InitializeSearch)
        viewModel.processIntent(SearchTransactionsIntent.LoadAvailableCategories)
    }

    // Handle search events
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is SearchTransactionsEvent.NavigateToTransactionDetail -> {
                    Log.i("SearchTransactionsScreen", "Navigate to transaction detail: ${event.transactionId}")
                    navController?.navigate(Screen.DetailTransaction.createRoute(event.transactionId))
                }
                
                is SearchTransactionsEvent.NavigateBack -> {
                    Log.i("SearchTransactionsScreen", "Navigate back")
                    navController?.popBackStack()
                }

                is SearchTransactionsEvent.ShowSearchError -> {
                    Log.e("SearchTransactionsScreen", "Search error: ${event.message}")
                    // TODO: Show snackbar or toast
                }
                
                is SearchTransactionsEvent.ShowInvalidAmountRange -> {
                    Log.e("SearchTransactionsScreen", "Invalid amount range: ${event.message}")
                    // TODO: Show snackbar or toast
                }
                
                is SearchTransactionsEvent.ShowInvalidDateRange -> {
                    Log.e("SearchTransactionsScreen", "Invalid date range: ${event.message}")
                    // TODO: Show snackbar or toast
                }
                
                is SearchTransactionsEvent.ShowEmptySearchCriteria -> {
                    Log.w("SearchTransactionsScreen", "Empty search criteria: ${event.message}")
                    // TODO: Show snackbar or toast
                }
                
                SearchTransactionsEvent.SearchTextTooShort -> {
                    Log.w("SearchTransactionsScreen", "Search text too short")
                    // TODO: Show snackbar or toast
                }
                
                SearchTransactionsEvent.SearchSuccessful -> {
                    Log.d("SearchTransactionsScreen", "Search completed successfully")
                }
                
                SearchTransactionsEvent.SearchCleared -> {
                    Log.d("SearchTransactionsScreen", "Search cleared")
                }

                else -> {
                    // Handle other events silently
                }
            }
        }
    }

    SearchTransactionsScreenContent(
        state = state,
        viewModel = viewModel,
        onBackClick = {
            viewModel.processIntent(SearchTransactionsIntent.NavigateBack)
        },
        onTransactionClick = { transaction ->
            viewModel.processIntent(SearchTransactionsIntent.NavigateToTransactionDetail(transaction.id))
        },
        modifier = modifier
    )
}

/**
 * Main content composable for search screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTransactionsScreenContent(
    state: SearchTransactionsState,
    viewModel: SearchTransactionsViewModel?,
    onBackClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchCriteria = state.searchCriteria
    val searchResults = state.searchResults
    val searchSuggestions = state.searchSuggestions
    val isSearching = state.isSearching
    val availableCategoryIds = state.availableCategoryIds
    
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor,
        topBar = {
            SearchTopAppBar(
                onBackClick = onBackClick,
                currentWalletName = state.currentWallet.walletName,
                isTotalWallet = state.isTotalWallet
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize()
        ) {
            // Search Bar
            SearchBar(
                searchText = searchCriteria.searchText,
                suggestions = searchSuggestions,
                isSearching = isSearching,
                onSearchTextChange = { viewModel?.processIntent(SearchTransactionsIntent.UpdateSearchText(it)) },
                onSuggestionClick = { viewModel?.processIntent(SearchTransactionsIntent.SelectSearchSuggestion(it)) },
                onSearchSubmit = { viewModel?.processIntent(SearchTransactionsIntent.ExecuteSearch) },
                onClearSearch = { viewModel?.processIntent(SearchTransactionsIntent.ClearSearch) },
                onToggleFilters = { 
                    showFilters = !showFilters
                    viewModel?.processIntent(SearchTransactionsIntent.ToggleFiltersPanel)
                },
                hasActiveFilters = searchCriteria.hasActiveFilters,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Search Filters Panel
            if (showFilters || state.showFiltersPanel) {
                SearchFiltersPanel(
                    minAmount = searchCriteria.minAmount,
                    maxAmount = searchCriteria.maxAmount,
                    selectedCategoryIds = searchCriteria.selectedCategoryIds,
                    availableCategoryIds = availableCategoryIds,
                    selectedTransactionType = searchCriteria.selectedTransactionType,
                    startDate = searchCriteria.startDate,
                    endDate = searchCriteria.endDate,
                    onMinAmountChange = { viewModel?.processIntent(SearchTransactionsIntent.UpdateMinAmount(it)) },
                    onMaxAmountChange = { viewModel?.processIntent(SearchTransactionsIntent.UpdateMaxAmount(it)) },
                    onCategoryToggle = { viewModel?.processIntent(SearchTransactionsIntent.ToggleCategoryFilter(it)) },
                    onTransactionTypeChange = { viewModel?.processIntent(SearchTransactionsIntent.UpdateTransactionTypeFilter(it)) },
                    onDateRangeChange = { start, end -> viewModel?.processIntent(SearchTransactionsIntent.UpdateDateRange(start, end)) },
                    onClearFilters = { viewModel?.processIntent(SearchTransactionsIntent.ClearAllFilters) },
                    onApplyFilters = { 
                        viewModel?.processIntent(SearchTransactionsIntent.ExecuteSearch)
                        showFilters = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Search Content
            when {
                state.isLoading -> {
                    SearchLoadingState()
                }
                
                !searchCriteria.hasActiveFilters -> {
                    SearchInitialState(
                        recentSearches = state.recentSearches,
                        onQuickSearchClick = { query ->
                            viewModel?.processIntent(SearchTransactionsIntent.UpdateSearchText(query))
                            viewModel?.processIntent(SearchTransactionsIntent.ExecuteSearch)
                        },
                        onRecentSearchClick = { query ->
                            viewModel?.processIntent(SearchTransactionsIntent.UseQuickSearchTerm(query))
                        },
                        onClearRecentSearches = {
                            viewModel?.processIntent(SearchTransactionsIntent.ClearRecentSearches)
                        }
                    )
                }
                
                searchResults.isEmpty() -> {
                    SearchEmptyState(
                        searchCriteria = searchCriteria,
                        onClearSearch = { viewModel?.processIntent(SearchTransactionsIntent.ClearSearch) },
                        onClearAllFilters = { viewModel?.processIntent(SearchTransactionsIntent.ClearAllFilters) }
                    )
                }
                
                else -> {
                    SearchResultsContent(
                        searchResults = searchResults,
                        searchCriteria = searchCriteria,
                        onTransactionClick = onTransactionClick,
                        showWalletIndicator = state.isTotalWallet
                    )
                }
            }
        }
    }
}

/**
 * Top app bar for search screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopAppBar(
    onBackClick: () -> Unit,
    currentWalletName: String,
    isTotalWallet: Boolean
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Search Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Text(
                    text = if (isTotalWallet) "All Wallets" else currentWalletName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMain
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            titleContentColor = AppColor.Light.PrimaryColor.contentColor
        )
    )
}

/**
 * Loading state during search
 */
@Composable
private fun SearchLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = TextAccent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Searching...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Initial state when no search is active
 */
@Composable
private fun SearchInitialState(
    recentSearches: List<String>,
    onQuickSearchClick: (String) -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickSearchTerms = listOf(
        "Coffee", "Food & Dining", "Gas & Fuel", "Shopping", 
        "Phone", "Internet", "Rent", "Healthcare",
        "Salary", "Bonus", "Savings", "Investment"
    )
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        // Search Icon and Instructions
//        item {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Search,
//                    contentDescription = "Search",
//                    modifier = Modifier.size(64.dp),
//                    tint = TextSecondary
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Text(
//                    text = "Search Transactions",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = TextPrimary
//                )
//
//                Text(
//                    text = "Enter keywords or use filters to search",
//                    fontSize = 14.sp,
//                    color = TextSecondary,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//        }
        
        // Recent Searches
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    
                    TextButton(onClick = onClearRecentSearches) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            items(recentSearches.take(5)) { searchTerm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onRecentSearchClick(searchTerm) },
                    colors = CardDefaults.cardColors(
                        containerColor = AppColor.Light.SecondaryColor.color2
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recent search",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = searchTerm,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Use search",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Quick Search Terms
        item {
            Text(
                text = "Quick Search:",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        items(quickSearchTerms.chunked(2)) { rowTerms ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTerms.forEach { term ->
                    ElevatedButton(
                        onClick = { onQuickSearchClick(term) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFE4E7EC),
                            contentColor = Color(0xFF2B3B48)
                        )
                    ) {
                        Text(
                            text = term,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Fill remaining space if odd number of terms
                if (rowTerms.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Empty state when no search results found
 */
@Composable
private fun SearchEmptyState(
    searchCriteria: SearchCriteria,
    onClearSearch: () -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
//        Icon(
//            imageVector = Icons.Default.Search,
//            contentDescription = "No results",
//            modifier = Modifier.size(64.dp),
//            tint = TextSecondary
//        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No transactions found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        if (searchCriteria.isTextOnlySearch) {
            Text(
                text = "No transactions match \"${searchCriteria.searchText}\"",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "No transactions match the current filters",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        if (searchCriteria.isTextOnlySearch) {
            OutlinedButton(
                onClick = onClearSearch,
                modifier= Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2B3B48)
                )
            ) {
                Text("Clear Search")
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearSearch,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2B3B48)
                    )
                ) {
                    Text("Clear Search Results")
                }

                TextButton(onClick = onClearAllFilters) {
                    Text(
                        "Clear All Filters",
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Content showing search results
 */
@Composable
private fun SearchResultsContent(
    searchResults: List<DailyTransactions>,
    searchCriteria: SearchCriteria,
    onTransactionClick: (Transaction) -> Unit,
    showWalletIndicator: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search summary
        item {
            SearchResultsSummary(
                resultCount = searchResults.sumOf { it.transactions.size },
                searchCriteria = searchCriteria
            )
        }
        
        // Daily transaction groups
        items(searchResults) { dailyTransactions ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DailyTransactionHeader(
                    date = dailyTransactions.date,
                    dailyTotal = dailyTransactions.dailyTotal
                )
                
                dailyTransactions.transactions.forEach { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) },
                        showWalletIndicator = showWalletIndicator
                    )
                }
            }
        }
    }
}

/**
 * Summary of search results
 */
@Composable
private fun SearchResultsSummary(
    resultCount: Int,
    searchCriteria: SearchCriteria,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColor.Light.SecondaryColor.color2
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search results",
                tint = TextAccent,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Found $resultCount transactions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                if (searchCriteria.searchText.isNotBlank()) {
                    Text(
                        text = "Keyword: \"${searchCriteria.searchText}\"",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Preview for SearchTransactionsScreen
 */
@Preview(showBackground = true)
@Composable
fun SearchTransactionsScreenPreview() {
    ExpenseTrackerTheme {
        val previewState = SearchTransactionsState(
            searchCriteria = SearchCriteria(
                searchText = "cafe"
            ),
            currentWallet = Wallet(
                id = 1,
                walletName = "My Wallet",
                currentBalance = 1000000.0,
                currency = Currency(
                    id = 1,
                    currencyName = "Vietnamese Dong",
                    currencyCode = "VND",
                    symbol = "₫"
                )
            ),
            recentSearches = listOf("coffee", "food & dining", "gas & fuel")
        )
        
        SearchTransactionsScreenContent(
            state = previewState,
            viewModel = null
        )
    }
}
