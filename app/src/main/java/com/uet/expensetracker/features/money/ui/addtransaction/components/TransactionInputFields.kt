@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.addtransaction.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.uet.expensetracker.ui.theme.MyIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Contact
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import com.uet.expensetracker.utils.getDrawableResId
import com.uet.expensetracker.utils.getStringResId
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ColorFilter
import com.uet.expensetracker.ui.theme.AppColor.Light.PrimaryColor.contentColor
import com.uet.expensetracker.ui.theme.TextAccent
import com.uet.expensetracker.utils.DateTimeUtils
import java.util.Calendar
import coil.compose.AsyncImage

// Define colors
val InputTextColor = Color(0xFF1E2A36)

/**
 * Main composable for transaction input fields
 * Displays wallet, amount, category, notes, date and additional details
 */
@Composable
fun TransactionInputFields(
    amount: Double = 0.0,
    amountInput: String = "0",
    formattedAmount: String = "0",
    displayExpression: String = "0",
    wallet: Wallet,
    category: Category? = null,
    description: String = "",
    transactionDate: Date = Date(),
    showMoreDetails: Boolean = false,
    withContacts: List<Contact> = emptyList(),
    eventName: String = "",
    reminderSet: Boolean = false,
    excludeFromReport: Boolean = false,
    photoUri: String? = null,
    onAmountClick: () -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onToggleMoreDetails: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    onDateClick: (Date) -> Unit = {},
    onWithPersonClick: () -> Unit = {},
    onContactRemove: (Contact) -> Unit = {},
    onEventClick: () -> Unit = {},
    onReminderClick: () -> Unit = {},
    onExcludeFromReportChange: (Boolean) -> Unit = {},
    onTakePhotoClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onRemovePhoto: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Wallet Row
                InputRow(
                    icon = MyIcons.BusinessCenter,
                    iconPainter = painterResource(
                        getDrawableResId(
                            LocalContext.current,
                            wallet.icon
                        )
                    ),
                    text = if (wallet.walletName.isEmpty()) stringResource(R.string.add_transaction_select_wallet) else wallet.walletName,
                    onClick = onWalletClick
                )
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 48.dp)
                )

                // Amount Row
                DisplayAmountRow(
                    amount = amount,
                    amountText = formattedAmount,
                    displayExpression = displayExpression,
                    currency = wallet.currency.currencyCode.uppercase(Locale.getDefault()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAmountClick)
                        .padding(vertical = 8.dp)
                )
                HorizontalDivider(
                    color = AmountColor.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 48.dp)
                )

                // Category Row
                CategoryRow(
                    category = category,
                    onClick = onCategoryClick
                )
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 48.dp)
                )

                // Note Row
                EditableNoteRow(
                    description = description,
                    onDescriptionChange = onDescriptionChange
                )
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 48.dp)
                )

                // Date Row
                DateRow(
                    icon = Icons.Filled.DateRange,
                    iconPainter = painterResource(R.drawable.ic_calendar),
                    text = DateTimeUtils.formatTransactionDateTime(transactionDate),
                    onClick = { onDateClick(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Add More Details" Button
        AnimatedVisibility(visible = !showMoreDetails) {
            Column(
                modifier = Modifier.padding(top = 0.dp)
            ) {
                Button(
                    onClick = onToggleMoreDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColor.Light.PrimaryColor.TextButtonColor),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.add_transaction_add_more_details), color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // Optional Fields (Shown when button is clicked)
        AnimatedVisibility(visible = showMoreDetails) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // More details section

                // With Person Row
//                Box(
//                    modifier = Modifier.background(
//                        shape = RoundedCornerShape(12.dp),
//                        color = Color.White
//                    )
//                ) {
//                    if (withContacts.isEmpty()) {
//                        InputRow(
//                            icon = MyIcons.Group,
//                            iconPainter = painterResource(R.drawable.ic_with),
//                            text = stringResource(R.string.add_transaction_with_label),
//                            onClick = onWithPersonClick,
//                            modifier = Modifier.padding(horizontal = 16.dp)
//                        )
//                    } else {
//                        ContactsInputRow(
//                            icon = MyIcons.Group,
//                            iconPainter = painterResource(R.drawable.ic_with),
//                            contacts = withContacts,
//                            onClick = onWithPersonClick,
//                            onContactRemove = onContactRemove,
//                            modifier = Modifier.padding(horizontal = 16.dp)
//                        )
//                    }
//                }

                // Event Row
//                    Box(
//                        modifier = Modifier.background(
//                            shape = RoundedCornerShape(12.dp),
//                            color = AppColor.Dark.PrimaryColor.containerColorSecondary
//                        )
//                    ) {
//                        InputRow(
//                            icon = MyIcons.BusinessCenter,
//                            iconPainter = painterResource(R.drawable.ic_events),
//                            text = if (eventName.isEmpty()) "Select event" else eventName,
//                            onClick = onEventClick,
//                            modifier = Modifier.padding(horizontal = 12.dp)
//                        )
//                    }

//                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Reminder Row
//                    Box(
//                        modifier = Modifier.background(
//                            shape = RoundedCornerShape(12.dp),
//                            color = AppColor.Dark.PrimaryColor.containerColorSecondary
//                        )
//                    ) {
//                        InputRow(
//                            icon = MyIcons.Schedule,
//                            text = if (!reminderSet) "No remind" else "Reminder set",
//                            onClick = onReminderClick,
//                            modifier = Modifier.padding(horizontal = 12.dp)
//                        )
//                    }

//                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Photo attachment row
                Box(
                    modifier = Modifier.background(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    )
                ) {
                    AttachmentRow(
                        photoUri = photoUri,
                        onTakePhotoClick = onTakePhotoClick,
                        onGalleryClick = onGalleryClick,
                        onRemovePhoto = onRemovePhoto
                    )
                }

//                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Exclude from report checkbox
//                Box(
//                    modifier = Modifier.background(
//                        shape = RoundedCornerShape(12.dp),
//                        color = Color.White
//                    )
//                ) {
//                    ExcludeFromReportRow(
//                        excludeFromReport = excludeFromReport,
//                        onExcludeFromReportChange = onExcludeFromReportChange
//                    )
//                }

                Text(
                    text = stringResource(R.string.add_transaction_exclude_from_reports),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp, end = 28.dp)
                )
            }

        }
    }
}

