package com.uet.expensetracker.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import com.uet.expensetracker.utils.formatAmountWithCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BalanceAppWidgetProvider : AppWidgetProvider() {

    @javax.inject.Inject
    lateinit var walletRepository: WalletRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { widgetId ->
            scope.launch {
                // Fetch default wallet
                val walletEntity = walletRepository.getDefaultWallet().first()
                // Prepare values
                val name = walletEntity?.wallet?.walletName ?: "Ví mặc định"
                val balance = walletEntity?.wallet?.currentBalance ?: 0.0
                val symbol = walletEntity?.currency?.symbol ?: "₫"
                val balanceText = formatAmountWithCurrency(balance, symbol)

                // Update UI
                val views = RemoteViews(context.packageName, R.layout.widget_balance)
                views.setTextViewText(R.id.tvWalletName, name)
                views.setTextViewText(R.id.tvBalance, balanceText)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
} 