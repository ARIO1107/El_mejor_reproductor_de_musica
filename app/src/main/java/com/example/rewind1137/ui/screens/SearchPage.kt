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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.ui.components.EmptyStateIllustration
import com.example.rewind1137.ui.components.SongItem
import com.example.rewind1137.utils.ThumbnailPrefetcher
import com.example.rewind1137.viewmodel.LibraryUiState
import com.example.rewind1137.viewmodel.NowPlayingAnimation
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    libraryUiState: LibraryUiState,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onClearRecentSearches: () -> Unit = {},
    nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS,
    gridColumns: Int = 1,
    compactList: Boolean = false,
    searchLoopResults: Boolean = false,
    onSearchLoopResultsChange: (Boolean) -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf(TextFieldValue(libraryUiState.searchQuery)) }
    var searchSubmitted by remember { mutableStateOf(false) }

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun setQuery(text: String, submitted: Boolean = false) {
        searchQuery = TextFieldValue(text = text, selection = TextRange(text.length))
        searchSubmitted = submitted
        if (submitted) hideKeyboard()
    }

    fun submitSearch() {
        searchSubmitted = true
        onSearchQueryChange(searchQuery.text)
        hideKeyboard()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery.text }
            .debounce(300)
            .collect { q -> onSearchQueryChange(q) }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text ->
                setQuery(text, submitted = true)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    searchSubmitted = false
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Buscar por título, artista o álbum...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    IconButton(onClick = { submitSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar y ocultar teclado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(onClick = { setQuery("") }) {
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
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(min = 52.dp)
            ) {
                Switch(
                    checked = searchLoopResults,
                    onCheckedChange = onSearchLoopResultsChange
                )
                Text(
                    text = "Bucle",
                    fontSize = 10.sp,
                    color = if (searchLoopResults) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchQuery.text.isBlank() && libraryUiState.recentSearches.isNotEmpty()) {
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
                        onClick = { setQuery(q, submitted = true) },
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        val query = searchQuery.text.trim()
        val searchResults = remember(query, libraryUiState.songsList) {
            if (query.isBlank()) {
                emptyList()
            } else {
                libraryUiState.songsList.filter { song ->
                    song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true) ||
                        song.album.contains(query, ignoreCase = true)
                }
            }
        }

        val querySuggestions = remember(query, libraryUiState.songsList, libraryUiState.recentSearches) {
            if (query.isBlank()) {
                emptyList()
            } else {
                val seen = LinkedHashSet<String>()
                fun addIfMatches(value: String) {
                    val v = value.trim()
                    if (v.isNotBlank() && v.contains(query, ignoreCase = true)) {
                        val key = v.lowercase()
                        if (seen.none { it.equals(key, ignoreCase = true) }) {
                            seen.add(v)
                        }
                    }
                }
                libraryUiState.recentSearches.forEach { addIfMatches(it) }
                libraryUiState.songsList.forEach { song ->
                    addIfMatches(song.title)
                    addIfMatches(song.artist)
                    addIfMatches(song.album)
                }
                seen.toList().take(10)
            }
        }

        val prefetchContext = LocalContext.current.applicationContext
        LaunchedEffect(searchResults) {
            ThumbnailPrefetcher.prefetch(prefetchContext, searchResults.take(60))
        }

        val showSuggestions = !searchSubmitted && querySuggestions.isNotEmpty()

        if (query.isBlank()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateIllustration(
                    imageVector = Icons.Filled.SearchOff,
                    title = "Buscar música",
                    subtitle = "Escribe el nombre de una canción, artista o álbum",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (searchResults.isEmpty() && !showSuggestions) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateIllustration(
                    imageVector = Icons.Filled.SearchOff,
                    title = "No se encontraron resultados",
                    subtitle = "No hay resultados para \"$query\"",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            if (gridColumns > 1) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns.coerceIn(2, 5)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showSuggestions) {
                        item(key = "header_suggestions", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Sugerencias")
                        }
                        gridItems(querySuggestions, key = { "suggestion_$it" }, span = { GridItemSpan(maxLineSpan) }) { suggestion ->
                            SuggestionRow(
                                icon = if (libraryUiState.recentSearches.any { it.equals(suggestion, ignoreCase = true) }) {
                                    Icons.Default.History
                                } else {
                                    Icons.Default.Search
                                },
                                text = suggestion,
                                onClick = { setQuery(suggestion, submitted = true) }
                            )
                        }
                    }
                    if (searchResults.isNotEmpty()) {
                        item(key = "header_songs", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader("Canciones")
                        }
                        gridItems(searchResults, key = { it.id }) { song ->
                            LibraryGridItem(
                                song = song,
                                isCurrentSong = song.id == currentSongId,
                                isPlaying = isPlaying,
                                nowPlayingAnimation = nowPlayingAnimation,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlaySong(song, searchResults)
                                }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showSuggestions) {
                        item(key = "header_suggestions") { SectionHeader("Sugerencias") }
                        items(querySuggestions, key = { "suggestion_$it" }) { suggestion ->
                            SuggestionRow(
                                icon = if (libraryUiState.recentSearches.any { it.equals(suggestion, ignoreCase = true) }) {
                                    Icons.Default.History
                                } else {
                                    Icons.Default.Search
                                },
                                text = suggestion,
                                onClick = { setQuery(suggestion, submitted = true) }
                            )
                        }
                    }
                    if (searchResults.isNotEmpty()) {
                        item(key = "header_songs") { SectionHeader("Canciones") }
                        itemsIndexed(searchResults, key = { _, song -> song.id }) { index, song ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        300,
                                        delayMillis = index * 30,
                                        easing = FastOutSlowInEasing
                                    )
                                ) + slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(
                                        300,
                                        delayMillis = index * 30,
                                        easing = FastOutSlowInEasing
                                    )
                                ),
                                exit = fadeOut(animationSpec = tween(150)) +
                                    slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(150)),
                            ) {
                                SongItem(
                                    song = song,
                                    isCurrentSong = song.id == currentSongId,
                                    isPlaying = isPlaying,
                                    isCompact = compactList,
                                    nowPlayingAnimation = nowPlayingAnimation,
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPlaySong(song, searchResults)
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(160.dp)) }
                }
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