/**
 * Reusable row component with icon and text that's clickable
 */
@Composable
private fun InputRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconPainter: Painter? = null,
    text: String,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        iconPainter?.let {
            Image(
                painter = iconPainter,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
            )
        } ?: run {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = InputTextColor
            )
        }

        Text(
            text = text,
            color = InputTextColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.add_transaction_more_cd),
            tint = Color(0xFF505D6D),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DateRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconPainter: Painter? = null,
    text: String,
    onClick: (Date) -> Unit = {},
) {
    val showDatePicker = remember { mutableStateOf(false) }
    val showTimePicker = remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker.value = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        iconPainter?.let {
            Image(
                painter = iconPainter,
                colorFilter = ColorFilter.tint(InputTextColor),
                contentDescription = text,
                modifier = Modifier.size(24.dp),
            )
        } ?: run {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = InputTextColor
            )
        }

        Text(
            text = text,
            color = InputTextColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.add_transaction_more_cd),
            tint = Color(0xFF505D6D),
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDatePicker.value) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            calendar.timeInMillis = millis
                            showDatePicker.value = false
//                            showTimePicker.value = true
                            onClick(Date(calendar.timeInMillis))

                        }
                    }
                ) {
                    Text(stringResource(id = R.string.common_ok))
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(id = R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker.value) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )

        DatePickerDialog(
            onDismissRequest = { showTimePicker.value = false },
            confirmButton = {
                Button(
                    onClick = {
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                        showTimePicker.value = false
//                        onClick(Date(calendar.timeInMillis))
                    }
                ) {
                    Text(stringResource(id = R.string.common_ok))
                }
            },
            dismissButton = {
                Button(onClick = { showTimePicker.value = false }) {
                    Text(stringResource(id = R.string.common_cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun CategoryRow(
    modifier: Modifier = Modifier,
    category: Category? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        category?.icon?.let {
            Image(
                painter = painterResource(getDrawableResId(LocalContext.current, it)),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        } ?: run {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = 4.dp)
                    .background(color = Color.Gray, shape = CircleShape)
            )
        }

            Text(
                text = category?.title?.let {
                    stringResource(
                        getStringResId(
                            LocalContext.current,
                            it
                        )
                    )
                } ?: stringResource(id = R.string.add_transaction_select_category),
                color = if (category != null) Color(0xFF1E2A36) else Color(0xFF505D6D),
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.add_transaction_more_cd),
            tint = Color(0xFF505D6D),
            modifier = Modifier.size(20.dp)
        )
    }
}


/**
 * Editable note field component
 */
@Composable
private fun EditableNoteRow(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_description),
            contentDescription = stringResource(id = R.string.add_transaction_note_cd),
            modifier = Modifier.size(24.dp),
            tint = InputTextColor
        )
        TextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.add_transaction_note_placeholder),
                    color = Color.Gray
                )
            },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = InputTextColor,
                fontSize = 16.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

/**
 * Photo attachment options component
 */
