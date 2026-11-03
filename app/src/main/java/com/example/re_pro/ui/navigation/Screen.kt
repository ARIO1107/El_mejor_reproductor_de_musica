package com.example.re_pro.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Player : Screen("player")
    data object Queue : Screen("queue")
    data object Settings : Screen("settings")
    data object Folders : Screen("folders")
    data object Theme : Screen("theme")
    data object TagEditor : Screen("tag_editor")
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
}
