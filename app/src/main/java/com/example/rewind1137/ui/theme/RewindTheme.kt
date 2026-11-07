package com.example.rewind1137.ui.theme

import androidx.compose.runtime.Composable
import com.example.rewind1137.data.model.AppThemeMode

@Composable
fun RewindTheme(
    themeMode: AppThemeMode = AppThemeMode.REWIND_NEON_BLACK,
    content: @Composable () -> Unit
) {
    RewindAppTheme(
        themeMode = themeMode,
        content = content
    )
}

