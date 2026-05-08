package com.uet.expensetracker.features.money.ui.budgetdetails.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.features.money.ui.budgetdetails.BudgetDetailsState
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.painterResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import com.uet.expensetracker.utils.getDrawableResId

@Composable
fun BudgetDurationCard(
    state: BudgetDetailsState,
    modifier: Modifier = Modifier
) {
    val budget = state.budget ?: return
    val startLocal = budget.fromDate.toInstant()
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val endLocal = budget.endDate.toInstant()
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), endLocal)
        .coerceAtLeast(0)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.cardColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = TextMain,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "${startLocal.format(DateTimeFormatter.ofPattern("dd/MM"))} - " +
                    "${endLocal.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                    fontSize = 16.sp, color = TextMain
                )
                Text(
                    "$daysLeft days left",
                    fontSize = 14.sp, color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = if (state.budget?.wallet?.id == null || state.budget?.wallet?.id == 0) R.drawable.ic_category_all else getDrawableResId(
                        LocalContext.current, state.budget?.wallet?.icon ?: "" )),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(state.budget?.wallet?.walletName ?: "Total", fontSize = 16.sp, color = TextMain)
            }
        }
    }
} 