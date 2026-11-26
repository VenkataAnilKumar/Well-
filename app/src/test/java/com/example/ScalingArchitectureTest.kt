package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.core.DateKeyProvider
import com.example.core.MedicationActionReceiver
import com.example.core.ReminderScheduler
import com.example.data.model.Consultation
import com.example.data.model.VitalReading
import com.example.data.repository.MedicationRepository
import com.example.data.repository.PaginationMerger
import com.example.navigation.DeepLinkParser
import com.example.navigation.PendingDeepLink
import com.example.navigation.WellPlusRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScalingArchitectureTest {

    @Test
    fun paginationMerger_mergesLiveHeadAndTailWithoutDuplicatesAndPreservesOrder() {
        val liveHead = listOf(
            Consultation(id = "c4", title = "Newest"),
            Consultation(id = "c3", title = "Updated Head Copy"),
            Consultation(id = "c2", title = "Second")
        )
        val olderTail = listOf(
            Consultation(id = "c3", title = "Stale Tail Copy"),
            Consultation(id = "c1", title = "Oldest")
        )

        val merged = PaginationMerger.mergeLiveHeadAndTail(
            liveHead = liveHead,
            olderTail = olderTail,
            idSelector = { it.id }
        )

        assertEquals(listOf("c4", "c3", "c2", "c1"), merged.map { it.id })
        assertEquals("Updated Head Copy", merged[1].title)
    }

    @Test
    fun paginationMerger_appendOlderPageIsIdempotent() {
        val page1 = listOf(VitalReading(id = "v2"), VitalReading(id = "v1"))
        val page2WithOverlap = listOf(VitalReading(id = "v1"), VitalReading(id = "v0"))

        val combined = PaginationMerger.appendOlderPage(page1, page2WithOverlap) { it.id }
        assertEquals(listOf("v2", "v1", "v0"), combined.map { it.id })

        val reapplied = PaginationMerger.appendOlderPage(combined, page2WithOverlap) { it.id }
        assertEquals(listOf("v2", "v1", "v0"), reapplied.map { it.id })
    }

    @Test
    fun dateKeyProvider_detectsMidnightRolloverWithInjectedClock() {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.NOVEMBER, 25, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var nowMillis = cal.timeInMillis
        val provider = DateKeyProvider { nowMillis }

        assertEquals("2026-11-25", provider.todayKey())
        assertEquals("2026-11-25", provider.todayDateKeyFlow.value)
        assertFalse(provider.refresh())

        // Advance past midnight into next day
        nowMillis += 2 * 60 * 1000L
        assertTrue(provider.refresh())
        assertEquals("2026-11-26", provider.todayDateKeyFlow.value)
        assertEquals("2026-11-26", provider.todayKey())
    }

    @Test
    fun medicationLogId_sanitizesSpecialCharactersAndBoundsLength() {
        val id = MedicationRepository.buildMedicationLogId("2026-11-25", "med/123 *abc_def-9")
        assertEquals("2026-11-25_med123abc_def-9", id)
        assertTrue(id.length <= 128)
    }

    @Test
    fun medicationActionReceiver_generatesUniqueNonCollidingRequestCodesAndValidIntents() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seenCodes = mutableSetOf<Int>()
        for (notifId in 4000..4025) {
            val takenCode = MedicationActionReceiver.markTakenRequestCode(notifId)
            val snoozeCode = MedicationActionReceiver.snoozeRequestCode(notifId)
            assertNotEquals(takenCode, snoozeCode)
            assertTrue("Duplicate taken request code for $notifId", seenCodes.add(takenCode))
            assertTrue("Duplicate snooze request code for $notifId", seenCodes.add(snoozeCode))
        }

        val takenIntent = MedicationActionReceiver.createMarkTakenIntent(
            context = context,
            medicationId = "med_lisinopril",
            medicationName = "Lisinopril 10mg",
            notifId = 4012
        )
        assertEquals(MedicationActionReceiver.ACTION_MARK_TAKEN, takenIntent.action)
        assertEquals("med_lisinopril", takenIntent.getStringExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID))
        assertEquals("Lisinopril 10mg", takenIntent.getStringExtra(MedicationActionReceiver.EXTRA_MEDICATION_NAME))
        assertEquals(4012, takenIntent.getIntExtra(MedicationActionReceiver.EXTRA_NOTIF_ID, -1))

        val snoozeIntent = MedicationActionReceiver.createSnoozeIntent(
            context = context,
            medicationId = "med_lisinopril",
            medicationName = "Lisinopril 10mg",
            title = "Morning Dose",
            body = "Take with water",
            notifId = 4012
        )
        assertEquals(MedicationActionReceiver.ACTION_SNOOZE_30M, snoozeIntent.action)
        assertEquals("Morning Dose", snoozeIntent.getStringExtra(MedicationActionReceiver.EXTRA_TITLE))
        assertEquals("Take with water", snoozeIntent.getStringExtra(MedicationActionReceiver.EXTRA_BODY))
        assertEquals(4012, snoozeIntent.getIntExtra(MedicationActionReceiver.EXTRA_NOTIF_ID, -1))
    }

    @Test
    fun deepLinkParser_mapsNotificationDestinationsAccurately() {
        assertNull(DeepLinkParser.fromIntent(null))
        assertNull(DeepLinkParser.fromIntent(Intent()))

        val medsIntent = Intent().apply {
            putExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION, ReminderScheduler.DEST_MEDICATIONS)
        }
        assertEquals(PendingDeepLink.Medications, DeepLinkParser.fromIntent(medsIntent))

        val checkInIntent = Intent().apply {
            putExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION, ReminderScheduler.DEST_CHECK_IN)
        }
        assertEquals(PendingDeepLink.DailyCheckIn, DeepLinkParser.fromIntent(checkInIntent))

        val followUpWithId = Intent().apply {
            putExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION, ReminderScheduler.DEST_FOLLOW_UP)
            putExtra(ReminderScheduler.EXTRA_CONSULTATION_ID, "consult_abc123")
        }
        assertEquals(
            PendingDeepLink.FollowUp("consult_abc123"),
            DeepLinkParser.fromIntent(followUpWithId)
        )

        val followUpNoId = Intent().apply {
            putExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION, ReminderScheduler.DEST_FOLLOW_UP)
        }
        assertEquals(
            PendingDeepLink.FollowUp(null),
            DeepLinkParser.fromIntent(followUpNoId)
        )

        val unknownIntent = Intent().apply {
            putExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION, "unknown_screen")
        }
        assertNull(DeepLinkParser.fromIntent(unknownIntent))
    }

    @Test
    fun wellPlusRoutes_distinguishesTopLevelTabsFromFullScreenDetails() {
        assertTrue(WellPlusRoute.isTopLevelTab(WellPlusRoute.Today.route))
        assertTrue(WellPlusRoute.isTopLevelTab(WellPlusRoute.Meds.route))
        assertTrue(WellPlusRoute.isTopLevelTab(WellPlusRoute.Consult.route))
        assertTrue(WellPlusRoute.isTopLevelTab(WellPlusRoute.History.route))
        assertTrue(WellPlusRoute.isTopLevelTab(WellPlusRoute.Profile.route))

        assertFalse(WellPlusRoute.isTopLevelTab(WellPlusRoute.DailyCheckIn.route))
        assertFalse(WellPlusRoute.isTopLevelTab(WellPlusRoute.Vitals.route))
        assertFalse(WellPlusRoute.isTopLevelTab(WellPlusRoute.MentalWellbeing.route))
        assertFalse(WellPlusRoute.isTopLevelTab(WellPlusRoute.BodyAndCycle.route))
        assertFalse(WellPlusRoute.isTopLevelTab(WellPlusRoute.HealthConnect.route))
        assertFalse(WellPlusRoute.isTopLevelTab(null))
    }
}
