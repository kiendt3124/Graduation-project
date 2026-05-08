package com.uet.expensetracker.features.ai.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val a1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot1"
    )
    val a2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 150), RepeatMode.Reverse),
        label = "dot2"
    )
    val a3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 300), RepeatMode.Reverse),
        label = "dot3"
    )

    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row {
        Text("•", color = color.copy(alpha = a1), modifier = Modifier.size(12.dp))
        Text("•", color = color.copy(alpha = a2), modifier = Modifier.size(12.dp))
        Text("•", color = color.copy(alpha = a3), modifier = Modifier.size(12.dp))
    }
}


