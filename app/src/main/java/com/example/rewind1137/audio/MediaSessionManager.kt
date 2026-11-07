package com.example.rewind1137.audio

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.media3.session.MediaSession
import androidx.media3.exoplayer.ExoPlayer
import com.example.rewind1137.utils.MusicPlayerManager

class MediaSessionManager(private val context: Context) {
    private var mediaSession: MediaSession? = null

    fun createSession(player: ExoPlayer): MediaSession {
        mediaSession?.release()

        mediaSession = MediaSession.Builder(context, player)
            .build()

        return mediaSession!!
    }

    fun getSession(): MediaSession? = mediaSession

    @Suppress("DEPRECATION")
    fun handleMediaButtonEvent(intent: Intent): Boolean {
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    MusicPlayerManager.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    MusicPlayerManager.skipNext()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    MusicPlayerManager.skipPrevious()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    if (MusicPlayerManager.isPlaying()) {
                        MusicPlayerManager.togglePlayPause()
                    }
                    return true
                }
            }
        }
        return false
    }

    fun release() {
        mediaSession?.release()
        mediaSession = null
    }
}
