package com.uet.expensetracker.features.ai.analytics

import com.uet.expensetracker.features.ai.data.remote.dto.DataRequestDto
import com.uet.expensetracker.features.ai.data.remote.dto.DataResponseDto
import com.uet.expensetracker.features.ai.data.remote.dto.DatasetTypeDto
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.TransactionType
import com.uet.expensetracker.features.money.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

class AnalyticsQueryEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend fun execute(request: DataRequestDto, zoneId: ZoneId = ZoneId.systemDefault()): DataResponseDto {
        val months = resolveMonths(request.timeRange)
        val monthlyTotals = if (request.requiredDatasets.contains(DatasetTypeDto.MONTHLY_TOTALS)) {
            buildMonthlyTotals(months, zoneId)
        } else null
        val topCategories = if (request.requiredDatasets.contains(DatasetTypeDto.TOP_CATEGORIES)) {
            val topK = request.topK ?: 5
            buildTopCategories(months, topK, zoneId)
        } else null
        return DataResponseDto(monthlyTotals = monthlyTotals, topCategories = topCategories)
    }

    private fun resolveMonths(timeRange: String): List<YearMonth> {
        val now = YearMonth.now()
        return when (timeRange.uppercase(Locale.US)) {
            "LAST_3_MONTHS" -> listOf(now.minusMonths(2), now.minusMonths(1), now)
            "LAST_6_MONTHS" -> (5 downTo 0).map { now.minusMonths(it.toLong()) }
            "LAST_12_MONTHS" -> (11 downTo 0).map { now.minusMonths(it.toLong()) }
            else -> listOf(now) // THIS_MONTH default
        }
    }

    private suspend fun buildMonthlyTotals(months: List<YearMonth>, zoneId: ZoneId): List<DataResponseDto.MonthlyTotalDto> {
        return months.map { ym ->
            val tx = getTransactionsForMonth(ym, null).filter { !it.excludeFromReport }
            val income = tx.filter { it.transactionType == TransactionType.INFLOW }.sumOf { it.amount }
            val expense = tx.filter { it.transactionType == TransactionType.OUTFLOW }.sumOf { it.amount }
            DataResponseDto.MonthlyTotalDto(
                month = ym.toString(), // yyyy-MM
                income = income,
                expense = expense
            )
        }
    }

    private suspend fun buildTopCategories(
        months: List<YearMonth>,
        topK: Int,
        zoneId: ZoneId
    ): List<DataResponseDto.CategoryTotalDto> {
        val all = months.flatMap { ym ->
            getTransactionsForMonth(ym, null)
        }.filter { !it.excludeFromReport && it.transactionType == TransactionType.OUTFLOW }

        val grouped = all.groupBy { it.category.metaData }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(topK)

        return grouped.map { (meta, amount) ->
            DataResponseDto.CategoryTotalDto(categoryMetadata = meta, amount = amount)
        }
    }

    private suspend fun getTransactionsForMonth(yearMonth: YearMonth, walletId: Int?): List<Transaction> {
        val flow = transactionRepository.getTransactionsByMonth(
            year = yearMonth.year,
            month = yearMonth.monthValue,
            walletId = walletId
        )
        return flow.first()
    }
}



