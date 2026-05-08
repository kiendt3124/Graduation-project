package com.uet.expensetracker.features.money.ui.monthlyreport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uet.expensetracker.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import com.uet.expensetracker.features.money.ui.transactions.components.MonthSelectionTabs
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.ui.theme.TextSecondary
import com.uet.expensetracker.utils.formatAmountWithCurrency
import kotlin.math.roundToInt

/**
 * Monthly report screen that shows:
 * - Month tabs (includes previous month, this month, and other recent months)
 * - Opening and closing balance for the selected month
 * - Two pie charts: one for expenses by category, one for income by category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    viewModel: MonthlyReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.viewState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.monthly_report_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = TextMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.monthly_report_back_cd),
                            tint = TextMain
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColor.Light.PrimaryColor.containerColor,
                    titleContentColor = AppColor.Light.PrimaryColor.contentColor
                )
            )
        },
        containerColor = AppColor.Light.PrimaryColor.containerColor,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
        ) {
            // Month tabs (reuse existing MonthSelectionTabs for visual consistency)
            MonthSelectionTabs(
                months = uiState.months,
                selectedIndex = uiState.selectedTabIndex,
                onTabSelected = { index ->
                    viewModel.processIntent(MonthlyReportIntent.SelectTab(index))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: stringResource(R.string.monthly_report_error_generic),
                        color = TextSecondary
                    )
                }
            } else {
                MonthlyReportContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    state = uiState
                )
            }
        }
    }
}

/**
 * Content for a single month showing real data from ViewModel.
 */
@Composable
private fun MonthlyReportContent(
    modifier: Modifier = Modifier,
    state: MonthlyReportState,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OpeningClosingBalanceCard(
            openingBalance = formatAmountWithCurrency(state.openingBalance, state.currencySymbol),
            closingBalance = formatAmountWithCurrency(state.closingBalance, state.currencySymbol)
        )

        NetIncomeCard(
            netIncomeLabel = if (state.netIncome >= 0) {
                "+ ${formatAmountWithCurrency(state.netIncome, state.currencySymbol)}"
            } else {
                formatAmountWithCurrency(state.netIncome, state.currencySymbol)
            },
            incomeAmount = formatAmountWithCurrency(state.totalIncome, state.currencySymbol),
            expenseAmount = formatAmountWithCurrency(state.totalExpense, state.currencySymbol)
        )

        GroupReportSection(
            totalIncome = formatAmountWithCurrency(state.totalIncome, state.currencySymbol),
            totalExpense = formatAmountWithCurrency(state.totalExpense, state.currencySymbol),
            incomeByCategory = state.incomeByCategory,
            expenseByCategory = state.expenseByCategory
        )
    }
}

/**
 * Card showing opening and closing balance for the selected month.
 */
@Composable
private fun OpeningClosingBalanceCard(
    openingBalance: String,
    closingBalance: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.monthly_report_opening_balance),
                color = TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = openingBalance,
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.monthly_report_closing_balance),
                color = TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = closingBalance,
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Card showing net income and split between income and expenses.
 * Structurally inspired by the screenshot you provided.
 */
@Composable
private fun NetIncomeCard(
    netIncomeLabel: String,
    incomeAmount: String,
    expenseAmount: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.monthly_report_net_income),
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = netIncomeLabel,
                        color = Color(0xFF00C853),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.monthly_report_view_details),
                    color = AppColor.Light.PrimaryColor.TextButtonColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Simple side‑by‑side income/expense summary placeholders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.monthly_report_income),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = incomeAmount,
                        color = AppColor.Light.InflowAmountColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.monthly_report_expense),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = expenseAmount,
                        color = AppColor.Light.OutflowAmountColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Section that holds the "Báo cáo theo nhóm" header and
 * two pie charts: one for income groups and one for expense groups.
 */
