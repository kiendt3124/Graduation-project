package com.uet.expensetracker.features.money.ui.budgetdetails.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.features.money.ui.budgetdetails.GraphData
import com.uet.expensetracker.features.money.ui.budgetdetails.GraphPoint


import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.common.Fill
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import java.util.Calendar
import java.time.format.DateTimeFormatter

private fun abbreviate(value: Double): String {
    return when {
        value >= 1_000_000 -> {
            val v = value / 1_000_000
            if (v % 1.0 == 0.0) "${v.toInt()}M" else "${"%.1f".format(v)}M"
        }

        value >= 1_000 -> {
            val v = value / 1_000
            if (v % 1.0 == 0.0) "${v.toInt()}K" else "${"%.1f".format(v)}K"
        }

        else -> value.toInt().toString()
    }
}

@Composable
fun BudgetLineChart(
    graphData: GraphData,
    budgetLimit: Double,
    budgetStart: Date,
    budgetEnd: Date,
    modifier: Modifier = Modifier
) {
    // Build the inclusive date-range from budgetStart to budgetEnd
    val startDate = remember(budgetStart) {
        budgetStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val endDate = remember(budgetEnd) {
        budgetEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val days = remember(startDate, endDate) {
        ChronoUnit.DAYS.between(startDate, endDate).toInt()
    }
    val dateRange = remember(startDate, days) {
        (0..days).map { startDate.plusDays(it.toLong()) }
    }

    // Sum up your graphData by LocalDate
    val groupedByDate = remember(graphData) {
        graphData.points.groupBy { gp ->
            gp.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { it.value.sumOf { p -> p.value } }
    }

    // Build sequential day offsets and y-values for the budget period
    val offsets = remember(dateRange) { dateRange.indices.map { it.toDouble() } }
    val yValues = remember(dateRange, groupedByDate) {
        dateRange.map { groupedByDate[it] ?: 0.0 }
    }
    // Formatter for X-axis labels (day/month)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d/M") }

    // Span from day 0 to last offset
    val minX = 0.0
    val maxX = offsets.lastOrNull() ?: 0.0
    val limitX = listOf(minX, maxX)
    val limitY = listOf(budgetLimit, budgetLimit)

    // Determine maxY for chart range
    val rawMaxY = maxOf(budgetLimit, yValues.maxOrNull() ?: 0.0)
    val maxY = rawMaxY * 1.1

    // Chart model
    // Detect preview mode
    val isPreview = LocalInspectionMode.current
    val modelProducer = remember {
        CartesianChartModelProducer().apply {
            if (isPreview) {
                runBlocking {
                    runTransaction {
                        lineSeries {
                            series(x = offsets, y = yValues)
                            series(x = limitX, y = limitY)
                        }
                    }
                }
            }
        }
    }

    // Update on data change
    LaunchedEffect(graphData) {
        modelProducer.runTransaction {
            lineSeries {
                series(x = offsets, y = yValues)
                series(x = limitX, y = limitY)
            }
        }
    }
    // Chart with two line series: data and limit
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                listOf(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(
                            fill(
                                Color(0xFF4CAF50).copy(alpha = 0.8f)
                            )
                        ),
                    ),
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(
                            fill(
                                Color.Red.copy(alpha = 0.5f)
                            )
                        ),
                    )
                )
            ),
            rangeProvider = CartesianLayerRangeProvider.fixed(
                minX = minX - 0.5,
                maxX = maxX + 0.5,
                minY = 0.0,
                maxY = maxY * 1.1 + 0.1  // hoặc + một giá trị nhỏ
            )
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                val idx = value.toInt().coerceIn(dateRange.indices)
                dateFormatter.format(dateRange[idx])
            },
            itemPlacer = HorizontalAxis.ItemPlacer.aligned({ _ -> 1 }, shiftExtremeLines = true),
            guideline = null
        ),
        endAxis = VerticalAxis.rememberEnd(
            valueFormatter = CartesianValueFormatter { _, value, _ -> abbreviate(value) }
        ),
        getXStep = { _ -> 1.0 },
        marker = null,
        persistentMarkers = null
    )
    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )


}

@Preview(showBackground = true)
@Composable
fun PreviewBudgetLineChart() {
    val today = Date()
    val points = listOf(
        GraphPoint(Date(today.time - 5L * 24 * 60 * 60 * 1000), 200.0),
        GraphPoint(Date(today.time - 4L * 24 * 60 * 60 * 1000), 400.0),
        GraphPoint(Date(today.time - 3L * 24 * 60 * 60 * 1000), 600.0),
        GraphPoint(Date(today.time - 2L * 24 * 60 * 60 * 1000), 800.0),
        GraphPoint(Date(today.time - 1L * 24 * 60 * 60 * 1000), 400.0),
        GraphPoint(today, 500.0)
    )
    val graphData = GraphData(points)
    BudgetLineChart(
        graphData   = graphData,
        budgetLimit = 700.0,
        budgetStart = Date(today.time - 55L * 24 * 60 * 60 * 1000),
        budgetEnd   = today,
        modifier    = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBudgetLineChart2() {
    val today = Date()
    val points = listOf(
        GraphPoint(Date(today.time - 5L * 24 * 60 * 60 * 1000), 200.0),
        GraphPoint(Date(today.time - 4L * 24 * 60 * 60 * 1000), 400.0),
        GraphPoint(Date(today.time - 3L * 24 * 60 * 60 * 1000), 600.0),
        GraphPoint(Date(today.time - 2L * 24 * 60 * 60 * 1000), 800.0),
        GraphPoint(Date(today.time - 1L * 24 * 60 * 60 * 1000), 400.0),
        GraphPoint(today, 500.0)
    )
    val graphData = GraphData(points)
    BudgetLineChart(
        graphData   = graphData,
        budgetLimit = 700.0,
        budgetStart = Date(today.time - 10L * 24 * 60 * 60 * 1000),
        budgetEnd   = today,
        modifier    = Modifier.padding(16.dp)
    )
}