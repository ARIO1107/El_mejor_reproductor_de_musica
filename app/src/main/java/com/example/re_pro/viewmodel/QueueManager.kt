package com.example.re_pro.viewmodel

import com.example.re_pro.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackQueue(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0
)

data class QueuesState(
    val queues: List<PlaybackQueue> = listOf(
        PlaybackQueue(id = "default", name = "Cola Principal")
    ),
    val activeQueueId: String = "default"
) {
    val activeQueue: PlaybackQueue
        get() = queues.find { it.id == activeQueueId } ?: queues.first()
}

class QueueManager {

    private val _queuesState = MutableStateFlow(QueuesState())
    val queuesState: StateFlow<QueuesState> = _queuesState.asStateFlow()

    fun getActiveQueue(): PlaybackQueue = _queuesState.value.activeQueue

    fun getActiveSongs(): List<Song> = _queuesState.value.activeQueue.songs

    fun setCurrentIndex(index: Int) {
        _queuesState.update { state ->
            val activeQueue = state.activeQueue
            state.copy(
                queues = state.queues.map {
                    if (it.id == activeQueue.id) it.copy(currentIndex = index) else it
                }
            )
        }
    }

    fun setQueue(songs: List<Song>) {
        _queuesState.update { state ->
            val activeQueue = state.activeQueue
            state.copy(
                queues = state.queues.map {
                    if (it.id == activeQueue.id) it.copy(songs = songs, currentIndex = 0) else it
                }
            )
        }
    }

    fun setQueueForSong(song: Song, allSongs: List<Song>) {
        _queuesState.update { state ->
            val activeQueue = state.activeQueue
            val idx = allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            state.copy(
                queues = state.queues.map {
                    if (it.id == activeQueue.id) it.copy(songs = allSongs, currentIndex = idx) else it
                }
            )
        }
    }

    fun createQueue(name: String): PlaybackQueue {
        val id = "queue_${System.currentTimeMillis()}"
        val newQueue = PlaybackQueue(id = id, name = name)
        _queuesState.update { state ->
            state.copy(queues = state.queues + newQueue)
        }
        return newQueue
    }

    fun deleteQueue(queueId: String) {
        _queuesState.update { state ->
            if (queueId == "default") return@update state
            val newQueues = state.queues.filter { it.id != queueId }
            val newActiveId = if (state.activeQueueId == queueId) "default" else state.activeQueueId
            state.copy(queues = newQueues, activeQueueId = newActiveId)
        }
    }

    fun renameQueue(queueId: String, newName: String) {
        _queuesState.update { state ->
            state.copy(
                queues = state.queues.map {
                    if (it.id == queueId) it.copy(name = newName) else it
                }
            )
        }
    }

    fun switchQueue(queueId: String) {
        _queuesState.update { it.copy(activeQueueId = queueId) }
    }

    fun addSongToQueue(song: Song, queueId: String? = null) {
        val targetId = queueId ?: _queuesState.value.activeQueueId
        _queuesState.update { state ->
            state.copy(
                queues = state.queues.map {
                    if (it.id == targetId) it.copy(songs = it.songs + song) else it
                }
            )
        }
    }

    fun removeSongFromQueue(songId: Long, queueId: String? = null) {
        val targetId = queueId ?: _queuesState.value.activeQueueId
        _queuesState.update { state ->
            state.copy(
                queues = state.queues.map {
                    if (it.id == targetId) {
                        it.copy(songs = it.songs.filter { s -> s.id != songId })
                    } else it
                }
            )
        }
    }

    fun clearQueue(queueId: String? = null) {
        val targetId = queueId ?: _queuesState.value.activeQueueId
        _queuesState.update { state ->
            state.copy(
                queues = state.queues.map {
                    if (it.id == targetId) it.copy(songs = emptyList(), currentIndex = 0) else it
                }
            )
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        _queuesState.update { state ->
            val activeQueue = state.activeQueue
            val songs = activeQueue.songs.toMutableList()
            if (fromIndex in songs.indices && toIndex in songs.indices && fromIndex != toIndex) {
                val item = songs.removeAt(fromIndex)
                songs.add(toIndex, item)
                val newCurrentIndex = when {
                    activeQueue.currentIndex == fromIndex -> toIndex
                    fromIndex < activeQueue.currentIndex && toIndex >= activeQueue.currentIndex -> activeQueue.currentIndex - 1
                    fromIndex > activeQueue.currentIndex && toIndex <= activeQueue.currentIndex -> activeQueue.currentIndex + 1
                    else -> activeQueue.currentIndex
                }
                state.copy(
                    queues = state.queues.map {
                        if (it.id == activeQueue.id) it.copy(songs = songs, currentIndex = newCurrentIndex.coerceIn(0, songs.size)) else it
                    }
                )
            } else state
        }
    }
}
