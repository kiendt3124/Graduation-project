package com.uet.expensetracker.features.money.ui.monthlyreport

import androidx.lifecycle.viewModelScope
import com.uet.expensetracker.core.interactor.UseCase
import com.uet.expensetracker.core.platform.BaseViewModel
import com.uet.expensetracker.features.money.domain.usecases.GetMonthlyReportUseCase
import com.uet.expensetracker.features.money.domain.usecases.GetWalletsUseCase
import com.uet.expensetracker.features.money.ui.transactions.MonthItem
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.uet.expensetracker.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MonthlyReportScreen.
 * Handles business logic and manages UI state for monthly reports.
 */
@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<MonthlyReportState, MonthlyReportIntent, MonthlyReportEvent>() {

    override val _viewState = MutableStateFlow(
        MonthlyReportState(
            months = MonthItem.buildMonthItems(monthsBack = 18).filter { !it.isFuture },
            selectedTabIndex = MonthItem.findThisMonthIndex(
                MonthItem.buildMonthItems(monthsBack = 18).filter { !it.isFuture }
            )
        )
    )

    init {
        processIntent(MonthlyReportIntent.LoadInitialData)
    }

    override fun processIntent(intent: MonthlyReportIntent) {
        when (intent) {
            is MonthlyReportIntent.LoadInitialData -> loadInitialData()
            is MonthlyReportIntent.SelectTab -> selectTab(intent.index)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Get currency symbol from main wallet
                getWalletsUseCase(UseCase.None()) { result ->
                    result.fold(
                        { /* Handle failure */ },
                        { walletsFlow ->
                            viewModelScope.launch {
                                val wallets = walletsFlow.first()
                                val mainWallet = wallets.find { it.isMainWallet }
                                val currencySymbol = mainWallet?.currency?.symbol ?: "đ"
                                
                                _viewState.update { it.copy(currencySymbol = currencySymbol) }
                                
                                // Load report for initially selected month
                                loadReportForSelectedMonth(currencySymbol)
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.monthly_report_error_load_data, e.message ?: "")
                    )
                }
            }
        }
    }

    private fun selectTab(index: Int) {
        _viewState.update { it.copy(selectedTabIndex = index) }
        loadReportForSelectedMonth(_viewState.value.currencySymbol)
    }

    private fun loadReportForSelectedMonth(currencySymbol: String) {
        val state = _viewState.value
        val months = state.months
        val selectedIndex = state.selectedTabIndex
        
        if (selectedIndex < 0 || selectedIndex >= months.size) {
            return
        }
        
        val selectedMonth = months[selectedIndex]
        
        _viewState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                getMonthlyReportUseCase(
                    GetMonthlyReportUseCase.Params(
                        year = selectedMonth.year,
                        month = selectedMonth.month,
                        walletId = null // null = all wallets
                    )
                ) { result ->
                    result.fold(
                        { failure ->
                            _viewState.update {
                                it.copy(
                                    isLoading = false,
                                    error = context.getString(R.string.monthly_report_error_load_report)
                                )
                            }
                        },
                        { reportData ->
                            // Convert CategoryAmount to CategoryPieSlice with colors and proper display names
                            val incomeSlices = reportData.incomeByCategory
                                .map { categoryAmount ->
                                    CategoryPieSlice(
                                        categoryMetadata = categoryAmount.categoryMetadata,
                                        categoryDisplayName = getCategoryDisplayName(
                                            categoryAmount.categoryDisplayName,
                                            categoryAmount.categoryMetadata
                                        ),
                                        amount = categoryAmount.amount,
                                        color = getCategoryColorForReport(
                                            categoryAmount.categoryDisplayName,
                                            categoryAmount.categoryMetadata
                                        ),
                                        percentage = 0f // Will be calculated below
                                    )
                                }
                                .calculatePercentages()
                            
                            val expenseSlices = reportData.expenseByCategory
                                .map { categoryAmount ->
                                    CategoryPieSlice(
                                        categoryMetadata = categoryAmount.categoryMetadata,
                                        categoryDisplayName = getCategoryDisplayName(
                                            categoryAmount.categoryDisplayName,
                                            categoryAmount.categoryMetadata
                                        ),
                                        amount = categoryAmount.amount,
                                        color = getCategoryColorForReport(
                                            categoryAmount.categoryDisplayName,
                                            categoryAmount.categoryMetadata
                                        ),
                                        percentage = 0f // Will be calculated below
                                    )
                                }
                                .calculatePercentages()
                            
                            _viewState.update {
                                it.copy(
                                    isLoading = false,
                                    openingBalance = reportData.openingBalance,
                                    closingBalance = reportData.closingBalance,
                                    netIncome = reportData.netIncome,
                                    totalIncome = reportData.totalIncome,
                                    totalExpense = reportData.totalExpense,
                                    incomeByCategory = incomeSlices,
                                    expenseByCategory = expenseSlices,
                                    currencySymbol = currencySymbol
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.monthly_report_error_load_report_with_message, e.message ?: "")
                    )
                }
            }
        }
    }

    /**
     * Calculates percentage for each slice based on total amount.
     * Ensures the sum of all percentages equals exactly 100% by adjusting rounding.
     * Uses largest remainder method to distribute rounding differences fairly.
     */
    private fun List<CategoryPieSlice>.calculatePercentages(): List<CategoryPieSlice> {
        val total = this.sumOf { it.amount }
        if (total == 0.0) return this
        
        // Calculate raw percentages and prepare for rounding
        data class SliceWithRemainder(
            val slice: CategoryPieSlice,
            val rounded: Int,
            val remainder: Float,
            val originalIndex: Int
        )
        
        val slicesWithRemainder = this.mapIndexed { index, slice ->
            val rawPercent = (slice.amount / total) * 100f
            val rounded = rawPercent.toInt()
            val remainder = (rawPercent - rounded).toFloat()
            SliceWithRemainder(slice, rounded, remainder, index)
        }
        
        // Round all to integers
        val roundedSlices = slicesWithRemainder.map { it.slice.copy(percentage = it.rounded.toFloat()) }
        
        // Calculate sum of rounded percentages
        val roundedSum = roundedSlices.sumOf { it.percentage.toInt() }.toInt()
        val difference = 100 - roundedSum
        
        // If there's a difference, distribute it to slices with largest remainders
        if (difference != 0 && slicesWithRemainder.isNotEmpty()) {
            // Sort by remainder (largest first) to prioritize which slices get +1
            val sortedByRemainder = slicesWithRemainder.sortedByDescending { it.remainder }
            
            // Add +1 to the top |difference| slices
            val adjustedIndices = sortedByRemainder.take(kotlin.math.abs(difference))
                .map { it.originalIndex }
                .toSet()
            
            return roundedSlices.mapIndexed { index, slice ->
                if (adjustedIndices.contains(index)) {
                    slice.copy(percentage = slice.percentage + if (difference > 0) 1f else -1f)
                } else {
                    slice
                }
            }
        }
        
        return roundedSlices
    }
    
    /**
     * Maps category title (like "cate_food") or metadata to a user-friendly display name.
     * Based on moneylover_categories_v3.json structure where "name" field is descriptive.
     */
    private fun getCategoryDisplayName(categoryTitle: String, categoryMetadata: String): String {
        // Map từ category name (từ JSON "name" field) sang tên hiển thị đẹp
        // Sử dụng metadata làm key chính vì nó unique và stable
        val displayNameMap = mapOf(
            // Income categories
            "salary0" to "Lương",
            "IS_COLLECT_INTEREST" to "Thu lãi",
            "IS_OTHER_INCOME" to "Thu nhập khác",
            "IS_INCOMING_TRANSFER" to "Chuyển tiền đến",
            
            // Expense categories
            "foodndrink0" to "Ăn uống",
            "utilities0" to "Tiện ích",
            "phone0" to "Điện thoại",
            "water0" to "Nước",
            "electricity0" to "Điện",
            "gas0" to "Gas",
            "television0" to "Truyền hình",
            "internet0" to "Internet",
            "rentals0" to "Thuê nhà",
            "other_bill0" to "Hóa đơn khác",
            "shopping0" to "Mua sắm",
            "personal_items0" to "Đồ dùng cá nhân",
            "houseware0" to "Đồ gia dụng",
            "makeup0" to "Trang điểm",
            "family0" to "Gia đình",
            "home_maintenance0" to "Bảo trì nhà",
            "home_service0" to "Dịch vụ nhà",
            "pets0" to "Thú cưng",
            "transport0" to "Giao thông",
            "vehicle_maintenance0" to "Bảo trì xe",
            "medical0" to "Y tế",
            "medical_checkup0" to "Khám sức khỏe",
            "fitness0" to "Thể dục",
            "education0" to "Giáo dục",
            "entertainment0" to "Giải trí",
            "streaming_service0" to "Dịch vụ streaming",
            "fun_money0" to "Tiền vui chơi",
            "gifts_donations0" to "Quà tặng",
            "insurance0" to "Bảo hiểm",
            "invest0" to "Đầu tư",
            "IS_OTHER_EXPENSE" to "Chi phí khác",
            "IS_OUTGOING_TRANSFER" to "Chuyển tiền đi",
            
            // Debt/Loan categories
            "IS_LOAN" to "Cho vay",
            "IS_REPAYMENT" to "Trả nợ",
            "IS_PAY_INTEREST" to "Trả lãi",
            "IS_DEBT" to "Nợ",
            "IS_DEBT_COLLECTION" to "Thu nợ"
        )
        
        // Try to find in map by metadata first (most reliable)
        if (categoryMetadata.isNotEmpty()) {
            displayNameMap[categoryMetadata]?.let { return it }
        }
        
        // Try by category title
        if (categoryTitle.isNotEmpty()) {
            displayNameMap[categoryTitle]?.let { return it }
            
            // If title starts with "cate_", try to format it nicely
            if (categoryTitle.startsWith("cate_")) {
                val namePart = categoryTitle.removePrefix("cate_")
                // Convert snake_case to Title Case
                return namePart.split("_")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
            }
        }
        
        // Fallback: return title as is
        return categoryTitle.ifEmpty { categoryMetadata }
    }
    
    /**
     * Generates a consistent color for a category based on its metadata/name.
     * This is a copy of the function from MonthlyReportScreen to avoid circular dependency.
     */
    private fun getCategoryColorForReport(categoryName: String = "", categoryMetadata: String = ""): Color {
        // Map màu dựa trên metadata từ JSON file
        val colorMap = mapOf(
            // Income categories (type 1)
            "salary0" to Color(0xFF4CAF50),
            "IS_COLLECT_INTEREST" to Color(0xFF8BC34A),
            "IS_OTHER_INCOME" to Color(0xFFCDDC39),
            "IS_INCOMING_TRANSFER" to Color(0xFF00BCD4),
            
            // Expense categories (type 2)
            "foodndrink0" to Color(0xFFF44336),
            "utilities0" to Color(0xFF2196F3),
            "phone0" to Color(0xFF03A9F4),
            "water0" to Color(0xFF00BCD4),
            "electricity0" to Color(0xFFFFC107),
            "gas0" to Color(0xFFFF9800),
            "television0" to Color(0xFF9C27B0),
            "internet0" to Color(0xFF673AB7),
            "rentals0" to Color(0xFF3F51B5),
            "other_bill0" to Color(0xFF607D8B),
            "shopping0" to Color(0xFF9C27B0),
            "personal_items0" to Color(0xFFE91E63),
            "houseware0" to Color(0xFF795548),
            "makeup0" to Color(0xFFFF4081),
            "family0" to Color(0xFF009688),
            "home_maintenance0" to Color(0xFF4CAF50),
            "home_service0" to Color(0xFF8BC34A),
            "pets0" to Color(0xFFFF9800),
            "transport0" to Color(0xFF2196F3),
            "vehicle_maintenance0" to Color(0xFF03A9F4),
            "medical0" to Color(0xFFE91E63),
            "medical_checkup0" to Color(0xFFF06292),
            "fitness0" to Color(0xFF4CAF50),
            "education0" to Color(0xFF00BCD4),
            "entertainment0" to Color(0xFFFF9800),
            "streaming_service0" to Color(0xFFFF5722),
            "fun_money0" to Color(0xFFFFC107),
            "gifts_donations0" to Color(0xFFE91E63),
            "insurance0" to Color(0xFF607D8B),
            "invest0" to Color(0xFF4CAF50),
            "IS_OTHER_EXPENSE" to Color(0xFF9E9E9E),
            "IS_OUTGOING_TRANSFER" to Color(0xFF757575),
            
            // Debt/Loan categories (type 3)
            "IS_LOAN" to Color(0xFFFF5722),
            "IS_REPAYMENT" to Color(0xFF4CAF50),
            "IS_PAY_INTEREST" to Color(0xFFFFC107),
            "IS_DEBT" to Color(0xFFF44336),
            "IS_DEBT_COLLECTION" to Color(0xFF8BC34A)
        )
        
        if (categoryMetadata.isNotEmpty()) {
            colorMap[categoryMetadata]?.let { return it }
        }
        
        if (categoryName.isNotEmpty()) {
            colorMap[categoryName]?.let { return it }
        }
        
        val hash = kotlin.math.abs(categoryName.hashCode() + categoryMetadata.hashCode())
        val colors = listOf(
            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0),
            Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3),
            Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688),
            Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
            Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800),
            Color(0xFFFF5722), Color(0xFF795548), Color(0xFF9E9E9E)
        )
        return colors[hash % colors.size]
    }
    
}

