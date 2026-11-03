package com.example.re_pro.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.data.model.UserSettings
import com.example.re_pro.data.model.EqualizerPreset
import com.example.re_pro.viewmodel.Playlist
import com.example.re_pro.viewmodel.AlbumArtMode
import com.example.re_pro.viewmodel.NowPlayingAnimation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "repro_user_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val EXCLUDE_SHORT_CLIPS = booleanPreferencesKey("exclude_short_clips")
        val MIN_AUDIO_DURATION_MS = longPreferencesKey("min_audio_duration_ms")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_BLUR = floatPreferencesKey("background_blur")
        val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val COMPACT_LIST = booleanPreferencesKey("compact_list")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val FAVORITE_SONG_IDS = stringSetPreferencesKey("favorite_song_ids")
        val CUSTOM_PLAYLISTS_JSON = stringPreferencesKey("custom_playlists_json")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val REPLAY_GAIN_ENABLED = booleanPreferencesKey("replay_gain_enabled")
        val REPLAY_GAIN_PREAMP = floatPreferencesKey("replay_gain_preamp")
        val EQUALIZER_PRESETS_JSON = stringPreferencesKey("equalizer_presets_json")
        val ACTIVE_EQUALIZER_PRESET_ID = stringPreferencesKey("active_equalizer_preset_id")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")
        val ALBUM_ART_MODE = stringPreferencesKey("album_art_mode")
        val NOW_PLAYING_ANIMATION = stringPreferencesKey("now_playing_animation")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val excludeClips = preferences[PreferencesKeys.EXCLUDE_SHORT_CLIPS] ?: true
        val minDurationMs = preferences[PreferencesKeys.MIN_AUDIO_DURATION_MS] ?: 30000L
        val themeString = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.AMOLED_PITCH_BLACK.name
        val themeMode = try {
            AppThemeMode.valueOf(themeString)
        } catch (e: Exception) {
            AppThemeMode.AMOLED_PITCH_BLACK
        }
        val bgUri = preferences[PreferencesKeys.BACKGROUND_IMAGE_URI]
        val bgBlur = preferences[PreferencesKeys.BACKGROUND_BLUR] ?: 20f
        val bgOpacity = preferences[PreferencesKeys.BACKGROUND_OPACITY] ?: 0.5f
        val gridCols = preferences[PreferencesKeys.GRID_COLUMNS] ?: 2
        val compactList = preferences[PreferencesKeys.COMPACT_LIST] ?: false
        val crossfade = preferences[PreferencesKeys.CROSSFADE_SECONDS] ?: 0
        val replayGainEnabled = preferences[PreferencesKeys.REPLAY_GAIN_ENABLED] ?: false
        val replayGainPreamp = preferences[PreferencesKeys.REPLAY_GAIN_PREAMP] ?: 1.0f
        val eqEnabled = preferences[PreferencesKeys.EQUALIZER_ENABLED] ?: true
        val albumArtModeString = preferences[PreferencesKeys.ALBUM_ART_MODE] ?: AlbumArtMode.STATIC.name
        val albumArtMode = try {
            AlbumArtMode.valueOf(albumArtModeString)
        } catch (e: Exception) {
            AlbumArtMode.STATIC
        }
        val nowPlayingAnimationString = preferences[PreferencesKeys.NOW_PLAYING_ANIMATION] ?: NowPlayingAnimation.BARS.name
        val nowPlayingAnimation = try {
            NowPlayingAnimation.valueOf(nowPlayingAnimationString)
        } catch (e: Exception) {
            NowPlayingAnimation.BARS
        }

        UserSettings(
            excludeShortAudios = excludeClips,
            minAudioDurationMs = minDurationMs,
            themeMode = themeMode,
            backgroundImageUri = bgUri,
            backgroundBlur = bgBlur,
            backgroundOpacity = bgOpacity,
            gridColumns = gridCols,
            compactList = compactList,
            crossfadeSeconds = crossfade,
            replayGainEnabled = replayGainEnabled,
            replayGainPreamp = replayGainPreamp,
            equalizerEnabled = eqEnabled,
            albumArtMode = albumArtMode,
            nowPlayingAnimation = nowPlayingAnimation
        )
    }

    val favoriteSongIdsFlow: Flow<Set<Long>> = context.dataStore.data.map { preferences ->
        val rawSet = preferences[PreferencesKeys.FAVORITE_SONG_IDS] ?: emptySet()
        rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    val customPlaylistsFlow: Flow<List<Playlist>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[PreferencesKeys.CUSTOM_PLAYLISTS_JSON] ?: ""
        jsonToPlaylists(jsonStr)
    }

    val recentSearchesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[PreferencesKeys.RECENT_SEARCHES] ?: ""
        if (jsonStr.isBlank()) {
            emptyList()
        } else {
            runCatching {
                val array = JSONArray(jsonStr)
                List(array.length()) { array.getString(it) }
            }.getOrDefault(emptyList())
        }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = recentSearchesFlow.first().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(10)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECENT_SEARCHES] = JSONArray(capped).toString()
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.RECENT_SEARCHES)
        }
    }

    suspend fun setExcludeShortClips(exclude: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXCLUDE_SHORT_CLIPS] = exclude
        }
    }

    suspend fun setMinAudioDurationMs(durationMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIN_AUDIO_DURATION_MS] = durationMs
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setBackgroundImageUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.BACKGROUND_IMAGE_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.BACKGROUND_IMAGE_URI)
            }
        }
    }

    suspend fun setBackgroundBlur(blur: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_BLUR] = blur
        }
    }

    suspend fun setBackgroundOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_OPACITY] = opacity
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    suspend fun setCompactList(compact: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPACT_LIST] = compact
        }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CROSSFADE_SECONDS] = seconds
        }
    }

    suspend fun setReplayGain(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLAY_GAIN_ENABLED] = enabled
        }
    }

    suspend fun setReplayGainPreamp(preamp: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLAY_GAIN_PREAMP] = preamp
        }
    }

    suspend fun saveFavoriteSongIds(favorites: Set<Long>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_SONG_IDS] = favorites.map { it.toString() }.toSet()
        }
    }

    suspend fun saveCustomPlaylists(playlists: List<Playlist>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_PLAYLISTS_JSON] = playlistsToJson(playlists)
        }
    }

    private fun playlistsToJson(playlists: List<Playlist>): String {
        val jsonArray = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject()
            obj.put("id", playlist.id)
            obj.put("name", playlist.name)
            val idsArray = JSONArray()
            playlist.songIds.forEach { idsArray.put(it) }
            obj.put("songIds", idsArray)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun jsonToPlaylists(jsonStr: String): List<Playlist> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<Playlist>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val idsArray = obj.getJSONArray("songIds")
                val songIds = mutableSetOf<Long>()
                for (j in 0 until idsArray.length()) {
                    songIds.add(idsArray.getLong(j))
                }
                list.add(Playlist(id, name, songIds))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    suspend fun isFirstLaunch(): Boolean {
        return context.dataStore.data.first()[PreferencesKeys.FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
        }
    }

    val equalizerPresetsFlow: Flow<List<EqualizerPreset>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[PreferencesKeys.EQUALIZER_PRESETS_JSON] ?: ""
        jsonToEqualizerPresets(jsonStr)
    }

    val activeEqualizerPresetIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_EQUALIZER_PRESET_ID] ?: "builtin_normal"
    }

    suspend fun saveEqualizerPreset(preset: EqualizerPreset) {
        val current = equalizerPresetsFlow.first().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == preset.id }
        if (existingIndex >= 0) {
            current[existingIndex] = preset
        } else {
            current.add(preset)
        }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EQUALIZER_PRESETS_JSON] = equalizerPresetsToJson(current)
        }
    }

    suspend fun deleteEqualizerPreset(presetId: String) {
        val current = equalizerPresetsFlow.first().toMutableList()
        val filtered = current.filter { it.id != presetId }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EQUALIZER_PRESETS_JSON] = equalizerPresetsToJson(filtered)
            if (preferences[PreferencesKeys.ACTIVE_EQUALIZER_PRESET_ID] == presetId) {
                preferences[PreferencesKeys.ACTIVE_EQUALIZER_PRESET_ID] = "builtin_normal"
            }
        }
    }

    suspend fun setActiveEqualizerPreset(presetId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_EQUALIZER_PRESET_ID] = presetId
        }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EQUALIZER_ENABLED] = enabled
        }
    }

    suspend fun setAlbumArtMode(mode: AlbumArtMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALBUM_ART_MODE] = mode.name
        }
    }

    suspend fun setNowPlayingAnimation(mode: NowPlayingAnimation) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOW_PLAYING_ANIMATION] = mode.name
        }
    }

    private fun equalizerPresetsToJson(presets: List<EqualizerPreset>): String {
        val jsonArray = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject()
            obj.put("id", preset.id)
            obj.put("name", preset.name)
            val bandsArray = JSONArray()
            preset.bandLevels.forEach { bandsArray.put(it) }
            obj.put("bandLevels", bandsArray)
            obj.put("bassBoost", preset.bassBoost)
            obj.put("virtualizer", preset.virtualizer)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun jsonToEqualizerPresets(jsonStr: String): List<EqualizerPreset> {
        if (jsonStr.isBlank()) return EqualizerPreset.BUILTIN_PRESETS
        val list = mutableListOf<EqualizerPreset>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val bandsArray = obj.getJSONArray("bandLevels")
                val bandLevels = mutableListOf<Int>()
                for (j in 0 until bandsArray.length()) {
                    bandLevels.add(bandsArray.getInt(j))
                }
                val bassBoost = obj.optInt("bassBoost", 0)
                val virtualizer = obj.optInt("virtualizer", 0)
                list.add(EqualizerPreset(id, name, bandLevels, bassBoost, virtualizer))
            }
        } catch (_: Exception) {}
        return if (list.isEmpty()) EqualizerPreset.BUILTIN_PRESETS else list
    }

    fun getAllSettingsJson(): JSONObject {
        val settings = runBlocking { userSettingsFlow.first() }
        val favorites = runBlocking { favoriteSongIdsFlow.first() }
        val playlists = runBlocking { customPlaylistsFlow.first() }
        val presets = runBlocking { equalizerPresetsFlow.first() }

        return JSONObject().apply {
            put("excludeShortClips", settings.excludeShortAudios)
            put("themeMode", settings.themeMode.name)
            put("backgroundBlur", settings.backgroundBlur.toDouble())
            put("backgroundOpacity", settings.backgroundOpacity.toDouble())
            put("gridColumns", settings.gridColumns)
            put("compactList", settings.compactList)
            put("crossfadeSeconds", settings.crossfadeSeconds)
            put("replayGainEnabled", settings.replayGainEnabled)
            put("replayGainPreamp", settings.replayGainPreamp.toDouble())
            put("albumArtMode", settings.albumArtMode.name)
            put("nowPlayingAnimation", settings.nowPlayingAnimation.name)
            put("favoriteSongIds", JSONArray(favorites.map { it }))
            put("customPlaylists", JSONArray().apply {
                playlists.forEach { pl ->
                    put(JSONObject().apply {
                        put("id", pl.id)
                        put("name", pl.name)
                        put("songIds", JSONArray(pl.songIds.map { it }))
                    })
                }
            })
            put("equalizerPresets", JSONArray().apply {
                presets.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("bandLevels", JSONArray(p.bandLevels.map { it }))
                        put("bassBoost", p.bassBoost)
                        put("virtualizer", p.virtualizer)
                    })
                }
            })
        }
    }

    suspend fun importSettingsFromJson(json: JSONObject) {
        context.dataStore.edit { preferences ->
            if (json.has("themeMode")) preferences[PreferencesKeys.THEME_MODE] = json.getString("themeMode")
            if (json.has("backgroundBlur")) preferences[PreferencesKeys.BACKGROUND_BLUR] = json.getDouble("backgroundBlur").toFloat()
            if (json.has("backgroundOpacity")) preferences[PreferencesKeys.BACKGROUND_OPACITY] = json.getDouble("backgroundOpacity").toFloat()
            if (json.has("gridColumns")) preferences[PreferencesKeys.GRID_COLUMNS] = json.getInt("gridColumns")
            if (json.has("compactList")) preferences[PreferencesKeys.COMPACT_LIST] = json.getBoolean("compactList")
            if (json.has("crossfadeSeconds")) preferences[PreferencesKeys.CROSSFADE_SECONDS] = json.getInt("crossfadeSeconds")
            if (json.has("replayGainEnabled")) preferences[PreferencesKeys.REPLAY_GAIN_ENABLED] = json.getBoolean("replayGainEnabled")
            if (json.has("replayGainPreamp")) preferences[PreferencesKeys.REPLAY_GAIN_PREAMP] = json.getDouble("replayGainPreamp").toFloat()
            if (json.has("excludeShortClips")) preferences[PreferencesKeys.EXCLUDE_SHORT_CLIPS] = json.getBoolean("excludeShortClips")
            if (json.has("albumArtMode")) preferences[PreferencesKeys.ALBUM_ART_MODE] = json.getString("albumArtMode")
            if (json.has("nowPlayingAnimation")) preferences[PreferencesKeys.NOW_PLAYING_ANIMATION] = json.getString("nowPlayingAnimation")
        }

        if (json.has("favoriteSongIds")) {
            val favArray = json.getJSONArray("favoriteSongIds")
            val favSet = mutableSetOf<String>()
            for (i in 0 until favArray.length()) {
                favSet.add(favArray.getLong(i).toString())
            }
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FAVORITE_SONG_IDS] = favSet
            }
        }

        if (json.has("customPlaylists")) {
            val playlistsJson = json.getJSONArray("customPlaylists").toString()
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.CUSTOM_PLAYLISTS_JSON] = playlistsJson
            }
        }

        if (json.has("equalizerPresets")) {
            val presetsJson = json.getJSONArray("equalizerPresets").toString()
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.EQUALIZER_PRESETS_JSON] = presetsJson
            }
        }
    }
}
