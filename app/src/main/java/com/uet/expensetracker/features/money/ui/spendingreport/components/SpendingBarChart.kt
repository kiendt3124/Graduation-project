package com.uet.expensetracker.features.money.ui.spendingreport.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uet.expensetracker.R
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.runBlocking
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.uet.expensetracker.features.money.ui.trendingreport.components.abbreviate
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shape.toVicoShape
import com.patrykandpatrick.vico.core.common.Defaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.uet.expensetracker.ui.theme.AppColor
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore

@Composable
private fun rememberSpendingColumnProvider(
    lastWeekComp: LineComponent,
    thisWeekComp: LineComponent,
): ColumnCartesianLayer.ColumnProvider = remember(lastWeekComp, thisWeekComp) {
    object : ColumnCartesianLayer.ColumnProvider {
        override fun getColumn(
            entry: ColumnCartesianLayerModel.Entry,
            seriesIndex: Int,
            extraStore: ExtraStore,
        ) = if (entry.x.toInt() == 1) lastWeekComp else thisWeekComp

        override fun getWidestSeriesColumn(
            seriesIndex: Int,
            extraStore: ExtraStore,
        ) = lastWeekComp
    }
}

@Composable
fun SpendingBarChart(
    lastAmount: Double,
    thisAmount: Double,
    modifier: Modifier = Modifier
) {
    val lastWeekLabel = stringResource(R.string.spending_chart_last_week)
    val thisWeekLabel = stringResource(R.string.spending_chart_this_week)

    val fillColor = remember {
        Fill(AppColor.Light.OutflowAmountColor.toArgb())
    }
    // Define lighter and normal colors and their components for Last Week and This Week
    val lightColor = Color(fillColor.color).copy(alpha = 0.5f)
    val darkColor = Color(fillColor.color)
    val lastWeekComponent = rememberLineComponent(
        fill = fill(lightColor),
        thickness = Defaults.COLUMN_WIDTH.dp,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp).toVicoShape()
    )
    val thisWeekComponent = rememberLineComponent(
        fill = fill(darkColor),
        thickness = Defaults.COLUMN_WIDTH.dp,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp).toVicoShape()
    )
    // Detect preview mode
    val isPreview = LocalInspectionMode.current
    // Custom column provider to choose color per entry (centers bars under labels)
    val columnProvider = rememberSpendingColumnProvider(lastWeekComponent, thisWeekComponent)
    // Prepare data transaction producer with synchronous setup in preview
    val chartProducer = remember(lastAmount, thisAmount) {
        CartesianChartModelProducer().apply {
            if (isPreview) {
                runBlocking {
                    runTransaction {
                        columnSeries {
                            series(x = listOf(1, 2), y = listOf(lastAmount, thisAmount))
                        }
                    }
                }
            }
        }
    }

    // Populate chart model when amounts change (runtime)
    LaunchedEffect(lastAmount, thisAmount) {
        if (!isPreview) {
            chartProducer.runTransaction {
                columnSeries {
                    series(x = listOf(1, 2), y = listOf(lastAmount, thisAmount))
                }
            }
        }
    }

    // Create the chart with rounded-top bars and customized axes
    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = columnProvider,
            rangeProvider = com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider.fixed(
                minY = 0.0,
                maxY = maxOf(lastAmount, thisAmount, 10.0)
            )
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            // Show only Last week and This week
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                if (value.toInt() == 1) lastWeekLabel else thisWeekLabel
            },
            itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                spacing = { _ -> 1 }, offset = { _ -> 0 }, shiftExtremeLines = false, addExtremeLabelPadding = true
            ),
            guideline = null,
            label = rememberAxisLabelComponent(
                color = vicoTheme.lineColor
            ),
        ),
        endAxis = VerticalAxis.rememberEnd(
            // Show only min and max with abbreviated format
            valueFormatter = CartesianValueFormatter { _, value, _ -> abbreviate(value) },
            itemPlacer = VerticalAxis.ItemPlacer.count({ _ -> 2 }, shiftTopLines = true),
            guideline = null,
            label = rememberAxisLabelComponent(
                color = vicoTheme.lineColor
            ),
            line = rememberLineComponent(
                fill = fill(Color.Transparent)
            )
        )
    )

    // Render the chart
    CartesianChartHost(
        chart = chart,
        modelProducer = chartProducer,
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
    )
}

@Preview
@Composable
fun SpendingBarChartPreview() {
    SpendingBarChart(
        lastAmount = 150000.0,
        thisAmount = 300000.0,
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
    )
} 