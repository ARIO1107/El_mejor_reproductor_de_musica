package com.example.re_pro.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.re_pro.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Letras con multiples fuentes y cachés:
 *  1. Memoria
 *  2. Disco (filesDir/lyrics)
 *  3. Archivos .lrc junto al audio (mismo nombre, "Artista - Titulo.lrc", etc.)
 *  4. Etiquetas embebidas (ID3 USLT en mp3 y otros formatos)
 *  5. Online: LRCLIB (sincronizadas + simples) y lyrics.ovh (simples), con
 *     varias variantes de consulta para maximizar la cobertura.
 *
 * También guarda un "no encontrado" temporal para no repetir peticiones de red
 * en cada reproducción de una canción sin letra.
 */
@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "lyrics").apply { if (!exists()) mkdirs() }
    }
    private val negativeDir: File by lazy {
        File(context.filesDir, "lyrics_miss").apply { if (!exists()) mkdirs() }
    }

    private val memoryCache = HashMap<String, String>()
    private val missCache = HashMap<String, Long>()

private companion object {
        const val MISS_TTL_MS = 24L * 60L * 60L * 1000L
        const val MAX_NETWORK_ATTEMPTS = 3

        /** "(Remastered 2011)", "[Live]", "{Demo}" y similares. */
        val BRACKETS = Regex("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}")

        /** "feat. X", "- Remix", "Live", "Version", " remastered "… */
        val NOISE = Regex(
            "\\b(feat|ft|featuring|with)\\b.*" +
                "|\\b(remaster(ed)?|remasterizado|remix|live|en vivo|version|versi[oó]n|edit|mix|" +
                "acoustic|ac[uú]stico|instrumental|demo|mono|stereo|bonus track|album version|" +
                "single version|original mix|extended|reprise)\\b.*",
            RegexOption.IGNORE_CASE
        )

        val SPACES = Regex("\\s+")
    }

    suspend fun getLyrics(song: Song): String = withContext(Dispatchers.IO) {
        val key = cacheKey(song)
        if (key.isEmpty()) return@withContext ""

        memoryCache[key]?.let { return@withContext it }
        if (isRecentMiss(key)) return@withContext ""

        val cacheFile = File(cacheDir, "$key.lrc")
        readIfUsable(cacheFile)?.let {
            memoryCache[key] = it
            return@withContext it
        }

        // Búsqueda local antes de salir a la red.
        findLocalLyrics(song)?.let {
            remember(key, it, cacheFile)
            return@withContext it
        }

        val online = fetchOnlineLyrics(song)
        if (online.isNotBlank()) {
            remember(key, online, cacheFile)
            return@withContext online
        }

        markMiss(key)
        ""
    }

    private fun cacheKey(song: Song): String {
        val idPart = if (song.id > 0L) "id${song.id}" else ""
        val uriPart = song.uri.toString().hashCode().toString()
        return if (idPart.isNotEmpty()) idPart else "uri$uriPart"
    }

    private fun remember(key: String, lyrics: String, cacheFile: File) {
        memoryCache[key] = lyrics
        runCatching { cacheFile.writeText(lyrics) }
    }

    private fun isRecentMiss(key: String): Boolean {
        missCache[key]?.let { stamp ->
            if (System.currentTimeMillis() - stamp < MISS_TTL_MS) return true
            missCache.remove(key)
        }
        val last = File(negativeDir, key).lastModified()
        return last > 0L && System.currentTimeMillis() - last < MISS_TTL_MS
    }

    private fun markMiss(key: String) {
        val now = System.currentTimeMillis()
        missCache[key] = now
        runCatching {
            negativeDir.mkdirs()
            File(negativeDir, key).writeText(now.toString())
        }
    }

    private fun readIfUsable(file: File): String? {
        if (!file.exists() || !file.isFile) return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        return text.takeIf { it.isNotBlank() }
    }

    // ---------- Fuentes locales ----------

    private fun findLocalLyrics(song: Song): String? {
        val audioPath = resolveAudioPath(song.uri) ?: return null
        val audioFile = File(audioPath)
        val parent = audioFile.parentFile ?: return null
        val baseName = audioFile.nameWithoutExtension

        val candidates = LinkedHashSet<File>()
        candidates.add(File(parent, "$baseName.lrc"))
        candidates.add(File(parent, "$baseName.txt"))
        candidates.add(File(parent, "$baseName.LRC"))

        val artist = cleanText(song.artist)
        val title = cleanText(song.title)
        if (artist.isNotBlank() && title.isNotBlank()) {
            candidates.add(File(parent, "$artist - $title.lrc"))
            candidates.add(File(parent, "$artist - $title.txt"))
            candidates.add(File(parent, "$title.lrc"))
        }

        // Carpetas de música comunes del dispositivo.
        MUSIC_FOLDERS.forEach { folder ->
            candidates.add(File(folder, "$baseName.lrc"))
            candidates.add(File(folder, "$baseName.txt"))
            if (artist.isNotBlank() && title.isNotBlank()) {
                candidates.add(File(folder, "$artist - $title.lrc"))
                candidates.add(File(folder, "$title - $artist.lrc"))
            }
        }

        candidates.forEach { file ->
            readIfUsable(file)?.let { text ->
                if (looksLikeLyrics(text)) return text
            }
        }

        // Etiquetas embebidas: mp3 con mp3agic y cualquier otro formato vía ID3v2.
        if (audioFile.extension.equals("mp3", ignoreCase = true)) {
            readEmbeddedMp3Lyrics(audioFile)?.let { text ->
                if (looksLikeLyrics(text)) return text
            }
        }
        readEmbeddedId3Lyrics(audioFile)?.let { text ->
            if (looksLikeLyrics(text)) return text
        }
        return null
    }

    private val MUSIC_FOLDERS = listOf(
        "/storage/emulated/0/Music",
        "/storage/emulated/0/Download",
        "/storage/emulated/0/Downloads",
        "/sdcard/Music",
        "/sdcard/Download"
    )

    private fun looksLikeLyrics(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 8) return false
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return false
        val hasTimestamp = Regex("\\[\\d{1,3}[:.]\\d{1,2}").containsMatchIn(trimmed)
        val hasWords = trimmed.count { it.isLetter() } > trimmed.length / 3
        return hasWords || hasTimestamp
    }

    private fun readEmbeddedMp3Lyrics(file: File): String? {
        return runCatching {
            val mp3 = com.mpatric.mp3agic.Mp3File(file.absolutePath)
            if (mp3.hasId3v2Tag()) {
                mp3.id3v2Tag?.lyrics?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Extrae el frame USLT/SYLT de un ID3v2 sin depender de mp3agic, para que
     * también funcione con m4a, aac, wav, ogg y demás contenedores.
     */
    private fun readEmbeddedId3Lyrics(file: File): String? = runCatching {
        val fis = file.inputStream().buffered()
        val header = ByteArray(10)
        if (fis.read(header) != 10) return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() ||
            header[2] != '3'.code.toByte()
        ) return null

        val version = header[3].toInt() and 0xFF
        val sizeBytes = ByteArray(4) { header[6 + it] }
        val id3Size = syncSafe(sizeBytes, if (version == 4) 4 else 3)
        if (id3Size <= 0) return null

        val unsynced = StringBuilder()
        val synced = StringBuilder()
        var pos = 10

        while (pos < id3Size) {
            val id = ByteArray(4)
            if (fis.read(id) != 4) break
            pos += 4
            val frameName = String(id, Charsets.ISO_8859_1)
            if (!frameName.all { it.isLetterOrDigit() }) break

            val len = ByteArray(4)
            if (fis.read(len) != 4) break
            pos += 4
            val frameSize = syncSafe(len, if (version == 4) 4 else 3)
            if (frameSize <= 0) break

            val frame = ByteArray(minOf(frameSize, 512 * 1024))
            val read = fis.read(frame)
            if (read <= 0) break
            pos += read

            when (frameName) {
                "USLT" -> {
                    if (frame.size > 5) {
                        val encoding = frame[0].toInt()
                        val text = decodeText(frame.copyOfRange(5, read), encoding)
                        if (text.isNotBlank()) unsynced.append(text).append('\n')
                    }
                }
                "SYLT" -> {
                    val text = frame.copyOfRange(7, read)
                    val encoding = frame[0].toInt()
                    val decoded = decodeText(text, encoding)
                    if (decoded.isNotBlank()) synced.append(decoded).append('\n')
                }
            }
        }
        fis.close()
        synced.toString().takeIf { it.isNotBlank() } ?: unsynced.toString().takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun syncSafe(bytes: ByteArray, bytesPerValue: Int): Int {
        var value = 0
        for (i in 0 until bytesPerValue) {
            value = (value shl 8) or (bytes[i].toInt() and 0xFF)
        }
        return value
    }

    private fun decodeText(bytes: ByteArray, encoding: Int): String {
        if (bytes.isEmpty()) return ""
        return when (encoding) {
            0 -> String(bytes, Charsets.ISO_8859_1)
            1 -> String(bytes, Charsets.UTF_16)
            2 -> String(bytes, Charsets.UTF_16BE)
            3 -> String(bytes, Charsets.UTF_8)
            else -> String(bytes, Charsets.UTF_8)
        }
    }

    private fun resolveAudioPath(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == null) return null
        return runCatching {
            val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    // ---------- Fuentes online ----------

    private fun fetchOnlineLyrics(song: Song): String {
        val variants = queryVariants(song)
        if (variants.isEmpty()) return ""

        // 1) Sincronizadas de LRCLIB: es la única fuente con marcas de tiempo.
        for ((title, artist, album) in variants) {
            val synced = fetchLrclibSynced(title, artist, album, song.duration)
            if (synced.isNotBlank()) return synced
        }

        // 2) Texto plano de LRCLIB.
        for ((title, artist, _) in variants) {
            val plain = fetchLrclibPlain(title, artist)
            if (plain.isNotBlank()) return plain
        }

        // 3) lyrics.ovh como último recurso (muy buena cobertura internacional).
        for ((title, artist, _) in variants) {
            val plain = fetchLyricsOvh(title, artist)
            if (plain.isNotBlank()) return plain
        }

        return ""
    }

    private fun queryVariants(song: Song): List<Triple<String, String, String>> {
        val rawTitle = song.title.trim()
        if (rawTitle.isBlank()) return emptyList()
        val rawArtist = song.artist.trim()
        val album = song.album.trim()

        val titles = LinkedHashSet<String>()
        titles.add(rawTitle)
        normalize(rawTitle)?.let { titles.add(it) }
        val noDash = rawTitle.substringBeforeLast(" - ").trim()
        if (noDash.isNotBlank() && noDash != rawTitle) titles.add(noDash)

        val artists = LinkedHashSet<String>()
        artists.add(rawArtist)
        normalize(rawArtist)?.let { artists.add(it) }

        val result = LinkedHashSet<Triple<String, String, String>>()
        for (t in titles) {
            if (t.isBlank()) continue
            for (a in artists) {
                if (result.size >= MAX_NETWORK_ATTEMPTS) return result.toList()
                result.add(Triple(t, a, album))
            }
            if (result.size >= MAX_NETWORK_ATTEMPTS) return result.toList()
        }
        return result.toList()
    }

    /** Quita paréntesis/etiquetas y sufijos tipo "Remastered", "feat."… */
    private fun normalize(value: String): String? {
        val withoutFeat = NOISE.replace(value, " ")
        val withoutBrackets = BRACKETS.replace(withoutFeat, " ")
        val cleaned = SPACES.replace(withoutBrackets, " ").trim()
        return cleaned.takeIf { it.length >= 2 && !it.equals(value, ignoreCase = true) }
    }

    private fun fetchLrclibSynced(title: String, artist: String, album: String, durationMs: Long): String {
        val durationSec = (durationMs / 1000L).coerceAtLeast(0L)
        runCatching {
            val exact = buildString {
                append("https://lrclib.net/api/get?track_name=").append(enc(title))
                if (artist.isNotBlank()) append("&artist_name=").append(enc(artist))
                if (album.isNotBlank()) append("&album_name=").append(enc(album))
                if (durationSec > 0) append("&duration=").append(durationSec)
            }
            val body = httpGet(exact)
            if (body != null) {
                parseLyricsObject(JSONObject(body))?.let { return it }
            }
        }

        return runCatching {
            val search = buildString {
                append("https://lrclib.net/api/search?track_name=").append(enc(title))
                if (artist.isNotBlank()) append("&artist_name=").append(enc(artist))
            }
            val body = httpGet(search) ?: return@runCatching ""
            val array = JSONArray(body)
            var best: String? = null
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val synced = obj.optString("syncedLyrics", "")
                if (synced.isNotBlank()) return@runCatching synced
                val plain = obj.optString("plainLyrics", "")
                if (plain.isNotBlank() && best == null) best = plain
            }
            best ?: ""
        }.getOrDefault("")
    }

    private fun fetchLrclibPlain(title: String, artist: String): String {
        return runCatching {
            val url = buildString {
                append("https://lrclib.net/api/get?track_name=").append(enc(title))
                if (artist.isNotBlank()) append("&artist_name=").append(enc(artist))
            }
            val body = httpGet(url) ?: return@runCatching ""
            JSONObject(body).optString("plainLyrics", "")
        }.getOrDefault("")
    }

    private fun fetchLyricsOvh(title: String, artist: String): String {
        return runCatching {
            val url = "https://api.lyrics.ovh/v1/${enc(artist)}/${enc(title)}"
            val body = httpGet(url) ?: return@runCatching ""
            val text = JSONObject(body).optString("lyrics", "")
            text.trim().takeIf { it.length > 12 } ?: ""
        }.getOrDefault("")
    }

    private fun parseLyricsObject(obj: JSONObject): String? {
        val synced = obj.optString("syncedLyrics", "")
        if (synced.isNotBlank()) return synced
        val plain = obj.optString("plainLyrics", "")
        return plain.takeIf { it.isNotBlank() }
    }

    private fun httpGet(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Re-Pro/2.0 (Android music player)")
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("LyricsRepository", "Fallo al consultar $url: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun cleanText(value: String): String =
        value.replace(Regex("[_]+"), " ").replace(SPACES, " ").trim()

    private fun enc(value: String): String =
        URLEncoder.encode(value.lowercase(Locale.ROOT), "UTF-8")
}