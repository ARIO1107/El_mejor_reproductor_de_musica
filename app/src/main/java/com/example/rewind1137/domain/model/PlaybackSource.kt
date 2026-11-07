package com.example.rewind1137.domain.model

sealed interface PlaybackSource {
    data class Local(val songs: List<Song>) : PlaybackSource
    data class Remote(val songs: List<Song>) : PlaybackSource
    data class Hybrid(val localSongs: List<Song>, val remoteAvailable: Boolean) : PlaybackSource
}