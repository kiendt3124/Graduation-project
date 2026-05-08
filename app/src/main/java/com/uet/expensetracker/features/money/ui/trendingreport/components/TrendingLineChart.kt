package com.uet.expensetracker.features.money.ui.trendingreport.components

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.features.money.domain.model.Category
import com.uet.expensetracker.features.money.domain.model.CategoryType
import com.uet.expensetracker.features.money.domain.model.Currency
import com.uet.expensetracker.features.money.domain.model.Transaction
import com.uet.expensetracker.features.money.domain.model.Wallet
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.util.Calendar
import com.uet.expensetracker.features.money.domain.model.TransactionType
import androidx.compose.ui.platform.LocalInspectionMode
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import kotlinx.coroutines.runBlocking
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import java.util.Date
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.compose.common.shape.toVicoShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.uet.expensetracker.ui.theme.AppColor.Light.InflowAmountColor
import com.uet.expensetracker.ui.theme.AppColor.Light.OutflowAmountColor
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.vicoTheme

@Composable
fun TrendingLineChart(
    transactions: List<Transaction>,
    isIncome: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fillColor = remember(isIncome) {
        if (isIncome) {
            // Use OutflowAmountColor for inflow transactions
            Fill(InflowAmountColor.toArgb())
        } else {
            // Use OutflowAmountColor for outflow transactions
            Fill(OutflowAmountColor.toArgb())
        }
    }

    // Aggregate transaction amounts by day of month within the same month-year, building data only up to last available day
    val dailySums = remember(transactions) {
        if (transactions.isEmpty()) {
            emptyList<Pair<Double, Double>>()
        } else {
            // Reference month and year from first transaction
            val firstDate = transactions.first().transactionDate
            val refCal = Calendar.getInstance().apply { time = firstDate }
            val refMonth = refCal.get(Calendar.MONTH)
            val refYear = refCal.get(Calendar.YEAR)
            val daysCount = refCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Determine the last day with data (up to today)
            val today = Calendar.getInstance().apply { time = Date() }
            val lastAvailableDay = (if (today.get(Calendar.MONTH) == refMonth && today.get(Calendar.YEAR) == refYear)
                today.get(Calendar.DAY_OF_MONTH) else daysCount).coerceAtMost(daysCount)
            // Filter and group transactions by day
            val grouped = transactions
                .filter { cal -> Calendar.getInstance().apply { time = cal.transactionDate }.let { c ->
                    c.get(Calendar.MONTH) == refMonth && c.get(Calendar.YEAR) == refYear
                } }
                .groupBy { cal -> Calendar.getInstance().apply { time = cal.transactionDate }.get(Calendar.DAY_OF_MONTH) }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            // Build data list for days 1 through lastAvailableDay
            (1..lastAvailableDay).map { day ->
                day.toDouble() to (grouped[day] ?: 0.0)
            }
        }
    }

    // Detect preview mode
    val isPreview = LocalInspectionMode.current

    // Compute full month length for X-axis range
    val daysInMonth: Double = remember(transactions) {
        Calendar.getInstance().apply { time = transactions.firstOrNull()?.transactionDate ?: Date() }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
            .toDouble()
    }

    // Compute raw max Y for data
    val rawMaxY = remember(transactions) { dailySums.maxOfOrNull { it.second } ?: 0.0 }
    val segments = 4
    // Determine nice step and max Y
    val niceStep = remember(rawMaxY) {
        val step = niceNumber(rawMaxY / segments, roundUp = true)
        if (step <= 0.0) 1.0 else step
    }
    val niceMaxY = remember(niceStep) { niceStep * segments }

    // Compute month for X-axis labels
    val monthNumber = remember(transactions) {
        Calendar.getInstance().apply { time = transactions.firstOrNull()?.transactionDate ?: Date() }
            .get(Calendar.MONTH) + 1
    }
    val monthStr = remember(monthNumber) { monthNumber.toString().padStart(2, '0') }
    // Compute year for marker date label
    val yearInt = remember(transactions) {
        Calendar.getInstance().apply { time = transactions.firstOrNull()?.transactionDate ?: Date() }
            .get(Calendar.YEAR)
    }
    val yearStr = remember(yearInt) { yearInt.toString() }

    // Producer for building and animating the chart model, with synchronous initial data in preview
    val modelProducer = remember { CartesianChartModelProducer() }
    val chartProducer = remember(dailySums) {
        modelProducer.apply {
            if (isPreview) {
                runBlocking {
                    runTransaction {
                        lineSeries {
                            series(
                                x = dailySums.map { it.first },
                                y = dailySums.map { it.second }
                            )
                        }
                    }
                }
            }
        }
    }

    // Populate the model on data change (runtime)
    LaunchedEffect(dailySums) {
        if (!isPreview) {
            chartProducer.runTransaction {
                lineSeries {
                    series(
                        x = dailySums.map { it.first },
                        y = dailySums.map { it.second }
                    )
                }
            }
        }
    }

    // Create marker label formatter for showing abbreviated y-values
    val markerFormatter = remember {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            targets.joinToString(", ") { target ->
                val entry = (target as? LineCartesianLayerMarkerTarget)?.points?.first()?.entry
                val day = entry?.x?.toInt()?.toString()?.padStart(2, '0') ?: ""
                val amount = entry?.y ?: 0.0
                val amountStr = abbreviate(amount)
                // Label includes full date and abbreviated amount
                "${day}/${monthStr}/${yearStr}: $amountStr"
            }
        }
    }

    val shapeComponent = rememberShapeComponent(
        fill = Fill.Black,
        shape = CircleShape.toVicoShape()
    )
    // Create a text label component with background box for marker
    val markerLabelComponent = rememberTextComponent(
        color = Color.White,
        textSize = 12.sp,
        background = rememberShapeComponent(
            fill = fillColor,
            shape = RoundedCornerShape(4.dp).toVicoShape()
        ),
        padding = insets(6.dp, 4.dp)
    )

    // Remember interactive marker composable
    val marker = rememberDefaultCartesianMarker(
        label = markerLabelComponent,
        valueFormatter = markerFormatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.Top,
        indicator = { color -> shapeComponent.copy(fill = fillColor) },
        indicatorSize = 10.dp,
        guideline = rememberAxisGuidelineComponent()
    )

    // Create a persistent marker showing only the indicator
    val persistentMarker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = Color.Transparent,
            textSize = 0.sp
        ),
        valueFormatter = DefaultCartesianMarker.ValueFormatter { _, _ -> "" },
        labelPosition = DefaultCartesianMarker.LabelPosition.Top,
        indicator = { shapeComponent.copy(fill = fillColor) },
        indicatorSize = 8.dp,
        guideline = null
    )

    // Configure the line chart with custom line color, fixed ranges, and formatting
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            // Use custom fillColor for the line
            lineProvider = LineCartesianLayer.LineProvider.series(
                listOf(
                    LineCartesianLayer.rememberLine(
                        fill = remember(fillColor) { LineCartesianLayer.LineFill.single(fill(Color(fillColor.color))) }
                    )
                )
            ),
            rangeProvider = CartesianLayerRangeProvider.fixed(
                minX = 1.0,
                maxX = daysInMonth,
                minY = 0.0,
                maxY = niceMaxY
            )
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            // Show only first and last day, formatted as DD/MM
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                val day = value.toInt().toString().padStart(2, '0')
                "$day/$monthStr"
            },
            itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                spacing = { _ -> (daysInMonth.toInt() - 1) },
                offset = { _ -> 0 },
                shiftExtremeLines = false,
                addExtremeLabelPadding = true
            ),
            guideline = null,
            label = rememberAxisLabelComponent(
                color = vicoTheme.lineColor
            )
        ),
        endAxis = VerticalAxis.rememberEnd(
            valueFormatter = CartesianValueFormatter { _, value, _ -> abbreviate(value) },
            itemPlacer = VerticalAxis.ItemPlacer.step({ _ -> niceStep }, shiftTopLines = true),
            guideline = rememberAxisGuidelineComponent(),
            label = rememberAxisLabelComponent(
                color = vicoTheme.lineColor
            ),
            line = rememberLineComponent(
                fill = fill(Color.Transparent)
            )
        ),
        // Use interactive marker on touch
        marker = marker,
        // Add a persistent marker (only dot) at the last day of the month
        persistentMarkers = { persistentMarker at daysInMonth },
        getXStep = { _ -> 1.0 }
    )

    // Host composable to render the chart
    CartesianChartHost(
        chart = chart,
        modelProducer = chartProducer,
        // Disable horizontal scrolling, show all data in view
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
    )
}

