@file:OptIn(ExperimentalMaterial3Api::class)

package com.uet.expensetracker.features.money.ui.budgets.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun StraightProgressIndicator(
    progress: Float,
    totalAmount: String,
    spentAmount: String = "",
    width: Dp = 200.dp,
    indicatorThickness: Dp = 8.dp,
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

    // Create gradient for progress
    val progressGradient = Brush.linearGradient(
        colors = listOf(
            foregroundColor.copy(alpha = 0.7f),
            foregroundColor
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .width(width)
    ) {
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(indicatorThickness)
                .clip(RoundedCornerShape(indicatorThickness / 2))
                .shadow(2.dp, RoundedCornerShape(indicatorThickness / 2))
                .background(backgroundColor)
        ) {
            // Foreground progress
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(indicatorThickness / 2))
                        .background(brush = progressGradient)
                )

                // Indicator dot at the end of the progress
                if (animatedProgress < 1f) {
                    Box(
                        modifier = Modifier
                            .size(indicatorThickness * 1.5f)
                            .align(Alignment.CenterStart)
                            .offset(x = (width - indicatorThickness) * animatedProgress - (indicatorThickness * 0.75f))
                            .shadow(2.dp, CircleShape)
                            .background(foregroundColor, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center text displaying amount
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Số tiền bạn có thể chi tiêu",
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
                        append("Đã tiêu: ")
                        withStyle(
                            style = SpanStyle(
                                color = if (animatedProgress > 0.8f) Color.Red else foregroundColor,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
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
    }
}

@Preview
@Composable
fun StraightProgressIndicatorPreview() {
    StraightProgressIndicator(
        progress = 0.65f,
        totalAmount = "$1,250",
        spentAmount = "$750",
        backgroundColor = Color(0xFFE0E0E0),
        foregroundColor = Color(0xFF4CAF50)
    )
}

@Preview
@Composable
fun StraightProgressIndicatorEmptyPreview() {
    StraightProgressIndicator(
        progress = 0f,
        totalAmount = "$2,000",
        backgroundColor = Color(0xFFE0E0E0),
        foregroundColor = Color(0xFF2196F3)
    )
}

@Preview
@Composable
fun StraightProgressIndicatorFullPreview() {
    StraightProgressIndicator(
        progress = 1f,
        totalAmount = "$2,000",
        spentAmount = "$3,000",
        backgroundColor = Color(0xFFE0E0E0),
        foregroundColor = Color(0xFFFF5722)
    )
}