package com.example.rewind1137.utils

import android.content.Context
import android.os.SystemClock
import com.example.rewind1137.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Precarga las miniaturas de la ventana visible y, cuando el usuario deja de
 * desplazarse, el resto de la biblioteca en segundo plano.
 *
 * Antes se encolaba la biblioteca completa (600+ canciones) con 6 hilos
 * decodificando audio en paralelo, lo que saturaba la CPU y provocaba tirones
 * al desplazar. Ahora la ventana visible siempre tiene prioridad y la precarga
 * masiva solo arranca tras unos cientos de milisegundos sin movimiento.
 */
object ThumbnailPrefetcher {

    private const val WORKERS = 3
    private const val IDLE_DELAY_MS = 120L
    private const val IDLE_AFTER_MS = 450L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val latestWindow = AtomicReference<List<Long>>(emptyList())
    private val idleQueue = AtomicReference<List<Long>>(emptyList())
    private val lastWindowAt = AtomicLong(0L)
    private val songs = ConcurrentHashMap<Long, Song>()
    private val inProgress = Collections.synchronizedSet(HashSet<Long>())
    private val started = AtomicBoolean(false)

    /**
     * Reemplaza la ventana de precarga por [songs] (normalmente la ventana
     * visible más un margen). Las peticiones anteriores dejan de tener prioridad.
     */
    fun prefetch(context: Context, songs: List<Song>) {
        if (songs.isEmpty()) return
        ensureStarted(context.applicationContext)
        latestWindow.set(register(songs))
        lastWindowAt.set(SystemClock.elapsedRealtime())
    }

    /**
     * Encola el resto de la biblioteca para cuando la interfaz esté quieta. No
     * se toca mientras el usuario sigue desplazándose.
     */
    fun prefetchIdle(context: Context, songs: List<Song>) {
        if (songs.isEmpty()) return
        ensureStarted(context.applicationContext)
        idleQueue.set(register(songs))
    }

    private fun register(songs: List<Song>): List<Long> {
        val ids = ArrayList<Long>(songs.size)
        for (song in songs) {
            if (song.id <= 0L) continue
            this.songs[song.id] = song
            ids.add(song.id)
        }
        return ids
    }

    private fun ensureStarted(context: Context) {
        if (!started.compareAndSet(false, true)) return

        repeat(WORKERS) {
            scope.launch {
                while (true) {
                    val window = latestWindow.get()
                    var next = reserve(window)
                    if (next == null &&
                        SystemClock.elapsedRealtime() - lastWindowAt.get() >= IDLE_AFTER_MS
                    ) {
                        next = reserve(idleQueue.get())
                    }

                    val id = next
                    if (id == null) {
                        delay(IDLE_DELAY_MS)
                        continue
                    }
                    try {
                        val song = songs[id] ?: continue
                        AlbumArtUtils.getThumbnailArtwork(context, song)
                    } catch (_: Exception) {
                        // Una portada ilegible no debe detener la precarga.
                    } finally {
                        inProgress.remove(id)
                    }
                }
            }
        }
    }

    /** Devuelve el primer id pendiente de [ids] y lo marca como en curso. */
    private fun reserve(ids: List<Long>): Long? {
        for (id in ids) {
            if (AlbumArtUtils.getCachedThumbnail(id) != null) continue
            if (inProgress.add(id)) return id
        }
        return null
    }
}
