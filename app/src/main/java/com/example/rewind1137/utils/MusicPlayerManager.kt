package com.example.rewind1137.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.rewind1137.audio.VocalRemovalRenderersFactory
import com.example.rewind1137.audio.VocalRemovalState

object MusicPlayerManager {
    private var exoPlayer: ExoPlayer? = null
    private var crossfadeDurationMs: Long = 0L
    private var isFadingOut = false
    private val handler = Handler(Looper.getMainLooper())

    private val crossfadeListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state != Player.STATE_READY || crossfadeDurationMs <= 0L) return
            val player = exoPlayer ?: return
            val duration = player.duration
            if (duration <= 0L) return
            val remaining = duration - player.currentPosition
            if (remaining <= crossfadeDurationMs && !isFadingOut && player.hasNextMediaItem()) {
                isFadingOut = true
                fadeOutAndSwitch()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) isFadingOut = false
        }
    }

    private fun fadeOutAndSwitch() {
        val player = exoPlayer ?: return
        val steps = (crossfadeDurationMs / 50).toInt().coerceAtLeast(1)
        val startVolume = player.volume
        var currentStep = 0

        val fadeRunnable = object : Runnable {
            override fun run() {
                currentStep++
                val progress = currentStep.toFloat() / steps
                player.volume = startVolume * (1f - progress)
                if (currentStep < steps) {
                    handler.postDelayed(this, 50)
                } else {
                    player.volume = 0f
                    player.seekToNext()
                    player.play()
                    fadeVolumeIn(startVolume)
                }
            }
        }
        handler.postDelayed(fadeRunnable, 50)
    }

    private fun fadeVolumeIn(targetVolume: Float) {
        val player = exoPlayer ?: return
        val steps = (crossfadeDurationMs / 50).toInt().coerceAtLeast(1)
        var currentStep = 0

        val fadeRunnable = object : Runnable {
            override fun run() {
                currentStep++
                val progress = currentStep.toFloat() / steps
                player.volume = targetVolume * progress
                if (currentStep < steps) {
                    handler.postDelayed(this, 50)
                } else {
                    player.volume = targetVolume
                    isFadingOut = false
                }
            }
        }
        handler.postDelayed(fadeRunnable, 50)
    }

    fun getPlayer(context: Context): ExoPlayer {
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

            exoPlayer!!.addListener(crossfadeListener)
        }
        return exoPlayer!!
    }

    private var replayGainEnabled = false
    private var replayGainPreamp: Float = 1.0f

    fun setReplayGain(enabled: Boolean, preamp: Float = 1.0f) {
        replayGainEnabled = enabled
        replayGainPreamp = preamp.coerceIn(0.1f, 2.0f)
        if (enabled) {
            exoPlayer?.volume = replayGainPreamp
        } else {
            exoPlayer?.volume = 1.0f
        }
    }

    fun isReplayGainEnabled(): Boolean = replayGainEnabled

    fun setVocalRemovalEnabled(enabled: Boolean) {
        VocalRemovalState.enabled = enabled
    }

    fun isVocalRemovalEnabled(): Boolean = VocalRemovalState.enabled

    fun setCrossfadeDuration(seconds: Int) {
        crossfadeDurationMs = (seconds * 1000L).coerceIn(0L, 10000L)
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun play() {
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
        }
    }

    fun skipNext() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            } else {
                player.seekTo(0)
            }
        }
    }

    fun skipPrevious() {
        exoPlayer?.let { player ->
            if (player.currentPosition > 3000) {
                player.seekTo(0)
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            } else {
                player.seekTo(0)
            }
        }
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    fun skipTo(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index.toLong())
            player.play()
        }
    }

    fun getAllSongs(context: android.content.Context): List<com.example.rewind1137.data.model.Song> {
        val songs = mutableListOf<com.example.rewind1137.data.model.Song>()
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.TITLE,
            android.provider.MediaStore.Audio.Media.ARTIST,
            android.provider.MediaStore.Audio.Media.ALBUM,
            android.provider.MediaStore.Audio.Media.DURATION,
            android.provider.MediaStore.Audio.Media.ALBUM_ID,
            android.provider.MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${android.provider.MediaStore.Audio.Media.DURATION} > 30000"
        val sortOrder = "${android.provider.MediaStore.Audio.Media.DATE_ADDED} DESC"
        val contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val artworkUri = android.net.Uri.parse("content://media/external/audio/albumart")

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
                val artUri = android.content.ContentUris.withAppendedId(artworkUri, albumId)
                val genre = try {
                    val genreProjection = arrayOf(android.provider.MediaStore.Audio.Genres.NAME)
                    val genreUri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Genres.getContentUri("external"), id
                    )
                    context.contentResolver.query(genreUri, genreProjection, null, null, null)?.use { gCursor ->
                        if (gCursor.moveToFirst()) gCursor.getString(0) ?: "" else ""
                    } ?: ""
                } catch (_: Exception) { "" }

                songs.add(
                    com.example.rewind1137.data.model.Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        duration = duration,
                        albumId = albumId,
                        albumArtUri = artUri,
                        dateAdded = dateAdded,
                        genre = genre
                    )
                )
            }
        }
        val distinctSongs = songs
            .distinctBy { it.id }
            .distinctBy { song ->
                val titleNorm = song.title.trim().lowercase()
                val artistNorm = song.artist.trim().lowercase()
                val durationSec = song.duration / 1000
                if (titleNorm.isNotEmpty() && durationSec > 0) {
                    "${titleNorm}_${artistNorm}_${durationSec}"
                } else {
                    song.id.toString()
                }
            }
        return distinctSongs
    }

    fun releasePlayer() {
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.removeListener(crossfadeListener)
        exoPlayer?.release()
        exoPlayer = null
    }
}
