package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sanctuaryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sana_care_device_prefs"
)

data class SanctuaryDeviceSettings(
    val biometricLockEnabled: Boolean = false,
    val screenPrivacyEnabled: Boolean = true,
    val cloudAiEnabled: Boolean = true,
    val preferredHealthRegion: String = "",
    val emergencyRegionOverride: String = "",
    val aiQuotaDateKey: String = "",
    val aiQuotaUsedToday: Int = 0,
    val checkInReminderEnabled: Boolean = false,
    val checkInReminderHour: Int = 20,
    val checkInReminderMinute: Int = 0,
    val medRemindersEnabled: Boolean = false,
    val followUpRemindersEnabled: Boolean = true,
    val cycleTrackingEnabled: Boolean = false,
    val typicalCycleLengthDays: Int = 28,
    val useMetricBodyUnits: Boolean = false,
    val backgroundHcSyncEnabled: Boolean = false,
    val autoMaskPiiBeforeScan: Boolean = true,
    val showEmergencyIdOnWidget: Boolean = false
)

class SanctuaryPreferencesStore(private val context: Context) {

    private object Keys {
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        val SCREEN_PRIVACY = booleanPreferencesKey("screen_privacy_enabled")
        val CLOUD_AI_ENABLED = booleanPreferencesKey("cloud_ai_enabled")
        val PREFERRED_HEALTH_REGION = stringPreferencesKey("preferred_health_region")
        val EMERGENCY_REGION_OVERRIDE = stringPreferencesKey("emergency_region_override")
        val AI_QUOTA_DATE_KEY = stringPreferencesKey("ai_quota_date_key")
        val AI_QUOTA_USED_TODAY = intPreferencesKey("ai_quota_used_today")
        val CHECKIN_REMINDER_ENABLED = booleanPreferencesKey("checkin_reminder_enabled")
        val CHECKIN_REMINDER_HOUR = intPreferencesKey("checkin_reminder_hour")
        val CHECKIN_REMINDER_MINUTE = intPreferencesKey("checkin_reminder_minute")
        val MED_REMINDERS_ENABLED = booleanPreferencesKey("med_reminders_enabled")
        val FOLLOWUP_REMINDERS_ENABLED = booleanPreferencesKey("followup_reminders_enabled")
        val CYCLE_TRACKING_ENABLED = booleanPreferencesKey("cycle_tracking_enabled")
        val TYPICAL_CYCLE_LENGTH_DAYS = intPreferencesKey("typical_cycle_length_days")
        val USE_METRIC_BODY_UNITS = booleanPreferencesKey("use_metric_body_units")
        val BACKGROUND_HC_SYNC_ENABLED = booleanPreferencesKey("background_hc_sync_enabled")
        val AUTO_MASK_PII_BEFORE_SCAN = booleanPreferencesKey("auto_mask_pii_before_scan")
        val SHOW_EMERGENCY_ID_ON_WIDGET = booleanPreferencesKey("show_emergency_id_on_widget")
    }

