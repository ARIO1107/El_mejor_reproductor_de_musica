package com.example.rewind1137.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.rewind1137.MainActivity
import com.example.rewind1137.R

class RewindWidgetLarge : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = RewindWidgetState
        provideContent {
            GlanceTheme {
                WidgetContentLarge(
                    songTitle = state.songTitle,
                    songArtist = state.songArtist,
                    songAlbum = state.songAlbum,
                    isPlaying = state.isPlaying,
                    hasSong = state.hasSong,
                    progressMs = state.progressMs,
                    durationMs = state.durationMs,
                    art = state.albumArt,
                    upNext = state.upNext,
                    queueCount = state.queueCount
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
    progressMs: Long,
    durationMs: Long,
    art: android.graphics.Bitmap?,
    upNext: List<String>,
    queueCount: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_bg_card)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REWIND 1137",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_accent)
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetStatusDot(isPlaying && hasSong)
                Spacer(GlanceModifier.width(5.dp))
                Text(
                    text = when {
                        !hasSong -> "Sin rewindducción"
                        isPlaying -> "Sonando ahora"
                        else -> "Pausado"
                    },
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(R.color.widget_subtext)
                    )
                )
            }
        }
        Spacer()
        WidgetArtImage(
            bitmap = if (hasSong) art else null,
            fallbackIcon = R.drawable.ic_launcher_foreground,
            size = 140.dp
        )
        Spacer()
        Text(
            text = if (hasSong) songTitle else "Toca para abrir Rewind 1137",
            maxLines = 1,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_text)
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            text = if (hasSong && songAlbum.isNotBlank()) "$songArtist · $songAlbum" else songArtist,
            maxLines = 1,
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(R.color.widget_subtext)
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Spacer()
        WidgetProgress(
            progressMs = progressMs,
            durationMs = durationMs,
            visible = hasSong
        )
        Spacer()
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetPrevButton()
            Spacer(GlanceModifier.width(18.dp))
            WidgetPlayPauseButton(isPlaying && hasSong)
            Spacer(GlanceModifier.width(18.dp))
            WidgetNextButton()
        }
        if (hasSong && upNext.isNotEmpty()) {
            Spacer()
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "A CONTINUACIÓN",
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
            Spacer()
            upNext.take(3).forEachIndexed { index, title ->
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
                Spacer()
            }
        }
    }
}