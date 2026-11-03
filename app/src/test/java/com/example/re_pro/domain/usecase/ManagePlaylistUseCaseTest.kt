package com.example.re_pro.domain.usecase

import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.data.model.UserSettings
import com.example.re_pro.domain.model.Playlist
import com.example.re_pro.domain.repository.SettingsRepository
import com.example.re_pro.viewmodel.AlbumArtMode
import com.example.re_pro.viewmodel.NowPlayingAnimation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ManagePlaylistUseCaseTest {

    private lateinit var useCase: ManagePlaylistUseCase
    private lateinit var fakeRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        fakeRepository = FakeSettingsRepository()
        useCase = ManagePlaylistUseCase(fakeRepository)
    }

    @Test
    fun `create playlist adds new playlist`() = runBlocking {
        val existing = emptyList<Playlist>()
        val result = useCase.create(existing, "My Playlist")
        assertEquals(1, result.size)
        assertEquals("My Playlist", result[0].name)
        assertTrue(result[0].id.isNotEmpty())
    }

    @Test
    fun `create playlist with blank name returns unchanged`() = runBlocking {
        val existing = emptyList<Playlist>()
        val result = useCase.create(existing, "  ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `create playlist trims name`() = runBlocking {
        val result = useCase.create(emptyList(), "  My Playlist  ")
        assertEquals("My Playlist", result[0].name)
    }

    @Test
    fun `addSong adds song to correct playlist`() = runBlocking {
        val playlists = listOf(Playlist(id = "1", name = "Test", songIds = emptySet()))
        val result = useCase.addSong(playlists, "1", 42L)
        assertTrue(result[0].songIds.contains(42L))
    }

    @Test
    fun `addSong does not affect other playlists`() = runBlocking {
        val playlists = listOf(
            Playlist(id = "1", name = "Test1", songIds = emptySet()),
            Playlist(id = "2", name = "Test2", songIds = emptySet())
        )
        val result = useCase.addSong(playlists, "1", 42L)
        assertTrue(result[0].songIds.contains(42L))
        assertTrue(result[1].songIds.isEmpty())
    }

    @Test
    fun `removeSong removes song from playlist`() = runBlocking {
        val playlists = listOf(Playlist(id = "1", name = "Test", songIds = setOf(1L, 2L, 3L)))
        val result = useCase.removeSong(playlists, "1", 2L)
        assertEquals(setOf(1L, 3L), result[0].songIds)
    }

    @Test
    fun `toggleFavorite adds to favorites`() = runBlocking {
        val result = useCase.toggleFavorite(emptySet(), 42L)
        assertTrue(result.contains(42L))
    }

    @Test
    fun `toggleFavorite removes from favorites`() = runBlocking {
        val result = useCase.toggleFavorite(setOf(42L), 42L)
        assertFalse(result.contains(42L))
    }

    @Test
    fun `toggleFavorite does not affect other favorites`() = runBlocking {
        val result = useCase.toggleFavorite(setOf(1L, 2L), 2L)
        assertTrue(result.contains(1L))
        assertFalse(result.contains(2L))
    }
}

class FakeSettingsRepository : SettingsRepository {
    private var favorites = MutableStateFlow<Set<Long>>(emptySet())
    private var playlists = MutableStateFlow<List<Playlist>>(emptyList())

    override fun userSettingsFlow(): Flow<UserSettings> = flowOf(UserSettings())
    override fun favoriteSongIdsFlow(): Flow<Set<Long>> = favorites
    override fun customPlaylistsFlow(): Flow<List<Playlist>> = playlists
    override suspend fun isFirstLaunch(): Boolean = false
    override suspend fun setFirstLaunchComplete() {}
    override suspend fun saveFavoriteSongIds(ids: Set<Long>) { favorites.value = ids }
    override suspend fun saveCustomPlaylists(playlists: List<Playlist>) { this.playlists.value = playlists }
    override suspend fun setExcludeShortClips(exclude: Boolean) {}
    override suspend fun setThemeMode(themeMode: AppThemeMode) {}
    override suspend fun setBackgroundImageUri(uri: String?) {}
    override suspend fun setBackgroundBlur(blur: Float) {}
    override suspend fun setBackgroundOpacity(opacity: Float) {}
    override suspend fun setGridColumns(columns: Int) {}
    override suspend fun setCompactList(compact: Boolean) {}
    override suspend fun setAlbumArtMode(mode: AlbumArtMode) {}
    override suspend fun setNowPlayingAnimation(mode: NowPlayingAnimation) {}
}
