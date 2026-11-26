package com.example.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WidgetSnapshotAndFlagsTest {

    @Test
    fun widgetSnapshotStore_persistsMoodMedsAndHealthConnectData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WidgetSnapshotStore(context)
        store.clear()

        store.updateMood(
            dateKey = "2026-11-25",
            moodLevel = 4,
            moodLabel = "Calm",
            streakDays = 6
        )
        store.updateMedicationSummary(
            nextMedId = "med-1",
            nextMedName = "Lisinopril",
            nextMedDosage = "10 mg",
            nextMedTimeLabel = "Morning",
            takenTodayCount = 1,
            activeMedsCount = 2
        )
        store.updateHealthConnectSummary(
            stepsToday = 6420L,
            heartRateBpm = 68L,
            syncedAtMillis = 1_700_000_000_000L
        )

        val snap = store.read()
        assertEquals("2026-11-25", snap.todayDateKey)
        assertEquals(4, snap.todayMoodLevel)
        assertEquals("Calm", snap.todayMoodLabel)
        assertEquals(6, snap.streakDays)
        assertEquals("Lisinopril", snap.nextMedName)
        assertEquals(1, snap.takenTodayCount)
        assertEquals(2, snap.activeMedsCount)
        assertEquals(6420L, snap.lastStepsToday)
        assertEquals(68L, snap.lastHeartRateBpm)

        store.clear()
        val cleared = store.read()
        assertEquals(0, cleared.todayMoodLevel)
        assertEquals(-1L, cleared.lastStepsToday)
    }

    @Test
    fun quotaAndCacheManager_readsDynamicRemoteConfigFlags() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SanctuaryPreferencesStore(context)
        var activeFlags = AppSafetyFeatureFlags(
            cloudAiGloballyEnabled = true,
            dailyAiFairUseLimit = 20,
            labBiomarkerAiEnabled = true,
            clarifyingQuestionsEnabled = false,
            proactiveReassessmentEnabled = true
        )
        val manager = AiQuotaAndCacheManager(prefs) { activeFlags }
        val provider = LocalAiEntitlementProvider(manager)

        assertTrue(provider.labBiomarkerAiEnabled)
        assertFalse(provider.clarifyingQuestionsEnabled)
        assertEquals(20, provider.dailyLimit)

        activeFlags = activeFlags.copy(
            dailyAiFairUseLimit = 40,
            clarifyingQuestionsEnabled = true,
            urgentSafetyNotice = "Maintenance window"
        )
        assertTrue(provider.clarifyingQuestionsEnabled)
        assertEquals(40, provider.dailyLimit)
        assertEquals("Maintenance window", provider.urgentSafetyNotice)
    }

    @Test
    fun emergencyIdWidgetSnapshot_respectsOptInOptOutAndSignOutClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WidgetSnapshotStore(context)
        store.clear()

        // Default must be OFF
        assertFalse(store.read().emergencyWidgetEnabled)

        store.updateEmergencyId(
            enabled = true,
            displayName = "Maya Lin",
            bloodType = "O+",
            allergiesSummary = "Penicillin, Peanuts",
            medsSummary = "Lisinopril 10 mg",
            contactLabel = "Daniel Lin",
            contactPhone = "+1-555-0199",
            emergencyDialNumber = "911"
        )
        val enabledSnap = store.read()
        assertTrue(enabledSnap.emergencyWidgetEnabled)
        assertEquals("Maya Lin", enabledSnap.emergencyDisplayName)
        assertEquals("O+", enabledSnap.emergencyBloodType)
        assertEquals("Penicillin, Peanuts", enabledSnap.emergencyAllergiesSummary)
        assertEquals("+1-555-0199", enabledSnap.emergencyContactPhone)

        // Opting out immediately scrubs sensitive Medical ID fields
        store.updateEmergencyId(
            enabled = false,
            displayName = "Maya Lin",
            bloodType = "O+",
            allergiesSummary = "Penicillin",
            medsSummary = "Lisinopril",
            contactLabel = "Daniel",
            contactPhone = "+1-555-0199",
            emergencyDialNumber = "911"
        )
        val disabledSnap = store.read()
        assertFalse(disabledSnap.emergencyWidgetEnabled)
        assertEquals("", disabledSnap.emergencyDisplayName)
        assertEquals("", disabledSnap.emergencyAllergiesSummary)
        assertEquals("", disabledSnap.emergencyContactPhone)
    }

    @Test
    fun entitlementProvider_scalesDailyQuotaForWellPlusProTier() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SanctuaryPreferencesStore(context)
        var tier = com.example.data.model.WellPlusTier.FREE
        val manager = AiQuotaAndCacheManager(prefs) { AppSafetyFeatureFlags(dailyAiFairUseLimit = 25) }
        val provider = LocalAiEntitlementProvider(
            quotaAndCacheManager = manager,
            subscriptionTierProvider = { tier }
        )

        assertFalse(provider.isProTier)
        assertEquals(25, provider.dailyLimit)

        tier = com.example.data.model.WellPlusTier.PRO
        assertTrue(provider.isProTier)
        assertEquals(200, provider.dailyLimit)
    }
}
