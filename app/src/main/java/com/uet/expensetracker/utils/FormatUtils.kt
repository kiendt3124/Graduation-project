package com.uet.expensetracker.utils

import java.text.NumberFormat
import java.util.Locale

// Helper to format currency (Vietnamese Dong)
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    // Handle negative numbers correctly for display if needed
    val formatted = format.format(amount).replace("₫", "").trim()
    // You might want specific formatting for negative values, e.g.:
    // if (amount < 0) { return "-${formatted.replace("-","")}" } // Ensure only one minus sign
    return formatted
}

/**
 * Formats a numeric amount with thousand separators
 * @param amount The amount to format
 * @param showDecimal Whether to show decimal places (default: false)
 * @return Formatted string with thousand separators (e.g., "250.000")
 */
fun formatAmount(amount: Double, showDecimal: Boolean = false): String {
    val format = NumberFormat.getNumberInstance(Locale.getDefault())
    format.isGroupingUsed = true // Enable thousand separators
    format.minimumFractionDigits = if (showDecimal) 1 else 0
    format.maximumFractionDigits = if (showDecimal) 2 else 0
    return format.format(amount)
}

// Format amount with currency symbol
fun formatAmountWithCurrency(amount: Double, currencySymbol: String, showDecimal: Boolean = false): String {
    return "${formatAmount(amount, showDecimal)} $currencySymbol"
}

// Add other formatting utilities here in the future