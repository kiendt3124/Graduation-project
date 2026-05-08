package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.features.money.ui.transactions.MonthItem
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextPrimary
import com.uet.expensetracker.ui.theme.TextSecondary

@Composable
fun MonthSelectionTabs(
    months: List<MonthItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = AppColor.Light.PrimaryColor.contentColor,
        edgePadding = 16.dp, // Padding on the edges
        indicator = { tabPositions ->
            if (selectedIndex in tabPositions.indices) {
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = AppColor.Light.TabIndicatorColor
                )
            }
        }
    ) {
        months.forEachIndexed { index, monthItem ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = monthItem.label,
                        color = if (selectedIndex == index) TextMain else Color(0xFF505D6D)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun MonthSelectionTabsPreview() {
    // Create sample months for preview
    val sampleMonths = MonthItem.buildMonthItems(monthsBack = 6) // Shorter list for preview
    val thisMonthIndex = MonthItem.findThisMonthIndex(sampleMonths)
    var selectedTab by remember { mutableStateOf(thisMonthIndex) }
    
    ExpenseTrackerTheme {
        MonthSelectionTabs(
            months = sampleMonths,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}
