package com.example.rewind1137.widget

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.rewind1137.R
import com.example.rewind1137.viewmodel.PlaybackMode

const val ACTION_PREVIOUS = "com.example.rewind1137.ACTION_PREVIOUS"
const val ACTION_PLAY_PAUSE = "com.example.rewind1137.ACTION_PLAY_PAUSE"
const val ACTION_NEXT = "com.example.rewind1137.ACTION_NEXT"
const val ACTION_TOGGLE_FAVORITE = "com.example.rewind1137.ACTION_TOGGLE_FAVORITE"
const val ACTION_TOGGLE_MODE = "com.example.rewind1137.ACTION_TOGGLE_MODE"
const val ACTION_OPEN_APP = "com.example.rewind1137.ACTION_OPEN_APP"

@Composable
private fun ControlButton(
    iconRes: Int,
    tint: ColorProvider,
    action: Action,
    contentDescription: String,
    size: Dp,
    iconSize: Dp,
    background: Int? = R.drawable.widget_bg_btn
) {
    val base = GlanceModifier.size(size)
    val modifier = if (background != null) base.background(background) else base
    Box(
        modifier = modifier.clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@Composable
fun WidgetPrevButton(size: Dp = 38.dp) {
    val context = LocalContext.current
    ControlButton(
        iconRes = R.drawable.ic_widget_prev,
        tint = ColorProvider(R.color.widget_text),
        action = actionSendBroadcast(ACTION_PREVIOUS, ComponentName(context, WidgetReceiver::class.java)),
        contentDescription = "Anterior",
        size = size,
        iconSize = size * 0.46f
    )
}

@Composable
fun WidgetPlayPauseButton(isPlaying: Boolean, size: Dp = 44.dp) {
    val context = LocalContext.current
    ControlButton(
        iconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        tint = ColorProvider(R.color.widget_icon_on_primary),
        action = actionSendBroadcast(ACTION_PLAY_PAUSE, ComponentName(context, WidgetReceiver::class.java)),
        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
        size = size,
        iconSize = size * 0.48f,
        background = R.drawable.widget_bg_btn_primary
    )
}

@Composable
fun WidgetNextButton(size: Dp = 38.dp) {
    val context = LocalContext.current
    ControlButton(
        iconRes = R.drawable.ic_widget_next,
        tint = ColorProvider(R.color.widget_text),
        action = actionSendBroadcast(ACTION_NEXT, ComponentName(context, WidgetReceiver::class.java)),
        contentDescription = "Siguiente",
        size = size,
        iconSize = size * 0.46f
    )
}

@Composable
fun WidgetLikeButton(isFavorite: Boolean, enabled: Boolean, size: Dp = 34.dp) {
    val context = LocalContext.current
    val active = enabled && isFavorite
    ControlButton(
        iconRes = if (active) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart,
        tint = ColorProvider(
            when {
                !enabled -> R.color.widget_heart_off
                isFavorite -> R.color.widget_heart_on
                else -> R.color.widget_heart_off
            }
        ),
        action = actionSendBroadcast(ACTION_TOGGLE_FAVORITE, ComponentName(context, WidgetReceiver::class.java)),
        contentDescription = if (active) "Quitar de favoritos" else "Agregar a favoritos",
        size = size,
        iconSize = size * 0.52f,
        background = if (active) R.drawable.widget_bg_btn_like_on else R.drawable.widget_bg_btn_ghost
    )
}

@Composable
fun WidgetModeButton(mode: PlaybackMode, size: Dp = 32.dp) {
    val context = LocalContext.current
    val icon = when (mode) {
        PlaybackMode.SHUFFLE -> R.drawable.ic_widget_mode_shuffle
        PlaybackMode.REPEAT_ONE -> R.drawable.ic_widget_mode_repeat_one
        PlaybackMode.SEQUENTIAL -> R.drawable.ic_widget_mode_repeat
    }
    ControlButton(
        iconRes = icon,
        tint = ColorProvider(R.color.widget_accent),
        action = actionSendBroadcast(ACTION_TOGGLE_MODE, ComponentName(context, WidgetReceiver::class.java)),
        contentDescription = "Cambiar modo de reproduccion",
        size = size,
        iconSize = size * 0.5f,
        background = null
    )
}

@Composable
fun WidgetOpenButton(size: Dp = 32.dp) {
    val context = LocalContext.current
    ControlButton(
        iconRes = R.drawable.ic_widget_open,
        tint = ColorProvider(R.color.widget_accent),
        action = actionSendBroadcast(ACTION_OPEN_APP, ComponentName(context, WidgetReceiver::class.java)),
        contentDescription = "Abrir Rewind 1137",
        size = size,
        iconSize = size * 0.5f,
        background = null
    )
}

@Composable
fun WidgetArtImage(
    bitmap: android.graphics.Bitmap?,
    fallbackIcon: Int,
    size: Dp,
    cornerRadius: Dp = 14.dp
) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(cornerRadius)
            .background(R.drawable.widget_bg_art),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Portada de la cancion",
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                provider = ImageProvider(fallbackIcon),
                contentDescription = null,
                modifier = GlanceModifier.size(size * 0.42f),
                colorFilter = ColorFilter.tint(ColorProvider(R.color.widget_accent))
            )
        }
    }
}

@Composable
fun WidgetStatusDot(playing: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(8.dp)
            .background(
                if (playing) R.drawable.widget_bg_dot_playing else R.drawable.widget_bg_dot_paused
            )
    ) {}
}

@Composable
fun WidgetStatusText(hasSong: Boolean, isPlaying: Boolean) {
    Text(
        text = when {
            !hasSong -> "Sin reproduccion"
            isPlaying -> "Sonando"
            else -> "Pausado"
        },
        style = TextStyle(
            fontSize = 10.sp,
            color = ColorProvider(R.color.widget_subtext)
        )
    )
}

@Composable
fun WidgetProgress(
    progressMs: Long,
    durationMs: Long,
    visible: Boolean
) {
    if (!visible) return
    androidx.glance.appwidget.LinearProgressIndicator(
        progress = progressFraction(progressMs, durationMs),
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(4.dp),
        color = ColorProvider(R.color.widget_accent),
        backgroundColor = ColorProvider(R.color.widget_pill)
    )
}

fun formatWidgetTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
