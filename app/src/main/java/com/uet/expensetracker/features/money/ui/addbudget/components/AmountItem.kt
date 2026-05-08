package com.uet.expensetracker.features.money.ui.addbudget.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Preview(showBackground = true)
@Composable
fun AmountItem(
    amount: Double,
    amountText: String = "",
    displayExpression: String = "",
    currencyCode: String,
    onClick: () -> Unit
) {
    // Show expression if it exists and not just "0", otherwise show amount
    val shouldShowExpression = displayExpression.isNotEmpty() && displayExpression != "0" && displayExpression != amountText
    
    val displayText = if (shouldShowExpression) {
        displayExpression
    } else if (amountText.isNotEmpty()) {
        amountText
    } else {
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        numberFormat.format(amount)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = stringResource(R.string.add_budget_amount_label),
            color = TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = currencyCode,
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = displayText,
                color = TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
} 