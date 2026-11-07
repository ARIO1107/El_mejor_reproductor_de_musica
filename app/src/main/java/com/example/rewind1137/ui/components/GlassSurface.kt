package com.example.rewind1137.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contenedor glassmorphism reutilizable.
 * Adaptativo y dinámico según el tema activo (Light/Dark/Pastel).
 */
@Composable
fun GlassmorphicContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    blurRadius: Dp = 24.dp,
    glassAlpha: Float = 0.85f,
    borderAlpha: Float = 0.15f,
    gradientColors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseSurface = MaterialTheme.colorScheme.surface
    val baseSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val colors = gradientColors ?: listOf(
        baseSurface.copy(alpha = glassAlpha),
        baseSurfaceVariant.copy(alpha = glassAlpha * 0.85f)
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .blur(blurRadius)
                .background(Brush.verticalGradient(colors))
                .border(1.dp, outlineColor.copy(alpha = borderAlpha), shape)
        )
        content()
    }
}

@Composable
fun DarkGlassmorphicContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    blurRadius: Dp = 24.dp,
    glassAlpha: Float = 0.85f,
    borderAlpha: Float = 0.15f,
    content: @Composable BoxScope.() -> Unit
) {
    GlassmorphicContainer(
        modifier = modifier,
        cornerRadius = cornerRadius,
        blurRadius = blurRadius,
        glassAlpha = glassAlpha,
        borderAlpha = borderAlpha,
        content = content
    )
}
