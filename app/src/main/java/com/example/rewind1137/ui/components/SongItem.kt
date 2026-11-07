package com.example.rewind1137.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.viewmodel.NowPlayingAnimation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isCompact: Boolean = false,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    nowPlayingAnimation: NowPlayingAnimation = NowPlayingAnimation.BARS
) {
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label = "songSelection"
    )
    val shape = RoundedCornerShape(12.dp)
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        isCurrentSong -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .graphicsLayer {
                val s = 1f - 0.02f * selectionProgress
                scaleX = s
                scaleY = s
            }
            .border(
                width = (1.5f * selectionProgress).dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f * selectionProgress),
                shape = shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            ),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 6.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtThumbnail(
                    albumArtUri = song.albumArtUri,
                    size = if (isCompact) 36.dp else 48.dp,
                    shapeRadius = if (isCompact) 8.dp else 10.dp,
                    isCurrentSong = isCurrentSong,
                    songId = song.id,
                    contentUri = song.uri,
                    dateModified = song.dateAdded
                )

                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))

                Column {
                    Text(
                        text = song.title,
                        color = if (isCurrentSong && !isMultiSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isCompact) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isMultiSelectMode) {
                SelectionBadge(
                    selected = isSelected,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                AnimatedVisibility(
                    visible = isCurrentSong,
                    enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    NowPlayingIndicator(
                        animation = nowPlayingAnimation,
                        color = MaterialTheme.colorScheme.primary,
                        isPlaying = isPlaying
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionBadge(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "selectionCheck"
    )
    val scale = 0.85f + 0.15f * progress
    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            )
            .border(
                width = 1.6.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + 0.3f * progress),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Seleccionada",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
