package com.uet.expensetracker.features.money.ui.budgets.transactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.collectLatest
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen

@Composable
fun BudgetTransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetTransactionsViewModel = hiltViewModel(),
    budgetId: Int,
    navController: NavController
) {
    // Collect state
    val state by viewModel.viewState.collectAsState()

    // Handle events
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.event
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest { event ->
                when (event) {
                    is BudgetTransactionsEvent.NavigateToTransactionDetail -> {
                        navController.navigate(
                            Screen.DetailTransaction.createRoute(event.transactionId)
                        )
                    }
                    is BudgetTransactionsEvent.NavigateBack -> {
                        navController.popBackStack()
                    }
                    is BudgetTransactionsEvent.ShowError -> {
                        // TODO: Show error message via Snackbar or Toast
                    }
                    null -> {}
                }
            }
    }

    // UI Content
    BudgetTransactionsScreenContent(
        state = state,
        onBackClick = { viewModel.processIntent(BudgetTransactionsIntent.BackClicked) },
        onTransactionClick = { transaction -> 
            viewModel.processIntent(BudgetTransactionsIntent.TransactionClicked(transaction.id))
        },
        modifier = modifier
    )
} 