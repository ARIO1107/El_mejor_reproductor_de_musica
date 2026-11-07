package com.example.rewind1137.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformSeekBar(
    progressMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    barHeightMax: Dp = 32.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progressFraction = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    // Sample height pattern for waveform
    val barHeights = remember(barCount) {
        List(barCount) { i ->
            val factor = (sin(i * 0.45f.toDouble()) * 0.4 + sin(i * 0.15f.toDouble()) * 0.3 + 0.5).toFloat()
            factor.coerceIn(0.2f, 1.0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightMax)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0 && size.width > 0) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
            }
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.4f)).coerceAtLeast(3f)
        val gap = ((totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)).coerceAtLeast(0f)

        barHeights.forEachIndexed { index, heightFactor ->
            val left = index * (barWidth + gap)
            val barHeight = canvasHeight * heightFactor
            val top = (canvasHeight - barHeight) / 2f

            val barFraction = (left + barWidth / 2f) / totalWidth
            val isPlayed = barFraction <= progressFraction

            drawRoundRect(
                color = if (isPlayed) activeColor else inactiveColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
