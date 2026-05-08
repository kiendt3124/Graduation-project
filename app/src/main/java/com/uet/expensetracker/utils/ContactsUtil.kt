package com.uet.expensetracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.uet.expensetracker.features.money.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for handling contacts-related operations
 */
object ContactsUtil {

    /**
     * Check if the app has contacts permission
     */
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all contacts from the device
     * This function should be called from a background thread
     */
    suspend fun getAllContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission(context)) {
            return@withContext emptyList()
        }

        val contacts = mutableListOf<Contact>()
        val contactsMap = mutableMapOf<String, Contact>()

        try {
            // First, get all contacts with their display names
            val contactsCursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )

            contactsCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val hasPhoneNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                    if (hasPhoneNumber > 0 && !displayName.isNullOrEmpty()) {
                        // Get the first letter of the contact name for initial
                        val initial = displayName.firstOrNull()?.takeIf { it.isLetter() }?.uppercaseChar()

                        val contact = Contact(
                            id = contactId,
                            name = displayName,
                            initial = initial,
                            phoneNumber = null, // Will be populated below
                            isSelected = false
                        )
                        contactsMap[contactId] = contact
                    }
                }
            }

            // Now get phone numbers for contacts that have them
            val phonesCursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE
                ),
                null,
                null,
                null
            )

            phonesCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                    val phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                    contactsMap[contactId]?.let { contact ->
                        // If contact doesn't have a phone number yet, or this is a mobile number, update it
                        if (contact.phoneNumber == null) {
                            contactsMap[contactId] = contact.copy(phoneNumber = phoneNumber?.replace("\\s".toRegex(), ""))
                        }
                    }
                }
            }

            // Convert map to list and filter out contacts without phone numbers
            contacts.addAll(contactsMap.values.filter { !it.phoneNumber.isNullOrEmpty() })

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext contacts
    }

    /**
     * Search contacts by name or phone number
     */
    fun searchContacts(contacts: List<Contact>, query: String): List<Contact> {
        if (query.isBlank()) return contacts

        val searchQuery = query.lowercase().trim()
        return contacts.filter { contact ->
            contact.name.lowercase().contains(searchQuery) ||
            contact.phoneNumber?.contains(searchQuery) == true
        }
    }

    /**
     * Group contacts by their initial letter
     */
    fun groupContactsByInitial(contacts: List<Contact>): Map<Char?, List<Contact>> {
        return contacts.groupBy { it.initial }
    }

    /**
     * Validate phone number format
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        
        // Remove all non-digit characters for validation
        val digitsOnly = phoneNumber.replace(Regex("[^\\d]"), "")
        
        // Check if it has at least 10 digits (minimum for most phone numbers)
        return digitsOnly.length >= 10
    }

    /**
     * Format phone number for display
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        val digitsOnly = phoneNumber.replace(Regex("[^\\d]"), "")
        
        return when {
            digitsOnly.length == 10 -> {
                "${digitsOnly.substring(0, 3)}-${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
            }
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
                "+1 ${digitsOnly.substring(1, 4)}-${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7)}"
            }
            else -> phoneNumber // Return original if we can't format it
        }
    }

    /**
     * Create a contact from a phone number
     */
    fun createContactFromPhoneNumber(phoneNumber: String): Contact {
        return Contact(
            id = "phone_$phoneNumber",
            name = phoneNumber,
            initial = null,
            phoneNumber = phoneNumber,
            isSelected = false
        )
    }
} 