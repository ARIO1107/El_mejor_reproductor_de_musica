package com.example.rewind1137.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.viewmodel.PlaybackMode
import com.example.rewind1137.viewmodel.PlaybackStateHolder

/**
 * Estado compartido entre el reproductor y los widgets de la pantalla de inicio.
 *
 * Ademas de la cancion exacta que suena, guarda una copia ligera en disco para
 * que los widgets sigan mostrando la ultima cancion aunque Android haya matado
 * el proceso de la app.
 */
object RewindWidgetState {

    @Volatile
    var songTitle: String = "Rewind 1137"

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

    @Volatile
    var currentSongId: Long = -1L

    @Volatile
    var isFavorite: Boolean = false

    @Volatile
    var playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL

    @Volatile
    var songUri: Uri? = null

    @Volatile
    var albumArtUri: Uri? = null

    private const val PREFS_NAME = "rewind_widget_state"
    private const val KEY_HAS_STATE = "has_state"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_ALBUM = "album"
    private const val KEY_HAS_SONG = "has_song"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_DURATION = "duration"
    private const val KEY_SONG_ID = "song_id"
    private const val KEY_SONG_URI = "song_uri"
    private const val KEY_ALBUM_ART_URI = "album_art_uri"
    private const val KEY_FAVORITE = "favorite"
    private const val KEY_MODE = "mode"
    private const val KEY_UP_NEXT = "up_next"
    private const val KEY_QUEUE_COUNT = "queue_count"
    private const val LIST_SEPARATOR = '\u001F'

    @Volatile
    private var initialized = false

    fun refresh(player: ExoPlayer) {
        val mediaItem = player.currentMediaItem
        val metadata = mediaItem?.mediaMetadata
        songTitle = metadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: "Rewind 1137"
        songArtist = metadata?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Tu musica, tu estilo"
        songAlbum = metadata?.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: ""
        isPlaying = player.isPlaying
        hasSong = mediaItem != null
        progressMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.coerceAtLeast(0L)
        if (mediaItem != null) {
            initialized = true
        }
    }

    fun refreshPlayback(player: ExoPlayer) {
        isPlaying = player.isPlaying
        progressMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.coerceAtLeast(0L)
        if (player.currentMediaItem != null) {
            initialized = true
        }
    }

    fun refresh(state: PlaybackStateHolder.State) {
        val song = state.currentSong
            ?: state.filteredSongs.firstOrNull { it.id == state.currentSongId }
            ?: state.filteredSongs.getOrNull(state.currentSongIndex)
        val index = state.filteredSongs
            .indexOfFirst { it.id == song?.id }
            .takeIf { it >= 0 }
            ?: state.currentSongIndex

        songTitle = song?.title?.takeIf { it.isNotBlank() } ?: "Rewind 1137"
        songArtist = song?.artist?.takeIf { it.isNotBlank() } ?: "Tu musica, tu estilo"
        songAlbum = song?.album?.takeIf { it.isNotBlank() } ?: ""
        isPlaying = state.isPlaying
        hasSong = song != null
        progressMs = state.currentPositionMs.coerceAtLeast(0L)
        durationMs = state.durationMs.coerceAtLeast(0L)
        currentSongId = song?.id ?: -1L
        isFavorite = song != null && state.favoriteSongIds.contains(song.id)
        playbackMode = state.playbackMode
        songUri = song?.uri
        albumArtUri = song?.albumArtUri
        fillUpNext(state.filteredSongs, index)

        if (song != null || state.filteredSongs.isNotEmpty()) {
            initialized = true
        }
    }

    private fun fillUpNext(songs: List<Song>, currentIndex: Int) {
        if (songs.isEmpty()) {
            upNext = emptyList()
            queueCount = 0
            return
        }
        queueCount = songs.size
        val start = (currentIndex + 1).coerceIn(0, songs.size)
        upNext = songs.drop(start).take(3).map { it.title }
    }

    /**
     * Recupera la ultima cancion guardada cuando el proceso arranca de cero
     * (por ejemplo al agregar el widget despues de reiniciar el telefono).
     */
    fun restore(context: Context) {
        if (initialized) return
        initialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_STATE, false)) return

        songTitle = prefs.getString(KEY_TITLE, songTitle) ?: songTitle
        songArtist = prefs.getString(KEY_ARTIST, songArtist) ?: songArtist
        songAlbum = prefs.getString(KEY_ALBUM, songAlbum) ?: songAlbum
        hasSong = prefs.getBoolean(KEY_HAS_SONG, false)
        isPlaying = false
        progressMs = prefs.getLong(KEY_PROGRESS, 0L)
        durationMs = prefs.getLong(KEY_DURATION, 0L)
        currentSongId = prefs.getLong(KEY_SONG_ID, -1L)
        isFavorite = prefs.getBoolean(KEY_FAVORITE, false)
        songUri = prefs.getString(KEY_SONG_URI, null)?.let(Uri::parse)
        albumArtUri = prefs.getString(KEY_ALBUM_ART_URI, null)?.let(Uri::parse)
        playbackMode = runCatching {
            PlaybackMode.valueOf(
                prefs.getString(KEY_MODE, PlaybackMode.SEQUENTIAL.name) ?: PlaybackMode.SEQUENTIAL.name
            )
        }.getOrDefault(PlaybackMode.SEQUENTIAL)
        upNext = prefs.getString(KEY_UP_NEXT, null)
            ?.split(LIST_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        queueCount = prefs.getInt(KEY_QUEUE_COUNT, 0)
    }

    fun save(context: Context) {
        if (!initialized || !hasSong) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_STATE, true)
            .putString(KEY_TITLE, songTitle)
            .putString(KEY_ARTIST, songArtist)
            .putString(KEY_ALBUM, songAlbum)
            .putBoolean(KEY_HAS_SONG, hasSong)
            .putLong(KEY_PROGRESS, progressMs)
            .putLong(KEY_DURATION, durationMs)
            .putLong(KEY_SONG_ID, currentSongId)
            .putBoolean(KEY_FAVORITE, isFavorite)
            .putString(KEY_MODE, playbackMode.name)
            .putString(KEY_SONG_URI, songUri?.toString())
            .putString(KEY_ALBUM_ART_URI, albumArtUri?.toString())
            .putString(KEY_UP_NEXT, upNext.joinToString(LIST_SEPARATOR.toString()))
            .putInt(KEY_QUEUE_COUNT, queueCount)
            .apply()
    }
}

fun progressFraction(pos: Long, dur: Long): Float =
    if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
