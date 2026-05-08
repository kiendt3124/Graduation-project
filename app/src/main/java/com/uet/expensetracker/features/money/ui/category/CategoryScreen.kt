package com.uet.expensetracker.features.money.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryGroup
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import com.uet.expensetracker.ui.theme.TextMain
import com.uet.expensetracker.utils.getDrawableResId
import com.uet.expensetracker.utils.getStringResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onCategorySelected: (Category) -> Unit = {},
    onNewCategoryClick: () -> Unit = {}
) {
    val state = viewModel.viewState.collectAsState().value

    val selectedCategoryId = state.selectedCategory?.id

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CategoryEvent.CategorySelected -> {
                    // Make sure the Category is serializable and pass it back
                    onCategorySelected(event.category)
                }
                null -> {} // Handle null case
            }
        }
    }

    Scaffold(
        topBar = {
            CategoryAppBar(
                onBackClick = onBackClick,
                onSortClick = { /* TODO: Implement sort/filter */ },
                onSearchClick = { /* TODO: Implement search */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            CategoryTabs(
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.processIntent(CategoryIntent.SelectTab(it)) }
            )
            CategoryList(
                categories = state.categoryGroups,
                selectedCategoryId = selectedCategoryId,
                onCategoryClick = { category ->
                    viewModel.processIntent(
                        CategoryIntent.SelectCategory(
                            category.id
                        )
                    )
                },
                onNewCategoryClick = onNewCategoryClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAppBar(
    onBackClick: () -> Unit,
    onSortClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.select_category_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button_desc)
                )
            }
        },
//        actions = {
//            IconButton(onClick = onSortClick) {
//                Icon(
//                    imageVector = Icons.Default.Menu,
//                    contentDescription = stringResource(R.string.sort_button_desc)
//                )
//            }
//            IconButton(onClick = onSearchClick) {
//                Icon(
//                    imageVector = Icons.Default.Search,
//                    contentDescription = stringResource(R.string.search_button_desc)
//                )
//            }
//        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun CategoryTabs(selectedTab: CategoryType, onTabSelected: (CategoryType) -> Unit) {
    val tabs = listOf(CategoryType.EXPENSE, CategoryType.INCOME, CategoryType.DEBT_LOAN)
    val tabTitles = listOf(
        stringResource(R.string.tab_expense),
        stringResource(R.string.tab_income),
        stringResource(R.string.tab_debt_loan)
    )
    val selectedIndex = tabs.indexOf(selectedTab)

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, type ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(type) },
                text = { Text(tabTitles[index], fontWeight = FontWeight.Bold) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CategoryList(
    modifier: Modifier = Modifier,
    categories: List<CategoryGroup>,
    selectedCategoryId: Int?,
    onCategoryClick: (Category) -> Unit,
    onNewCategoryClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            NewCategoryItem(onClick = onNewCategoryClick)
            HorizontalDivider(thickness = 1.dp, color = Color.DarkGray.copy(alpha = 0.3f))
        }

        categories.forEach { group ->
            item(key = group.parentName) {
                CategoryGroupHeader(group = group)
            }
            items(group.subCategories, key = { "${group.parentName}_${it.id}" }) { category ->
                val isSubCategory =
                    group.subCategories.size > 1 || group.subCategories.firstOrNull()?.metaData != group.parentName
                CategoryItem(
                    category = category,
                    isSelected = category.id == selectedCategoryId,
                    isSubCategory = isSubCategory,
                    onClick = { onCategoryClick(category) }
                )
            }
            item {
                HorizontalDivider(thickness = 1.dp, color = Color.DarkGray.copy(alpha = 0.3f))
            }
        }

        item {
            HelpItem()
        }
    }
}

@Composable
fun NewCategoryItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = stringResource(R.string.new_category_desc),
            tint = AppColor.Light.PrimaryColor.TextButtonColor,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.new_category_label),
            style = MaterialTheme.typography.bodyLarge,
            color = AppColor.Light.PrimaryColor.TextButtonColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryGroupHeader(group: CategoryGroup) {
    val context = LocalContext.current

    // Resolve parent title: if it's a known string key (e.g. "cate_..."), load from resources;
    // otherwise treat it as plain text (e.g. user-defined category name).
    val parentTitleText = remember(group.parentTitleResName) {
        if (group.parentTitleResName.startsWith("cate_")) {
            context.getString(getStringResId(context, group.parentTitleResName))
        } else {
            group.parentTitleResName
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = getDrawableResId(context, group.parentIconResName)),
            contentDescription = parentTitleText + " icon",
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = parentTitleText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray.copy(alpha = 0.5f))
}

