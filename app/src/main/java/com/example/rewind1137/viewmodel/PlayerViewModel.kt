package com.example.rewind1137.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.rewind1137.audio.AudioEffectManager
import com.example.rewind1137.data.model.EqualizerPreset
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.data.model.UserSettings
import com.example.rewind1137.service.MusicService
import com.example.rewind1137.utils.MusicPlayerManager
import com.example.rewind1137.utils.SleepTimerManager
import com.example.rewind1137.utils.SleepTimerOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

data class PlayerUiState(
    val currentSong: Song? = null,
    val currentSongIndex: Int = -1,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val playbackQueue: List<Song> = emptyList(),
    val displayQueue: List<Song> = emptyList(),
    val isEqualizerEnabled: Boolean = false,
    val isBassBoostEnabled: Boolean = false,
    val isVisualizerActive: Boolean = false,
    val audioSessionId: Int = 0,
    val isPlayerSheetExpanded: Boolean = false,
    val playerSheetAnchor: Rect? = null,
    val isQueueVisible: Boolean = false,
    val lyricsRaw: String = ""
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val audioEffectManager: AudioEffectManager,
    private val playbackStateHolder: PlaybackStateHolder,
    private val settingsRepository: com.example.rewind1137.data.repository.SettingsRepository,
    private val cacheMetadataDao: com.example.rewind1137.data.database.dao.CacheMetadataDao,
    private val lyricsRepository: com.example.rewind1137.data.repository.LyricsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val queueManager = QueueManager()
    val queuesState: StateFlow<QueuesState> = queueManager.queuesState

    var exoPlayer: ExoPlayer = MusicPlayerManager.getPlayer(application)
        private set

    private var isAudioEffectsInitialized = false
    private var isUserInitiatedTransition = false
    private var currentSongPlaylist: List<Song> = emptyList()
    private var unshuffledQueue: List<Song> = emptyList()
    private var activeEqPreset: EqualizerPreset? = null
    private var loadQueueJob: Job? = null
    private var positionTrackerJob: Job? = null

    init {
        initPlayerListeners()
        startPositionTracker()
        initCrossfadeFromSettings()
        initReplayGainFromSettings()
        initAudioEffects()
    }

    private fun initCrossfadeFromSettings() {
        viewModelScope.launch {
            settingsRepository.userSettingsFlow.first().let { settings ->
                MusicPlayerManager.setCrossfadeDuration(settings.crossfadeSeconds)
            }
        }
    }

    private fun initReplayGainFromSettings() {
        viewModelScope.launch {
            settingsRepository.userSettingsFlow.first().let { settings ->
                MusicPlayerManager.setReplayGain(settings.replayGainEnabled, settings.replayGainPreamp)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionTracker() else positionTrackerJob?.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            durationMs = dur,
                            audioSessionId = exoPlayer.audioSessionId
                        )
                    }
                    audioEffectManager.onAudioSessionIdChanged(activeEqPreset)
                } else if (playbackState == Player.STATE_ENDED) {
                    if (SleepTimerManager.activeOption == SleepTimerOption.END_OF_TRACK) {
                        SleepTimerManager.stopAfterCurrentTrackFinished()
                        return
                    }
                    onTrackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (SleepTimerManager.activeOption == SleepTimerOption.END_OF_TRACK &&
                    !isUserInitiatedTransition
                ) {
                    SleepTimerManager.stopAfterCurrentTrackFinished()
                }
                isUserInitiatedTransition = false

                val newIndex = exoPlayer.currentMediaItemIndex
                val song = currentSongPlaylist.getOrNull(newIndex)
                if (song != null) {
                    _uiState.update {
                        it.copy(
                            currentSong = song,
                            currentSongIndex = newIndex,
                            currentPositionMs = 0L,
                            durationMs = song.duration,
                            lyricsRaw = ""
                        )
                    }
                    queueManager.setCurrentIndex(newIndex)
                    playbackStateHolder.update { state ->
                        state.copy(
                            filteredSongs = currentSongPlaylist.ifEmpty { state.filteredSongs },
                            currentSongId = song.id,
                            currentSongIndex = newIndex,
                            durationMs = song.duration,
                            currentPositionMs = 0L
                        )
                    }
                    recordSongPlay(song)
                    loadLyrics(song)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffectManager.onAudioSessionIdChanged(activeEqPreset)
            }
        })
        _uiState.update { it.copy(audioSessionId = exoPlayer.audioSessionId) }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        if (_uiState.value.isPlaying) {
            positionTrackerJob = viewModelScope.launch {
                while (true) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _uiState.update { it.copy(currentPositionMs = pos) }
                    playbackStateHolder.update { it.copy(currentPositionMs = pos) }
                    delay(100)
                }
            }
        }
    }

    private fun buildPlaybackSequence(mode: PlaybackMode, songs: List<Song>, startIndex: Int): List<Song> {
        if (songs.isEmpty()) return emptyList()
        val index = startIndex.coerceIn(0, songs.size - 1)
        return when (mode) {
            PlaybackMode.SEQUENTIAL, PlaybackMode.REPEAT_ONE -> {
                songs.subList(index, songs.size) + songs.subList(0, index)
            }
            PlaybackMode.SHUFFLE -> {
                val current = songs[index]
                listOf(current) + songs.filter { it.id != current.id }.shuffled()
            }
        }
    }

    fun playSongAtIndex(index: Int, songs: List<Song>) {
        if (songs.isEmpty()) return
        val song = songs.getOrNull(index.coerceIn(0, songs.size - 1)) ?: return
        unshuffledQueue = songs
        val sequence = buildPlaybackSequence(_uiState.value.playbackMode, songs, index)

        currentSongPlaylist = sequence
        queueManager.setCurrentIndex(0)
        queueManager.setQueue(sequence)

        isUserInitiatedTransition = true

        loadQueueJob?.cancel()

        val initialBatchSize = 20
        val initialBatch = sequence.take(initialBatchSize)
        val initialMediaItems = initialBatch.map { songToMediaItem(it) }

        exoPlayer.apply {
            stop()
            clearMediaItems()
            setMediaItems(initialMediaItems, 0, 0L)
            prepare()
            play()
            applyRepeatModeToPlayer()
        }

        if (sequence.size > initialBatchSize) {
            val remainingItems = sequence.drop(initialBatchSize)
            loadQueueJob = viewModelScope.launch(Dispatchers.Default) {
                val remainingMediaItems = remainingItems.map { songToMediaItem(it) }
                withContext(Dispatchers.Main) {
                    exoPlayer.addMediaItems(remainingMediaItems)
                }
            }
        }

        _uiState.update {
            it.copy(
                currentSong = song,
                currentSongIndex = 0,
                currentPositionMs = 0L,
                durationMs = song.duration,
                playbackQueue = sequence,
                displayQueue = sequence
            )
        }
        startMusicService()
        loadLyrics(song)
    }

    private fun songToMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(song.albumArtUri)
                    .build()
            )
            .build()

    fun playNext(song: Song) {
        val state = _uiState.value
        val queue = state.displayQueue.toMutableList()
        val insertIndex = (state.currentSongIndex + 1).coerceIn(0, queue.size)
        queue.add(insertIndex, song)
        currentSongPlaylist = queue
        exoPlayer.addMediaItem(exoPlayer.currentMediaItemIndex + 1, songToMediaItem(song))
        _uiState.update { it.copy(playbackQueue = queue, displayQueue = queue) }
        queueManager.setQueue(queue)
        queueManager.setCurrentIndex(state.currentSongIndex)
    }

    fun playLast(song: Song) {
        val state = _uiState.value
        val queue = state.displayQueue.toMutableList()
        queue.add(song)
        currentSongPlaylist = queue
        exoPlayer.addMediaItem(exoPlayer.mediaItemCount, songToMediaItem(song))
        _uiState.update { it.copy(playbackQueue = queue, displayQueue = queue) }
        queueManager.setQueue(queue)
        queueManager.setCurrentIndex(state.currentSongIndex)
    }

    private var lyricsJob: Job? = null

    fun loadLyrics(song: Song) {
        lyricsJob?.cancel()
        _uiState.update { it.copy(lyricsRaw = "") }
        lyricsJob = viewModelScope.launch {
            val lyrics = runCatching { lyricsRepository.getLyrics(song) }.getOrDefault("")
            val current = _uiState.value.currentSong
            val stillPlaying = current != null &&
                (if (song.id > 0L && current.id > 0L) current.id == song.id else current.uri == song.uri)
            if (stillPlaying) {
                _uiState.update { it.copy(lyricsRaw = lyrics) }
            }
        }
    }

    private fun recordSongPlay(song: Song) {
        viewModelScope.launch {
            runCatching {
                cacheMetadataDao.recordPlay(
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
            }
        }
    }

    private fun startMusicService() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun setPlayerVolume(volume: Float) {
        MusicPlayerManager.setVolume(volume)
    }

    fun togglePlayPause() = MusicPlayerManager.togglePlayPause()

    fun seekTo(positionMs: Long) {
        _uiState.update { it.copy(currentPositionMs = positionMs) }
        exoPlayer.seekTo(positionMs)
    }

    fun skipNext() {
        val queue = _uiState.value.displayQueue
        if (queue.isEmpty()) return
        val currentIndex = _uiState.value.currentSongIndex
        val nextIndex = when (_uiState.value.playbackMode) {
            PlaybackMode.SHUFFLE -> if (queue.size > 1) Random.nextInt(queue.size) else 0
            PlaybackMode.SEQUENTIAL, PlaybackMode.REPEAT_ONE -> {
                if (currentIndex in queue.indices) (currentIndex + 1) % queue.size else 0
            }
        }
        playSongAtIndex(nextIndex, queue)
    }

    fun skipPrevious() {
        val queue = _uiState.value.displayQueue
        if (queue.isEmpty()) return
        val currentIndex = _uiState.value.currentSongIndex
        val prevIndex = when (_uiState.value.playbackMode) {
            PlaybackMode.SHUFFLE -> if (queue.size > 1) Random.nextInt(queue.size) else 0
            PlaybackMode.SEQUENTIAL, PlaybackMode.REPEAT_ONE -> {
                if (currentIndex in queue.indices) (currentIndex - 1 + queue.size) % queue.size else 0
            }
        }
        playSongAtIndex(prevIndex, queue)
    }

    fun switchToQueue(queueId: String) {
        queueManager.switchQueue(queueId)
        val newQueue = queueManager.getActiveQueue()
        if (newQueue.songs.isNotEmpty()) {
            playSongAtIndex(newQueue.currentIndex.coerceIn(0, newQueue.songs.size - 1), newQueue.songs)
        }
    }

    fun createQueue(name: String) {
        queueManager.createQueue(name)
    }

    fun deleteQueue(queueId: String) {
        queueManager.deleteQueue(queueId)
    }

    fun addSongToCurrentQueue(song: Song) {
        queueManager.addSongToQueue(song)
        updateQueue(queueManager.getActiveSongs())
    }

    fun removeSongFromQueue(songId: Long) {
        queueManager.removeSongFromQueue(songId)
        updateQueue(queueManager.getActiveSongs())
    }

    fun shareCurrentSong() {
        val song = _uiState.value.currentSong
        song?.let { s ->
            val context = getApplication<Application>()
            val mime = when {
                s.uri.path?.endsWith(".mp3", true) == true -> "audio/mpeg"
                s.uri.path?.endsWith(".m4a", true) == true || s.uri.path?.endsWith(".aac", true) == true -> "audio/mp4"
                s.uri.path?.endsWith(".flac", true) == true -> "audio/flac"
                s.uri.path?.endsWith(".ogg", true) == true || s.uri.path?.endsWith(".opus", true) == true -> "audio/ogg"
                s.uri.path?.endsWith(".wav", true) == true -> "audio/wav"
                s.uri.path?.endsWith(".webm", true) == true -> "audio/webm"
                else -> "audio/*"
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                clipData = ClipData.newUri(context.contentResolver, s.title, s.uri)
                putExtra(Intent.EXTRA_STREAM, s.uri)
                putExtra(Intent.EXTRA_SUBJECT, s.title)
                putExtra(Intent.EXTRA_TEXT, "Escuchando \"${s.title}\" de ${s.artist} en Rewind 1137")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Compartir canción")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun updateCurrentSongMetadata(updated: Song) {
        val current = _uiState.value.currentSong
        if (current == null || current.id != updated.id) return
        _uiState.update { it.copy(currentSong = updated) }
    }

    fun togglePlaybackMode() {
        val nextMode = when (_uiState.value.playbackMode) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SEQUENTIAL
        }
        val currentSong = _uiState.value.currentSong
        val source = if (unshuffledQueue.isNotEmpty()) unshuffledQueue else _uiState.value.displayQueue
        if (unshuffledQueue.isEmpty()) {
            unshuffledQueue = source
        }

        val newQueue = when (nextMode) {
            PlaybackMode.SHUFFLE -> {
                if (currentSong != null) {
                    listOf(currentSong) + source.filter { it.id != currentSong.id }.shuffled()
                } else {
                    source.shuffled()
                }
            }
            else -> unshuffledQueue
        }

        val newIndex = if (currentSong != null) newQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0) else 0

        currentSongPlaylist = newQueue
        queueManager.setQueue(newQueue)
        queueManager.setCurrentIndex(newIndex)

        _uiState.update {
            it.copy(
                playbackMode = nextMode,
                displayQueue = newQueue,
                playbackQueue = newQueue,
                currentSongIndex = newIndex
            )
        }
        applyRepeatModeToPlayer(nextMode)
    }

    fun clearDisplayQueue() {
        val currentSong = _uiState.value.currentSong
        val newQueue = if (currentSong != null) listOf(currentSong) else emptyList()
        unshuffledQueue = newQueue
        currentSongPlaylist = newQueue
        queueManager.setQueue(newQueue)
        queueManager.setCurrentIndex(0)

        if (newQueue.isNotEmpty()) {
            val mediaItems = newQueue.map { songToMediaItem(it) }
            val currentPos = exoPlayer.currentPosition
            val isPlaying = exoPlayer.isPlaying
            exoPlayer.setMediaItems(mediaItems, 0, currentPos)
            if (isPlaying) exoPlayer.play()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }

        _uiState.update {
            it.copy(
                displayQueue = newQueue,
                playbackQueue = newQueue,
                currentSongIndex = 0
            )
        }
    }

    private fun applyRepeatModeToPlayer(mode: PlaybackMode = _uiState.value.playbackMode) {
        exoPlayer.repeatMode = when (mode) {
            PlaybackMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
            PlaybackMode.SEQUENTIAL, PlaybackMode.SHUFFLE -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.shuffleModeEnabled = (mode == PlaybackMode.SHUFFLE)
    }

    fun updateQueue(songs: List<Song>) {
        val state = _uiState.value
        if (songs.isEmpty()) {
            _uiState.update { it.copy(playbackQueue = emptyList(), displayQueue = emptyList()) }
            return
        }
        queueManager.setQueue(songs)
        val queue = buildPlaybackSequence(state.playbackMode, songs, state.currentSongIndex)
        _uiState.update { it.copy(playbackQueue = queue, displayQueue = queue) }
    }

    fun reorderDisplayQueue(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.displayQueue.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            currentSongPlaylist = list

            if (fromIndex in 0 until exoPlayer.mediaItemCount && toIndex in 0 until exoPlayer.mediaItemCount) {
                exoPlayer.moveMediaItem(fromIndex, toIndex)
            }

            val currentDisplayIndex = _uiState.value.currentSongIndex
            val newDisplayIndex = when {
                currentDisplayIndex == fromIndex -> toIndex
                fromIndex < currentDisplayIndex && toIndex >= currentDisplayIndex -> currentDisplayIndex - 1
                fromIndex > currentDisplayIndex && toIndex <= currentDisplayIndex -> currentDisplayIndex + 1
                else -> currentDisplayIndex
            }

            _uiState.update {
                it.copy(
                    displayQueue = list,
                    playbackQueue = list,
                    currentSongIndex = newDisplayIndex.coerceIn(0, list.size),
                    currentSong = list.getOrNull(newDisplayIndex.coerceIn(0, list.size))
                )
            }
        }
    }

    fun removeSongFromDisplayQueue(index: Int) {
        val list = _uiState.value.displayQueue.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            currentSongPlaylist = list
            val currentDisplayIndex = _uiState.value.currentSongIndex
            val newDisplayIndex = when {
                index < currentDisplayIndex -> currentDisplayIndex - 1
                else -> currentDisplayIndex
            }
            _uiState.update {
                it.copy(
                    displayQueue = list,
                    playbackQueue = list,
                    currentSongIndex = newDisplayIndex.coerceIn(0, list.size),
                    currentSong = list.getOrNull(newDisplayIndex.coerceIn(0, list.size))
                )
            }
        }
    }

    fun setPlayerSheetExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPlayerSheetExpanded = expanded) }
    }

    fun openPlayerSheet(anchor: Rect?) {
        _uiState.update { it.copy(isPlayerSheetExpanded = true, playerSheetAnchor = anchor) }
    }

    fun setQueueVisible(visible: Boolean) {
        _uiState.update { it.copy(isQueueVisible = visible) }
    }

    fun initAudioEffects() {
        if (!isAudioEffectsInitialized) {
            audioEffectManager.initEffects()
            isAudioEffectsInitialized = true
            viewModelScope.launch {
                try {
                    val settings = settingsRepository.userSettingsFlow.first()
                    val presets = settingsRepository.equalizerPresetsFlow.first()
                    val activeId = settingsRepository.activeEqualizerPresetIdFlow.first()
                    val preset = presets.find { it.id == activeId }
                    if (preset != null) {
                        activeEqPreset = preset
                        audioEffectManager.applyPreset(preset)
                    }
                    audioEffectManager.setEqualizerEnabled(settings.equalizerEnabled)
                    audioEffectManager.setBassBoostEnabled(settings.bassBoostEnabled)
                    audioEffectManager.setVirtualizerEnabled(settings.virtualizerEnabled)
                    _uiState.update { it.copy(isEqualizerEnabled = settings.equalizerEnabled, isBassBoostEnabled = settings.bassBoostEnabled) }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Failed to restore EQ preset", e)
                }
            }
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        audioEffectManager.setEqualizerEnabled(enabled)
        audioEffectManager.setBassBoostEnabled(enabled)
        audioEffectManager.setVirtualizerEnabled(enabled)
        _uiState.update { it.copy(isEqualizerEnabled = enabled, isBassBoostEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setEqualizerEnabled(enabled)
            settingsRepository.setBassBoostEnabled(enabled)
            settingsRepository.setVirtualizerEnabled(enabled)
        }
    }

    fun toggleBassBoost(enabled: Boolean) {
        audioEffectManager.setBassBoostEnabled(enabled)
        _uiState.update { it.copy(isBassBoostEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBassBoostEnabled(enabled) }
    }

    fun setBassBoostStrength(strength: Int) = audioEffectManager.setBassBoostStrength(strength)

    fun updateActiveEqPreset(preset: EqualizerPreset?) {
        activeEqPreset = preset
        preset?.let { audioEffectManager.applyPreset(it) }
    }
    fun setVirtualizerStrength(strength: Int) = audioEffectManager.setVirtualizerStrength(strength)
    fun toggleVisualizer(enabled: Boolean) {
        _uiState.update { it.copy(isVisualizerActive = enabled) }
    }

    fun getAudioEffectManager(): AudioEffectManager = audioEffectManager

    fun syncPlaybackState(
        filteredSongs: List<Song>,
        currentSongId: Long?,
        favoriteSongIds: Set<Long>,
        userSettings: UserSettings
    ) {
        val index = filteredSongs.indexOfFirst { it.id == currentSongId }
        playbackStateHolder.update {
            it.copy(
                filteredSongs = filteredSongs,
                currentSongId = currentSongId,
                currentSongIndex = index,
                isPlaying = _uiState.value.isPlaying,
                currentPositionMs = _uiState.value.currentPositionMs,
                durationMs = _uiState.value.durationMs,
                favoriteSongIds = favoriteSongIds,
                userSettings = userSettings,
                playbackMode = _uiState.value.playbackMode
            )
        }
    }

    private fun onTrackEnded() {
        when (_uiState.value.playbackMode) {
            PlaybackMode.REPEAT_ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            PlaybackMode.SHUFFLE, PlaybackMode.SEQUENTIAL -> {
                skipNext()
            }
        }
    }

    override fun onCleared() {
        audioEffectManager.release()
        super.onCleared()
    }
}