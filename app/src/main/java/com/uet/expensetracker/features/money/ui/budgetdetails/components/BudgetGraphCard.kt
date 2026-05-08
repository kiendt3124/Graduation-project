package com.uet.expensetracker.features.money.ui.budgetdetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.features.money.ui.budgetdetails.BudgetDetailsState
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import com.uet.expensetracker.features.money.ui.budgetdetails.components.BudgetLineChart
import com.uet.expensetracker.utils.formatAmountWithCurrency
import com.uet.expensetracker.ui.theme.AppColor
import java.util.Date

@Composable
fun BudgetGraphCard(
    state: BudgetDetailsState,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.cardColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BudgetLineChart(
                graphData   = state.graphData,
                budgetLimit = state.budget?.amount ?: 0.0,
                budgetStart = state.budget?.fromDate ?: Date(),
                budgetEnd   = state.budget?.endDate   ?: Date(),
                modifier    = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                InfoRow(
                    label = "Đề xuất chi tiêu hàng ngày",
                    value = formatAmountWithCurrency(state.recommendedDailySpending, state.budget?.wallet?.currency?.symbol ?: "USD")
                )
                InfoRow(
                    "Chi tiêu dự kiến",
                    formatAmountWithCurrency(state.projectedSpending, state.budget?.wallet?.currency?.symbol ?: "USD"))
//                InfoRow("Chi tiêu thực tế hàng ngày",
//                    formatAmountWithCurrency(state.actualDailySpending, state.budget?.wallet?.currency?.symbol ?: "USD"))
            }
        }
    }
} 