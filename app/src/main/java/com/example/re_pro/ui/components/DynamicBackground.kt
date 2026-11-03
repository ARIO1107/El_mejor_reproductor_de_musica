package com.example.re_pro.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DynamicBackground(
    albumArtUri: Uri?,
    modifier: Modifier = Modifier,
    defaultColor: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val baseColor = if (defaultColor == Color.Unspecified) MaterialTheme.colorScheme.background else defaultColor
    var dominantColor by remember { mutableStateOf(baseColor) }
    var vibrantColor by remember { mutableStateOf(baseColor) }

    LaunchedEffect(albumArtUri, baseColor) {
        if (albumArtUri != null) {
            val extracted = withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .size(160, 160)
                        .precision(Precision.INEXACT)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val bitmap = (result as? SuccessResult)?.drawable as? android.graphics.drawable.BitmapDrawable
                    bitmap?.let {
                        withContext(Dispatchers.Default) {
                            val palette = Palette.from(it.bitmap).generate()
                            val dom = palette.getDominantColor(baseColor.toArgb())
                            val vib = palette.getVibrantColor(palette.getMutedColor(baseColor.toArgb()))
                            dom to vib
                        }
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (extracted != null) {
                dominantColor = Color(extracted.first)
                vibrantColor = Color(extracted.second)
            } else {
                dominantColor = baseColor
                vibrantColor = baseColor
            }
        } else {
            dominantColor = baseColor
            vibrantColor = baseColor
        }
    }

    val animatedDom by animateColorAsState(targetValue = dominantColor, animationSpec = tween(1000), label = "dom")
    val animatedVib by animateColorAsState(targetValue = vibrantColor, animationSpec = tween(1000), label = "vib")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 70.dp)
        ) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(animatedVib.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = size.width * 0.9f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(animatedDom.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.4f),
                    radius = size.width * 1.1f
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            baseColor.copy(alpha = 0.55f),
                            baseColor
                        )
                    )
                )
        )
    }
}

private fun Color.toArgb(): Int {
    return (this.value shr 32).toInt()
}
