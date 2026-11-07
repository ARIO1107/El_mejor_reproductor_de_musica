package com.example.rewind1137.domain.usecase

import com.example.rewind1137.domain.model.LibraryFilter
import com.example.rewind1137.domain.model.Song
import com.example.rewind1137.domain.model.SortOption
import com.example.rewind1137.domain.repository.MusicRepository
import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetSongsUseCaseTest {

    private lateinit var useCase: GetSongsUseCase

    private val testSongs = listOf(
        Song(id = 1, title = "Alpha Song", artist = "Charlie", uri = mockk<Uri>(), duration = 180000, dateAdded = 100),
        Song(id = 2, title = "Beta Song", artist = "Alice", uri = mockk<Uri>(), duration = 240000, dateAdded = 300),
        Song(id = 3, title = "Gamma Song", artist = "Bob", uri = mockk<Uri>(), duration = 120000, dateAdded = 200),
        Song(id = 4, title = "Delta Song", artist = "Alice", uri = mockk<Uri>(), duration = 300000, dateAdded = 400)
    )

    private val favorites = setOf(1L, 3L)

    @Before
    fun setUp() {
        useCase = GetSongsUseCase(FakeMusicRepository())
    }

    @Test
    fun `filterAndSort all songs with no query`() {
        val result = useCase.filterAndSort(testSongs, "", SortOption.DATE_ADDED_DESC, LibraryFilter.ALL, favorites)
        assertEquals(4, result.size)
    }

    @Test
    fun `filterAndSort by title ascending`() {
        val result = useCase.filterAndSort(testSongs, "", SortOption.TITLE_ASC, LibraryFilter.ALL, favorites)
        assertEquals("Alpha Song", result[0].title)
        assertEquals("Beta Song", result[1].title)
        assertEquals("Delta Song", result[2].title)
        assertEquals("Gamma Song", result[3].title)
    }

    @Test
    fun `filterAndSort by artist ascending`() {
        val result = useCase.filterAndSort(testSongs, "", SortOption.ARTIST_ASC, LibraryFilter.ALL, favorites)
        assertEquals("Alice", result[0].artist)
        assertEquals("Alice", result[1].artist)
        assertEquals("Bob", result[2].artist)
        assertEquals("Charlie", result[3].artist)
    }

    @Test
    fun `filterAndSort by duration descending`() {
        val result = useCase.filterAndSort(testSongs, "", SortOption.DURATION_DESC, LibraryFilter.ALL, favorites)
        assertEquals(4, result[0].id)
        assertEquals(2, result[1].id)
    }

    @Test
    fun `filterAndSort favorites only`() {
        val result = useCase.filterAndSort(testSongs, "", SortOption.DATE_ADDED_DESC, LibraryFilter.FAVORITES, favorites)
        assertEquals(2, result.size)
        assertTrue(result.all { favorites.contains(it.id) })
    }

    @Test
    fun `filterAndSort search by title`() {
        val result = useCase.filterAndSort(testSongs, "alpha", SortOption.DATE_ADDED_DESC, LibraryFilter.ALL, favorites)
        assertEquals(1, result.size)
        assertEquals("Alpha Song", result[0].title)
    }

    @Test
    fun `filterAndSort search by artist`() {
        val result = useCase.filterAndSort(testSongs, "alice", SortOption.DATE_ADDED_DESC, LibraryFilter.ALL, favorites)
        assertEquals(2, result.size)
        assertTrue(result.all { it.artist == "Alice" })
    }

    @Test
    fun `filterAndSort search case insensitive`() {
        val result = useCase.filterAndSort(testSongs, "BOB", SortOption.DATE_ADDED_DESC, LibraryFilter.ALL, favorites)
        assertEquals(1, result.size)
        assertEquals("Bob", result[0].artist)
    }

    @Test
    fun `filterAndSort empty query returns all`() {
        val result = useCase.filterAndSort(testSongs, "", SortOption.DATE_ADDED_DESC, LibraryFilter.ALL, favorites)
        assertEquals(4, result.size)
    }

    @Test
    fun `filterAndSort no matches returns empty`() {
        val result = useCase.filterAndSort(testSongs, "xyz", SortOption.DATE_ADDED_DESC, LibraryFilter.ALL, favorites)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterAndSort empty songs list returns empty`() {
        val result = useCase.filterAndSort(emptyList(), "alpha", SortOption.TITLE_ASC, LibraryFilter.ALL, favorites)
        assertTrue(result.isEmpty())
    }
}

class FakeMusicRepository : MusicRepository {
    override suspend fun fetchLocalSongs(excludeShortClips: Boolean): List<Song> = emptyList()
    override suspend fun getOfflineQueue(): List<Song> = emptyList()
    override suspend fun batchCacheSongs(songs: List<Song>) {}
    override fun getPlaybackSource(preferLocal: Boolean, forceRemote: Boolean): com.example.rewind1137.domain.model.PlaybackSource =
        com.example.rewind1137.domain.model.PlaybackSource.Local(emptyList())
}
