package com.uet.expensetracker.features.money.ui.transfermoney.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.uet.expensetracker.R
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.utils.formatAmount
import com.uet.expensetracker.utils.getDrawableResId
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransferTopBar(
    onSaveClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColor.Light.PrimaryColor.containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.transfer_money_close_cd),
                    tint = Color(0xFF1E2A36),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = stringResource(R.string.transfer_money_title),
                color = Color(0xFF1E2A36),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
            
            TextButton(
                onClick = onSaveClick,
                modifier = Modifier.align(Alignment.CenterEnd),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFCA419)
                )
            ) {
                Text(stringResource(R.string.transfer_money_save_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountTextField(
    amount: Double,
    currencySymbol: String,
    readOnly: Boolean = false,
    onAmountChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val textValue = if (amount > 0) formatAmount(amount) else ""
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            val numericValue = newValue.replace(Regex("[^0-9]"), "")
            val doubleValue = if (numericValue.isNotEmpty()) numericValue.toDouble() else 0.0
            onAmountChange(doubleValue)
        },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("0") },
        colors = OutlinedTextFieldDefaults.colors(
           focusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedContainerColor = Color.Transparent,
            unfocusedBorderColor = Color.DarkGray,
            focusedBorderColor = Color.White,
            focusedPlaceholderColor = Color.Gray
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            Text(
                text = currencySymbol,
                color = Color.White,
                modifier = Modifier.padding(end = 16.dp)
            )
        },
        singleLine = true,
        readOnly = readOnly
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = Color(0xFF505D6D),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun TransferItem(
    icon: ImageVector,
    painter: Painter? = null,
    title: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            painter?.let {
                Image(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = iconTint?.let { ColorFilter.tint(it) }
                )
            } ?: run {
                // Fallback to vector icon if painter is not provided
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint ?: Color.Unspecified
                )
            }

            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                color = TextMain,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            if (value != null) {
                Text(
                    text = value,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(1.dp))
}

@Composable
fun WalletSelector(
    wallets: List<Wallet>,
    selectedWallet: Wallet?,
    painter: Painter? = null,
    modifier: Modifier = Modifier,
    onWalletSelected: (Int) -> Unit
) {
    val walletName = selectedWallet?.walletName ?: "Select Wallet"
    
    TransferItem(
        icon = Icons.Default.Email,
        painter = painterResource(id = getDrawableResId(LocalContext.current, selectedWallet?.icon ?: "ic_wallet")),
        title = walletName,
        modifier = modifier,
        onClick = {
            // Instead of directly selecting a wallet, invoke the callback
            // which will navigate to the ChooseWalletScreen
            onWalletSelected(-1) // Just a trigger to navigate, value doesn't matter
        }
    )
}

@Composable
fun DateSelector(
    date: Date,
    icon: Painter? = null,
    onDateChange: (Date) -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val displayValue = if (isToday(date)) stringResource(R.string.transfer_money_today) else dateFormat.format(date)
    
    TransferItem(
        icon = Icons.Default.DateRange,
        painter = icon,
        title = stringResource(R.string.transfer_money_today),
        value = displayValue,
        iconTint = iconTint,
        onClick = {
            // In a real app, show a date picker dialog here
            // For now, we'll just use the current date
        },
        modifier = modifier
    )
}

@Composable
fun ToggleOption(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextMain,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CD964),
                checkedTrackColor = Color(0xFF4CD964).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

// Helper function to check if a date is today
private fun isToday(date: Date): Boolean {
    val calendar1 = Calendar.getInstance()
    val calendar2 = Calendar.getInstance()
    calendar2.time = date
    
    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
           calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
} 