package com.uet.expensetracker.features.money.ui.budgetdetails.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor

@Composable
fun TransactionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor)
    ) {
        Text(stringResource(R.string.budget_details_transactions_list_button), color = Color.White, fontWeight = FontWeight.Bold)
    }
} 