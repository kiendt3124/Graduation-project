package com.uet.expensetracker.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.utils.formatAmountWithCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@AndroidEntryPoint
class WeeklyExpenseAppWidgetProvider : AppWidgetProvider() {

    @javax.inject.Inject
    lateinit var transactionRepository: TransactionRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { widgetId ->
            scope.launch {
                // Calculate start of current week (Monday)
                val now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                val startOfWeek = now.with(java.time.DayOfWeek.MONDAY)
                    .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                // Fetch all transactions
                val transactions = transactionRepository.getAllTransactions().first()
                // Sum outflows this week
                val total = transactions.filter { txn ->
                    txn.transactionType == TransactionType.OUTFLOW &&
                    txn.transactionDate.time >= startOfWeek
                }.sumOf { it.amount }

                val label = "Weekly Expense"
                val totalText = formatAmountWithCurrency(total, "₫")

                val views = RemoteViews(context.packageName, R.layout.widget_weekly_expense)
                views.setTextViewText(R.id.tvWeekLabel, label)
                views.setTextViewText(R.id.tvWeeklyTotal, totalText)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 