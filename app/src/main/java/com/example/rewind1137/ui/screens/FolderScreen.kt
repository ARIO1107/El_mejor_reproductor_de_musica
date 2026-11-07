package com.example.rewind1137.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.rewind1137.data.database.entities.CacheMetadataEntity
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.ui.components.AlbumArtThumbnail
import com.example.rewind1137.ui.components.DynamicBackground
import com.example.rewind1137.viewmodel.LibraryUiState

enum class FolderTab(val label: String) {
    FAVORITES("Me Gusta"),
    FOLDERS("Carpetas"),
    ALBUMS("Albumes"),
    ARTISTS("Artistas"),
    GENRES("Generos"),
    RECENT("Recientes"),
    MOST_PLAYED("Mas Escuchadas"),
    HISTORY("Historial")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    libraryUiState: LibraryUiState,
    onBackClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit = { _, _ -> },
    playStatsMap: Map<Long, com.example.rewind1137.data.database.entities.CacheMetadataEntity> = emptyMap()
) {
    var selectedTab by remember { mutableStateOf(FolderTab.FAVORITES) }

    val context = LocalContext.current
    var treeUri by remember { mutableStateOf<Uri?>(null) }
    var navStack by remember { mutableStateOf(listOf<DocumentFile>()) }

    val rootDoc = remember(treeUri) {
        treeUri?.let { uri ->
            runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()
        }
    }
    val currentDir = navStack.lastOrNull() ?: rootDoc

    val children = remember(treeUri, navStack, rootDoc) {
        currentDir?.listFiles()?.toList() ?: emptyList()
    }
    val subFolders = children.filter { it.isDirectory }
    val audioDocs = children.filter { it.isFile && isAudioDocument(it) }
    val folderSongs = remember(audioDocs) { audioDocs.map { documentToSong(it) } }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            treeUri = uri
            navStack = emptyList()
        }
    }

    val favoriteSongs = remember(libraryUiState.songsList, libraryUiState.favoriteSongIds) {
        libraryUiState.songsList.filter { libraryUiState.favoriteSongIds.contains(it.id) }
    }

    val recentSongs = remember(libraryUiState.songsList) {
        libraryUiState.songsList.sortedByDescending { it.dateAdded }
    }

    val activeSong = libraryUiState.filteredSongs.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = activeSong?.albumArtUri)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Carpetas y Listas Inteligentes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
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
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    FolderTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedTab) {
                    FolderTab.FAVORITES -> {
                        if (favoriteSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aún no has marcado canciones como favoritas.\n\nToca el icono ♥ en el rewindductor para agregarlas a la playlist predeterminada me gusta.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(favoriteSongs) { _, song ->
                                    SongItemRow(song = song, onClick = { onPlaySong(song) })
                                }
                            }
                        }
                    }
                    FolderTab.FOLDERS -> {
                        if (treeUri == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { folderPicker.launch(null) }
                                                .padding(horizontal = 24.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Elegir carpeta del dispositivo",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Explora cualquier carpeta de tu teléfono\n(selecciona la raíz o una subcarpeta)\ny rewindduce sus canciones directamente.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        } else if (currentDir == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se pudo acceder a la carpeta.\nPodría haber sido revocada o borrada.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            if (navStack.isNotEmpty()) navStack = navStack.dropLast(1)
                                        },
                                        enabled = navStack.isNotEmpty(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Subir",
                                            modifier = Modifier.padding(horizontal = 12.dp).wrapContentHeight(),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val rootLabel = treeUri?.lastPathSegment?.substringAfterLast(':') ?: "carpeta"
                                    Text(
                                        text = navStack.lastOrNull()?.name ?: "Raíz ($rootLabel)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (subFolders.isEmpty() && folderSongs.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Carpeta vacía",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    subFolders.forEachIndexed { index, folder ->
                                        item(key = "folder_$index") {
                                            FolderItemCard(
                                                folderName = folder.name ?: "Carpeta",
                                                songCount = (folder.listFiles()?.count { it.isFile && isAudioDocument(it) } ?: 0).coerceAtLeast(0),
                                                onFolderClick = { navStack = navStack + folder }
                                            )
                                        }
                                    }

                                    itemsIndexed(folderSongs, key = { index, _ -> "audio_$index" }) { index, song ->
                                        SongItemRow(song = song, onClick = { onPlayQueue(folderSongs, index) })
                                    }
                                }
                            }
                        }
                    }
                    FolderTab.ALBUMS -> {
                        val grouped = libraryUiState.songsList.groupBy { it.album }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grouped.forEach { (albumName, songs) ->
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { songs.firstOrNull()?.let { onPlaySong(it) } }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = albumName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${songs.size} canciones",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(songs.size) { index ->
                                    val song = songs[index]
                                    SongItemRow(song = song, onClick = { onPlaySong(song) })
                                }
                            }
                        }
                    }
                    FolderTab.ARTISTS -> {
                        val grouped = libraryUiState.songsList.groupBy { it.artist }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grouped.forEach { (artistName, songs) ->
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { songs.firstOrNull()?.let { onPlaySong(it) } }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = artistName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${songs.size} canciones",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(songs.size) { index ->
                                    val song = songs[index]
                                    SongItemRow(song = song, onClick = { onPlaySong(song) })
                                }
                            }
                        }
                    }
                    FolderTab.GENRES -> {
                        val grouped = libraryUiState.songsList.groupBy { it.genre }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grouped.forEach { (genreName, songs) ->
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { songs.firstOrNull()?.let { onPlaySong(it) } }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = genreName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${songs.size} canciones",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(songs.size) { index ->
                                    val song = songs[index]
                                    SongItemRow(song = song, onClick = { onPlaySong(song) })
                                }
                            }
                        }
                    }
                    FolderTab.RECENT -> {
                        if (recentSongs.isEmpty()) {
                            TabEmptyState(
                                title = "Nada reciente",
                                subtitle = "Aquí verás las canciones añadidas recientemente a tu biblioteca."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(recentSongs) { _, song ->
                                    SongItemRow(
                                        song = song,
                                        onClick = { onPlaySong(song) },
                                        subtitle = song.artist.ifBlank { song.album }
                                    )
                                }
                            }
                        }
                    }
                    FolderTab.MOST_PLAYED -> {
                        val mostPlayed = remember(libraryUiState.songsList, playStatsMap) {
                            libraryUiState.songsList
                                .mapNotNull { song ->
                                    val count = playStatsMap[song.id]?.playCount ?: 0
                                    if (count > 0) song to count else null
                                }
                                .sortedByDescending { it.second }
                        }
                        if (mostPlayed.isEmpty()) {
                            TabEmptyState(
                                title = "Aún no hay estadísticas",
                                subtitle = "Reproduce tus canciones y aquí verás las que más escuchas."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(mostPlayed, key = { _, item -> "most_${item.first.id}" }) { index, item ->
                                    SongItemRow(
                                        song = item.first,
                                        onClick = { onPlaySong(item.first) },
                                        rank = index + 1,
                                        subtitle = item.first.artist.ifBlank { item.first.album },
                                        trailingText = if (index < 3) "★" else null
                                    )
                                }
                            }
                        }
                    }
                    FolderTab.HISTORY -> {
                        val history = remember(libraryUiState.songsList, playStatsMap) {
                            libraryUiState.songsList
                                .mapNotNull { song ->
                                    val last = playStatsMap[song.id]?.lastPlayed ?: 0L
                                    if (last > 0L) song to last else null
                                }
                                .sortedByDescending { it.second }
                        }
                        if (history.isEmpty()) {
                            TabEmptyState(
                                title = "Sin historial",
                                subtitle = "Las canciones que rewindduzcas aparecerán aquí ordenadas por fecha."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(history, key = { _, item -> "hist_${item.first.id}" }) { _, item ->
                                    SongItemRow(
                                        song = item.first,
                                        onClick = { onPlaySong(item.first) },
                                        subtitle = item.first.artist.ifBlank { item.first.album },
                                        trailingText = formatRelativeTime(item.second)
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

@Composable
private fun TabEmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun FolderItemCard(
    folderName: String,
    songCount: Int,
    onFolderClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFolderClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$songCount canciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    onClick: () -> Unit,
    rank: Int? = null,
    subtitle: String? = null,
    trailingText: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rank != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            AlbumArtThumbnail(
                albumArtUri = song.albumArtUri,
                size = 44.dp,
                shapeRadius = 8.dp,
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
                    text = subtitle ?: song.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Hace un momento"
        diff < 3_600_000L -> "Hace ${diff / 60_000L} min"
        diff < 86_400_000L -> "Hace ${diff / 3_600_000L} h"
        diff < 604_800_000L -> "Hace ${diff / 86_400_000L} d"
        else -> {
            val fmt = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
            fmt.format(java.util.Date(timestamp))
        }
    }
}

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "flac", "wav", "ogg", "m4a", "aac", "opus", "amr", "mid", "midi", "mp2", "3gp", "wma", "ts"
)

private fun isAudioDocument(doc: DocumentFile): Boolean {
    val mime = doc.type?.lowercase() ?: ""
    return mime.startsWith("audio/") ||
        (doc.name?.substringAfterLast('.', "")?.lowercase() in AUDIO_EXTENSIONS)
}

private fun documentToSong(doc: DocumentFile): Song {
    return Song(
        id = 0L,
        title = doc.name?.substringBeforeLast('.') ?: "Audio",
        artist = "",
        uri = doc.uri,
        duration = 0L,
        albumId = 0L,
        albumArtUri = null,
        dateAdded = System.currentTimeMillis(),
        album = "",
        genre = ""
    )
}
