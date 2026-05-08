package com.uet.expensetracker.features.money.ui.budgetdetails.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.ui.budgetdetails.BudgetDetailsState
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.utils.formatAmountWithCurrency
import com.uet.expensetracker.utils.getStringResId
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetSummaryCard(
    state: BudgetDetailsState,
    modifier: Modifier = Modifier
) {
    val budget = state.budget ?: return
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColor.Light.PrimaryColor.cardColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconRes = context.resources.getIdentifier(
                    budget.category.icon, "drawable", context.packageName
                )
                Image(
                    painter = painterResource(
                        id = if (iconRes != 0) iconRes else R.drawable.ic_category_foodndrink
                    ),
                    contentDescription = budget.category.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
                Text(
                    text = budget.category.title?.let {
                        stringResource(id = getStringResId(context, it))
                    } ?: run {
                        stringResource(R.string.budget_details_select_category)
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }
            Spacer(modifier = Modifier.height(8.dp))


            Text(
                text = formatAmountWithCurrency(budget.amount, budget.wallet.currency.symbol),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.budget_details_spent_label), fontSize = 14.sp, color = TextSecondary)
                    Text(
                        formatAmountWithCurrency(state.spentAmount, budget.wallet.currency.symbol),
                        fontSize = 18.sp,
                        color = TextMain,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.budget_details_left_label), fontSize = 14.sp, color = TextSecondary)
                    Text(
                        formatAmountWithCurrency(state.remainingBudget, budget.wallet.currency.symbol),
                        fontSize = 18.sp,
                        color = TextMain,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val progress = if (budget.amount > 0) (state.spentAmount / budget.amount).toFloat() else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color.Gray.copy(alpha = 0.3f)
            )
            Text(
                stringResource(R.string.budget_details_today),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
} 