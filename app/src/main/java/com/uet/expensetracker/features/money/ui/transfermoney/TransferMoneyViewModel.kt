package com.uet.expensetracker.features.money.ui.transfermoney

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.TransferMoneyParams
import com.uet.expensetracker.features.money.domain.usecases.TransferMoneyUseCase
import com.uet.expensetracker.utils.CurrencyConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.uet.expensetracker.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransferMoneyViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val transferMoneyUseCase: TransferMoneyUseCase,
    private val currencyConverter: CurrencyConverter,
    @ApplicationContext private val context: Context
) : BaseViewModel<TransferMoneyState, TransferMoneyIntent, TransferMoneyEvent>() {

    override val _viewState = MutableStateFlow(TransferMoneyState())

    init {
        // Initialize the currency converter
        viewModelScope.launch {
            currencyConverter.initialize()
        }
    }

    override fun processIntent(intent: TransferMoneyIntent) {
        when (intent) {
            is TransferMoneyIntent.LoadWallets -> loadWallets(intent.selectedWalletId)
            is TransferMoneyIntent.SelectFromWallet -> selectFromWallet(intent.walletId)
            is TransferMoneyIntent.SelectToWallet -> selectToWallet(intent.walletId)
            is TransferMoneyIntent.UpdateAmount -> updateAmount(intent.amount)
            is TransferMoneyIntent.UpdateFormattedAmount -> updateFormattedAmount(intent.formattedAmount)
            is TransferMoneyIntent.UpdateNote -> updateNote(intent.note)
            is TransferMoneyIntent.UpdateDate -> updateDate(intent.date)
            is TransferMoneyIntent.UpdateExcludeFromReport -> updateExcludeFromReport(intent.exclude)
            is TransferMoneyIntent.UpdateAddTransferFee -> updateAddTransferFee(intent.addFee)
            is TransferMoneyIntent.UpdateTransferFee -> updateTransferFee(intent.fee)
            is TransferMoneyIntent.SetWalletSelectionMode -> setWalletSelectionMode(intent.mode)
            is TransferMoneyIntent.SaveTransfer -> saveTransfer()
            is TransferMoneyIntent.Cancel -> emitEvent(TransferMoneyEvent.NavigateBack)
            is TransferMoneyIntent.ConvertCurrency -> convertCurrency()
            is TransferMoneyIntent.OpenEnterAmountScreen -> emitEvent(TransferMoneyEvent.NavigateToEnterAmount)
        }
    }

    private fun loadWallets(selectedWalletId: Int? = null) {
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        getWalletsUseCase(UseCase.None()) { result ->
            result.fold(
                { failure ->
                    _viewState.value = _viewState.value.copy(
                        isLoading = false,
                        error = failure.toString()
                    )
                    emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_load_wallets)))
                },
                { walletsFlow ->
                    walletsFlow
                        .onEach { wallets ->
                            val state = _viewState.value.copy(
                                isLoading = false,
                                wallets = wallets
                            )
                            
                            // Auto-select the first wallet as source wallet if not already selected
                            val updatedState = if (state.fromWallet == null && wallets.isNotEmpty()) {
                                val initialWallet = selectedWalletId?.let { id ->
                                    wallets.find { it.id == id }
                                } ?: wallets.first()
                                
                                state.copy(fromWallet = initialWallet)
                            } else {
                                state
                            }
                            
                            _viewState.value = updatedState
                        }
                        .catch { error ->
                            _viewState.value = _viewState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Unknown error"
                            )
                            emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_load_wallets)))
                        }
                        .launchIn(viewModelScope)
                }
            )
        }
    }

    private fun selectFromWallet(walletId: Int) {
        val wallet = _viewState.value.wallets.find { it.id == walletId }
        if (wallet != null) {
            // Ensure from and to wallets are different
            val toWallet = if (_viewState.value.toWallet?.id == walletId) {
                null
            } else {
                _viewState.value.toWallet
            }
            
            _viewState.value = _viewState.value.copy(
                fromWallet = wallet,
                toWallet = toWallet
            )
            
            // If both wallets are selected with different currencies, convert the amount
            if (toWallet != null && wallet.currency.currencyCode != toWallet.currency.currencyCode) {
                convertCurrency()
            }
        }
    }

    private fun selectToWallet(walletId: Int) {
        val wallet = _viewState.value.wallets.find { it.id == walletId }
        if (wallet != null && _viewState.value.fromWallet?.id != walletId) {
            _viewState.value = _viewState.value.copy(toWallet = wallet)
            
            // If from wallet is also selected with different currency, convert the amount
            val fromWallet = _viewState.value.fromWallet
            if (fromWallet != null && fromWallet.currency.currencyCode != wallet.currency.currencyCode) {
                convertCurrency()
            }
        }
    }

    private fun updateAmount(amount: Double) {
        _viewState.value = _viewState.value.copy(amount = amount)
        
        // If we have both wallets selected with different currencies, convert the amount
        val fromWallet = _viewState.value.fromWallet
        val toWallet = _viewState.value.toWallet
        if (fromWallet != null && toWallet != null && 
            fromWallet.currency.currencyCode != toWallet.currency.currencyCode) {
            convertCurrency()
        }
    }
    
    private fun updateFormattedAmount(formattedAmount: String) {
        _viewState.value = _viewState.value.copy(formattedAmount = formattedAmount)
    }

    private fun updateNote(note: String) {
        _viewState.value = _viewState.value.copy(note = note)
    }

    private fun updateDate(date: Date) {
        _viewState.value = _viewState.value.copy(date = date)
    }

    private fun updateExcludeFromReport(exclude: Boolean) {
        _viewState.value = _viewState.value.copy(excludeFromReport = exclude)
    }

    private fun updateAddTransferFee(addFee: Boolean) {
        _viewState.value = _viewState.value.copy(addTransferFee = addFee)
    }

    private fun updateTransferFee(fee: Double) {
        _viewState.value = _viewState.value.copy(transferFee = fee)
    }
    
    private fun setWalletSelectionMode(mode: String) {
        if (mode == "from" || mode == "to") {
            _viewState.value = _viewState.value.copy(lastWalletSelectionMode = mode)
        }
    }
    
    private fun convertCurrency() {
        val state = _viewState.value
        val fromWallet = state.fromWallet
        val toWallet = state.toWallet
        
        if (fromWallet == null || toWallet == null || state.amount <= 0) {
            return
        }
        
        val fromCurrencyCode = fromWallet.currency.currencyCode
        val toCurrencyCode = toWallet.currency.currencyCode
        
        // If currencies are the same, no conversion needed
        if (fromCurrencyCode == toCurrencyCode) {
            val formattedAmount = com.uet.expensetracker.utils.formatAmount(state.amount, true)
            _viewState.value = _viewState.value.copy(
                toAmount = state.amount,
                formattedToAmount = formattedAmount,
                exchangeRate = "1:1"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                // Get the exchange rate for display
                val originalRate = currencyConverter.getExchangeRate(fromCurrencyCode, toCurrencyCode)
                
                // Convert the amount
                val convertedAmount = currencyConverter.convert(state.amount, fromCurrencyCode, toCurrencyCode)
                
                if (convertedAmount != null && originalRate != null) {
                    // Format the exchange rate for display
                    val displayExchangeRate = "1 $fromCurrencyCode = ${com.uet.expensetracker.utils.formatAmount(originalRate, true)} $toCurrencyCode"
                    
                    // Format the converted amount
                    val formattedToAmount = com.uet.expensetracker.utils.formatAmount(convertedAmount, true)
                    
                    _viewState.value = _viewState.value.copy(
                        toAmount = convertedAmount,
                        formattedToAmount = formattedToAmount,
                        exchangeRate = displayExchangeRate
                    )
                } else {
                    _viewState.value = _viewState.value.copy(
                        exchangeRate = context.getString(R.string.transfer_money_error_convert_failed)
                    )
                    emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_convert)))
                }
            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(
                    exchangeRate = context.getString(R.string.transfer_money_error_convert_failed)
                )
                emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_convert_with_message, e.message ?: "")))
            }
        }
    }

    private fun saveTransfer() {
        val state = _viewState.value
        val fromWallet = state.fromWallet
        val toWallet = state.toWallet
        
        // Validate input
        if (fromWallet == null || toWallet == null) {
            emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_select_wallets)))
            return
        }
        
        if (state.amount <= 0) {
            emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_valid_amount)))
            return
        }
        
        if (fromWallet.id == toWallet.id) {
            emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_same_wallet)))
            return
        }
        
        // Check if source wallet has sufficient balance
        val totalAmount = state.amount + (if (state.addTransferFee) state.transferFee else 0.0)
        if (fromWallet.currentBalance < totalAmount) {
            emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_insufficient_balance)))
            return
        }
        
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        val params = TransferMoneyParams(
            fromWallet = fromWallet,
            toWallet = toWallet,
            amount = state.amount,
            toAmount = if (fromWallet.currency.currencyCode != toWallet.currency.currencyCode) state.toAmount else null,
            note = if (state.note.isBlank()) null else state.note,
            date = state.date,
            excludeFromReport = state.excludeFromReport,
            addTransferFee = state.addTransferFee,
            transferFee = state.transferFee
        )
        
        viewModelScope.launch {
            try {
                android.util.Log.d("TransferMoneyViewModel", "Starting transfer process...")
                transferMoneyUseCase(params, viewModelScope) { result ->
                    android.util.Log.d("TransferMoneyViewModel", "Transfer result received: $result")
                    _viewState.value = _viewState.value.copy(isLoading = false)
                    
                    result.fold(
                        { failure ->
                            android.util.Log.e("TransferMoneyViewModel", "Transfer failed: $failure")
                            emitEvent(TransferMoneyEvent.ShowError("Error occurred during transfer: $failure"))
                        },
                        { success ->
                            android.util.Log.d("TransferMoneyViewModel", "Transfer success: $success")
                            if (success) {
                                emitEvent(TransferMoneyEvent.TransferCompleted)
                                emitEvent(TransferMoneyEvent.NavigateBack)
                            } else {
                                emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_transfer_failed)))
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("TransferMoneyViewModel", "Exception during transfer: ${e.message}", e)
                _viewState.value = _viewState.value.copy(isLoading = false)
                emitEvent(TransferMoneyEvent.ShowError(context.getString(R.string.transfer_money_error_exception, e.message ?: "")))
            }
        }
    }
} 
