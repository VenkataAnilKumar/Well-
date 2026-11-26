package com.example.core

import android.content.Context
import android.util.Log

/**
 * Lightweight observability & crash-reporting abstraction so core/feature modules
 * remain decoupled from Firebase Crashlytics during JVM & Robolectric unit tests.
 */
interface AppTelemetry {
    fun log(tag: String, message: String)
    fun recordNonFatal(tag: String, throwable: Throwable, attributes: Map<String, String> = emptyMap())
    fun setCustomKey(key: String, value: String)
}

object NoOpAppTelemetry : AppTelemetry {
    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun recordNonFatal(tag: String, throwable: Throwable, attributes: Map<String, String>) {
        Log.w(tag, "Non-fatal recorded ($attributes): ${throwable.message}")
    }

    override fun setCustomKey(key: String, value: String) = Unit
}

object AppTelemetryRegistry {
    @Volatile
    var current: AppTelemetry = NoOpAppTelemetry
}

/**
 * Lightweight hook allowing `:core:healthconnect` workers and `:app` container to notify
 * `:feature:widgets` to refresh Glance widgets without introducing reverse module dependencies.
 */
object WidgetRefreshBridge {
    @Volatile
    var onRefreshRequested: ((Context) -> Unit)? = null

    fun notifyDataChanged(context: Context) {
        try {
            onRefreshRequested?.invoke(context.applicationContext)
        } catch (e: Exception) {
            Log.w("WidgetRefreshBridge", "Widget refresh hook failed", e)
        }
    }
}
