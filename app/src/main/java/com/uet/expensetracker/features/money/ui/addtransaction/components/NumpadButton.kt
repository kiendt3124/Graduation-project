package com.uet.expensetracker.features.money.ui.addtransaction.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.AppColor

/**
 * Enum representing all possible buttons on the transaction numpad
 */
enum class NumpadButton(val displayText: String, val type: ButtonType) {
    // Digit buttons
    ZERO("0", ButtonType.DIGIT),
    ONE("1", ButtonType.DIGIT),
    TWO("2", ButtonType.DIGIT),
    THREE("3", ButtonType.DIGIT),
    FOUR("4", ButtonType.DIGIT),
    FIVE("5", ButtonType.DIGIT),
    SIX("6", ButtonType.DIGIT),
    SEVEN("7", ButtonType.DIGIT),
    EIGHT("8", ButtonType.DIGIT),
    NINE("9", ButtonType.DIGIT),
    TRIPLE_ZERO("000", ButtonType.DIGIT),
    DECIMAL(".", ButtonType.DIGIT),

    // Operator buttons
    PLUS("+", ButtonType.OPERATOR),
    MINUS("-", ButtonType.OPERATOR),
    MULTIPLY("×", ButtonType.OPERATOR),
    DIVIDE("÷", ButtonType.OPERATOR),

    // Function buttons
    CLEAR("C", ButtonType.FUNCTION),
    DELETE("DEL", ButtonType.FUNCTION),
    ACTION("", ButtonType.ACTION),
    SAVE("Save", ButtonType.FUNCTION);

    /**
     * Checks if button has an icon instead of text
     */
    fun hasIcon(): Boolean = this == DELETE || this == ACTION

    /**
     * Gets the icon for buttons that use icons instead of text
     */
    fun getIcon(): ImageVector? {
        return when (this) {
            DELETE -> Icons.Filled.Delete
            ACTION -> Icons.Filled.Equals
            else -> null
        }
    }

    fun getIconPainterResource(): Int? {
        return when (this) {
            DELETE -> R.drawable.ic_backspace
            else -> null
        }
    }

    /**
     * Gets the color for this button's content (text or icon)
     */
    fun getContentColor() = when (type) {
        ButtonType.DIGIT -> AppColor.Light.NumpadColors.ButtonContentColor
        ButtonType.OPERATOR -> AppColor.Light.NumpadColors.OperatorContentColor
        ButtonType.FUNCTION -> when (this) {
            SAVE -> AppColor.Light.NumpadColors.SaveButtonTextColor
            else -> AppColor.Light.NumpadColors.OperatorContentColor
        }

        ButtonType.ACTION -> AppColor.Light.NumpadColors.ButtonContentColor
    }

    /**
     * Gets the background color for this button
     */
    fun getBackgroundColor() = when (this) {
        ACTION -> AppColor.Light.NumpadColors.ActionBackgroundColor
        SAVE -> AppColor.Light.NumpadColors.SaveButtonBackgroundColor
        else -> AppColor.Light.NumpadColors.ButtonBackgroundColor
    }
}

/**
 * Types of numpad buttons
 */
enum class ButtonType {
    DIGIT,      // Number buttons and decimal point
    OPERATOR,   // Mathematical operators
    FUNCTION,   // Function buttons like Clear, Delete, Save
    ACTION      // The action button (blue arrow)
}


val Icons.Filled.Equals: ImageVector
    get() {
        if (_equals != null) {
            return _equals!!
        }
        _equals = materialIcon(name = "Filled.Equals") {
            materialPath {
                moveTo(20.0f, 14.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(-2.0f)
                close()
                moveTo(20.0f, 8.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineTo(8.0f)
                close()
            }
        }
        return _equals!!
    }

private var _equals: ImageVector? = null