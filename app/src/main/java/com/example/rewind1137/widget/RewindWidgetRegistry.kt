package com.example.rewind1137.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

object RewindWidgetRegistry {
    private val widgets: List<GlanceAppWidget> = listOf(
        RewindWidget(),
        RewindWidgetCompact(),
        RewindWidgetLarge()
    )

    suspend fun updateAll(context: Context) {
        widgets.forEach { widget ->
            runCatching { widget.updateAll(context) }
                .onFailure { it.printStackTrace() }
        }
    }
}