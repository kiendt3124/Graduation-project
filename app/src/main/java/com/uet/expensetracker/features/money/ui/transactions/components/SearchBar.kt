package com.uet.expensetracker.features.money.ui.transactions.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.uet.expensetracker.R
import com.uet.expensetracker.ui.theme.*

/**
 * Search bar component with real-time search and suggestions
 * 
 * @param searchText Current search query
 * @param suggestions List of search suggestions to display
 * @param isSearching Whether search is in progress
 * @param placeholder Placeholder text for the search field
 * @param onSearchTextChange Callback when search text changes
 * @param onSuggestionClick Callback when a suggestion is clicked
 * @param onSearchSubmit Callback when search is submitted
 * @param onClearSearch Callback to clear the search
 * @param onToggleFilters Callback to toggle filter panel
 * @param hasActiveFilters Whether there are active filters applied
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchText: String,
    suggestions: List<String> = emptyList(),
    isSearching: Boolean = false,
    placeholder: String = "Search transactions...",
    onSearchTextChange: (String) -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onSearchSubmit: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    onToggleFilters: () -> Unit = {},
    hasActiveFilters: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val showSuggestions = isFocused && suggestions.isNotEmpty() && searchText.isNotBlank()

    Column(modifier = modifier) {
        // Search Input Field
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColor.Light.PrimaryColor.cardColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Icon
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isFocused) TextAccent else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Search TextField
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = TextAccent
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchSubmit()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Loading indicator
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TextAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // Clear button
                            if (searchText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onClearSearch()
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Filter toggle button
                IconButton(
                    onClick = onToggleFilters,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = "Filters",
                            tint = if (hasActiveFilters) TextAccent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        if (hasActiveFilters) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .offset(x = 12.dp, y = (-1).dp)
                                    .background(
                                        AppColor.Light.ExpenseAmountColor,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        // Suggestions Dropdown
        AnimatedVisibility(
            visible = showSuggestions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .zIndex(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColor.Light.PrimaryColor.cardColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            searchText = searchText,
                            onClick = {
                                onSuggestionClick(suggestion)
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Handle focus changes
    LaunchedEffect(isFocused) {
        // Logic to handle focus changes if needed
    }
}

/**
 * Individual suggestion item with highlighting
 */
@Composable
private fun SuggestionItem(
    suggestion: String,
    searchText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = "Recent search",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = suggestion,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = "Use suggestion",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
fun SearchBarPreview() {
    ExpenseTrackerTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Empty state
            SearchBar(
                searchText = "",
                onSearchTextChange = {},
                modifier = Modifier.fillMaxWidth()
            )
            
            // With text and suggestions
            SearchBar(
                searchText = "cafe",
                suggestions = listOf(
                    "Cafe Trung Nguyên",
                    "Cafe phố",
                    "Cafe sáng"
                ),
                hasActiveFilters = true,
                onSearchTextChange = {},
                modifier = Modifier.fillMaxWidth()
            )
            
            // Loading state
            SearchBar(
                searchText = "restaurant",
                isSearching = true,
                onSearchTextChange = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 