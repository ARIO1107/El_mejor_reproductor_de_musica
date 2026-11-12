package com.example.rewind1137.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.rewind1137.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.math.max

object AlbumArtUtils {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)

    private val memoryCache = object : LruCache<Long, Bitmap>(cacheSize) {
        override fun sizeOf(key: Long, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val noEmbeddedArtSet = Collections.synchronizedSet(HashSet<Long>())

    private fun cacheKey(songId: Long, maxSize: Int): Long = songId * 4096L + maxSize.toLong()

    fun clearCache() {
        memoryCache.evictAll()
        noEmbeddedArtSet.clear()
    }

    private fun decodeDownsampled(art: ByteArray, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(art, 0, art.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val largerSide = max(bounds.outWidth, bounds.outHeight)
        while (largerSide / sample > maxSize && sample < 64) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(art, 0, art.size, opts)
    }

    fun loadArtworkFromUri(context: Context, uri: Uri?, maxSize: Int = 1024): Bitmap? {
        if (uri == null) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isEmpty()) null else decodeDownsampled(bytes, maxSize)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getEmbeddedArt(
        context: Context,
        songId: Long,
        contentUri: Uri?,
        maxSize: Int = 1024
    ): Bitmap? {
        if (songId <= 0L || contentUri == null || contentUri.scheme == "android.resource") return null

        if (noEmbeddedArtSet.contains(songId)) {
            return null
        }

        val key = cacheKey(songId, maxSize)
        memoryCache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(pfd.fileDescriptor)
                    val art = retriever.embeddedPicture
                    retriever.release()

                    if (art != null && art.isNotEmpty()) {
                        val bitmap = decodeDownsampled(art, maxSize)
                        if (bitmap != null) {
                            memoryCache.put(key, bitmap)
                            return@withContext bitmap
                        }
                    }
                }
            } catch (_: Exception) {
            }

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, contentUri)
                val art = retriever.embeddedPicture
                retriever.release()

                if (art != null && art.isNotEmpty()) {
                    val bitmap = decodeDownsampled(art, maxSize)
                    if (bitmap != null) {
                        memoryCache.put(key, bitmap)
                        return@withContext bitmap
                    }
                }
            } catch (_: Exception) {
            }

            noEmbeddedArtSet.add(songId)
            null
        }
    }

    fun getCachedArt(songId: Long, maxSize: Int = 1024): Bitmap? {
        if (songId <= 0L || noEmbeddedArtSet.contains(songId)) return null
        return memoryCache.get(cacheKey(songId, maxSize))
    }

    /**
     * Resuelve la carátula exacta de una canción usando el mismo camino para la UI
     * (PlayerSheet/MiniPlayer) y para la notificación: primero arte embebido y,
     * si no existe, la carátula del álbum.
     */
    suspend fun getSongArtwork(
        context: Context,
        songId: Long,
        contentUri: Uri?,
        albumArtUri: Uri?,
        maxSize: Int = 1024
    ): Bitmap? {
        val key = if (songId > 0L) cacheKey(songId, maxSize) else null
        if (key != null) {
            memoryCache.get(key)?.let { return it }
        }

        val embedded = if (songId > 0L) {
            getEmbeddedArt(context, songId, contentUri, maxSize)
        } else {
            null
        }
        if (embedded != null) return embedded

        val albumArt = withContext(Dispatchers.IO) {
            loadArtworkFromUri(context, albumArtUri, maxSize)
        }
        if (albumArt != null && key != null) {
            memoryCache.put(key, albumArt)
        }
        return albumArt
    }

    fun getSongArtworkBlocking(
        context: Context,
        songId: Long,
        contentUri: Uri?,
        albumArtUri: Uri?,
        maxSize: Int = 1024
    ): Bitmap? = runBlocking {
        getSongArtwork(context, songId, contentUri, albumArtUri, maxSize)
    }
}

@Composable
fun rememberSongArtwork(song: Song?, maxArtSize: Int = 1024): Any? {
    return rememberSongArtwork(
        songId = song?.id ?: 0L,
        contentUri = song?.uri,
        albumArtUri = song?.albumArtUri,
        maxArtSize = maxArtSize
    )
}

@Composable
fun rememberSongArtwork(
    songId: Long,
    contentUri: Uri?,
    albumArtUri: Uri?,
    maxArtSize: Int = 1024
): Any? {
    val context = LocalContext.current
    var artModel by remember(songId, maxArtSize) {
        mutableStateOf<Any?>(AlbumArtUtils.getCachedArt(songId, maxArtSize))
    }

    LaunchedEffect(songId, contentUri, albumArtUri, maxArtSize) {
        artModel = AlbumArtUtils.getSongArtwork(
            context = context,
            songId = songId,
            contentUri = contentUri,
            albumArtUri = albumArtUri,
            maxSize = maxArtSize
        )
    }

    return artModel
}
