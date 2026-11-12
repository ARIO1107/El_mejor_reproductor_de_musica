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

class RewindWidgetCompact : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ensureWidgetArtwork(context)
        provideContent {
            GlanceTheme {
                WidgetContentCompact(
                    songTitle = RewindWidgetState.songTitle,
                    songArtist = RewindWidgetState.songArtist,
                    isPlaying = RewindWidgetState.isPlaying,
                    hasSong = RewindWidgetState.hasSong,
                    art = RewindWidgetState.albumArt
                )
            }
        }
    }
}

@Composable
private fun WidgetContentCompact(
    songTitle: String,
    songArtist: String,
    isPlaying: Boolean,
    hasSong: Boolean,
    art: android.graphics.Bitmap?
) {
    val height = LocalSize.current.height.takeIf { it > 0.dp } ?: 56.dp
    val artSize = (height - 12.dp).coerceIn(28.dp, 44.dp)
    val buttonSize = (height - 20.dp).coerceIn(26.dp, 38.dp)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_bg_card)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtImage(
            bitmap = if (hasSong) art else null,
            fallbackIcon = R.drawable.ic_widget_music,
            size = artSize,
            cornerRadius = 12.dp
        )
        Spacer(GlanceModifier.width(9.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasSong) songTitle else "Rewind 1137",
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_text)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (hasSong) {
                    WidgetStatusDot(isPlaying)
                    Spacer(GlanceModifier.width(4.dp))
                }
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = if (hasSong) songArtist else "Tu musica, tu estilo",
                maxLines = 1,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = ColorProvider(R.color.widget_subtext)
                )
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        WidgetPlayPauseButton(isPlaying = isPlaying && hasSong, size = buttonSize)
    }
}
