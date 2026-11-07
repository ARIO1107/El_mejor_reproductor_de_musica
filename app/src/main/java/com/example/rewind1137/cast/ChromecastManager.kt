package com.example.rewind1137.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage

class ChromecastManager(private val context: Context) {

    companion object {
        private const val TAG = "ChromecastManager"
    }

    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private var isCasting = false

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            isCasting = true
            Log.d(TAG, "Cast session started: $sessionId")
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            isCasting = false
            Log.e(TAG, "Cast session failed: $error")
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            isCasting = false
            Log.d(TAG, "Cast session ended")
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            isCasting = true
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            isCasting = false
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    fun initialize() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
            castSession = castContext?.sessionManager?.currentCastSession
            isCasting = castSession?.isConnected == true
            Log.d(TAG, "Chromecast initialized, connected: $isCasting")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Chromecast", e)
        }
    }

    fun isCastConnected(): Boolean = isCasting

    fun castSong(title: String, artist: String, uri: String, albumArtUri: String?) {
        val session = castSession ?: return
        try {
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, title)
                putString(MediaMetadata.KEY_SUBTITLE, artist)
                albumArtUri?.let {
                    addImage(WebImage(Uri.parse(it)))
                }
            }

            val mediaInfo = MediaInfo.Builder(uri)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("audio/mpeg")
                .setMetadata(metadata)
                .build()

            val remoteMediaClient = session.remoteMediaClient
            remoteMediaClient?.load(mediaInfo, true, 0)
            Log.d(TAG, "Casting: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cast song", e)
        }
    }

    fun togglePlayPause() {
        castSession?.remoteMediaClient?.let { client ->
            if (client.isPlaying) client.pause() else client.play()
        }
    }

    fun stopCasting() {
        castSession?.remoteMediaClient?.stop()
        isCasting = false
    }

    fun release() {
        castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        castSession = null
        isCasting = false
    }
}
