@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextAccent
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.ui.budgets.transactions.components.DailyTransactionsCard
import com.uet.expensetracker.features.money.ui.budgets.transactions.components.EmptyBudgetTransactionsContent
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.ui.transactions.getPlaceholderTransactions
import com.uet.expensetracker.utils.getStringResId

@Composable
fun BudgetTransactionsScreenContent(
    state: BudgetTransactionsState,
    onBackClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(id = getStringResId(LocalContext.current, state.category.title)), 
                        color = TextMain
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.budgets_back_cd), tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColor.Light.PrimaryColor.containerColor,
                    titleContentColor = AppColor.Light.PrimaryColor.contentColor
                )
            )
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor,
        modifier = modifier
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = TextAccent
                    )
                }
            }
            state.groupedTransactions.isEmpty() -> {
                EmptyBudgetTransactionsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColor.Light.PrimaryColor.containerColor)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    state.groupedTransactions.forEach { dailyData ->
                        item(key = "daily-${dailyData.date.time}") {
                            DailyTransactionsCard(
                                dailyData = dailyData,
                                onTransactionClick = onTransactionClick,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColor.Light.PrimaryColor.cardColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun BudgetTransactionsScreenContentPreview() {
    ExpenseTrackerTheme {
        // Sample category
        val sampleCategory = Category(
            id = 2,
            metaData = "food_beverage",
            title = "Food & Beverage",
            icon = "ic_category_foodndrink",
            type = CategoryType.EXPENSE
        )
        // Sample data
        val sampleTransactions = getPlaceholderTransactions()
        val state = BudgetTransactionsState(
            category = sampleCategory,
            groupedTransactions = sampleTransactions,
            isLoading = false,
            budgetId = 1,
            error = null
        )
        BudgetTransactionsScreenContent(
            state = state,
            onBackClick = {},
            onTransactionClick = {},
            modifier = Modifier
        )
    }
} 