@Preview
@Composable
fun TrendingLineChartPreview() {
    // Create sample wallet for transactions
    val sampleWallet = Wallet(
        id = 1,
        walletName = "Cash",
        currentBalance = 10000000.0,
        currency = Currency(
            id = 1,
            currencyName = "Vietnamese Dong",
            currencyCode = "VND",
            symbol = "₫"
        )
    )

    // Create sample category
    val sampleCategory = Category(
        id = 1,
        metaData = "food",
        title = "Food & Beverages",
        icon = "ic_food",
        type = CategoryType.EXPENSE
    )

    // Generate 15 days of sample transactions
    val sampleTransactions = buildList {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -10) // Start 14 days ago

        repeat(15) { day ->
            // Add 1-3 transactions per day
            repeat((1..3).random()) {
                add(
                    Transaction(
                        id = day * 3 + it,
                        wallet = sampleWallet,
                        transactionType = if (Math.random() > 0.7) TransactionType.INFLOW else TransactionType.OUTFLOW,
                        amount = (100000..1000000).random().toDouble(),
                        transactionDate = calendar.time,
                        description = "Transaction ${day * 3 + it}",
                        category = sampleCategory
                    )
                )
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    TrendingLineChart(
        transactions = sampleTransactions
    )
}

// Add nice number and abbreviation helpers
private fun niceNumber(value: Double, roundUp: Boolean): Double {
    if (value <= 0) return 0.0
    val exponent = floor(log10(value))
    val fraction = value / 10.0.pow(exponent)
    val niceFraction = if (roundUp) {
        when {
            fraction <= 1 -> 1.0
            fraction <= 2 -> 2.0
            fraction <= 5 -> 5.0
            else -> 10.0
        }
    } else {
        when {
            fraction < 1.5 -> 1.0
            fraction < 3 -> 2.0
            fraction < 7 -> 5.0
            else -> 10.0
        }
    }
    return niceFraction * 10.0.pow(exponent)
}

fun abbreviate(value: Double): String {
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
 