package com.example.re_pro.widget

import android.graphics.Bitmap
import androidx.media3.exoplayer.ExoPlayer
import com.example.re_pro.data.model.Song
import com.example.re_pro.viewmodel.PlaybackStateHolder

object ReProWidgetState {
    @Volatile
    var songTitle: String = "Re-Pro"

    @Volatile
    var songArtist: String = "Tu musica, tu estilo"

    @Volatile
    var songAlbum: String = ""

    @Volatile
    var isPlaying: Boolean = false

    @Volatile
    var hasSong: Boolean = false

    @Volatile
    var progressMs: Long = 0L

    @Volatile
    var durationMs: Long = 0L

    @Volatile
    var albumArt: Bitmap? = null

    @Volatile
    var upNext: List<String> = emptyList()

    @Volatile
    var queueCount: Int = 0

    fun refresh(player: ExoPlayer) {
        val metadata = player.currentMediaItem?.mediaMetadata
        songTitle = metadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: "Re-Pro"
        songArtist = metadata?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Tu musica, tu estilo"
        songAlbum = metadata?.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: ""
        isPlaying = player.isPlaying
        hasSong = player.currentMediaItem != null
        progressMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.coerceAtLeast(0L)
    }

    fun refresh(state: PlaybackStateHolder.State) {
        val song = state.filteredSongs.firstOrNull { it.id == state.currentSongId }
            ?: state.filteredSongs.getOrNull(state.currentSongIndex)
        songTitle = song?.title?.takeIf { it.isNotBlank() } ?: "Re-Pro"
        songArtist = song?.artist?.takeIf { it.isNotBlank() } ?: "Tu musica, tu estilo"
        songAlbum = song?.album?.takeIf { it.isNotBlank() } ?: ""
        isPlaying = state.isPlaying
        hasSong = song != null
        progressMs = state.currentPositionMs.coerceAtLeast(0L)
        durationMs = state.durationMs.coerceAtLeast(0L)
        fillUpNext(state.filteredSongs, state.currentSongIndex)
    }

    private fun fillUpNext(songs: List<Song>, currentIndex: Int) {
        if (songs.isEmpty()) {
            upNext = emptyList()
            queueCount = 0
            return
        }
        queueCount = songs.size
        upNext = songs.drop(currentIndex + 1).take(3).map { it.title }
    }
}

fun progressFraction(pos: Long, dur: Long): Float =
    if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f