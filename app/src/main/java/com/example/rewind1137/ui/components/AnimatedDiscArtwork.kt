package com.example.rewind1137.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

enum class DiscAnimationMode {
    ROTATE,
    PULSE,
    FLOAT,
    NONE
}

@Composable
fun AnimatedDiscArtwork(
    albumArtUri: Any?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 200,
    animationMode: DiscAnimationMode = DiscAnimationMode.ROTATE,
    backgroundColor: Color = Color(0xFF130E0E),
    showDefaultIcon: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_animation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.05f else 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_scale"
    )

    val discShadowElevation by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = if (isPlaying) 16f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_shadow"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isPlaying) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_pulse"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = discShadowElevation
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (albumArtUri != null) {
            val coilModifier = Modifier
                .size(size.dp)
                .clip(CircleShape)

            when (animationMode) {
                DiscAnimationMode.ROTATE -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        modifier = coilModifier.rotate(if (isPlaying) rotation else 0f),
                        contentScale = ContentScale.Crop
                    )
                }
                DiscAnimationMode.PULSE -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        modifier = coilModifier.graphicsLayer { alpha = pulseAlpha },
                        contentScale = ContentScale.Crop
                    )
                }
                DiscAnimationMode.FLOAT -> {
                    val offsetY = if (isPlaying) 4f else 0f
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        modifier = coilModifier
                            .graphicsLayer { translationY = offsetY * 2f }
                            .rotate(if (isPlaying) rotation * 0.15f else 0f),
                        contentScale = ContentScale.Crop
                    )
                }
                DiscAnimationMode.NONE -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        modifier = coilModifier,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        if (albumArtUri == null && showDefaultIcon) {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                val noteIconSize = if (isPlaying) 48.dp else 36.dp
                NoteIcon(size = noteIconSize, color = Color(0xFFFBB8AC))
            }
        }

        if (isPlaying && animationMode != DiscAnimationMode.NONE) {
            Box(
                modifier = Modifier
                    .size(size.dp + 3.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = Color(0xFFFBB8AC).copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun DiscArtworkCircularReveal(
    albumArtUri: Any?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 200
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_circular")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) Float.MAX_VALUE else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_circular_rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_circular_scale"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0xFF130E0E))
        )

        if (albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Disc",
                modifier = Modifier
                    .size(size.dp - 20.dp)
                    .clip(CircleShape)
                    .rotate(rotation),
                contentScale = ContentScale.Crop
            )
        }

        if (isPlaying) {
            val innerGlow by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "inner_glow"
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBB8AC).copy(alpha = innerGlow))
            )
        }
    }
}

@Composable
private fun NoteIcon(size: androidx.compose.ui.unit.Dp, color: Color) {
    Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = "Música",
        tint = color,
        modifier = Modifier.size(size)
    )
}