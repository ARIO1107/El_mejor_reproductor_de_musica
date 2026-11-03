package com.example.re_pro.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: Set<Long> = emptySet()
)