    val settingsFlow: Flow<SanctuaryDeviceSettings> =
        context.sanctuaryDataStore.data.map { prefs ->
            SanctuaryDeviceSettings(
                biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
                screenPrivacyEnabled = prefs[Keys.SCREEN_PRIVACY] ?: true,
                cloudAiEnabled = prefs[Keys.CLOUD_AI_ENABLED] ?: true,
                preferredHealthRegion = prefs[Keys.PREFERRED_HEALTH_REGION].orEmpty(),
                emergencyRegionOverride = prefs[Keys.EMERGENCY_REGION_OVERRIDE].orEmpty(),
                aiQuotaDateKey = prefs[Keys.AI_QUOTA_DATE_KEY].orEmpty(),
                aiQuotaUsedToday = (prefs[Keys.AI_QUOTA_USED_TODAY] ?: 0).coerceAtLeast(0),
                checkInReminderEnabled = prefs[Keys.CHECKIN_REMINDER_ENABLED] ?: false,
                checkInReminderHour = (prefs[Keys.CHECKIN_REMINDER_HOUR] ?: 20).coerceIn(0, 23),
                checkInReminderMinute = (prefs[Keys.CHECKIN_REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
                medRemindersEnabled = prefs[Keys.MED_REMINDERS_ENABLED] ?: false,
                followUpRemindersEnabled = prefs[Keys.FOLLOWUP_REMINDERS_ENABLED] ?: true,
                cycleTrackingEnabled = prefs[Keys.CYCLE_TRACKING_ENABLED] ?: false,
                typicalCycleLengthDays = (prefs[Keys.TYPICAL_CYCLE_LENGTH_DAYS] ?: 28).coerceIn(21, 45),
                useMetricBodyUnits = prefs[Keys.USE_METRIC_BODY_UNITS] ?: false,
                backgroundHcSyncEnabled = prefs[Keys.BACKGROUND_HC_SYNC_ENABLED] ?: false,
                autoMaskPiiBeforeScan = prefs[Keys.AUTO_MASK_PII_BEFORE_SCAN] ?: true,
                showEmergencyIdOnWidget = prefs[Keys.SHOW_EMERGENCY_ID_ON_WIDGET] ?: false
            )
        }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setScreenPrivacyEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.SCREEN_PRIVACY] = enabled }
    }

    suspend fun setCloudAiEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.CLOUD_AI_ENABLED] = enabled }
    }

    suspend fun setPreferredHealthRegion(regionCode: String) {
        context.sanctuaryDataStore.edit {
            it[Keys.PREFERRED_HEALTH_REGION] = regionCode.trim().uppercase().take(8)
        }
    }

    suspend fun setEmergencyRegionOverride(countryCode: String) {
        context.sanctuaryDataStore.edit {
            it[Keys.EMERGENCY_REGION_OVERRIDE] = countryCode.trim().uppercase().take(8)
        }
    }

    /**
     * Atomically consumes 1 AI request credit if under [dailyLimit] for [todayKey].
     * Returns the new used count for today, or -1 if the daily limit has already been reached.
     */
    suspend fun consumeAiQuotaIfAvailable(todayKey: String, dailyLimit: Int): Int {
        var resultCount = -1
        context.sanctuaryDataStore.edit { prefs ->
            val storedDate = prefs[Keys.AI_QUOTA_DATE_KEY].orEmpty()
            val currentUsed = if (storedDate == todayKey) {
                (prefs[Keys.AI_QUOTA_USED_TODAY] ?: 0).coerceAtLeast(0)
            } else {
                0
            }
            if (currentUsed < dailyLimit) {
                val nextUsed = currentUsed + 1
                prefs[Keys.AI_QUOTA_DATE_KEY] = todayKey
                prefs[Keys.AI_QUOTA_USED_TODAY] = nextUsed
                resultCount = nextUsed
            } else {
                resultCount = -1
            }
        }
        return resultCount
    }

    suspend fun setCheckInReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        context.sanctuaryDataStore.edit { prefs ->
            prefs[Keys.CHECKIN_REMINDER_ENABLED] = enabled
            prefs[Keys.CHECKIN_REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.CHECKIN_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setMedicationRemindersEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.MED_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setFollowUpRemindersEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.FOLLOWUP_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setCycleTrackingEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.CYCLE_TRACKING_ENABLED] = enabled }
    }

    suspend fun setTypicalCycleLengthDays(days: Int) {
        context.sanctuaryDataStore.edit { it[Keys.TYPICAL_CYCLE_LENGTH_DAYS] = days.coerceIn(21, 45) }
    }

    suspend fun setUseMetricBodyUnits(useMetric: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.USE_METRIC_BODY_UNITS] = useMetric }
    }

    suspend fun setBackgroundHcSyncEnabled(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.BACKGROUND_HC_SYNC_ENABLED] = enabled }
    }

    suspend fun setAutoMaskPiiBeforeScan(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.AUTO_MASK_PII_BEFORE_SCAN] = enabled }
    }

    suspend fun setShowEmergencyIdOnWidget(enabled: Boolean) {
        context.sanctuaryDataStore.edit { it[Keys.SHOW_EMERGENCY_ID_ON_WIDGET] = enabled }
    }

    suspend fun clearAll() {
        context.sanctuaryDataStore.edit { it.clear() }
    }
}
