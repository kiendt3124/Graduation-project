package com.uet.expensetracker.utils

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.uet.expensetracker.R

private const val TAG = "ResourceUtil"
@StringRes
 fun getStringResId(context: Context, name: String): Int {
    return try {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        if (resId == 0) Log.w(TAG, "String resource not found: $name")
        if (resId == 0) R.string.app_name else resId
    } catch (e: Exception) {
        Log.e(TAG, "Error getting string resource ID for $name", e)
        R.string.app_name
    }
}

@DrawableRes
fun getDrawableResId(context: Context, name: String): Int {
    val cleanedName = name.replace(".png", "").replace(".xml", "").lowercase()
    return try {
        val resId =
            context.resources.getIdentifier(cleanedName, "drawable", context.packageName)
        if (resId == 0) {
            Log.w(TAG, "Drawable resource not found: $cleanedName (original: $name)")
            R.drawable.ic_category_placeholder
        } else {
            resId
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting drawable resource ID for $cleanedName (original: $name)", e)
        R.drawable.ic_category_placeholder
    }
}