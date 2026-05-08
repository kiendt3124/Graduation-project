package com.uet.expensetracker.features.money.ui.currency

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.data.data_source.local.CurrencyDataSource
import com.uet.expensetracker.features.money.domain.usecases.CheckCurrenciesExistUseCase
import com.uet.expensetracker.features.money.domain.usecases.SaveCurrenciesUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetAllCurrenciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.uet.expensetracker.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val checkCurrenciesExistUseCase: CheckCurrenciesExistUseCase,
    private val saveCurrenciesUseCase: SaveCurrenciesUseCase,
    private val getAllCurrenciesUseCase: GetAllCurrenciesUseCase,
    private val currencyDataSource: CurrencyDataSource,
    @ApplicationContext private val context: Context
) : BaseViewModel<CurrencyState, CurrencyIntent, CurrencyEvent>() {

    override val _viewState = MutableStateFlow(CurrencyState())

    override fun processIntent(intent: CurrencyIntent) {
        when (intent) {
            CurrencyIntent.LoadCurrencies -> loadCurrencies()
            is CurrencyIntent.SelectCurrency -> selectCurrency(intent.currency)
        }
    }

    private fun loadCurrencies() {
        _viewState.value = _viewState.value.copy(isLoading = true, error = null)
        checkCurrenciesExistUseCase(UseCase.None(), viewModelScope) { result ->
            result.fold(
                {
                    val errorMsg = context.getString(R.string.currency_error_check_currencies)
                    _viewState.value = _viewState.value.copy(isLoading = false, error = errorMsg)
                    emitEvent(CurrencyEvent.ShowError(errorMsg))
                },
                { exists ->
                    if (!exists) {
                        viewModelScope.launch {
                            try {
                                val list = currencyDataSource.loadCurrencies(context)
                                saveCurrenciesUseCase(SaveCurrenciesUseCase.Params(list), viewModelScope) { saveResult ->
                                    saveResult.fold(
                                        {
                                            val errorMsg = context.getString(R.string.currency_error_save_currencies)
                                            _viewState.value = _viewState.value.copy(isLoading = false, error = errorMsg)
                                            emitEvent(CurrencyEvent.ShowError(errorMsg))
                                        },
                                        {
                                            loadFromDb()
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                _viewState.value = _viewState.value.copy(isLoading = false, error = e.message)
                                emitEvent(CurrencyEvent.ShowError("Failed to load currencies: ${e.message}"))
                            }
                        }
                    } else {
                        loadFromDb()
                    }
                }
            )
        }
    }

    private fun loadFromDb() {
        getAllCurrenciesUseCase(UseCase.None(), viewModelScope) { result ->
            result.fold(
                {
                    val errorMsg = context.getString(R.string.currency_error_load_currencies)
                    _viewState.value = _viewState.value.copy(isLoading = false, error = errorMsg)
                    emitEvent(CurrencyEvent.ShowError(errorMsg))
                },
                { flow ->
                    flow.onEach { list ->
                        _viewState.value = _viewState.value.copy(isLoading = false, currencies = list)
                    }.launchIn(viewModelScope)
                }
            )
        }
    }

    private fun selectCurrency(currency: com.uet.expensetracker.features.money.domain.model.Currency) {
        _viewState.value = _viewState.value.copy(selectedCurrencyCode = currency.currencyCode)
        emitEvent(CurrencyEvent.CurrencySelected(currency))
    }
} 