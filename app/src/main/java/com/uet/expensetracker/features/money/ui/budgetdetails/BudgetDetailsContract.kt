package com.uet.expensetracker.features.money.ui.budgetdetails

import com.uet.expensetracker.core.platform.MviEventBase
import com.uet.expensetracker.core.platform.MviIntentBase
import com.uet.expensetracker.core.platform.MviStateBase
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.Transaction
import java.util.Date

data class GraphPoint(val date: Date, val value: Double)
data class GraphData(val points: List<GraphPoint> = emptyList())

// State representing the UI
data class BudgetDetailsState(
    val budget: Budget? = null,
    val transactions: List<Transaction> = emptyList(),
    val graphData: GraphData = GraphData(),
    val recommendedDailySpending: Double = 0.0,
    val projectedSpending: Double = 0.0,
    val actualDailySpending: Double = 0.0,
    val spentAmount: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
) : MviStateBase

// Intents representing user actions
sealed interface BudgetDetailsIntent : MviIntentBase {
    object LoadBudgetDetails : BudgetDetailsIntent
    object EditBudget : BudgetDetailsIntent
    object DeleteBudget : BudgetDetailsIntent
    object ShowTransactions : BudgetDetailsIntent
    object NavigateBack : BudgetDetailsIntent
}

// Events for one-time actions like navigation or showing a toast
sealed interface BudgetDetailsEvent : MviEventBase {
    object NavigateToEditBudget : BudgetDetailsEvent
    object NavigateToTransactions : BudgetDetailsEvent
    object NavigateBack : BudgetDetailsEvent
    data class ShowDeleteConfirmation(val budgetId: String) :
        BudgetDetailsEvent // Assuming budget has an ID
    data class ShowError(val message: String) : BudgetDetailsEvent
}
