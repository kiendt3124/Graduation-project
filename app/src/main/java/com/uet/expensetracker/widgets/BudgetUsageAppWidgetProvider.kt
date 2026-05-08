package com.uet.expensetracker.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.utils.formatAmountWithCurrency
import com.uet.expensetracker.utils.getStringResId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class BudgetUsageAppWidgetProvider : AppWidgetProvider() {

    @javax.inject.Inject
    lateinit var budgetRepository: BudgetRepository

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
                val today = Date()
                // Get active budgets for today
                val budgets = budgetRepository.getActiveBudgetsForDate(today).first()
                val budget: Budget? = budgets.firstOrNull()

                // Prepare default values
                var categoryLabel = "-"
                var progressText = "-"

                if (budget != null) {
                    // Fetch transactions and filter by category and budget period
                    val allTxns = transactionRepository.getAllTransactions().first()
                    val spent = allTxns.filter { txn ->
                        txn.transactionType == TransactionType.OUTFLOW &&
                        txn.category.id == budget.category.id &&
                        txn.transactionDate.time in budget.fromDate.time..budget.endDate.time
                    }.sumOf { it.amount }
                    val limit = budget.amount

                    categoryLabel = budget.category.title
                    progressText = "${formatAmountWithCurrency(spent, "₫")} / ${formatAmountWithCurrency(limit, "₫")}"
                }

                val views = RemoteViews(context.packageName, R.layout.widget_budget_usage)
                views.setTextViewText(R.id.tvBudgetLabel, context.getString(getStringResId(context, categoryLabel)))
                views.setTextViewText(R.id.tvBudgetProgress, progressText)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 