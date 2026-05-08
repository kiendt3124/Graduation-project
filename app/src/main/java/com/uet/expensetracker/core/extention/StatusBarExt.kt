package com.uet.expensetracker.core.extention

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Extension function to set status bar color and icon appearance
 * @param color The color for the status bar (default: transparent)
 * @param isLightStatusBar Whether to use dark icons on light background (true) or light icons on dark background (false)
 */
@Suppress("DEPRECATION")
fun Activity.setStatusBarStyle(
    color: Int = Color.TRANSPARENT,
    isLightStatusBar: Boolean = false
) {
    try {
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = color

                val controller = WindowInsetsControllerCompat(window, window.decorView)
                if (isLightStatusBar) {
                    controller.isAppearanceLightStatusBars = true
                } else {
                    controller.isAppearanceLightStatusBars = false
                }
            } else {
                // Android 6.0+ (API 23+)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = color

                // Allow content to draw under the status bar
                val decorView = window.decorView
                var flags = decorView.systemUiVisibility
                flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                flags = if (isLightStatusBar) {
                    // Dark icons on light background
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    // Light icons on dark background
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }

                decorView.systemUiVisibility = flags
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

/**
 * Convenience function to set dark status bar (light background with dark icons)
 */
fun Activity.setLightStatusBar(color: Int = Color.WHITE) {
    setStatusBarStyle(color, isLightStatusBar = true)
}

/**
 * Convenience function to set light status bar (dark background with light icons)
 */
fun Activity.setDarkStatusBar(color: Int = Color.TRANSPARENT) {
    setStatusBarStyle(color, isLightStatusBar = false)
} 