package com.uet.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

object AppColor {
    object Light {
        object PrimaryColor {
            val disabledContentColor = Color(0xFFB0B0B0) // Light gray for disabled text
            val disabledContainerColor = Color(0xFF2A2A2A) // Dark gray for disabled state
            val containerColor = Color(0xFFF6F6F6)
            val contentColor = Color.Black
            val cardColor = ScreenBackground
            val TextButtonColor = Color(0xFFFCA419)
            val containerColorSecondary = Color(0xFF2A2A2A)
        }

        object SecondaryColor {
            val color1 = Color(0x99ffffff)
            val color2 = Color(0xff1c1c1e)
        }

        // Colors from the Transaction Screen screenshot
        val WalletSelectorBackground = Color.White
        val InflowAmountColor = Color(0xFF1178F6)
        val OutflowAmountColor = Color(0xFFF765A3)
        val ReportButtonBackground = Color(0xFF4CAF50)
        val ExpenseAmountColor = Color(0xFFF05252)
        val IncomeAmountColor = Color(0xFF4CAF50)
        val CategoryIconBackgroundLoan = Color(0xFFEF5350)
        val CategoryIconBackgroundFood = Color(0xFF26A69A)
        val DividerColor = Color(0x1AFFFFFF)
        val TabIndicatorColor = Color(0xFF505D6D)
        val TransactionItemBackground = Color(0xFF2C2C2E)

        // Colors for the TransactionNumpad component
        object NumpadColors {
            val ButtonBackgroundColor = Color.White // Dark gray button background
            val ButtonContentColor = TextMain // White text for numbers
            val OperatorContentColor = Color(0xFFFCA419) // Green text/icons for operators
            val ActionBackgroundColor = Color(0xFFFCA419) // Blue background for action button
            val DividerColor = Color.White // Black divider between buttons
            val SaveButtonBackgroundColor = Color(0xFFFCA419)  // Background for Save button
            val SaveButtonTextColor = TextMain // Text color for Save button
        }
    }
}

// Custom colors based on the screenshot
val ScreenBackground = Color.White
val CardBackground = Color(0xFF2C2C2E)
val TextPrimary = Color.White
val TextMain = Color(0xFF1E2A36)
val TextSecondary = Color(0xFF505D6D)
val TextAccent = Color(0xFF4CAF50)
val IconTint = Color(0xFF1E2A36)
val BalanceColor = Color.White
val WalletIconBgOrange = Color(0xFFE67E22)
val WalletIconBgTeal = Color(0xFF1ABC9C)
val ChartPlaceholderColor = Color(0xFF3A3A3C)
val ReportHighlightColor = Color(0xFFE91E63)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val greenIndicator = Color(0xFF00C853)
val darkSurface = Color(0xFF1E1E1E) // A slightly lighter dark for cards
val onDarkSurface = TextMain
val fabColor = Color(0xFF00C853) // Green FAB
val ScreenBgMain = Color(0xFFF6F6F6)