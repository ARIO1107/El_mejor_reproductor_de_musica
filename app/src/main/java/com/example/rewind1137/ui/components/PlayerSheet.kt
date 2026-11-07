package com.example.rewind1137.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.example.rewind1137.data.model.Song
import com.example.rewind1137.utils.MusicPlayerManager
import com.example.rewind1137.utils.rememberSongArtwork
import com.example.rewind1137.viewmodel.AlbumArtMode
import com.example.rewind1137.viewmodel.PlaybackMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

private data class SongExtraInfo(
    val sizeBytes: Long? = null,
    val path: String? = null,
    val format: String? = null
)

@Composable
fun PlayerSheet(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackMode: PlaybackMode,
    albumArtMode: AlbumArtMode = AlbumArtMode.STATIC,
    onAlbumArtModeChange: (AlbumArtMode) -> Unit = {},
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onTogglePlaybackMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylistClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onTagEditorClick: () -> Unit = {},
    onAudioInfoClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    lyricsRaw: String = "",
    onSeekToLine: ((Long) -> Unit)? = null,
    isCastConnected: Boolean = false,
    onCastClick: () -> Unit = {},
    audioSessionId: Int = 0,
    openAnchor: Rect? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }
    var accentColor by remember { mutableStateOf(Color(0xFF4A90D9)) }
    var showLyrics by remember { mutableStateOf(false) }
    var showKaraoke by remember { mutableStateOf(false) }
    var showSongInfo by remember { mutableStateOf(false) }
    var songInfo by remember(song?.id) { mutableStateOf<SongExtraInfo?>(null) }

    BackHandler(enabled = showLyrics || showKaraoke) {
        if (showLyrics) showLyrics = false
        if (showKaraoke) showKaraoke = false
    }
    var albumArtRotation by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var artRect by remember { mutableStateOf<Rect?>(null) }
    var morphProgress by remember { mutableFloatStateOf(0f) }
    var hasMorphed by remember { mutableStateOf(false) }
    var showArtModes by remember { mutableStateOf(false) }

    val beatTransition = rememberInfiniteTransition(label = "beat_anim")
    val beatScale by beatTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beat_scale"
    )
    val beatGlow by beatTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beat_glow"
    )

    val breatheTransition = rememberInfiniteTransition(label = "breathe_anim")
    val breatheScale by breatheTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    val zoomTransition = rememberInfiniteTransition(label = "zoom_anim")
    val zoomScale by zoomTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom_scale"
    )

    val spinTransition = rememberInfiniteTransition(label = "spin_anim")
    val spinRotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_rotation"
    )

    val neonTransition = rememberInfiniteTransition(label = "neon_anim")
    val neonPulse by neonTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_pulse"
    )

    val dreamTransition = rememberInfiniteTransition(label = "dream_anim")
    val dreamPulse by dreamTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dream_pulse"
    )

    val waveTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveAngle by waveTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_angle"
    )

    LaunchedEffect(Unit) {
        while (artRect == null) {
            delay(16)
        }
        if (!hasMorphed) {
            hasMorphed = true
            animate(
                initialValue = 0f,
                targetValue = 1f,
                initialVelocity = 0f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            ) { value, _ -> morphProgress = value }
        }
    }

    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var volume by remember {
        mutableFloatStateOf(
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 0.5f) /
            (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 15f)
        )
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 0.5f
                val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 15f
                if (maxVol > 0f) {
                    volume = (currentVol / maxVol).coerceIn(0f, 1f)
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(song?.id, song?.albumArtUri) {
        val artUri = song?.albumArtUri ?: song?.uri
        artUri?.let { uri ->
            val extracted = withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .size(160, 160)
                        .precision(Precision.INEXACT)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val bitmap = (result as? SuccessResult)?.drawable as? android.graphics.drawable.BitmapDrawable
                    bitmap?.let {
                        withContext(Dispatchers.Default) {
                            val palette = androidx.palette.graphics.Palette.from(it.bitmap).generate()
                            palette.getDarkMutedColor(0xFF1A1A1A.toInt()) to
                                palette.getVibrantColor(palette.getLightVibrantColor(0xFF4A90D9.toInt()))
                        }
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (extracted != null) {
                dominantColor = Color(extracted.first)
                accentColor = Color(extracted.second)
            }
        }
    }

    LaunchedEffect(song?.id) {
        songInfo = null
        song?.let { s ->
            try {
                if (s.uri.scheme == "file") {
                    val f = File(s.uri.path.orEmpty())
                    songInfo = SongExtraInfo(
                        sizeBytes = if (f.exists()) f.length() else null,
                        path = f.absolutePath,
                        format = s.uri.path?.substringAfterLast('.', "")?.uppercase()
                    )
                } else {
                    try {
                        context.contentResolver.query(
                            s.uri,
                            arrayOf(MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATA),
                            null, null, null
                        )?.use { c ->
                            if (c.moveToFirst()) {
                                val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                                val path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                                songInfo = SongExtraInfo(
                                    sizeBytes = size,
                                    path = path,
                                    format = path?.substringAfterLast('.', "")?.uppercase()
                                )
                            }
                        }
                    } catch (_: Exception) {}
                    if (songInfo == null) {
                        songInfo = SongExtraInfo(format = s.uri.path?.substringAfterLast('.', "")?.uppercase())
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, albumArtMode) {
        if (isPlaying && albumArtMode == AlbumArtMode.DISC) {
            while (isActive) {
                albumArtRotation += 0.35f
                delay(16)
            }
        }
    }

    val animatedDom by animateColorAsState(targetValue = dominantColor, animationSpec = tween(600), label = "dom")
    val animatedAcc by animateColorAsState(targetValue = accentColor, animationSpec = tween(600), label = "acc")

    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val fontScale = LocalDensity.current.fontScale
    val artWidthFraction = when {
        fontScale > 1.4f -> 0.60f
        fontScale > 1.2f -> 0.70f
        else -> 0.82f
    }

    val sheetBg = MaterialTheme.colorScheme.background
    val TextWhite = MaterialTheme.colorScheme.onBackground
    val TextGray = MaterialTheme.colorScheme.onSurfaceVariant
    val TextDim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val SurfaceGray = MaterialTheme.colorScheme.surfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isPlaying) 0.35f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val artShape = if (albumArtMode == AlbumArtMode.DISC || albumArtMode == AlbumArtMode.SPIN) CircleShape else RoundedCornerShape(24.dp)

    val artAnimModifier = remember(albumArtMode, isPlaying) {
        Modifier.graphicsLayer {
        rotationZ = when (albumArtMode) {
            AlbumArtMode.DISC -> if (isPlaying) albumArtRotation else 0f
            AlbumArtMode.SPIN -> if (isPlaying) spinRotation else 0f
            AlbumArtMode.WAVE -> if (isPlaying) waveAngle else 0f
            else -> 0f
        }
        val s = when (albumArtMode) {
            AlbumArtMode.STATIC -> 1f
            AlbumArtMode.DISC -> 1f
            AlbumArtMode.BEAT -> if (isPlaying) beatScale else 1f
            AlbumArtMode.FUSION -> 1.06f
            AlbumArtMode.BREATHE -> if (isPlaying) breatheScale else 1f
            AlbumArtMode.ZOOM -> if (isPlaying) zoomScale else 1f
            AlbumArtMode.SPIN -> 1f
            AlbumArtMode.NEON -> if (isPlaying) 1f + 0.05f * neonPulse else 1f
            AlbumArtMode.DREAM -> if (isPlaying) dreamPulse else 1f
            AlbumArtMode.WAVE -> 1f
        }
        scaleX = s
        scaleY = s
        alpha = when {
            albumArtMode == AlbumArtMode.FUSION -> 0.45f
            albumArtMode == AlbumArtMode.DREAM && isPlaying -> 0.92f
            else -> 1f
        }
    }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sheetBg)
            .graphicsLayer { alpha = morphProgress }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 100) onDismiss()
                }
            }
    ) {
        if (isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedAcc.copy(alpha = glowAlpha * 0.4f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.15f),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.5f, size.height * 0.15f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedDom.copy(alpha = glowAlpha * 0.2f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.85f),
                        radius = size.width * 0.42f
                    ),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.5f, size.height * 0.85f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .semantics { contentDescription = "Cerrar rewindductor" }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Cerrar", tint = TextGray, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = "Reproduciendo",
                    color = TextDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box {
                        IconButton(
                            onClick = { showArtModes = true },
                            modifier = Modifier
                                .semantics { contentDescription = "Modo de portada: ${albumArtMode.title}" }
                        ) {
                            Icon(
                                Icons.Default.Album,
                                "Modo de portada",
                                tint = if (albumArtMode != AlbumArtMode.STATIC) animatedAcc else TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showArtModes,
                            onDismissRequest = { showArtModes = false }
                        ) {
                            Text(
                                text = "Animación de portada",
                                fontSize = 12.sp,
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            AlbumArtMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.title, fontSize = 13.sp) },
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAlbumArtModeChange(mode)
                                        showArtModes = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            albumArtModeIcon(mode),
                                            contentDescription = null,
                                            tint = if (mode == albumArtMode) animatedAcc else TextDim,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (mode == albumArtMode) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = animatedAcc,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onEqualizerClick,
                        modifier = Modifier
                            .semantics { contentDescription = "Ecualizador" }
                    ) {
                        Icon(Icons.Default.Tune, "Ecualizador", tint = TextGray, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(artWidthFraction)
                    .aspectRatio(1f)
                    .onGloballyPositioned { coords ->
                        if (artRect == null) artRect = coords.boundsInWindow()
                    }
                    .shadow(
                        elevation = 50.dp,
                        shape = artShape,
                        ambientColor = animatedAcc.copy(alpha = 0.25f),
                        spotColor = animatedAcc.copy(alpha = 0.4f)
                    )
                    .clip(artShape)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (abs(swipeOffset) > 150) {
                                    if (swipeOffset > 0) onPreviousClick() else onNextClick()
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = { swipeOffset = 0f },
                            onHorizontalDrag = { _, dragAmount -> swipeOffset += dragAmount }
                        )
                    }
                    .graphicsLayer {
                        translationX = swipeOffset * 0.5f
                        rotationY = swipeOffset * 0.05f

                        val targetRect = artRect
                        val anchorF = openAnchor
                        val p = morphProgress
                        transformOrigin = TransformOrigin(0f, 0f)
                        val scale = if (targetRect != null && anchorF != null && targetRect.width > 0f && anchorF.width > 0f) {
                            lerp(anchorF.width / targetRect.width, 1f, p)
                        } else {
                            lerp(0.94f, 1f, p)
                        }
                        scaleX = scale
                        scaleY = scale
                        if (targetRect != null && anchorF != null) {
                            translationX += lerp(anchorF.left - targetRect.left, 0f, p)
                            translationY += lerp(anchorF.top - targetRect.top, 0f, p)
                        }
                        alpha = lerp(0.5f, 1f, p)
                    },
                contentAlignment = Alignment.Center
) {
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.94f, animationSpec = tween(280)))
                            .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 1.06f, animationSpec = tween(200)))
                    },
                    label = "albumArtCrossfade"
                ) { currentSong ->
                    val artworkModel = rememberSongArtwork(currentSong, maxArtSize = 1024)
                    val songId = currentSong?.id ?: 0L
                    val dateMod = currentSong?.dateAdded ?: 0L
                    val artworkImageBitmap = remember(artworkModel) {
                        (artworkModel as? Bitmap)?.asImageBitmap()
                    }

                    if (artworkImageBitmap != null) {
                        Image(
                            bitmap = artworkImageBitmap,
                            contentDescription = "Carátula del álbum",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(artAnimModifier)
                        )
                    } else if (artworkModel != null) {
                        val uniqueKey = "player_art_v9_${songId}_${dateMod}"
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artworkModel)
                                .memoryCacheKey(uniqueKey)
                                .diskCacheKey(uniqueKey)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Carátula del álbum",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(artAnimModifier)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SurfaceGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }

                when (albumArtMode) {
                    AlbumArtMode.STATIC -> Unit

                    AlbumArtMode.DISC -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xCC111111),
                                        Color(0xE6141414),
                                        Color(0xEE111111)
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.5f
                                ),
                                radius = size.minDimension * 0.5f,
                                center = center
                            )
                            val grooveStroke = 1.dp.toPx()
                            val maxR = size.minDimension * 0.47f
                            var groove = maxR * 0.16f
                            while (groove < maxR * 0.68f) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.05f),
                                    radius = groove,
                                    center = center,
                                    style = Stroke(grooveStroke)
                                )
                                groove += maxR * 0.055f
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color(0xFF0D0D0D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(animatedAcc.copy(alpha = 0.9f))
                            )
                        }
                    }

                    AlbumArtMode.BEAT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = if (isPlaying) beatGlow else 0.15f }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(animatedAcc, Color.Transparent),
                                        center = center,
                                        radius = size.minDimension * 0.7f
                                    ),
                                    radius = size.minDimension * 0.7f,
                                    center = center
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = if (isPlaying) beatGlow * 0.5f else 0.15f }
                                .border(
                                    width = 2.dp,
                                    color = animatedAcc,
                                    shape = artShape
                                )
                        )
                    }

                    AlbumArtMode.FUSION -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        sheetBg.copy(alpha = 0.75f),
                                        sheetBg.copy(alpha = 0.95f)
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.72f
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.dp,
                                    color = animatedDom.copy(alpha = 0.35f),
                                    shape = artShape
                                )
                        )
                    }

                    AlbumArtMode.BREATHE -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        animatedAcc.copy(alpha = 0.1f)
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.7f
                                ),
                                radius = size.minDimension * 0.7f,
                                center = center
                            )
                        }
                    }

                    AlbumArtMode.ZOOM -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent.copy(alpha = 0f),
                                        Color.Black.copy(alpha = 0.18f * neonPulse)
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.72f
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.28f * neonPulse }
                                .border(
                                    width = 1.dp,
                                    color = animatedAcc,
                                    shape = artShape
                                )
                        )
                    }

                    AlbumArtMode.SPIN -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val inset = size.minDimension * 0.05f
                            drawArc(
                                color = animatedAcc.copy(alpha = 0.55f),
                                startAngle = spinRotation,
                                sweepAngle = 210f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawArc(
                                color = animatedAcc.copy(alpha = 0.18f),
                                startAngle = -spinRotation,
                                sweepAngle = 120f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, animatedDom.copy(alpha = 0.3f), artShape)
                        )
                    }

                    AlbumArtMode.NEON -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedAcc.copy(alpha = 0.22f * neonPulse),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.85f
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 3.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(animatedAcc, animatedDom, animatedAcc),
                                        center = Offset.Zero
                                    ),
                                    shape = artShape
                                )
                                .graphicsLayer { shadowElevation = 12.dp.toPx() * neonPulse }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.7f * neonPulse }
                                .border(
                                    width = 1.5.dp,
                                    color = animatedAcc,
                                    shape = artShape
                                )
                        )
                    }

                    AlbumArtMode.DREAM -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedAcc.copy(alpha = 0.16f * dreamPulse),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.75f
                                ),
                                radius = size.minDimension * 0.75f,
                                center = center
                            )
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        sheetBg.copy(alpha = 0.28f * (1f - dreamPulse))
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.4f
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.3f * dreamPulse }
                                .border(
                                    width = 1.dp,
                                    color = animatedAcc,
                                    shape = artShape
                                )
                        )
                    }

                    AlbumArtMode.WAVE -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            fun drawWaves(yBase: Float, alpha: Float) {
                                var x = 0f
                                var prevX = 0f
                                var prevY = yBase + kotlin.math.sin(x / 40f + waveAngle).toFloat() * 5f
                                while (x <= w) {
                                    x += 22f
                                    val sinY = yBase + kotlin.math.sin(x / 40f + waveAngle).toFloat() * 5f
                                    drawLine(
                                        color = animatedAcc.copy(alpha = alpha),
                                        start = Offset(prevX, prevY),
                                        end = Offset(x, sinY),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                    prevX = x
                                    prevY = sinY
                                }
                            }
                            drawWaves(h * 0.2f, 0.28f * neonPulse)
                            drawWaves(h * 0.85f, 0.18f * (1f - neonPulse))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.dp,
                                    color = animatedAcc.copy(alpha = 0.22f),
                                    shape = artShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)).togetherWith(
                        fadeOut(animationSpec = tween(180)) + slideOutVertically { it / 6 }
                    )
                },
                label = "songInfoCrossfade"
            ) { currentSong ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = currentSong?.title ?: "Sin cancion",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artist ?: "",
                        color = TextGray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var isSeeking by remember { mutableStateOf(false) }
            var seekPos by remember { mutableFloatStateOf(0f) }

            val sliderValue = if (isSeeking) seekPos else currentPositionMs.toFloat()
            val displayPos = if (isSeeking) seekPos.toLong() else currentPositionMs

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderValue.coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { newPos ->
                        isSeeking = true
                        seekPos = newPos
                    },
                    onValueChangeFinished = {
                        onSeek(seekPos.toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = animatedAcc,
                        activeTrackColor = animatedAcc,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Progreso" }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(displayPos), color = TextDim, fontSize = 11.sp)
                    Text(formatDuration(durationMs), color = TextDim, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
val favScale = remember { Animatable(1f) }
                LaunchedEffect(isFavorite) {
                    favScale.snapTo(1f)
                    favScale.animateTo(
                        targetValue = 1.25f,
                        animationSpec = tween(durationMillis = 90)
                    )
                    favScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }

                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = favScale.value
                            scaleY = favScale.value
                        }
                        .semantics { contentDescription = if (isFavorite) "Quitar favorito" else "Agregar favorito" }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) animatedAcc else TextDim,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onPreviousClick()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .semantics { contentDescription = "Cancion anterior" }
                ) {
                    Icon(Icons.Default.SkipPrevious, null, tint = TextWhite, modifier = Modifier.size(36.dp))
                }

                Surface(
                    onClick = {
                        onPlayPauseClick()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    shape = CircleShape,
                    color = TextWhite,
                    modifier = Modifier
                        .size(72.dp)
                        .semantics { contentDescription = if (isPlaying) "Pausar" else "Reproducir" }
                ) {
Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200)))
                                    .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.6f, animationSpec = tween(150)))
                            },
                            label = "playPauseIcon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        onNextClick()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .semantics { contentDescription = "Siguiente cancion" }
                ) {
                    Icon(Icons.Default.SkipNext, null, tint = TextWhite, modifier = Modifier.size(36.dp))
                }

                IconButton(
                    onClick = onTogglePlaybackMode,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Modo: ${playbackMode.title}" }
                ) {
                    Icon(
imageVector = when (playbackMode) {
                            PlaybackMode.SEQUENTIAL -> Icons.Default.Repeat
                            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
                            PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = null,
                        tint = if (playbackMode == PlaybackMode.SEQUENTIAL) TextDim else animatedAcc,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.VolumeDown, null, tint = TextDim, modifier = Modifier.size(18.dp))
                Slider(
                    value = volume,
                    onValueChange = { value ->
                        volume = value
                        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                        audioManager?.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (maxVol * value).toInt().coerceIn(0, maxVol),
                            0
                        )
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = animatedAcc,
                        activeTrackColor = animatedAcc,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Volumen" }
                )
                Icon(Icons.Default.VolumeUp, null, tint = TextDim, modifier = Modifier.size(18.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onAddToPlaylistClick,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Añadir a lista de rewindducción" }
                ) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onQueueClick,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Ver cola" }
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Compartir" }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onTagEditorClick,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Editar información" }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { showSongInfo = true },
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Ver información" }
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (lyricsRaw.isNotBlank()) {
                IconButton(
                    onClick = { showLyrics = true },
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Ver letras" }
                ) {
                    Icon(
                        Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { showKaraoke = true },
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Modo karaoke" }
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showSongInfo && song != null) {
            AudioInfoDialog(
                song = song,
                onDismiss = { showSongInfo = false }
            )
        }

        val lyricsBgLuminance = animatedDom.luminance() * 0.85f + sheetBg.luminance() * 0.15f
        val lyricsBaseColor = if (lyricsBgLuminance > 0.5f) Color(0xFF1C1C20) else Color(0xFFE8E8EE)
        val lyricsDimColor = if (lyricsBgLuminance > 0.5f) Color(0xFF55555E) else Color(0xFFA9ACB6)

        AnimatedVisibility(
            visible = showLyrics && lyricsRaw.isNotBlank(),
            enter = fadeIn(animationSpec = tween(260)) +
                slideInVertically(animationSpec = tween(260), initialOffsetY = { it / 4 }),
            exit = fadeOut(animationSpec = tween(220)) +
                slideOutVertically(animationSpec = tween(220), targetOffsetY = { it / 4 }),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(animatedDom.copy(alpha = 0.85f), sheetBg)
                        )
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 100) showLyrics = false
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(animatedAcc.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.1f),
                            radius = size.width * 0.75f
                        ),
                        radius = size.width * 0.75f,
                        center = Offset(size.width * 0.5f, size.height * 0.1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showLyrics = false }) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                "Cerrar letras",
                                tint = TextGray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Letras",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = buildString {
                                    song?.title?.takeIf { it.isNotBlank() }?.let { append(it) }
                                    song?.artist?.takeIf { it.isNotBlank() }?.let {
                                        if (isNotEmpty()) append("  ·  ")
                                        append(it)
                                    }
                                },
                                color = TextGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = animatedAcc,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    val animatedLyricsProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(300),
                        label = "lyricsProgress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedLyricsProgress.coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .background(animatedAcc)
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LyricsView(
                            lyricsRaw = lyricsRaw,
                            currentPositionMs = currentPositionMs,
                            accentColor = animatedAcc,
                            baseTextColor = lyricsBaseColor,
                            dimTextColor = lyricsDimColor,
                            onSeekToLine = onSeekToLine,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(sheetBg.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, sheetBg.copy(alpha = 0.9f))
                                    )
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Desliza hacia abajo para cerrar",
                            color = TextDim,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showKaraoke && lyricsRaw.isNotBlank(),
            enter = fadeIn(animationSpec = tween(260)) +
                slideInVertically(animationSpec = tween(260), initialOffsetY = { it / 4 }),
            exit = fadeOut(animationSpec = tween(220)) +
                slideOutVertically(animationSpec = tween(220), targetOffsetY = { it / 4 }),
            modifier = Modifier.fillMaxSize()
        ) {
            KaraokeOverlay(
                lyricsRaw = lyricsRaw,
                song = song,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                accentColor = animatedAcc,
                dominantColor = animatedDom,
                sheetBg = sheetBg,
                isPlaying = isPlaying,
                onSeekToLine = onSeekToLine,
                onDismiss = { showKaraoke = false }
            )
        }
    }
}

