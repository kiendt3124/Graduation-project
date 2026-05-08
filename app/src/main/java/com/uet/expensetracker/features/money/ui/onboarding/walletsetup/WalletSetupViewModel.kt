package com.uet.expensetracker.features.money.ui.onboarding.walletsetup

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.CurrencyEntity
import com.uet.expensetracker.features.money.data.data_source.local.model.WalletWithCurrencyEntity
import com.uet.expensetracker.features.money.domain.usecases.CreateWalletUseCase
import com.uet.expensetracker.features.money.domain.repository.OnboardingRepository
import com.uet.expensetracker.features.money.domain.usecases.InsertCategoriesUseCase
import com.uet.expensetracker.features.money.data.data_source.local.CategoryDataSource
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import com.uet.expensetracker.features.money.ui.addtransaction.components.NumpadButton
import com.uet.expensetracker.utils.CalculatorUtil
import com.uet.expensetracker.utils.CalculatorUtil.CalculatorCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.data.data_source.local.db.LocalDatabase
import java.io.File

@HiltViewModel
class WalletSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createWalletUseCase: CreateWalletUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val insertCategoriesUseCase: InsertCategoriesUseCase,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : BaseViewModel<WalletSetupState, WalletSetupIntent, WalletSetupEvent>(), CalculatorCallback {

    override val _viewState = MutableStateFlow(WalletSetupState())
    // Calculator utility for amount input
    private val calculator = CalculatorUtil(this)
    private val databaseName = LocalDatabase.DATABASE_NAME

    override fun processIntent(intent: WalletSetupIntent) {
        when (intent) {
            WalletSetupIntent.ChangeIcon -> emitEvent(WalletSetupEvent.NavigateToIconPicker)
            is WalletSetupIntent.IconPicked -> _viewState.value = _viewState.value.copy(
                selectedIcon = intent.iconRes
            )
            is WalletSetupIntent.UpdateName -> _viewState.value = _viewState.value.copy(
                walletName = intent.name
            )
            WalletSetupIntent.ChangeCurrency -> emitEvent(WalletSetupEvent.NavigateToCurrencyPicker)
            // Toggle bottom-sheet numpad
            WalletSetupIntent.ToggleNumpad -> _viewState.value = _viewState.value.copy(
                showNumpad = !_viewState.value.showNumpad
            )
            is WalletSetupIntent.CurrencySelected -> _viewState.value = _viewState.value.copy(
                selectedCurrency = intent.currency
            ).also {
                // Update calculator format for the selected currency
                calculator.setCurrency(intent.currency)
            }
            WalletSetupIntent.ChangeBalance -> _viewState.value = _viewState.value.copy(
                showNumpad = !_viewState.value.showNumpad
            )
            is WalletSetupIntent.BalanceEntered -> _viewState.value = _viewState.value.copy(
                enteredAmount = intent.amount
            )
            is WalletSetupIntent.NumpadButtonPressed -> calculator.handleNumpadButtonPressed(intent.button)
            WalletSetupIntent.ConfirmSetup -> handleConfirmSetup()
            WalletSetupIntent.SkipSetup -> skipSetup()
        }
    }

