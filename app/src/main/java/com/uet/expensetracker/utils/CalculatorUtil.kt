package com.uet.expensetracker.utils

import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.addtransaction.components.ButtonType
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility class to handle calculator operations and formatting
 */
class CalculatorUtil(private val callback: CalculatorCallback) {

    // Using DecimalFormat for more precise control over number formatting
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
    
    // Maximum allowed digits (excluding decimal point and grouping separators)
    private val MAX_DIGITS = 12
    
    // Track calculator state
    private var pendingOperation: String? = null
    private var operand1: Double = 0.0
    private var isOperationPending = false
    private var amount: String = "0"
    private var formattedAmount: String = "0"
    private var displayExpression: String = "0"
    private var currency: Currency? = null
    
    // Flag to track if we just finished a calculation (pressed equals)
    private var justCalculated = false
    
    init {
        numberFormat.apply {
            isGroupingUsed = true
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }

    /**
     * Sets the currency and updates number formatting
     */
    fun setCurrency(currency: Currency) {
        this.currency = currency
        updateNumberFormatForCurrency(currency)
    }
    
    /**
     * Updates number formatting based on currency
     */
    private fun updateNumberFormatForCurrency(currency: Currency) {
        when (currency.currencyCode) {
            "JPY", "KRW" -> {
                numberFormat.maximumFractionDigits = 0
                numberFormat.minimumFractionDigits = 0
            }
            else -> {
                numberFormat.maximumFractionDigits = 2
                numberFormat.minimumFractionDigits = 0
            }
        }
        
        // Re-format the current amount with the new settings
        if (amount != "0") {
            try {
                val currentAmount = amount.toDouble()
                formattedAmount = numberFormat.format(currentAmount)
                updateCallback()
            } catch (e: NumberFormatException) {
                // If there's an issue, reset to zero
                amount = "0"
                formattedAmount = "0"
                displayExpression = "0"
                updateCallback()
            }
        }
    }

    /**
     * Handle button press events
     */
    fun handleNumpadButtonPressed(button: NumpadButton) {
        when (button.type) {
            ButtonType.DIGIT -> handleDigitPress(button.displayText)
            ButtonType.FUNCTION -> handleFunctionPress(button)
            ButtonType.OPERATOR -> handleOperatorPress(button.displayText)
            ButtonType.ACTION -> handleActionPress()
        }
    }

    /**
     * Update amount from assistant input
     * This is used when the assistant provides an amount to be entered
     */
    fun updateAmountFromAssistant(input: String) {
       updateAmount(input)
    }
    
    private fun handleDigitPress(digit: String) {
        // Handle special case of decimal point
        if (digit == ".") {
            if (!amount.contains(".")) {
                updateAmount(amount + digit)
            } else {
                callback.onError("Decimal point already entered", CalculatorError.INFO)
            }
            return
        }
        
        // If a pending operation exists and we're starting a new number entry
        if (isOperationPending) {
            isOperationPending = false
            
            // Start with the digit
            updateAmount(digit)
            return
        }
        
        // Check if we've reached the maximum number of digits
        val digitsOnly = amount.replace(".", "")
        if (digitsOnly.length >= MAX_DIGITS && digit != "000") {
            // Don't add more digits if we've reached the maximum
            callback.onError("Maximum digits reached", CalculatorError.WARNING)
            return
        }
        
        // Handle triple zeros
        if (digit == "000") {
            if (amount == "0") {
                return // Don't add zeros to zero
            }
            
            // Check if adding 000 would exceed MAX_DIGITS
            if (digitsOnly.length + 3 > MAX_DIGITS) {
                // Add as many zeros as possible without exceeding MAX_DIGITS
                val zerosToAdd = MAX_DIGITS - digitsOnly.length
                if (zerosToAdd > 0) {
                    val zeros = "0".repeat(zerosToAdd)
                    updateAmount(amount + zeros)
                    callback.onError(
                        "Maximum digits reached, added $zerosToAdd zeros",
                        CalculatorError.INFO
                    )
                } else {
                    callback.onError("Maximum digits reached", CalculatorError.WARNING)
                }
                return
            }
            
            updateAmount(amount + digit)
            return
        }
        
        // If current amount is 0, replace it unless it's a decimal point
        val newAmount = if (amount == "0" && digit != ".") {
            digit
        } else {
            amount + digit
        }
        
        updateAmount(newAmount)
    }
    
    private fun handleFunctionPress(button: NumpadButton) {
        when (button) {
            NumpadButton.DELETE -> handleDelete()
            NumpadButton.CLEAR -> handleClear()
            NumpadButton.SAVE -> handleSaveAmount()
            else -> {
                callback.onError("Function not supported in this screen", CalculatorError.INFO)
            }
        }
    }
    
    private fun handleOperatorPress(operator: String) {
        try {
            val currentValue = amount.toDouble()
            
            // Reset the justCalculated flag when an operator is pressed
            justCalculated = false
            
            // If there's already a pending operation, perform it first
            if (pendingOperation != null && isOperationPending) {
                // Just change the operator without calculation
                pendingOperation = operator
                updateDisplayExpression()
                return
            }
            
            if (pendingOperation != null) {
                // Calculate the result of the previous operation
                val result = calculateResult(operand1, currentValue, pendingOperation!!)
                
                // Update the display with the result
                updateAmount(result.toString())
                
                // Save the result as first operand for the next operation
                operand1 = result
            } else {
                // Save the current value as the first operand
                operand1 = currentValue
            }
            
            // Set the pending operation
            pendingOperation = operator
            isOperationPending = true
            
            // Update display expression to include the operator
            updateDisplayExpression()
            
        } catch (e: NumberFormatException) {
            callback.onError("Invalid number for calculation", CalculatorError.ERROR)
        } catch (e: ArithmeticException) {
            callback.onError(e.message ?: "Calculation error", CalculatorError.ERROR)
        }
    }
    
    private fun calculateResult(operand1: Double, operand2: Double, operation: String): Double {
        return when (operation) {
            "+" -> operand1 + operand2
            "-" -> operand1 - operand2
            "×" -> operand1 * operand2
            "÷" -> {
                if (operand2 == 0.0) {
                    throw ArithmeticException("Cannot divide by zero")
                }
                operand1 / operand2
            }
            else -> throw IllegalArgumentException("Unknown operator: $operation")
        }
    }
    
    private fun handleActionPress() {
        // Action button (equals) - evaluate the current expression
        try {
            if (pendingOperation != null && !isOperationPending) {
                val currentValue = amount.toDouble()
                val result = calculateResult(operand1, currentValue, pendingOperation!!)
                
                // Update the display with the result
                updateAmount(result.toString())
                
                // Reset the operation state after calculation 
                pendingOperation = null
                isOperationPending = false
                
                // Show completed calculation in display expression
                updateDisplayExpression(showResult = true)
            } else {
                // If no operation is pending, just save the amount
                handleSaveAmount()
            }
        } catch (e: NumberFormatException) {
            callback.onError("Invalid number for calculation", CalculatorError.ERROR)
        } catch (e: ArithmeticException) {
            callback.onError(e.message ?: "Calculation error", CalculatorError.ERROR)
        }
    }

    private fun handleDelete() {
        if (amount.length <= 1) {
            updateAmount("0")
        } else {
            updateAmount(amount.substring(0, amount.length - 1))
        }
    }
    
    private fun handleClear() {
        // Reset calculator state
        pendingOperation = null
        operand1 = 0.0
        isOperationPending = false
        justCalculated = false
        
        // Reset the amount
        updateAmount("0")
        displayExpression = "0"
        updateCallback()
    }
    
    fun handleSaveAmount() {
        // Ensure we have a valid amount before saving
        try {
            val amountValue = amount.toDouble()
            
            // Check if amount is positive
            if (amountValue <= 0) {
                callback.onError(
                    "Please enter an amount greater than zero",
                    CalculatorError.ERROR
                )
                return
            }
            
            // For amounts that end with decimal point or trailing zeros after decimal point,
            // clean up before returning
            val cleanedAmount = if (amount.contains(".")) {
                val bd = BigDecimal(amount)
                bd.stripTrailingZeros().toPlainString()
            } else {
                amount
            }
            
            // Create the formatted amount with currency symbol for display
            val cleanedFormattedAmount = if (formattedAmount.endsWith(".")) {
                formattedAmount.substring(0, formattedAmount.length - 1)
            } else {
                formattedAmount
            }
            
            callback.onSaveAmount(cleanedAmount, cleanedFormattedAmount)
        } catch (e: NumberFormatException) {
            callback.onError("Invalid amount format", CalculatorError.ERROR)
        }
    }
    
    private fun updateAmount(newAmount: String) {
        try {
            // Handle special case of just a decimal point
            if (newAmount == ".") {
                amount = "0."
                formattedAmount = "0."
                displayExpression = getDisplayExpressionPrefix() + "0."
                updateCallback()
                return
            }
            
            // Handle case where amount ends with decimal point
            if (newAmount.endsWith(".")) {
                val numericPart = newAmount.substring(0, newAmount.length - 1)
                val numericValue = numericPart.toDouble()
                formattedAmount = numberFormat.format(numericValue) + "."
                amount = newAmount
                updateDisplayExpression()
                updateCallback()
                return
            }
            
            // Standard case - parse and format
            val numericValue = newAmount.toDouble()
            formattedAmount = numberFormat.format(numericValue)
            amount = newAmount
            updateDisplayExpression()
            updateCallback()
        } catch (e: NumberFormatException) {
            // If the input is not a valid number, revert to the previous state
            callback.onError("Invalid number format", CalculatorError.ERROR)
        }
    }
    
    private fun updateDisplayExpression(showResult: Boolean = false) {
        displayExpression = if (showResult) {
            // Show completed calculation
            formattedAmount
        } else if (pendingOperation != null) {
            if (isOperationPending) {
                // Show first operand with operator pending
                val formattedOperand1 = numberFormat.format(operand1)
                "$formattedOperand1 $pendingOperation "
            } else {
                // Show full expression with both operands
                val formattedOperand1 = numberFormat.format(operand1)
                "$formattedOperand1 $pendingOperation $formattedAmount"
            }
        } else {
            // Just show the current number
            formattedAmount
        }
        
        updateCallback()
    }
    
    private fun getDisplayExpressionPrefix(): String {
        return if (pendingOperation != null && !isOperationPending) {
            val formattedOperand1 = numberFormat.format(operand1)
            "$formattedOperand1 $pendingOperation "
        } else {
            ""
        }
    }
    
    private fun updateCallback() {
        callback.onCalculatorUpdate(
            amount,
            formattedAmount,
            displayExpression
        )
    }
    
    /**
     * Current calculator state
     */
    fun getCurrentState(): CalculatorState {
        return CalculatorState(
            amount = amount,
            formattedAmount = formattedAmount,
            displayExpression = displayExpression
        )
    }
    
    /**
     * Reset calculator to initial state
     */
    fun reset() {
        pendingOperation = null
        operand1 = 0.0
        isOperationPending = false
        justCalculated = false
        amount = "0"
        formattedAmount = "0"
        displayExpression = "0"
        updateCallback()
    }
    
    /**
     * Calculator state data class
     */
    data class CalculatorState(
        val amount: String,
        val formattedAmount: String,
        val displayExpression: String
    )
    
    /**
     * Error levels for calculator operations
     */
    enum class CalculatorError {
        INFO,
        WARNING,
        ERROR
    }
    
    /**
     * Callback interface for calculator events
     */
    interface CalculatorCallback {
        fun onCalculatorUpdate(amount: String, formattedAmount: String, displayExpression: String)
        fun onError(message: String, level: CalculatorError)
        fun onSaveAmount(amount: String, formattedAmount: String)
    }
} 