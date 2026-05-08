package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.* // Import colors
import com.uet.expensetracker.utils.formatCurrency // Import formatter

@Composable
fun SummarySection(
    inflow: Double,
    outflow: Double,
    modifier: Modifier = Modifier
) {
    val total = inflow - outflow

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SummaryRow(
            label = stringResource(id = R.string.transaction_summary_inflow),
            amount = inflow,
            color = AppColor.Light.InflowAmountColor
        )
        SummaryRow(
            label = stringResource(id = R.string.transaction_summary_outflow),
            amount = outflow,
            color = AppColor.Light.OutflowAmountColor
        )
        HorizontalDivider(
            modifier = Modifier
                .height(1.dp),
            color = Color(0x14000000),
        )
        SummaryRow(
            label = stringResource(id = R.string.transaction_summary_total),
            amount = total,
            color = TextMain,
            isTotal = true,
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Double,
    color: Color,
    isTotal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Text(
            text = formatCurrency(amount),
            style = if (isTotal) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun SummarySectionPreview() {
    ExpenseTrackerTheme {
        SummarySection(inflow = 3955237.0, outflow = 4658173.0)
    }
}
