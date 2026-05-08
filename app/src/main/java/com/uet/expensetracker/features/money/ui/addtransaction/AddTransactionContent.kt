package com.uet.expensetracker.features.money.ui.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Contact
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.ui.addtransaction.components.AddTransactionTopAppBar
import com.uet.expensetracker.features.money.ui.addtransaction.components.ImageSourceDialog
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import com.uet.expensetracker.features.money.ui.addtransaction.components.TransactionInputFields
import com.uet.expensetracker.features.money.ui.addtransaction.components.TransactionNumpad
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import java.util.Date
import kotlin.String
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Composable
fun AddTransactionContent(
    state: AddTransactionState,
    onIntent: (AddTransactionIntent) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    title: String = "Add Transaction",
    saveButtonText: String = "Save"
) {
    Scaffold(
        topBar = {
            AddTransactionTopAppBar(onCloseClick = onCloseClick, title = title)
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Hide numpad when clicking outside
                    onIntent(AddTransactionIntent.HideNumpad)
                }
        ) {
            // Scrollable input fields area and suggestions take available vertical space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Prevent click propagation - don't hide numpad when clicking on input fields
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionInputFields(
                    amount = state.amount,
                    amountInput = state.amountInput,
                    formattedAmount = state.formattedAmount,
                    displayExpression = state.displayExpression,
                    wallet = state.wallet,
                    category = state.category,
                    description = state.description,
                    transactionDate = state.transactionDate,
                    showMoreDetails = state.showMoreDetails,
                    withContacts = state.withContacts,
                    eventName = state.eventName,
                    reminderSet = state.reminderSet,
                    excludeFromReport = state.excludeFromReport,
                    photoUri = state.photoUri,
                    onAmountClick = { onIntent(AddTransactionIntent.ShowNumpad) },
                    onDescriptionChange = { onIntent(AddTransactionIntent.UpdateDescription(it)) },
                    onToggleMoreDetails = { onIntent(AddTransactionIntent.ToggleMoreDetails) },
                    onWalletClick = { onIntent(AddTransactionIntent.ShowWalletSelector) },
                    onCategoryClick = {
                        onIntent(AddTransactionIntent.ShowCategorySelector)
                        onCategoryClick()
                    },
                    onDateClick = { date -> onIntent(AddTransactionIntent.SelectDate(date)) },
                    onWithPersonClick = { onIntent(AddTransactionIntent.ShowContactSelector) },
                    onContactRemove = { contact -> onIntent(AddTransactionIntent.RemoveContact(contact)) },
                    onEventClick = { /* Open event selector */ },
                    onReminderClick = { onIntent(AddTransactionIntent.SetReminder(!state.reminderSet)) },
                    onExcludeFromReportChange = { onIntent(AddTransactionIntent.UpdateExcludeFromReport(it)) },
                    onTakePhotoClick = { onIntent(AddTransactionIntent.ShowImageSourceDialog) },
                    onGalleryClick = { onIntent(AddTransactionIntent.SelectFromGallery) },
                    onRemovePhoto = { onIntent(AddTransactionIntent.RemovePhoto) }
                )

//                AmountSuggestionBar(
//                    suggestedAmounts = state.suggestedAmounts,
//                    onSuggestionClick = { onIntent(AddTransactionIntent.UpdateAmount(it)) }
//                )
            }

            // Save Button - always visible
            TextButton(
                onClick = {
                    onIntent(AddTransactionIntent.Save)
                          },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(size = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = AppColor.Light.PrimaryColor.contentColor,
                    containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    disabledContainerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    disabledContentColor = AppColor.Light.PrimaryColor.disabledContentColor
                ),
                enabled = !state.amount.equals(0) && state.category != null && state.wallet != null
            ) {
                Text(text = saveButtonText, fontSize = 16.sp, color = Color.White)
            }

            // Conditionally show numpad
            if (state.showNumpad) {
                Column(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Prevent click propagation - don't hide numpad when clicking on numpad itself
                    }
                ) {
                    TransactionNumpad(
                        onButtonClick = { button ->
                            onIntent(AddTransactionIntent.NumpadPressed(button))
                            if (button == NumpadButton.SAVE) {
                                onIntent(AddTransactionIntent.Save)
                            }
                        },
                        showSaveButton = false,
                        suggestedAmounts = null
                    )
                }
            }
        }
    }
    
    // Show image source dialog when needed
    if (state.showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { onIntent(AddTransactionIntent.HideImageSourceDialog) },
            onCameraClick = { onIntent(AddTransactionIntent.SelectFromCamera) },
            onGalleryClick = { onIntent(AddTransactionIntent.SelectFromGallery) }
        )
    }
}

@Preview()
@Composable
fun AddTransactionContentPreview() {
    ExpenseTrackerTheme(darkTheme = true) {
        val previewCurrency = Currency(
            id = 1,
            currencyName = "US Dollar",
            currencyCode = "USD",
            symbol = "$",
            displayType = "prefix",
            image = null
        )

        val previewCategory = Category(
            id = 1,
            metaData = "food0",
            title = "Food & Beverage",
            icon = "ic_category_foodndrink",
            type = CategoryType.EXPENSE,
            parentName = "food"
        )

        val previewState = AddTransactionState(
            amount = 123.45,
            description = "Lunch with colleagues",
            transactionDate = Date(),
            category = previewCategory,
            showMoreDetails = false,
            showNumpad = false,
            wallet = Wallet(
                id = 1,
                walletName = "Cash",
                currentBalance = 1000.0,
                currency = previewCurrency,
                isMainWallet = true
            ),
            suggestedAmounts = listOf("10.00", "20.00", "50.00", "100.00"),
            withContacts = listOf(
                Contact(id = "1", name = "John Doe", initial = 'J', isSelected = true)
            ),
            eventName = "",
            reminderSet = false,
            excludeFromReport = false
        )

        AddTransactionContent(
            state = previewState,
            onIntent = {},
            onCloseClick = {},
            onCategoryClick = {},
            title = "Add Transaction",
            saveButtonText = "Save"
        )
    }
}