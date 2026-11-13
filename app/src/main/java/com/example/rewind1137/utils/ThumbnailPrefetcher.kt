package com.example.rewind1137.utils

import android.content.Context
import com.example.rewind1137.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Precarga las miniaturas en segundo plano para que la lista y la cuadricula
 * (incluso a 5 columnas) se desplacen sin cargas visibles ni saltos.
 *
 * La ventana visible tiene prioridad sobre la precarga de fondo.
 */
object ThumbnailPrefetcher {

    private const val WORKERS = 4

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val highPriority = Channel<Long>(Channel.UNLIMITED)
    private val background = Channel<Long>(Channel.UNLIMITED)
    private val queued = Collections.synchronizedSet(HashSet<Long>())
    private val songs = ConcurrentHashMap<Long, Song>()
    private val started = AtomicBoolean(false)

    /**
     * Encola [songs] para precargar. Con [highPriority] se adelanta a la cola
     * de fondo (se usa para lo que el usuario esta viendo ahora mismo).
     */
    fun prefetch(
        context: Context,
        songs: List<Song>,
        highPriority: Boolean = false
    ) {
        if (songs.isEmpty()) return
        ensureStarted(context.applicationContext)
        val target = if (highPriority) this.highPriority else background

        for (song in songs) {
            if (song.id <= 0L) continue
            if (AlbumArtUtils.getCachedThumbnail(song.id) != null) continue
            this.songs[song.id] = song
            if (!queued.add(song.id)) continue
            if (target.trySend(song.id).isFailure) {
                queued.remove(song.id)
            }
        }
    }

    private fun ensureStarted(context: Context) {
        if (!started.compareAndSet(false, true)) return

        repeat(WORKERS) {
            scope.launch {
                while (true) {
                    val songId: Long = highPriority.tryReceive().getOrNull()
                        ?: background.tryReceive().getOrNull()
                        ?: select {
                            highPriority.onReceive { it }
                            background.onReceive { it }
                        }

                    try {
                        val song = songs[songId] ?: continue
                        if (AlbumArtUtils.getCachedThumbnail(songId) == null) {
                            AlbumArtUtils.getThumbnailArtwork(context, song)
                        }
                    } catch (_: Exception) {
                        // Una portada ilegible no debe detener la precarga.
                    } finally {
                        queued.remove(songId)
                    }
                }
            }
        }
    }
}
