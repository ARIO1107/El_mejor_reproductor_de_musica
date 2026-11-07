package com.example.rewind1137.widget

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.rewind1137.R

const val ACTION_PREVIOUS = "com.example.rewind1137.ACTION_PREVIOUS"
const val ACTION_PLAY_PAUSE = "com.example.rewind1137.ACTION_PLAY_PAUSE"
const val ACTION_NEXT = "com.example.rewind1137.ACTION_NEXT"

@Composable
private fun ControlButton(
    glyph: String,
    glyphSize: Float,
    tint: ColorProvider,
    background: Int,
    action: Action
) {
    Box(
        modifier = GlanceModifier
            .size(44.dp)
            .background(background)
            .clickable { action },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = TextStyle(
                fontSize = glyphSize.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        )
    }
}

@Composable
fun WidgetPrevButton() {
    val context = LocalContext.current
    val action = actionSendBroadcast(ACTION_PREVIOUS, ComponentName(context, WidgetReceiver::class.java))
    ControlButton(
        glyph = "\u25C0",
        glyphSize = 16f,
        tint = ColorProvider(R.color.widget_text),
        background = R.drawable.widget_bg_btn,
        action = action
    )
}

@Composable
fun WidgetPlayPauseButton(isPlaying: Boolean) {
    val context = LocalContext.current
    val action = actionSendBroadcast(ACTION_PLAY_PAUSE, ComponentName(context, WidgetReceiver::class.java))
    ControlButton(
        glyph = if (isPlaying) "\u258F\u258F" else "\u25B6",
        glyphSize = 20f,
        tint = ColorProvider(R.color.widget_green),
        background = R.drawable.widget_bg_btn_primary,
        action = action
    )
}

@Composable
fun WidgetNextButton() {
    val context = LocalContext.current
    val action = actionSendBroadcast(ACTION_NEXT, ComponentName(context, WidgetReceiver::class.java))
    ControlButton(
        glyph = "\u25B6",
        glyphSize = 16f,
        tint = ColorProvider(R.color.widget_text),
        background = R.drawable.widget_bg_btn,
        action = action
    )
}

@Composable
fun WidgetArtImage(
    bitmap: android.graphics.Bitmap?,
    fallbackIcon: Int,
    size: Dp
) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(R.drawable.widget_bg_art),
        contentAlignment = Alignment.Center
    ) {
        val provider = bitmap?.let { androidx.glance.ImageProvider(it) }
            ?: androidx.glance.ImageProvider(fallbackIcon)
        androidx.glance.Image(
            provider = provider,
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun WidgetStatusDot(playing: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(8.dp)
            .background(if (playing) R.color.widget_green else R.color.widget_subtext)
    ) {}
}

@Composable
fun WidgetProgress(
    progressMs: Long,
    durationMs: Long,
    visible: Boolean
) {
    if (!visible) {
        return
    }
    androidx.glance.appwidget.LinearProgressIndicator(
        progress = progressFraction(progressMs, durationMs),
        modifier = GlanceModifier.fillMaxWidth(),
        color = ColorProvider(R.color.widget_accent),
        backgroundColor = ColorProvider(R.color.widget_pill)
    )
}