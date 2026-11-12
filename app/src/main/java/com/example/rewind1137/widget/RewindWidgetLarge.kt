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

class RewindWidgetLarge : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ensureWidgetArtwork(context)
        provideContent {
            GlanceTheme {
                WidgetContentLarge(
                    songTitle = RewindWidgetState.songTitle,
                    songArtist = RewindWidgetState.songArtist,
                    songAlbum = RewindWidgetState.songAlbum,
                    isPlaying = RewindWidgetState.isPlaying,
                    hasSong = RewindWidgetState.hasSong,
                    isFavorite = RewindWidgetState.isFavorite,
                    playbackMode = RewindWidgetState.playbackMode,
                    progressMs = RewindWidgetState.progressMs,
                    durationMs = RewindWidgetState.durationMs,
                    art = RewindWidgetState.albumArt,
                    upNext = RewindWidgetState.upNext,
                    queueCount = RewindWidgetState.queueCount
                )
            }
        }
    }
}

@Composable
private fun WidgetContentLarge(
    songTitle: String,
    songArtist: String,
    songAlbum: String,
    isPlaying: Boolean,
    hasSong: Boolean,
    isFavorite: Boolean,
    playbackMode: PlaybackMode,
    progressMs: Long,
    durationMs: Long,
    art: android.graphics.Bitmap?,
    upNext: List<String>,
    queueCount: Int
) {
    val height = LocalSize.current.height.takeIf { it > 0.dp } ?: 280.dp
    val contentHeight = (height - 28.dp).coerceAtLeast(150.dp)
    val artSize = (contentHeight * 0.36f).coerceIn(72.dp, 122.dp)
    val controlSize = (contentHeight * 0.16f).coerceIn(32.dp, 42.dp)
    val maxUpNext = if (height > 300.dp) 3 else 2

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_bg_card)
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REWIND 1137",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_accent)
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            WidgetStatusDot(isPlaying && hasSong)
            Spacer(GlanceModifier.width(5.dp))
            WidgetStatusText(hasSong, isPlaying)
            WidgetOpenButton(size = 28.dp)
        }

        Spacer(GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetArtImage(
                bitmap = if (hasSong) art else null,
                fallbackIcon = R.drawable.ic_widget_music,
                size = artSize,
                cornerRadius = 18.dp
            )
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (hasSong) songTitle else "Toca para abrir Rewind 1137",
                    maxLines = 2,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_text)
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = if (hasSong) songArtist else "Tu musica, tu estilo",
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(R.color.widget_subtext)
                    )
                )
                if (hasSong && songAlbum.isNotBlank()) {
                    Text(
                        text = songAlbum,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(R.color.widget_subtext)
                        )
                    )
                }
                Spacer(GlanceModifier.height(7.dp))
                WidgetProgress(
                    progressMs = progressMs,
                    durationMs = durationMs,
                    visible = hasSong
                )
                if (hasSong) {
                    Spacer(GlanceModifier.height(3.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = formatWidgetTime(progressMs),
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = ColorProvider(R.color.widget_subtext)
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = formatWidgetTime(durationMs),
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = ColorProvider(R.color.widget_subtext)
                            )
                        )
                    }
                }
            }
        }

        Spacer(GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetLikeButton(
                isFavorite = isFavorite,
                enabled = hasSong,
                size = controlSize
            )
            Spacer(GlanceModifier.width(8.dp))
            WidgetPrevButton(size = controlSize)
            Spacer(GlanceModifier.width(12.dp))
            WidgetPlayPauseButton(isPlaying = isPlaying && hasSong, size = controlSize + 6.dp)
            Spacer(GlanceModifier.width(12.dp))
            WidgetNextButton(size = controlSize)
            Spacer(GlanceModifier.width(8.dp))
            WidgetModeButton(mode = playbackMode, size = controlSize - 2.dp)
        }

        if (hasSong && upNext.isNotEmpty()) {
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "A CONTINUACION",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_accent)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "$queueCount en cola",
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = ColorProvider(R.color.widget_subtext)
                    )
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            upNext.take(maxUpNext).forEachIndexed { index, title ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_accent)
                        ),
                        modifier = GlanceModifier.width(18.dp)
                    )
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(R.color.widget_text)
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
                Spacer(GlanceModifier.height(2.dp))
            }
        }
    }
}
