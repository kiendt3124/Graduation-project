package com.uet.expensetracker.features.money.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.compose.ui.platform.LocalInspectionMode
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.ui.trendingreport.components.TrendingLineChart
import com.uet.expensetracker.features.money.ui.trendingreport.components.TrendingLineChartPreview
import com.uet.expensetracker.features.money.ui.spendingreport.components.SpendingBarChart

import com.uet.expensetracker.ui.theme.AppColor
import java.util.Calendar
import com.uet.expensetracker.ui.theme.TextSecondary
import com.uet.expensetracker.utils.formatAmountWithCurrency

enum class MainTab(@androidx.annotation.StringRes val titleRes: Int) {
    Trending(R.string.home_tab_trending),
    Spending(R.string.home_tab_spending)
}

enum class TrendingSubTab(@androidx.annotation.StringRes val titleRes: Int) {
    TotalSpent(R.string.home_tab_total_spent),
    TotalIncome(R.string.home_tab_total_income)
}

enum class SpendingSubTab(@androidx.annotation.StringRes val titleRes: Int) {
    Week(R.string.home_tab_week),
    Month(R.string.home_tab_month)
}

@Composable
fun SpendingSummary(
    currencySymbol: String,
    current: Double,
    previous: Double,
    @androidx.annotation.StringRes periodRes: Int
) {
    val changePercent = if (previous != 0.0) (current - previous) / previous * 100 else 0.0
    val icon = when {
        changePercent > 0 -> Icons.Default.KeyboardArrowUp
        changePercent < 0 -> Icons.Default.KeyboardArrowDown
        else -> Icons.AutoMirrored.Outlined.Send
    }
    val color = when {
        changePercent > 0 -> AppColor.Light.InflowAmountColor
        changePercent < 0 -> AppColor.Light.OutflowAmountColor
        else -> Color.Black
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatAmountWithCurrency(current, currencySymbol),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_total_spent_period, stringResource(periodRes)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${String.format(java.util.Locale.US, "%.1f", abs(changePercent))}%",
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun HomeReportTabs(
    selectedMainTab: MainTab,
    onSelectMainTab: (MainTab) -> Unit,
    selectedTrendingTab: TrendingSubTab,
    onSelectTrendingTab: (TrendingSubTab) -> Unit,
    selectedSpendingTab: SpendingSubTab,
    onSelectSpendingTab: (SpendingSubTab) -> Unit,
    transactions: List<Transaction>,
    totalSpent: Double,
    totalIncome: Double,
    currentSpent: Double,
    previousSpent: Double,
    currentMonthSpent: Double,
    previousMonthSpent: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-tabs based on selected main tab
        when (selectedMainTab) {
            MainTab.Trending -> TabRow(
                selectedTabIndex = selectedTrendingTab.ordinal,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTrendingTab.ordinal]),
                        color = if (selectedTrendingTab == TrendingSubTab.TotalSpent)
                            AppColor.Light.OutflowAmountColor else AppColor.Light.InflowAmountColor
                    )
                }
            ) {
                TrendingSubTab.values().forEach { subTab ->
                    Tab(
                        selected = subTab == selectedTrendingTab,
                        onClick = { onSelectTrendingTab(subTab) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stringResource(subTab.titleRes),
                                    color = Color(0xFF505D6D)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val value = if (subTab == TrendingSubTab.TotalSpent) totalSpent else totalIncome
                                val color = if (subTab == TrendingSubTab.TotalSpent)
                                    AppColor.Light.OutflowAmountColor else AppColor.Light.InflowAmountColor
                                Text(
                                    text = formatAmountWithCurrency(value, currencySymbol),
                                    color = color
                                )
                            }
                        }
                    )
                }
            }

            MainTab.Spending -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    SpendingSubTab.values().forEach { subTab ->
                        val isSelected = subTab == selectedSpendingTab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clickable { onSelectSpendingTab(subTab) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = if (isSelected) 0.3f else 0.0f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = stringResource(subTab.titleRes),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                // Summary under spending tabs
                val (curr, prev, periodRes) = if (selectedSpendingTab == SpendingSubTab.Week) {
                    Triple(currentSpent, previousSpent, R.string.home_tab_week)
                } else {
                    Triple(currentMonthSpent, previousMonthSpent, R.string.home_tab_month)
                }
                SpendingSummary(
                    currencySymbol = currencySymbol,
                    current = curr,
                    previous = prev,
                    periodRes = periodRes
                )
            }
        }
        // Chart area
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            val isPreview = LocalInspectionMode.current
            if (selectedMainTab == MainTab.Trending) {
                if (isPreview) {
                    TrendingLineChartPreview()
                } else {
                    val filteredTx = if (selectedTrendingTab == TrendingSubTab.TotalSpent)
                        transactions.filter { it.transactionType == TransactionType.OUTFLOW }
                    else
                        transactions.filter { it.transactionType == TransactionType.INFLOW }
                    if (filteredTx.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_no_transactions),
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        TrendingLineChart(
                            transactions = filteredTx,
                            isIncome = (selectedTrendingTab == TrendingSubTab.TotalIncome),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                SpendingBarChart(
                    lastAmount = previousSpent,
                    thisAmount = currentSpent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Main tab selector with icons
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        val newTab = if (selectedMainTab == MainTab.Trending) MainTab.Spending else MainTab.Trending
                        onSelectMainTab(newTab)
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.home_nav_previous),
                        tint = Color(0xFF1E2A36)
                    )
                }
                Text(
                    text = stringResource(selectedMainTab.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1E2A36),
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = {
                        val newTab = if (selectedMainTab == MainTab.Trending) MainTab.Spending else MainTab.Trending
                        onSelectMainTab(newTab)
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.home_nav_next),
                        tint = Color(0xFF1E2A36)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                MainTab.values().forEachIndexed { index, tab ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (tab == selectedMainTab) Color(0xFF94A1B1)
                                else Color(0xFFE4E7EC)
                            )
                    )
                    if (index < MainTab.values().size - 1) Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeReportTabsPreview() {
    ExpenseTrackerTheme {
        // Create sample wallet for transactions
        val sampleWallet = Wallet(
            id = 1,
            walletName = "Cash",
            currentBalance = 10000000.0,
            currency = Currency(
                id = 1,
                currencyName = "Vietnamese Dong",
                currencyCode = "VND",
                symbol = "₫"
            )
        )

        // Create sample category
        val sampleCategory = Category(
            id = 1,
            metaData = "food",
            title = "Food & Beverages",
            icon = "ic_food",
            type = CategoryType.EXPENSE
        )

        // Generate 15 days of sample transactions
        val sampleTransactions = buildList {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -10) // Start 14 days ago

            repeat(15) { day ->
                // Add 1-3 transactions per day
                repeat((1..3).random()) {
                    add(
                        Transaction(
                            id = day * 3 + it,
                            wallet = sampleWallet,
                            transactionType = if (Math.random() > 0.7) TransactionType.INFLOW else TransactionType.OUTFLOW,
                            amount = (100000..1000000).random().toDouble(),
                            transactionDate = calendar.time,
                            description = "Transaction ${day * 3 + it}",
                            category = sampleCategory
                        )
                    )
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        HomeReportTabs(
            selectedMainTab = MainTab.Trending,
            onSelectMainTab = {},
            selectedTrendingTab = TrendingSubTab.TotalSpent,
            onSelectTrendingTab = {},
            selectedSpendingTab = SpendingSubTab.Week,
            onSelectSpendingTab = {},
            transactions = sampleTransactions,
            totalSpent = 1234.56,
            totalIncome = 7890.12,
            currentSpent = 150.0,
            previousSpent = 100.0,
            currentMonthSpent = 1000.0,
            previousMonthSpent = 1200.0,
            currencySymbol = "$",
            modifier = Modifier.fillMaxSize()
        )
    }
} 