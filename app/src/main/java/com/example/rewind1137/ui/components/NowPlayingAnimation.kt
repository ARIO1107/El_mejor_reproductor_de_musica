package com.example.rewind1137.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.viewmodel.NowPlayingAnimation
import kotlin.math.abs
import kotlin.math.sin

private const val TWO_PI = kotlin.math.PI.toFloat() * 2f

@Composable
fun NowPlayingIndicator(
    animation: NowPlayingAnimation,
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    if (!isPlaying) {
        StaticCurrentIcon(color = color, modifier = modifier.size(size))
        return
    }
    when (animation) {
        NowPlayingAnimation.BARS -> BarsIndicator(color, modifier.size(size))
        NowPlayingAnimation.PULSE -> PulseIndicator(color, modifier.size(size))
        NowPlayingAnimation.SLASH -> SlashIndicator(color, modifier.size(size))
        NowPlayingAnimation.NOTE_FLY -> NoteFlyIndicator(color, modifier.size(size))
        NowPlayingAnimation.WAVE -> WaveIndicator(color, modifier.size(size))
        NowPlayingAnimation.SPIN_DOT -> SpinDotIndicator(color, modifier.size(size))
        NowPlayingAnimation.GLOW -> GlowIndicator(color, modifier.size(size))
        NowPlayingAnimation.JUMP_DOT -> JumpDotIndicator(color, modifier.size(size))
        NowPlayingAnimation.BEAT -> BeatIndicator(color, modifier.size(size))
        NowPlayingAnimation.RING -> RingIndicator(color, modifier.size(size))
    }
}

@Composable
private fun StaticCurrentIcon(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.alpha(0.8f),
        contentAlignment = Alignment.Center
    ) {
        BarsCanvas(
            phase = 0.5f,
            color = color,
            width = 14f,
            height = 14f,
            barCount = 3,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BarsIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "bars_phase"
    )
    BarsCanvas(
        phase = phase,
        color = color,
        width = 18f,
        height = 18f,
        barCount = 5,
        modifier = modifier
    )
}

@Composable
private fun BarsCanvas(
    phase: Float,
    color: Color,
    width: Float,
    height: Float,
    barCount: Int,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barWidth = this.size.width / (barCount * 1.8f)
        val spacing = (this.size.width - barWidth * barCount) / (barCount + 1)
        repeat(barCount) { i ->
            val envelope = 0.35f + 0.65f * abs(sin(TWO_PI * phase + i * 1.1f))
            val barHeight = this.size.height * envelope
            val x = spacing + i * (barWidth + spacing)
            val top = (this.size.height - barHeight) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun PulseIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse_t"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = this.size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = (0.5f * (1f - t)).coerceAtLeast(0f)),
            radius = maxRadius * (0.3f + 0.7f * t),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = color.copy(alpha = (0.35f * abs(sin(TWO_PI * t))).coerceAtLeast(0.05f)),
            radius = maxRadius * (0.5f + 0.35f * t),
            center = center,
            style = Stroke(width = 1.5f)
        )
        drawCircle(color = color, radius = maxRadius * 0.18f, center = center)
    }
}

@Composable
private fun SlashIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "slash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "slash_t"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val h = this.size.height
        val w = this.size.width
        drawRoundRect(
            color = color.copy(alpha = 0.25f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.28f),
            topLeft = Offset(0f, (h - h * 0.28f) / 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        val travelX = w - w * 0.45f
        val x = t * travelX + w * 0.08f
        drawRoundRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(w * 0.45f, h * 0.22f),
            topLeft = Offset(x, (h - h * 0.22f) / 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}

@Composable
private fun NoteFlyIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "note")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "note_t"
    )
    val y1 = (sin(TWO_PI * t) * 4f).dp
    val y2 = (sin(TWO_PI * t + 2.1f) * 4f).dp
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "\u266B",
            fontSize = 15.sp,
            color = color.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = y2.toPx() }
        )
        Text(
            text = "\u266A",
            fontSize = 18.sp,
            color = color,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer { translationY = y1.toPx() }
        )
    }
}

