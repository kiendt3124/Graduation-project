package com.uet.expensetracker.features.money.ui.choosewallet

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.utils.CurrencyConverter
import com.uet.expensetracker.utils.TOTAL_WALLET_ID
import com.uet.expensetracker.utils.createTotalWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.uet.expensetracker.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseWalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val currencyConverter: CurrencyConverter,
    @ApplicationContext private val context: Context
) : BaseViewModel<ChooseWalletState, ChooseWalletIntent, ChooseWalletEvent>() {

    override val _viewState = MutableStateFlow(ChooseWalletState())

    override fun processIntent(intent: ChooseWalletIntent) {
        when (intent) {
            is ChooseWalletIntent.LoadWallets -> loadWallets()
            is ChooseWalletIntent.SelectWallet -> selectWallet(intent.walletId)
            is ChooseWalletIntent.SelectTotalWallet -> selectTotalWallet()
            is ChooseWalletIntent.ConfirmSelection -> confirmSelection()
            is ChooseWalletIntent.Cancel -> cancelSelection()
            is ChooseWalletIntent.SetShowTotalWallet -> setShowTotalWallet(intent.show)
        }
    }

    private fun loadWallets() {
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        getWalletsUseCase(UseCase.None()) { result ->
            result.fold(
                { failure ->
                    _viewState.value = _viewState.value.copy(
                        isLoading = false,
                        error = failure.toString()
                    )
                    emitEvent(ChooseWalletEvent.ShowError(context.getString(R.string.choose_wallet_error_load_wallets)))
                },
                { walletsFlow ->
                    walletsFlow
                        .onEach { wallets ->
                            viewModelScope.launch {
                                try {
                                    // Create total wallet with proper currency conversion
                                    val totalWallet = createTotalWallet(wallets, currencyConverter)
                                    
                                    _viewState.value = _viewState.value.copy(
                                        isLoading = false,
                                        wallets = wallets,
                                        totalWallet = totalWallet
                                    )
                                } catch (e: Exception) {
                                    _viewState.value = _viewState.value.copy(
                                        isLoading = false,
                                        error = e.message ?: "Unknown error occurred"
                                    )
                                    emitEvent(ChooseWalletEvent.ShowError(e.message ?: "Failed to load wallets"))
                                }
                            }
                        }
                        .catch { e ->
                            _viewState.value = _viewState.value.copy(
                                isLoading = false,
                                error = e.message ?: "Unknown error occurred"
                            )
                            emitEvent(ChooseWalletEvent.ShowError(e.message ?: "Failed to load wallets"))
                        }
                        .launchIn(viewModelScope)
                }
            )
        }
    }


    private fun selectWallet(walletId: Int) {
        _viewState.value = _viewState.value.copy(selectedWalletId = walletId)
    }
    
    private fun selectTotalWallet() {
        _viewState.value = _viewState.value.copy(selectedWalletId = TOTAL_WALLET_ID)
    }

    private fun confirmSelection() {
        val selectedId = _viewState.value.selectedWalletId ?: return
        
        if (selectedId == TOTAL_WALLET_ID) {
            val totalWallet = _viewState.value.totalWallet
            totalWallet?.let {
                emitEvent(ChooseWalletEvent.WalletSelected(it, true))
            } ?: emitEvent(ChooseWalletEvent.ShowError(context.getString(R.string.choose_wallet_error_total_not_available)))
            return
        }
        
        val selectedWallet = _viewState.value.wallets.find { it.id == selectedId }
        selectedWallet?.let {
            emitEvent(ChooseWalletEvent.WalletSelected(it))
        } ?: emitEvent(ChooseWalletEvent.ShowError(context.getString(R.string.choose_wallet_error_no_wallet_selected)))
    }

    private fun cancelSelection() {
        emitEvent(ChooseWalletEvent.NavigateBack)
    }
    
    private fun setShowTotalWallet(show: Boolean) {
        _viewState.value = _viewState.value.copy(showTotalWallet = show)
    }
} 