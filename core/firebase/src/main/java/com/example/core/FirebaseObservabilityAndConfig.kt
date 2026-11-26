package com.example.core

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val OBSERVABILITY_TAG = "WellPlusObservability"

/**
 * Production Firebase Crashlytics telemetry adapter.
 * Safe in unit/Robolectric environments where FirebaseApp may not be initialized.
 */
class FirebaseCrashlyticsTelemetry : AppTelemetry {

    private fun crashlyticsOrNull() = try {
        Firebase.crashlytics
    } catch (_: Exception) {
        null
    }

    override fun log(tag: String, message: String) {
        Log.d(tag, message)
        try {
            crashlyticsOrNull()?.log("[$tag] ${message.take(240)}")
        } catch (_: Exception) {
            // Safe ignore
        }
    }

    override fun recordNonFatal(
        tag: String,
        throwable: Throwable,
        attributes: Map<String, String>
    ) {
        Log.w(tag, "Non-fatal exception: ${throwable.message}")
        try {
            val crashlytics = crashlyticsOrNull() ?: return
            attributes.forEach { (k, v) ->
                crashlytics.setCustomKey(k.take(40), v.take(120))
            }
            crashlytics.log("[$tag] ${throwable.javaClass.simpleName}: ${throwable.message?.take(200)}")
            crashlytics.recordException(throwable)
        } catch (_: Exception) {
            // Safe ignore
        }
    }

    override fun setCustomKey(key: String, value: String) {
        try {
            crashlyticsOrNull()?.setCustomKey(key.take(40), value.take(120))
        } catch (_: Exception) {
            // Safe ignore
        }
    }
}

/**
 * Live Firebase Remote Config provider for clinical safety kill-switches, daily AI fair-use
 * quotas, urgent clinical safety banners, and staged P1 AI feature toggles.
 */
class RemoteConfigSafetyFlagProvider(
    initialDefaults: AppSafetyFeatureFlags = AppSafetyFeatureFlags()
) {
    private val _flags = MutableStateFlow(initialDefaults)
    val flags: StateFlow<AppSafetyFeatureFlags> = _flags.asStateFlow()

    val currentFlags: AppSafetyFeatureFlags
        get() = _flags.value

    fun initializeAndFetchAsync(minimumFetchIntervalSeconds: Long = 3_600L) {
        try {
            val rc = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds
            }
            rc.setConfigSettingsAsync(configSettings)
            rc.setDefaultsAsync(
                mapOf(
                    KEY_CLOUD_AI_ENABLED to true,
                    KEY_DAILY_AI_LIMIT to 25L,
                    KEY_URGENT_SAFETY_NOTICE to "",
                    KEY_LAB_BIOMARKER_AI_ENABLED to true,
                    KEY_CLARIFYING_QUESTIONS_ENABLED to true,
                    KEY_PROACTIVE_REASSESSMENT_ENABLED to true
                )
            )
            applyLoadedValues()
            rc.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    applyLoadedValues()
                    Log.i(OBSERVABILITY_TAG, "Remote Config flags activated: ${_flags.value}")
                }
            }
        } catch (e: Exception) {
            Log.w(OBSERVABILITY_TAG, "Remote Config unavailable; retaining safe local defaults", e)
        }
    }

    private fun applyLoadedValues() {
        try {
            val rc = Firebase.remoteConfig
            val notice = rc.getString(KEY_URGENT_SAFETY_NOTICE).trim().takeIf { it.isNotEmpty() }
            val limit = rc.getLong(KEY_DAILY_AI_LIMIT).toInt().coerceIn(5, 200)
            _flags.value = _flags.value.copy(
                cloudAiGloballyEnabled = rc.getBoolean(KEY_CLOUD_AI_ENABLED),
                dailyAiFairUseLimit = limit,
                urgentSafetyNotice = notice,
                labBiomarkerAiEnabled = rc.getBoolean(KEY_LAB_BIOMARKER_AI_ENABLED),
                clarifyingQuestionsEnabled = rc.getBoolean(KEY_CLARIFYING_QUESTIONS_ENABLED),
                proactiveReassessmentEnabled = rc.getBoolean(KEY_PROACTIVE_REASSESSMENT_ENABLED)
            )
        } catch (e: Exception) {
            Log.w(OBSERVABILITY_TAG, "Could not read Remote Config values", e)
        }
    }

    companion object {
        const val KEY_CLOUD_AI_ENABLED = "cloud_ai_globally_enabled"
        const val KEY_DAILY_AI_LIMIT = "daily_ai_fair_use_limit"
        const val KEY_URGENT_SAFETY_NOTICE = "urgent_safety_notice"
        const val KEY_LAB_BIOMARKER_AI_ENABLED = "lab_biomarker_ai_enabled"
        const val KEY_CLARIFYING_QUESTIONS_ENABLED = "clarifying_questions_enabled"
        const val KEY_PROACTIVE_REASSESSMENT_ENABLED = "proactive_reassessment_enabled"
    }
}
