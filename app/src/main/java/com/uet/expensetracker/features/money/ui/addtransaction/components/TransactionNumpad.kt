package com.uet.expensetracker.features.money.ui.addtransaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor

// Import only what we need
private val NumpadDividerColor = AppColor.Light.NumpadColors.DividerColor
val suggest: List<String> = listOf(
"500000.0",
"2000000.0",
"30000.0",
"100000.0",
"5000.0"
)
/**
 * Calculator numpad for transaction input
 */
@Composable
fun TransactionNumpad(
    onButtonClick: (NumpadButton) -> Unit = {},
    showSaveButton: Boolean = true,
    suggestedAmounts: List<String>? = null,
    onSuggestionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(
                Modifier
                    .fillMaxWidth()
                    .background(AppColor.Light.NumpadColors.ButtonBackgroundColor)
            )
    ) {
        if (suggestedAmounts!= null) {
            AmountSuggestionBar(
                suggestedAmounts = suggestedAmounts,
                onSuggestionClick = onSuggestionClick
            )
        }

        // Save Button - only shown if showSaveButton is true
        if (showSaveButton) {
            Row(modifier = Modifier.fillMaxWidth()) {
                NumpadButtonComponent(
                    button = NumpadButton.SAVE,
                    modifier = Modifier.weight(1f),
                    onClick = { onButtonClick(NumpadButton.SAVE) }
                )
            }
        }

        // First row: C, ÷, ×, DEL
        Row(modifier = Modifier.fillMaxWidth()) {
            NumpadButtonComponent(
                button = NumpadButton.CLEAR,
                onClick = { onButtonClick(NumpadButton.CLEAR) },
                modifier = Modifier.background(color = Color.White.copy(alpha = 0.1f))
            )
            NumpadButtonComponent(
                button = NumpadButton.DIVIDE,
                onClick = { onButtonClick(NumpadButton.DIVIDE) },
                modifier = Modifier.background(color = Color.White.copy(alpha = 0.1f))
            )
            NumpadButtonComponent(
                button = NumpadButton.MULTIPLY,
                onClick = { onButtonClick(NumpadButton.MULTIPLY) },
                modifier = Modifier.background(color = Color.White.copy(alpha = 0.1f))
            )
            NumpadButtonComponent(
                button = NumpadButton.DELETE,
                onClick = { onButtonClick(NumpadButton.DELETE) },
                modifier = Modifier.background(color = Color.White.copy(alpha = 0.1f))
            )
        }

        // Second row: 7, 8, 9, -
        Row(modifier = Modifier.fillMaxWidth()) {
            NumpadButtonComponent(
                button = NumpadButton.SEVEN,
                onClick = { onButtonClick(NumpadButton.SEVEN) }
            )
            NumpadButtonComponent(
                button = NumpadButton.EIGHT,
                onClick = { onButtonClick(NumpadButton.EIGHT) }
            )
            NumpadButtonComponent(
                button = NumpadButton.NINE,
                onClick = { onButtonClick(NumpadButton.NINE) }
            )
            NumpadButtonComponent(
                button = NumpadButton.MINUS,
                onClick = { onButtonClick(NumpadButton.MINUS) },
                modifier = Modifier.background(color = Color.White.copy(alpha = 0.1f))
            )
        }

        // Third row: 4, 5, 6, +
        Row(modifier = Modifier.fillMaxWidth()) {
            NumpadButtonComponent(
                button = NumpadButton.FOUR,
                onClick = { onButtonClick(NumpadButton.FOUR) }
            )
            NumpadButtonComponent(
                button = NumpadButton.FIVE,
                onClick = { onButtonClick(NumpadButton.FIVE) }
            )
            NumpadButtonComponent(
                button = NumpadButton.SIX,
                onClick = { onButtonClick(NumpadButton.SIX) }
            )
            NumpadButtonComponent(
                button = NumpadButton.PLUS,
                onClick = { onButtonClick(NumpadButton.PLUS) },
                modifier = Modifier.background(color = Color.White.copy(alpha = 0.1f))
            )
        }

        // Fourth and fifth rows with action button
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left part with numbers (1-3 and 0-000-.)
            Column(modifier = Modifier.weight(3f)) {
                // Fourth row: 1, 2, 3
                Row(modifier = Modifier.fillMaxWidth()) {
                    NumpadButtonComponent(
                        button = NumpadButton.ONE,
                        onClick = { onButtonClick(NumpadButton.ONE) }
                    )
                    NumpadButtonComponent(
                        button = NumpadButton.TWO,
                        onClick = { onButtonClick(NumpadButton.TWO) }
                    )
                    NumpadButtonComponent(
                        button = NumpadButton.THREE,
                        onClick = { onButtonClick(NumpadButton.THREE) }
                    )
                }

                // Fifth row: 0, 000, .
                Row(modifier = Modifier.fillMaxWidth()) {
                    NumpadButtonComponent(
                        button = NumpadButton.ZERO,
                        onClick = { onButtonClick(NumpadButton.ZERO) }
                    )
                    NumpadButtonComponent(
                        button = NumpadButton.TRIPLE_ZERO,
                        onClick = { onButtonClick(NumpadButton.TRIPLE_ZERO) }
                    )
                    NumpadButtonComponent(
                        button = NumpadButton.DECIMAL,
                        onClick = { onButtonClick(NumpadButton.DECIMAL) }
                    )
                }
            }

            // Action button (blue)
            NumpadActionButton(
                onClick = { onButtonClick(NumpadButton.ACTION) }
            )
        }
    }
}

/**
 * Standard numpad button component
 */
@Composable
private fun RowScope.NumpadButtonComponent(
    button: NumpadButton,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .then(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .border(0.5.dp, NumpadDividerColor)
            ),
    ) {
        if (button.hasIcon()) {
            button.getIcon()?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = button.displayText,
                    tint = button.getContentColor(),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            val label = if (button == NumpadButton.SAVE) {
                stringResource(id = R.string.add_transaction_button_save)
            } else {
                button.displayText
            }
            Text(
                text = label,
                color = button.getContentColor(),
                fontSize = 20.sp
            )
        }
    }
}

/**
 * Special numpad action button (blue button spanning two rows)
 */
@Composable
private fun RowScope.NumpadActionButton(
    onClick: () -> Unit = {}
) {
    val button = NumpadButton.ACTION
    Box(
        modifier = Modifier
            .weight(1f)
            .height(100.dp)
            .background(button.getBackgroundColor())
            .border(0.5.dp, NumpadDividerColor)
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        button.getIcon()?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.add_transaction_action_cd),
                tint = button.getContentColor(),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionNumpadPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black
    ) {
        TransactionNumpad()
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionNumpadWithoutSavePreview() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black
    ) {
        TransactionNumpad(showSaveButton = false)
    }
}