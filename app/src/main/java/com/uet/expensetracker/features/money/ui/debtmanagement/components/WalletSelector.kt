package com.uet.expensetracker.features.money.ui.debtmanagement.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.utils.formatAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSelector(
    wallets: List<Wallet>,
    selectedWallet: Wallet?,
    onWalletSelected: (Wallet?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 32.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.outline
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.debt_wallet_selector_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            // Wallet list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All wallets option
                item {
                    WalletItem(
                        wallet = null,
                        isSelected = selectedWallet == null,
                        onSelected = { onWalletSelected(null) },
                        isAllWalletsOption = true
                    )
                }
                
                // Individual wallets
                items(wallets) { wallet ->
                    WalletItem(
                        wallet = wallet,
                        isSelected = selectedWallet?.id == wallet.id,
                        onSelected = { onWalletSelected(wallet) }
                    )
                }
            }
            
            // Bottom spacing for safe area
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletItem(
    wallet: Wallet?,
    isSelected: Boolean,
    onSelected: () -> Unit,
    isAllWalletsOption: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onSelected,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                AppColor.Light.PrimaryColor.cardColor
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                ),
                width = 2.dp
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Wallet icon
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAllWalletsOption) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAllWalletsOption) {
                                Icons.Default.AccountBox
                            } else {
                                Icons.Default.AccountBox
                            },
                            contentDescription = null,
                            tint = if (isAllWalletsOption) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Wallet info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isAllWalletsOption) {
                            stringResource(R.string.debt_wallet_selector_all_wallets)
                        } else {
                            wallet?.walletName ?: stringResource(R.string.debt_wallet_selector_unknown_wallet)
                        },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!isAllWalletsOption && wallet != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatAmount(wallet?.currentBalance ?: 0.0)} ${wallet.currency.symbol}",
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                TextSecondary
                            },
                            fontSize = 12.sp
                        )
                    } else if (isAllWalletsOption) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.debt_wallet_selector_view_all_debts),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                TextSecondary
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.debt_wallet_selector_selected_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
