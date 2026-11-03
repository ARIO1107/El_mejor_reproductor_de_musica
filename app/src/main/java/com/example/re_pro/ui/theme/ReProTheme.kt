package com.example.re_pro.ui.theme

import androidx.compose.runtime.Composable
import com.example.re_pro.data.model.AppThemeMode

@Composable
fun ReProTheme(
    themeMode: AppThemeMode = AppThemeMode.REPRO_NEON_BLACK,
    content: @Composable () -> Unit
) {
    ReProAppTheme(
        themeMode = themeMode,
        content = content
    )
}

