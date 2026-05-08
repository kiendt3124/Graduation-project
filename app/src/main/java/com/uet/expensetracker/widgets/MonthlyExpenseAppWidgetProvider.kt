package com.uet.expensetracker.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
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
import java.util.Calendar

@AndroidEntryPoint
class MonthlyExpenseAppWidgetProvider : AppWidgetProvider() {

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
                // Calculate start of month
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = cal.timeInMillis

                // Fetch transactions
                val allTxns = transactionRepository.getAllTransactions().first()
                // Sum outflows from start of month
                val total = allTxns.filter { txn ->
                    txn.transactionType == TransactionType.OUTFLOW &&
                    txn.transactionDate.time >= startOfMonth
                }.sumOf { it.amount }

                // Prepare UI
                val label = "Tháng này"
                val totalText = formatAmountWithCurrency(total, "₫")

                val views = RemoteViews(context.packageName, R.layout.widget_monthly_expense)
                views.setTextViewText(R.id.tvMonthLabel, label)
                views.setTextViewText(R.id.tvMonthlyTotal, totalText)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 