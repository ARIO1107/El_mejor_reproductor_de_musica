package com.example.rewind1137.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import androidx.core.graphics.ColorUtils

/**
 * Genera el fondo "ambient light" de la notificación a partir de la carátula:
 * una versión muy difuminada de la portada, un halo del color dominante y un
 * degradado oscuro para que el texto siempre se lea bien.
 */
object NotificationGlowUtils {

    data class Glow(
        val background: Bitmap?,
        val accent: Int
    )

    private const val FALLBACK_ACCENT = 0xFFB388FE.toInt()
    private const val FALLBACK_SECONDARY = 0xFF7C4DFF.toInt()

    // La notificación se refresca cada segundo: reutilizamos el último fondo
    // generado para no crear bitmaps nuevos en cada actualización.
    private val glowCache = object : LruCache<Int, Glow>(3) {}

    fun build(art: Bitmap?, width: Int = 480, height: Int = 480): Glow {
        if (art == null) return Glow(null, FALLBACK_ACCENT)

        val key = ((System.identityHashCode(art) * 31) + width) * 31 + height
        glowCache.get(key)?.let { return it }

        val accent = extractAccent(art)
        val blurred = blurArtwork(art, 28)
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val scale = maxOf(width / blurred.width.toFloat(), height / blurred.height.toFloat())
        val drawWidth = blurred.width * scale
        val drawHeight = blurred.height * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f
        val artPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(
            blurred,
            null,
            RectF(left, top, left + drawWidth, top + drawHeight),
            artPaint
        )

        // Halo del color dominante arriba a la izquierda.
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.24f,
                height * 0.16f,
                width * 0.82f,
                intArrayOf(
                    ColorUtils.setAlphaComponent(accent, 120),
                    ColorUtils.setAlphaComponent(FALLBACK_SECONDARY, 40),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), haloPaint)

        // Scrim vertical: deja ver el brillo arriba y oscurece hacia abajo.
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                intArrayOf(0x7A0A0810, 0xE60A0810.toInt(), 0xFF0A0810.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        val result = Glow(out, accent)
        glowCache.put(key, result)
        return result
    }

    fun extractAccent(art: Bitmap?): Int {
        if (art == null) return FALLBACK_ACCENT

        val small = Bitmap.createScaledBitmap(art, 16, 16, true)
        var bestColor = FALLBACK_ACCENT
        var bestScore = -1.0
        val hsv = FloatArray(3)

        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val color = small.getPixel(x, y)
                if (Color.alpha(color) < 200) continue
                Color.colorToHSV(color, hsv)
                if (hsv[2] < 0.14f) continue
                val score = hsv[1] * 0.72 + hsv[2] * 0.28
                if (score > bestScore) {
                    bestScore = score
                    bestColor = color
                }
            }
        }

        val accentHsv = FloatArray(3)
        Color.colorToHSV(bestColor, accentHsv)
        accentHsv[1] = accentHsv[1].coerceIn(0.38f, 0.88f)
        accentHsv[2] = accentHsv[2].coerceIn(0.58f, 0.96f)
        return Color.HSVToColor(accentHsv)
    }

    private fun blurArtwork(art: Bitmap, size: Int): Bitmap {
        val square = centerCropSquare(art)
        val tiny = Bitmap.createScaledBitmap(square, size, size, true)
        val softer = Bitmap.createScaledBitmap(tiny, size / 2, size / 2, true)
        return Bitmap.createScaledBitmap(softer, size, size, true)
    }

    private fun centerCropSquare(art: Bitmap): Bitmap {
        val side = minOf(art.width, art.height)
        val x = (art.width - side) / 2
        val y = (art.height - side) / 2
        return Bitmap.createBitmap(art, x, y, side, side)
    }
}
