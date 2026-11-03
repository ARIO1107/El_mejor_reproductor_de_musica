package com.example.re_pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.re_pro.data.model.Song
import com.example.re_pro.ui.components.AlbumArtThumbnail
import com.example.re_pro.ui.components.EmptyStateIllustration
import com.example.re_pro.ui.components.MultiSelectActionBar
import com.example.re_pro.ui.components.NowPlayingIndicator
import com.example.re_pro.ui.components.SelectionBadge
import com.example.re_pro.ui.components.SongItem
import com.example.re_pro.viewmodel.LibraryFilter
import com.example.re_pro.viewmodel.LibraryUiState
import com.example.re_pro.viewmodel.NowPlayingAnimation
import com.example.re_pro.viewmodel.PlayerUiState
import com.example.re_pro.viewmodel.SortOption

@Composable
fun LibraryGrid(
    libraryUiState: LibraryUiState,
    playerUiState: PlayerUiState,
    gridColumns: Int,
    onCompactListChange: (Boolean) -> Unit = { _ -> },
    onPlaySongAtIndex: (Int) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit = {},
    onPlaylistClick: ((String) -> Unit)? = null,
    onMultiSelectAction: ((action: String, songs: List<Song>) -> Unit)? = null,
    onRefresh: () -> Unit = {},
    nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS,
    onSelectionModeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isGridView = gridColumns > 1
    val compactList = libraryUiState.isCompactMode
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(setOf<Long>()) }
    val isMultiSelectMode = selectedSongs.isNotEmpty()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(isMultiSelectMode) {
        onSelectionModeChange(isMultiSelectMode)
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChange(false) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            LibraryHeader(
                songCount = libraryUiState.filteredSongs.size,
                isGridView = isGridView,
                gridColumns = gridColumns,
                compactList = compactList,
                onCompactListChange = { onCompactListChange(!compactList) },
                showSortMenu = showSortMenu,
                onToggleView = {
                    onGridColumnsChange(if (isGridView) 1 else 2)
                },
                onGridColumnsChange = {
                    val nextCols = if (gridColumns < 5) gridColumns + 1 else 2
                    onGridColumnsChange(nextCols)
                },
                onShowSortMenu = { showSortMenu = true },
                onDismissSortMenu = { showSortMenu = false },
                onSortOptionSelected = {
                    onSortOptionSelected(it)
                    showSortMenu = false
                }
            )

            LibraryFilters(
                libraryUiState = libraryUiState,
                onFilterSelected = onFilterSelected,
                onPlaylistClick = onPlaylistClick
            )

            if (libraryUiState.filteredSongs.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyStateIllustration(
                        imageVector = Icons.Filled.MusicOff,
                        title = "No hay canciones",
                        subtitle = if (libraryUiState.searchQuery.isNotBlank()) {
                            "No se encontraron resultados para \"${libraryUiState.searchQuery}\""
                        } else {
                            "Escanea tu biblioteca desde Ajustes o agrega archivos de musica"
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns.coerceIn(2, 5)),
                    state = gridState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(libraryUiState.filteredSongs.size, key = { libraryUiState.filteredSongs[it].id }) { index ->
                        val song = libraryUiState.filteredSongs[index]
                        val isSelected = song.id in selectedSongs
                        val isCurrent = song.id == playerUiState.currentSong?.id
                        val itemLongClick = remember(song.id, isSelected) {
                            {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedSongs = if (isSelected) selectedSongs - song.id else selectedSongs + song.id
                            }
                        }
                        val itemClick = remember(song.id, isSelected, isMultiSelectMode, index) {
                            {
                                if (isMultiSelectMode) {
                                    selectedSongs = if (isSelected) selectedSongs - song.id else selectedSongs + song.id
                                } else {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlaySongAtIndex(index)
                                }
                            }
                        }
                        LibraryGridItem(
                            song = song,
                            isSelected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            isCurrentSong = isCurrent,
                            isPlaying = playerUiState.isPlaying,
                            nowPlayingAnimation = nowPlayingAnimation,
                            onLongClick = itemLongClick,
                            onClick = itemClick
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(libraryUiState.filteredSongs.size, key = { libraryUiState.filteredSongs[it].id }) { index ->
                        val song = libraryUiState.filteredSongs[index]
                        val isSelected = song.id in selectedSongs
                        val isCurrent = song.id == playerUiState.currentSong?.id
                        val itemLongClick = remember(song.id, isSelected) {
                            {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedSongs = if (isSelected) selectedSongs - song.id else selectedSongs + song.id
                            }
                        }
                        val itemClick = remember(song.id, isSelected, isMultiSelectMode, index) {
                            {
                                if (isMultiSelectMode) {
                                    selectedSongs = if (isSelected) selectedSongs - song.id else selectedSongs + song.id
                                } else {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlaySongAtIndex(index)
                                }
                            }
                        }
                        StaggeredItem(index = index) {
                            SongItem(
                                song = song,
                                isCompact = compactList,
                                isMultiSelectMode = isMultiSelectMode,
                                isCurrentSong = isCurrent,
                                isPlaying = playerUiState.isPlaying,
                                isSelected = isSelected,
                                nowPlayingAnimation = nowPlayingAnimation,
                                onLongClick = itemLongClick,
                                onClick = itemClick
                            )
                        }
                    }
                }
            }
        }

        val selectedSongList = libraryUiState.filteredSongs.filter { it.id in selectedSongs }
        MultiSelectActionBar(
            selectedCount = selectedSongs.size,
            isMultiSelectMode = isMultiSelectMode,
            onClearSelection = { selectedSongs = emptySet() },
            onSelectAll = { selectedSongs = libraryUiState.filteredSongs.map { it.id }.toSet() },
            onPlayNow = {
                onMultiSelectAction?.invoke("play", selectedSongList)
                selectedSongs = emptySet()
            },
            onAddToPlaylist = {
                onMultiSelectAction?.invoke("playlist", selectedSongList)
                selectedSongs = emptySet()
            },
            onToggleFavorite = {
                onMultiSelectAction?.invoke("favorite", selectedSongList)
                selectedSongs = emptySet()
            },
            onShare = {
                onMultiSelectAction?.invoke("share", selectedSongList)
                selectedSongs = emptySet()
            },
            onDelete = {
                onMultiSelectAction?.invoke("delete", selectedSongList)
                selectedSongs = emptySet()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 92.dp)
        )
    }
}

