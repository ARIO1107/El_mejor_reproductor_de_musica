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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object AlbumArtUtils {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Cache amplia: el desplazamiento fluido importa mas que el consumo de RAM.
    private val cacheSize = (maxMemory / 4).coerceIn(16 * 1024, 192 * 1024)

    /** Tamano unico para todas las miniaturas (lista y cuadricula). */
    const val THUMBNAIL_ART_SIZE = 288

    private val memoryCache = object : LruCache<Long, Bitmap>(cacheSize) {
        override fun sizeOf(key: Long, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val noEmbeddedArtSet = Collections.synchronizedSet(HashSet<Long>())

    /** Peticiones de portada en curso, para no decodificar dos veces la misma. */
    private val inFlight = ConcurrentHashMap<Long, CompletableDeferred<Bitmap?>>()

    private fun cacheKey(songId: Long, maxSize: Int): Long = songId * 4096L + maxSize.toLong()

    fun clearCache() {
        memoryCache.evictAll()
        noEmbeddedArtSet.clear()
        inFlight.clear()
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

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            // Las miniaturas no necesitan canal alfa: RGB_565 usa la mitad de
            // memoria y decodifica mas rapido, lo que mantiene fluida la
            // cuadricula y permite que quepan muchas mas portadas en cache.
            if (maxSize <= THUMBNAIL_ART_SIZE) {
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = false
            }
        }
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
        if (songId <= 0L) return null
        // La memoria caché también guarda la portada de álbum cuando la canción
        // no tiene arte embebido, así que no hay que descartarla por estar en
        // noEmbeddedArtSet.
        return memoryCache.get(cacheKey(songId, maxSize))
    }

    fun getCachedThumbnail(songId: Long): Bitmap? =
        getCachedArt(songId, THUMBNAIL_ART_SIZE)

    /**
     * MediaStore agrupa las canciones sin album real bajo la misma URI
     * (`albumart/0`), por lo que esa imagen se repetia en muchas canciones.
     * Se descarta para que solo se use el arte embebido de cada archivo.
     */
    private fun sanitizeAlbumArtUri(albumArtUri: Uri?): Uri? {
        if (albumArtUri == null) return null
        val albumId = albumArtUri.lastPathSegment
        if (albumId.isNullOrBlank() || albumId == "0" || albumId == "-1") return null
        return albumArtUri
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
            // Si otra peticion ya esta decodificando esta misma portada, se
            // espera su resultado en vez de repetir el trabajo.
            inFlight[key]?.let { return it.await() }
        }

        val pending = if (key != null) CompletableDeferred<Bitmap?>() else null
        if (key != null && pending != null) {
            val existing = inFlight.putIfAbsent(key, pending)
            if (existing != null) return existing.await()
        }

        try {
            val embedded = if (songId > 0L) {
                getEmbeddedArt(context, songId, contentUri, maxSize)
            } else {
                null
            }

            val result = embedded ?: withContext(Dispatchers.IO) {
                loadArtworkFromUri(context, sanitizeAlbumArtUri(albumArtUri), maxSize)
            }

            if (result != null && key != null) {
                memoryCache.put(key, result)
            }
            pending?.complete(result)
            return result
        } catch (t: Throwable) {
            pending?.complete(null)
            throw t
        } finally {
            if (key != null && pending != null) {
                inFlight.remove(key, pending)
            }
        }
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

    suspend fun getThumbnailArtwork(context: Context, song: Song): Bitmap? =
        getSongArtwork(
            context = context,
            songId = song.id,
            contentUri = song.uri,
            albumArtUri = song.albumArtUri,
            maxSize = THUMBNAIL_ART_SIZE
        )
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
