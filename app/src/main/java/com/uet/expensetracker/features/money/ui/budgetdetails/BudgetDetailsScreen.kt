package com.uet.expensetracker.features.money.ui.budgetdetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.features.money.ui.budgetdetails.components.BudgetSummaryCard
import com.uet.expensetracker.features.money.ui.budgetdetails.components.BudgetDurationCard
import com.uet.expensetracker.features.money.ui.budgetdetails.components.BudgetGraphCard
import com.uet.expensetracker.features.money.ui.budgetdetails.components.TransactionsButton
import com.uet.expensetracker.features.money.ui.budgetdetails.components.BudgetDetailsTopAppBar
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Budget
import java.util.Date

@Composable
fun BudgetDetailsScreen(
    viewModel: BudgetDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onEditBudget: () -> Unit = {},
    onShowTransactions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is BudgetDetailsEvent.NavigateBack -> onNavigateBack()
                is BudgetDetailsEvent.NavigateToEditBudget -> onEditBudget()
                is BudgetDetailsEvent.NavigateToTransactions -> onShowTransactions()
                is BudgetDetailsEvent.ShowDeleteConfirmation -> showDeleteDialog = true
                is BudgetDetailsEvent.ShowError -> {/* TODO: show error */}
                null -> Unit
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(
                    stringResource(R.string.budget_details_delete_title), 
                    color = TextMain,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    stringResource(R.string.budget_details_delete_message),
                    color = TextMain
                ) 
            },
            containerColor = AppColor.Light.PrimaryColor.cardColor,
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.confirmDelete()
                }) {
                    Text(stringResource(R.string.budget_details_delete_button), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.budget_details_cancel_button), color = TextMain)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            BudgetDetailsTopAppBar(
                onNavigateBack = { viewModel.processIntent(BudgetDetailsIntent.NavigateBack) },
                onEditBudget = { viewModel.processIntent(BudgetDetailsIntent.EditBudget) },
                onDeleteBudget = { viewModel.processIntent(BudgetDetailsIntent.DeleteBudget) }
            )
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        modifier = modifier
    ) { paddingValues ->
        BudgetDetailsContent(
            state = state,
            onNavigateBack = { viewModel.processIntent(BudgetDetailsIntent.NavigateBack) },
            onEditBudget = { viewModel.processIntent(BudgetDetailsIntent.EditBudget) },
            onDeleteBudget = { viewModel.processIntent(BudgetDetailsIntent.DeleteBudget) },
            onShowTransactions = { viewModel.processIntent(BudgetDetailsIntent.ShowTransactions) },
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        )
    }
}

@Composable
fun BudgetDetailsContent(
    state: BudgetDetailsState,
    onNavigateBack: () -> Unit,
    onEditBudget: () -> Unit,
    onDeleteBudget: () -> Unit,
    onShowTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BudgetSummaryCard(state = state, modifier = Modifier.padding(bottom = 16.dp))
        BudgetDurationCard(state = state, modifier = Modifier.padding(bottom = 16.dp))
        BudgetGraphCard(state = state, modifier = Modifier.padding(bottom = 24.dp))
        TransactionsButton(onClick = onShowTransactions, modifier = Modifier.padding(bottom = 16.dp))
    }
}


@Preview
@Composable
fun PreviewBudgetDetailsContent() {
    val category = Category(
        id = 1,
        metaData = "",
        title = "Food & Beverage",
        icon = "ic_category_foodndrink",
        type = CategoryType.EXPENSE
    )
    val currency = Currency(
        id = 1,
        currencyName = "United States Dollar",
        currencyCode = "USD"
    )
    val wallet = Wallet(
        id = 1,
        walletName = "Main Wallet",
        currentBalance = 2000.0,
        currency = currency
    )
    val budget = Budget(
        budgetId = 1,
        category = category,
        wallet = wallet,
        amount = 1000000.0,
        fromDate = Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000),
        endDate = Date(System.currentTimeMillis() + 23L * 24 * 60 * 60 * 1000),
        isRepeating = true
    )
    val state = BudgetDetailsState(
        budget = budget,
        transactions = emptyList(),
        recommendedDailySpending = budget.amount / 30,
        projectedSpending = budget.amount,
        actualDailySpending = budget.amount / 20
    )
    BudgetDetailsContent(
        state = state,
        onNavigateBack = {},
        onEditBudget = {},
        onDeleteBudget = {},
        onShowTransactions = {},
        modifier = Modifier
    )
}