package com.example.re_pro.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.re_pro.data.database.entities.CacheMetadataEntity
import com.example.re_pro.data.model.Song
import com.example.re_pro.data.repository.HybridMusicRepository
import com.example.re_pro.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LibraryUiState(
    val songsList: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val isScanning: Boolean = false,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val favoriteSongIds: Set<Long> = emptySet(),
    val customPlaylists: List<Playlist> = emptyList(),
    val playStatsMap: Map<Long, CacheMetadataEntity> = emptyMap(),
    val groupedByAlbum: Map<String, List<Song>> = emptyMap(),
    val groupedByArtist: Map<String, List<Song>> = emptyMap(),
    val groupedByGenre: Map<String, List<Song>> = emptyMap(),
    val recentSearches: List<String> = emptyList(),
    val isCompactMode: Boolean = false,
    val excludeShortAudios: Boolean = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val hybridRepository: HybridMusicRepository,
    private val settingsRepository: SettingsRepository,
    private val cacheMetadataDao: com.example.re_pro.data.database.dao.CacheMetadataDao
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        loadPlayStats()
    }

    private var currentMinDurationMs: Long = 30000L
    private var currentExcludeShortClips: Boolean = false
    private var loadSongsJob: Job? = null
    private var lastScanCompletedAt: Long = 0L

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.userSettingsFlow.collect { settings ->
                val shouldExclude = settings.excludeShortAudios
                val durationChanged = settings.minAudioDurationMs != currentMinDurationMs
                // Si el estado de exclusión cambia, si cambió la duración mínima, o si la lista aún está vacía,
                // forzamos la recarga usando el valor del DataStore
                if (_uiState.value.songsList.isEmpty() || currentExcludeShortClips != shouldExclude || durationChanged) {
                    currentExcludeShortClips = shouldExclude
                    currentMinDurationMs = settings.minAudioDurationMs
                    loadSongs(shouldExclude)
                }
                // Sincroniza el resto de estados (compactMode, etc)
                _uiState.update {
                    it.copy(
                        isCompactMode = settings.compactList,
                        excludeShortAudios = shouldExclude
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.favoriteSongIdsFlow.collectLatest { favs ->
                val state = _uiState.value
                val processed = applyFilterAndSort(
                    songs = state.songsList,
                    query = state.searchQuery,
                    sortOption = state.sortOption,
                    filter = state.libraryFilter,
                    favorites = favs
                )
                _uiState.update { it.copy(favoriteSongIds = favs, filteredSongs = processed) }
            }
        }
        viewModelScope.launch {
            settingsRepository.customPlaylistsFlow.collectLatest { playlists ->
                _uiState.update { it.copy(customPlaylists = playlists) }
            }
        }
        viewModelScope.launch {
            settingsRepository.recentSearchesFlow.collectLatest { recentSearches ->
                _uiState.update { it.copy(recentSearches = recentSearches) }
            }
        }
    }

    fun loadSongs(excludeShortClips: Boolean = false) {
        if (loadSongsJob?.isActive == true) return
        loadSongsJob = viewModelScope.launch {
            performScan(excludeShortClips)
        }
    }

    fun refreshSongs() {
        if (loadSongsJob?.isActive == true) return
        loadSongsJob = viewModelScope.launch {
            val settings = settingsRepository.userSettingsFlow.first()
            currentExcludeShortClips = settings.excludeShortAudios
            currentMinDurationMs = settings.minAudioDurationMs
            performScan(settings.excludeShortAudios)
        }
    }

    fun refreshOnForeground() {
        if (loadSongsJob?.isActive == true) return
        val now = System.currentTimeMillis()
        val isEmpty = _uiState.value.songsList.isEmpty()
        if (isEmpty || now - lastScanCompletedAt > 60_000L) {
            refreshSongs()
        }
    }

    private suspend fun performScan(excludeShortClips: Boolean) {
        _uiState.update { it.copy(isScanning = true) }
        val rawSongs = hybridRepository.fetchLocalSongs(excludeShortClips)
        val minDuration = currentMinDurationMs
        val filteredByDuration = if (excludeShortClips && minDuration > 0) {
            rawSongs.filter { it.duration >= minDuration }
        } else {
            rawSongs
        }
        val state = _uiState.value
        val processed = withContext(Dispatchers.Default) {
            applyFilterAndSort(
                songs = filteredByDuration,
                query = state.searchQuery,
                sortOption = state.sortOption,
                filter = state.libraryFilter,
                favorites = state.favoriteSongIds
            )
        }
        lastScanCompletedAt = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                songsList = filteredByDuration,
                filteredSongs = processed,
                isScanning = false
            )
        }
    }

    fun search(query: String) {
        val processed = applyFilterAndSort(
            songs = _uiState.value.songsList,
            query = query,
            sortOption = _uiState.value.sortOption,
            filter = _uiState.value.libraryFilter,
            favorites = _uiState.value.favoriteSongIds
        )
        _uiState.update { it.copy(searchQuery = query, filteredSongs = processed) }
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                settingsRepository.addRecentSearch(trimmed)
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            settingsRepository.clearRecentSearches()
        }
    }

    fun setSortOption(sortOption: SortOption) {
        val processed = applyFilterAndSort(
            songs = _uiState.value.songsList,
            query = _uiState.value.searchQuery,
            sortOption = sortOption,
            filter = _uiState.value.libraryFilter,
            favorites = _uiState.value.favoriteSongIds
        )
        _uiState.update { it.copy(sortOption = sortOption, filteredSongs = processed) }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        val processed = applyFilterAndSort(
            songs = _uiState.value.songsList,
            query = _uiState.value.searchQuery,
            sortOption = _uiState.value.sortOption,
            filter = filter,
            favorites = _uiState.value.favoriteSongIds
        )
        _uiState.update { it.copy(libraryFilter = filter, filteredSongs = processed) }
    }

    fun setCompactMode(compact: Boolean) {
        viewModelScope.launch { settingsRepository.setCompactList(compact) }
    }

    fun toggleFavorite(songId: Long) {
        val currentFavorites = _uiState.value.favoriteSongIds.toMutableSet()
        if (currentFavorites.contains(songId)) {
            currentFavorites.remove(songId)
        } else {
            currentFavorites.add(songId)
        }
        val processed = applyFilterAndSort(
            songs = _uiState.value.songsList,
            query = _uiState.value.searchQuery,
            sortOption = _uiState.value.sortOption,
            filter = _uiState.value.libraryFilter,
            favorites = currentFavorites
        )
        _uiState.update {
            it.copy(
                favoriteSongIds = currentFavorites,
                filteredSongs = processed
            )
        }
        viewModelScope.launch {
            settingsRepository.saveFavoriteSongIds(currentFavorites)
        }
    }

    fun deleteSongFromDevice(song: Song, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.delete(song.uri, null, null)
                refreshSongs()
            } catch (e: Exception) {
                val newList = _uiState.value.songsList.filter { it.id != song.id }
                val filtered = applyFilterAndSort(
                    newList, _uiState.value.searchQuery, _uiState.value.sortOption,
                    _uiState.value.libraryFilter, _uiState.value.favoriteSongIds
                )
                _uiState.update { it.copy(songsList = newList, filteredSongs = filtered) }
            }
        }
    }

    /**
     * Ruta de borrado para Android < 11 o como respaldo si falla
     * MediaStore.createDeleteRequest. Elimina los índices de MediaStore
     * y refresca la biblioteca.
     */
    fun deleteSongsFromDeviceFallback(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            var deletedAny = false
            withContext(Dispatchers.IO) {
                songs.forEach { song ->
                    try {
                        getApplication<Application>().contentResolver.delete(song.uri, null, null)
                        deletedAny = true
                    } catch (_: Exception) {
                    }
                }
            }
            if (deletedAny) refreshSongs()
        }
    }

    fun createPlaylist(name: String) = createPlaylistWithSongs(name, emptySet())

    fun createPlaylistWithSongs(name: String, songIds: Set<Long>) {
        if (name.isBlank()) return
        val currentPlaylists = _uiState.value.customPlaylists.toMutableList()
        val newPlaylist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim(),
            songIds = songIds
        )
        currentPlaylists.add(newPlaylist)
        _uiState.update { it.copy(customPlaylists = currentPlaylists) }
        viewModelScope.launch {
            settingsRepository.saveCustomPlaylists(currentPlaylists)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: Long) {
        val currentPlaylists = _uiState.value.customPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds + songId)
            } else playlist
        }
        _uiState.update { it.copy(customPlaylists = currentPlaylists) }
        viewModelScope.launch {
            settingsRepository.saveCustomPlaylists(currentPlaylists)
        }
    }

    fun addSongsToPlaylist(playlistId: String, songIds: Set<Long>) {
        val currentPlaylists = _uiState.value.customPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds + songIds)
            } else playlist
        }
        _uiState.update { it.copy(customPlaylists = currentPlaylists) }
        viewModelScope.launch {
            settingsRepository.saveCustomPlaylists(currentPlaylists)
        }
    }

    fun addSongsToImportedPlaylist(songIds: Set<Long>) {
        if (songIds.isEmpty()) return
        val name = "Importadas (Spotify)"
        val existing = _uiState.value.customPlaylists.firstOrNull { it.name == name }
        if (existing != null) addSongsToPlaylist(existing.id, songIds)
        else createPlaylistWithSongs(name, songIds)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val currentPlaylists = _uiState.value.customPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds - songId)
            } else playlist
        }
        _uiState.update { it.copy(customPlaylists = currentPlaylists) }
        viewModelScope.launch {
            settingsRepository.saveCustomPlaylists(currentPlaylists)
        }
    }

    fun deletePlaylist(playlistId: String) {
        val currentPlaylists = _uiState.value.customPlaylists.filter { it.id != playlistId }
        _uiState.update { it.copy(customPlaylists = currentPlaylists) }
        viewModelScope.launch {
            settingsRepository.saveCustomPlaylists(currentPlaylists)
        }
    }

    suspend fun switchToOfflineMode() {
        val localSongs = hybridRepository.fetchLocalSongs()
        _uiState.update { it.copy(songsList = localSongs, filteredSongs = localSongs) }
    }

    suspend fun switchToOnlineMode() {
        val source = hybridRepository.getPlaybackSource(preferLocal = false, forceRemote = true)
        when (source) {
            is HybridMusicRepository.PlaybackSource.Remote -> {
                _uiState.update { it.copy(songsList = source.songs, filteredSongs = source.songs) }
            }
            else -> switchToOfflineMode()
        }
    }

    suspend fun getOfflineQueue(): List<Song> = hybridRepository.getOfflineQueue()

    fun getHybridRepository(): HybridMusicRepository = hybridRepository

    suspend fun batchCacheSongs(songs: List<Song>) = hybridRepository.batchCacheSongs(songs)

    fun loadPlayStats() {
        viewModelScope.launch {
            hybridRepository.getAllCachedMetadata().collect { statsList ->
                val statsMap = statsList.associateBy { it.songId }
                _uiState.update { state ->
                    state.copy(
                        playStatsMap = statsMap,
                        groupedByAlbum = state.songsList.groupBy { it.album },
                        groupedByArtist = state.songsList.groupBy { it.artist },
                        groupedByGenre = state.songsList.groupBy { it.genre }
                    )
                }
            }
        }
    }

    fun incrementPlayCount(songId: Long) {
        viewModelScope.launch {
            cacheMetadataDao.incrementPlayCount(songId)
        }
    }

    private fun applyFilterAndSort(
        songs: List<Song>,
        query: String,
        sortOption: SortOption,
        filter: LibraryFilter = LibraryFilter.ALL,
        favorites: Set<Long> = emptySet()
    ): List<Song> {
        var result = if (filter == LibraryFilter.FAVORITES) {
            songs.filter { favorites.contains(it.id) }
        } else {
            songs
        }

        if (query.isNotBlank()) {
            result = result.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true) ||
                        song.album.contains(query, ignoreCase = true)
            }
        }

        return when (sortOption) {
            SortOption.DATE_ADDED_DESC -> result.sortedByDescending { it.dateAdded }
            SortOption.TITLE_ASC -> result.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> result.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> result.sortedBy { it.artist.lowercase() }
            SortOption.DURATION_DESC -> result.sortedByDescending { it.duration }
            SortOption.PLAY_COUNT_DESC -> {
                val statsMap = uiState.value.playStatsMap
                result.sortedByDescending { statsMap[it.id]?.playCount ?: 0 }
            }
            SortOption.LAST_PLAYED -> {
                val statsMap = uiState.value.playStatsMap
                result.sortedByDescending { statsMap[it.id]?.lastPlayed ?: 0 }
            }
            SortOption.ALBUM_ASC -> result.sortedBy { it.album }
            SortOption.GENRE_ASC -> result.sortedBy { it.genre }
        }
    }
}
