package com.uet.expensetracker.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val calendar: Calendar = Calendar.getInstance()
    private val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatTransactionDateTime(date: Date, isShowTime: Boolean = false): String {
        val today = Calendar.getInstance()
        calendar.time = date

//        return fullDateFormat.format(
//            date
//        )
        return when {
            isToday(date) -> if (isShowTime) "Today at ${timeFormat.format(date)}" else "Today"
            isYesterday(date) -> if (isShowTime) "Yesterday at ${timeFormat.format(date)}" else "Yesterday"
            else -> if (isShowTime) "${fullDateFormat.format(date)} at ${timeFormat.format(date)}" else fullDateFormat.format(
                date
            )
        }
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        calendar.time = date
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        calendar.time = date
        return yesterday.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }
} 