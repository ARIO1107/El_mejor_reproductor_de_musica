package com.example.rewind1137.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.rewind1137.audio.VocalRemovalRenderersFactory
import com.example.rewind1137.audio.VocalRemovalState
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.viewmodel.PlaybackMode

/**
 * Dueño único del ExoPlayer y de la cola.
 * La interfaz, la notificación, el widget y Android Auto pasan por aquí,
 * así que siguiente / anterior / aleatorio hacen lo mismo en todos lados.
 */
@OptIn(UnstableApi::class)
object MusicPlayerManager {
    private var exoPlayer: ExoPlayer? = null
    private var auxPlayer: ExoPlayer? = null
    private var crossfadeDurationMs: Long = 0L
    private var isFadingOut = false
    private val handler = Handler(Looper.getMainLooper())

    private val queue = mutableListOf<Song>()
    private var unshuffled: List<Song> = emptyList()
    var playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL
        private set

    private var loadGeneration = 0
    private var replayGainEnabled = false
    private var replayGainPreamp: Float = 1.0f
    private var baseVolume = 1f

    private val crossfadeRunnable = Runnable { startOverlap() }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) scheduleCrossfade()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) scheduleCrossfade() else if (!isFadingOut) cancelCrossfade()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!isFadingOut) {
                exoPlayer?.volume = baseVolume
                scheduleCrossfade()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) scheduleCrossfade()
        }
    }

    fun getPlayer(context: Context): ExoPlayer {
        auxContext = context.applicationContext
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            exoPlayer = ExoPlayer.Builder(
                context.applicationContext,
                VocalRemovalRenderersFactory(context.applicationContext)
            )
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .also { it.addListener(playerListener) }
        }
        return exoPlayer!!
    }

    fun currentQueue(): List<Song> = queue.toList()

    fun currentIndex(): Int = exoPlayer?.currentMediaItemIndex?.coerceAtLeast(0) ?: 0

    fun currentPlaybackMode(): PlaybackMode = playbackMode

    fun hasSavedSession(context: Context): Boolean = PlaybackSessionStore.hasSession(context)

    fun playQueue(songs: List<Song>, startIndex: Int, play: Boolean = true) {
        val player = exoPlayer ?: return
        if (songs.isEmpty()) return
        cancelCrossfade()
        unshuffled = songs
        val sequence = sequenceFor(playbackMode, songs, startIndex)
        queue.clear()
        queue.addAll(sequence)
        val playIndex = if (playbackMode == PlaybackMode.SHUFFLE) 0
        else startIndex.coerceIn(0, sequence.lastIndex)

        loadGeneration++
        val generation = loadGeneration
        val playAt = playIndex.coerceIn(0, sequence.lastIndex)
        player.pause()
        Thread {
            val items = sequence.map { songToMediaItem(it) }
            handler.post {
                if (generation != loadGeneration) return@post
                player.setMediaItems(items, playAt, 0L)
                player.prepare()
                applyRepeat(player)
                if (play) player.play() else player.pause()
            }
        }.start()
    }

    fun restore(context: Context): Boolean {
        val player = getPlayer(context)
        if (player.mediaItemCount > 0 && queue.isNotEmpty()) return true
        if (player.mediaItemCount > 0 && queue.isEmpty()) {
            rebuildQueueFromPlayer()
            return queue.isNotEmpty()
        }
        val session = PlaybackSessionStore.load(context) ?: return false
        playbackMode = session.mode
        unshuffled = session.songs
        queue.clear()
        queue.addAll(session.songs)
        loadGeneration++
        player.setMediaItems(
            session.songs.map { songToMediaItem(it) },
            session.index.coerceIn(0, session.songs.lastIndex),
            session.positionMs
        )
        player.prepare()
        player.playWhenReady = false
        applyRepeat(player)
        return true
    }

    fun persist(context: Context) {
        val player = exoPlayer ?: return
        if (queue.isEmpty()) return
        PlaybackSessionStore.save(
            context.applicationContext,
            queue.toList(),
            player.currentMediaItemIndex.coerceIn(0, queue.lastIndex),
            player.currentPosition.coerceAtLeast(0L),
            playbackMode
        )
    }

    fun rebuildQueueFromPlayer() {
        val player = exoPlayer ?: return
        val songs = (0 until player.mediaItemCount).mapNotNull { index -> songFromItem(player.getMediaItemAt(index)) }
        if (songs.isEmpty()) return
        queue.clear()
        queue.addAll(songs)
        if (unshuffled.isEmpty()) unshuffled = songs
    }

    fun skipNext() {
        val player = exoPlayer ?: return
        val count = player.mediaItemCount
        if (count == 0) return
        cancelCrossfade()
        val next = (player.currentMediaItemIndex + 1) % count
        player.seekTo(next, 0L)
        player.play()
    }

    fun skipPrevious() {
        val player = exoPlayer ?: return
        val count = player.mediaItemCount
        if (count == 0) return
        cancelCrossfade()
        if (player.currentPosition > 3_000L) {
            player.seekTo(0L)
        } else {
            val prev = (player.currentMediaItemIndex - 1 + count) % count
            player.seekTo(prev, 0L)
        }
        player.play()
    }

    fun skipTo(index: Int) {
        val player = exoPlayer ?: return
        if (index !in 0 until player.mediaItemCount) return
        cancelCrossfade()
        player.seekTo(index, 0L)
        player.play()
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause()
            else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0L)
                player.play()
            }
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun play() {
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_ENDED && player.mediaItemCount > 0) {
                player.seekTo(0, 0L)
            }
            player.play()
        }
    }

    fun seekToPosition(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
        scheduleCrossfade()
    }

    fun removeAt(index: Int) {
        val player = exoPlayer ?: return
        if (index !in queue.indices) return
        val removed = queue.removeAt(index)
        unshuffled = unshuffled.filterNot { it.id == removed.id && it.uri == removed.uri }
        if (index < player.mediaItemCount) player.removeMediaItem(index)
    }

    fun move(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices || from == to) return
        val item = queue.removeAt(from)
        queue.add(to, item)
        val player = exoPlayer ?: return
        if (from < player.mediaItemCount && to < player.mediaItemCount) {
            player.moveMediaItem(from, to)
        }
        if (playbackMode != PlaybackMode.SHUFFLE) unshuffled = queue.toList()
    }

    fun addNext(song: Song) {
        val player = exoPlayer ?: return
        val insert = (player.currentMediaItemIndex + 1).coerceIn(0, queue.size)
        queue.add(insert, song)
        if (insert <= player.mediaItemCount) player.addMediaItem(insert, songToMediaItem(song))
    }

    fun addLast(song: Song) {
        val player = exoPlayer ?: return
        queue.add(song)
        player.addMediaItem(songToMediaItem(song))
    }

    fun clearKeepingCurrent() {
        val player = exoPlayer ?: return
        val index = player.currentMediaItemIndex
        val current = queue.getOrNull(index) ?: return
        val position = player.currentPosition
        val playing = player.isPlaying
        queue.clear()
        queue.add(current)
        unshuffled = listOf(current)
        loadGeneration++
        player.setMediaItems(listOf(songToMediaItem(current)), 0, position.coerceAtLeast(0L))
        if (playing) player.play()
    }

    fun togglePlaybackMode(): PlaybackMode {
        val next = when (playbackMode) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SEQUENTIAL
        }
        val player = exoPlayer
        val current = player?.let { queue.getOrNull(it.currentMediaItemIndex) }
        playbackMode = next
        if (player != null && (queue.isNotEmpty() || unshuffled.isNotEmpty())) {
            val source = unshuffled.ifEmpty { queue.toList() }
            val rebuilt = when (next) {
                PlaybackMode.SHUFFLE -> {
                    if (current != null) listOf(current) + source.filterNot { it.id == current.id && it.uri == current.uri }.shuffled()
                    else source.shuffled()
                }
                else -> source
            }
            if (rebuilt.isNotEmpty()) {
                queue.clear()
                queue.addAll(rebuilt)
                val newIndex = current?.let { song ->
                    rebuilt.indexOfFirst { it.id == song.id && it.uri == song.uri }
                }?.coerceAtLeast(0) ?: 0
                val position = player.currentPosition.coerceAtLeast(0L)
                val playing = player.isPlaying
                loadGeneration++
                player.setMediaItems(rebuilt.map { songToMediaItem(it) }, newIndex.coerceIn(0, rebuilt.lastIndex), position)
                if (playing) player.play()
            }
        }
        player?.let { applyRepeat(it) }
        return next
    }

    fun applyModeFlag(mode: PlaybackMode) {
        playbackMode = mode
        exoPlayer?.let { applyRepeat(it) }
    }

    fun setReplayGain(enabled: Boolean, preamp: Float = 1.0f) {
        replayGainEnabled = enabled
        replayGainPreamp = preamp.coerceIn(0.1f, 2.0f)
        if (!enabled) {
            baseVolume = 1f
            if (!isFadingOut) exoPlayer?.volume = baseVolume
        }
    }

    fun applyReplayGainFor(context: Context, uri: Uri) {
        if (!replayGainEnabled) {
            baseVolume = 1f
        } else {
            val gain = ReplayGainReader.readTrackGainDb(context, uri)
            baseVolume = if (gain == null) {
                replayGainPreamp.coerceIn(0.1f, 2f)
            } else {
                ReplayGainReader.toLinear(gain, replayGainPreamp)
            }
        }
        if (!isFadingOut) exoPlayer?.volume = baseVolume
    }

    fun isReplayGainEnabled(): Boolean = replayGainEnabled

    /** Fracción 0..1 del volumen base (temporizador, ducking). */
    fun setVolume(volume: Float) {
        exoPlayer?.volume = (baseVolume * volume.coerceIn(0f, 1f)).coerceIn(0f, 2f)
    }

    fun restoreBaseVolume() {
        if (!isFadingOut) exoPlayer?.volume = baseVolume
    }

    fun setVocalRemovalEnabled(enabled: Boolean) {
        VocalRemovalState.enabled = enabled
    }

    fun isVocalRemovalEnabled(): Boolean = VocalRemovalState.enabled

    fun setCrossfadeDuration(seconds: Int) {
        crossfadeDurationMs = (seconds * 1000L).coerceIn(0L, 12_000L)
        if (crossfadeDurationMs == 0L) cancelCrossfade() else scheduleCrossfade()
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    private fun sequenceFor(mode: PlaybackMode, songs: List<Song>, startIndex: Int): List<Song> {
        if (songs.isEmpty()) return emptyList()
        val index = startIndex.coerceIn(0, songs.lastIndex)
        return when (mode) {
            PlaybackMode.SHUFFLE -> {
                val current = songs[index]
                listOf(current) + songs.filterIndexed { i, _ -> i != index }.shuffled()
            }
            PlaybackMode.SEQUENTIAL, PlaybackMode.REPEAT_ONE -> songs
        }
    }

    private fun applyRepeat(player: ExoPlayer) {
        player.shuffleModeEnabled = false
        player.repeatMode = if (playbackMode == PlaybackMode.REPEAT_ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    private fun scheduleCrossfade() {
        handler.removeCallbacks(crossfadeRunnable)
        val player = exoPlayer ?: return
        if (crossfadeDurationMs <= 0L || isFadingOut || playbackMode == PlaybackMode.REPEAT_ONE) return
        if (!player.isPlaying || player.mediaItemCount <= 1) return
        val duration = player.duration
        if (duration <= 0L || duration == C.TIME_UNSET) return
        val remaining = duration - player.currentPosition
        if (remaining <= 0L) return
        val delayMs = (remaining - crossfadeDurationMs).coerceAtLeast(0L)
        handler.postDelayed(crossfadeRunnable, delayMs)
    }

    private fun startOverlap() {
        val player = exoPlayer ?: return
        if (isFadingOut || crossfadeDurationMs <= 0L || player.mediaItemCount <= 1) return
        if (playbackMode == PlaybackMode.REPEAT_ONE) return
        val next = (player.currentMediaItemIndex + 1) % player.mediaItemCount
        val item = runCatching { player.getMediaItemAt(next) }.getOrNull() ?: return
        isFadingOut = true
        val aux = ensureAux()
        aux.stop()
        aux.setMediaItem(item)
        aux.volume = 0f
        aux.prepare()
        aux.play()

        val steps = (crossfadeDurationMs / 50L).toInt().coerceAtLeast(1)
        val start = baseVolume
        var step = 0
        val fade = object : Runnable {
            override fun run() {
                if (!isFadingOut) return
                step++
                val progress = step.toFloat() / steps
                player.volume = start * (1f - progress)
                aux.volume = start * progress
                if (step < steps) {
                    handler.postDelayed(this, 50L)
                } else {
                    val handoff = aux.currentPosition.coerceAtLeast(0L)
                    aux.pause()
                    isFadingOut = false
                    player.volume = baseVolume
                    player.seekTo(next, handoff)
                    player.play()
                }
            }
        }
        handler.post(fade)
    }

    private fun ensureAux(): ExoPlayer {
        auxPlayer?.let { return it }
        val context = auxContext ?: error("El reproductor todavía no tiene contexto")
        val created = ExoPlayer.Builder(context, VocalRemovalRenderersFactory(context))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false
            )
            .build()
        auxPlayer = created
        return created
    }

    @Volatile
    private var auxContext: Context? = null

    fun getAllSongs(context: android.content.Context): List<Song> {
        auxContext = context.applicationContext
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.TITLE,
            android.provider.MediaStore.Audio.Media.ARTIST,
            android.provider.MediaStore.Audio.Media.ALBUM,
            android.provider.MediaStore.Audio.Media.DURATION,
            android.provider.MediaStore.Audio.Media.ALBUM_ID,
            android.provider.MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
        val contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val artworkUri = Uri.parse("content://media/external/audio/albumart")

        context.contentResolver.query(contentUri, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Desconocido"
                val artist = cursor.getString(artistColumn) ?: "Desconocido"
                val album = cursor.getString(albumColumn) ?: ""
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val uri = android.content.ContentUris.withAppendedId(contentUri, id)
                val artUri = if (albumId > 0) android.content.ContentUris.withAppendedId(artworkUri, albumId) else null
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        duration = duration,
                        albumId = albumId,
                        albumArtUri = artUri,
                        dateAdded = dateAdded,
                        genre = ""
                    )
                )
            }
        }
        return songs.distinctBy { it.id }
    }

    fun releasePlayer() {
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        auxPlayer?.release()
        auxPlayer = null
        queue.clear()
    }

    private fun cancelCrossfade() {
        isFadingOut = false
        handler.removeCallbacksAndMessages(null)
        auxPlayer?.pause()
        exoPlayer?.volume = baseVolume
    }

    private fun songToMediaItem(song: Song): MediaItem = song.toMediaItem()

    private fun songFromItem(item: MediaItem): Song? {
        val uri = item.localConfiguration?.uri
            ?: item.mediaMetadata.extras?.getString(SongMediaExtras.SONG_URI)?.let(Uri::parse)
            ?: return null
        val extras = item.mediaMetadata.extras
        val id = extras?.getLong(SongMediaExtras.SONG_ID, uri.hashCode().toLong()) ?: uri.hashCode().toLong()
        val art = extras?.getString(SongMediaExtras.ALBUM_ART_URI)
        return Song(
            id = id,
            title = item.mediaMetadata.title?.toString() ?: extras?.getString("title") ?: "Sin título",
            artist = item.mediaMetadata.artist?.toString() ?: extras?.getString("artist") ?: "",
            album = item.mediaMetadata.albumTitle?.toString() ?: extras?.getString("album") ?: "",
            uri = uri,
            duration = extras?.getLong("duration") ?: 0L,
            albumId = extras?.getLong("albumId") ?: 0L,
            albumArtUri = art?.takeIf { it.isNotBlank() }?.let(Uri::parse),
            genre = extras?.getString("genre") ?: ""
        )
    }
}
