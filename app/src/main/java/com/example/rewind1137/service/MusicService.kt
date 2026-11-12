package com.example.rewind1137.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.rewind1137.MainActivity
import com.example.rewind1137.R
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.utils.AlbumArtUtils
import com.example.rewind1137.utils.MusicPlayerManager
import com.example.rewind1137.utils.SongBitmapLoader
import com.example.rewind1137.viewmodel.PlaybackMode
import com.example.rewind1137.viewmodel.PlaybackStateHolder
import com.example.rewind1137.widget.RewindWidgetRegistry
import com.example.rewind1137.widget.RewindWidgetState
import com.example.rewind1137.data.repository.SettingsRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var mediaSession: MediaSession? = null
    private var isForegroundStarted = false

    companion object {
        const val CHANNEL_ID = "rewind_media_channel"
        const val CHANNEL_NAME = "Rewind 1137 Media Controller"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_CLOSE = "ACTION_CLOSE"
        const val ACTION_TOGGLE_FAVORITE = "ACTION_TOGGLE_FAVORITE"
        const val ACTION_TOGGLE_MODE = "ACTION_TOGGLE_MODE"

        private const val NOTIF_ART_SIZE = 256
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val bitmapExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val sharedPlayer = MusicPlayerManager.getPlayer(this)

        mediaSession = MediaSession.Builder(this, sharedPlayer)
            .setBitmapLoader(SongBitmapLoader(this, bitmapExecutor))
            .build()

        sharedPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotification()
                refreshWidgetState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
                refreshWidgetState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateNotification()
                refreshWidgetState()
            }
        })

        serviceScope.launch {
            var lastNotificationMs = 0L
            var lastSignature = ""
            playbackStateHolder.state.collect { state ->
                val signature = buildString {
                    append(state.currentSongId).append('|')
                    append(state.isPlaying).append('|')
                    append(state.playbackMode).append('|')
                    append(state.durationMs).append('|')
                    append(state.filteredSongs.size).append('|')
                    append(state.favoriteSongIds.contains(state.currentSongId))
                }
                val now = System.currentTimeMillis()
                val significant = signature != lastSignature
                if (significant || now - lastNotificationMs >= 1000L) {
                    lastSignature = signature
                    lastNotificationMs = now
                    updateNotification()
                }
            }
        }

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayoutActions: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                callback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val uiState = playbackStateHolder.state.value
                val notification = buildMediaNotification(session, uiState)
                return MediaNotification(NOTIFICATION_ID, notification)
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean = false
        })

        val uiState = playbackStateHolder.state.value
        val notification = buildMediaNotification(mediaSession!!, uiState)
        startForeground(NOTIFICATION_ID, notification)
        isForegroundStarted = true
        refreshWidgetState()
    }

    @OptIn(UnstableApi::class)
    private fun updateNotification() {
        if (isForegroundStarted) {
            mediaSession?.let { session ->
                val uiState = playbackStateHolder.state.value
                val notification = buildMediaNotification(session, uiState)
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildMediaNotification(session: MediaSession, uiState: PlaybackStateHolder.State): Notification {
        val remoteViews = RemoteViews(packageName, R.layout.notification_media_custom)
        val expandedViews = RemoteViews(packageName, R.layout.notification_expanded)
        val activeSong = uiState.currentSong
            ?: uiState.filteredSongs.firstOrNull { it.id == uiState.currentSongId }
            ?: uiState.filteredSongs.getOrNull(uiState.currentSongIndex)
        val albumArt = loadAlbumArt(activeSong)

        remoteViews.setTextViewText(R.id.notification_title, activeSong?.title ?: "Rewind 1137")
        remoteViews.setTextViewText(R.id.notification_artist, activeSong?.artist ?: "")

        expandedViews.setTextViewText(R.id.notification_expanded_title, activeSong?.title ?: "Rewind 1137")
        expandedViews.setTextViewText(R.id.notification_expanded_artist, activeSong?.artist ?: "")
        expandedViews.setTextViewText(R.id.notification_expanded_album, activeSong?.album ?: "")

        val currentPos = uiState.currentPositionMs
        val totalDuration = uiState.durationMs.coerceAtLeast(1L)
        val progressPercent = ((currentPos * 100) / totalDuration).toInt().coerceIn(0, 100)

        remoteViews.setProgressBar(R.id.notification_progress, 100, progressPercent, false)
        remoteViews.setTextViewText(R.id.notification_time_current, formatMillis(currentPos))
        remoteViews.setTextViewText(R.id.notification_time_total, formatMillis(uiState.durationMs))

        expandedViews.setProgressBar(R.id.notification_expanded_progress, 100, progressPercent, false)
        expandedViews.setTextViewText(R.id.notification_expanded_time_current, formatMillis(currentPos))
        expandedViews.setTextViewText(R.id.notification_expanded_time_total, formatMillis(uiState.durationMs))

        if (albumArt != null) {
            remoteViews.setImageViewBitmap(R.id.notification_album_art, albumArt)
            expandedViews.setImageViewBitmap(R.id.notification_expanded_art, albumArt)
        }

        val isFav = activeSong?.let { uiState.favoriteSongIds.contains(it.id) } ?: false
        val notifHeartOn = R.drawable.ic_notif_heart_on
        val notifHeartOff = R.drawable.ic_notif_heart_off
        val notifPlay = R.drawable.ic_notif_play
        val notifPause = R.drawable.ic_notif_pause
        val notifPrev = R.drawable.ic_notif_prev
        val notifNext = R.drawable.ic_notif_next
        val notifClose = R.drawable.ic_notif_close
        val notifMode = when (uiState.playbackMode) {
            PlaybackMode.SHUFFLE -> R.drawable.ic_notif_mode_shuffle
            PlaybackMode.REPEAT_ONE -> R.drawable.ic_notif_mode_repeat_one
            PlaybackMode.SEQUENTIAL -> R.drawable.ic_notif_mode_repeat
        }

        remoteViews.setImageViewResource(
            R.id.notification_btn_like,
            if (isFav) notifHeartOn else notifHeartOff
        )
        remoteViews.setImageViewResource(
            R.id.notification_btn_play_pause,
            if (uiState.isPlaying) notifPause else notifPlay
        )
        remoteViews.setImageViewResource(R.id.notification_btn_prev, notifPrev)
        remoteViews.setImageViewResource(R.id.notification_btn_next, notifNext)
        remoteViews.setImageViewResource(R.id.notification_btn_mode, notifMode)
        remoteViews.setImageViewResource(R.id.notification_btn_close, notifClose)

        expandedViews.setImageViewResource(
            R.id.notification_expanded_btn_like,
            if (isFav) notifHeartOn else notifHeartOff
        )
        expandedViews.setImageViewResource(
            R.id.notification_expanded_btn_play_pause,
            if (uiState.isPlaying) notifPause else notifPlay
        )
        expandedViews.setImageViewResource(R.id.notification_expanded_btn_prev, notifPrev)
        expandedViews.setImageViewResource(R.id.notification_expanded_btn_next, notifNext)
        expandedViews.setImageViewResource(R.id.notification_expanded_btn_mode, notifMode)
        expandedViews.setImageViewResource(R.id.notification_expanded_btn_close, notifClose)

        val pendingIntents = mapOf(
            R.id.notification_btn_like to createFavoriteIntent(activeSong),
            R.id.notification_btn_prev to createActionIntent(ACTION_PREVIOUS, R.id.notification_btn_prev),
            R.id.notification_btn_play_pause to createActionIntent(ACTION_PLAY_PAUSE, R.id.notification_btn_play_pause),
            R.id.notification_btn_next to createActionIntent(ACTION_NEXT, R.id.notification_btn_next),
            R.id.notification_btn_mode to createActionIntent(ACTION_TOGGLE_MODE, R.id.notification_btn_mode),
            R.id.notification_btn_close to createActionIntent(ACTION_CLOSE, R.id.notification_btn_close)
        )
        pendingIntents.forEach { (viewId, pi) ->
            remoteViews.setOnClickPendingIntent(viewId, pi)
            expandedViews.setOnClickPendingIntent(viewId, pi)
        }

        expandedViews.setOnClickPendingIntent(R.id.notification_expanded_btn_like, createFavoriteIntent(activeSong))
        expandedViews.setOnClickPendingIntent(R.id.notification_expanded_btn_prev, createActionIntent(ACTION_PREVIOUS, R.id.notification_expanded_btn_prev))
        expandedViews.setOnClickPendingIntent(R.id.notification_expanded_btn_play_pause, createActionIntent(ACTION_PLAY_PAUSE, R.id.notification_expanded_btn_play_pause))
        expandedViews.setOnClickPendingIntent(R.id.notification_expanded_btn_next, createActionIntent(ACTION_NEXT, R.id.notification_expanded_btn_next))
        expandedViews.setOnClickPendingIntent(R.id.notification_expanded_btn_mode, createActionIntent(ACTION_TOGGLE_MODE, R.id.notification_expanded_btn_mode))
        expandedViews.setOnClickPendingIntent(R.id.notification_expanded_btn_close, createActionIntent(ACTION_CLOSE, R.id.notification_expanded_btn_close))

        val actionLike = NotificationCompat.Action(
            if (isFav) notifHeartOn else notifHeartOff,
            "Favorito",
            createFavoriteIntent(activeSong)
        )
        val actionPrev = NotificationCompat.Action(
            notifPrev,
            "Anterior",
            createActionIntent(ACTION_PREVIOUS, R.id.notification_btn_prev)
        )
        val actionPlayPause = NotificationCompat.Action(
            if (uiState.isPlaying) notifPause else notifPlay,
            if (uiState.isPlaying) "Pausa" else "Reproducir",
            createActionIntent(ACTION_PLAY_PAUSE, R.id.notification_btn_play_pause)
        )
        val actionNext = NotificationCompat.Action(
            notifNext,
            "Siguiente",
            createActionIntent(ACTION_NEXT, R.id.notification_btn_next)
        )
        val actionMode = NotificationCompat.Action(
            notifMode,
            "Modo de rewindducción",
            createActionIntent(ACTION_TOGGLE_MODE, R.id.notification_btn_mode)
        )
        val actionClose = NotificationCompat.Action(
            notifClose,
            "Cerrar",
            createActionIntent(ACTION_CLOSE, R.id.notification_btn_close)
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(activeSong?.title ?: "Rewind 1137")
            .setContentText(activeSong?.artist ?: "Música")
            .setSubText(activeSong?.album)
            .setLargeIcon(albumArt)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(expandedViews)
            .setContentIntent(contentIntent)
            .addAction(actionLike)
            .addAction(actionPrev)
            .addAction(actionPlayPause)
            .addAction(actionNext)
            .addAction(actionMode)
            .addAction(actionClose)
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionCompatToken)
                    .setShowActionsInCompactView(1, 2, 3)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        createActionIntent(ACTION_CLOSE, R.id.notification_btn_close)
                    )
            )
            .setOngoing(uiState.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createFavoriteIntent(song: Song?): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = ACTION_TOGGLE_FAVORITE
            putExtra("SONG_ID", song?.id ?: -1L)
        }
        return PendingIntent.getService(
            this,
            R.id.notification_btn_like,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadAlbumArt(song: Song?): Bitmap? {
        if (song == null) return null
        val cached = AlbumArtUtils.getCachedArt(song.id, NOTIF_ART_SIZE)
        if (cached != null) return cached

        loadAlbumArtAsync(song)
        return null
    }

    private fun loadAlbumArtAsync(song: Song) {
        if (song.id <= 0L || song.uri.scheme == "android.resource") return
        serviceScope.launch(Dispatchers.IO) {
            val bitmap = AlbumArtUtils.getSongArtwork(
                context = this@MusicService,
                songId = song.id,
                contentUri = song.uri,
                albumArtUri = song.albumArtUri,
                maxSize = NOTIF_ART_SIZE
            )
            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    updateNotification()
                }
            }
        }
    }

    private fun createActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> MusicPlayerManager.togglePlayPause()
            ACTION_NEXT -> MusicPlayerManager.skipNext()
            ACTION_PREVIOUS -> MusicPlayerManager.skipPrevious()
            ACTION_TOGGLE_MODE -> {
                sendBroadcast(Intent("ACTION_TOGGLE_MODE").setPackage(packageName))
            }
            ACTION_TOGGLE_FAVORITE -> {
                val songId = intent.getLongExtra("SONG_ID", -1L)
                if (songId != -1L) {
                    val current = playbackStateHolder.state.value.favoriteSongIds
                    val updated = if (current.contains(songId)) current - songId else current + songId
                    playbackStateHolder.update { it.copy(favoriteSongIds = updated) }
                    serviceScope.launch {
                        runCatching { settingsRepository.saveFavoriteSongIds(updated) }
                    }
                    updateNotification()
                }
            }
            ACTION_CLOSE -> {
                if (MusicPlayerManager.isPlaying()) MusicPlayerManager.togglePlayPause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundStarted = false
                stopSelf()
            }
        }
        if (MusicPlayerManager.isPlaying() && !isForegroundStarted) {
            isForegroundStarted = true
            mediaSession?.let { session ->
                startForeground(NOTIFICATION_ID, buildMediaNotification(session, playbackStateHolder.state.value))
            }
        } else if (isForegroundStarted) {
            val uiState = playbackStateHolder.state.value
            mediaSession?.let { session ->
                val notification = buildMediaNotification(session, uiState)
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
        refreshWidgetState()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun refreshWidgetState() {
        RewindWidgetState.refresh(playbackStateHolder.state.value)
        RewindWidgetState.refresh(MusicPlayerManager.getPlayer(this))
        val state = playbackStateHolder.state.value
        val song = state.currentSong
            ?: state.filteredSongs.firstOrNull { it.id == state.currentSongId }
            ?: state.filteredSongs.getOrNull(state.currentSongIndex)
        runCatching {
            serviceScope.launch {
                val art = song?.let {
                    runCatching {
                        AlbumArtUtils.getSongArtwork(
                            this@MusicService,
                            it.id,
                            it.uri,
                            it.albumArtUri,
                            NOTIF_ART_SIZE
                        )
                    }.getOrNull()
                }
                RewindWidgetState.albumArt = art
                RewindWidgetRegistry.updateAll(this@MusicService)
            }
        }
        scheduleWidgetTicker()
    }

    private var widgetTickJob: kotlinx.coroutines.Job? = null

    private fun scheduleWidgetTicker() {
        widgetTickJob?.cancel()
        if (!RewindWidgetState.isPlaying) return
        widgetTickJob = serviceScope.launch {
            while (isActive) {
                delay(2000)
                if (!RewindWidgetState.isPlaying) break
                RewindWidgetState.run {
                    progressMs = if (progressMs < durationMs) progressMs + 2000 else 0L
                }
                RewindWidgetRegistry.updateAll(this@MusicService)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!MusicPlayerManager.isPlaying()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForegroundStarted = false
            stopSelf()
        }
    }

    private fun formatMillis(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Control de rewindducción de música Rewind 1137"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        widgetTickJob?.cancel()
        if (isForegroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForegroundStarted = false
        }
        mediaSession?.run {
            release()
            mediaSession = null
        }
        bitmapExecutor.shutdown()
        super.onDestroy()
    }
}
