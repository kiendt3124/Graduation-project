package com.uet.expensetracker.features.money.ui.budgets.transactions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.features.money.ui.transactions.DailyTransactions
import com.uet.expensetracker.features.money.ui.transactions.components.DailyTransactionHeader
import com.uet.expensetracker.features.money.ui.transactions.components.TransactionItem
import com.uet.expensetracker.features.money.domain.model.Transaction

@Composable
fun DailyTransactionsCard(
    dailyData: DailyTransactions,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColor.Light.PrimaryColor.cardColor
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with date and total
            DailyTransactionHeader(
                date = dailyData.date,
                dailyTotal = dailyData.dailyTotal
            )

            // Divider between header and items
            HorizontalDivider(
                color = AppColor.Light.DividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Transactions list
            dailyData.transactions.forEach { transaction ->
                TransactionItem(
                    transaction = transaction,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = onTransactionClick,
                    showWalletIndicator = false
                )
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
} 