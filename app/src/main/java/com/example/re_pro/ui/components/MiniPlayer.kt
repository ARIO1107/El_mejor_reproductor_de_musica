package com.example.re_pro.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.re_pro.utils.rememberSongArtwork
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.re_pro.R
import com.example.re_pro.data.model.Song
import com.example.re_pro.viewmodel.PlaybackMode
import java.util.Locale

fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackMode: PlaybackMode,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onOpenPlayerSheet: (Rect?) -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    var anchorRect by remember { mutableStateOf<Rect?>(null) }
    val currentAnchor by rememberUpdatedState(anchorRect)
    val infiniteTransition = rememberInfiniteTransition(label = "mini_disc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_rotation"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_glow"
    )

    var isSeeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableFloatStateOf(0f) }

    val sliderValue = if (isSeeking) seekPos else currentPositionMs.toFloat()
    val displayPos = if (isSeeking) seekPos.toLong() else currentPositionMs

    GlassmorphicContainer(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                anchorRect = coords.boundsInWindow()
            },
        cornerRadius = 28.dp,
        blurRadius = 24.dp,
        glassAlpha = 0.85f,
        borderAlpha = 0.20f
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenPlayerSheet(currentAnchor) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), CircleShape)
                            )
                        }
                        val artworkModel = rememberSongArtwork(song)
                        if (artworkModel != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(artworkModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                                error = painterResource(id = R.drawable.ic_launcher_foreground),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .rotate(if (isPlaying) rotation else 0f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePlaybackMode, modifier = Modifier.size(32.dp)) {
                        val (icon, tint) = when (playbackMode) {
                            PlaybackMode.SEQUENTIAL -> Icons.Default.Repeat to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle to MaterialTheme.colorScheme.primary
                            PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne to MaterialTheme.colorScheme.primary
                        }
                        Icon(icon, contentDescription = playbackMode.title, tint = tint, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = {
                        onPreviousClick()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Anterior", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                    }

                    Surface(
                        onClick = {
                            onPlayPauseClick()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(180)))
                                        .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)))
                                },
                                label = "playPauseIcon"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pausa",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        onNextClick()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipNext, "Siguiente", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMillis(displayPos), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = sliderValue.coerceIn(0f, if (durationMs > 0) durationMs.toFloat() else 1f),
                    onValueChange = { newPos ->
                        isSeeking = true
                        seekPos = newPos
                    },
                    onValueChangeFinished = {
                        onSeek(seekPos.toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(18.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surface
                    )
                )
                Text(formatMillis(durationMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