@Composable
private fun LibraryHeader(
    songCount: Int,
    isGridView: Boolean,
    gridColumns: Int,
    compactList: Boolean,
    showSortMenu: Boolean,
    onToggleView: () -> Unit,
    onGridColumnsChange: () -> Unit,
    onCompactListChange: () -> Unit,
    onShowSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tu Biblioteca",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$songCount canciones",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = accent.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = accent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$songCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = containerColor.copy(alpha = 0.6f)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentedTool(
                        icon = if (isGridView) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                        label = if (isGridView) "Cuadricula" else "Lista",
                        selected = isGridView,
                        containerColor = containerColor,
                        accent = accent,
                        onClick = onToggleView
                    )
                    SegmentedTool(
                        icon = Icons.Default.ViewStream,
                        label = "Compacto",
                        selected = !isGridView && compactList,
                        containerColor = containerColor,
                        accent = accent,
                        onClick = onCompactListChange,
                        enabled = !isGridView
                    )
                    SegmentedTool(
                        icon = null,
                        label = "${gridColumns}C",
                        selected = isGridView,
                        containerColor = containerColor,
                        accent = accent,
                        onClick = onGridColumnsChange,
                        enabled = isGridView
                    )
                    Box {
                        SegmentedTool(
                            icon = Icons.AutoMirrored.Filled.Sort,
                            label = "Orden",
                            selected = false,
                            containerColor = containerColor,
                            accent = accent,
                            onClick = onShowSortMenu
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = onDismissSortMenu,
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, color = MaterialTheme.colorScheme.onBackground) },
                                    onClick = { onSortOptionSelected(option) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    selected: Boolean,
    containerColor: Color,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = fg
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg
            )
        }
    }
}

@Composable
private fun LibraryFilters(
    libraryUiState: LibraryUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onPlaylistClick: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryFilter.entries.forEach { filter ->
            val isSelected = libraryUiState.libraryFilter == filter
            val count = if (filter == LibraryFilter.FAVORITES) {
                libraryUiState.favoriteSongIds.size
            } else {
                libraryUiState.songsList.size
            }
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = "${filter.label} ($count)",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                },
                modifier = Modifier.height(28.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        libraryUiState.customPlaylists.forEach { playlist ->
            val isSelected = false
            FilterChip(
                selected = isSelected,
                onClick = { onPlaylistClick?.invoke(playlist.id) },
                label = {
                    Text(
                        text = "${playlist.name} (${playlist.songIds.size})",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                },
                modifier = Modifier.height(28.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300, delayMillis = index.coerceAtMost(12) * 30, easing = FastOutSlowInEasing)) +
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(300, delayMillis = index.coerceAtMost(12) * 30, easing = FastOutSlowInEasing)
            ),
        exit = fadeOut(animationSpec = tween(150)) +
            slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(150))
    ) {
        content()
    }
}

@Composable
fun LibraryGridItem(
    song: Song,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label = "gridSelection"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AlbumArtThumbnail(
                albumArtUri = song.albumArtUri,
                size = 100.dp,
                shapeRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val s = 1f - 0.04f * selectionProgress
                        scaleX = s
                        scaleY = s
                    },
                songId = song.id,
                contentUri = song.uri,
                dateModified = song.dateAdded
            )
            if (selectionProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f * selectionProgress)
                        )
                        .border(
                            width = (2.5f * selectionProgress).dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f * selectionProgress),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
            if (isCurrentSong && !isMultiSelectMode) {
                NowPlayingIndicator(
                    animation = nowPlayingAnimation,
                    color = MaterialTheme.colorScheme.primary,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
            if (isMultiSelectMode) {
                SelectionBadge(
                    selected = isSelected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = song.artist,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
