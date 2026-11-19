package com.example.rewind1137.utils

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.provider.MediaStore
import com.example.rewind1137.data.model.Song

data class AudioFormatInfo(
    val mime: String,
    val sampleRateHz: String,
    val channelCount: String,
    val bitrateKbps: String
)

object AudioFormatReader {
    private const val UNKNOWN = "—"

    fun read(context: Context, song: Song): AudioFormatInfo {
        val fromStore = readFromMediaStore(context, song)
        val fromFormat = readFromMediaFormat(context, song)

        val mime = firstKnown(fromStore.mime, fromFormat.mime)
        val sampleRateHz = firstKnown(fromStore.sampleRateHz, fromFormat.sampleRateHz)
        val channelCount = firstKnown(fromStore.channelCount, fromFormat.channelCount)
        val bitrateKbps = firstKnown(fromStore.bitrateKbps, fromFormat.bitrateKbps)

        return AudioFormatInfo(
            mime = mime ?: UNKNOWN,
            sampleRateHz = sampleRateHz?.let { "$it Hz" } ?: UNKNOWN,
            channelCount = channelCount?.let(::formatChannels) ?: UNKNOWN,
            bitrateKbps = bitrateKbps?.let { "$it kbps" } ?: UNKNOWN
        )
    }

    private fun readFromMediaStore(context: Context, song: Song): RawInfo {
        val projection = buildList {
            add(MediaStore.Audio.Media.MIME_TYPE)
            add("bitrate")
            if (Build.VERSION.SDK_INT >= 31) add("samplerate")
            add("channel_count")
            add("channels")
        }.toTypedArray()

        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use RawInfo()
                val mime = cursor.stringOrNull("mime_type")
                val bitrate = cursor.longOrNull("bitrate")?.takeIf { it > 0 }?.let { it / 1000 }
                val sampleRate = cursor.intOrNull("samplerate")?.takeIf { it > 0 }
                val channels = (cursor.intOrNull("channel_count") ?: cursor.intOrNull("channels"))?.takeIf { it > 0 }
                RawInfo(mime = mime, sampleRateHz = sampleRate, channelCount = channels, bitrateKbps = bitrate)
            } ?: RawInfo()
        }.getOrDefault(RawInfo())
    }

    private fun readFromMediaFormat(context: Context, song: Song): RawInfo {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, song.uri, null)
            val format = (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return RawInfo()

            val mime = format.stringOrNull(MediaFormat.KEY_MIME)
            val sampleRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE)?.takeIf { it > 0 }
            val channels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT)?.takeIf { it > 0 }
            val bitrate = format.intOrNull(MediaFormat.KEY_BIT_RATE)
                ?.takeIf { it > 0 }
                ?.let { it / 1000L }
            RawInfo(mime = mime, sampleRateHz = sampleRate, channelCount = channels, bitrateKbps = bitrate)
        } catch (_: Exception) {
            RawInfo()
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun formatChannels(count: Int): String = when (count) {
        1 -> "1 (mono)"
        2 -> "2 (estéreo)"
        else -> count.toString()
    }

    private fun <T> firstKnown(vararg values: T?): T? = values.firstOrNull { it != null }

    private data class RawInfo(
        val mime: String? = null,
        val sampleRateHz: Int? = null,
        val channelCount: Int? = null,
        val bitrateKbps: Long? = null
    )

    private fun android.database.Cursor.stringOrNull(column: String): String? {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getString(idx)?.takeIf { it.isNotBlank() } else null
    }

    private fun android.database.Cursor.intOrNull(column: String): Int? {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getInt(idx) else null
    }

    private fun android.database.Cursor.longOrNull(column: String): Long? {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getLong(idx) else null
    }

    private fun MediaFormat.stringOrNull(key: String): String? =
        if (containsKey(key)) runCatching { getString(key) }.getOrNull()?.takeIf { it.isNotBlank() } else null

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null
}