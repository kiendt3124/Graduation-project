package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.R

@Composable
fun EmptyTransactionsState(
    monthLabel: String,
    onAddTransactionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Empty state icon
        Image(
            painter = painterResource(id = R.drawable.ic_empty_transactions), // You may need to add this icon
            contentDescription = "No transactions",
            modifier = Modifier.size(120.dp),
            colorFilter = ColorFilter.tint(TextSecondary)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = stringResource(id = R.string.transaction_empty_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = onDarkSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description
        Text(
            text = stringResource(id = R.string.transaction_empty_description, monthLabel),
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Optional: Add transaction button
        OutlinedButton(
            onClick = onAddTransactionClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(0.6f),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Text(
                text = stringResource(id = R.string.transaction_empty_add_button),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyTransactionsStatePreview() {
    MaterialTheme {
        EmptyTransactionsState(
            monthLabel = "Tháng 12/2024"
        )
    }
} 