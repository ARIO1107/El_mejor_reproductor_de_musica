package com.example.rewind1137.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: Set<Long> = emptySet()
)
