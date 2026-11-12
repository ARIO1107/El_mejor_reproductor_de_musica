package com.example.rewind1137.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.LruCache
import com.example.rewind1137.utils.AlbumArtUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Restaura el ultimo estado guardado y resuelve la caratula exacta de la
 * cancion para que el widget nunca aparezca vacio despues de reiniciar.
 */
internal suspend fun ensureWidgetArtwork(context: Context) {
    RewindWidgetState.restore(context)
    if (!RewindWidgetState.hasSong || RewindWidgetState.albumArt != null) return

    val art = withContext(Dispatchers.IO) {
        runCatching {
            AlbumArtUtils.getSongArtworkBlocking(
                context = context,
                songId = RewindWidgetState.currentSongId,
                contentUri = RewindWidgetState.songUri,
                albumArtUri = RewindWidgetState.albumArtUri,
                maxSize = 256
            )
        }.getOrNull()
    }
    if (art != null) {
        RewindWidgetState.albumArt = roundWidgetArtwork(art)
    }
}

/**
 * Glance no recorta las imagenes hijas con [androidx.glance.appwidget.cornerRadius],
 * asi que redondeamos el bitmap antes de mostrarlo.
 */
internal fun roundWidgetArtwork(src: Bitmap, radiusRatio: Float = 0.18f): Bitmap {
    val key = System.identityHashCode(src)
    roundedCache.get(key)?.let { return it }

    val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = minOf(src.width, src.height) * radiusRatio
    canvas.drawRoundRect(
        RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()),
        radius,
        radius,
        paint
    )
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(src, 0f, 0f, paint)
    roundedCache.put(key, output)
    return output
}

private val roundedCache = object : LruCache<Int, Bitmap>(4) {}
