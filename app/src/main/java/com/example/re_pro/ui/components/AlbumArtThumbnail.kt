package com.example.re_pro.ui.components

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.re_pro.R
import com.example.re_pro.utils.rememberSongArtwork

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

    val context = LocalContext.current
    val targetPx = with(LocalDensity.current) { (size * 2f).roundToPx() }.coerceIn(128, 512)
    val artworkModel = rememberSongArtwork(
        songId = songId,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        maxArtSize = targetPx
    )
    val artworkBitmap = remember(artworkModel) {
        (artworkModel as? Bitmap)?.asImageBitmap()
    }

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
            tint = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap,
                contentDescription = "Portada de la canción",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (albumArtUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .size(targetPx)
                    .crossfade(true)
                    .build(),
                contentDescription = "Portada de la canción",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showBadge) {
            Image(
                painter = painterResource(id = R.drawable.ic_piranha_logo),
                contentDescription = "Re-Pro Badge",
                modifier = Modifier
                    .size(size * 0.35f)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}
