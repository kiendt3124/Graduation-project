package com.uet.expensetracker.features.money.ui.mywallets

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.failure.Failure
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.DeleteWalletUseCase
import com.uet.expensetracker.features.money.domain.usecases.UpdateWalletUseCase
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
import javax.inject.Inject

@HiltViewModel
class MyWalletsViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val currencyConverter: CurrencyConverter,
    @ApplicationContext private val context: Context
) : BaseViewModel<MyWalletsState, MyWalletsIntent, MyWalletsEvent>() {

    override val _viewState = MutableStateFlow(MyWalletsState())

    init {
        loadWallets()
    }

    override fun processIntent(intent: MyWalletsIntent) {
        when (intent) {
            is MyWalletsIntent.LoadWallets -> loadWallets()
            is MyWalletsIntent.AddWallet -> emitEvent(MyWalletsEvent.NavigateToAddWallet(intent.walletType))

            is MyWalletsIntent.DeleteWallet -> {
                val walletToDelete = _viewState.value.wallets.find { it.id == intent.walletId }
                if (walletToDelete?.isMainWallet == true) {
                    // Cannot delete the main wallet
                    emitEvent(MyWalletsEvent.ShowError(context.getString(R.string.my_wallets_error_cannot_delete_main)))
                } else {
                    viewModelScope.launch {
                        deleteWalletUseCase(DeleteWalletUseCase.Params(intent.walletId)) {
                            it.fold(
                                { failure -> handleFailure(failure) },
                                {
                                    _viewState.value = _viewState.value.copy(
                                        wallets = _viewState.value.wallets.filter { it.id != intent.walletId }
                                    )
                                    emitEvent(MyWalletsEvent.WalletDeleted)
                                }
                            )
                        }
                    }
                }
            }

            is MyWalletsIntent.SetMainWallet -> {
                val walletToSet = _viewState.value.wallets.find { it.id == intent.walletId }
                if (walletToSet != null) {
                    viewModelScope.launch {
                        // Unset previous main wallet if different
                        _viewState.value.wallets.find { it.isMainWallet && it.id != intent.walletId }?.let { prevMain ->
                            updateWalletUseCase(UpdateWalletUseCase.Params(prevMain.toWalletEntity().copy(isMainWallet = false))) { res ->
                                res.fold(
                                    { failure -> handleFailure(failure) },
                                    { /* previous main wallet unset */ }
                                )
                            }
                        }
                        // Set new main wallet
                        updateWalletUseCase(UpdateWalletUseCase.Params(walletToSet.toWalletEntity().copy(isMainWallet = true))) { res2 ->
                            res2.fold(
                                { failure -> handleFailure(failure) },
                                {
                                    val updatedList = _viewState.value.wallets.map { w ->
                                        w.copy(isMainWallet = w.id == intent.walletId)
                                    }
                                    _viewState.value = _viewState.value.copy(wallets = updatedList)
                                    emitEvent(MyWalletsEvent.MainWalletSet)
                                }
                            )
                        }
                    }
                }
            }

            is MyWalletsIntent.EditWallet -> emitEvent(MyWalletsEvent.NavigateToEditWallet(intent.walletId))
            is MyWalletsIntent.TransferMoney -> emitEvent(MyWalletsEvent.NavigateToTransferMoney(intent.walletId))
        }
    }

    private fun loadWallets() {
        _viewState.value = _viewState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            getWalletsUseCase(UseCase.None()) { result ->
                result.fold(
                    { failure ->
                        handleWalletsFailure(failure)
                    },
                    { walletsFlow ->
                        walletsFlow
                            .onEach { wallets -> handleWalletsSuccess(wallets) }
                            .catch { e -> handleWalletsFailure(Failure.DatabaseError) }
                            .launchIn(viewModelScope)
                    }
                )
            }
        }
    }

    private fun handleWalletsSuccess(wallets: List<Wallet>) {
        viewModelScope.launch {
            try {
                val totalBalance = calculateTotalBalance(wallets)
                
                _viewState.value = _viewState.value.copy(
                    isLoading = false,
                    wallets = wallets,
                    totalBalance = totalBalance,
                    error = null
                )
            } catch (e: Exception) {
                handleWalletsFailure(Failure.DatabaseError)
            }
        }
    }

    private fun handleWalletsFailure(failure: Failure) {
        _viewState.value = _viewState.value.copy(
            isLoading = false,
            error = context.getString(R.string.my_wallets_error_load_wallets_with_message, failure.javaClass.simpleName)
        )
        
        emitEvent(MyWalletsEvent.ShowError(context.getString(R.string.my_wallets_error_load_wallets)))
        handleFailure(failure)
    }

    private suspend fun calculateTotalBalance(wallets: List<Wallet>): Double {
        // Initialize currency converter
        currencyConverter.initialize()

        // Find the main wallet and its currency
        val mainWallet = wallets.find { it.isMainWallet }
        val mainCurrency = mainWallet?.currency?.currencyCode ?: "VND"

        // Calculate total balance by converting all amounts to main currency
        var totalBalance = 0.0
        wallets.forEach { wallet ->
            val convertedAmount = if (wallet.currency.currencyCode == mainCurrency) {
                wallet.currentBalance
            } else {
                currencyConverter.convert(
                    amount = wallet.currentBalance,
                    fromCurrency = wallet.currency.currencyCode,
                    toCurrency = mainCurrency
                ) ?: 0.0
            }
            totalBalance += convertedAmount
        }

        return totalBalance
    }
} 