package com.example.re_pro.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.example.re_pro.data.model.Song
import com.example.re_pro.utils.MusicPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AndroidAutoMediaBrowserService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "AndroidAutoService"
        private const val ROOT_ID = "repro_root"
        private const val SONGS_CATEGORY = "songs_category"
        private const val FAVORITES_CATEGORY = "favorites_category"
        private const val MOST_PLAYED_CATEGORY = "most_played_category"
    }

    private var mediaSession: MediaSessionCompat? = null
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var allSongs: List<Song> = emptyList()
    private var favoriteIds: Set<Long> = emptySet()
    private var playCountMap: Map<Long, Long> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Android Auto MediaBrowserService created")

        mediaSession = MediaSessionCompat(this, "ReProSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    MusicPlayerManager.togglePlayPause()
                }

                override fun onPause() {
                    MusicPlayerManager.togglePlayPause()
                }

                override fun onSkipToNext() {
                    MusicPlayerManager.skipNext()
                }

                override fun onSkipToPrevious() {
                    MusicPlayerManager.skipPrevious()
                }

                override fun onStop() {
                    MusicPlayerManager.togglePlayPause()
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    mediaId?.let { id ->
                        val songId = id.toLongOrNull() ?: return
                        val song = allSongs.find { it.id == songId } ?: return
                        val index = allSongs.indexOf(song)
                        if (index >= 0) {
                            MusicPlayerManager.skipTo(index)
                        }
                    }
                }
            })
            isActive = true
        }

        sessionToken = mediaSession?.sessionToken

        loadSongsForAuto()
    }

    private fun loadSongsForAuto() {
        ioScope.launch {
            try {
                val songs = runCatching { MusicPlayerManager.getAllSongs(this@AndroidAutoMediaBrowserService) }
                    .getOrDefault(emptyList())
                allSongs = songs
                Log.d(TAG, "Loaded ${allSongs.size} songs for Android Auto")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load songs for Auto", e)
                allSongs = emptyList()
            }
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.d(TAG, "onGetRoot for: $clientPackageName")
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren for: $parentId")

        val mediaItems = when (parentId) {
            ROOT_ID -> getRootItems()
            SONGS_CATEGORY -> getSongItems(allSongs)
            FAVORITES_CATEGORY -> getSongItems(allSongs.filter { favoriteIds.contains(it.id) })
            MOST_PLAYED_CATEGORY -> getSongItems(
                allSongs.sortedByDescending { playCountMap[it.id] ?: 0 }.take(50)
            )
            else -> emptyList()
        }

        result.sendResult(mediaItems.toMutableList())
    }

    private fun getRootItems(): List<MediaItem> {
        return listOf(
            createBrowsableMediaItem(SONGS_CATEGORY, "Todas las Canciones", allSongs.size),
            createBrowsableMediaItem(FAVORITES_CATEGORY, "Favoritas", allSongs.count { favoriteIds.contains(it.id) }),
            createBrowsableMediaItem(MOST_PLAYED_CATEGORY, "Mas Escuchadas", minOf(50, allSongs.size))
        )
    }

    private fun createBrowsableMediaItem(mediaId: String, title: String, itemCount: Int): MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle("$itemCount canciones")
            .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    private fun getSongItems(songs: List<Song>): List<MediaItem> {
        return songs.map { song ->
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build()

            val description = MediaDescriptionCompat.Builder()
                .setMediaId(song.id.toString())
                .setTitle(song.title)
                .setSubtitle(song.artist)
                .setMediaUri(song.uri)
                .build()

            MediaItem(description, MediaItem.FLAG_PLAYABLE)
        }
    }

    fun updateData(songs: List<Song>, favorites: Set<Long>, playCounts: Map<Long, Long>) {
        allSongs = songs
        favoriteIds = favorites
        playCountMap = playCounts
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession?.release()
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
