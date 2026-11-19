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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    /**
     * Canciones sin carátula conocida. Evita reintentar la decodificación en
     * cada refresco de la notificación (antes se lanzaba una lectura de metadatos
     * por segundo y por canción sin arte).
     */
    private val artRetryAfter = ConcurrentHashMap<Long, Long>()
    private const val ART_RETRY_COOLDOWN_MS = 20_000L

    /** Limita cuántas lecturas de metadatos/decodificaciones corren a la vez. */
    private val decodeSemaphore = Semaphore(3)

    fun isArtKnownMissing(songId: Long): Boolean {
        if (songId <= 0L) return false
        val until = artRetryAfter[songId] ?: return false
        if (System.currentTimeMillis() >= until) {
            artRetryAfter.remove(songId)
            return false
        }
        return true
    }

    fun markArtMissing(songId: Long) {
        if (songId > 0L) artRetryAfter[songId] = System.currentTimeMillis() + ART_RETRY_COOLDOWN_MS
    }

    private fun cacheKey(songId: Long, maxSize: Int): Long = songId * 4096L + maxSize.toLong()

    fun clearCache() {
        memoryCache.evictAll()
        noEmbeddedArtSet.clear()
        inFlight.clear()
        artRetryAfter.clear()
    }

    // --- Caché en disco de miniaturas -------------------------------------
    // Leer el arte embebido con MediaMetadataRetriever cuesta 30-100 ms por
    // canción. Guardarlo ya decodificado hace que, a partir de la segunda
    // apertura, la cuadrícula se desplace sin decodificar audio en segundo
    // plano (que era lo que robaba CPU al hilo de interfaz y causaba tirones).

    private const val DISK_CACHE_DIR = "song_art"
    private const val DISK_CACHE_QUALITY = 88
    private const val DISK_CACHE_MAX_SIZE = 1024

    private fun diskCacheFile(context: Context, songId: Long, maxSize: Int): File =
        File(File(context.cacheDir, DISK_CACHE_DIR), "${songId}_$maxSize.jpg")

    private fun readDiskArt(context: Context, songId: Long, maxSize: Int): Bitmap? {
        val file = diskCacheFile(context, songId, maxSize)
        if (!file.isFile || file.length() == 0L) return null
        return try {
            val options = BitmapFactory.Options().apply {
                if (maxSize <= THUMBNAIL_ART_SIZE) {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun writeDiskArt(context: Context, songId: Long, maxSize: Int, bitmap: Bitmap) {
        if (maxSize > DISK_CACHE_MAX_SIZE) return
        try {
            val file = diskCacheFile(context, songId, maxSize)
            val dir = file.parentFile ?: return
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, DISK_CACHE_QUALITY, out)
            }
        } catch (_: Throwable) {
            // Sin caché en disco se sigue funcionando igual, solo más lento.
        }
    }

    /** Borra el arte memorizado y en disco de una canción (p. ej. al editar tags). */
    fun evictSong(context: Context, songId: Long) {
        if (songId <= 0L) return
        val snapshot = memoryCache.snapshot()
        for (key in snapshot.keys) {
            if (key / 4096L == songId) memoryCache.remove(key)
        }
        noEmbeddedArtSet.remove(songId)
        artRetryAfter.remove(songId)
        try {
            File(context.cacheDir, DISK_CACHE_DIR)
                .listFiles { file -> file.name.startsWith("${songId}_") }
                ?.forEach { it.delete() }
        } catch (_: Throwable) {
        }
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
            readDiskArt(context, songId, maxSize)?.let { cached ->
                memoryCache.put(key, cached)
                return@withContext cached
            }

            // Nunca más de tres lecturas de metadatos a la vez: si la cuadrícula
            // pide 40 celdas de golpe, las demás esperan en vez de saturar la CPU
            // y provocar tirones en el hilo de interfaz.
            decodeSemaphore.withPermit {
                try {
                    context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(pfd.fileDescriptor)
                        val art = retriever.embeddedPicture
                        retriever.release()

                        if (art != null && art.isNotEmpty()) {
                            val bitmap = decodeDownsampled(art, maxSize)
                            if (bitmap != null) {
                                writeDiskArt(context, songId, maxSize, bitmap)
                                memoryCache.put(key, bitmap)
                                return@withPermit bitmap
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AlbumArt", "embedded fd fail song=$songId: ${e.message}")
                }

                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, contentUri)
                    val art = retriever.embeddedPicture
                    retriever.release()

                    if (art != null && art.isNotEmpty()) {
                        val bitmap = decodeDownsampled(art, maxSize)
                        if (bitmap != null) {
                            writeDiskArt(context, songId, maxSize, bitmap)
                            memoryCache.put(key, bitmap)
                            return@withPermit bitmap
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AlbumArt", "embedded uri fail song=$songId: ${e.message}")
                }

                noEmbeddedArtSet.add(songId)
                null
            }
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
    internal fun sanitizeAlbumArtUri(albumArtUri: Uri?): Uri? {
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
        maxSize: Int = 1024,
        /**
         * La caratula de album se comparte entre todas las canciones del mismo
         * `album_id`. En la notificacion y en el MediaSession se desactiva para
         * no mostrar nunca la portada de otra cancion.
         */
        allowAlbumFallback: Boolean = true
    ): Bitmap? {
        val key = if (songId > 0L) cacheKey(songId, maxSize) else null
        if (key != null) {
            memoryCache.get(key)?.let { return it }
            // Si otra peticion ya esta decodificando esta misma portada, se
            // espera su resultado en vez de repetir el trabajo.
            inFlight[key]?.let { return it.await() }
        }
        if (isArtKnownMissing(songId)) return null

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

            val result = embedded ?: if (allowAlbumFallback) {
                withContext(Dispatchers.IO) {
                    loadArtworkFromUri(context, sanitizeAlbumArtUri(albumArtUri), maxSize)
                }
            } else {
                null
            }

            if (result != null && key != null) {
                memoryCache.put(key, result)
            } else if (result == null) {
                markArtMissing(songId)
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
        maxSize: Int = 1024,
        allowAlbumFallback: Boolean = true
    ): Bitmap? = runBlocking {
        getSongArtwork(context, songId, contentUri, albumArtUri, maxSize, allowAlbumFallback)
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
