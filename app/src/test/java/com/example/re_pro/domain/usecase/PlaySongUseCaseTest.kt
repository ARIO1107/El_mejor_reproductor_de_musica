package com.example.re_pro.domain.usecase

import com.example.re_pro.domain.model.PlaybackMode
import com.example.re_pro.domain.model.Song
import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlaySongUseCaseTest {

    private lateinit var useCase: PlaySongUseCase

    private val testSongs = listOf(
        Song(id = 1, title = "Song A", artist = "Artist A", uri = mockk<Uri>(), duration = 180000, dateAdded = 100),
        Song(id = 2, title = "Song B", artist = "Artist B", uri = mockk<Uri>(), duration = 240000, dateAdded = 200),
        Song(id = 3, title = "Song C", artist = "Artist C", uri = mockk<Uri>(), duration = 200000, dateAdded = 300),
        Song(id = 4, title = "Song D", artist = "Artist D", uri = mockk<Uri>(), duration = 300000, dateAdded = 400)
    )

    @Before
    fun setUp() {
        useCase = PlaySongUseCase()
    }

    @Test
    fun `getNextIndex sequential moves to next`() {
        val result = useCase.getNextIndex(0, testSongs, PlaybackMode.SEQUENTIAL)
        assertEquals(1, result)
    }

    @Test
    fun `getNextIndex sequential wraps around`() {
        val result = useCase.getNextIndex(3, testSongs, PlaybackMode.SEQUENTIAL)
        assertEquals(0, result)
    }

    @Test
    fun `getNextIndex repeat_one stays on same`() {
        val result = useCase.getNextIndex(2, testSongs, PlaybackMode.REPEAT_ONE)
        assertEquals(2, result)
    }

    @Test
    fun `getNextIndex shuffle returns valid index`() {
        val result = useCase.getNextIndex(0, testSongs, PlaybackMode.SHUFFLE)
        assertTrue(result in testSongs.indices)
    }

    @Test
    fun `getNextIndex shuffle with single song returns 0`() {
        val singleSong = listOf(testSongs[0])
        val result = useCase.getNextIndex(0, singleSong, PlaybackMode.SHUFFLE)
        assertEquals(0, result)
    }

    @Test
    fun `getPreviousIndex sequential moves to previous`() {
        val result = useCase.getPreviousIndex(2, testSongs, PlaybackMode.SEQUENTIAL)
        assertEquals(1, result)
    }

    @Test
    fun `getPreviousIndex sequential wraps around from 0`() {
        val result = useCase.getPreviousIndex(0, testSongs, PlaybackMode.SEQUENTIAL)
        assertEquals(3, result)
    }

    @Test
    fun `getPreviousIndex repeat_one stays on same`() {
        val result = useCase.getPreviousIndex(1, testSongs, PlaybackMode.REPEAT_ONE)
        assertEquals(1, result)
    }

    @Test
    fun `buildQueue sequential produces correct order`() {
        val queue = useCase.buildQueue(testSongs, 1, PlaybackMode.SEQUENTIAL)
        assertEquals(testSongs[1], queue[0])
        assertEquals(testSongs[2], queue[1])
        assertEquals(testSongs[3], queue[2])
        assertEquals(testSongs[0], queue[3])
    }

    @Test
    fun `buildQueue repeat_one contains only current song`() {
        val queue = useCase.buildQueue(testSongs, 2, PlaybackMode.REPEAT_ONE)
        assertEquals(1, queue.size)
        assertEquals(testSongs[2], queue[0])
    }

    @Test
    fun `buildQueue shuffle contains all songs`() {
        val queue = useCase.buildQueue(testSongs, 0, PlaybackMode.SHUFFLE)
        assertEquals(testSongs.size, queue.size)
        assertEquals(testSongs[0], queue[0])
        assertTrue(queue.drop(1).containsAll(testSongs.filter { it.id != 1L }))
    }

    @Test
    fun `buildQueue empty list returns empty`() {
        val queue = useCase.buildQueue(emptyList(), 0, PlaybackMode.SEQUENTIAL)
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `cyclePlaybackMode sequential to shuffle`() {
        val result = useCase.cyclePlaybackMode(PlaybackMode.SEQUENTIAL)
        assertEquals(PlaybackMode.SHUFFLE, result)
    }

    @Test
    fun `cyclePlaybackMode shuffle to repeat_one`() {
        val result = useCase.cyclePlaybackMode(PlaybackMode.SHUFFLE)
        assertEquals(PlaybackMode.REPEAT_ONE, result)
    }

    @Test
    fun `cyclePlaybackMode repeat_one to sequential`() {
        val result = useCase.cyclePlaybackMode(PlaybackMode.REPEAT_ONE)
        assertEquals(PlaybackMode.SEQUENTIAL, result)
    }
}
