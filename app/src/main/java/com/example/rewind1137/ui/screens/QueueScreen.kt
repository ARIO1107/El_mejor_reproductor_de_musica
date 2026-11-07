package com.example.rewind1137.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.example.rewind1137.R
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.ui.components.AlbumArtThumbnail
import com.example.rewind1137.ui.components.DynamicBackground
import com.example.rewind1137.ui.components.EmptyStateIllustration
import com.example.rewind1137.ui.components.GlassmorphicContainer
import com.example.rewind1137.viewmodel.LibraryUiState
import com.example.rewind1137.viewmodel.PlaybackMode
import com.example.rewind1137.viewmodel.PlayerUiState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    libraryUiState: LibraryUiState,
    playerUiState: PlayerUiState,
    onBackClick: () -> Unit,
    onPlaySongAtIndex: (Int) -> Unit,
    onPlayPauseClick: () -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onRemoveItem: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onAddToPlaylistClick: (Song) -> Unit = {},
    onPlayNextClick: (Song) -> Unit = {},
    onPlayLastClick: (Song) -> Unit = {},
    onSaveQueueAsPlaylist: (String) -> Unit = {}
) {
    val activeSong = playerUiState.currentSong
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val displayQueue = playerUiState.displayQueue

    var queueMenuSong by remember { mutableStateOf<Song?>(null) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var queuePlaylistName by remember { mutableStateOf("") }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LaunchedEffect(activeSong?.id) {
        if (activeSong != null) {
            val index = displayQueue.indexOfFirst { it.id == activeSong.id }
            if (index >= 0) lazyListState.animateScrollToItem(index)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = activeSong?.albumArtUri)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.queue_screen_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${displayQueue.size} canciones • ${formatTotalDuration(displayQueue)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Active Song Hero Card
                activeSong?.let { song ->
                    GlassmorphicContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        cornerRadius = 16.dp,
                        blurRadius = 20.dp,
                        glassAlpha = 0.40f,
                        borderAlpha = 0.20f
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtThumbnail(
                                albumArtUri = song.albumArtUri,
                                size = 48.dp,
                                shapeRadius = 10.dp,
                                isCurrentSong = true,
                                songId = song.id,
                                contentUri = song.uri,
                                dateModified = song.dateAdded
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "REWINDDUCIENDO AHORA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${formatDuration(song.duration)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onPlayPauseClick) {
                                AnimatedContent(
                                    targetState = playerUiState.isPlaying,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(180)))
                                            .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)))
                                    },
                                    label = "playPauseIcon"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.play_pause),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Mode Selector Pill
                GlassmorphicContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    cornerRadius = 14.dp,
                    blurRadius = 16.dp,
                    glassAlpha = 0.35f,
                    borderAlpha = 0.18f
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePlaybackMode() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val modeIcon = when (playerUiState.playbackMode) {
                                    PlaybackMode.SEQUENTIAL -> Icons.Default.Repeat
                                    PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
                                    PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (playerUiState.playbackMode) {
                                            PlaybackMode.SHUFFLE -> "A continuación (Aleatorio / Mezclado)"
                                            PlaybackMode.REPEAT_ONE -> "A continuación (Repetir canción actual)"
                                            PlaybackMode.SEQUENTIAL -> "A continuación (Orden secuencial)"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Toca para cambiar el modo de la cola",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (displayQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateIllustration(
                            imageVector = Icons.Default.QueueMusic,
                            title = stringResource(R.string.queue_empty_title),
                            subtitle = stringResource(R.string.queue_empty_subtitle),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = displayQueue,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            val isCurrent = song.id == activeSong?.id

                            ReorderableItem(reorderableState, key = song.id) { isDragging ->
                                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(0, -elevation.roundToPx()) }
                                        .clickable { onPlaySongAtIndex(index) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(24.dp)
                                        )

                                        Box(contentAlignment = Alignment.Center) {
                                            AlbumArtThumbnail(
                                                albumArtUri = song.albumArtUri,
                                                size = 40.dp,
                                                shapeRadius = 8.dp,
                                                isCurrentSong = isCurrent,
                                                songId = song.id,
                                                contentUri = song.uri,
                                                dateModified = song.dateAdded
                                            )
                                            if (isCurrent && playerUiState.isPlaying) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black.copy(alpha = 0.45f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${song.artist} • ${formatDuration(song.duration)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box {
                                            IconButton(
                                                onClick = { queueMenuSong = song },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.options),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = queueMenuSong?.id == song.id,
                                                onDismissRequest = { queueMenuSong = null },
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Añadir a lista") },
                                                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                                                    onClick = {
                                                        onAddToPlaylistClick(song)
                                                        queueMenuSong = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.queue_play_next)) },
                                                    leadingIcon = { Icon(Icons.Default.SkipNext, null) },
                                                    onClick = {
                                                        onPlayNextClick(song)
                                                        queueMenuSong = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.queue_remove_from_queue)) },
                                                    leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        onRemoveItem(index)
                                                        queueMenuSong = null
                                                    }
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier
                                                .size(36.dp)
                                                .draggableHandle(
                                                    onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = stringResource(R.string.drag_to_reorder),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(180.dp)) }
                    }
                }
            }
        }
    }

    if (showSaveQueueDialog) {
        AlertDialog(
            onDismissRequest = { showSaveQueueDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.queue_save_as_playlist),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                OutlinedTextField(
                    value = queuePlaylistName,
                    onValueChange = { queuePlaylistName = it },
                    label = { Text(stringResource(R.string.queue_playlist_name_label), color = MaterialTheme.colorScheme.onBackground) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (queuePlaylistName.isNotBlank()) {
                            onSaveQueueAsPlaylist(queuePlaylistName.trim())
                            queuePlaylistName = ""
                            showSaveQueueDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveQueueDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Limpiar cola", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("¿Deseas vaciar la cola de rewindducción actual?", color = MaterialTheme.colorScheme.onBackground) },
            confirmButton = {
                TextButton(onClick = {
                    onClearQueue()
                    showClearConfirmDialog = false
                }) {
                    Text("Limpiar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private fun formatTotalDuration(songs: List<Song>): String {
    val totalMs = songs.sumOf { it.duration }
    val totalMinutes = totalMs / (1000 * 60)
    return if (totalMinutes >= 60) {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        "${hours}h ${mins}m"
    } else {
        "${totalMinutes} min"
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
}
