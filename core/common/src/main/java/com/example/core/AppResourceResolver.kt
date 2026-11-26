package com.example.core

import android.content.Context

/**
 * Resolves host-app string resources (such as google-services `default_web_client_id`
 * and `firestore_database_id`) from library modules without depending on `:app`'s `R` class.
 */
object AppResourceResolver {
    fun getStringOrNull(context: Context, resourceName: String): String? {
        val appContext = context.applicationContext ?: context
        val resId = appContext.resources.getIdentifier(resourceName, "string", appContext.packageName)
        if (resId == 0) return null
        return runCatching { appContext.getString(resId) }.getOrNull()
    }

    fun getRequiredString(context: Context, resourceName: String): String {
        return getStringOrNull(context, resourceName)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Required app string resource '$resourceName' not found")
    }
}
