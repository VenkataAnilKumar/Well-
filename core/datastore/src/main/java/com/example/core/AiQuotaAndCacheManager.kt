package com.example.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized runtime safety & feature-flag registry prepared for remote kill-switches
 * and urgent clinical safety banners across large user populations.
 */
data class AppSafetyFeatureFlags(
    val cloudAiGloballyEnabled: Boolean = true,
    val dailyAiFairUseLimit: Int = 25,
    val cacheTtlMillis: Long = 6L * 60L * 60L * 1000L, // 6 hours
    val urgentSafetyNotice: String? = null,
    val labBiomarkerAiEnabled: Boolean = true,
    val clarifyingQuestionsEnabled: Boolean = true,
    val proactiveReassessmentEnabled: Boolean = true
)

sealed interface AiQuotaCheckResult {
    data class Allowed(val remainingAfter: Int, val dailyLimit: Int) : AiQuotaCheckResult
    data class DailyLimitReached(val dailyLimit: Int) : AiQuotaCheckResult
}

/**
 * Per-user fair-use AI rate limiter (25 requests/day, resetting at local midnight)
 * paired with content-fingerprinted TTL response caching to control Gemini spend at 1M+ scale.
 * Note: Deterministic emergency & vital safety evaluations never consume AI quota.
 */
class AiQuotaAndCacheManager(
    private val preferencesStore: SanctuaryPreferencesStore,
    private val flagsProvider: () -> AppSafetyFeatureFlags = { AppSafetyFeatureFlags() }
) {
    constructor(
        preferencesStore: SanctuaryPreferencesStore,
        featureFlags: AppSafetyFeatureFlags
    ) : this(preferencesStore, { featureFlags })

    val featureFlags: AppSafetyFeatureFlags
        get() = flagsProvider()

    private data class CacheEntry(
        val fingerprint: String,
        val value: Any,
        val cachedAtMillis: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun fingerprintOf(vararg parts: Any?): String {
        val raw = parts.joinToString("|") { it?.toString().orEmpty() }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getCached(cacheKey: String, fingerprint: String): T? {
        val entry = cache[cacheKey] ?: return null
        val ageMs = System.currentTimeMillis() - entry.cachedAtMillis
        if (entry.fingerprint != fingerprint || ageMs > featureFlags.cacheTtlMillis) {
            cache.remove(cacheKey)
            return null
        }
        return entry.value as? T
    }

    fun <T : Any> putCached(cacheKey: String, fingerprint: String, value: T) {
        cache[cacheKey] = CacheEntry(
            fingerprint = fingerprint,
            value = value,
            cachedAtMillis = System.currentTimeMillis()
        )
    }

    fun clearCache() {
        cache.clear()
    }

    suspend fun tryConsumeDailyQuota(overrideDailyLimit: Int? = null): AiQuotaCheckResult {
        val limit = (overrideDailyLimit ?: featureFlags.dailyAiFairUseLimit).coerceAtLeast(1)
        val today = todayDateKey()
        val consumed = preferencesStore.consumeAiQuotaIfAvailable(todayKey = today, dailyLimit = limit)
        return if (consumed >= 0) {
            AiQuotaCheckResult.Allowed(
                remainingAfter = (limit - consumed).coerceAtLeast(0),
                dailyLimit = limit
            )
        } else {
            AiQuotaCheckResult.DailyLimitReached(dailyLimit = limit)
        }
    }

    fun computeRemaining(settings: SanctuaryDeviceSettings, overrideDailyLimit: Int? = null): Int {
        val limit = (overrideDailyLimit ?: featureFlags.dailyAiFairUseLimit).coerceAtLeast(1)
        val today = todayDateKey()
        val used = if (settings.aiQuotaDateKey == today) settings.aiQuotaUsedToday else 0
        return (limit - used).coerceIn(0, limit)
    }
}

/**
 * Reactive connectivity monitor exposing real-time network availability for graceful offline mode.
 */
class NetworkConnectivityMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = checkCurrentlyOnline()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentlyOnline()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            _isOnline.value = checkCurrentlyOnline()
        }
    }

    private var registered = false

    fun start() {
        val cm = connectivityManager ?: return
        if (registered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            registered = true
            _isOnline.value = checkCurrentlyOnline()
        } catch (_: Exception) {
            _isOnline.value = true
        }
    }

    fun stop() {
        val cm = connectivityManager ?: return
        if (!registered) return
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
            // Safe ignore
        } finally {
            registered = false
        }
    }

    private fun checkCurrentlyOnline(): Boolean {
        val cm = connectivityManager ?: return true
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
