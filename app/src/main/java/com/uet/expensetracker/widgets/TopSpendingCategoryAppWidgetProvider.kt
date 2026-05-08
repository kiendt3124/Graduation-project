package com.uet.expensetracker.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.utils.formatAmountWithCurrency
import com.uet.expensetracker.utils.getStringResId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@AndroidEntryPoint
class TopSpendingCategoryAppWidgetProvider : AppWidgetProvider() {

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
                // Filter only outflows today
                val todayOutflows = allTxns.filter { txn ->
                    txn.transactionType == TransactionType.OUTFLOW &&
                    txn.transactionDate.time in startOfDay until startOfNext
                }
                // Group by category and find top 3
                val grouped = todayOutflows.groupBy { it.category.metaData }
                val sorted = grouped.map { (cat, txns) -> cat to txns.sumOf { it.amount } }
                    .sortedByDescending { it.second }

                val views = RemoteViews(context.packageName, R.layout.widget_top_spending_category)
                for (i in 0 until 3) {
                    val cat = sorted.getOrNull(i)?.first ?: "-"
                    val amount = sorted.getOrNull(i)?.second ?: 0.0
                    val labelId = when (i) {
                        0 -> R.id.tvTopCategoryLabel1
                        1 -> R.id.tvTopCategoryLabel2
                        else -> R.id.tvTopCategoryLabel3
                    }
                    val totalId = when (i) {
                        0 -> R.id.tvTopCategoryTotal1
                        1 -> R.id.tvTopCategoryTotal2
                        else -> R.id.tvTopCategoryTotal3
                    }
                    views.setTextViewText(labelId, cat) //  context.getString(getStringResId(context, cat))
                    views.setTextViewText(totalId, formatAmountWithCurrency(amount, "₫"))
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 