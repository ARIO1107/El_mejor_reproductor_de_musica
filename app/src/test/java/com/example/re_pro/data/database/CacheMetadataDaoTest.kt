package com.example.re_pro.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.re_pro.data.database.dao.CacheMetadataDao
import com.example.re_pro.data.database.entities.CacheMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CacheMetadataDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CacheMetadataDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.cacheMetadataDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun createEntity(
        songId: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        albumArtUri: String? = null,
        duration: Long = 180000L,
        dateAdded: Long = 1000L,
        lastPlayed: Long = 2000L,
        playCount: Int = 1
    ) = CacheMetadataEntity(
        songId = songId,
        title = title,
        artist = artist,
        albumArtUri = albumArtUri,
        duration = duration,
        dateAdded = dateAdded,
        lastPlayed = lastPlayed,
        playCount = playCount
    )

    @Test
    fun `insert and retrieve cache metadata`() = runTest {
        val metadata = createEntity()
        dao.insertCacheMetadata(metadata)

        val result = dao.getCachedMetadata(1L)
        assertNotNull(result)
        assertEquals("Test Song", result?.title)
        assertEquals("Test Artist", result?.artist)
        assertEquals(180000L, result?.duration)
        assertEquals(1000L, result?.dateAdded)
        assertEquals(2000L, result?.lastPlayed)
        assertEquals(1, result?.playCount)
    }

    @Test
    fun `getCachedMetadata returns null for non-existent song`() = runTest {
        val result = dao.getCachedMetadata(999L)
        assertNull(result)
    }

    @Test
    fun `insert with replace updates existing entry`() = runTest {
        val original = createEntity(playCount = 1)
        dao.insertCacheMetadata(original)

        val updated = createEntity(playCount = 5)
        dao.insertCacheMetadata(updated)

        val result = dao.getCachedMetadata(1L)
        assertNotNull(result)
        assertEquals(5, result?.playCount)
    }

    @Test
    fun `getAllCachedMetadata returns all entries ordered by lastPlayed`() = runTest {
        val older = createEntity(songId = 1L, title = "Old Song", lastPlayed = 100L)
        val newer = createEntity(songId = 2L, title = "New Song", lastPlayed = 500L)
        dao.insertCacheMetadata(older)
        dao.insertCacheMetadata(newer)

        val all = dao.getAllCachedMetadata().first()
        assertEquals(2, all.size)
        assertEquals("New Song", all[0].title)
        assertEquals("Old Song", all[1].title)
    }

    @Test
    fun `getAllCachedMetadata returns empty list when no entries`() = runTest {
        val all = dao.getAllCachedMetadata().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `deleteCacheMetadata removes specific entry`() = runTest {
        dao.insertCacheMetadata(createEntity(songId = 1L, title = "Song A"))
        dao.insertCacheMetadata(createEntity(songId = 2L, title = "Song B"))

        dao.deleteCacheMetadata(1L)

        val all = dao.getAllCachedMetadata().first()
        assertEquals(1, all.size)
        assertEquals("Song B", all[0].title)
    }

    @Test
    fun `clearAllCacheMetadata removes all entries`() = runTest {
        dao.insertCacheMetadata(createEntity(songId = 1L))
        dao.insertCacheMetadata(createEntity(songId = 2L))
        dao.insertCacheMetadata(createEntity(songId = 3L))

        dao.clearAllCacheMetadata()

        val all = dao.getAllCachedMetadata().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `entity with albumArtUri persists correctly`() = runTest {
        val uri = "content://media/external/audio/albumart/1"
        val metadata = createEntity(albumArtUri = uri)
        dao.insertCacheMetadata(metadata)

        val result = dao.getCachedMetadata(1L)
        assertEquals(uri, result?.albumArtUri)
    }

    @Test
    fun `multiple operations work in sequence`() = runTest {
        dao.insertCacheMetadata(createEntity(songId = 1L, title = "First"))
        dao.insertCacheMetadata(createEntity(songId = 2L, title = "Second"))
        dao.insertCacheMetadata(createEntity(songId = 3L, title = "Third"))

        dao.deleteCacheMetadata(2L)
        val afterDelete = dao.getAllCachedMetadata().first()
        assertEquals(2, afterDelete.size)

        dao.clearAllCacheMetadata()
        val afterClear = dao.getAllCachedMetadata().first()
        assertTrue(afterClear.isEmpty())
    }
}
