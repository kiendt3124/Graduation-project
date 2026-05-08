@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary

enum class BudgetAlertType {
    WARNING,
    EXCEEDED,
    EXPIRING
}

@Composable
fun BudgetItem(
    modifier: Modifier = Modifier,
    categoryIcon: Int,
    categoryName: String,
    amount: String,
    leftAmount: String,
    progress: Float,
    alertType: BudgetAlertType? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColor.Light.PrimaryColor.cardColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = categoryIcon),
                        contentDescription = categoryName,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMain
                    )
                }

                // Alert badge
                alertType?.let {
                    AlertBadge(alertType = it)
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Amount
                Text(
                    text = amount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Left amount text
            Text(
                text = stringResource(R.string.budgets_left_label, leftAmount),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar with dynamic color based on progress and alert
            val progressColor = when {
                alertType == BudgetAlertType.EXCEEDED || progress >= 1.0f -> Color(0xFFF05252) // Red
                alertType == BudgetAlertType.WARNING || progress >= 0.95f -> Color(0xFFFF9800) // Orange
                progress >= 0.90f -> Color(0xFFFFC107) // Amber
                progress >= 0.80f -> Color(0xFFFCA419) // Yellow
                else -> Color(0xFF4CAF50) // Green
            }
            
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = progressColor,
                trackColor = Color(0xFFE4E7EC),
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Today text
            Text(
                text = stringResource(R.string.budgets_today),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun AlertBadge(alertType: BudgetAlertType) {
    val (badgeColor, badgeText) = when (alertType) {
        BudgetAlertType.WARNING -> Color(0xFFFFC107) to stringResource(R.string.budget_alert_badge_warning)
        BudgetAlertType.EXCEEDED -> Color(0xFFF05252) to stringResource(R.string.budget_alert_badge_exceeded)
        BudgetAlertType.EXPIRING -> Color(0xFFFF9800) to stringResource(R.string.budget_alert_badge_expiring)
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
    }
} 