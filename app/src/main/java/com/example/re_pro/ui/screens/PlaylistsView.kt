package com.example.re_pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.re_pro.data.database.entities.CacheMetadataEntity
import com.example.re_pro.data.model.Song
import com.example.re_pro.ui.components.AlbumArtThumbnail
import com.example.re_pro.viewmodel.Playlist

data class SmartPlaylist(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val songCount: Int
)

@Composable
fun PlaylistsView(
    songs: List<Song>,
    favoriteSongIds: Set<Long>,
    customPlaylists: List<Playlist>,
    playStatsMap: Map<Long, CacheMetadataEntity>,
    onCreatePlaylist: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    val recentSongs = remember(songs, playStatsMap) {
        val played = songs.filter { (playStatsMap[it.id]?.lastPlayed ?: 0) > 0 }
            .sortedByDescending { playStatsMap[it.id]?.lastPlayed ?: 0 }
        if (played.isNotEmpty()) played.take(50) else songs.sortedByDescending { it.dateAdded }.take(50)
    }

    val mostPlayedSongs = remember(songs, playStatsMap) {
        val played = songs.filter { (playStatsMap[it.id]?.playCount ?: 0) > 0 }
            .sortedByDescending { playStatsMap[it.id]?.playCount ?: 0 }
        if (played.isNotEmpty()) played.take(50) else songs.sortedByDescending { it.dateAdded }.take(50)
    }

    val smartPlaylists = listOf(
        SmartPlaylist("favorites", "Favoritos", Icons.Default.Favorite, Color(0xFFE91E63), favoriteSongIds.size),
        SmartPlaylist("recent", "Recientes", Icons.Default.History, Color(0xFF2196F3), recentSongs.size),
        SmartPlaylist("most_played", "Más Escuchadas", Icons.Default.Fireplace, Color(0xFFFF9800), mostPlayedSongs.size)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Tus Listas de Reproducción",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gestiona tus favoritas y listas personalizadas",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Surface(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Crear",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Nueva Playlist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Diseña una lista de canciones a tu gusto",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Listas Inteligentes",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        itemsIndexed(smartPlaylists) { _, playlist ->
            SmartPlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) }
            )
        }

        if (customPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = "Playlists Creadas (${customPlaylists.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            itemsIndexed(customPlaylists) { _, playlist ->
                CustomPlaylistCard(
                    playlist = playlist,
                    allSongs = songs,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva Playlist", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nombre de la playlist", color = MaterialTheme.colorScheme.onBackground) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName.trim())
                            showCreateDialog = false
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) { Text("Crear", color = MaterialTheme.colorScheme.onPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SmartPlaylistCard(
    playlist: SmartPlaylist,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = playlist.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        playlist.icon,
                        contentDescription = null,
                        tint = playlist.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} canciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomPlaylistCard(
    playlist: Playlist,
    allSongs: List<Song>,
    onClick: () -> Unit
) {
    val firstSong = remember(playlist, allSongs) {
        playlist.songIds.firstOrNull()?.let { firstId -> allSongs.find { it.id == firstId } }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (firstSong != null) {
                AlbumArtThumbnail(
                    albumArtUri = firstSong.albumArtUri,
                    size = 48.dp,
                    shapeRadius = 12.dp,
                    isCurrentSong = false,
                    songId = firstSong.id,
                    contentUri = firstSong.uri,
                    dateModified = firstSong.dateAdded
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Default.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songIds.size} canciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
