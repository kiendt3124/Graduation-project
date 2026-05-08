package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.*
import com.uet.expensetracker.utils.getDrawableResId

@Composable
fun WalletSelector(
    walletName: String,
    walletIcon: String = "img_wallet_default_widget",
    modifier: Modifier = Modifier,
    onWalletSelected: () -> Unit = {},
    isTotalWallet: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColor.Light.WalletSelectorBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { 
                android.util.Log.d("WalletSelector", "Wallet selector clicked, calling onWalletSelected callback")
                onWalletSelected() 
            },  
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use ResourceUtil to get drawable resource ID for regular wallet
        val context = LocalContext.current
        val iconRes = getDrawableResId(context, walletIcon)

        Image(
            painter = painterResource(iconRes),
            contentDescription = stringResource(R.string.transactions_wallet_icon_cd),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = walletName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = if (isTotalWallet) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = stringResource(R.string.transactions_select_wallet_cd),
            tint = TextMain,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun WalletSelectorPreview() {
    ExpenseTrackerTheme {
        WalletSelector(
            walletName = "Tiền Tài Khoản",
            walletIcon = "wallet_1",
            onWalletSelected = {},
            isTotalWallet = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun TotalWalletSelectorPreview() {
    ExpenseTrackerTheme {
        WalletSelector(
            walletName = "All Wallets",
            onWalletSelected = {},
            isTotalWallet = true
        )
    }
}