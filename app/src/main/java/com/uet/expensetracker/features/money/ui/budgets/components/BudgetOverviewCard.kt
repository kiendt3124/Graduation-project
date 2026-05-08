@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary

/**
 * Budget overview card component
 * Displays a card with circular progress indicator showing spending,
 * stats row showing totals, and a create budget button
 *
 * @param totalAmount Total budget amount to display
 * @param spentAmount Amount spent so far
 * @param progress Progress value between 0-1 representing percentage spent
 * @param totalBudgetsText Text to display for total budgets
 * @param totalSpentText Text to display for total spent
 * @param daysLeftText Text to display for days remaining
 * @param onCreateBudgetClick Callback when Create Budget button is clicked
 */
@Composable
fun BudgetOverviewCard(
    totalAmount: String = "2,000,000",
    spentAmount: String = "700,000",
    progress: Float = 0.35f,
    totalBudgetsText: String = "2 M",
    totalSpentText: String = "0",
    daysLeftText: String = "19 days",
    onCreateBudgetClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColor.Light.PrimaryColor.cardColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Circular progress with amount
            Box(
                contentAlignment = Alignment.Center,
            ) {
                // Enhanced circular progress indicator
                StraightProgressIndicator(
                    progress = progress,
                    totalAmount = totalAmount,
                    spentAmount = spentAmount,
                    backgroundColor = Color(0xFFE4E7EC),
                    foregroundColor = Color(0xFFFCA419),
                    showAnimation = true
                )
            }
            
            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Total budgets
                StatColumn(
                    value = totalBudgetsText,
                    label = stringResource(R.string.budgets_total_budgets_label)
                )
                
                // Total spent
                StatColumn(
                    value = totalSpentText,
                    label = stringResource(R.string.budgets_total_spent_label)
                )
                
                // End of month
                StatColumn(
                    value = daysLeftText,
                    label = stringResource(R.string.budgets_end_of_month_label)
                )
            }
            
            // Create Budget button
            Button(
                onClick = onCreateBudgetClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor)
            ) {
                Text(
                    stringResource(R.string.budgets_create_budget_button),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Stat column used in the stats row
 *
 * @param value The statistic value to display
 * @param label The label for the statistic
 */
@Composable
private fun StatColumn(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
} 