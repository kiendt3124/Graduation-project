package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive search filters panel
 * 
 * @param minAmount Minimum amount filter
 * @param maxAmount Maximum amount filter  
 * @param selectedCategoryIds Selected category IDs
 * @param availableCategoryIds Available category IDs to choose from
 * @param selectedTransactionType Selected transaction type
 * @param startDate Start date filter in milliseconds
 * @param endDate End date filter in milliseconds
 * @param onMinAmountChange Callback when min amount changes
 * @param onMaxAmountChange Callback when max amount changes
 * @param onCategoryToggle Callback when category is toggled
 * @param onTransactionTypeChange Callback when transaction type changes
 * @param onDateRangeChange Callback when date range changes
 * @param onClearFilters Callback to clear all filters
 * @param onApplyFilters Callback to apply filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersPanel(
    minAmount: Double? = null,
    maxAmount: Double? = null,
    selectedCategoryIds: List<Int> = emptyList(),
    availableCategoryIds: List<Int> = emptyList(),
    selectedTransactionType: TransactionType? = null,
    startDate: Long? = null,
    endDate: Long? = null,
    onMinAmountChange: (Double?) -> Unit = {},
    onMaxAmountChange: (Double?) -> Unit = {},
    onCategoryToggle: (Int) -> Unit = {},
    onTransactionTypeChange: (TransactionType?) -> Unit = {},
    onDateRangeChange: (Long?, Long?) -> Unit = { _, _ -> },
    onClearFilters: () -> Unit = {},
    onApplyFilters: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var minAmountText by remember(minAmount) { mutableStateOf(minAmount?.toString() ?: "") }
    var maxAmountText by remember(maxAmount) { mutableStateOf(maxAmount?.toString() ?: "") }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColor.Light.PrimaryColor.containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Filters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                TextButton(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColor.Light.ExpenseAmountColor
                    )
                ) {
                    Text("Clear All")
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Amount Range Section
            FilterSection(
                title = "Money Range",
                icon = Icons.Default.Create
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { 
                            minAmountText = it
                            onMinAmountChange(it.toDoubleOrNull())
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("From", color = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextAccent,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    
                    Text(
                        text = "—",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { 
                            maxAmountText = it
                            onMaxAmountChange(it.toDoubleOrNull())
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("To", color = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextAccent,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Transaction Type Section
            FilterSection(
                title = "Loại giao dịch",
                icon = Icons.Default.Create
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TransactionTypeChip(
                            type = null,
                            label = "All",
                            isSelected = selectedTransactionType == null,
                            onClick = { onTransactionTypeChange(null) }
                        )
                    }
                    item {
                        TransactionTypeChip(
                            type = TransactionType.INFLOW,
                            label = "Income",
                            isSelected = selectedTransactionType == TransactionType.INFLOW,
                            onClick = { onTransactionTypeChange(TransactionType.INFLOW) }
                        )
                    }
                    item {
                        TransactionTypeChip(
                            type = TransactionType.OUTFLOW,
                            label = "Expense",
                            isSelected = selectedTransactionType == TransactionType.OUTFLOW,
                            onClick = { onTransactionTypeChange(TransactionType.OUTFLOW) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Categories Section
            if (availableCategoryIds.isNotEmpty()) {
                FilterSection(
                    title = "Danh mục",
                    icon = Icons.Default.Create
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableCategoryIds) { categoryId ->
                            CategoryChip(
                                categoryId = categoryId,
                                isSelected = selectedCategoryIds.contains(categoryId),
                                onClick = { onCategoryToggle(categoryId) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Date Range Section
            FilterSection(
                title = "Khoảng thời gian",
                icon = Icons.Default.DateRange
            ) {
                DateRangeSelector(
                    startDate = startDate,
                    endDate = endDate,
                    onDateRangeChange = onDateRangeChange
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClearFilters,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text("Clear Filters")
                }
                
                Button(
                    onClick = onApplyFilters,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

/**
 * Filter section with title and icon
 */
@Composable
private fun FilterSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        content()
    }
}

/**
 * Transaction type filter chip
 */
@Composable
private fun TransactionTypeChip(
    type: TransactionType?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) TextAccent else Color.Transparent
    val contentColor = if (isSelected) Color.White else TextSecondary
    val borderColor = if (isSelected) TextAccent else TextSecondary
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Category filter chip
 */
@Composable
private fun CategoryChip(
    categoryId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) TextAccent else Color.Transparent
    val contentColor = if (isSelected) Color.White else TextSecondary
    val borderColor = if (isSelected) TextAccent else TextSecondary
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Cat $categoryId", // In real app, would map to category name
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Date range selector component
 */
@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onDateRangeChange: (Long?, Long?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date
            OutlinedTextField(
                value = startDate?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = { },
                modifier = Modifier.weight(1f),
                placeholder = { Text("From date", color = TextSecondary) },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select start date",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextAccent,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            
            // End Date
            OutlinedTextField(
                value = endDate?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = { },
                modifier = Modifier.weight(1f),
                placeholder = { Text("To date", color = TextSecondary) },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select end date",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextAccent,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }
        
        // Quick date range buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                QuickDateButton(
                    label = "7 days",
                    onClick = {
                        val now = System.currentTimeMillis()
                        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)
                        onDateRangeChange(sevenDaysAgo, now)
                    }
                )
            }
            item {
                QuickDateButton(
                    label = "30 days",
                    onClick = {
                        val now = System.currentTimeMillis()
                        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000)
                        onDateRangeChange(thirtyDaysAgo, now)
                    }
                )
            }
            item {
                QuickDateButton(
                    label = "3 months",
                    onClick = {
                        val now = System.currentTimeMillis()
                        val threeMonthsAgo = now - (90 * 24 * 60 * 60 * 1000)
                        onDateRangeChange(threeMonthsAgo, now)
                    }
                )
            }
        }
    }
}

/**
 * Quick date range button
 */
@Composable
private fun QuickDateButton(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = TextAccent
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun SearchFiltersPanelPreview() {
    ExpenseTrackerTheme {
        SearchFiltersPanel(
            minAmount = 10000.0,
            maxAmount = 500000.0,
            selectedCategoryIds = listOf(1, 3),
            availableCategoryIds = listOf(1, 2, 3, 4, 5),
            selectedTransactionType = TransactionType.OUTFLOW,
            startDate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
            endDate = System.currentTimeMillis(),
            modifier = Modifier.fillMaxWidth()
        )
    }
} 