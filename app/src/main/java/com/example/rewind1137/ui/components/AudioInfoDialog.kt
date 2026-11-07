package com.example.rewind1137.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioInfo(
    val format: String = "MP3",
    val bitrate: String = "320 kbps",
    val sampleRate: String = "44.1 kHz",
    val channels: String = "Estéreo",
    val fileSize: String = "Desconocido"
)

suspend fun getAudioInfo(context: Context, song: Song): AudioInfo = withContext(Dispatchers.IO) {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, song.uri)

        val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/mp3"
        val format = mime.replace("audio/", "").replace("x-", "").uppercase()

        val bitrateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
        val bitrate = if (bitrateRaw > 0) "${bitrateRaw / 1000} kbps" else "320 kbps"

        val sampleRateRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
        } else 44100
        val sampleRate = if (sampleRateRaw > 0) "${sampleRateRaw / 1000}.${(sampleRateRaw % 1000) / 100} kHz" else "44.1 kHz"

        retriever.release()

        val sizeBytes = try {
            context.contentResolver.openFileDescriptor(song.uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (_: Exception) { 0L }

        val fileSize = if (sizeBytes > 1024 * 1024) {
            String.format(java.util.Locale.getDefault(), "%.2f MB", sizeBytes.toDouble() / (1024 * 1024))
        } else if (sizeBytes > 0) {
            "${sizeBytes / 1024} KB"
        } else "N/A"

        AudioInfo(
            format = format,
            bitrate = bitrate,
            sampleRate = sampleRate,
            channels = "Estéreo",
            fileSize = fileSize
        )
    } catch (_: Exception) {
        AudioInfo()
    }
}

@Composable
fun AudioInfoDialog(
    song: Song?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var audioInfo by remember(song) { mutableStateOf(AudioInfo()) }

    LaunchedEffect(song) {
        song?.let { audioInfo = getAudioInfo(context, it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Información de Audio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Formato", audioInfo.format)
                InfoRow("Bitrate", audioInfo.bitrate)
                InfoRow("Frecuencia", audioInfo.sampleRate)
                InfoRow("Canales", audioInfo.channels)
                InfoRow("Tamaño", audioInfo.fileSize)
                song?.let { s ->
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Título", s.title)
                    InfoRow("Artista", s.artist)
                    InfoRow("Álbum", if (s.album.isNotBlank()) s.album else "Desconocido")
                    InfoRow("Género", if (s.genre.isNotBlank()) s.genre else "Desconocido")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
    }
}
