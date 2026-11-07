package com.example.rewind1137.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra inferior de acciones para la selección múltiple (pulsación larga).
 * Aparece como una píldora flotante vidriosa con acciones claras.
 */
@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    isMultiSelectMode: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onPlayNow: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isMultiSelectMode,
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(animationSpec = tween(260), initialOffsetY = { it / 2 }),
        exit = fadeOut(animationSpec = tween(180)) +
            slideOutVertically(animationSpec = tween(240), targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MultiSelectActionItem(
                    icon = Icons.Default.Close,
                    label = "",
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                    onClick = onClearSelection,
                    contentDescription = "Cancelar selección"
                )

                MultiSelectActionItem(
                    icon = Icons.Default.SelectAll,
                    label = "Todo",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    onClick = onSelectAll,
                    contentDescription = "Seleccionar todo"
                )

                MultiSelectActionItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Reproducir",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    onClick = onPlayNow,
                    contentDescription = "Reproducir selección"
                )

                MultiSelectActionItem(
                    icon = Icons.Default.PlaylistAdd,
                    label = "Lista",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    onClick = onAddToPlaylist,
                    contentDescription = "Agregar a playlist"
                )

                MultiSelectActionItem(
                    icon = Icons.Default.Favorite,
                    label = "Favorito",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    onClick = onToggleFavorite,
                    contentDescription = "Marcar como favoritas"
                )

                MultiSelectActionItem(
                    icon = Icons.Default.Share,
                    label = "Compartir",
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    onClick = onShare,
                    contentDescription = "Compartir selección"
                )

                MultiSelectActionItem(
                    icon = Icons.Default.Delete,
                    label = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                    contentDescription = "Eliminar del dispositivo"
                )
            }
        }
    }
}

@Composable
private fun MultiSelectActionItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    contentDescription: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = tint.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Barra superior de multi-selección (compatibilidad con uso previo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit
) {
    MultiSelectActionBar(
        selectedCount = selectedCount,
        isMultiSelectMode = selectedCount > 0,
        onClearSelection = onClearSelection,
        onSelectAll = onSelectAll,
        onPlayNow = { },
        onAddToPlaylist = onAddToPlaylist,
        onToggleFavorite = onToggleFavorite,
        onShare = onShare,
        onDelete = onDelete
    )
}