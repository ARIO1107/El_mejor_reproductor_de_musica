package com.example.re_pro.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.re_pro.data.database.dao.CacheMetadataDao
import com.example.re_pro.data.database.entities.CacheMetadataEntity
import com.example.re_pro.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HybridMusicRepository(
    private val context: Context,
    private val cacheMetadataDao: CacheMetadataDao
) {

    // ═══════════════════════════════════════════════
    // CACHE METADATA (Room)
    // ═══════════════════════════════════════════════

    fun getAllCachedMetadata(): Flow<List<CacheMetadataEntity>> = cacheMetadataDao.getAllCachedMetadata()

    suspend fun cacheSongMetadata(song: Song) {
        val metadata = CacheMetadataEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            albumArtUri = song.albumArtUri?.toString(),
            duration = song.duration,
            dateAdded = song.dateAdded,
            album = song.album,
            genre = song.genre,
            contentUri = song.uri.toString()
        )
        cacheMetadataDao.insertCacheMetadata(metadata)
    }

    suspend fun getCachedMetadata(songId: Long): CacheMetadataEntity? {
        return cacheMetadataDao.getCachedMetadata(songId)
    }

    suspend fun clearAllCache() {
        cacheMetadataDao.clearAllCacheMetadata()
    }

    // ═══════════════════════════════════════════════
    // HYBRID REPOSITORY: Local Network switching
    // ═══════════════════════════════════════════════

    sealed class PlaybackSource {
        data class Local(val songs: List<Song>) : PlaybackSource()
        data class Remote(val songs: List<Song>) : PlaybackSource()
        data class Hybrid(val localSongs: List<Song>, val remoteAvailable: Boolean) : PlaybackSource()
    }

    suspend fun getPlaybackSource(
        preferLocal: Boolean = true,
        forceRemote: Boolean = false
    ): PlaybackSource {
        return when {
            forceRemote -> {
                PlaybackSource.Remote(fetchRemoteSongs())
            }
            preferLocal -> {
                val localSongs = fetchLocalSongs()
                if (localSongs.isNotEmpty()) {
                    localSongs.forEach { cacheSongMetadata(it) }
                    PlaybackSource.Local(localSongs)
                } else {
                    PlaybackSource.Remote(fetchRemoteSongs())
                }
            }
            else -> {
                val localSongs = fetchLocalSongs()
                val remoteSongs = fetchRemoteSongs()
                PlaybackSource.Hybrid(localSongs, remoteSongs.isNotEmpty())
            }
        }
    }

    suspend fun fetchLocalSongs(excludeShortClips: Boolean = true): List<Song> {
        return withContext(Dispatchers.IO) {
            fetchLocalSongsBlocking(excludeShortClips)
        }
    }

    private fun fetchLocalSongsBlocking(excludeShortClips: Boolean): List<Song> {
        val songsList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val selection = if (excludeShortClips) {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        }

        val artworkUri = Uri.parse("content://media/external/audio/albumart")

        try {
            context.contentResolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val genreColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    -1
                }
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Título Desconocido"
                    val artist = cursor.getString(artistColumn) ?: "Artista Desconocido"
                    val album = cursor.getString(albumColumn) ?: "Álbum Desconocido"
                    val albumId = cursor.getLong(albumIdColumn)
                    val genre = if (genreColumn >= 0) {
                        cursor.getString(genreColumn) ?: "Género Desconocido"
                    } else {
                        "Género Desconocido"
                    }
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)

                    val contentUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val albumName = album ?: ""
                    // Bloqueamos explícitamente nombres genéricos que MediaStore agrupa bajo un mismo albumId
                    val isGenericAlbum = albumName.isBlank() ||
                            albumName.contains("Desconocido", ignoreCase = true) ||
                            albumName.contains("<unknown>", ignoreCase = true) ||
                            albumName.contains("WhatsApp", ignoreCase = true) ||
                            albumName.contains("Download", ignoreCase = true)

                    // Si es un álbum genérico, forzamos null para que la UI (AlbumArtThumbnail) use el contentUri individual
                    val albumArtUri = if (albumId > 0 && !isGenericAlbum) {
                        android.content.ContentUris.withAppendedId(artworkUri, albumId)
                    } else {
                        null
                    }

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            uri = contentUri,
                            duration = duration,
                            albumId = albumId,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            album = album,
                            genre = genre
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val distinctSongs = songsList
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

    private suspend fun fetchRemoteSongs(): List<Song> {
        return emptyList()
    }

    suspend fun syncFavoritesWithServer() {
        // TODO: Implement server sync
    }

    suspend fun getOfflineQueue(): List<Song> {
        val cachedMetadata = cacheMetadataDao.getAllCachedMetadata().first()
        return cachedMetadata.map { metadata ->
            Song(
                id = metadata.songId,
                title = metadata.title,
                artist = metadata.artist,
                uri = Uri.parse(metadata.contentUri.ifEmpty { metadata.albumArtUri ?: "" }),
                duration = metadata.duration,
                dateAdded = metadata.dateAdded,
                album = metadata.album,
                genre = metadata.genre
            )
        }
    }

    suspend fun batchCacheSongs(songs: List<Song>) {
        songs.forEach { song ->
            cacheSongMetadata(song)
        }
    }
}