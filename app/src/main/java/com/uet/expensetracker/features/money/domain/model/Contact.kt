package com.uet.expensetracker.features.money.domain.model

import java.io.Serializable

data class Contact(
    val id: String,
    val name: String,
    val initial: Char?,
    val phoneNumber: String? = null, // Added for completeness, based on image
    var isSelected: Boolean = false
): Serializable