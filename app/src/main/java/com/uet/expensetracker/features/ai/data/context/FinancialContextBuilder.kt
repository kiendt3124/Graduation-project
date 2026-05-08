package com.uet.expensetracker.features.ai.data.context

import com.uet.expensetracker.features.ai.data.remote.dto.context.FinancialContextDto
import com.uet.expensetracker.features.ai.data.remote.dto.context.MonthlyTotalDto
import com.uet.expensetracker.features.ai.data.remote.dto.context.TransactionDto
import com.uet.expensetracker.features.ai.data.remote.dto.context.WalletDto
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import com.uet.expensetracker.features.money.domain.repository.WalletRepository
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Builds a lightweight financial context snapshot for AI sync.
 */
class FinancialContextBuilder @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun build(): FinancialContextDto {
        val wallets = walletRepository.getWallets().first()

        val now = Instant.now()
        val start30Days = now.minus(30, ChronoUnit.DAYS)
        val recentTransactions = transactionRepository.searchTransactionsByDateRange(
            startDate = start30Days.toEpochMilli(),
            endDate = now.toEpochMilli(),
            walletId = null
        ).first()

        val categorySpending = recentTransactions
            .filter { it.transactionType == TransactionType.OUTFLOW && !it.excludeFromReport }
            .groupBy { it.category.metaData }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val monthlyTotals = buildMonthlyTotals(6)

        val totalBalance = wallets.sumOf { it.wallet.currentBalance }
        val monthlyAvgIncome = monthlyTotals.mapNotNull { it.income }.averageOrNull()
        val monthlyAvgExpense = monthlyTotals.mapNotNull { it.expense }.averageOrNull()
        val savingsRate = monthlyAvgIncome?.takeIf { it > 0 }?.let { inc ->
            val exp = monthlyAvgExpense ?: 0.0
            (inc - exp) / inc
        }

        return FinancialContextDto(
            wallets = wallets.map {
                WalletDto(
                    id = it.wallet.id,
                    name = it.wallet.walletName,
                    balance = it.wallet.currentBalance,
                    currencyCode = it.currency.currencyCode
                )
            },
            recentTransactions = recentTransactions.map { tx ->
                TransactionDto(
                    id = tx.id,
                    amount = tx.amount,
                    type = tx.transactionType.name,
                    date = dateFormatter.format(
                        tx.transactionDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    ),
                    description = tx.description,
                    categoryMetadata = tx.category.metaData,
                    walletId = tx.wallet.id
                )
            },
            categorySpending = categorySpending,
            monthlyTotals = monthlyTotals,
            analyticsCache = emptyMap(),
            totalBalance = totalBalance,
            monthlyAvgIncome = monthlyAvgIncome,
            monthlyAvgExpense = monthlyAvgExpense,
            savingsRate = savingsRate
        )
    }

    private suspend fun buildMonthlyTotals(monthCount: Int): List<MonthlyTotalDto> {
        val now = YearMonth.now()
        val months = (0 until monthCount).map { now.minusMonths(it.toLong()) }
        return months.map { ym ->
            val txs = transactionRepository.getTransactionsByMonth(
                year = ym.year,
                month = ym.monthValue,
                walletId = null
            ).first()

            val income = txs.filter { it.transactionType == TransactionType.INFLOW && !it.excludeFromReport }
                .sumOf { it.amount }
            val expense = txs.filter { it.transactionType == TransactionType.OUTFLOW && !it.excludeFromReport }
                .sumOf { it.amount }

            MonthlyTotalDto(
                month = ym.toString(), // yyyy-MM
                income = income,
                expense = expense
            )
        }.reversed() // oldest -> newest
    }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}

