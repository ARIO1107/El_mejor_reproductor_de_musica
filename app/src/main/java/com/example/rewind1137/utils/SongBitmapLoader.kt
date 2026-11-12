package com.example.rewind1137.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import java.util.concurrent.Callable

/**
 * Claves que viajan en los extras de cada MediaItem para que el MediaSession
 * pueda resolver la caratula exacta de la cancion (arte embebido incluido).
 */
object SongMediaExtras {
    const val SONG_ID = "rewind_song_id"
    const val SONG_URI = "rewind_song_uri"
    const val ALBUM_ART_URI = "rewind_album_art_uri"
}

/**
 * Cargador de caratulas del MediaSession. Usa exactamente la misma logica que
 * la UI (AlbumArtUtils.getSongArtwork), de modo que la notificacion del sistema,
 * el PlayerSheet y el MiniPlayer muestren siempre la misma portada.
 */
class SongBitmapLoader(
    private val context: Context,
    private val executor: ListeningExecutorService
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean =
        mimeType.startsWith("image/", ignoreCase = true)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit(Callable {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IllegalArgumentException("No se pudo decodificar la caratula")
        })

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit(Callable {
            AlbumArtUtils.loadArtworkFromUri(context, uri, MAX_ART_SIZE)
                ?: throw IllegalArgumentException("No se pudo cargar la caratula")
        })

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap> {
        val extras = metadata.extras
        val songId = extras?.getLong(SongMediaExtras.SONG_ID, 0L) ?: 0L
        val songUri = extras?.getString(SongMediaExtras.SONG_URI)?.let(Uri::parse)
        val albumArtUri = extras?.getString(SongMediaExtras.ALBUM_ART_URI)?.let(Uri::parse)

        return executor.submit(Callable {
            AlbumArtUtils.getSongArtworkBlocking(
                context = context,
                songId = songId,
                contentUri = songUri,
                albumArtUri = albumArtUri,
                maxSize = MAX_ART_SIZE
            ) ?: AlbumArtUtils.loadArtworkFromUri(context, metadata.artworkUri, MAX_ART_SIZE)
                ?: throw IllegalArgumentException("La cancion no tiene caratula")
        })
    }

    private companion object {
        const val MAX_ART_SIZE = 512
    }
}
