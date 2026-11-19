package com.example.rewind1137.utils

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.rewind1137.data.model.Song

/**
 * Constructor unico de [MediaItem] para todo el reproductor.
 *
 * Importante: NO se define `setArtworkUri`. Media3 publica esa URI en la
 * MediaSession como ALBUM_ART_URI/ART_URI/DISPLAY_ICON_URI y el sistema la
 * comparte entre todas las canciones del mismo `album_id`, por lo que la
 * notificacion terminaba mostrando la misma portada para canciones distintas.
 * La caratula real la resuelve [SongBitmapLoader] con el arte embebido de cada
 * archivo, usando los extras que se adjuntan aqui.
 */
fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setAlbumArtist(artist)
                .setExtras(
                    Bundle().apply {
                        putLong(SongMediaExtras.SONG_ID, id)
                        putString(SongMediaExtras.SONG_URI, uri.toString())
                        putString(SongMediaExtras.ALBUM_ART_URI, albumArtUri?.toString())
                        putString("title", title)
                        putString("artist", artist)
                        putString("album", album)
                        putLong("duration", duration)
                        putLong("albumId", albumId)
                        putString("genre", genre)
                    }
                )
                .build()
        )
        .build()
