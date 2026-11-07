package com.example.rewind1137.domain.repository

import com.example.rewind1137.data.model.AppThemeMode
import com.example.rewind1137.data.model.UserSettings
import com.example.rewind1137.domain.model.Playlist
import com.example.rewind1137.viewmodel.AlbumArtMode
import com.example.rewind1137.viewmodel.NowPlayingAnimation
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun userSettingsFlow(): Flow<UserSettings>
    fun favoriteSongIdsFlow(): Flow<Set<Long>>
    fun customPlaylistsFlow(): Flow<List<Playlist>>
    suspend fun isFirstLaunch(): Boolean
    suspend fun setFirstLaunchComplete()
    suspend fun saveFavoriteSongIds(ids: Set<Long>)
    suspend fun saveCustomPlaylists(playlists: List<Playlist>)
    suspend fun setExcludeShortClips(exclude: Boolean)
    suspend fun setThemeMode(themeMode: AppThemeMode)
    suspend fun setBackgroundImageUri(uri: String?)
    suspend fun setBackgroundBlur(blur: Float)
    suspend fun setBackgroundOpacity(opacity: Float)
    suspend fun setGridColumns(columns: Int)

    suspend fun setCompactList(compact: Boolean)

    suspend fun setAlbumArtMode(mode: AlbumArtMode)

    suspend fun setNowPlayingAnimation(mode: NowPlayingAnimation)
}
