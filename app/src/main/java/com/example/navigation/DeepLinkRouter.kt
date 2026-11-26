package com.example.navigation

import android.content.Intent
import com.example.core.ReminderScheduler

/**
 * Strongly-typed representation of notification deep-link destinations handled after
 * Auth, Onboarding, and Biometric Lock gates have cleared.
 */
sealed interface PendingDeepLink {
    data object Medications : PendingDeepLink
    data object DailyCheckIn : PendingDeepLink
    data class FollowUp(val consultationId: String?) : PendingDeepLink
    data object EmergencyMedicalId : PendingDeepLink
}

object DeepLinkParser {
    const val DEST_EMERGENCY_ID = "emergency_id"

    fun fromIntent(intent: Intent?): PendingDeepLink? {
        if (intent == null) return null
        val dest = intent.getStringExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val consultationId = intent.getStringExtra(ReminderScheduler.EXTRA_CONSULTATION_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return when (dest) {
            ReminderScheduler.DEST_MEDICATIONS -> PendingDeepLink.Medications
            ReminderScheduler.DEST_CHECK_IN -> PendingDeepLink.DailyCheckIn
            ReminderScheduler.DEST_FOLLOW_UP -> PendingDeepLink.FollowUp(consultationId)
            DEST_EMERGENCY_ID -> PendingDeepLink.EmergencyMedicalId
            else -> null
        }
    }
}
