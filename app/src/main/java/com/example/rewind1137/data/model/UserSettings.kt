package com.example.rewind1137.data.model

import com.example.rewind1137.viewmodel.AlbumArtMode
import com.example.rewind1137.viewmodel.NowPlayingAnimation

data class UserSettings(
    val excludeShortAudios: Boolean = true,
    val minAudioDurationMs: Long = 30000L,
    val themeMode: AppThemeMode = AppThemeMode.AMOLED_PITCH_BLACK,
    val backgroundImageUri: String? = null,
    val backgroundBlur: Float = 20f,
    val backgroundOpacity: Float = 0.5f,
    val gridColumns: Int = 2,
    val compactList: Boolean = false,
    val crossfadeSeconds: Int = 0,
    val replayGainEnabled: Boolean = false,
    val replayGainPreamp: Float = 1.0f,
    val equalizerEnabled: Boolean = true,
    val albumArtMode: AlbumArtMode = AlbumArtMode.STATIC,
    val nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS
)
