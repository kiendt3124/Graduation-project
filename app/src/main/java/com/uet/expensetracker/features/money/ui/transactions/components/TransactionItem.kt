package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.utils.formatCurrency
import com.uet.expensetracker.utils.getDrawableResId
import com.uet.expensetracker.utils.getStringResId
import java.util.Calendar

@Composable
fun TransactionItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: ((Transaction) -> Unit)? = null,
    showWalletIndicator: Boolean = false
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INFLOW -> AppColor.Light.IncomeAmountColor
        TransactionType.OUTFLOW -> AppColor.Light.ExpenseAmountColor
    }
    val iconBackgroundColor = getIconBackgroundColor(transaction.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke(transaction) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(getDrawableResId(LocalContext.current,  transaction.category.icon)),
                contentDescription = transaction.category.metaData,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Category and Description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = getStringResId(LocalContext.current, transaction.category.title)),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextMain
                )
                
                // Show wallet indicator when in total wallet view
                if (showWalletIndicator) {
                    Spacer(modifier = Modifier.width(8.dp))
                    WalletChip(walletName = transaction.wallet.walletName)
                }
            }
            
            transaction.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Amount
        Text(
            text = formatCurrency(transaction.amount),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor
        )
    }
}

@Composable
fun WalletChip(walletName: String) {
    Surface(
        modifier = Modifier.height(20.dp),
        color = Color(0xFF2C2C2C),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = Color(0xFFFFA726),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = walletName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

fun getCategoryIcon(category: Category): ImageVector {
    return when (category.metaData.lowercase()) {
        "loan" -> Icons.Filled.Favorite
        "food_beverage" -> Icons.Filled.Favorite
        "salary" -> Icons.Filled.Favorite
        "groceries" -> Icons.Filled.ShoppingCart
        "transport" -> Icons.Filled.Favorite
        "entertainment" -> Icons.Filled.Favorite
        else -> Icons.Filled.Favorite
    }
}

fun getIconBackgroundColor(category: Category): Color {
    return when (category.metaData.lowercase()) {
        "loan" -> AppColor.Light.CategoryIconBackgroundLoan
        "food_beverage" -> AppColor.Light.CategoryIconBackgroundFood
        else -> Color.Gray
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun TransactionItemPreviewExpense() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.APRIL, 17)
    val previewDate = calendar.time

    val currency = Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫"
    )

    val wallet = Wallet(
        id = 1,
        walletName = "Tiền Tài Khoản",
        currentBalance = 249188.0,
        currency = currency
    )

    ExpenseTrackerTheme {
        TransactionItem(
            transaction = Transaction(
                id = 1,
                wallet = wallet,
                transactionType = TransactionType.OUTFLOW,
                amount = 99000.0,
                transactionDate = previewDate,
                description = "Cho ô toàn cty vay to s...",
                category = Category(
                    id = 1,
                    metaData = "loan",
                    title = "cate_food",
                    icon = "ic_category_foodndrink",
                    type = CategoryType.DEBT_LOAN
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun TransactionItemPreviewWithWallet() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.APRIL, 17)
    val previewDate = calendar.time

    val currency = Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫"
    )

    val wallet = Wallet(
        id = 1,
        walletName = "Tiền Tài Khoản",
        currentBalance = 249188.0,
        currency = currency
    )

    ExpenseTrackerTheme {
        TransactionItem(
            transaction = Transaction(
                id = 1,
                wallet = wallet,
                transactionType = TransactionType.OUTFLOW,
                amount = 99000.0,
                transactionDate = previewDate,
                description = "Cho ô toàn cty vay to s...",
                category = Category(
                    id = 1,
                    metaData = "loan",
                    title = "cate_food",
                    icon = "ic_category_foodndrink",
                    type = CategoryType.DEBT_LOAN
                )
            ),
            showWalletIndicator = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun TransactionItemPreviewIncome() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.APRIL, 16)
    val previewDate = calendar.time

    val currency = Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫"
    )

    val wallet = Wallet(
        id = 1,
        walletName = "Tiền Tài Khoản",
        currentBalance = 249188.0,
        currency = currency
    )

    ExpenseTrackerTheme {
        TransactionItem(
            transaction = Transaction(
                id = 2,
                wallet = wallet,
                transactionType = TransactionType.INFLOW,
                amount = 1500000.0,
                transactionDate = previewDate,
                description = "Monthly Salary",
                category = Category(
                    id = 2,
                    metaData = "salary",
                    title = "cate_food",
                    icon = "ic_category_foodndrink",
                    type = CategoryType.INCOME
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun TransactionItemPreviewFood() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.APRIL, 17)
    val previewDate = calendar.time

    val currency = Currency(
        id = 1,
        currencyName = "Vietnamese Dong",
        currencyCode = "VND",
        symbol = "₫"
    )

    val wallet = Wallet(
        id = 1,
        walletName = "Tiền Tài Khoản",
        currentBalance = 249188.0,
        currency = currency
    )

    ExpenseTrackerTheme {
        TransactionItem(
            transaction = Transaction(
                id = 3,
                wallet = wallet,
                transactionType = TransactionType.OUTFLOW,
                amount = 44000.0,
                transactionDate = previewDate,
                description = "Adjust Balance",
                category = Category(
                    id = 3,
                    metaData = "food_beverage",
                    title = "cate_food",
                    icon = "ic_category_foodndrink",
                    type = CategoryType.EXPENSE
                )
            )
        )
    }
}