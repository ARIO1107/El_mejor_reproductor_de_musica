package com.example.re_pro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.data.model.UserSettings
import com.example.re_pro.viewmodel.AlbumArtMode
import com.example.re_pro.viewmodel.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class TestableSettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val EXCLUDE_SHORT_CLIPS = booleanPreferencesKey("exclude_short_clips")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_BLUR = floatPreferencesKey("background_blur")
        val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val FAVORITE_SONG_IDS = stringSetPreferencesKey("favorite_song_ids")
        val CUSTOM_PLAYLISTS_JSON = stringPreferencesKey("custom_playlists_json")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val ALBUM_ART_MODE = stringPreferencesKey("album_art_mode")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        val excludeClips = preferences[PreferencesKeys.EXCLUDE_SHORT_CLIPS] ?: true
        val themeString = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.REPRO_NEON_BLACK.name
        val themeMode = try {
            AppThemeMode.valueOf(themeString)
        } catch (e: Exception) {
            AppThemeMode.REPRO_NEON_BLACK
        }
        val bgUri = preferences[PreferencesKeys.BACKGROUND_IMAGE_URI]
        val bgBlur = preferences[PreferencesKeys.BACKGROUND_BLUR] ?: 20f
        val bgOpacity = preferences[PreferencesKeys.BACKGROUND_OPACITY] ?: 0.5f
        val gridCols = preferences[PreferencesKeys.GRID_COLUMNS] ?: 2
        val albumArtModeString = preferences[PreferencesKeys.ALBUM_ART_MODE] ?: AlbumArtMode.STATIC.name
        val albumArtMode = try {
            AlbumArtMode.valueOf(albumArtModeString)
        } catch (e: Exception) {
            AlbumArtMode.STATIC
        }

        UserSettings(
            excludeShortAudios = excludeClips,
            themeMode = themeMode,
            backgroundImageUri = bgUri,
            backgroundBlur = bgBlur,
            backgroundOpacity = bgOpacity,
            gridColumns = gridCols,
            albumArtMode = albumArtMode
        )
    }

    val favoriteSongIdsFlow: Flow<Set<Long>> = dataStore.data.map { preferences ->
        val rawSet = preferences[PreferencesKeys.FAVORITE_SONG_IDS] ?: emptySet()
        rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    val customPlaylistsFlow: Flow<List<Playlist>> = dataStore.data.map { preferences ->
        val jsonStr = preferences[PreferencesKeys.CUSTOM_PLAYLISTS_JSON] ?: ""
        jsonToPlaylists(jsonStr)
    }

    suspend fun setExcludeShortClips(exclude: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXCLUDE_SHORT_CLIPS] = exclude
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setBackgroundImageUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.BACKGROUND_IMAGE_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.BACKGROUND_IMAGE_URI)
            }
        }
    }

    suspend fun setBackgroundBlur(blur: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_BLUR] = blur
        }
    }

    suspend fun setBackgroundOpacity(opacity: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_OPACITY] = opacity
        }
    }

    suspend fun setGridColumns(columns: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    suspend fun setAlbumArtMode(mode: AlbumArtMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_ART_MODE] = mode.name
        }
    }

    suspend fun saveFavoriteSongIds(favorites: Set<Long>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_SONG_IDS] = favorites.map { it.toString() }.toSet()
        }
    }

    suspend fun saveCustomPlaylists(playlists: List<Playlist>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_PLAYLISTS_JSON] = playlistsToJson(playlists)
        }
    }

    private fun playlistsToJson(playlists: List<Playlist>): String {
        val sb = StringBuilder("[")
        playlists.forEachIndexed { index, playlist ->
            if (index > 0) sb.append(",")
            sb.append("{")
            sb.append("\"id\":\"${playlist.id}\",")
            sb.append("\"name\":\"${playlist.name}\",")
            sb.append("\"songIds\":[")
            playlist.songIds.forEachIndexed { songIndex, songId ->
                if (songIndex > 0) sb.append(",")
                sb.append(songId)
            }
            sb.append("]")
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun jsonToPlaylists(jsonStr: String): List<Playlist> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<Playlist>()
        try {
            val trimmed = jsonStr.trim()
            if (!trimmed.startsWith("[")) return emptyList()
            val inner = trimmed.removeSurrounding("[", "]")
            if (inner.isBlank()) return emptyList()
            val objects = splitJsonObjects(inner)
            for (objStr in objects) {
                val id = extractJsonValue(objStr, "id")
                val name = extractJsonValue(objStr, "name")
                val songIdsStr = extractJsonArray(objStr, "songIds")
                val songIds = songIdsStr.mapNotNull { it.toLongOrNull() }.toSet()
                if (id != null && name != null) {
                    list.add(Playlist(id, name, songIds))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun splitJsonObjects(inner: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in inner.indices) {
            when (inner[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        objects.add(inner.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return objects
    }

    private fun extractJsonValue(objStr: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(objStr)?.groupValues?.get(1)
    }

    private fun extractJsonArray(objStr: String, key: String): List<String> {
        val pattern = "\"$key\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
        val match = pattern.find(objStr) ?: return emptyList()
        val content = match.groupValues[1].trim()
        if (content.isBlank()) return emptyList()
        return content.split(",").map { it.trim() }
    }

    suspend fun isFirstLaunch(): Boolean {
        return dataStore.data.first()[PreferencesKeys.FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
        }
    }
}

private fun createTestDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create {
        File.createTempFile("test_settings_", ".preferences_pb")
    }
}

class SettingsRepositoryTest {

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: TestableSettingsRepository

    @Before
    fun setup() {
        testDataStore = createTestDataStore()
        repository = TestableSettingsRepository(testDataStore)
    }

    @Test
    fun `initial state has default values`() = runTest {
        val settings = repository.userSettingsFlow.first()
        assertTrue(settings.excludeShortAudios)
        assertEquals(AppThemeMode.REPRO_NEON_BLACK, settings.themeMode)
        assertNull(settings.backgroundImageUri)
        assertEquals(20f, settings.backgroundBlur)
        assertEquals(0.5f, settings.backgroundOpacity)
        assertEquals(2, settings.gridColumns)
    }

    @Test
    fun `setExcludeShortClips updates value`() = runTest {
        repository.setExcludeShortClips(false)
        val settings = repository.userSettingsFlow.first()
        assertFalse(settings.excludeShortAudios)
    }

    @Test
    fun `setGridColumns updates value`() = runTest {
        repository.setGridColumns(4)
        val settings = repository.userSettingsFlow.first()
        assertEquals(4, settings.gridColumns)
    }

    @Test
    fun `albumArtMode defaults to STATIC`() = runTest {
        val settings = repository.userSettingsFlow.first()
        assertEquals(AlbumArtMode.STATIC, settings.albumArtMode)
    }

    @Test
    fun `setAlbumArtMode updates value`() = runTest {
        repository.setAlbumArtMode(AlbumArtMode.DISC)
        val settings = repository.userSettingsFlow.first()
        assertEquals(AlbumArtMode.DISC, settings.albumArtMode)
    }

    @Test
    fun `setThemeMode updates value`() = runTest {
        repository.setThemeMode(AppThemeMode.SUNSET_ORANGE)
        val settings = repository.userSettingsFlow.first()
        assertEquals(AppThemeMode.SUNSET_ORANGE, settings.themeMode)
    }

    @Test
    fun `setBackgroundImageUri updates value`() = runTest {
        val uri = "content://media/external/images/1"
        repository.setBackgroundImageUri(uri)
        val settings = repository.userSettingsFlow.first()
        assertEquals(uri, settings.backgroundImageUri)
    }

    @Test
    fun `setBackgroundImageUri null removes value`() = runTest {
        repository.setBackgroundImageUri("content://media/external/images/1")
        repository.setBackgroundImageUri(null)
        val settings = repository.userSettingsFlow.first()
        assertNull(settings.backgroundImageUri)
    }

    @Test
    fun `setBackgroundBlur updates value`() = runTest {
        repository.setBackgroundBlur(40f)
        val settings = repository.userSettingsFlow.first()
        assertEquals(40f, settings.backgroundBlur)
    }

    @Test
    fun `setBackgroundOpacity updates value`() = runTest {
        repository.setBackgroundOpacity(0.8f)
        val settings = repository.userSettingsFlow.first()
        assertEquals(0.8f, settings.backgroundOpacity)
    }

    @Test
    fun `favoriteSongIds persist correctly`() = runTest {
        val initial = repository.favoriteSongIdsFlow.first()
        assertTrue(initial.isEmpty())

        repository.saveFavoriteSongIds(setOf(1L, 2L, 3L))
        val updated = repository.favoriteSongIdsFlow.first()
        assertEquals(setOf(1L, 2L, 3L), updated)
    }

    @Test
    fun `saveFavoriteSongIds replaces previous values`() = runTest {
        repository.saveFavoriteSongIds(setOf(10L, 20L))
        repository.saveFavoriteSongIds(setOf(30L))
        val result = repository.favoriteSongIdsFlow.first()
        assertEquals(setOf(30L), result)
    }

    @Test
    fun `custom playlists persist correctly`() = runTest {
        val playlist = Playlist(
            id = "test-id",
            name = "Test Playlist",
            songIds = setOf(100L, 200L)
        )
        repository.saveCustomPlaylists(listOf(playlist))

        val playlists = repository.customPlaylistsFlow.first()
        assertEquals(1, playlists.size)
        assertEquals("Test Playlist", playlists[0].name)
        assertEquals(setOf(100L, 200L), playlists[0].songIds)
    }

    @Test
    fun `saveCustomPlaylists replaces previous playlists`() = runTest {
        val playlist1 = Playlist(id = "p1", name = "Playlist 1", songIds = setOf(1L))
        val playlist2 = Playlist(id = "p2", name = "Playlist 2", songIds = setOf(2L, 3L))

        repository.saveCustomPlaylists(listOf(playlist1))
        repository.saveCustomPlaylists(listOf(playlist2))

        val playlists = repository.customPlaylistsFlow.first()
        assertEquals(1, playlists.size)
        assertEquals("Playlist 2", playlists[0].name)
    }

    @Test
    fun `isFirstLaunch returns true by default`() = runTest {
        assertTrue(repository.isFirstLaunch())
    }

    @Test
    fun `setFirstLaunchComplete changes isFirstLaunch to false`() = runTest {
        repository.setFirstLaunchComplete()
        assertFalse(repository.isFirstLaunch())
    }
}
