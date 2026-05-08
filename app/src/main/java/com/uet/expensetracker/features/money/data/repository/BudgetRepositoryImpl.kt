package com.uet.expensetracker.features.money.data.repository

import com.uet.expensetracker.features.money.data.data_source.local.dao.BudgetDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.CategoryDao
import com.uet.expensetracker.features.money.data.data_source.local.dao.WalletDao
import com.uet.expensetracker.features.money.data.data_source.local.model.BudgetEntity
import com.uet.expensetracker.features.money.domain.model.Budget
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.repository.BudgetRepository
import com.uet.expensetracker.utils.CurrencyConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val walletDao: WalletDao,
    private val currencyConverter: CurrencyConverter
) : BudgetRepository {

    override suspend fun saveBudget(budget: Budget): Long {
        val budgetEntity = budget.toBudgetEntity()
        return budgetDao.insertBudget(budgetEntity)
    }

    override suspend fun getBudgetById(budgetId: Int): Budget? {
        val budgetEntity = budgetDao.getBudgetById(budgetId) ?: return null

        // Get associated category and wallet
        val categoryEntity = categoryDao.getCategoryById(budgetEntity.categoryId.toString()) ?: return null
        val wallet = if (budgetEntity.walletId == null) {
            // Budget for all wallets: aggregate all
            val allEntities = walletDao.getWallets().first()
            val allWallets = allEntities.map { com.uet.expensetracker.features.money.domain.model.Wallet.fromEntity(it) }
            val mainCurrency = allWallets.find { it.isMainWallet }?.currency ?: allWallets.first().currency

            // convert balances to main currency
            currencyConverter.initialize()
            val totalBalance = allWallets.sumOf {
                if (mainCurrency == it.currency)
                    it.currentBalance
                else {
                    currencyConverter.convert(it.currentBalance, it.currency.currencyCode, mainCurrency.currencyCode) ?:  it.currentBalance
                }
            }

            com.uet.expensetracker.features.money.domain.model.Wallet(
                id = -1,
                walletName = "Total Wallets",
                currentBalance = totalBalance,
                currency = mainCurrency,
                icon = "ic_category_all",
                isMainWallet = false
            )

        } else {
            val entity = walletDao.getWalletById(budgetEntity.walletId).firstOrNull() ?: return null
            com.uet.expensetracker.features.money.domain.model.Wallet.fromEntity(entity)
        }

        return Budget(
            budgetId = budgetEntity.budgetId,
            category = Category(
                id = categoryEntity.id,
                metaData = categoryEntity.metaData,
                title = categoryEntity.title,
                icon = categoryEntity.icon,
                type = when (categoryEntity.type) {
                    1 -> com.uet.expensetracker.features.money.domain.model.CategoryType.INCOME
                    2 -> com.uet.expensetracker.features.money.domain.model.CategoryType.EXPENSE
                    3 -> com.uet.expensetracker.features.money.domain.model.CategoryType.DEBT_LOAN
                    else -> com.uet.expensetracker.features.money.domain.model.CategoryType.UNKNOWN
                },
                parentName = categoryEntity.parentName
            ),
            wallet = wallet,
            amount = budgetEntity.amount,
            fromDate = budgetEntity.fromDate,
            endDate = budgetEntity.endDate,
            isRepeating = budgetEntity.isRepeating
        )
    }

    private suspend fun convertBudgetEntity(budgetEntity: BudgetEntity): Budget? {
        val categoryEntity = categoryDao.getCategoryById(budgetEntity.categoryId.toString()) ?: return null
        val wallet = if (budgetEntity.walletId == null) {
            // Global budget: aggregate all wallets
            val allEntities = walletDao.getWallets().first()
            val allWallets = allEntities.map { com.uet.expensetracker.features.money.domain.model.Wallet.fromEntity(it) }
            val mainCurrency = allWallets.find { it.isMainWallet }?.currency ?: allWallets.first().currency

            // convert balances to main currency
            currencyConverter.initialize()
            val totalBalance = allWallets.sumOf {
                if (mainCurrency == it.currency)
                    it.currentBalance
                else {
                    currencyConverter.convert(it.currentBalance, it.currency.currencyCode, mainCurrency.currencyCode) ?:  it.currentBalance
                }
            }

            com.uet.expensetracker.features.money.domain.model.Wallet(
                id = -1,
                walletName = "All Wallets",
                currentBalance = totalBalance,
                currency = mainCurrency,
                icon = "img_total_wallet",
                isMainWallet = false
            )
        } else {
            val entity = walletDao.getWalletById(budgetEntity.walletId).firstOrNull() ?: return null
            com.uet.expensetracker.features.money.domain.model.Wallet.fromEntity(entity)
        }

        val category = Category(
            id = categoryEntity.id,
            metaData = categoryEntity.metaData,
            title = categoryEntity.title,
            icon = categoryEntity.icon,
            type = when (categoryEntity.type) {
                1 -> com.uet.expensetracker.features.money.domain.model.CategoryType.INCOME
                2 -> com.uet.expensetracker.features.money.domain.model.CategoryType.EXPENSE
                3 -> com.uet.expensetracker.features.money.domain.model.CategoryType.DEBT_LOAN
                else -> com.uet.expensetracker.features.money.domain.model.CategoryType.UNKNOWN
            },
            parentName = categoryEntity.parentName
        )

        return Budget(
            budgetId = budgetEntity.budgetId,
            category = category,
            wallet = wallet,
            amount = budgetEntity.amount,
            fromDate = budgetEntity.fromDate,
            endDate = budgetEntity.endDate,
            isRepeating = budgetEntity.isRepeating
        )
    }

    override suspend fun getBudgetsByCategory(categoryId: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsByCategory(categoryId).map { entities ->
            entities.mapNotNull { convertBudgetEntity(it) }
        }
    }

    override suspend fun getBudgetsByCategory(walletId: Int, categoryId: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsByCategory(walletId, categoryId).map { entities ->
            entities.mapNotNull { convertBudgetEntity(it) }
        }
    }

    override suspend fun getBudgetsByWallet(walletId: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsByWallet(walletId).map { entities ->
            entities.mapNotNull { convertBudgetEntity(it) }
        }
    }

    override suspend fun getActiveBudgetsForDate(date: Date): Flow<List<Budget>> {
        return budgetDao.getActiveBudgetsForDate(date).map { entities ->
            entities.mapNotNull { convertBudgetEntity(it) }
        }
    }

    override suspend fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets().map { entities ->
            entities.mapNotNull { convertBudgetEntity(it) }
        }
    }

    override suspend fun getBudgetsFromTotalWallet(): Flow<List<Budget>> {
        return budgetDao.getBudgetsFromTotalWallet().map { entities ->
            entities.mapNotNull { convertBudgetEntity(it) }
        }
    }

    override suspend fun deleteBudget(budgetId: Int) {
        budgetDao.deleteBudget(budgetId)
    }
} 