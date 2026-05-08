@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EnhancedCircularProgressIndicator(
    progress: Float,
    totalAmount: String,
    spentAmount: String = "",
    size: Dp = 200.dp,
    indicatorThickness: Dp = 12.dp,
    backgroundColor: Color = Color(0xFF333333),
    foregroundColor: Color = Color(0xFF4CAF50),
    trackCap: StrokeCap = StrokeCap.Round,
    showAnimation: Boolean = true,
    animationDuration: Int = 1000
) {
    // Animate the progress value
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = if (showAnimation) animationDuration else 0,
            delayMillis = 0
        ),
        label = "progressAnimation"
    )
    
    // This is the angle that determines how much of the circle is filled
    val sweepAngle = 180f * animatedProgress
    
    // We start from the left (180 degrees)
    val startAngle = 180f
    
    // Indicator dot position calculation
    val dotAngleRadians = (startAngle + sweepAngle) * (PI / 180f)
    val indicatorRadius = (size.value / 2) - (indicatorThickness.value / 2)
    val indicatorX = cos(dotAngleRadians).toFloat() * indicatorRadius
    val indicatorY = sin(dotAngleRadians).toFloat() * indicatorRadius
    
    // Create gradient for progress
    val progressGradient = Brush.linearGradient(
        colors = listOf(
            foregroundColor.copy(alpha = 0.7f),
            foregroundColor
        )
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(16.dp).size(size)
    ) {
        // Draw the circular progress
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, CircleShape)
        ) {
            // Draw tick marks (small lines) along the track
//            drawTickMarks(backgroundColor.copy(alpha = 0.3f), startAngle, 180f, 20, indicatorRadius, indicatorThickness.toPx() * 0.7f)
            
            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = startAngle,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = indicatorThickness.toPx(), cap = trackCap)
            )
            
            // Foreground progress
            if (animatedProgress > 0f) {
                drawArc(
                    brush = progressGradient,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = indicatorThickness.toPx(), cap = trackCap)
                )
                
//                // Draw indicator dot at the end of the progress
//                drawCircle(
//                    color = foregroundColor,
//                    radius = (indicatorThickness.toPx() / 2) + 2f,
//                    center = Offset(
//                        x = (size.toPx() / 2) + indicatorX,
//                        y = (size.toPx() / 2) + indicatorY
//                    ),
//                    style = androidx.compose.ui.graphics.drawscope.Fill
//                )
                
                // Add a glow effect to the dot
//                for (i in 1..3) {
//                    drawCircle(
//                        color = foregroundColor.copy(alpha = 0.1f / i),
//                        radius = (indicatorThickness.toPx() / 2) + 2f + (i * 3),
//                        center = Offset(
//                            x = (size.toPx() / 2) + indicatorX,
//                            y = (size.toPx() / 2) + indicatorY
//                        ),
//                        style = androidx.compose.ui.graphics.drawscope.Fill
//                    )
//                }
            }
        }
        
        // Center text displaying amount
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Amount you can spend",
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = totalAmount,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = foregroundColor
            )
            
            // Show spent amount if provided
            if (spentAmount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Use annotated string to combine text with different styles
                Text(
                    text = buildAnnotatedString {
                        append("Spent: ")
                        withStyle(style = SpanStyle(
                            color = if (animatedProgress > 0.8f) Color.Red else foregroundColor,
                            fontWeight = FontWeight.Bold
                        )) {
                            append(spentAmount)
                        }
                    },
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                // Show percentage
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = if (animatedProgress > 0.8f) Color.Red else foregroundColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Indicator at the start of the progress (with shadow effect)
        Box(
            modifier = Modifier
                .size(16.dp)
                .offset(
                    x = (-size.value / 2 + indicatorThickness.value / 2 - indicatorThickness.value / 4).dp,
                    y = 0.dp
                )
                .shadow(2.dp, CircleShape)
                .background(foregroundColor, CircleShape)
        )
    }
}

// Extension function to draw tick marks on a circular track
private fun DrawScope.drawTickMarks(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    count: Int,
    radius: Float,
    length: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val angleStep = sweepAngle / count
    
    for (i in 0..count) {
        val angle = (startAngle + i * angleStep) * (PI / 180f)
        val start = Offset(
            x = center.x + (radius - length) * cos(angle).toFloat(),
            y = center.y + (radius - length) * sin(angle).toFloat()
        )
        val end = Offset(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat()
        )
        
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 1f
        )
    }
}

@Preview
@Composable
fun EnhancedCircularProgressIndicatorPreview() {
    EnhancedCircularProgressIndicator(
        progress = 0.65f,
        totalAmount = "$1,250",
        spentAmount = "$750",
        backgroundColor = Color(0xFFE0E0E0),
        foregroundColor = Color(0xFF4CAF50)
    )
}

@Preview
@Composable
fun EnhancedCircularProgressIndicatorEmptyPreview() {
    EnhancedCircularProgressIndicator(
        progress = 0f,
        totalAmount = "$2,000",
        backgroundColor = Color(0xFFE0E0E0),
        foregroundColor = Color(0xFF2196F3)
    )
}

@Preview
@Composable
fun EnhancedCircularProgressIndicatorFullPreview() {
    EnhancedCircularProgressIndicator(
        progress = 1f,
        totalAmount = "$2,000",
        spentAmount = "$3,000",
        backgroundColor = Color(0xFFE0E0E0),
        foregroundColor = Color(0xFFFF5722)
    )
}