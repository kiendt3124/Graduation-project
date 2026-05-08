package com.uet.expensetracker.features.money.ui.navigation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.uet.expensetracker.core.extention.hideNavigationBar
import com.uet.expensetracker.core.extention.setStatusBarStyle
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "appactions"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val shouldNavigateToAddTransaction = mutableStateOf(false)
    private val incomingIntent = mutableStateOf<Intent?>(null)
    private val shouldShowBalanceSheet = mutableStateOf(false)
    private val shouldShowWeeklyExpenseSheet = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideNavigationBar()
        enableEdgeToEdge()
        setStatusBarStyle(isLightStatusBar = true)

        setContent {
            ExpenseTrackerTheme {
                AppNavigation(
                    shouldNavigateToAddTransaction = shouldNavigateToAddTransaction,
                    incomingIntent = incomingIntent,
                    showBalanceSheet = shouldShowBalanceSheet,
                    onDismissBalanceSheet = { shouldShowBalanceSheet.value = false },
                    showWeeklyExpenseSheet = shouldShowWeeklyExpenseSheet,
                    onDismissWeeklyExpenseSheet = { shouldShowWeeklyExpenseSheet.value = false }
                )
            }
        }

        handleIntent(intent) // Handle intent when the app opens

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Log.i(TAG, "handleIntent: intent: ${intent}")

        if (intent.action == Intent.ACTION_VIEW) {
            when (intent.data?.host) {
                "balance" -> shouldShowBalanceSheet.value = true
                "weeklyexpense" -> shouldShowWeeklyExpenseSheet.value = true
                else -> incomingIntent.value = intent
            }
        } else {
            incomingIntent.value = intent
        }
    }
}
