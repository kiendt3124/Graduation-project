package com.uet.expensetracker.features.money.ui.detailtransaction.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.TextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTransactionTopBar(
    onCloseClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = TextMain
                )
            }
        },
        actions = {
//            IconButton(onClick = onCopyClick) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_w_add),
//                    contentDescription = "Copy",
//                    tint = TextMain
//                )
//            }
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = TextMain
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = TextMain
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = TextMain
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor,
            titleContentColor = AppColor.Light.PrimaryColor.contentColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DetailTransactionTopBarPreview() {
    DetailTransactionTopBar(
        onCloseClick = {},
        onCopyClick = {},
        onShareClick = {},
        onEditClick = {},
        onDeleteClick = {}
    )
}