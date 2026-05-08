package com.uet.expensetracker.features.money.ui.enteramount

import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.ui.addtransaction.components.ButtonType
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.uet.expensetracker.R

@HiltViewModel
class EnterAmountViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseViewModel<
        EnterAmountState,
        EnterAmountIntent,
        EnterAmountEvent>() {

    override val _viewState = MutableStateFlow(EnterAmountState())
    
    // Using DecimalFormat for more precise control over number formatting
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
    
    // Maximum allowed digits (excluding decimal point and grouping separators)
    private val MAX_DIGITS = 12
    
    // Track calculator state
    private var pendingOperation: String? = null
    private var operand1: Double = 0.0
    private var isOperationPending = false
    
    init {
        numberFormat.apply {
            isGroupingUsed = true
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }

    fun setCurrency(currency: Currency) {
        _viewState.value = _viewState.value.copy(currency = currency)
        // If the currency has changed, we may need to adjust formatting settings
        updateNumberFormatForCurrency(currency)
    }
    
    private fun updateNumberFormatForCurrency(currency: Currency) {
        // Could customize number format based on currency if needed
        // For example, some currencies don't use decimal places
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
        if (_viewState.value.amount != "0") {
            try {
                val currentAmount = _viewState.value.amount.toDouble()
                _viewState.value = _viewState.value.copy(
                    formattedAmount = numberFormat.format(currentAmount)
                )
            } catch (e: NumberFormatException) {
                // If there's an issue, reset to zero
                _viewState.value = _viewState.value.copy(
                    amount = "0",
                    formattedAmount = "0"
                )
            }
        }
    }

    override fun processIntent(intent: EnterAmountIntent) {
        when (intent) {
            is EnterAmountIntent.NumpadButtonPressed -> handleNumpadButtonPressed(intent.button)
            is EnterAmountIntent.SaveAmount -> handleSaveAmount()
        }
    }

    private fun handleNumpadButtonPressed(button: NumpadButton) {
        when (button.type) {
            ButtonType.DIGIT -> handleDigitPress(button.displayText)
            ButtonType.FUNCTION -> handleFunctionPress(button)
            ButtonType.OPERATOR -> handleOperatorPress(button.displayText)
            ButtonType.ACTION -> handleActionPress()
        }
    }
    
    private fun handleDigitPress(digit: String) {
        val currentState = _viewState.value
        val currentAmount = currentState.amount
        
        // If a pending operation exists and we're starting a new number entry
        if (isOperationPending) {
            isOperationPending = false
            
            // If digit is decimal point, start with "0."
            if (digit == ".") {
                updateAmount("0.")
                return
            }
            
            // Otherwise, start with just the digit
            updateAmount(digit)
            return
        }
        
        // Handle special case of decimal point
        if (digit == ".") {
            if (!currentAmount.contains(".")) {
                updateAmount(currentAmount + digit)
            } else {
                emitEvent(
                    EnterAmountEvent.ShowError(
                        context.getString(R.string.enter_amount_error_decimal_already),
                        EnterAmountEvent.ErrorLevel.INFO
                    )
                )
            }
            return
        }
        
        // Check if we've reached the maximum number of digits
        val digitsOnly = currentAmount.replace(".", "")
        if (digitsOnly.length >= MAX_DIGITS && digit != "000") {
            // Don't add more digits if we've reached the maximum
            emitEvent(
                EnterAmountEvent.ShowError(
                    context.getString(R.string.enter_amount_error_max_digits),
                    EnterAmountEvent.ErrorLevel.WARNING
                )
            )
            return
        }
        
        // Handle triple zeros
        if (digit == "000") {
            if (currentAmount == "0") {
                return // Don't add zeros to zero
            }
            
            // Check if adding 000 would exceed MAX_DIGITS
            if (digitsOnly.length + 3 > MAX_DIGITS) {
                // Add as many zeros as possible without exceeding MAX_DIGITS
                val zerosToAdd = MAX_DIGITS - digitsOnly.length
                if (zerosToAdd > 0) {
                    val zeros = "0".repeat(zerosToAdd)
                    updateAmount(currentAmount + zeros)
                    emitEvent(
                        EnterAmountEvent.ShowError(
                            context.getString(R.string.enter_amount_error_max_digits_with_zeros, zerosToAdd.toString()),
                            EnterAmountEvent.ErrorLevel.INFO
                        )
                    )
                } else {
                    emitEvent(
                        EnterAmountEvent.ShowError(
                            context.getString(R.string.enter_amount_error_max_digits),
                            EnterAmountEvent.ErrorLevel.WARNING
                        )
                    )
                }
                return
            }
            
            updateAmount(currentAmount + digit)
            return
        }
        
        // If current amount is 0, replace it unless it's a decimal point
        val newAmount = if (currentAmount == "0" && digit != ".") {
            digit
        } else {
            currentAmount + digit
        }
        
        updateAmount(newAmount)
    }
    
    private fun handleFunctionPress(button: NumpadButton) {
        when (button) {
            NumpadButton.DELETE -> handleDelete()
            NumpadButton.CLEAR -> handleClear()
            NumpadButton.SAVE -> handleSaveAmount()
            else -> {
                emitEvent(
                    EnterAmountEvent.ShowError(
                        context.getString(R.string.enter_amount_error_function_not_supported),
                        EnterAmountEvent.ErrorLevel.INFO
                    )
                )
            }
        }
    }
    
    private fun handleOperatorPress(operator: String) {
        try {
            val currentValue = _viewState.value.amount.toDouble()
            
            // If there's already a pending operation, perform it first
            if (pendingOperation != null && isOperationPending) {
                // Just change the operator without calculation
                pendingOperation = operator
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
            
        } catch (e: NumberFormatException) {
            emitEvent(
                EnterAmountEvent.ShowError(
                    context.getString(R.string.enter_amount_error_invalid_number),
                    EnterAmountEvent.ErrorLevel.ERROR
                )
            )
        } catch (e: ArithmeticException) {
            emitEvent(
                EnterAmountEvent.ShowError(
                    e.message ?: context.getString(R.string.enter_amount_error_calculation),
                    EnterAmountEvent.ErrorLevel.ERROR
                )
            )
        }
    }
    
    private fun calculateResult(operand1: Double, operand2: Double, operation: String): Double {
        return when (operation) {
            "+" -> operand1 + operand2
            "-" -> operand1 - operand2
            "×" -> operand1 * operand2
            "÷" -> {
                if (operand2 == 0.0) {
                    throw ArithmeticException(context.getString(R.string.enter_amount_error_divide_by_zero))
                }
                operand1 / operand2
            }
            else -> throw IllegalArgumentException(context.getString(R.string.enter_amount_error_unknown_operator, operation))
        }
    }
    
    private fun handleActionPress() {
        // Action button (equals) - evaluate the current expression
        try {
            if (pendingOperation != null && !isOperationPending) {
                val currentValue = _viewState.value.amount.toDouble()
                val result = calculateResult(operand1, currentValue, pendingOperation!!)
                
                // Update the display with the result
                updateAmount(result.toString())
                
                // Reset the calculator state
                pendingOperation = null
                isOperationPending = false
            } else {
                // If no operation is pending, just save the amount
                handleSaveAmount()
            }
        } catch (e: NumberFormatException) {
            emitEvent(
                EnterAmountEvent.ShowError(
                    context.getString(R.string.enter_amount_error_invalid_number),
                    EnterAmountEvent.ErrorLevel.ERROR
                )
            )
        } catch (e: ArithmeticException) {
            emitEvent(
                EnterAmountEvent.ShowError(
                    e.message ?: context.getString(R.string.enter_amount_error_calculation),
                    EnterAmountEvent.ErrorLevel.ERROR
                )
            )
        }
    }

    private fun handleDelete() {
        val currentAmount = _viewState.value.amount
        
        val newAmount = if (currentAmount.length <= 1) {
            "0"
        } else {
            currentAmount.substring(0, currentAmount.length - 1)
        }
        
        updateAmount(newAmount)
    }
    
    private fun handleClear() {
        // Reset calculator state
        pendingOperation = null
        operand1 = 0.0
        isOperationPending = false
        
        // Reset the amount
        updateAmount("0")
    }
    
    private fun handleSaveAmount() {
        val currentAmount = _viewState.value.amount
        val formattedAmount = _viewState.value.formattedAmount
        
        // Ensure we have a valid amount before saving
        try {
            val amountValue = currentAmount.toDouble()
            
            // Check if amount is positive
            if (amountValue <= 0) {
                emitEvent(
                    EnterAmountEvent.ShowError(
                        context.getString(R.string.enter_amount_error_amount_zero),
                        EnterAmountEvent.ErrorLevel.ERROR
                    )
                )
                return
            }
            
            // For amounts that end with decimal point or trailing zeros after decimal point,
            // clean up before returning
            val cleanedAmount = if (currentAmount.contains(".")) {
                val bd = BigDecimal(currentAmount)
                bd.stripTrailingZeros().toPlainString()
            } else {
                currentAmount
            }
            
            // Create the formatted amount with currency symbol for display
            val cleanedFormattedAmount = if (formattedAmount.endsWith(".")) {
                formattedAmount.substring(0, formattedAmount.length - 1)
            } else {
                formattedAmount
            }
            
            emitEvent(EnterAmountEvent.NavigateBack(cleanedAmount, cleanedFormattedAmount))
        } catch (e: NumberFormatException) {
            emitEvent(
                EnterAmountEvent.ShowError(
                    context.getString(R.string.enter_amount_error_invalid_format),
                    EnterAmountEvent.ErrorLevel.ERROR
                )
            )
        }
    }
    
    private fun updateAmount(newAmount: String) {
        try {
            // Handle special case of just a decimal point
            if (newAmount == ".") {
                _viewState.value = _viewState.value.copy(
                    amount = "0.",
                    formattedAmount = "0."
                )
                return
            }
            
            // Handle case where amount ends with decimal point
            if (newAmount.endsWith(".")) {
                val numericPart = newAmount.substring(0, newAmount.length - 1)
                val numericValue = numericPart.toDouble()
                val formatted = numberFormat.format(numericValue) + "."
                
                _viewState.value = _viewState.value.copy(
                    amount = newAmount,
                    formattedAmount = formatted
                )
                return
            }
            
            // Standard case - parse and format
            val numericValue = newAmount.toDouble()
            val formatted = numberFormat.format(numericValue)
            
            _viewState.value = _viewState.value.copy(
                amount = newAmount,
                formattedAmount = formatted
            )
        } catch (e: NumberFormatException) {
            // If the input is not a valid number, revert to the previous state
            emitEvent(
                EnterAmountEvent.ShowError(
                    context.getString(R.string.enter_amount_error_invalid_number_format),
                    EnterAmountEvent.ErrorLevel.ERROR
                )
            )
        }
    }
} 