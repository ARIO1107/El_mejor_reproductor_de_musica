package com.example.re_pro.widget

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
import com.example.re_pro.MainActivity
import com.example.re_pro.R

class ReProWidgetCompact : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ReProWidgetState
        provideContent {
            GlanceTheme {
                WidgetContentCompact(
                    songTitle = state.songTitle,
                    songArtist = state.songArtist,
                    isPlaying = state.isPlaying,
                    hasSong = state.hasSong,
                    art = state.albumArt
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
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_bg_card)
            .padding(horizontal = 10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtImage(
            bitmap = if (hasSong) art else null,
            fallbackIcon = R.drawable.ic_launcher_foreground,
            size = 42.dp
        )
        Spacer(GlanceModifier.width(10.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasSong) songTitle else "Re-Pro",
                maxLines = 1,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_text)
                )
            )
            Spacer()
            Text(
                text = if (hasSong) songArtist else "Tu música, tu ritmo",
                maxLines = 1,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(R.color.widget_subtext)
                )
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        WidgetPlayPauseButton(isPlaying && hasSong)
    }
}