    private fun skipSetup() {
        // When skipping after a successful Google login, restore the user's backup immediately,
        // then proceed to Home.
        if (_viewState.value.isRestoreLoading) return
        _viewState.value = _viewState.value.copy(isRestoreLoading = true, error = null)

        val user = auth.currentUser
        if (user == null) {
            _viewState.value = _viewState.value.copy(
                isRestoreLoading = false,
                error = context.getString(R.string.error_please_sign_in_first)
            )
            viewModelScope.launch {
                onboardingRepository.setOnboardingCompleted(true)
                emitEvent(WalletSetupEvent.NavigateToHome)
            }
            return
        }

        try {
            val localFile = context.getDatabasePath(databaseName)
            val dbDir = localFile.parentFile!!
            val walFile = File(dbDir, "${databaseName}-wal")
            val shmFile = File(dbDir, "${databaseName}-shm")

            val refDb = storage.reference.child("backups/${user.uid}/$databaseName")
            val refWal = storage.reference.child("backups/${user.uid}/${databaseName}-wal")
            val refShm = storage.reference.child("backups/${user.uid}/${databaseName}-shm")

            refDb.getFile(localFile)
                .addOnSuccessListener {
                    refWal.getFile(walFile)
                        .addOnSuccessListener {
                            refShm.getFile(shmFile)
                                .addOnSuccessListener {
                                    _viewState.value = _viewState.value.copy(isRestoreLoading = false)
                                    viewModelScope.launch {
                                        onboardingRepository.setOnboardingCompleted(true)
                                        emitEvent(WalletSetupEvent.NavigateToHome)
                                    }
                                }
                                .addOnFailureListener { error ->
                                    _viewState.value = _viewState.value.copy(
                                        isRestoreLoading = false,
                                        error = error.message ?: context.getString(R.string.error_restore_shm_failed)
                                    )
                                    viewModelScope.launch {
                                        onboardingRepository.setOnboardingCompleted(true)
                                        emitEvent(WalletSetupEvent.NavigateToHome)
                                    }
                                }
                        }
                        .addOnFailureListener { error ->
                            _viewState.value = _viewState.value.copy(
                                isRestoreLoading = false,
                                error = error.message ?: context.getString(R.string.error_restore_wal_failed)
                            )
                            viewModelScope.launch {
                                onboardingRepository.setOnboardingCompleted(true)
                                emitEvent(WalletSetupEvent.NavigateToHome)
                            }
                        }
                }
                .addOnFailureListener { error ->
                    _viewState.value = _viewState.value.copy(
                        isRestoreLoading = false,
                        error = error.message ?: context.getString(R.string.error_restore_db_failed)
                    )
                    viewModelScope.launch {
                        onboardingRepository.setOnboardingCompleted(true)
                        emitEvent(WalletSetupEvent.NavigateToHome)
                    }
                }
        } catch (e: Exception) {
            _viewState.value = _viewState.value.copy(
                isRestoreLoading = false,
                error = e.localizedMessage ?: context.getString(R.string.error_restore_generic)
            )
            viewModelScope.launch {
                onboardingRepository.setOnboardingCompleted(true)
                emitEvent(WalletSetupEvent.NavigateToHome)
            }
        }
    }

    private fun handleConfirmSetup() {
        // Assemble and execute create wallet use case
        val state = _viewState.value
        state.selectedCurrency?.let { currency ->
            viewModelScope.launch {
                _viewState.value = state.copy(isCreating = true, error = null)
                // Create entity for insertion
                val walletEntity = WalletEntity(
                    id = 0,
                    walletName = state.walletName,
                    currentBalance = state.amountInput.toDoubleOrNull() ?: 0.0,
                    currencyId = currency.id,
                    icon = state.selectedIcon.toString(),
                    isMainWallet = true
                )
                val currencyEntity = CurrencyEntity(
                    id = currency.id,
                    currencyName = currency.currencyName,
                    currencyCode = currency.currencyCode,
                    symbol = currency.symbol,
                    displayType = currency.displayType,
                    image = currency.image
                )
                val params = CreateWalletUseCase.Params(
                    WalletWithCurrencyEntity(walletEntity, currencyEntity)
                )
                // First populate categories, then create wallet
                populateCategories { categoriesSuccess ->
                    if (categoriesSuccess) {
                        // Invoke use case
                        createWalletUseCase(params, viewModelScope) { result ->
                            result.fold(
                                { failure ->
                                    _viewState.value = state.copy(
                                        isCreating = false,
                                        error = failure.javaClass.simpleName
                                    )
                                },
                                {
                                    // On success, save onboarding flag and navigate
                                    viewModelScope.launch {
                                        onboardingRepository.setOnboardingCompleted(true)
                                        emitEvent(WalletSetupEvent.NavigateToHome)
                                    }
                                }
                            )
                        }
                    } else {
                        _viewState.value = state.copy(
                            isCreating = false,
                            error = context.getString(R.string.error_init_categories_failed)
                        )
                    }
                }
            }
        }
    }

    // Calculator callback implementations
    override fun onCalculatorUpdate(amount: String, formattedAmount: String, displayExpression: String) {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(
                amountInput = amount,
                formattedAmount = formattedAmount
            )
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

    override fun onError(message: String, level: CalculatorUtil.CalculatorError) {
        // Update error state
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(error = message)
        }
    }
    
    private fun populateCategories(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Load categories from JSON
                val categoryDataSource = CategoryDataSource()
                val categoryEntities = categoryDataSource.loadCategoryEntities(context)
                val categories = categoryEntities.map { it.toCategory() }
                
                // Insert categories into database
                insertCategoriesUseCase(
                    params = InsertCategoriesUseCase.Params(categories),
                    scope = viewModelScope
                ) { result ->
                    result.fold(
                        { failure ->
                            onComplete(false)
                        },
                        {
                            onComplete(true)
                        }
                    )
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
} 