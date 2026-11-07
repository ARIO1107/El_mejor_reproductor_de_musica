package com.example.rewind1137.data.database.dao

import androidx.room.*
import com.example.rewind1137.data.database.entities.CacheMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata ORDER BY lastPlayed DESC")
    fun getAllCachedMetadata(): Flow<List<CacheMetadataEntity>>

    @Query("SELECT * FROM cache_metadata WHERE songId = :songId")
    suspend fun getCachedMetadata(songId: Long): CacheMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheMetadata(metadata: CacheMetadataEntity)

    @Query("DELETE FROM cache_metadata WHERE songId = :songId")
    suspend fun deleteCacheMetadata(songId: Long)

    @Query("DELETE FROM cache_metadata")
    suspend fun clearAllCacheMetadata()

    @Query("UPDATE cache_metadata SET playCount = playCount + 1, lastPlayed = :timestamp WHERE songId = :songId")
    suspend fun incrementPlayCount(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cache_metadata SET lastPlayed = :timestamp WHERE songId = :songId")
    suspend fun updateLastPlayed(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM cache_metadata WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 50): Flow<List<CacheMetadataEntity>>

    @Query("SELECT * FROM cache_metadata WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<CacheMetadataEntity>>

    @Query("SELECT * FROM cache_metadata ORDER BY playCount DESC")
    fun getAllByPlayCount(): Flow<List<CacheMetadataEntity>>

    @Transaction
    suspend fun recordPlay(
        songId: Long,
        title: String,
        artist: String,
        albumArtUri: String?,
        duration: Long,
        dateAdded: Long,
        album: String,
        genre: String,
        contentUri: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val existing = getCachedMetadata(songId)
        if (existing == null) {
            insertCacheMetadata(
                CacheMetadataEntity(
                    songId = songId,
                    title = title,
                    artist = artist,
                    albumArtUri = albumArtUri,
                    duration = duration,
                    dateAdded = dateAdded,
                    lastPlayed = timestamp,
                    playCount = 1,
                    album = album,
                    genre = genre,
                    contentUri = contentUri
                )
            )
        } else {
            incrementPlayCount(songId, timestamp)
        }
    }
}