@Composable
private fun WaveIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_t"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height
        val mid = h / 2f
        listOf(1f to 0.18f, 1.8f to 0.12f).forEachIndexed { index, (freqMul, alpha) ->
            val path = Path()
            val amplitude = h * 0.22f
            val phaseOffset = t * TWO_PI
            val steps = 22
            for (i in 0..steps) {
                val x = (i.toFloat() / steps.toFloat()) * w
                val y = mid + sin((x / w) * freqMul * TWO_PI + phaseOffset) * amplitude
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha * (1f + index * 0.2f)),
                style = Stroke(width = if (index == 0) 2.2f else 1.4f)
            )
        }
    }
}

@Composable
private fun SpinDotIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "spin")
    val rot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin_rot"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val arcSize = androidx.compose.ui.geometry.Size(
            this.size.minDimension * 0.78f,
            this.size.minDimension * 0.78f
        )
        val topLeft = Offset(
            (this.size.width - arcSize.width) / 2f,
            (this.size.height - arcSize.height) / 2f
        )
        drawArc(
            color = color,
            startAngle = rot,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 3f)
        )
    }
}

@Composable
private fun GlowIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_t"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val base = this.size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = 0.10f + 0.32f * t),
            radius = base * (0.6f + 0.35f * t),
            center = center
        )
        drawCircle(
            color = color,
            radius = base * (0.22f + 0.06f * t),
            center = center
        )
    }
}

@Composable
private fun JumpDotIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jump")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "jump_t"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height
        val dotRadius = 3.2f
        val y = h - dotRadius - abs(sin(TWO_PI * t)) * (h * 0.62f)
        drawCircle(color = color.copy(alpha = 0.18f), radius = dotRadius, center = Offset(w / 2f, h - dotRadius - 1f))
        drawCircle(color = color, radius = dotRadius, center = Offset(w / 2f, y))
    }
}

@Composable
private fun BeatIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "beat")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "beat_t"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height
        val mid = h / 2f
        val path = Path()
        path.moveTo(0f, mid)
        val spikeX = w * (0.15f + 0.7f * t)
        path.lineTo(w * 0.25f, mid)
        path.lineTo(spikeX * 0.85f, mid - h * 0.38f * abs(sin(TWO_PI * t * 1f)))
        path.lineTo(spikeX, mid)
        path.lineTo(w * 0.75f, mid)
        path.lineTo(w, mid)
        drawPath(path = path, color = color, style = Stroke(width = 2.4f))
        path.reset()
        path.moveTo(w * 0.3f, mid - h * 0.24f)
        path.lineTo(w * 0.35f, mid - h * 0.36f)
        path.lineTo(w * 0.4f, mid - h * 0.2f)
        path.lineTo(w * 0.46f, mid - h * 0.34f)
        path.lineTo(w * 0.52f, mid - h * 0.2f)
        drawPath(path = path, color = color.copy(alpha = 0.6f), style = Stroke(width = 2f))
    }
}

@Composable
private fun RingIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ring")
    val rot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring_rot"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val r = size * 0.5f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = r,
            center = center,
            style = Stroke(width = 2f)
        )
        val angle = (rot - 90f) * kotlin.math.PI.toFloat() / 180f
        val dotCenter = Offset(
            center.x + r * kotlin.math.cos(angle),
            center.y + r * kotlin.math.sin(angle)
        )
        drawCircle(color = color, radius = 4f, center = dotCenter)
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = r,
            center = center,
            style = Stroke(width = 3.5f)
        )
    }
}

@Composable
fun NowPlayingAnimationPicker(
    current: NowPlayingAnimation,
    onSelect: (NowPlayingAnimation) -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.width(2.dp)) }
        items(NowPlayingAnimation.entries.size) { index ->
            val mode = NowPlayingAnimation.entries[index]
            val selected = mode == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    NowPlayingIndicator(
                        animation = mode,
                        color = accent,
                        isPlaying = true,
                        size = 22.dp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (selected) "\u2713 ${mode.title}" else mode.title,
                    fontSize = 9.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
        item { Spacer(Modifier.width(2.dp)) }
    }
}