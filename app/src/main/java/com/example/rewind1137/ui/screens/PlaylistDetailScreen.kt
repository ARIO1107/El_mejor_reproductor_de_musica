package com.example.rewind1137.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.ui.components.AlbumArtThumbnail
import com.example.rewind1137.ui.components.DynamicBackground
import com.example.rewind1137.viewmodel.LibraryUiState
import com.example.rewind1137.viewmodel.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    libraryUiState: LibraryUiState,
    onBackClick: () -> Unit,
    onPlaySongAtPosition: (Int) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit
) {
    val playlist = remember(playlistId, libraryUiState.customPlaylists) {
        libraryUiState.customPlaylists.find { it.id == playlistId }
    }

    val songs = remember(playlist, libraryUiState.songsList) {
        playlist?.let { p ->
            p.songIds.mapNotNull { id -> libraryUiState.songsList.find { it.id == id } }
        } ?: emptyList()
    }

    val smartSongs = remember(playlistId, libraryUiState.songsList, libraryUiState.favoriteSongIds, libraryUiState.playStatsMap) {
        when (playlistId) {
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
            else -> emptyList()
        }
    }

    val displaySongs = if (playlist != null) songs else smartSongs
    val playlistName = playlist?.name ?: when (playlistId) {
        "favorites" -> "Favoritos"
        "recent" -> "Recientes"
        "most_played" -> "Más Escuchadas"
        else -> playlistId
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val firstSong = displaySongs.firstOrNull()
        DynamicBackground(albumArtUri = firstSong?.albumArtUri)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = playlistName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        if (playlist != null) {
                            IconButton(onClick = { showAddSongsDialog = true }) {
                                Icon(
                                    Icons.Default.PlaylistAdd,
                                    contentDescription = "Añadir canciones",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar playlist",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Hero Banner Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (firstSong != null) {
                                AlbumArtThumbnail(
                                    albumArtUri = firstSong.albumArtUri,
                                    size = 110.dp,
                                    shapeRadius = 16.dp,
                                    isCurrentSong = false,
                                    songId = firstSong.id,
                                    contentUri = firstSong.uri,
                                    dateModified = firstSong.dateAdded
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.size(110.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = playlistName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${displaySongs.size} canciones • ${formatTotalDuration(displaySongs)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { if (displaySongs.isNotEmpty()) onPlaySongAtPosition(0) },
                                    enabled = displaySongs.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reproducir", fontWeight = FontWeight.Bold)
                                }

                                if (playlist != null) {
                                    OutlinedButton(
                                        onClick = { showAddSongsDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Añadir", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (displaySongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Esta playlist está vacía",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (playlist != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { showAddSongsDialog = true }) {
                                        Text("+ Añadir canciones desde tu biblioteca", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(displaySongs, key = { _, song -> song.id }) { index, song ->
                        Surface(
                            onClick = { onPlaySongAtPosition(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                AlbumArtThumbnail(
                                    albumArtUri = song.albumArtUri,
                                    size = 44.dp,
                                    shapeRadius = 10.dp,
                                    isCurrentSong = false,
                                    songId = song.id,
                                    contentUri = song.uri,
                                    dateModified = song.dateAdded
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
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
                                if (playlist != null) {
                                    IconButton(onClick = { onRemoveSongFromPlaylist(playlist.id, song.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Quitar",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsDialog && playlist != null) {
        AddSongsToPlaylistDialog(
            playlistName = playlist.name,
            allSongs = libraryUiState.songsList,
            playlistSongIds = playlist.songIds,
            onDismiss = { showAddSongsDialog = false },
            onToggleSong = { songId, isCurrentlyIn ->
                if (isCurrentlyIn) {
                    onRemoveSongFromPlaylist(playlist.id, songId)
                } else {
                    onAddSongToPlaylist(playlist.id, songId)
                }
            }
        )
    }

    if (showDeleteDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar playlist", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("¿Eliminar \"${playlist.name}\"? Esta acción no se puede deshacer.", color = MaterialTheme.colorScheme.onBackground) },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(playlist.id)
                    showDeleteDialog = false
                    onBackClick()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsToPlaylistDialog(
    playlistName: String,
    allSongs: List<Song>,
    playlistSongIds: Set<Long>,
    onDismiss: () -> Unit,
    onToggleSong: (songId: Long, isInPlaylist: Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSongs = remember(allSongs, searchQuery) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Añadir Canciones", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("Playlist: $playlistName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar canción") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                if (filteredSongs.isEmpty()) {
                    Text("No se encontraron canciones.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(filteredSongs, key = { _, song -> song.id }) { _, song ->
                            val isIn = playlistSongIds.contains(song.id)
                            Surface(
                                onClick = { onToggleSong(song.id, isIn) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isIn,
                                        onCheckedChange = { onToggleSong(song.id, isIn) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                                        Text(song.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
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
