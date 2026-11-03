package com.example.re_pro.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

@Composable
fun AudioVisualizer(
    audioSessionId: Int,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF4CAF50),
    barCount: Int = 32,
    isActive: Boolean = true
) {
    var fftData by remember { mutableStateOf(FloatArray(barCount)) }

    val animatedHeights = remember(barCount) {
        List(barCount) { Animatable(0f) }
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(audioSessionId, isActive) {
        if (!isActive || audioSessionId <= 0) return@LaunchedEffect

        val visualizer = try {
            Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
            }
        } catch (_: Exception) {
            return@LaunchedEffect
        }

        val listener = object : Visualizer.OnDataCaptureListener {
            override fun onWaveFormDataCapture(vis: Visualizer, waveform: ByteArray, samplingRate: Int) {}

            override fun onFftDataCapture(vis: Visualizer, fft: ByteArray, samplingRate: Int) {
                val spectrum = FloatArray(barCount)
                val step = fft.size / 2 / barCount

                for (i in 0 until barCount) {
                    var magnitude = 0f
                    val start = i * step + 2
                    val end = minOf(start + step, fft.size / 2)

                    for (j in start until end step 2) {
                        if (j + 1 < fft.size) {
                            val real = fft[j].toFloat()
                            val imag = fft[j + 1].toFloat()
                            magnitude += sqrt(real * real + imag * imag)
                        }
                    }
                    magnitude /= (end - start).coerceAtLeast(1)

                    val db = if (magnitude > 0) {
                        (20 * log10(magnitude / 128f)).coerceIn(-60f, 0f)
                    } else -60f
                    spectrum[i] = ((db + 60f) / 60f).coerceIn(0f, 1f)
                }
                fftData = spectrum
            }
        }

        try {
            visualizer.setDataCaptureListener(listener, Visualizer.getMaxCaptureRate() / 2, true, true)
            visualizer.enabled = true
        } catch (_: Exception) {
            visualizer.release()
            return@LaunchedEffect
        }

        try {
            awaitCancellation()
        } finally {
            try { visualizer.release() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(fftData) {
        fftData.forEachIndexed { index, target ->
            launch {
                animatedHeights[index].animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 80,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 1.5f)
        val gap = barWidth * 0.5f
        val totalWidth = barCount * barWidth + (barCount - 1) * gap
        val startX = (size.width - totalWidth) / 2f

        for (i in 0 until barCount) {
            val height = animatedHeights[i].value * size.height * 0.85f
            val x = startX + i * (barWidth + gap)
            val y = size.height - height

            drawRoundRect(
                color = barColor.copy(alpha = 0.7f + 0.3f * animatedHeights[i].value),
                topLeft = Offset(x, y),
                size = Size(barWidth, height.coerceAtLeast(2.dp.toPx())),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
