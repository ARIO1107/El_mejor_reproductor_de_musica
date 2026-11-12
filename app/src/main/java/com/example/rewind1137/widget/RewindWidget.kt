package com.example.rewind1137.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.rewind1137.R
import com.example.rewind1137.viewmodel.PlaybackMode

class RewindWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ensureWidgetArtwork(context)
        provideContent {
            GlanceTheme {
                WidgetContentMedium(
                    songTitle = RewindWidgetState.songTitle,
                    songArtist = RewindWidgetState.songArtist,
                    songAlbum = RewindWidgetState.songAlbum,
                    isPlaying = RewindWidgetState.isPlaying,
                    hasSong = RewindWidgetState.hasSong,
                    isFavorite = RewindWidgetState.isFavorite,
                    playbackMode = RewindWidgetState.playbackMode,
                    progressMs = RewindWidgetState.progressMs,
                    durationMs = RewindWidgetState.durationMs,
                    art = RewindWidgetState.albumArt
                )
            }
        }
    }
}

@Composable
private fun WidgetContentMedium(
    songTitle: String,
    songArtist: String,
    songAlbum: String,
    isPlaying: Boolean,
    hasSong: Boolean,
    isFavorite: Boolean,
    playbackMode: PlaybackMode,
    progressMs: Long,
    durationMs: Long,
    art: android.graphics.Bitmap?
) {
    val height = LocalSize.current.height.takeIf { it > 0.dp } ?: 120.dp
    val contentHeight = (height - 16.dp).coerceAtLeast(60.dp)
    val controlSize = (contentHeight * 0.30f).coerceIn(26.dp, 34.dp)
    val artSize = (contentHeight - 22.dp - controlSize - 10.dp).coerceIn(28.dp, 56.dp)
    val likeSize = (artSize * 0.80f).coerceIn(26.dp, 34.dp)
    val subtitle = if (hasSong && songAlbum.isNotBlank()) "$songArtist · $songAlbum" else songArtist

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_bg_card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REWIND 1137",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_accent)
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            WidgetStatusDot(isPlaying && hasSong)
            Spacer(GlanceModifier.width(4.dp))
            WidgetStatusText(hasSong, isPlaying)
            WidgetOpenButton(size = 22.dp)
        }

        Spacer(GlanceModifier.height(3.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetArtImage(
                bitmap = if (hasSong) art else null,
                fallbackIcon = R.drawable.ic_widget_music,
                size = artSize
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (hasSong) songTitle else "Toca para abrir Rewind 1137",
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_text)
                    )
                )
                Spacer(GlanceModifier.height(1.dp))
                Text(
                    text = if (hasSong) subtitle else "Tu musica, tu estilo",
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(R.color.widget_subtext)
                    )
                )
                Spacer(GlanceModifier.height(5.dp))
                WidgetProgress(
                    progressMs = progressMs,
                    durationMs = durationMs,
                    visible = hasSong
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            WidgetLikeButton(
                isFavorite = isFavorite,
                enabled = hasSong,
                size = likeSize
            )
        }

        Spacer(GlanceModifier.height(2.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetModeButton(mode = playbackMode, size = controlSize - 2.dp)
            Spacer(GlanceModifier.width(8.dp))
            WidgetPrevButton(size = controlSize)
            Spacer(GlanceModifier.width(12.dp))
            WidgetPlayPauseButton(isPlaying = isPlaying && hasSong, size = controlSize + 2.dp)
            Spacer(GlanceModifier.width(12.dp))
            WidgetNextButton(size = controlSize)
        }
    }
}
