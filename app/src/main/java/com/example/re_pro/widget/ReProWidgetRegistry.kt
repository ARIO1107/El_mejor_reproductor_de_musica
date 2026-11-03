package com.example.re_pro.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

object ReProWidgetRegistry {
    private val widgets: List<GlanceAppWidget> = listOf(
        ReProWidget(),
        ReProWidgetCompact(),
        ReProWidgetLarge()
    )

    suspend fun updateAll(context: Context) {
        widgets.forEach { widget ->
            runCatching { widget.updateAll(context) }
                .onFailure { it.printStackTrace() }
        }
    }
}