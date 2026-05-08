package com.uet.expensetracker.features.money.ui.budgetdetails.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsTopAppBar(
    onNavigateBack: () -> Unit,
    onEditBudget: () -> Unit,
    onDeleteBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(R.string.budget_details_title), color = TextMain, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.budget_details_back_cd), tint = TextMain)
            }
        },
        actions = {
            IconButton(onClick = onEditBudget) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.budget_details_edit_cd), tint = TextMain)
            }
            IconButton(onClick = onDeleteBudget) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Budget", tint = TextMain)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            titleContentColor = AppColor.Light.PrimaryColor.contentColor
        ),
        modifier = modifier
    )
} 