@Composable
private fun AttachmentRow(
    photoUri: String? = null,
    onTakePhotoClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    if (photoUri != null) {
        // Show selected image with options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Display the selected image
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(id = R.string.add_transaction_selected_photo_cd),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Change photo button
                Row(
                    modifier = Modifier
                        .clickable { onTakePhotoClick() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_photo),
                        contentDescription = stringResource(id = R.string.add_transaction_change_photo_cd),
                        tint = TextAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.add_transaction_change_photo),
                        color = TextAccent,
                        fontSize = 14.sp
                    )
                }

                // Remove photo button
                IconButton(
                    onClick = onRemovePhoto
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = stringResource(id = R.string.add_transaction_remove_photo_cd),
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        // Show add photo option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
                .clickable {
                    onTakePhotoClick()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onTakePhotoClick,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_add_photo),
                    contentDescription = stringResource(id = R.string.add_transaction_add_photo_cd),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.add_transaction_add_photo),
                color = InputTextColor
            )
        }
    }
}

/**
 * Row component for exclude from report option
 */
@Composable
private fun ExcludeFromReportRow(
    modifier: Modifier = Modifier,
    excludeFromReport: Boolean,
    onExcludeFromReportChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExcludeFromReportChange(!excludeFromReport) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = stringResource(id = R.string.add_transaction_exclude_from_report_label),
                color = InputTextColor,
                fontSize = 16.sp
            )
        }

//        Checkbox(
//            checked = excludeFromReport,
//            onCheckedChange = onExcludeFromReportChange,
//            colors = CheckboxDefaults.colors(
//                checkedColor = AmountColor,
//                uncheckedColor = Color.Gray
//            )
//        )

        Switch(
            checked = excludeFromReport,
            onCheckedChange = onExcludeFromReportChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                checkedTrackColor = TextAccent,
                uncheckedTrackColor = contentColor.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(end = 12.dp)
        )
    }

}

@Preview()
@Composable
fun TransactionInputFieldsPreview() {
    ExpenseTrackerTheme {
        TransactionInputFields(
            amount = 123.45,
            amountInput = "123.45",
            formattedAmount = "$123.45",
            displayExpression = "123.45",
            wallet = Wallet(
                id = 1,
                walletName = "Cash",
                currentBalance = 1000.0,
                currency = com.uet.expensetracker.features.money.domain.model.Currency(
                    id = 1,
                    currencyName = "US Dollar",
                    currencyCode = "USD",
                    symbol = "$",
                    displayType = "prefix",
                    image = null
                ),
                isMainWallet = true
            ),
            category = Category(
                id = 1,
                metaData = "food0",
                title = "Food & Beverage",
                icon = "ic_category_foodndrink",
                type = CategoryType.EXPENSE,
                parentName = "food"
            ),
            description = "Lunch with colleagues",
            transactionDate = Date(),
            showMoreDetails = true,
            withContacts = listOf(
                Contact(
                    id = "1",
                    name = "John Doe",
                    initial = 'J',
                    isSelected = true
                )
            ),
            eventName = "Team Building",
            reminderSet = true,
            excludeFromReport = false
        )
    }
}

/**
 * Contacts input row that displays selected contacts with remove buttons
 */
@Composable
private fun ContactsInputRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconPainter: Painter? = null,
    contacts: List<Contact>,
    onClick: () -> Unit = {},
    onContactRemove: (Contact) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Header row with icon and "Add" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            iconPainter?.let {
                Image(
                    painter = painterResource(id = R.drawable.ic_with_friend),
                    contentDescription = stringResource(id = R.string.add_transaction_with_label),
                    modifier = Modifier.size(24.dp),
                )
            } ?: run {
                Image(
                    painter = painterResource(id = R.drawable.ic_with_friend),
                    contentDescription = stringResource(id = R.string.add_transaction_with_label),
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = stringResource(id = R.string.add_transaction_with_label),
                color = InputTextColor,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF505D6D)
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.add_transaction_contact_add),
                        fontSize = 14.sp
                    )
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.add_transaction_more_cd),
                    tint = Color(0xFF505D6D),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Contacts chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            contacts.forEach { contact ->
                ContactChip(
                    contact = contact,
                    onRemove = { onContactRemove(contact) }
                )
            }
        }
    }
}

/**
 * Contact chip with remove button
 */
@Composable
fun ContactChip(
    contact: Contact,
    onRemove: () -> Unit = {}
) {
    Surface(
        modifier = Modifier,
        color = AppColor.Light.PrimaryColor.containerColor.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact initial or icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = TextAccent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initial.toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Contact name
            Text(
                text = contact.name,
                color = InputTextColor,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.add_transaction_contact_remove_cd),
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}