package com.uet.expensetracker.features.money.ui.addtransaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import java.text.NumberFormat
import java.util.Locale

// Define colors
val AmountColor = Color(0xFF4CD964) // Green color for amount

// Display-only amount row
@Composable
fun DisplayAmountRow(
    modifier: Modifier = Modifier,
    currency: String,
    amount: Double,
    amountText: String = "",
    displayExpression: String = "",
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xFFE4E7EC)
            ),
        ) {
            Text(
                text = currency,
                color = Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Color(0xFFE4E7EC))
            )
        }


        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.add_transaction_amount_label),
                color = Color(0xFF505D6D),
                fontSize = 12.sp,
            )
            Text(
                text = displayText,
                color = Color(0xFF1E2A36),
                fontSize = 24.sp,
            )
        }

    }
}