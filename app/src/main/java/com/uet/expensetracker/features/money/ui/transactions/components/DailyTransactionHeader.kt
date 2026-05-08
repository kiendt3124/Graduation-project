package com.uet.expensetracker.features.money.ui.transactions.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

@Composable
fun DailyTransactionHeader(date: Date, dailyTotal: Double) {
    val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())
    val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
    val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dayFormatter.format(date),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                color = TextMain,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = dayOfWeekFormatter.format(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMain,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = monthYearFormatter.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Text(
            text = formatCurrency(dailyTotal),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextMain
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun DailyTransactionHeaderPreview() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.APRIL, 17)
    val previewDate = calendar.time

    ExpenseTrackerTheme {
        DailyTransactionHeader(date = previewDate, dailyTotal = -99000.0)
    }
}