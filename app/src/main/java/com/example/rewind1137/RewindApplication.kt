package com.example.rewind1137

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

@HiltAndroidApp
class RewindApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 1. Configuración del manejador de bloqueos/crases personalizado
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(filesDir, "crash_log.txt")
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                file.writeText(
                    buildString {
                        appendLine("=== Crash ${Date()} ===")
                        appendLine("Thread: ${thread.name}")
                        appendLine("Version: ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})")
                        appendLine()
                        append(writer.toString())
                    }
                )
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
