package com.example.rewind1137.viewmodel

import com.example.rewind1137.data.model.Song
import com.example.rewind1137.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateHolder @Inject constructor() {

    data class State(
        val filteredSongs: List<Song> = emptyList(),
        val currentSongId: Long? = null,
        val currentSongIndex: Int = -1,
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val favoriteSongIds: Set<Long> = emptySet(),
        val userSettings: UserSettings = UserSettings(),
        val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun update(transform: (State) -> State) {
        _state.update(transform)
    }
}
