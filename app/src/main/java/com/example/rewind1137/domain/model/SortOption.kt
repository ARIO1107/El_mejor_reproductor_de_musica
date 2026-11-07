package com.example.rewind1137.domain.model

enum class SortOption(val label: String) {
    DATE_ADDED_DESC("Más recientes primero"),
    TITLE_ASC("Título (A-Z)"),
    TITLE_DESC("Título (Z-A)"),
    ARTIST_ASC("Artista (A-Z)"),
    DURATION_DESC("Mayor duración")
}
