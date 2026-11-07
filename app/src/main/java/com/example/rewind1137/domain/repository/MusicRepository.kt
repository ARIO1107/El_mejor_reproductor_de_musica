package com.example.rewind1137.domain.repository

import com.example.rewind1137.domain.model.PlaybackSource
import com.example.rewind1137.domain.model.Song

interface MusicRepository {
    suspend fun fetchLocalSongs(excludeShortClips: Boolean = false): List<Song>
    suspend fun getOfflineQueue(): List<Song>
    suspend fun batchCacheSongs(songs: List<Song>)
    fun getPlaybackSource(preferLocal: Boolean, forceRemote: Boolean): PlaybackSource
}
