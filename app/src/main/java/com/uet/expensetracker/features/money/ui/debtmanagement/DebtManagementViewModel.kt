package com.uet.expensetracker.features.money.ui.debtmanagement

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel

import com.uet.expensetracker.features.money.domain.model.DebtCategoryMetadata
import com.uet.expensetracker.features.money.domain.model.DebtSummary
import com.uet.expensetracker.features.money.domain.model.DebtType
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.core.functional.Either
import com.uet.expensetracker.features.money.domain.usecases.GetDebtSummaryUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetCategoryByNameUseCase
import com.uet.expensetracker.features.money.domain.usecases.CreatePartialPaymentUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetDebtPaymentHistoryUseCase
import com.uet.expensetracker.utils.formatAmount
import com.uet.expensetracker.features.money.ui.debtmanagement.components.DebtFilterOptions
import com.uet.expensetracker.features.money.ui.debtmanagement.components.DebtSortBy
import com.uet.expensetracker.features.money.ui.debtmanagement.components.DebtTab
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.uet.expensetracker.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ViewModel for the DebtManagement screen.
 * Handles business logic and manages the UI state for debt management.
 * Follows MVI architecture pattern.
 */
@HiltViewModel
class DebtManagementViewModel @Inject constructor(
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getCategoryByNameUseCase: GetCategoryByNameUseCase,
    private val createPartialPaymentUseCase: CreatePartialPaymentUseCase,
    private val getDebtPaymentHistoryUseCase: GetDebtPaymentHistoryUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<DebtManagementState, DebtManagementIntent, DebtManagementEvent>() {

    override val _viewState = MutableStateFlow(DebtManagementState())
    private var historyJob: Job? = null

    init {
        processIntent(DebtManagementIntent.LoadInitialData)
    }

    /**
     * Handles intents triggered from the UI.
     */
    override fun processIntent(intent: DebtManagementIntent) {
        when (intent) {
            DebtManagementIntent.LoadInitialData -> loadInitialData()
            DebtManagementIntent.RefreshData -> refreshData()
            
            // Wallet selection
            DebtManagementIntent.ShowWalletSelector -> showWalletSelector()
            DebtManagementIntent.HideWalletSelector -> hideWalletSelector()
            is DebtManagementIntent.SelectWallet -> selectWallet(intent.wallet)
            
            // Tab management
            is DebtManagementIntent.SelectTab -> selectTab(intent.tab)
            
            // Debt management
            is DebtManagementIntent.ViewDebtDetails -> viewDebtDetails(intent.debtSummary)
            is DebtManagementIntent.AddPartialPayment -> openPaymentSheet(intent.debtSummary)
            is DebtManagementIntent.ViewPaymentHistory -> viewPaymentHistory(intent.debtSummary)
            DebtManagementIntent.ClosePaymentHistory -> closePaymentHistory()
            is DebtManagementIntent.UpdatePaymentAmount -> updatePaymentAmount(intent.amountText)
            is DebtManagementIntent.UpdatePaymentNote -> updatePaymentNote(intent.note)
            DebtManagementIntent.ConfirmPayment -> confirmPayment()
            DebtManagementIntent.DismissPaymentSheet -> dismissPaymentSheet()
            
            // Filter management
            DebtManagementIntent.ShowFilterDialog -> showFilterDialog()
            DebtManagementIntent.HideFilterDialog -> hideFilterDialog()
            is DebtManagementIntent.ApplyFilter -> applyFilter(intent.filterOptions)
            DebtManagementIntent.ClearFilter -> clearFilter()
            
            // Error handling
            DebtManagementIntent.ClearError -> clearError()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true, error = null) }

            try {
                loadWalletsAndDebt(walletId = _viewState.value.selectedWallet?.id)
            } catch (e: Exception) {
                _viewState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Đã xảy ra lỗi: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun refreshData() {
        _viewState.update { it.copy(isRefreshing = true) }
        loadInitialData()
    }

    private fun showWalletSelector() {
        _viewState.update { it.copy(showWalletSelector = true) }
    }

    private fun hideWalletSelector() {
        _viewState.update { it.copy(showWalletSelector = false) }
    }

    private fun selectWallet(wallet: Wallet?) {
        _viewState.update { 
            it.copy(
                selectedWallet = wallet,
                showWalletSelector = false
            ) 
        }
        
        // Reload debt data for selected wallet
        loadDebtDataForWallet(wallet?.id)
    }

    private fun selectTab(tab: DebtTab) {
        _viewState.update { it.copy(selectedTab = tab) }
    }

    private fun viewDebtDetails(debtSummary: DebtSummary) {
        emitEvent(DebtManagementEvent.NavigateToDebtDetails(debtSummary))
    }

    private fun openPaymentSheet(debtSummary: DebtSummary) {
        if (debtSummary.isPaid) {
            emitEvent(DebtManagementEvent.ShowToast(context.getString(R.string.debt_fully_paid)))
            return
        }
        _viewState.update {
            it.copy(
                showPaymentSheet = true,
                paymentTarget = debtSummary,
                paymentAmountInput = "",
                paymentNoteInput = "",
                paymentSubmitting = false
            )
        }
    }

    private fun viewPaymentHistory(debtSummary: DebtSummary) {
        // Hủy collecting cũ nếu đang chạy để tránh đè job
        historyJob?.cancel()

        val isPayable = DebtCategoryMetadata.PAYABLE_ORIGINAL.contains(debtSummary.originalTransaction.category.metaData)
        val historyTitle = if (isPayable) context.getString(R.string.debt_payment_history) else context.getString(R.string.debt_collection_history)
        val historyDebtType = if (isPayable) DebtType.PAYABLE else DebtType.RECEIVABLE

        // Mở sheet và set loading
        _viewState.update {
            it.copy(
                showHistorySheet = true,
                historyLoading = true,
                historyError = null,
                historyItems = emptyList(),
                historyTitle = historyTitle,
                historyDebtType = historyDebtType
            )
        }

        historyJob = viewModelScope.launch {
            when (val result = getDebtPaymentHistoryUseCase.run(
                GetDebtPaymentHistoryUseCase.Params(
                    originalDebtId = debtSummary.originalTransaction.id,
                    debtReference = debtSummary.debtId
                )
            )) {
                is Either.Left -> {
                    _viewState.update {
                        it.copy(
                            historyLoading = false,
                            historyError = context.getString(R.string.debt_error_load_history)
                        )
                    }
                    emitEvent(DebtManagementEvent.ShowToast(context.getString(R.string.debt_error_load_history)))
                }
                is Either.Right -> {
                    // Collect Flow để cập nhật realtime
                    result.right.collectLatest { records ->
                        _viewState.update {
                            it.copy(
                                historyLoading = false,
                                historyError = null,
                                historyItems = records,
                                showHistorySheet = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun closePaymentHistory() {
        historyJob?.cancel()
        historyJob = null
        _viewState.update {
            it.copy(
                showHistorySheet = false,
                historyItems = emptyList(),
                historyLoading = false,
                historyError = null,
                historyTitle = null,
                historyDebtType = null
            )
        }
    }

    private fun updatePaymentAmount(amountText: String) {
        _viewState.update { it.copy(paymentAmountInput = amountText) }
    }

    private fun updatePaymentNote(note: String) {
        _viewState.update { it.copy(paymentNoteInput = note) }
    }

    private fun dismissPaymentSheet() {
        _viewState.update {
            it.copy(
                showPaymentSheet = false,
                paymentTarget = null,
                paymentAmountInput = "",
                paymentNoteInput = "",
                paymentSubmitting = false
            )
        }
    }

    private fun confirmPayment() {
        val state = _viewState.value
        val target = state.paymentTarget ?: return
        val amount = state.paymentAmountInput
            .replace(",", "")
            .replace(" ", "")
            .toDoubleOrNull()
        if (amount == null || amount <= 0) {
            emitEvent(DebtManagementEvent.ShowToast(context.getString(R.string.debt_error_invalid_amount)))
            return
        }
        if (amount > target.remainingAmount) {
            emitEvent(DebtManagementEvent.ShowToast(context.getString(R.string.debt_error_amount_exceeds_remaining)))
            return
        }

        viewModelScope.launch {
            _viewState.update { it.copy(paymentSubmitting = true) }
            
            try {
                /**
                 * Xác định loại nợ dựa trên category gốc:
                 * - Payable (Tab "Phải trả"): IS_DEBT (Tôi đi vay người khác)
                 * - Receivable (Tab "Được nhận"): IS_LOAN (Tôi cho người khác vay)
                 */
                val isPayable = DebtCategoryMetadata.PAYABLE_ORIGINAL.contains(target.originalTransaction.category.metaData)
                
                /**
                 * Xác định transaction type dựa trên loại nợ:
                 * - Payable (Phải trả): Trả nợ = tiền ra (OUTFLOW, màu đỏ)
                 * - Receivable (Được nhận): Thu nợ = tiền vào (INFLOW, màu xanh)
                 */
                val transactionType = if (isPayable) {
                    TransactionType.OUTFLOW  // Trả nợ = chi tiêu (tiền ra)
                } else {
                    TransactionType.INFLOW   // Thu nợ = thu nhập (tiền vào)
                }
                
                /**
                 * Tìm category phù hợp cho giao dịch thanh toán/thu nợ:
                 * - Payable: Dùng IS_REPAYMENT (trả nợ lại cho người khác)
                 * - Receivable: Dùng IS_DEBT_COLLECTION (thu nợ từ người khác)
                 */
                val categoryMeta = if (isPayable) {
                    DebtCategoryMetadata.REPAYMENT
                } else {
                    DebtCategoryMetadata.DEBT_COLLECTION
                }
                
                val category = fetchCategoryByMeta(categoryMeta) ?: run {
                    _viewState.update { it.copy(paymentSubmitting = false) }
                    emitEvent(DebtManagementEvent.ShowToast(if (isPayable) context.getString(R.string.debt_error_category_not_found_payable) else context.getString(R.string.debt_error_category_not_found_receivable)))
                    return@launch
                }

                // Tạo transaction thanh toán/thu nợ
                val paymentDescription = state.paymentNoteInput.ifBlank { 
                    if (isPayable) context.getString(R.string.debt_payment_title, target.personName) else context.getString(R.string.debt_collection_title, target.personName)
                }

                // Tạo transaction thanh toán/thu nợ với đầy đủ thông tin debt
                val paymentTx = Transaction(
                    id = 0, // Transaction mới
                    wallet = target.originalTransaction.wallet,
                    transactionType = transactionType,
                    amount = amount,
                    transactionDate = Date(),
                    description = paymentDescription,
                    category = category,
                    withPerson = target.originalTransaction.withPerson,
                    parentDebtId = target.originalTransaction.id,
                    debtReference = target.debtId,
                    debtMetadata = target.originalTransaction.debtMetadata
                )

                // Lưu transaction qua CreatePartialPaymentUseCase (tự động cập nhật wallet balance)
                createPartialPaymentUseCase(
                    CreatePartialPaymentUseCase.Params(
                        originalDebtId = target.originalTransaction.id,
                        debtReference = target.debtId,
                        paymentTransaction = paymentTx
                    )
                ) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update { it.copy(paymentSubmitting = false) }
                            emitEvent(DebtManagementEvent.ShowToast(context.getString(R.string.debt_error_save_transaction, failure.toString())))
                        },
                        { savedTransaction ->
                            // Sau khi lưu thành công, reload dữ liệu nợ để cập nhật UI
                            viewModelScope.launch {
                                try {
                                    // Đảm bảo database đã commit transaction mới
                                    // Delay nhỏ để Room database có thời gian commit transaction
                                    // Room thường commit trong 50-100ms, delay 150ms đảm bảo an toàn
                                    kotlinx.coroutines.delay(150)
                                    
                                    // Reload dữ liệu nợ để cập nhật UI
                                    // Sau delay, database đã commit nên query sẽ thấy transaction mới
                                    loadDebtDataForWallet(_viewState.value.selectedWallet?.id)
                                    
                                    // Đảm bảo state đã được cập nhật trước khi hiển thị message
                                    _viewState.update { it.copy(paymentSubmitting = false) }
                                    
                                    emitEvent(DebtManagementEvent.ShowSuccessMessage(
                                        if (isPayable) context.getString(R.string.debt_payment_recorded, formatAmount(amount), target.originalTransaction.wallet.currency.symbol)
                                        else context.getString(R.string.debt_collection_recorded, formatAmount(amount), target.originalTransaction.wallet.currency.symbol)
                                    ))
                                    dismissPaymentSheet()
                                } catch (e: Exception) {
                                    android.util.Log.e("DebtManagementVM", "Error reloading debt data", e)
                                    _viewState.update { it.copy(paymentSubmitting = false) }
                                    emitEvent(DebtManagementEvent.ShowToast(context.getString(R.string.debt_error_update_ui, e.message ?: "")))
                                }
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DebtManagementVM", "Error processing payment", e)
                _viewState.update { it.copy(paymentSubmitting = false) }
                emitEvent(DebtManagementEvent.ShowToast("Đã xảy ra lỗi: ${e.message}"))
            }
        }
    }

    /**
     * Trả về metadata danh mục phù hợp cho giao dịch thanh toán/thu nợ.
     */
    private fun resolvePaymentCategoryMeta(target: DebtSummary): String {
        return if (DebtCategoryMetadata.PAYABLE_ORIGINAL.contains(target.originalTransaction.category.metaData)) {
            DebtCategoryMetadata.REPAYMENT
        } else {
            DebtCategoryMetadata.DEPT_COLLECTION_SAFE_FALLBACK()
        }
    }

    /**
     * Lấy Category theo metadata (title/meta). Trả null nếu không có.
     */
    private suspend fun fetchCategoryByMeta(meta: String): Category? {
        return when (val result = getCategoryByNameUseCase.run(GetCategoryByNameUseCase.Params(meta))) {
            is Either.Right -> result.right
            is Either.Left -> null
        }
    }

    private fun showFilterDialog() {
        _viewState.update { it.copy(showFilterDialog = true) }
    }

    private fun hideFilterDialog() {
        _viewState.update { it.copy(showFilterDialog = false) }
    }

    private fun applyFilter(filterOptions: DebtFilterOptions) {
        _viewState.update { 
            it.copy(
                filterOptions = filterOptions,
                showFilterDialog = false
            ) 
        }
        
        // Apply filter to current debt data
        applyFilterToDebtData()
    }

    private fun clearFilter() {
        _viewState.update { 
            it.copy(
                filterOptions = DebtFilterOptions(),
                showFilterDialog = false
            ) 
        }
        
        // Reset to unfiltered data
        applyFilterToDebtData()
    }

    private fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    // Private helper methods

    private fun loadDebtDataForWallet(walletId: Int?) {
        viewModelScope.launch {
            try {
                loadWalletsAndDebt(walletId)
            } catch (e: Exception) {
                _viewState.update { 
                    it.copy(error = "Đã xảy ra lỗi khi tải dữ liệu: ${e.message}") 
                }
            }
        }
    }

    /**
     * Cập nhật state với dữ liệu nợ đã phân loại
     * 
     * Logic phân loại:
     * - Tab "Phải trả" (Payable): IS_DEBT (Tôi đi vay người khác → Tôi phải trả lại)
     * - Tab "Được nhận" (Receivable): IS_LOAN (Tôi cho người khác vay → Người khác nợ tôi)
     */
    private fun updateStateWithData(wallets: List<Wallet>, allDebtSummaries: List<DebtSummary>) {
        // Phân loại nợ theo metadata của giao dịch gốc:
        // - Payable (Tab "Phải trả"): IS_DEBT (Tôi đi vay → Tôi phải trả lại)
        // - Receivable (Tab "Được nhận"): IS_LOAN (Tôi cho vay → Người khác nợ tôi)
        val payableRaw = allDebtSummaries.filter { debt ->
            DebtCategoryMetadata.PAYABLE_ORIGINAL.contains(debt.originalTransaction.category.metaData)
        }
        val receivableRaw = allDebtSummaries.filter { debt ->
            DebtCategoryMetadata.RECEIVABLE_ORIGINAL.contains(debt.originalTransaction.category.metaData)
        }

        // Áp filter lên dữ liệu gốc để tránh lọc chồng nhiều lần
        val filteredPayableDebts = applyFilterToDebts(payableRaw)
        val filteredReceivableDebts = applyFilterToDebts(receivableRaw)

        // Phân chia đã trả/chưa trả
        val unpaidPayableDebts = filteredPayableDebts.filter { !it.isPaid }
        val paidPayableDebts = filteredPayableDebts.filter { it.isPaid }
        val unpaidReceivableDebts = filteredReceivableDebts.filter { !it.isPaid }
        val paidReceivableDebts = filteredReceivableDebts.filter { it.isPaid }

        // Thống kê:
        // - totalXAmount: tổng gốc (đã vay/đã cho vay) = sum(totalAmount)
        // - totalUnpaidX: số còn nợ/chưa thu = sum(remainingAmount)
        val totalPayableAmount = filteredPayableDebts.sumOf { it.totalAmount }
        val totalReceivableAmount = filteredReceivableDebts.sumOf { it.totalAmount }
        val totalUnpaidPayable = filteredPayableDebts.sumOf { it.remainingAmount }
        val totalUnpaidReceivable = filteredReceivableDebts.sumOf { it.remainingAmount }

        val currencySymbol = resolveCurrencySymbol(wallets)

        _viewState.update { currentState ->
            currentState.copy(
                availableWallets = wallets,
                allPayableDebts = payableRaw,
                allReceivableDebts = receivableRaw,
                payableDebts = filteredPayableDebts,
                receivableDebts = filteredReceivableDebts,
                unpaidPayableDebts = unpaidPayableDebts,
                paidPayableDebts = paidPayableDebts,
                unpaidReceivableDebts = unpaidReceivableDebts,
                paidReceivableDebts = paidReceivableDebts,
                totalPayableAmount = totalPayableAmount,
                totalReceivableAmount = totalReceivableAmount,
                totalUnpaidPayable = totalUnpaidPayable,
                totalUnpaidReceivable = totalUnpaidReceivable,
                currencySymbol = currencySymbol,
                isLoading = false,
                isRefreshing = false,
                error = null
            )
        }
    }

    private fun applyFilterToDebtData() {
        val currentState = _viewState.value
        
        // Cập nhật trực tiếp để tránh gọi lại use case, filter sẽ được áp trong updateStateWithData
        updateStateWithData(
            wallets = currentState.availableWallets,
            allDebtSummaries = currentState.allPayableDebts + currentState.allReceivableDebts
        )
    }

    private fun applyFilterToDebts(debts: List<DebtSummary>): List<DebtSummary> {
        val filter = _viewState.value.filterOptions
        
        var filteredDebts = debts
        
        // Filter by paid/unpaid status
        if (!filter.showPaidDebts) {
            filteredDebts = filteredDebts.filter { !it.isPaid }
        }
        if (!filter.showUnpaidDebts) {
            filteredDebts = filteredDebts.filter { it.isPaid }
        }
        
        // Filter by amount range
        filter.minAmount?.let { min ->
            filteredDebts = filteredDebts.filter { it.remainingAmount >= min }
        }
        filter.maxAmount?.let { max ->
            filteredDebts = filteredDebts.filter { it.remainingAmount <= max }
        }
        
        // Filter by search query
        if (filter.searchQuery.isNotBlank()) {
            filteredDebts = filteredDebts.filter { 
                it.personName.contains(filter.searchQuery, ignoreCase = true)
            }
        }
        
        // Sort debts (simplified sorting without DebtMapper)
        return when (filter.sortBy) {
            DebtSortBy.AMOUNT_ASC ->
                filteredDebts.sortedBy { it.remainingAmount }
            DebtSortBy.AMOUNT_DESC ->
                filteredDebts.sortedByDescending { it.remainingAmount }
            DebtSortBy.NAME_ASC ->
                filteredDebts.sortedBy { it.personName }
           DebtSortBy.NAME_DESC ->
                filteredDebts.sortedByDescending { it.personName }
            DebtSortBy.DATE_ASC ->
                filteredDebts.sortedBy { it.originalTransaction.transactionDate }
           DebtSortBy.DATE_DESC ->
                filteredDebts.sortedByDescending { it.originalTransaction.transactionDate }
        }
    }

    /**
     * Load wallets (snapshot) rồi load dữ liệu nợ cho 1 wallet hoặc tất cả.
     * Tách riêng để dùng lại cho load lần đầu, refresh, và khi chọn wallet.
     * 
     * Sử dụng suspendCoroutine để đợi callback hoàn thành, đảm bảo state được cập nhật
     * trước khi function return. Điều này quan trọng để UI được cập nhật đúng sau khi trả nợ/thu nợ.
     */
    private suspend fun loadWalletsAndDebt(walletId: Int?) {
        // Sử dụng suspendCoroutine để đợi callback hoàn thành
        suspendCoroutine<Unit> { continuation ->
            // Lấy danh sách ví một lần (first) để tránh collect vô hạn trong VM
            getWalletsUseCase(UseCase.None()) { walletsResult ->
                walletsResult.fold(
                    {
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = context.getString(R.string.debt_error_load_wallets)
                            )
                        }
                        // Resume ngay cả khi lỗi để không block
                        continuation.resume(Unit)
                    },
                    { walletsFlow ->
                        viewModelScope.launch {
                            try {
                                val wallets = walletsFlow.first()
                                _viewState.update { it.copy(availableWallets = wallets) }

                                // Sau khi có ví, lấy dữ liệu nợ (theo walletId hoặc tất cả nếu null)
                                getDebtSummaryUseCase(GetDebtSummaryUseCase.Params(walletId)) { debtResult ->
                                    debtResult.fold(
                                        {
                                            _viewState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    isRefreshing = false,
                                                    error = context.getString(R.string.debt_error_load_data)
                                                )
                                            }
                                            // Resume để tiếp tục
                                            continuation.resume(Unit)
                                        },
                                        { debtInfo ->
                                            // Thành công: Cập nhật state với dữ liệu nợ mới
                                            val allDebts = debtInfo.payableDebts + debtInfo.receivableDebts
                                            updateStateWithData(wallets, allDebts)
                                            // Resume sau khi đã cập nhật state - đây là điểm quan trọng
                                            // để đảm bảo UI được cập nhật trước khi function return
                                            continuation.resume(Unit)
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                // Xử lý exception trong quá trình lấy wallets
                                android.util.Log.e("DebtManagementVM", "Error loading wallets", e)
                                _viewState.update {
                                    it.copy(
                                        isLoading = false,
                                        isRefreshing = false,
                                        error = context.getString(R.string.debt_error_load_data_with_message, e.message ?: "")
                                    )
                                }
                                continuation.resume(Unit)
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * Ưu tiên symbol từ ví đã chọn, nếu không có lấy ví đầu tiên, fallback "đ".
     */
    private fun resolveCurrencySymbol(wallets: List<Wallet>): String {
        return _viewState.value.selectedWallet?.currency?.symbol
            ?: wallets.firstOrNull()?.currency?.symbol
            ?: "đ"
    }
} 