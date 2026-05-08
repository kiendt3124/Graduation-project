package com.uet.expensetracker.features.money.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.R

@Composable
fun ContactPermissionBottomSheet(
    onAllowAccess: () -> Unit,
    onNoThanks: () -> Unit,
    onDoNotAskAgainChanged: (Boolean) -> Unit
) {
    var doNotAskAgain by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF2C2C2E) // Dark background color from image
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_with),
                contentDescription = stringResource(R.string.contacts_icon_cd),
                tint = Color.LightGray, // Icon color
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.contacts_get_your_contacts),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.contacts_permission_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Checkbox(
                    checked = doNotAskAgain,
                    onCheckedChange = {
                        doNotAskAgain = it
                        onDoNotAskAgainChanged(it)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF4CAF50), // Green accent
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.contacts_do_not_ask_again),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onNoThanks,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50)) // Green accent
                ) {
                    Text(stringResource(R.string.contacts_no_thanks))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAllowAccess,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Green accent
                ) {
                    Text(stringResource(R.string.contacts_allow_access), color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2C2E)
@Composable
fun ContactPermissionBottomSheetPreview() {
    MaterialTheme { // Ensure MaterialTheme is applied for preview
        ContactPermissionBottomSheet(
            onAllowAccess = {},
            onNoThanks = {},
            onDoNotAskAgainChanged = {}
        )
    }
} 