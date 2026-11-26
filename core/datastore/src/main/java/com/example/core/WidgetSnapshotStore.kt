package com.example.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Compact, privacy-safe summary persisted for Glance Home Screen widgets
 * (Quick Mood Check-In + Next Medication Dose + latest Health Connect glance).
 */
data class WidgetSnapshot(
    val todayDateKey: String = "",
    val todayMoodLevel: Int = 0,
    val todayMoodLabel: String = "",
    val streakDays: Int = 0,
    val nextMedId: String = "",
    val nextMedName: String = "",
    val nextMedDosage: String = "",
    val nextMedTimeLabel: String = "",
    val takenTodayCount: Int = 0,
    val activeMedsCount: Int = 0,
    val lastStepsToday: Long = -1L,
    val lastHeartRateBpm: Long = -1L,
    val lastHealthSyncMillis: Long = 0L,
    val emergencyWidgetEnabled: Boolean = false,
    val emergencyDisplayName: String = "",
    val emergencyBloodType: String = "",
    val emergencyAllergiesSummary: String = "",
    val emergencyMedsSummary: String = "",
    val emergencyContactLabel: String = "",
    val emergencyContactPhone: String = "",
    val emergencyDialNumber: String = "911"
)

class WidgetSnapshotStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): WidgetSnapshot = WidgetSnapshot(
        todayDateKey = prefs.getString(KEY_TODAY_DATE, "").orEmpty(),
        todayMoodLevel = prefs.getInt(KEY_TODAY_MOOD_LEVEL, 0),
        todayMoodLabel = prefs.getString(KEY_TODAY_MOOD_LABEL, "").orEmpty(),
        streakDays = prefs.getInt(KEY_STREAK_DAYS, 0),
        nextMedId = prefs.getString(KEY_NEXT_MED_ID, "").orEmpty(),
        nextMedName = prefs.getString(KEY_NEXT_MED_NAME, "").orEmpty(),
        nextMedDosage = prefs.getString(KEY_NEXT_MED_DOSAGE, "").orEmpty(),
        nextMedTimeLabel = prefs.getString(KEY_NEXT_MED_TIME, "").orEmpty(),
        takenTodayCount = prefs.getInt(KEY_TAKEN_TODAY, 0),
        activeMedsCount = prefs.getInt(KEY_ACTIVE_MEDS, 0),
        lastStepsToday = prefs.getLong(KEY_LAST_STEPS, -1L),
        lastHeartRateBpm = prefs.getLong(KEY_LAST_HR, -1L),
        lastHealthSyncMillis = prefs.getLong(KEY_LAST_HC_SYNC, 0L),
        emergencyWidgetEnabled = prefs.getBoolean(KEY_EMERG_ENABLED, false),
        emergencyDisplayName = prefs.getString(KEY_EMERG_NAME, "").orEmpty(),
        emergencyBloodType = prefs.getString(KEY_EMERG_BLOOD, "").orEmpty(),
        emergencyAllergiesSummary = prefs.getString(KEY_EMERG_ALLERGIES, "").orEmpty(),
        emergencyMedsSummary = prefs.getString(KEY_EMERG_MEDS, "").orEmpty(),
        emergencyContactLabel = prefs.getString(KEY_EMERG_CONTACT_LABEL, "").orEmpty(),
        emergencyContactPhone = prefs.getString(KEY_EMERG_CONTACT_PHONE, "").orEmpty(),
        emergencyDialNumber = prefs.getString(KEY_EMERG_DIAL, "911").orEmpty().ifBlank { "911" }
    )

    fun updateMood(
        dateKey: String,
        moodLevel: Int,
        moodLabel: String,
        streakDays: Int
    ) {
        prefs.edit()
            .putString(KEY_TODAY_DATE, dateKey)
            .putInt(KEY_TODAY_MOOD_LEVEL, moodLevel.coerceIn(0, 5))
            .putString(KEY_TODAY_MOOD_LABEL, moodLabel.take(32))
            .putInt(KEY_STREAK_DAYS, streakDays.coerceAtLeast(0))
            .apply()
    }

    fun updateMedicationSummary(
        nextMedId: String,
        nextMedName: String,
        nextMedDosage: String,
        nextMedTimeLabel: String,
        takenTodayCount: Int,
        activeMedsCount: Int
    ) {
        prefs.edit()
            .putString(KEY_NEXT_MED_ID, nextMedId.take(128))
            .putString(KEY_NEXT_MED_NAME, nextMedName.take(80))
            .putString(KEY_NEXT_MED_DOSAGE, nextMedDosage.take(40))
            .putString(KEY_NEXT_MED_TIME, nextMedTimeLabel.take(24))
            .putInt(KEY_TAKEN_TODAY, takenTodayCount.coerceAtLeast(0))
            .putInt(KEY_ACTIVE_MEDS, activeMedsCount.coerceAtLeast(0))
            .apply()
    }

    fun updateHealthConnectSummary(
        stepsToday: Long?,
        heartRateBpm: Long?,
        syncedAtMillis: Long
    ) {
        prefs.edit()
            .putLong(KEY_LAST_STEPS, stepsToday ?: -1L)
            .putLong(KEY_LAST_HR, heartRateBpm ?: -1L)
            .putLong(KEY_LAST_HC_SYNC, syncedAtMillis)
            .apply()
    }

    fun updateEmergencyId(
        enabled: Boolean,
        displayName: String,
        bloodType: String,
        allergiesSummary: String,
        medsSummary: String,
        contactLabel: String,
        contactPhone: String,
        emergencyDialNumber: String
    ) {
        if (!enabled) {
            prefs.edit()
                .putBoolean(KEY_EMERG_ENABLED, false)
                .remove(KEY_EMERG_NAME)
                .remove(KEY_EMERG_BLOOD)
                .remove(KEY_EMERG_ALLERGIES)
                .remove(KEY_EMERG_MEDS)
                .remove(KEY_EMERG_CONTACT_LABEL)
                .remove(KEY_EMERG_CONTACT_PHONE)
                .apply()
            return
        }
        prefs.edit()
            .putBoolean(KEY_EMERG_ENABLED, true)
            .putString(KEY_EMERG_NAME, displayName.trim().take(60))
            .putString(KEY_EMERG_BLOOD, bloodType.trim().take(16))
            .putString(KEY_EMERG_ALLERGIES, allergiesSummary.trim().take(160))
            .putString(KEY_EMERG_MEDS, medsSummary.trim().take(160))
            .putString(KEY_EMERG_CONTACT_LABEL, contactLabel.trim().take(60))
            .putString(KEY_EMERG_CONTACT_PHONE, contactPhone.trim().take(30))
            .putString(KEY_EMERG_DIAL, emergencyDialNumber.trim().take(12).ifBlank { "911" })
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "wellplus_widget_snapshot"
        private const val KEY_TODAY_DATE = "today_date_key"
        private const val KEY_TODAY_MOOD_LEVEL = "today_mood_level"
        private const val KEY_TODAY_MOOD_LABEL = "today_mood_label"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_NEXT_MED_ID = "next_med_id"
        private const val KEY_NEXT_MED_NAME = "next_med_name"
        private const val KEY_NEXT_MED_DOSAGE = "next_med_dosage"
        private const val KEY_NEXT_MED_TIME = "next_med_time"
        private const val KEY_TAKEN_TODAY = "taken_today_count"
        private const val KEY_ACTIVE_MEDS = "active_meds_count"
        private const val KEY_LAST_STEPS = "last_steps_today"
        private const val KEY_LAST_HR = "last_hr_bpm"
        private const val KEY_LAST_HC_SYNC = "last_hc_sync_millis"
        private const val KEY_EMERG_ENABLED = "emerg_widget_enabled"
        private const val KEY_EMERG_NAME = "emerg_name"
        private const val KEY_EMERG_BLOOD = "emerg_blood"
        private const val KEY_EMERG_ALLERGIES = "emerg_allergies"
        private const val KEY_EMERG_MEDS = "emerg_meds"
        private const val KEY_EMERG_CONTACT_LABEL = "emerg_contact_label"
        private const val KEY_EMERG_CONTACT_PHONE = "emerg_contact_phone"
        private const val KEY_EMERG_DIAL = "emerg_dial_number"
    }
}
