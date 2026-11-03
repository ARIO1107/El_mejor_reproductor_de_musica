package com.example.re_pro.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val songId: Long,
    val title: String,
    val artist: String,
    val albumArtUri: String? = null,
    val duration: Long = 0L,
    val dateAdded: Long = 0L,
    val lastPlayed: Long = 0L,
    val playCount: Int = 0,
    val album: String = "",
    val genre: String = "",
    val contentUri: String = ""
)