@Composable
private fun KaraokeOverlay(
    lyricsRaw: String,
    song: Song?,
    currentPositionMs: Long,
    durationMs: Long,
    accentColor: Color,
    dominantColor: Color,
    sheetBg: Color,
    isPlaying: Boolean,
    onSeekToLine: ((Long) -> Unit)?,
    onDismiss: () -> Unit
) {
    val karaokeBgLuminance = dominantColor.luminance() * 0.9f + sheetBg.luminance() * 0.1f
    val onSurface = if (karaokeBgLuminance > 0.5f) Color(0xFF1C1C20) else Color(0xFFE8E8EE)
    val onSurfaceVariant = if (karaokeBgLuminance > 0.5f) Color(0xFF55555E) else Color(0xFFA9ACB6)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val lines = remember(lyricsRaw) { parseLrcText(lyricsRaw) }
    val activeIndex = remember(currentPositionMs, lines) {
        if (lines.isEmpty()) -1 else lines.indexOfLast { it.timeMs <= currentPositionMs + 220L }
    }

    var vocalRemovalEnabled by remember {
        mutableStateOf(MusicPlayerManager.isVocalRemovalEnabled())
    }
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "karaokeProgress"
    )

    val activeLine = lines.getOrNull(activeIndex)
    val prevLine = lines.getOrNull(activeIndex - 1)
    val nextLine = lines.getOrNull(activeIndex + 1)

    val lineDuration = if (activeLine != null) {
        val start = activeLine.timeMs
        val end = if (activeIndex + 1 < lines.size) lines[activeIndex + 1].timeMs else start + 6000L
        (end - start).coerceAtLeast(1L)
    } else 1L

    val charProgress = if (activeLine != null) {
        ((currentPositionMs - activeLine.timeMs).toFloat() / lineDuration).coerceIn(0f, 1f)
    } else 0f

    val activeText = remember(activeLine, charProgress, accentColor) {
        if (activeLine == null) {
            androidx.compose.ui.text.AnnotatedString("")
        } else {
            val fillCount = (activeLine.text.length * charProgress).toInt().coerceIn(0, activeLine.text.length)
            buildAnnotatedString {
                if (fillCount > 0) {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = accentColor, fontWeight = FontWeight.ExtraBold)) {
                        append(activeLine.text.take(fillCount))
                    }
                }
                append(activeLine.text.drop(fillCount))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(dominantColor.copy(alpha = 0.9f), sheetBg)))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 100) onDismiss()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.16f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.5f, size.height * 0.16f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        "Cerrar karaoke",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Karaoke",
                        color = onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = buildString {
                            song?.title?.takeIf { it.isNotBlank() }?.let { append(it) }
                            song?.artist?.takeIf { it.isNotBlank() }?.let {
                                if (isNotEmpty()) append("  ·  ")
                                append(it)
                            }
                        },
                        color = onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    prevLine?.let { prev ->
                        Text(
                            text = prev.text,
                            color = onSurfaceVariant.copy(alpha = 0.35f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable { onSeekToLine?.invoke(prev.timeMs) }
                                .padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = activeText,
                        color = onSurface,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 42.sp,
                        modifier = Modifier
                            .clickable { activeLine?.let { onSeekToLine?.invoke(it.timeMs) } }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    nextLine?.let { next ->
                        Text(
                            text = next.text,
                            color = onSurfaceVariant.copy(alpha = 0.35f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable { onSeekToLine?.invoke(next.timeMs) }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = surfaceVariant.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (vocalRemovalEnabled) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (vocalRemovalEnabled) accentColor else onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Eliminar voz",
                            color = onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (vocalRemovalEnabled) {
                                "Activo · minimiza la voz principal (L−R)"
                            } else {
                                "Aísla la música para cantar encima"
                            },
                            color = onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                    Switch(
                        checked = vocalRemovalEnabled,
                        onCheckedChange = { value ->
                            MusicPlayerManager.setVocalRemovalEnabled(value)
                            vocalRemovalEnabled = value
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = onSurface.copy(alpha = 0.9f),
                            checkedTrackColor = accentColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SongInfoDialog(
    song: Song,
    extra: SongExtraInfo?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Información de la canción", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Título", song.title)
                InfoRow("Artista", song.artist)
                InfoRow("Álbum", song.album)
                InfoRow("Género", song.genre)
                InfoRow("Duración", formatDuration(song.duration))
                if (song.dateAdded > 0) InfoRow("Agregada", formatDate(song.dateAdded))
                extra?.format?.let { InfoRow("Formato", it) }
                extra?.sizeBytes?.let { InfoRow("Tamaño", formatFileSize(it)) }
                extra?.path?.let { InfoRow("Ruta", it) }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value.ifBlank { "—" },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Desconocido"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[i])
}

private fun formatDate(epochMillis: Long): String {
    return try {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(epochMillis))
    } catch (_: Exception) {
        ""
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun albumArtModeIcon(mode: AlbumArtMode): ImageVector = when (mode) {
    AlbumArtMode.STATIC -> Icons.Default.Image
    AlbumArtMode.DISC -> Icons.Default.Album
    AlbumArtMode.BEAT -> Icons.Default.GraphicEq
    AlbumArtMode.FUSION -> Icons.Default.Layers
    AlbumArtMode.BREATHE -> Icons.Default.Animation
    AlbumArtMode.ZOOM -> Icons.Default.ZoomIn
    AlbumArtMode.SPIN -> Icons.Default.RotateRight
    AlbumArtMode.NEON -> Icons.Default.FlashOn
    AlbumArtMode.DREAM -> Icons.Default.AutoAwesome
    AlbumArtMode.WAVE -> Icons.Default.Waves
}
