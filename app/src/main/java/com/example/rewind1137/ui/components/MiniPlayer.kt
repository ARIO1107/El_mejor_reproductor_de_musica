package com.example.rewind1137.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.viewmodel.PlaybackMode
import kotlinx.coroutines.launch
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
    positionState: State<Long>,
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
    // Las animaciones solo corren mientras suena musica y se leen en fases de
    // dibujo (graphicsLayer / drawBehind), asi que no recompactan el MiniPlayer.
    // Antes el disco y el halo giraban incluso en pausa, gastando fotogramas que
    // le hacian falta a la cuadricula.
    var rotation by remember { mutableFloatStateOf(0f) }
    var glowAlpha by remember { mutableFloatStateOf(0.2f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            rotation = 0f
            glowAlpha = 0.2f
            return@LaunchedEffect
        }
        launch {
            animate(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) { value, _ -> rotation = value }
        }
        launch {
            animate(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            ) { value, _ -> glowAlpha = value }
        }
    }

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
                        val artworkState = rememberSongArtworkState(song, maxArtSize = 512)

                        // Luz ambiental: nace de la propia portada y se funde al
                        // mismo tiempo que la carátula al cambiar de canción.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1.55f
                                    scaleY = 1.55f
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .clip(CircleShape)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0f to Color.White,
                                                0.55f to Color.White.copy(alpha = 0.65f),
                                                1f to Color.Transparent
                                            ),
                                            center = center,
                                            radius = size.minDimension * 0.5f
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            SongArtworkLayers(
                                state = artworkState,
                                modifier = Modifier.fillMaxSize(),
                                opacity = 0.55f,
                                soften = 12,
                                contentDescription = null
                            )
                        }
                        if (isPlaying) {
                            val ringColor = MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = ringColor.copy(alpha = glowAlpha),
                                            radius = size.minDimension / 2f - 1.dp.toPx(),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                            )
                        }
                        SongArtworkLayers(
                            state = artworkState,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .graphicsLayer { rotationZ = rotation },
                            contentDescription = null,
                            placeholder = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
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
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    AnimatedContent(
                        targetState = song,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(240)) + slideInVertically(animationSpec = tween(300)) { height -> height / 3 })
                                .togetherWith(fadeOut(animationSpec = tween(140)) + slideOutVertically(animationSpec = tween(180)) { height -> -height / 3 })
                        },
                        label = "miniTrackInfo",
                        modifier = Modifier.weight(1f)
                    ) { currentSong ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = currentSong.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

            MiniPlayerProgressRow(
                positionState = positionState,
                durationMs = durationMs,
                onSeek = onSeek
            )
        }
    }
}

/**
 * Barra de progreso aislada: es la unica parte del MiniPlayer que depende de la
 * posicion de reproduccion, asi que al actualizarse 10 veces por segundo solo se
 * recompone esta fila y no todo el reproductor.
 */
@Composable
private fun MiniPlayerProgressRow(
    positionState: State<Long>,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableFloatStateOf(0f) }

    val currentPositionMs = positionState.value
    val sliderValue = if (isSeeking) seekPos else currentPositionMs.toFloat()
    val displayPos = if (isSeeking) seekPos.toLong() else currentPositionMs

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
