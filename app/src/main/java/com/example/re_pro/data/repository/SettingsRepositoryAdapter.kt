package com.example.re_pro.data.repository

import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.data.model.UserSettings
import com.example.re_pro.domain.model.Playlist as DomainPlaylist
import com.example.re_pro.domain.repository.SettingsRepository
import com.example.re_pro.viewmodel.AlbumArtMode
import com.example.re_pro.viewmodel.NowPlayingAnimation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryAdapter(
    private val delegate: com.example.re_pro.data.repository.SettingsRepository
) : SettingsRepository {

    override fun userSettingsFlow(): Flow<UserSettings> {
        return delegate.userSettingsFlow
    }

    override fun favoriteSongIdsFlow(): Flow<Set<Long>> {
        return delegate.favoriteSongIdsFlow
    }

    override fun customPlaylistsFlow(): Flow<List<DomainPlaylist>> {
        return delegate.customPlaylistsFlow.map { list ->
            list.map { vp ->
                DomainPlaylist(
                    id = vp.id,
                    name = vp.name,
                    songIds = vp.songIds
                )
            }
        }
    }

    override suspend fun isFirstLaunch(): Boolean = delegate.isFirstLaunch()

    override suspend fun setFirstLaunchComplete() = delegate.setFirstLaunchComplete()

    override suspend fun saveFavoriteSongIds(ids: Set<Long>) = delegate.saveFavoriteSongIds(ids)

    override suspend fun saveCustomPlaylists(playlists: List<DomainPlaylist>) {
        delegate.saveCustomPlaylists(playlists.map { dp ->
            com.example.re_pro.viewmodel.Playlist(
                id = dp.id,
                name = dp.name,
                songIds = dp.songIds
            )
        })
    }

    override suspend fun setExcludeShortClips(exclude: Boolean) = delegate.setExcludeShortClips(exclude)

    override suspend fun setThemeMode(themeMode: AppThemeMode) = delegate.setThemeMode(themeMode)

    override suspend fun setBackgroundImageUri(uri: String?) = delegate.setBackgroundImageUri(uri)

    override suspend fun setBackgroundBlur(blur: Float) = delegate.setBackgroundBlur(blur)

    override suspend fun setBackgroundOpacity(opacity: Float) = delegate.setBackgroundOpacity(opacity)

    override suspend fun setGridColumns(columns: Int) = delegate.setGridColumns(columns)

    override suspend fun setCompactList(compact: Boolean) = delegate.setCompactList(compact)

    override suspend fun setAlbumArtMode(mode: AlbumArtMode) = delegate.setAlbumArtMode(mode)

    override suspend fun setNowPlayingAnimation(mode: NowPlayingAnimation) = delegate.setNowPlayingAnimation(mode)
}
