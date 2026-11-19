package com.example.rewind1137.utils

import android.content.Context
import android.net.Uri
import java.util.Locale
import kotlin.math.pow

/**
 * Lee REPLAYGAIN_TRACK_GAIN del inicio del archivo (ID3 TXXX o comentario Vorbis/FLAC).
 * No recorre el archivo entero: la etiqueta vive en la cabecera.
 */
object ReplayGainReader {
    private val gainPattern = Regex(
        "REPLAYGAIN_TRACK_GAIN\\s*[:=]\\s*([+-]?[0-9]+(?:[.,][0-9]+)?)\\s*dB",
        RegexOption.IGNORE_CASE
    )

    fun readTrackGainDb(context: Context, uri: Uri): Float? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(256 * 1024)
                val read = input.read(buffer)
                if (read <= 0) return null
                val text = String(buffer, 0, read, Charsets.ISO_8859_1)
                val match = gainPattern.find(text) ?: return null
                match.groupValues[1].replace(',', '.').toFloatOrNull()
            }
        }.getOrNull()
    }

    fun toLinear(gainDb: Float, preamp: Float): Float {
        val linear = 10.0.pow((gainDb / 20.0).toDouble()).toFloat() * preamp
        return linear.coerceIn(0.05f, 2f)
    }

    fun formatDb(gainDb: Float): String =
        String.format(Locale.US, "%+.1f dB", gainDb)
}
