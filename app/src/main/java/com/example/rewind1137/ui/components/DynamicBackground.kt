package com.example.rewind1137.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.imageLoader
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        val widthPx = constraints.maxWidth.coerceAtLeast(1)
        val heightPx = constraints.maxHeight.coerceAtLeast(1)
        // El fondo se rasteriza UNA vez por canción en vez de pintar dos
        // degradados radiales a pantalla completa en cada fotograma. Antes el
        // GPU tardaba ~12 ms por fotograma solo en el fondo, así que la
        // cuadrícula no podía mantener 60 fps al desplazarse.
        val background = remember(vibrantColor, dominantColor, baseColor, widthPx, heightPx) {
            renderBackgroundBitmap(
                vibrant = vibrantColor,
                dominant = dominantColor,
                base = baseColor,
                widthPx = widthPx,
                heightPx = heightPx
            )
        }

        Crossfade(
            targetState = background,
            animationSpec = tween(durationMillis = 450),
            label = "dynamicBackground"
        ) { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Pinta el degradado ambiental en un bitmap pequeño (1/4 de resolución) y lo
 * deja listo para dibujarse como textura. Los degradados son suaves, así que al
 * escalarlos no se nota la pérdida de resolución.
 */
private fun renderBackgroundBitmap(
    vibrant: Color,
    dominant: Color,
    base: Color,
    widthPx: Int,
    heightPx: Int
): ImageBitmap {
    val w = (widthPx / 4).coerceAtLeast(1)
    val h = (heightPx / 4).coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    val transparent = android.graphics.Color.TRANSPARENT
    val wf = w.toFloat()
    val hf = h.toFloat()

    canvas.drawColor(base.toArgb())

    paint.shader = RadialGradient(
        wf * 0.2f,
        hf * 0.2f,
        wf * 0.9f,
        intArrayOf(vibrant.copy(alpha = 0.30f).toArgb(), transparent),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, wf, hf, paint)

    paint.shader = RadialGradient(
        wf * 0.8f,
        hf * 0.4f,
        wf * 1.1f,
        intArrayOf(dominant.copy(alpha = 0.30f).toArgb(), transparent),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, wf, hf, paint)

    paint.shader = LinearGradient(
        0f,
        0f,
        0f,
        hf,
        intArrayOf(
            transparent,
            base.copy(alpha = 0.55f).toArgb(),
            base.toArgb()
        ),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, wf, hf, paint)

    return bitmap.asImageBitmap()
}
