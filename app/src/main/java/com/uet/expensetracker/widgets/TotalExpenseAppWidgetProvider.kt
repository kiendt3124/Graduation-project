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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class TotalExpenseAppWidgetProvider : AppWidgetProvider() {

    @javax.inject.Inject
    lateinit var transactionRepository: TransactionRepository

    private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            widgetScope.launch {
                // Determine today's epoch millis range
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val startOfNext = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

                // Fetch all transactions
                val allTxns = transactionRepository.getAllTransactions().first()
                // Sum only outflows for today
                val totalExpense = allTxns.filter { txn ->
                    txn.transactionType == TransactionType.OUTFLOW &&
                    txn.transactionDate.time in startOfDay until startOfNext
                }.sumOf { it.amount }

                // Format values
                val dateText = today.format(dateFormatter)
                val totalText = formatAmountWithCurrency(totalExpense, "₫")

                // Update RemoteViews
                val views = RemoteViews(context.packageName, R.layout.widget_total_expense)
                views.setTextViewText(R.id.tvDate, dateText)
                views.setTextViewText(R.id.tvTotalExpense, totalText)

                // Push update
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
} 