@Composable
private fun GroupReportSection(
    totalIncome: String,
    totalExpense: String,
    incomeByCategory: List<CategoryPieSlice>,
    expenseByCategory: List<CategoryPieSlice>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.monthly_report_by_group),
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryChip(
                title = stringResource(R.string.monthly_report_income),
                amount = totalIncome,
                amountColor = AppColor.Light.InflowAmountColor,
                modifier = Modifier.weight(1f)
            )
            SummaryChip(
                title = stringResource(R.string.monthly_report_expense),
                amount = totalExpense,
                amountColor = AppColor.Light.OutflowAmountColor,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PieChartCard(
                title = stringResource(R.string.monthly_report_income),
                categorySlices = incomeByCategory,
                modifier = Modifier.weight(1f)
            )
            
            PieChartCard(
                title = stringResource(R.string.monthly_report_expense),
                categorySlices = expenseByCategory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Small rounded chip‑like card used for income / expense summary above the charts.
 */
@Composable
private fun SummaryChip(
    title: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = amount,
                color = amountColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Card wrapper around a pie chart showing category breakdown.
 * Displays pie chart with legend below showing category names, colors, and percentages.
 * Shows gray circle if no data is available.
 */
@Composable
private fun PieChartCard(
    title: String,
    categorySlices: List<CategoryPieSlice>,
    modifier: Modifier = Modifier,
) {
    val isEmpty = categorySlices.isEmpty() || categorySlices.sumOf { it.amount } == 0.0
    val emptyColor = Color(0xFF9E9E9E) // Gray color for empty state
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pie chart
            Box(
                modifier = Modifier
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isEmpty) {
                        // Draw gray circle for empty state
                        drawArc(
                            color = emptyColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = true
                        )
                    } else {
                        // Draw pie chart with category slices
                        var startAngle = -90f
                        categorySlices.forEach { slice ->
                            val sweepAngle = (slice.percentage / 100f) * 360f
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend: List of categories with color indicators
            if (!isEmpty) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categorySlices.forEach { slice ->
                        LegendItem(
                            categoryName = slice.categoryDisplayName,
                            color = slice.color,
                            percentage = slice.percentage
                        )
                    }
                }
            } else {
                // Show empty state message
                Text(
                    text = stringResource(R.string.monthly_report_no_data),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Legend item showing category name, color indicator, and percentage.
 */
@Composable
private fun LegendItem(
    categoryName: String,
    color: Color,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Color indicator (small square)
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        // Category name and percentage
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                color = TextMain,
                fontSize = 11.sp,
                maxLines = 2, // Hiển thị tối đa 2 dòng
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${percentage.roundToInt()}%",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Generates a consistent color for a category based on its metadata/name.
 * Uses a predefined color palette mapped to categories from moneylover_categories_v3.json.
 * This function is kept here for PieChartCard to use, but the main logic is in ViewModel.
 */
private fun getCategoryColor(categoryName: String = "", categoryMetadata: String = ""): Color {
    // Map màu dựa trên metadata từ JSON file
    // Sử dụng metadata làm key chính vì nó unique và ổn định
    val colorMap = mapOf(
        // Income categories (type 1)
        "salary0" to Color(0xFF4CAF50), // Lương - Xanh lá
        "IS_COLLECT_INTEREST" to Color(0xFF8BC34A), // Thu lãi - Xanh lá nhạt
        "IS_OTHER_INCOME" to Color(0xFFCDDC39), // Thu nhập khác - Vàng xanh
        "IS_INCOMING_TRANSFER" to Color(0xFF00BCD4), // Chuyển tiền đến - Xanh cyan
        
        // Expense categories (type 2)
        "foodndrink0" to Color(0xFFF44336), // Ăn uống - Đỏ
        "utilities0" to Color(0xFF2196F3), // Tiện ích - Xanh dương
        "phone0" to Color(0xFF03A9F4), // Điện thoại - Xanh dương nhạt
        "water0" to Color(0xFF00BCD4), // Nước - Cyan
        "electricity0" to Color(0xFFFFC107), // Điện - Vàng
        "gas0" to Color(0xFFFF9800), // Gas - Cam
        "television0" to Color(0xFF9C27B0), // TV - Tím
        "internet0" to Color(0xFF673AB7), // Internet - Tím đậm
        "rentals0" to Color(0xFF3F51B5), // Thuê nhà - Xanh dương đậm
        "other_bill0" to Color(0xFF607D8B), // Hóa đơn khác - Xám xanh
        "shopping0" to Color(0xFF9C27B0), // Mua sắm - Tím
        "personal_items0" to Color(0xFFE91E63), // Đồ dùng cá nhân - Hồng
        "houseware0" to Color(0xFF795548), // Đồ gia dụng - Nâu
        "makeup0" to Color(0xFFFF4081), // Trang điểm - Hồng đậm
        "family0" to Color(0xFF009688), // Gia đình - Xanh lá đậm
        "home_maintenance0" to Color(0xFF4CAF50), // Bảo trì nhà - Xanh lá
        "home_service0" to Color(0xFF8BC34A), // Dịch vụ nhà - Xanh lá nhạt
        "pets0" to Color(0xFFFF9800), // Thú cưng - Cam
        "transport0" to Color(0xFF2196F3), // Giao thông - Xanh dương
        "vehicle_maintenance0" to Color(0xFF03A9F4), // Bảo trì xe - Xanh dương nhạt
        "medical0" to Color(0xFFE91E63), // Y tế - Hồng
        "medical_checkup0" to Color(0xFFF06292), // Khám sức khỏe - Hồng nhạt
        "fitness0" to Color(0xFF4CAF50), // Thể dục - Xanh lá
        "education0" to Color(0xFF00BCD4), // Giáo dục - Cyan
        "entertainment0" to Color(0xFFFF9800), // Giải trí - Cam
        "streaming_service0" to Color(0xFFFF5722), // Dịch vụ streaming - Cam đậm
        "fun_money0" to Color(0xFFFFC107), // Tiền vui chơi - Vàng
        "gifts_donations0" to Color(0xFFE91E63), // Quà tặng - Hồng
        "insurance0" to Color(0xFF607D8B), // Bảo hiểm - Xám xanh
        "invest0" to Color(0xFF4CAF50), // Đầu tư - Xanh lá
        "IS_OTHER_EXPENSE" to Color(0xFF9E9E9E), // Chi phí khác - Xám
        "IS_OUTGOING_TRANSFER" to Color(0xFF757575), // Chuyển tiền đi - Xám đậm
        
        // Debt/Loan categories (type 3)
        "IS_LOAN" to Color(0xFFFF5722), // Cho vay - Cam đậm
        "IS_REPAYMENT" to Color(0xFF4CAF50), // Trả nợ - Xanh lá
        "IS_PAY_INTEREST" to Color(0xFFFFC107), // Trả lãi - Vàng
        "IS_DEBT" to Color(0xFFF44336), // Nợ - Đỏ
        "IS_DEBT_COLLECTION" to Color(0xFF8BC34A) // Thu nợ - Xanh lá nhạt
    )
    
    // Try to find in predefined map first by metadata
    if (categoryMetadata.isNotEmpty()) {
        colorMap[categoryMetadata]?.let { return it }
    }
    
    // Try by category name if metadata not found
    if (categoryName.isNotEmpty()) {
        colorMap[categoryName]?.let { return it }
    }
    
    // Generate color from hash if not found in map
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


@Preview(showBackground = true, backgroundColor = 0xFFF6F6F6)
@Composable
private fun MonthlyReportScreenPreview() {
    ExpenseTrackerTheme {
        MonthlyReportScreen(
            navController = rememberNavController()
        )
    }
}


