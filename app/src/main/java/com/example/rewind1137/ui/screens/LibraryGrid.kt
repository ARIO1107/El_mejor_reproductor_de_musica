package com.example.rewind1137.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.ui.components.AlbumArtThumbnail
import com.example.rewind1137.ui.components.EmptyStateIllustration
import com.example.rewind1137.ui.components.MultiSelectActionBar
import com.example.rewind1137.ui.components.NowPlayingIndicator
import com.example.rewind1137.ui.components.SelectionBadge
import com.example.rewind1137.ui.components.SongItem
import com.example.rewind1137.utils.ThumbnailPrefetcher
import com.example.rewind1137.viewmodel.LibraryFilter
import com.example.rewind1137.viewmodel.LibraryUiState
import com.example.rewind1137.viewmodel.NowPlayingAnimation
import com.example.rewind1137.viewmodel.PlayerUiState
import com.example.rewind1137.viewmodel.SortOption
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

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
    val context = LocalContext.current

    LaunchedEffect(isMultiSelectMode) {
        onSelectionModeChange(isMultiSelectMode)
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChange(false) }
    }

    // Precarga de portadas: primero la ventana visible y lo que viene encima,
    // y despues el resto en segundo plano. Con esto la cuadricula de 5 columnas
    // se desplaza sin cargas visibles.
    LaunchedEffect(isGridView, libraryUiState.filteredSongs) {
        val songs = libraryUiState.filteredSongs
        if (songs.isEmpty()) return@LaunchedEffect

        ThumbnailPrefetcher.prefetch(context, songs.take(160), highPriority = false)

        snapshotFlow<Pair<Int, Int>?> {
            if (isGridView) {
                val visible = gridState.layoutInfo.visibleItemsInfo
                if (visible.isEmpty()) null else visible.first().index to visible.last().index
            } else {
                val visible = listState.layoutInfo.visibleItemsInfo
                if (visible.isEmpty()) null else visible.first().index to visible.last().index
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { range ->
                val from = (range.first - 10).coerceAtLeast(0)
                val to = (range.second + 40).coerceAtMost(songs.lastIndex)
                if (from <= to) {
                    ThumbnailPrefetcher.prefetch(
                        context = context,
                        songs = songs.subList(from, to + 1),
                        highPriority = true
                    )
                }
            }
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
    val outline = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tu Biblioteca",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$songCount canciones",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Surface(
            shape = RoundedCornerShape(13.dp),
            color = containerColor.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, outline.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedTool(
                    icon = if (isGridView) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                    label = null,
                    contentDescription = if (isGridView) "Cambiar a lista" else "Cambiar a cuadricula",
                    selected = isGridView,
                    accent = accent,
                    onClick = onToggleView
                )
                if (!isGridView) {
                    SegmentedTool(
                        icon = Icons.Default.ViewStream,
                        label = null,
                        contentDescription = if (compactList) "Desactivar modo compacto" else "Activar modo compacto",
                        selected = compactList,
                        accent = accent,
                        onClick = onCompactListChange
                    )
                }
                if (isGridView) {
                    SegmentedTool(
                        icon = null,
                        label = "${gridColumns}C",
                        contentDescription = "Columnas: $gridColumns",
                        selected = true,
                        accent = accent,
                        onClick = onGridColumnsChange
                    )
                }
                Box {
                    SegmentedTool(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        label = null,
                        contentDescription = "Ordenar canciones",
                        selected = showSortMenu,
                        accent = accent,
                        onClick = onShowSortMenu
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = onDismissSortMenu,
                        modifier = Modifier.border(1.dp, outline, RoundedCornerShape(16.dp)),
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

@Composable
private fun SegmentedTool(
    icon: ImageVector?,
    label: String?,
    contentDescription: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val container by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(200),
        label = "segmentedContainer"
    )
    val foreground by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            selected -> accent
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "segmentedForeground"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = container,
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 30.dp)
                .padding(horizontal = if (label == null) 0.dp else 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(17.dp),
                    tint = foreground
                )
                if (label != null) {
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
            if (label != null) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    color = foreground
                )
            }
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
            .padding(top = 6.dp, bottom = 10.dp),
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
