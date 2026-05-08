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

@AndroidEntryPoint
class CategoryExpenseAppWidgetProvider : AppWidgetProvider() {

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
                // Fetch all transactions
                val allTxns = transactionRepository.getAllTransactions().first()
                // Handle category parameter
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val categoryArg = options.getString("category")
                // Filter outflows by category if provided
                val filteredTxns = allTxns.filter { txn ->
                    txn.transactionType == TransactionType.OUTFLOW &&
                    (categoryArg == null || txn.category.metaData.equals(categoryArg, ignoreCase = true))
                }
                val total = filteredTxns.sumOf { it.amount }

                // Prepare UI
                val label = categoryArg?.replaceFirstChar { it.uppercase() } ?: "Category"
                val totalText = formatAmountWithCurrency(total, "₫")

                val views = RemoteViews(context.packageName, R.layout.widget_category_expense)
                views.setTextViewText(R.id.tvCategoryLabel, label)
                views.setTextViewText(R.id.tvCategoryTotal, totalText)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 