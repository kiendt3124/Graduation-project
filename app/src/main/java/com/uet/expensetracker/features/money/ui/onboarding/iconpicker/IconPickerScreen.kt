package com.uet.expensetracker.features.money.ui.onboarding.iconpicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import com.uet.expensetracker.utils.Constants

@Composable
fun IconPickerScreen(
    navController: NavController
) {
    // List of available icon resource IDs
    val icons = listOf(
        R.drawable.ic_category_cash,
        R.drawable.icon_wallet_bank,
        R.drawable.icon_wallet_family,
        R.drawable.ic_credit_wallet,
        R.drawable.ic_lock_wallet,
        R.drawable.ic_category_award,
        R.drawable.ic_category_debt,
        R.drawable.ic_category_friendnlover,
        R.drawable.ic_category_education,
        R.drawable.ic_category_donations,
        R.drawable.ic_category_family,
        R.drawable.ic_category_loan,
        // TODO: add more icon resources here
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            items(icons) { iconRes ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            // Pass selected icon back to previous screen
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Constants.SELECTED_ICON_KEY, iconRes)
                            navController.popBackStack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IconPickerScreenPreview() {
    ExpenseTrackerTheme {
        val navController = rememberNavController()
        IconPickerScreen(navController = navController)
    }
} 