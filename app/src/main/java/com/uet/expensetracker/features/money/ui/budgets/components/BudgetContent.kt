@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.utils.getStringResId
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Main content for the Budget screen when budgets exist
 *
 * @param paddingValues PaddingValues from the parent Scaffold
 * @param transactions List of transactions
 * @param totalBudgetAmount Total budget amount
 * @param totalSpentAmount Total spent amount
 * @param currentWallet Current wallet
 * @param onCreateBudgetClick Callback when the Create Budget button is clicked
 * @param onDeleteBudget Callback when a budget is deleted
 */
@Composable
fun BudgetContent(
    paddingValues: PaddingValues,
    transactions: List<Transaction> = emptyList(),
    totalBudgetAmount: Double = 0.0,
    totalSpentAmount: Double = 0.0,
    currentWallet: Wallet? = null,
    onCreateBudgetClick: () -> Unit,
    onDeleteBudget: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Main content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Budget Summary Card
            item {
                BudgetSummaryCard(
                    totalBudgetAmount = totalBudgetAmount,
                    totalSpentAmount = totalSpentAmount,
                    currencySymbol = currentWallet?.currency?.symbol ?: "$"
                )
            }
            
            // Transaction List Header
            item {
                Text(
                    text = "Recent Transactions",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Transaction Items
            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions yet",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onDelete = { onDeleteBudget(transaction.id) }
                    )
                }
            }
        }
        
        // Add Budget FAB
        FloatingActionButton(
            onClick = onCreateBudgetClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            contentColor = AppColor.Light.PrimaryColor.contentColor
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Budget"
            )
        }
    }
}

@Composable
fun BudgetSummaryCard(
    totalBudgetAmount: Double,
    totalSpentAmount: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Budget Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Budget Progress
            val progress = if (totalBudgetAmount > 0)
                (totalSpentAmount / totalBudgetAmount).coerceIn(0.0, 1.0).toFloat()
            else 0f

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    progress < 0.7f -> Color.Green
                    progress < 0.9f -> Color(0xFFFFA500) // Orange
                    else -> Color.Red
                },
                trackColor = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Budget Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: $currencySymbol$totalSpentAmount",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Text(
                    text = "Total: $currencySymbol$totalBudgetAmount",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remaining Amount
            val remainingAmount = totalBudgetAmount - totalSpentAmount
            Text(
                text = "Remaining: $currencySymbol$remainingAmount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    remainingAmount > totalBudgetAmount * 0.3 -> Color.Green
                    remainingAmount > 0 -> Color(0xFFFFA500) // Orange
                    else -> Color.Red
                }
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category icon and name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3F51B5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(getStringResId(LocalContext.current, transaction.category.title)),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text =  stringResource(getStringResId(LocalContext.current, transaction.category.title)),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = dateFormatter.format(transaction.transactionDate),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Amount and delete
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transaction.wallet.currency.symbol}${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.amount < 0) Color.Red else Color.Green
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
} 