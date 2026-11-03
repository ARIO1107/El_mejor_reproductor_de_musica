package com.example.re_pro.ui.screens

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.re_pro.data.model.Song
import com.example.re_pro.ui.components.DynamicBackground
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorScreen(
    song: Song?,
    onBackClick: () -> Unit,
    onSaved: () -> Unit = {},
    onSongUpdated: (Song) -> Unit = {}
) {
    val context = LocalContext.current
    var title by remember(song) { mutableStateOf(song?.title ?: "") }
    var artist by remember(song) { mutableStateOf(song?.artist ?: "") }
    var album by remember(song) { mutableStateOf(song?.album ?: "") }
    var genre by remember(song) { mutableStateOf(song?.genre ?: "") }
    var year by remember(song) { mutableStateOf("") }
    var track by remember(song) { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        song?.let { s ->
            try {
                val path = getPathFromUri(context, s.uri)
                if (path != null && path.endsWith(".mp3", true)) {
                    val mp3File = Mp3File(path)
                    if (mp3File.hasId3v2Tag()) {
                        val tag = mp3File.id3v2Tag as ID3v24Tag
                        title = tag.title ?: s.title
                        artist = tag.artist ?: s.artist
                        album = tag.album ?: s.album
                        genre = tag.genreDescription ?: s.genre
                        year = tag.year ?: ""
                        track = tag.track ?: ""
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = song?.albumArtUri)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Editar Tags",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TagField("Titulo", title, { title = it }, Icons.Default.Title)
            TagField("Artista", artist, { artist = it }, Icons.Default.Person)
            TagField("Album", album, { album = it }, Icons.Default.Album)
            TagField("Genero", genre, { genre = it }, Icons.Default.MusicNote)
            TagField("Ano", year, { year = it }, Icons.Default.CalendarMonth)
            TagField("Pista", track, { track = it }, Icons.Default.Numbers)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    song?.let { s ->
                        isLoading = true
                        saveTags(context, s, title, artist, album, genre, year, track) { success ->
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Tags guardados", Toast.LENGTH_SHORT).show()
                                onSongUpdated(
                                    s.copy(
                                        title = title.trim().ifBlank { s.title },
                                        artist = artist.trim().ifBlank { s.artist },
                                        album = album.trim().ifBlank { s.album },
                                        genre = genre.trim().ifBlank { s.genre }
                                    )
                                )
                                onSaved()
                            } else {
                                Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && song != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Cambios")
                }
            }
        }
    }
}

@Composable
private fun TagField(label: String, value: String, onValueChange: (String) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

private fun getPathFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "file") return uri.path
    return try {
        val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA))
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveTags(
    context: Context,
    song: Song,
    title: String,
    artist: String,
    album: String,
    genre: String,
    year: String,
    track: String,
    onResult: (Boolean) -> Unit
) {
    Thread {
        try {
            val path = getPathFromUri(context, song.uri) ?: run { onResult(false); return@Thread }
            if (!path.endsWith(".mp3", true)) { onResult(false); return@Thread }

            val mp3File = Mp3File(path)
            val tag = if (mp3File.hasId3v2Tag()) mp3File.id3v2Tag as ID3v24Tag else ID3v24Tag()

            tag.title = title.ifBlank { null }
            tag.artist = artist.ifBlank { null }
            tag.album = album.ifBlank { null }
            tag.genreDescription = genre.ifBlank { null }
            tag.year = year.ifBlank { null }
            tag.track = track.ifBlank { null }

            mp3File.id3v2Tag = tag

            val tempPath = path + ".tmp"
            mp3File.save(tempPath)

            val originalFile = File(path)
            originalFile.delete()
            File(tempPath).renameTo(originalFile)
            MediaScannerConnection.scanFile(context, arrayOf(originalFile.absolutePath), null, null)

            onResult(true)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }.start()
}