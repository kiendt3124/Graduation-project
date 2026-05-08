package com.uet.expensetracker.features.money.ui.addtransaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AmountSuggestionBar(
    suggestedAmounts: List<String> = listOf(
        "500,000.0",
        "2,000,000.0",
        "30,000.0",
        "100,000.0",
        "5000.0"
    ),
    onSuggestionClick: (String) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(suggestedAmounts) { suggestion ->
            Button(
                onClick = { onSuggestionClick(suggestion) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                ),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {
                Text(text = suggestion, color = Color.White)
            }
        }
    }
}