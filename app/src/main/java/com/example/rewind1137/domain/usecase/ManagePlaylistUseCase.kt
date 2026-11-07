package com.example.rewind1137.domain.usecase

import com.example.rewind1137.domain.model.Playlist
import com.example.rewind1137.domain.repository.SettingsRepository
import javax.inject.Inject

class ManagePlaylistUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend fun create(playlists: List<Playlist>, name: String): List<Playlist> {
        if (name.isBlank()) return playlists
        val newPlaylist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim()
        )
        val updated = playlists + newPlaylist
        repository.saveCustomPlaylists(updated)
        return updated
    }

    suspend fun addSong(playlists: List<Playlist>, playlistId: String, songId: Long): List<Playlist> {
        val updated = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds + songId)
            } else playlist
        }
        repository.saveCustomPlaylists(updated)
        return updated
    }

    suspend fun removeSong(playlists: List<Playlist>, playlistId: String, songId: Long): List<Playlist> {
        val updated = playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds - songId)
            } else playlist
        }
        repository.saveCustomPlaylists(updated)
        return updated
    }

    suspend fun toggleFavorite(favorites: Set<Long>, songId: Long): Set<Long> {
        val updated = favorites.toMutableSet()
        if (updated.contains(songId)) {
            updated.remove(songId)
        } else {
            updated.add(songId)
        }
        repository.saveFavoriteSongIds(updated)
        return updated
    }
}
