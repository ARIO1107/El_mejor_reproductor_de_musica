package com.example.rewind1137.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.lerp
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.utils.AlbumArtUtils

/**
 * Estado de la carátula de una canción que conserva siempre la última portada
 * válida en pantalla. Mientras la portada nueva se decodifica se sigue viendo
 * la anterior y, cuando está lista, se funde hacia ella.
 *
 * Con esto se elimina el parpadeo portada -> nota musical -> icono -> portada
 * que ocurría al cambiar de canción.
 */
@Stable
class SongArtworkState internal constructor(
    private val context: Context,
    private val maxArtSize: Int,
    initial: Bitmap?
) {
    /** Portada visible actualmente. */
    var current by mutableStateOf(initial)
        private set

    /** Portada que se está desvaneciendo (solo durante la transición). */
    var previous by mutableStateOf<Bitmap?>(null)
        private set

    /** 0f = se ve [previous], 1f = se ve [current]. */
    var progress by mutableFloatStateOf(1f)
        private set

    internal suspend fun load(song: Song?) {
        val songId = song?.id ?: 0L
        if (song == null || songId <= 0L) {
            previous = null
            current = null
            progress = 1f
            return
        }

        // getCachedArt evita cualquier parpadeo si la portada ya está en memoria.
        val bitmap = AlbumArtUtils.getCachedArt(songId, maxArtSize)
            ?: AlbumArtUtils.getSongArtwork(
                context = context,
                songId = songId,
                contentUri = song.uri,
                albumArtUri = song.albumArtUri,
                maxSize = maxArtSize
            )

        if (bitmap == null) {
            // La canción no tiene portada: se muestra el marcador de posición.
            previous = null
            current = null
            progress = 1f
            return
        }

        if (bitmap === current) return

        previous = current
        current = bitmap
        progress = 0f
        try {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = CROSSFADE_MS,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                progress = value
            }
        } finally {
            // Si la animación se cancela (cambio rápido de canción) se deja la
            // portada nueva totalmente visible para que el siguiente cambio
            // arranque desde ella y no desde un estado intermedio.
            previous = null
            progress = 1f
        }
    }

    private companion object {
        const val CROSSFADE_MS = 420
    }
}

@Composable
fun rememberSongArtworkState(
    song: Song?,
    maxArtSize: Int = 1024
): SongArtworkState {
    val context = LocalContext.current.applicationContext
    val state = remember(maxArtSize) {
        SongArtworkState(
            context = context,
            maxArtSize = maxArtSize,
            initial = AlbumArtUtils.getCachedArt(song?.id ?: 0L, maxArtSize)
        )
    }
    LaunchedEffect(song?.id, song?.uri, song?.albumArtUri, maxArtSize) {
        state.load(song)
    }
    return state
}

/**
 * Dibuja la portada conservada por [state] con un fundido suave entre la
 * anterior y la nueva. Nunca muestra estados intermedios.
 */
@Composable
fun SongArtworkLayers(
    state: SongArtworkState,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    opacity: Float = 1f,
    soften: Int = 0,
    contentDescription: String? = "Carátula del álbum",
    placeholder: (@Composable () -> Unit)? = null
) {
    val current = state.current
    val previous = state.previous
    val currentImage = remember(current, soften) { current.toDisplayImage(soften) }
    val previousImage = remember(previous, soften) { previous.toDisplayImage(soften) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (currentImage == null && previousImage == null) {
            placeholder?.invoke()
        }

        if (previousImage != null) {
            Image(
                bitmap = previousImage,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = opacity * (1f - state.progress)
                        val s = lerp(1f, 1.06f, state.progress)
                        scaleX = s
                        scaleY = s
                    }
            )
        }

        if (currentImage != null) {
            Image(
                bitmap = currentImage,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = if (previousImage != null) state.progress else 1f
                        alpha = opacity * p
                        val s = lerp(0.94f, 1f, p)
                        scaleX = s
                        scaleY = s
                    }
            )
        }
    }
}

/**
 * Versión reducida de la portada usada para la luz ambiental: al escalarla se
 * ve como un degradado suave de la carátula.
 */
private fun Bitmap?.toDisplayImage(soften: Int): ImageBitmap? {
    if (this == null) return null
    return if (soften > 0 && (width > soften || height > soften)) {
        Bitmap.createScaledBitmap(this, soften, soften, true).asImageBitmap()
    } else {
        asImageBitmap()
    }
}
