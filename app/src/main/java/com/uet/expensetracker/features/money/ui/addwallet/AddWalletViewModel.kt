package com.uet.expensetracker.features.money.ui.addwallet

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.platform.BaseViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.usecases.CreateWalletUseCase
import com.uet.expensetracker.features.money.domain.usecases.UpdateWalletUseCase
import com.uet.expensetracker.features.money.domain.usecases.ObserveWalletByIdUseCase
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.utils.CalculatorUtil
import com.uet.expensetracker.utils.CalculatorUtil.CalculatorError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.uet.expensetracker.R
import com.uet.expensetracker.utils.Constants
import com.uet.expensetracker.utils.formatAmount
import com.uet.expensetracker.utils.formatCurrency

@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val createWalletUseCase: CreateWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val observeWalletByIdUseCase: ObserveWalletByIdUseCase,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : BaseViewModel<AddWalletState, AddWalletIntent, AddWalletEvent>(), CalculatorUtil.CalculatorCallback {

    override val _viewState = MutableStateFlow(AddWalletState())
    private val calculator = CalculatorUtil(this)

    // No init, loading now handled via LoadWallet intent
    private val walletId: Int? = savedStateHandle.get<Int>("walletId")?.takeIf { it >= 0 }

    init {
        walletId?.let {
            processIntent(AddWalletIntent.LoadWallet(it))
        }
    }

    override fun processIntent(intent: AddWalletIntent) {
        when (intent) {
            is AddWalletIntent.LoadWallet -> {

                    observeWalletByIdUseCase(ObserveWalletByIdUseCase.Params.ByWalletId(intent.walletId), scope =  viewModelScope) { result ->
                        result.fold(
                            { failure -> emitEvent(AddWalletEvent.ShowError(context.getString(R.string.add_wallet_error_load_wallet))) },
                            { walletFlow ->
                                viewModelScope.launch {
                                    walletFlow.collect { wallet ->
                                        processIntent(AddWalletIntent.SetInitialWallet(wallet))
                                    }
                                }

                            }
                        )
                    }
                
            }
            is AddWalletIntent.SetInitialWallet -> {
                viewModelScope.launch {


                    val w = intent.wallet
                    calculator.updateAmountFromAssistant(w.currentBalance.toString())

                    _viewState.value = _viewState.value.copy(
                        walletName = w.walletName,
                        currency = w.currency,
                        iconString = w.icon,
                        isEditMode = true,
                        walletToEdit = w
                    )
                }
            }
            is AddWalletIntent.UpdateWalletName -> updateWalletName(intent.name)
            is AddWalletIntent.SelectCurrency -> selectCurrency(intent.currency)
            is AddWalletIntent.UpdateInitialBalance -> updateInitialBalance(intent.balance)
            is AddWalletIntent.ToggleNotification -> toggleNotification(intent.enable)
            is AddWalletIntent.ToggleExcludedFromTotal -> toggleExcludedFromTotal(intent.excluded)
            is AddWalletIntent.ToggleNumpad -> toggleNumpad()
            is AddWalletIntent.NumpadButtonPressed -> calculator.handleNumpadButtonPressed(intent.button)
            is AddWalletIntent.NavigateToIconPicker -> navigateToIconPicker()
            is AddWalletIntent.SelectIcon -> selectIcon(intent.iconName)
            is AddWalletIntent.SaveWallet -> saveWallet()
            is AddWalletIntent.NavigateBack -> navigateBack()
            is AddWalletIntent.NavigateToCurrencyScreen -> navigateToCurrencyScreen()
        }
    }

    private fun updateWalletName(name: String) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(walletName = name)
        }
    }

    private fun selectCurrency(currency: Currency) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(currency = currency)
            calculator.setCurrency(currency)
        }
    }

    private fun updateInitialBalance(balance: String) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(initialBalance = balance)
        }
    }

    private fun toggleNotification(enable: Boolean) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(enableNotification = enable)
        }
    }

    private fun toggleExcludedFromTotal(excluded: Boolean) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(excludedFromTotal = excluded)
        }
    }

    private fun toggleNumpad() {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(showNumpad = !_viewState.value.showNumpad)
        }
    }

    override fun onCalculatorUpdate(amount: String, formattedAmount: String, displayExpression: String) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(
                amountInput = amount,
                formattedAmount = formattedAmount,
                displayExpression = displayExpression
            )
        }
    }

    override fun onError(message: String, level: CalculatorError) {
        when (level) {
            CalculatorError.ERROR -> emitEvent(AddWalletEvent.ShowError(message))
            else -> emitEvent(AddWalletEvent.ShowInfo(message))
        }
    }

    override fun onSaveAmount(amount: String, formattedAmount: String) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(
                amountInput = amount,
                formattedAmount = formattedAmount,
                showNumpad = false
            )
        }
    }

    private fun saveWallet() {
        viewModelScope.launch {
            val state = _viewState.value
            val walletName = state.walletName
            val currency = state.currency
            val initialBalance = state.amountInput.toDoubleOrNull() ?: 0.0

            // Input validation
            if (walletName.isBlank()) {
                val errorMsg = context.getString(R.string.add_wallet_error_name_empty)
                _viewState.value = state.copy(error = errorMsg)
                emitEvent(AddWalletEvent.ShowError(errorMsg))
                return@launch
            }

            if (currency == null) {
                val errorMsg = context.getString(R.string.add_wallet_error_select_currency)
                _viewState.value = state.copy(error = errorMsg)
                emitEvent(AddWalletEvent.ShowError(errorMsg))
                return@launch
            }

            _viewState.value = state.copy(isLoading = true, error = null)
            if (state.isEditMode && state.walletToEdit != null) {
                // Update existing wallet
                val entity = state.walletToEdit.copy(
                    walletName = walletName,
                    currentBalance = initialBalance,
                    currency = currency,
                    icon = state.iconString,
                    isMainWallet = state.walletToEdit.isMainWallet
                ).toWalletEntity()
                updateWalletUseCase(UpdateWalletUseCase.Params(entity)) { res ->
                    res.fold(
                        { failure ->
                            _viewState.value = state.copy(
                                isLoading = false,
                                error = context.getString(R.string.add_wallet_error_update_wallet_with_message, failure.javaClass.simpleName)
                            )
                            emitEvent(AddWalletEvent.ShowError(context.getString(R.string.add_wallet_error_update_wallet)))
                        },
                        {
                            _viewState.value = state.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null
                            )
                            emitEvent(AddWalletEvent.ShowSuccess)
                        }
                    )
                }
            } else {
                // Create new wallet
                val walletEntity = WalletEntity(
                    id = 0,
                    walletName = walletName,
                    currentBalance = initialBalance,
                    currencyId = currency.id,
                    icon = state.iconString,
                    isMainWallet = false
                )
                val currencyEntity = currency.toCurrencyEntity()
                val walletWithCurrency = WalletWithCurrencyEntity(
                    wallet = walletEntity,
                    currency = currencyEntity
                )
                createWalletUseCase(CreateWalletUseCase.Params(walletWithCurrency)) { result ->
                    result.fold(
                        { failure ->
                            _viewState.value = state.copy(
                                isLoading = false,
                                error = context.getString(R.string.add_wallet_error_save_wallet_with_message, failure.javaClass.simpleName)
                            )
                            emitEvent(AddWalletEvent.ShowError(context.getString(R.string.add_wallet_error_save_wallet)))
                        },
                        {
                            _viewState.value = state.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null
                            )
                            emitEvent(AddWalletEvent.ShowSuccess)
                        }
                    )
                }
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            emitEvent(AddWalletEvent.NavigateBack)
        }
    }

    private fun navigateToCurrencyScreen() {
        viewModelScope.launch {
            emitEvent(AddWalletEvent.NavigateToCurrencyScreen)
        }
    }

    private fun navigateToIconPicker() {
        viewModelScope.launch {
            emitEvent(AddWalletEvent.NavigateToIconPicker)
        }
    }

    private fun selectIcon(iconRes: String) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(iconString = iconRes)
        }
    }
} 