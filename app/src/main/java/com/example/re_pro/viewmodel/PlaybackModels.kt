package com.example.re_pro.viewmodel

enum class PlaybackMode(val title: String) {
    SEQUENTIAL("Secuencial"),
    SHUFFLE("Aleatorio"),
    REPEAT_ONE("Repetir Canción")
}

enum class AlbumArtMode(val title: String) {
    STATIC("Estática"),
    DISC("Disco Giratorio"),
    BEAT("Vibrante"),
    FUSION("Fundir con Fondo"),
    BREATHE("Respiración"),
    ZOOM("Zoom Cinemático"),
    SPIN("Rotación Continua"),
    NEON("Aura Neón"),
    DREAM("Sueño Difuminado"),
    WAVE("Onda Estéreo")
}

enum class NowPlayingAnimation(val title: String) {
    BARS("Barras de Ecualizador"),
    PULSE("Pulso de Círculo"),
    SLASH("Barrido Musical"),
    NOTE_FLY("Notas Flotantes"),
    WAVE("Onda Sonora"),
    SPIN_DOT("Giro Minimalista"),
    GLOW("Glow Respirante"),
    JUMP_DOT("Botecito"),
    BEAT("Latido"),
    RING("Anillo Giratorio")
}

enum class SortOption(val label: String) {
    DATE_ADDED_DESC("Más recientes primero"),
    TITLE_ASC("Título (A-Z)"),
    TITLE_DESC("Título (Z-A)"),
    ARTIST_ASC("Artista (A-Z)"),
    DURATION_DESC("Mayor duración"),
    PLAY_COUNT_DESC("Mas escuchadas"),
    LAST_PLAYED("Escuchadas reciente"),
    ALBUM_ASC("Album A-Z"),
    GENRE_ASC("Genero A-Z")
}

enum class LibraryFilter(val label: String) {
    ALL("Todas"),
    FAVORITES("♥ Me Gusta")
}

data class Playlist(
    val id: String,
    val name: String,
    val songIds: Set<Long> = emptySet()
)
