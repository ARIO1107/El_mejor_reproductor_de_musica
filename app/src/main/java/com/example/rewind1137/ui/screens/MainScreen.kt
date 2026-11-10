package com.example.rewind1137.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.rewind1137.ui.components.*
import com.example.rewind1137.audio.AudioEffectManager
import com.example.rewind1137.data.model.EqualizerPreset
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.viewmodel.LibraryUiState
import com.example.rewind1137.viewmodel.NowPlayingAnimation
import com.example.rewind1137.viewmodel.PlayerUiState
import com.example.rewind1137.viewmodel.SortOption
import com.example.rewind1137.viewmodel.LibraryFilter
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    libraryUiState: LibraryUiState,
    playerUiState: PlayerUiState,
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    onPlaySongAtIndex: (Int) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onCompactListChange: (Boolean) -> Unit = {},
    onFilterSelected: (LibraryFilter) -> Unit = {},
    audioEffectManager: AudioEffectManager? = null,
    eqUserPresets: List<EqualizerPreset> = emptyList(),
    eqActivePresetId: String = "",
    onEqSavePreset: (EqualizerPreset) -> Unit = {},
    onEqDeletePreset: (String) -> Unit = {},
    onEqSelectPreset: (String) -> Unit = {},
    onEqUpdatePreset: (EqualizerPreset) -> Unit = {},
    onToggleEqualizer: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onOpenPlayerSheet: (Rect?) -> Unit = {},
    onMultiSelectAction: ((action: String, songs: List<Song>) -> Unit)? = null,
    initialGridColumns: Int = 2,
    onPlaylistClick: (() -> Unit)? = null,
    onRefresh: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onSmartPlaylistClick: (String) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS
) {
    var gridColumns by remember { mutableIntStateOf(initialGridColumns) }
    var isSelectionMode by remember { mutableStateOf(false) }

    LaunchedEffect(initialGridColumns) {
        gridColumns = initialGridColumns
    }

    val activeSong = playerUiState.currentSong
    val hasActiveSong = activeSong != null

    val tabOrder = listOf(BottomNavTab.HOME, BottomNavTab.SEARCH, BottomNavTab.LIBRARY, BottomNavTab.EQUALIZER)
    val pagerState = rememberPagerState(pageCount = { tabOrder.size })
    val coroutineScope = rememberCoroutineScope()

    val currentIndex = tabOrder.indexOf(currentTab).coerceAtLeast(0)

    LaunchedEffect(currentTab) {
        val targetPage = tabOrder.indexOf(currentTab).coerceAtLeast(0)
        if (pagerState.currentPage != targetPage) {
            val jump = kotlin.math.abs(targetPage - pagerState.currentPage)
            if (jump > 1) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val newTab = tabOrder[pagerState.currentPage]
        if (newTab != currentTab) {
            onTabSelected(newTab)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = activeSong?.albumArtUri)

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.statusBarsPadding())

            TopBarRewind(
                onSettingsClick = onSettingsClick
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> LibraryGrid(
                        libraryUiState = libraryUiState,
                        playerUiState = playerUiState,
                        gridColumns = gridColumns,
                        onCompactListChange = { onCompactListChange(!libraryUiState.isCompactMode) },
                        onPlaySongAtIndex = onPlaySongAtIndex,
                        onSortOptionSelected = onSortOptionSelected,
                        onGridColumnsChange = { gridColumns = it; onGridColumnsChange(it) },
                        onFilterSelected = onFilterSelected,
                        onPlaylistClick = { playlistId -> onSmartPlaylistClick(playlistId) },
                        onMultiSelectAction = onMultiSelectAction,
                        onRefresh = onRefresh,
                        nowPlayingAnimation = nowPlayingAnimation,
                        onSelectionModeChange = { isSelectionMode = it }
                    )
                    1 -> SearchPage(
                        libraryUiState = libraryUiState,
                        playerUiState = playerUiState,
                        onSearchQueryChange = onSearchQueryChange,
                        onPlaySongAtIndex = onPlaySongAtIndex,
                        onClearRecentSearches = onClearRecentSearches,
                        nowPlayingAnimation = nowPlayingAnimation
                    )
                    2 -> PlaylistsView(
                        songs = libraryUiState.songsList,
                        favoriteSongIds = libraryUiState.favoriteSongIds,
                        customPlaylists = libraryUiState.customPlaylists,
                        playStatsMap = libraryUiState.playStatsMap,
                        onCreatePlaylist = onCreatePlaylist,
                        onPlaylistClick = onSmartPlaylistClick,
                        onSongClick = onSongClick
                    )
                    3 -> if (audioEffectManager != null) {
                        EqualizerScreen(
                            audioEffectManager = audioEffectManager,
                            onBackClick = { onTabSelected(BottomNavTab.HOME) },
                            userPresets = eqUserPresets,
                            activePresetId = eqActivePresetId,
                            onSavePreset = onEqSavePreset,
                            onDeletePreset = onEqDeletePreset,
                            onSelectPreset = onEqSelectPreset,
                            onUpdatePreset = onEqUpdatePreset,
                            onToggleEqualizer = onToggleEqualizer
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = hasActiveSong && !isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(300))
            ) {
                if (hasActiveSong) {
                    Column {
                        MiniPlayer(
                            song = activeSong!!,
                            isPlaying = playerUiState.isPlaying,
                            currentPositionMs = playerUiState.currentPositionMs,
                            durationMs = playerUiState.durationMs,
                            playbackMode = playerUiState.playbackMode,
                            onSeek = onSeek,
                            onPlayPauseClick = onPlayPauseClick,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                            onTogglePlaybackMode = onTogglePlaybackMode,
                            onOpenPlayerSheet = onOpenPlayerSheet
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            CustomBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = onTabSelected
            )
        }
    }
}
