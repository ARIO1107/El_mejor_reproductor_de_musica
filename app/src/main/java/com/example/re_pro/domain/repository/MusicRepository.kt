package com.example.re_pro.domain.repository

import com.example.re_pro.domain.model.PlaybackSource
import com.example.re_pro.domain.model.Song

interface MusicRepository {
    suspend fun fetchLocalSongs(excludeShortClips: Boolean = false): List<Song>
    suspend fun getOfflineQueue(): List<Song>
    suspend fun batchCacheSongs(songs: List<Song>)
    fun getPlaybackSource(preferLocal: Boolean, forceRemote: Boolean): PlaybackSource
}
