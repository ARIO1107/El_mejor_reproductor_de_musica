package com.example.re_pro.domain.usecase

import com.example.re_pro.domain.model.PlaybackMode
import com.example.re_pro.domain.model.Song
import javax.inject.Inject
import kotlin.random.Random

class PlaySongUseCase @Inject constructor() {

    fun getNextIndex(
        currentIndex: Int,
        songs: List<Song>,
        mode: PlaybackMode
    ): Int {
        return when (mode) {
            PlaybackMode.SHUFFLE -> if (songs.size > 1) Random.nextInt(songs.size) else 0
            PlaybackMode.REPEAT_ONE -> if (currentIndex in songs.indices) currentIndex else 0
            PlaybackMode.SEQUENTIAL -> (currentIndex + 1) % songs.size
        }
    }

    fun getPreviousIndex(
        currentIndex: Int,
        songs: List<Song>,
        mode: PlaybackMode
    ): Int {
        return when (mode) {
            PlaybackMode.SHUFFLE -> if (songs.size > 1) Random.nextInt(songs.size) else 0
            PlaybackMode.REPEAT_ONE -> if (currentIndex in songs.indices) currentIndex else 0
            PlaybackMode.SEQUENTIAL -> if (currentIndex > 0) currentIndex - 1 else songs.size - 1
        }
    }

    fun buildQueue(
        songs: List<Song>,
        currentIndex: Int,
        mode: PlaybackMode
    ): List<Song> {
        if (songs.isEmpty()) return emptyList()

        return when (mode) {
            PlaybackMode.SEQUENTIAL -> {
                val upcoming = songs.subList(currentIndex.coerceIn(0, songs.size), songs.size)
                upcoming + songs.subList(0, currentIndex.coerceIn(0, songs.size))
            }
            PlaybackMode.SHUFFLE -> {
                val currentSong = songs.getOrNull(currentIndex)
                val shuffled = songs.toMutableList().shuffled()
                if (currentSong != null) {
                    shuffled.filter { it.id != currentSong.id }.let { rest ->
                        listOf(currentSong) + rest
                    }
                } else {
                    shuffled
                }
            }
            PlaybackMode.REPEAT_ONE -> {
                val currentSong = songs.getOrNull(currentIndex)
                if (currentSong != null) listOf(currentSong) else emptyList()
            }
        }
    }

    fun cyclePlaybackMode(currentMode: PlaybackMode): PlaybackMode {
        return when (currentMode) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SEQUENTIAL
        }
    }
}
