package com.uet.expensetracker.features.money.ui.addbudget

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.usecases.CheckBudgetExistsByCategoryUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.SaveBudgetUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetBudgetByIdUseCase
import com.uet.expensetracker.utils.CalculatorUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.uet.expensetracker.features.money.ui.addbudget.AddBudgetIntent.SelectStartDate
import com.uet.expensetracker.features.money.ui.addbudget.AddBudgetIntent.SelectEndDate

@HiltViewModel
class AddBudgetViewModel @Inject constructor(
    private val savebudgetUseCase: SaveBudgetUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val checkBudgetExistsByCategoryUseCase: CheckBudgetExistsByCategoryUseCase,
    private val getBudgetByIdUseCase: GetBudgetByIdUseCase,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<AddBudgetState, AddBudgetIntent, AddBudgetEvent>(),
    CalculatorUtil.CalculatorCallback {

    override val _viewState = MutableStateFlow(AddBudgetState())

    // Initialize the calculator utility
    private val calculatorUtil = CalculatorUtil(this)

    // If editing, capture the budget ID from navigation
    private val budgetId: Int? = savedStateHandle.get<Int>("budgetId")?.takeIf { it >= 0 }

    init {
        budgetId?.let { loadBudgetForEdit(it) } ?: run {
            loadWallets()
        }
    }

    private fun loadWallets() {
        getWalletsUseCase(UseCase.None(), viewModelScope) { result ->
            result.fold(
                { failure ->
                    handleFailure(failure)
                    emitEvent(AddBudgetEvent.ShowError("Failed to load wallets"))
                },
                { flowWallets ->
                    viewModelScope.launch {
                        try {
                            flowWallets.collect { wallets ->
                                val mainWallet = wallets.find { it.isMainWallet }

                                // Pre-fill dates for new budget: first and last days of current month
                                val startCal = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val endCal = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }

                                _viewState.value = _viewState.value.copy(
                                    availableWallets = wallets,
                                    selectedWallet = mainWallet,
                                    startDate = startCal.time,
                                    endDate = endCal.time
                                )

                                // Set currency for calculator if a wallet is selected
                                mainWallet?.let {
                                    calculatorUtil.setCurrency(it.currency)
                                }
                            }
                        } catch (e: Exception) {
                            handleFailure(Failure.ServerError)
                            emitEvent(AddBudgetEvent.ShowError("Failed to load wallets"))
                        }
                    }
                }
            )
        }
    }

    // Load existing budget data for editing
    private fun loadBudgetForEdit(budgetId: Int) {
        // show loading
        _viewState.value = _viewState.value.copy(isLoading = true)
        getBudgetByIdUseCase(budgetId, viewModelScope) { result ->
            result.fold(
                { failure ->
                    _viewState.value = _viewState.value.copy(isLoading = false)
                    emitEvent(AddBudgetEvent.ShowError("Failed to load budget"))
                },
                { budget ->
                    // set currency for calculator
                    calculatorUtil.setCurrency(budget.wallet.currency)
                    calculatorUtil.updateAmountFromAssistant(budget.amount.toString())
                    // pre-fill form fields
                    _viewState.value = _viewState.value.copy(
                        selectedCategory = budget.category,
                        amount = budget.amount,
                        amountInput = budget.amount.toString(),
                        formattedAmount = budget.amount.toString(),
                        displayExpression = budget.amount.toString(),
                        startDate = budget.fromDate,
                        endDate = budget.endDate,
                        selectedWallet = budget.wallet,
                        existingBudgetId = budget.budgetId,
                        isEditMode = true,
                        isLoading = false
                    )
                }
            )
        }
    }

    override fun processIntent(intent: AddBudgetIntent) {
        when (intent) {
            is AddBudgetIntent.SelectCategory -> {
                _viewState.value = _viewState.value.copy(selectedCategory = intent.category)
            }

            is AddBudgetIntent.UpdateAmount -> {
                _viewState.value = _viewState.value.copy(amount = intent.amount)
            }

            is SelectStartDate -> {
                _viewState.value = _viewState.value.copy(startDate = intent.date)
            }

            is SelectEndDate -> {
                _viewState.value = _viewState.value.copy(endDate = intent.date)
            }

            is AddBudgetIntent.ToggleRepeat -> {
                _viewState.value = _viewState.value.copy(isRepeating = intent.isRepeating)
            }

            is AddBudgetIntent.SelectWallet -> {
                _viewState.value = _viewState.value.copy(
                    selectedWallet = intent.wallet,
                    isTotal = false
                )

                // Update calculator currency when wallet changes
                intent.wallet.let {
                    calculatorUtil.setCurrency(it.currency)
                }
            }

            is AddBudgetIntent.ToggleTotal -> {
                // Only toggle the total flag without changing selectedWallet
                _viewState.value = _viewState.value.copy(isTotal = intent.isTotal)
            }

            is AddBudgetIntent.LoadWallets -> {
                loadWallets()
            }

            is AddBudgetIntent.SaveBudget -> {
                calculatorUtil.handleSaveAmount()
            }

            is AddBudgetIntent.NavigateToSelectCategory -> {
                emitEvent(AddBudgetEvent.NavigateToSelectCategory)
            }

            is AddBudgetIntent.NavigateToSelectDate -> {
                emitEvent(AddBudgetEvent.NavigateToSelectDate)
            }

            is AddBudgetIntent.NavigateToSelectWallet -> {
                emitEvent(AddBudgetEvent.NavigateToSelectWallet)
            }

            is AddBudgetIntent.ToggleNumpad -> {
                _viewState.value = _viewState.value.copy(
                    showNumpad = !_viewState.value.showNumpad
                )
            }

            is AddBudgetIntent.NumpadButtonPressed -> {
                // Delegate to calculator utility
                calculatorUtil.handleNumpadButtonPressed(intent.button)
            }

            is AddBudgetIntent.ConfirmOverride -> {
                proceedWithSaveBudget()
            }

        }
    }

    // CalculatorCallback implementations
    override fun onCalculatorUpdate(
        amount: String,
        formattedAmount: String,
        displayExpression: String
    ) {
        _viewState.value = _viewState.value.copy(
            amountInput = amount,
            amount = amount.toDoubleOrNull() ?: 0.0,
            formattedAmount = formattedAmount,
            displayExpression = displayExpression
        )
    }

    override fun onError(message: String, level: CalculatorUtil.CalculatorError) {
        when (level) {
            CalculatorUtil.CalculatorError.ERROR ->
                emitEvent(AddBudgetEvent.ShowError(message))

            CalculatorUtil.CalculatorError.WARNING,
            CalculatorUtil.CalculatorError.INFO ->
                emitEvent(AddBudgetEvent.ShowInfo(message))
        }
    }

    override fun onSaveAmount(amount: String, formattedAmount: String) {
        _viewState.value = _viewState.value.copy(
            amountInput = amount,
            amount = amount.toDoubleOrNull() ?: 0.0,
            formattedAmount = formattedAmount,
            showNumpad = false
        )
        saveBudget()
    }

    private fun saveBudget() {
        val state = _viewState.value

        if (state.selectedCategory == null) {
            emitEvent(AddBudgetEvent.ShowError("Please select a category"))
            return
        }

        if (state.amount <= 0) {
            emitEvent(AddBudgetEvent.ShowError("Please enter a valid amount"))
            return
        }

        if (state.selectedWallet == null) {
            emitEvent(AddBudgetEvent.ShowError("Please select a wallet"))
            return
        }

        // If editing, skip existence check and update directly
        if (state.isEditMode) {
            proceedWithSaveBudget()
            return
        }

        // Check if budget already exists for this category and wallet
        val categoryId = state.selectedCategory.id
        val walletId = if (state.isTotal) -1 else state.selectedWallet.id
        
        checkBudgetExistsByCategoryUseCase(
            CheckBudgetExistsByCategoryUseCase.Params(
                categoryId = categoryId,
                walletId = walletId
            ),
            viewModelScope
        ) { result ->
            result.fold(
                { failure ->
                    handleFailure(failure)
                    emitEvent(AddBudgetEvent.ShowError("Failed to check existing budgets"))
                },
                { result ->
                    if (result.exists) {
                        // Budget exists, ask for confirmation and store the existing budget ID
                        _viewState.value = _viewState.value.copy(
                            existingBudgetId = result.existingBudgetId
                        )
                        emitEvent(AddBudgetEvent.ShowConfirmOverride(categoryId))
                    } else {
                        // No existing budget, proceed with saving
                        proceedWithSaveBudget()
                    }
                }
            )
        }
    }

    private fun proceedWithSaveBudget() {
        val state = _viewState.value
        // Use explicitly selected start and end dates
        val fromDate = state.startDate
        val endDate = state.endDate

        val budget = Budget(
            budgetId = state.existingBudgetId ?: 0, // Use existing ID if replacing, otherwise 0 for new
            category = state.selectedCategory!!,
            wallet = state.selectedWallet!!,
            amount = state.amount,
            fromDate = fromDate,
            endDate = endDate,
            isRepeating = state.isRepeating
        )

        _viewState.value = _viewState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                savebudgetUseCase(budget)
                _viewState.value = _viewState.value.copy(
                    isLoading = false,
                    existingBudgetId = null // Reset the existing budget ID
                )
                emitEvent(AddBudgetEvent.BudgetSaved)
                emitEvent(AddBudgetEvent.NavigateBack)
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(isLoading = false)
                handleFailure(Failure.ServerError)
                emitEvent(AddBudgetEvent.ShowError("Failed to save budget"))
            }
        }
    }
}