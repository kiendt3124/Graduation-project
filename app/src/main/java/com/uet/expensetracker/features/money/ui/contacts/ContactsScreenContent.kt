package com.uet.expensetracker.features.money.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Contact

// Define colors based on the image
private val DarkBackgroundColor = Color(0xFF1C1C1E)
private val SelectedItemColor = Color(0xFF4A4A4C)
private val TextColorPrimary = Color.White
private val TextColorSecondary = Color.LightGray
private val AccentColor = Color(0xFF34C759)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreenContent(
    state: ContactsState,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedContactNames = state.selectedContacts.joinToString(", ") { it.name }

    Scaffold(
        modifier = modifier,
        containerColor = DarkBackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contacts_with_title), color = TextColorPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.contacts_back_cd),
                            tint = AccentColor
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.contacts_done_button), color = AccentColor, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackgroundColor,
                    titleContentColor = TextColorPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Selected contacts display
            if (selectedContactNames.isNotEmpty()) {
                Text(
                    text = selectedContactNames,
                    color = AccentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp
                )
            }

            // Phone number / Search display area
            if (state.selectedContacts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firstSelectedContactWithNumber = state.selectedContacts.firstOrNull { it.phoneNumber != null }
                    Text(
                        text = firstSelectedContactWithNumber?.phoneNumber?.firstOrNull()?.toString() ?: " ",
                        color = AccentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = firstSelectedContactWithNumber?.phoneNumber?.drop(1) ?: "",
                        color = TextColorSecondary,
                        fontSize = 16.sp
                    )
                }
            }
            
            HorizontalDivider(color = SelectedItemColor, thickness = 1.dp)

            // Content based on state
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                }
                
                state.filteredContacts.isEmpty() && !state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) stringResource(R.string.contacts_no_contacts_found) else stringResource(R.string.contacts_no_contacts_available),
                            color = TextColorSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
                
                else -> {
                    ContactsList(
                        contacts = state.filteredContacts,
                        onContactClick = onContactClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        var lastInitial: Char? = null
        itemsIndexed(contacts) { index, contact ->
            val showInitial = contact.initial != null && contact.initial != lastInitial
            if (showInitial) {
                lastInitial = contact.initial
            }
            
            ContactItem(
                contact = contact,
                showInitial = showInitial,
                onContactClick = onContactClick
            )
            
            if (index < contacts.size - 1) {
                HorizontalDivider(
                    color = SelectedItemColor.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = if (contact.initial != null) 48.dp else 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    showInitial: Boolean,
    onContactClick: (Contact) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (contact.isSelected) SelectedItemColor else Color.Transparent)
            .clickable { onContactClick(contact) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(32.dp)) {
            if (showInitial && contact.initial != null) {
                Text(
                    text = contact.initial.toString(),
                    color = AccentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = contact.name,
            color = TextColorPrimary,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        if (contact.isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.contacts_selected_cd),
                tint = AccentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun ContactsScreenContentPreview() {
    val sampleContacts = listOf(
        Contact(id = "1", name = "Anh Bạch Thành", initial = 'A', phoneNumber = "84551245124", isSelected = true),
        Contact(id = "2", name = "Anh Cường Con Bác Cương", initial = null),
        Contact(id = "3", name = "Anh Duy", initial = null),
        Contact(id = "4", name = "Anh Tình", initial = 'A', isSelected = true)
    )
    
    val sampleState = ContactsState(
        contacts = sampleContacts,
        filteredContacts = sampleContacts,
        selectedContacts = sampleContacts.filter { it.isSelected },
        hasContactsPermission = true
    )
    
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackgroundColor) {
            ContactsScreenContent(
                state = sampleState,
                onNavigateBack = {},
                onDone = {},
                onContactClick = {}
            )
        }
    }
} 