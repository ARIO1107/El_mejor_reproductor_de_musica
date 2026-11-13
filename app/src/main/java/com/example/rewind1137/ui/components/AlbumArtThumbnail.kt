package com.example.rewind1137.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rewind1137.R
import com.example.rewind1137.utils.AlbumArtUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumArtThumbnail(
    albumArtUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 8.dp,
    isCurrentSong: Boolean = false,
    showBadge: Boolean = false,
    songId: Long = 0L,
    contentUri: Uri? = null,
    dateModified: Long = 0L
) {
    val boxColor = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    // Solo se muestra la portada resuelta para ESTA canción. Nunca se usa la
    // carátula de álbum compartida como respaldo temporal: esa era la causa de
    // que varias canciones mostraran la misma portada al desplazarse rápido.
    val artworkBitmap = rememberThumbnailArtwork(songId, contentUri, albumArtUri)
    val artworkAlpha by animateFloatAsState(
        targetValue = if (artworkBitmap != null) 1f else 0f,
        animationSpec = tween(180),
        label = "thumbnailFade"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(shapeRadius))
            .background(boxColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { alpha = 1f - artworkAlpha }
        )

        if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap,
                contentDescription = "Portada de la canción",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = artworkAlpha }
            )
        }

        if (showBadge) {
            Image(
                painter = painterResource(id = R.drawable.ic_piranha_logo),
                contentDescription = "Rewind 1137 Badge",
                modifier = Modifier
                    .size(size * 0.35f)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

/**
 * Devuelve la miniatura exacta de la canción (arte embebido primero y, solo si
 * no existe, la portada del álbum). La lectura es sincrona desde la caché para
 * que al reciclar celdas no aparezca ninguna imagen incorrecta.
 */
@Composable
private fun rememberThumbnailArtwork(
    songId: Long,
    contentUri: Uri?,
    albumArtUri: Uri?
): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(songId, albumArtUri) {
        mutableStateOf(if (songId > 0L) AlbumArtUtils.getCachedThumbnail(songId) else null)
    }

    LaunchedEffect(songId, contentUri, albumArtUri) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = if (songId > 0L) {
            AlbumArtUtils.getSongArtwork(
                context = context,
                songId = songId,
                contentUri = contentUri,
                albumArtUri = albumArtUri,
                maxSize = AlbumArtUtils.THUMBNAIL_ART_SIZE
            )
        } else {
            withContext(Dispatchers.IO) {
                AlbumArtUtils.loadArtworkFromUri(
                    context = context,
                    uri = albumArtUri,
                    maxSize = AlbumArtUtils.THUMBNAIL_ART_SIZE
                )
            }
        }
    }

    return remember(bitmap) { bitmap?.asImageBitmap() }
}
