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

@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class TodayIncomeAppWidgetProvider : AppWidgetProvider() {

    @javax.inject.Inject
    lateinit var transactionRepository: TransactionRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { widgetId ->
            scope.launch {
                // Determine today's epoch millis range
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val startOfNext = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

                // Fetch all transactions
                val allTxns = transactionRepository.getAllTransactions().first()
                // Sum only inflows for today
                val totalIncome = allTxns.filter { txn ->
                    txn.transactionType == TransactionType.INFLOW &&
                    txn.transactionDate.time in startOfDay until startOfNext
                }.sumOf { it.amount }

                // Prepare UI
                val label = "Hôm nay"
                val totalText = formatAmountWithCurrency(totalIncome, "₫")

                val views = RemoteViews(context.packageName, R.layout.widget_today_income)
                views.setTextViewText(R.id.tvTodayLabel, label)
                views.setTextViewText(R.id.tvTodayIncome, totalText)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 