@Composable
fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    isSubCategory: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = if (isSubCategory) 32.dp else 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = getDrawableResId(context, category.icon)),
            contentDescription = stringResource( getStringResId(context, category.title)) + " icon",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(getStringResId(context, category.title)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.selected_category_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = if (isSubCategory) 32.dp else 16.dp),
        thickness = 0.5.dp,
        color = Color.DarkGray.copy(alpha = 0.5f)
    )
}

@Composable
fun HelpItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.need_help_message),
            color = TextMain,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_help_outline),
            contentDescription = null,
            tint = TextMain,
            modifier = Modifier.size(16.dp)
        )
    }
}

fun getPreviewCategoryData(): Map<CategoryType, List<CategoryGroup>> {
    val expenseGroup1 = CategoryGroup(
        parentName = "bills_utilities",
        parentTitleResName = "cate_utilities",
        parentIconResName = "icon_135",
        type = CategoryType.EXPENSE,
        subCategories = listOf(
            Category(
                id = 1,
                metaData = "electricity0",
                title = "cate_electricity",
                icon = "icon_125",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 2,
                metaData = "gas0",
                title = "cate_gas",
                icon = "icon_139",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 3,
                metaData = "internet0",
                title = "cate_internet",
                icon = "icon_126",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 4,
                metaData = "other_bill0",
                title = "cate_other_utility_bill",
                icon = "icon_138",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 5,
                metaData = "phone0",
                title = "cate_phone",
                icon = "icon_134",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 6,
                metaData = "rentals0",
                title = "cate_rentals",
                icon = "icon_136",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 7,
                metaData = "television0",
                title = "cate_television",
                icon = "icon_84",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            ),
            Category(
                id = 8,
                metaData = "water0",
                title = "cate_water",
                icon = "icon_124",
                type = CategoryType.EXPENSE,
                parentName = "bills_utilities"
            )
        )
    )
    val expenseGroup2 = CategoryGroup(
        parentName = "education",
        parentTitleResName = "cate_education",
        parentIconResName = "ic_category_education",
        type = CategoryType.EXPENSE,
        subCategories = listOf(
            Category(
                id = 9,
                metaData = "education0",
                title = "cate_education",
                icon = "ic_category_education",
                type = CategoryType.EXPENSE,
                parentName = null
            )
        )
    )
    val expenseGroup3 = CategoryGroup(
        parentName = "entertainment",
        parentTitleResName = "cate_entertainment",
        parentIconResName = "ic_category_entertainment",
        type = CategoryType.EXPENSE,
        subCategories = listOf(
            Category(
                id = 10,
                metaData = "entertainment0",
                title = "cate_entertainment",
                icon = "ic_category_entertainment",
                type = CategoryType.EXPENSE,
                parentName = null
            )
        )
    )

    val incomeGroup1 = CategoryGroup(
        parentName = "collect_interest",
        parentTitleResName = "cate_collect_interest",
        parentIconResName = "ic_category_interestmoney",
        type = CategoryType.INCOME,
        subCategories = listOf(
            Category(
                id = 11,
                metaData = "IS_COLLECT_INTEREST",
                title = "cate_collect_interest",
                icon = "ic_category_interestmoney",
                type = CategoryType.INCOME,
                parentName = null
            )
        )
    )
    val incomeGroup2 = CategoryGroup(
        parentName = "incoming_transfer",
        parentTitleResName = "cate_incoming_transfer",
        parentIconResName = "icon_143",
        type = CategoryType.INCOME,
        subCategories = listOf(
            Category(
                id = 12,
                metaData = "IS_INCOMING_TRANSFER",
                title = "cate_incoming_transfer",
                icon = "icon_143",
                type = CategoryType.INCOME,
                parentName = null
            )
        )
    )
    val incomeGroup3 = CategoryGroup(
        parentName = "income_other",
        parentTitleResName = "cate_income_other",
        parentIconResName = "ic_category_other_income",
        type = CategoryType.INCOME,
        subCategories = listOf(
            Category(
                id = 13,
                metaData = "IS_OTHER_INCOME",
                title = "cate_income_other",
                icon = "ic_category_other_income",
                type = CategoryType.INCOME,
                parentName = null
            )
        )
    )
    val incomeGroup4 = CategoryGroup(
        parentName = "salary",
        parentTitleResName = "cate_salary",
        parentIconResName = "ic_category_salary",
        type = CategoryType.INCOME,
        subCategories = listOf(
            Category(
                id = 14,
                metaData = "salary0",
                title = "cate_salary",
                icon = "ic_category_salary",
                type = CategoryType.INCOME,
                parentName = null
            )
        )
    )

    val debtLoanGroup1 = CategoryGroup(
        parentName = "debt",
        parentTitleResName = "cate_debt",
        parentIconResName = "ic_category_debt",
        type = CategoryType.DEBT_LOAN,
        subCategories = listOf(
            Category(
                id = 15,
                metaData = "IS_DEBT",
                title = "cate_debt",
                icon = "ic_category_debt",
                type = CategoryType.DEBT_LOAN,
                parentName = null
            )
        )
    )
    val debtLoanGroup2 = CategoryGroup(
        parentName = "debt collection",
        parentTitleResName ="cate_debt_collection",
        parentIconResName = "icon_140",
        type = CategoryType.DEBT_LOAN,
        subCategories = listOf(
            Category(
                id = 16,
                metaData = "IS_DEBT_COLLECTION",
                title = "cate_debt_collection",
                icon = "icon_140",
                type = CategoryType.DEBT_LOAN,
                parentName = null
            )
        )
    )
    val debtLoanGroup3 = CategoryGroup(
        parentName = "loan",
  parentTitleResName = "cate_loan",
  parentIconResName = "ic_category_loan",
        type = CategoryType.DEBT_LOAN,
        subCategories = listOf(
            Category(
                id = 17,
                metaData = "IS_LOAN",
                title = "cate_loan",
                icon = "ic_category_loan",
                type = CategoryType.DEBT_LOAN,
                parentName = null
            )
        )
    )
    val debtLoanGroup4 = CategoryGroup(
        parentName = "repayment ",
        parentTitleResName = "cate_repayment",
        parentIconResName = "icon_141",
        type = CategoryType.DEBT_LOAN,
        subCategories = listOf(
            Category(
                id = 18,
                metaData = "IS_REPAYMENT",
                title = "cate_repayment",
                icon = "icon_141",
                type = CategoryType.DEBT_LOAN,
                parentName = null
            )
        )
    )

    return mapOf(
        CategoryType.EXPENSE to listOf(expenseGroup1, expenseGroup2, expenseGroup3),
        CategoryType.INCOME to listOf(incomeGroup1, incomeGroup2, incomeGroup3, incomeGroup4),
        CategoryType.DEBT_LOAN to listOf(
            debtLoanGroup1,
            debtLoanGroup2,
            debtLoanGroup3,
            debtLoanGroup4
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
fun CategoryScreenPreviewExpense() {
    ExpenseTrackerTheme(darkTheme = true) {
        val previewData = getPreviewCategoryData()
        var selectedTab by remember { mutableStateOf(CategoryType.EXPENSE) }
        var selectedCatId by remember { mutableStateOf<Int?>(null) }

        Scaffold(
            topBar = { CategoryAppBar({}, {}, {}) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(Modifier.padding(padding)) {
                CategoryTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                CategoryList(
                    categories = previewData[selectedTab] ?: emptyList(),
                    selectedCategoryId = selectedCatId,
                    onCategoryClick = { selectedCatId = it.id },
                    onNewCategoryClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
fun CategoryScreenPreviewIncome() {
    ExpenseTrackerTheme(darkTheme = true) {
        val previewData = getPreviewCategoryData()
        var selectedTab by remember { mutableStateOf(CategoryType.INCOME) }
        var selectedCatId by remember { mutableStateOf<Int?>(null) }

        Scaffold(
            topBar = { CategoryAppBar({}, {}, {}) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(Modifier.padding(padding)) {
                CategoryTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                CategoryList(
                    categories = previewData[selectedTab] ?: emptyList(),
                    selectedCategoryId = selectedCatId,
                    onCategoryClick = { selectedCatId = it.id },
                    onNewCategoryClick = {}
                )
            }
        }
    }
}