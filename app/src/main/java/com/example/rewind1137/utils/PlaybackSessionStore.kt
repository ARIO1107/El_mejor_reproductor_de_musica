package com.example.rewind1137.utils

import android.content.Context
import android.net.Uri
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.viewmodel.PlaybackMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Cola y posición para reabrir la app con la misma sesión.
 * Vive en filesDir, no en almacenamiento compartido.
 */
object PlaybackSessionStore {
    private const val FILE_NAME = "playback_session.json"

    data class Session(
        val songs: List<Song>,
        val index: Int,
        val positionMs: Long,
        val mode: PlaybackMode
    )

    fun hasSession(context: Context): Boolean = file(context).exists() && file(context).length() > 2L

    fun save(
        context: Context,
        songs: List<Song>,
        index: Int,
        positionMs: Long,
        mode: PlaybackMode
    ) {
        if (songs.isEmpty()) return
        val array = JSONArray()
        songs.forEach { song ->
            array.put(JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("uri", song.uri.toString())
                put("duration", song.duration)
                put("albumId", song.albumId)
                put("albumArt", song.albumArtUri?.toString().orEmpty())
                put("dateAdded", song.dateAdded)
                put("genre", song.genre)
            })
        }
        val root = JSONObject()
            .put("index", index)
            .put("position", positionMs.coerceAtLeast(0L))
            .put("mode", mode.name)
            .put("songs", array)
        file(context).writeText(root.toString())
    }

    fun load(context: Context): Session? {
        val stored = file(context)
        if (!stored.exists()) return null
        return runCatching {
            val root = JSONObject(stored.readText())
            val array = root.getJSONArray("songs")
            val songs = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val uri = obj.optString("uri")
                    if (uri.isBlank()) continue
                    val art = obj.optString("albumArt")
                    add(
                        Song(
                            id = obj.optLong("id"),
                            title = obj.optString("title", "Sin título"),
                            artist = obj.optString("artist", "Artista desconocido"),
                            album = obj.optString("album"),
                            uri = Uri.parse(uri),
                            duration = obj.optLong("duration"),
                            albumId = obj.optLong("albumId"),
                            albumArtUri = art.takeIf { it.isNotBlank() }?.let(Uri::parse),
                            dateAdded = obj.optLong("dateAdded"),
                            genre = obj.optString("genre")
                        )
                    )
                }
            }
            if (songs.isEmpty()) return null
            val mode = runCatching {
                PlaybackMode.valueOf(root.optString("mode", PlaybackMode.SEQUENTIAL.name))
            }.getOrDefault(PlaybackMode.SEQUENTIAL)
            Session(
                songs = songs,
                index = root.optInt("index", 0).coerceIn(0, songs.lastIndex),
                positionMs = root.optLong("position", 0L).coerceAtLeast(0L),
                mode = mode
            )
        }.getOrNull()
    }

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)
}
