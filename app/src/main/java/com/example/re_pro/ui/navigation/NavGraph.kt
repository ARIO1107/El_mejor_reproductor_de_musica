package com.example.re_pro.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.re_pro.R
import com.example.re_pro.data.model.Song
import com.example.re_pro.ui.components.BottomNavTab
import com.example.re_pro.ui.components.PlayerSheet
import com.example.re_pro.ui.screens.FolderScreen
import com.example.re_pro.ui.screens.MainScreen
import com.example.re_pro.ui.screens.PlaylistDetailScreen
import com.example.re_pro.ui.screens.QueueScreen
import com.example.re_pro.ui.screens.SettingsScreen
import com.example.re_pro.ui.screens.TagEditorScreen
import com.example.re_pro.ui.screens.ThemeScreen
import com.example.re_pro.utils.SleepTimerManager
import com.example.re_pro.utils.SleepTimerOption
import com.example.re_pro.viewmodel.LibraryViewModel
import com.example.re_pro.viewmodel.PlayerViewModel
import com.example.re_pro.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val playerUiState by playerViewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var songForPlaylistDialog by remember { mutableStateOf<com.example.re_pro.data.model.Song?>(null) }
    var songsPendingDelete by remember { mutableStateOf<List<Song>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val appContext = LocalContext.current

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            libraryViewModel.refreshSongs()
        }
        songsPendingDelete = null
    }

    LaunchedEffect(
        playerUiState.currentSong?.id,
        playerUiState.isPlaying,
        libraryUiState.filteredSongs,
        libraryUiState.favoriteSongIds,
        settingsUiState.userSettings
    ) {
        playerViewModel.syncPlaybackState(
            filteredSongs = libraryUiState.filteredSongs,
            currentSongId = playerUiState.currentSong?.id,
            favoriteSongIds = libraryUiState.favoriteSongIds,
            userSettings = settingsUiState.userSettings
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Text(
                    stringResource(R.string.sleep_timer_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepTimerOption.entries.forEach { option ->
                        val optionLabel = when (option) {
                            SleepTimerOption.OFF -> stringResource(R.string.sleep_timer_off)
                            SleepTimerOption.FIFTEEN_MIN -> stringResource(R.string.sleep_timer_15)
                            SleepTimerOption.THIRTY_MIN -> stringResource(R.string.sleep_timer_30)
                            SleepTimerOption.SIXTY_MIN -> stringResource(R.string.sleep_timer_60)
                            SleepTimerOption.END_OF_TRACK -> stringResource(R.string.sleep_timer_end_of_track)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SleepTimerManager.startTimer(
                                        option = option,
                                        scope = coroutineScope
                                    )
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = SleepTimerManager.activeOption == option,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                optionLabel,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    songForPlaylistDialog?.let { song ->
        com.example.re_pro.ui.components.AddToPlaylistDialog(
            song = song,
            isFavorite = libraryUiState.favoriteSongIds.contains(song.id),
            playlists = libraryUiState.customPlaylists,
            onDismiss = { songForPlaylistDialog = null },
            onToggleFavorite = { libraryViewModel.toggleFavorite(song.id) },
            onCreatePlaylist = { libraryViewModel.createPlaylist(it) },
            onTogglePlaylistSong = { playlistId, isInPlaylist ->
                if (isInPlaylist) {
                    libraryViewModel.removeSongFromPlaylist(playlistId, song.id)
                } else {
                    libraryViewModel.addSongToPlaylist(playlistId, song.id)
                }
            }
        )
    }

    val activeSong = playerUiState.currentSong

    songsPendingDelete?.let { pendingSongs ->
        AlertDialog(
            onDismissRequest = { songsPendingDelete = null },
            title = {
                Text(
                    "Eliminar ${pendingSongs.size} canción${if (pendingSongs.size == 1) "" else "es"}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    "Se eliminarán del dispositivo de forma permanente. Esta acción no se puede deshacer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val uris = pendingSongs.map { it.uri }
                            val sender = MediaStore.createDeleteRequest(appContext.contentResolver, uris).intentSender
                            deleteRequestLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        } catch (_: Exception) {
                            libraryViewModel.deleteSongsFromDeviceFallback(pendingSongs)
                            songsPendingDelete = null
                        }
                    } else {
                        libraryViewModel.deleteSongsFromDeviceFallback(pendingSongs)
                        songsPendingDelete = null
                    }
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songsPendingDelete = null }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composableWithSlideTransition(Screen.Main.route) {
            val multiSelectContext = LocalContext.current
            MainScreen(
                libraryUiState = libraryUiState,
                playerUiState = playerUiState,
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    when (tab) {
                        BottomNavTab.HOME -> { }
                        BottomNavTab.EQUALIZER -> { }
                        BottomNavTab.SEARCH -> { }
                        BottomNavTab.LIBRARY -> { }
                    }
                },
                onPlaySongAtIndex = { playerViewModel.playSongAtIndex(it, libraryUiState.filteredSongs) },
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onPreviousClick = { playerViewModel.skipPrevious() },
                onNextClick = { playerViewModel.skipNext() },
                onSeek = { playerViewModel.seekTo(it) },
                onTogglePlaybackMode = { playerViewModel.togglePlaybackMode() },
                onSortOptionSelected = { libraryViewModel.setSortOption(it) },
                onGridColumnsChange = { settingsViewModel.setGridColumns(it) },
                onCompactListChange = { libraryViewModel.setCompactMode(it) },
                onFilterSelected = { libraryViewModel.setLibraryFilter(it) },
                audioEffectManager = playerViewModel.getAudioEffectManager(),
                eqUserPresets = settingsUiState.equalizerPresets,
                eqActivePresetId = settingsUiState.activeEqualizerPresetId,
                onEqSavePreset = { settingsViewModel.saveEqualizerPreset(it) },
                onEqDeletePreset = { settingsViewModel.deleteEqualizerPreset(it) },
                onEqSelectPreset = { settingsViewModel.setActiveEqualizerPreset(it) },
                onEqUpdatePreset = { settingsViewModel.saveEqualizerPreset(it) },
                onToggleEqualizer = { playerViewModel.toggleEqualizer(it) },
                onSearchQueryChange = { libraryViewModel.search(it) },
                onClearRecentSearches = { libraryViewModel.clearRecentSearches() },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Main.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenPlayerSheet = { playerViewModel.openPlayerSheet(it) },
                onMultiSelectAction = { action, songs ->
                    when (action) {
                        "play" -> if (songs.isNotEmpty()) playerViewModel.playSongAtIndex(0, songs)
                        "favorite" -> songs.forEach { libraryViewModel.toggleFavorite(it.id) }
                        "playlist" -> { if (songs.isNotEmpty()) songForPlaylistDialog = songs.first() }
                        "share" -> shareSongs(multiSelectContext, songs)
                        "delete" -> { if (songs.isNotEmpty()) songsPendingDelete = songs }
                    }
                },
                initialGridColumns = settingsUiState.userSettings.gridColumns,
                onPlaylistClick = { navController.navigate(Screen.Folders.route) },
                onRefresh = { libraryViewModel.refreshSongs() },
                onCreatePlaylist = { libraryViewModel.createPlaylist(it) },
                onSmartPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onSongClick = { song ->
                    val index = libraryUiState.filteredSongs.indexOfFirst { it.id == song.id }
                    if (index >= 0) playerViewModel.playSongAtIndex(index, libraryUiState.filteredSongs)
                },
                nowPlayingAnimation = settingsUiState.userSettings.nowPlayingAnimation
            )
        }

        composableWithSlideUpTransition(Screen.Player.route) {
            val currentSong = playerUiState.currentSong
            PlayerSheet(
                song = currentSong,
                isPlaying = playerUiState.isPlaying,
                currentPositionMs = playerUiState.currentPositionMs,
                durationMs = playerUiState.durationMs,
                playbackMode = playerUiState.playbackMode,
                albumArtMode = settingsUiState.userSettings.albumArtMode,
                onAlbumArtModeChange = { settingsViewModel.setAlbumArtMode(it) },
                isFavorite = currentSong?.let { libraryUiState.favoriteSongIds.contains(it.id) } ?: false,
                onDismiss = { navController.popBackStack() },
                onSeek = { playerViewModel.seekTo(it) },
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onPreviousClick = { playerViewModel.skipPrevious() },
                onNextClick = { playerViewModel.skipNext() },
                onTogglePlaybackMode = { playerViewModel.togglePlaybackMode() },
                onToggleFavorite = { currentSong?.let { song -> libraryViewModel.toggleFavorite(song.id) } },
                onAddToPlaylistClick = { currentSong?.let { song -> songForPlaylistDialog = song } },
                onEqualizerClick = {
                    currentTab = BottomNavTab.EQUALIZER
                    navController.popBackStack()
                },
                onQueueClick = { navController.navigate(Screen.Queue.route) },
                onTagEditorClick = { navController.navigate(Screen.TagEditor.route) },
                onAudioInfoClick = { /* show audio info dialog - handled via state */ },
                onShareClick = { playerViewModel.shareCurrentSong() },
                lyricsRaw = playerUiState.lyricsRaw,
                onSeekToLine = { timeMs -> playerViewModel.seekTo(timeMs) }
            )
        }

        composableWithSlideTransition(Screen.Queue.route) {
            QueueScreen(
                libraryUiState = libraryUiState,
                playerUiState = playerUiState,
                onBackClick = { navController.popBackStack() },
                onPlaySongAtIndex = { playerViewModel.playSongAtIndex(it, playerUiState.displayQueue) },
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onTogglePlaybackMode = { playerViewModel.togglePlaybackMode() },
                onReorder = { from, to -> playerViewModel.reorderDisplayQueue(from, to) },
                onRemoveItem = { playerViewModel.removeSongFromDisplayQueue(it) },
                onClearQueue = { playerViewModel.clearDisplayQueue() },
                onAddToPlaylistClick = { songForPlaylistDialog = it },
                onPlayNextClick = { playerViewModel.playNext(it) },
                onPlayLastClick = { playerViewModel.playLast(it) },
                onSaveQueueAsPlaylist = { name ->
                    libraryViewModel.createPlaylistWithSongs(name, playerUiState.displayQueue.map { it.id }.toSet())
                }
            )
        }

        composableWithSlideTransition(Screen.Settings.route) {
            val scanningText = stringResource(R.string.snackbar_scanning)
            val cacheClearedText = stringResource(R.string.snackbar_cache_cleared)
            SettingsScreen(
                settingsUiState = settingsUiState,
                onBackClick = { navController.popBackStack() },
                onExcludeShortClipsChange = { settingsViewModel.setExcludeShortClips(it) },
                onMinAudioDurationChange = { settingsViewModel.setMinAudioDurationMs(it) },
                onRescanStorageClick = {
                    libraryViewModel.refreshSongs()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(scanningText)
                    }
                },
                onOpenEqualizerClick = {
                    currentTab = BottomNavTab.EQUALIZER
                    navController.popBackStack()
                },
                onClearCacheClick = {
                    settingsViewModel.clearAppCache(navController.context)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(cacheClearedText)
                    }
                },
                onOpenThemeScreenClick = { navController.navigate(Screen.Theme.route) },
                onOpenFoldersClick = { navController.navigate(Screen.Folders.route) },
                onOpenSleepTimerClick = { showSleepTimerDialog = true },
                onCrossfadeChange = { seconds ->
                    settingsViewModel.setCrossfadeSeconds(seconds)
                    com.example.re_pro.utils.MusicPlayerManager.setCrossfadeDuration(seconds)
                },
                onReplayGainChange = { settingsViewModel.setReplayGain(it) },
                onExportBackup = { settingsViewModel.exportBackup(navController.context) },
                onImportBackup = { settingsViewModel.importBackup(navController.context, it) },
                backupStatus = settingsUiState.backupStatus,
                onClearBackupStatus = { settingsViewModel.clearBackupStatus() },
                albumArtUri = playerUiState.currentSong?.albumArtUri,
                onNowPlayingAnimationChange = { settingsViewModel.setNowPlayingAnimation(it) }
            )
        }

        composableWithSlideTransition(Screen.Folders.route) {
            FolderScreen(
                libraryUiState = libraryUiState,
                onBackClick = { navController.popBackStack() },
                onPlaySong = { song ->
                    val index = libraryUiState.filteredSongs.indexOfFirst { it.id == song.id }
                    if (index >= 0) playerViewModel.playSongAtIndex(index, libraryUiState.filteredSongs)
                },
                onPlayQueue = { songs, index ->
                    if (songs.isNotEmpty()) playerViewModel.playSongAtIndex(index, songs)
                },
                playStatsMap = libraryUiState.playStatsMap
            )
        }

        composableWithSlideTransition(Screen.PlaylistDetail.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                playlistId = playlistId,
                libraryUiState = libraryUiState,
                onBackClick = { navController.popBackStack() },
                onPlaySongAtPosition = { index ->
                    val playlist = libraryUiState.customPlaylists.find { it.id == playlistId }
                    val smartSongs = when (playlistId) {
                        "favorites" -> libraryUiState.songsList.filter { libraryUiState.favoriteSongIds.contains(it.id) }
                        "recent" -> {
                            val played = libraryUiState.songsList
                                .filter { (libraryUiState.playStatsMap[it.id]?.lastPlayed ?: 0) > 0 }
                                .sortedByDescending { libraryUiState.playStatsMap[it.id]?.lastPlayed ?: 0 }
                            if (played.isNotEmpty()) played.take(50) else libraryUiState.songsList.sortedByDescending { it.dateAdded }.take(50)
                        }
                        "most_played" -> {
                            val played = libraryUiState.songsList
                                .filter { (libraryUiState.playStatsMap[it.id]?.playCount ?: 0) > 0 }
                                .sortedByDescending { libraryUiState.playStatsMap[it.id]?.playCount ?: 0 }
                            if (played.isNotEmpty()) played.take(50) else libraryUiState.songsList.sortedByDescending { it.dateAdded }.take(50)
                        }
                        else -> playlist?.let { p ->
                            p.songIds.mapNotNull { id -> libraryUiState.songsList.find { it.id == id } }
                        } ?: emptyList()
                    }
                    if (smartSongs.isNotEmpty() && index in smartSongs.indices) {
                        playerViewModel.playSongAtIndex(index, smartSongs)
                    }
                },
                onRemoveSongFromPlaylist = { playlistId, songId ->
                    libraryViewModel.removeSongFromPlaylist(playlistId, songId)
                },
                onAddSongToPlaylist = { playlistId, songId ->
                    libraryViewModel.addSongToPlaylist(playlistId, songId)
                },
                onDeletePlaylist = { libraryViewModel.deletePlaylist(it) }
            )
        }

        composableWithFadeTransition(Screen.Theme.route) {
            ThemeScreen(
                settingsUiState = settingsUiState,
                onBackClick = { navController.popBackStack() },
                onSelectTheme = { settingsViewModel.setThemeMode(it) },
                onSelectBackgroundImage = { settingsViewModel.setBackgroundImageUri(it) },
                onChangeBlur = { settingsViewModel.setBackgroundBlur(it) },
                onChangeOpacity = { settingsViewModel.setBackgroundOpacity(it) }
            )
        }

        composableWithSlideTransition(Screen.TagEditor.route) {
            val currentSong = playerUiState.currentSong
            TagEditorScreen(
                song = currentSong,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onSongUpdated = { updated ->
                    playerViewModel.updateCurrentSongMetadata(updated)
                    libraryViewModel.refreshSongs()
                }
            )
        }
    }

    AnimatedVisibility(
        visible = playerUiState.isPlayerSheetExpanded,
        enter = fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(260, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(180)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerSheet(
                song = activeSong,
                isPlaying = playerUiState.isPlaying,
                currentPositionMs = playerUiState.currentPositionMs,
                durationMs = playerUiState.durationMs,
                playbackMode = playerUiState.playbackMode,
                albumArtMode = settingsUiState.userSettings.albumArtMode,
                onAlbumArtModeChange = { settingsViewModel.setAlbumArtMode(it) },
                isFavorite = activeSong?.let { libraryUiState.favoriteSongIds.contains(it.id) } ?: false,
                onDismiss = { playerViewModel.setPlayerSheetExpanded(false) },
                onSeek = { playerViewModel.seekTo(it) },
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onPreviousClick = { playerViewModel.skipPrevious() },
                onNextClick = { playerViewModel.skipNext() },
                onTogglePlaybackMode = { playerViewModel.togglePlaybackMode() },
                onToggleFavorite = { activeSong?.let { song -> libraryViewModel.toggleFavorite(song.id) } },
                onAddToPlaylistClick = { activeSong?.let { song -> songForPlaylistDialog = song } },
                onEqualizerClick = {
                    currentTab = BottomNavTab.EQUALIZER
                    playerViewModel.setPlayerSheetExpanded(false)
                },
                onQueueClick = {
                    navController.navigate(Screen.Queue.route)
                    playerViewModel.setPlayerSheetExpanded(false)
                },
                onTagEditorClick = {
                    navController.navigate(Screen.TagEditor.route)
                    playerViewModel.setPlayerSheetExpanded(false)
                },
                onAudioInfoClick = { /* show audio info dialog - handled via state */ },
                onShareClick = { playerViewModel.shareCurrentSong() },
                lyricsRaw = playerUiState.lyricsRaw,
                onSeekToLine = { timeMs -> playerViewModel.seekTo(timeMs) },
                audioSessionId = playerUiState.audioSessionId
            )
        }
    }
}

private fun shareSongs(context: Context, songs: List<Song>) {
    if (songs.isEmpty()) return
    val body = songs.joinToString("\n") { song -> "${song.title}\n${song.artist}" }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir canciones"))
}
