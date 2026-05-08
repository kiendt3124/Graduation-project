package com.uet.expensetracker.features.money.ui.addtransaction.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionTopAppBar(
    title: String,
    onCloseClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
        navigationIcon = {
            TextButton (onClick = onCloseClick, colors = ButtonDefaults.textButtonColors(
                contentColor = AppColor.Light.PrimaryColor.contentColor,
                containerColor = Color.Transparent
            )) {
                Text(
                    text = stringResource(id = R.string.add_transaction_action_cancel),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF2B3B48)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            titleContentColor = AppColor.Light.PrimaryColor.contentColor,
            navigationIconContentColor = AppColor.Light.PrimaryColor.contentColor
        )
    )
}