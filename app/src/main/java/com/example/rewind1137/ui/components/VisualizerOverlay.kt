package com.example.rewind1137.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerOverlay(
    isPlaying: Boolean,
    audioSessionId: Int,
    modifier: Modifier = Modifier,
    height: Float = 200f,
    barColor: Color = Color(0xFFFBB8AC)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_overlay")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) Float.MAX_VALUE else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vibr_phase"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.8f else 0f,
        targetValue = if (isPlaying) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Restart
        ),
        label = "vibr_alpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
        val width = size.width
        val heightPx = size.height
        val barCount = 64
        val barWidth = width / barCount
        val centerY = heightPx / 2f

        for (i in 0 until barCount) {
            val t = phase + i * 0.15f
            val x = i * barWidth

            val wave1 = (sin(t) * 0.5f + 0.5f) * heightPx * 0.4f
            val wave2 = (sin(t * 2 + phase * 0.5f) * 0.3f + 0.5f) * heightPx * 0.3f
            val combined = (wave1 + wave2) * alpha

            val barHeight = combined.coerceIn(2f, heightPx * 0.45f)
            val offsetY = centerY - barHeight / 2f

            val gradient = Brush.linearGradient(
                colors = listOf(
                    barColor.copy(alpha = alpha * 0.3f),
                    barColor.copy(alpha = alpha),
                    barColor.copy(alpha = alpha * 0.3f)
                ),
                start = Offset(x, offsetY),
                end = Offset(x, offsetY + barHeight)
            )

            drawRect(
                brush = gradient,
                topLeft = Offset(x, offsetY),
                size = Size(barWidth - 1f, barHeight)
            )
        }
    }
}

@Composable
fun CircularVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 200,
    colors: List<Color> = listOf(
        Color(0xFFFBB8AC),
        Color(0xFF38BDF8),
        Color(0xFF34D399),
        Color(0xFFF59E0B)
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_viz")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) Float.MAX_VALUE else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circ_viz_phase"
    )

    val ringCount = colors.size
    val ringSpacing = 8f

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        for (ringIndex in colors.indices) {
            val ringPhase = phase + ringIndex * 0.5f
            val ringRadius = (size / 2f) - ringIndex * ringSpacing
            val pointCount = 36

            Canvas(
                modifier = Modifier
                    .size((ringRadius * 2f).dp)
                    .align(Alignment.Center)
            ) {
                val w = this.size.width
                val h = this.size.height
                val cx = w / 2f
                val cy = h / 2f

                for (i in 0 until pointCount) {
                    val angle = ringPhase + (i * 2 * PI.toFloat() / pointCount)
                    val radius = (ringRadius - 4f) + (sin(phase + i * 0.2f).toFloat() * 3f)
                    val x = cx + cos(angle).toFloat() * radius
                    val y = cy + sin(angle).toFloat() * radius

                    drawCircle(
                        color = colors[ringIndex].copy(alpha = 0.6f),
                        radius = 2f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Center glow
        val glowPulse by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = if (isPlaying) 0.7f else 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "glow_pulse"
        )

        Box(
            modifier = Modifier
                .size((size * 0.3f).dp)
                .clip(CircleShape)
                .background(colors[0].copy(alpha = glowPulse * 0.3f))
        )
    }
}