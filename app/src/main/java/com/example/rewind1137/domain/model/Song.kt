package com.example.rewind1137.domain.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val duration: Long,
    val albumId: Long = 0L,
    val albumArtUri: Uri? = null,
    val dateAdded: Long = 0L
)
