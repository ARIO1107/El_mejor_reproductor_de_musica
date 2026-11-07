package com.example.rewind1137.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.ui.components.EmptyStateIllustration
import com.example.rewind1137.ui.components.SongItem
import com.example.rewind1137.viewmodel.LibraryUiState
import com.example.rewind1137.viewmodel.NowPlayingAnimation
import com.example.rewind1137.viewmodel.PlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    libraryUiState: LibraryUiState,
    playerUiState: PlayerUiState,
    onSearchQueryChange: (String) -> Unit,
    onPlaySongAtIndex: (Int) -> Unit,
    onClearRecentSearches: () -> Unit = {},
    nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf(libraryUiState.searchQuery) }
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(300)
            .collect { q -> onSearchQueryChange(q) }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text ->
                searchQuery = text
                onSearchQueryChange(text)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun launchVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para buscar música")
            }
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Reconocimiento de voz no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Buscar Música",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por título o artista...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { launchVoiceSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Buscar por voz",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (searchQuery.isBlank() && libraryUiState.recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Búsquedas recientes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClearRecentSearches) {
                    Text("Limpiar", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(libraryUiState.recentSearches, key = { it }) { q ->
                    FilterChip(
                        selected = false,
                        onClick = { searchQuery = q },
                        label = {
                            Text(
                                text = q,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        if (libraryUiState.filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateIllustration(
                    imageVector = Icons.Filled.SearchOff,
                    title = if (searchQuery.isBlank()) "Buscar música" else "No se encontraron canciones",
                    subtitle = if (searchQuery.isBlank())
                        "Escribe el nombre de una canción o artista"
                        else "No hay resultados para \"$searchQuery\"",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            val query = searchQuery.trim()
            val artists = remember(query, libraryUiState.songsList) {
                libraryUiState.songsList
                    .map { it.artist }
                    .filter { it.isNotBlank() && it.contains(query, ignoreCase = true) }
                    .distinct()
            }
            val albums = remember(query, libraryUiState.songsList) {
                libraryUiState.songsList
                    .map { it.album }
                    .filter { it.isNotBlank() && it.contains(query, ignoreCase = true) }
                    .distinct()
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (artists.isNotEmpty()) {
                    item(key = "header_artists") { SectionHeader("Artistas") }
                    items(artists, key = { "artist_$it" }) { artist ->
                        SuggestionRow(
                            icon = Icons.Default.Person,
                            text = artist,
                            onClick = { searchQuery = artist }
                        )
                    }
                }
                if (albums.isNotEmpty()) {
                    item(key = "header_albums") { SectionHeader("Álbumes") }
                    items(albums, key = { "album_$it" }) { album ->
                        SuggestionRow(
                            icon = Icons.Default.Album,
                            text = album,
                            onClick = { searchQuery = album }
                        )
                    }
                }
                if (libraryUiState.filteredSongs.isNotEmpty()) {
                    item(key = "header_songs") { SectionHeader("Canciones") }
                }
                itemsIndexed(libraryUiState.filteredSongs, key = { index, song -> song.id }) { index, song ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = index * 30, easing = FastOutSlowInEasing)) +
                                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300, delayMillis = index * 30, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(150)) +
                                slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(150)),
                    ) {
                        SongItem(
                            song = song,
                            isCurrentSong = song.id == playerUiState.currentSong?.id,
                            isPlaying = playerUiState.isPlaying,
                            nowPlayingAnimation = nowPlayingAnimation,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlaySongAtIndex(index)
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(160.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}