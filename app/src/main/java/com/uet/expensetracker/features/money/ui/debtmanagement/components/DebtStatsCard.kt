package com.uet.expensetracker.features.money.ui.debtmanagement.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.DebtType
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.utils.formatAmount

@Composable
fun DebtStatsCard(
    debtType: DebtType,
    totalAmount: Double,
    unpaidAmount: Double,
    totalCount: Int,
    unpaidCount: Int,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColor.Light.PrimaryColor.cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (debtType) {
                        DebtType.PAYABLE -> stringResource(R.string.debt_stats_total_payable)
                        DebtType.RECEIVABLE -> stringResource(R.string.debt_stats_total_receivable)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Status indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (debtType) {
                        DebtType.PAYABLE -> MaterialTheme.colorScheme.errorContainer
                        DebtType.RECEIVABLE -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = stringResource(R.string.debt_stats_item_count, totalCount),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (debtType) {
                            DebtType.PAYABLE -> MaterialTheme.colorScheme.onErrorContainer
                            DebtType.RECEIVABLE -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Main stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total amount
                StatItem(
                    title = stringResource(R.string.debt_stats_total_label),
                    amount = totalAmount,
                    currencySymbol = currencySymbol,
                    textColor = when (debtType) {
                        DebtType.PAYABLE -> MaterialTheme.colorScheme.error
                        DebtType.RECEIVABLE -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // Unpaid amount
                StatItem(
                    title = when (debtType) {
                        DebtType.PAYABLE -> stringResource(R.string.debt_stats_unpaid_payable)
                        DebtType.RECEIVABLE -> stringResource(R.string.debt_stats_unpaid_receivable)
                    },
                    amount = unpaidAmount,
                    currencySymbol = currencySymbol,
                    textColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress indicator
            if (totalAmount > 0) {
                val paidAmount = totalAmount - unpaidAmount
                val progressPercentage = (paidAmount / totalAmount).toFloat()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (debtType) {
                                DebtType.PAYABLE -> stringResource(R.string.debt_stats_paid_payable)
                                DebtType.RECEIVABLE -> stringResource(R.string.debt_stats_paid_receivable)
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${(progressPercentage * 100).toInt()}%",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LinearProgressIndicator(
                        progress = progressPercentage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = when (debtType) {
                            DebtType.PAYABLE -> MaterialTheme.colorScheme.primary
                            DebtType.RECEIVABLE -> MaterialTheme.colorScheme.secondary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Text(
                        text = "${formatAmount(paidAmount)} $currencySymbol",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Additional info
            if (unpaidCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (debtType) {
                            DebtType.PAYABLE -> stringResource(R.string.debt_stats_remaining_unpaid_payable, unpaidCount)
                            DebtType.RECEIVABLE -> stringResource(R.string.debt_stats_remaining_unpaid_receivable, unpaidCount)
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    
                    if (totalCount > unpaidCount) {
                        Text(
                            text = stringResource(R.string.debt_stats_completed_count, totalCount - unpaidCount, totalCount),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    amount: Double,
    currencySymbol: String,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatAmount(amount)} $currencySymbol",
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DebtStatsCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DebtStatsCard(
                debtType = DebtType.PAYABLE,
                totalAmount = 5000000.0,
                unpaidAmount = 2000000.0,
                totalCount = 5,
                unpaidCount = 3,
                currencySymbol = "₫"
            )
            
            DebtStatsCard(
                debtType = DebtType.RECEIVABLE,
                totalAmount = 8000000.0,
                unpaidAmount = 1500000.0,
                totalCount = 4,
                unpaidCount = 1,
                currencySymbol = "₫"
            )
        }
    }
} 