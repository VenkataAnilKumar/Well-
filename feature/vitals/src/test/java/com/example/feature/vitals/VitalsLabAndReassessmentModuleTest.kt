package com.example.feature.vitals

import com.example.data.model.LabBiomarkerStatus
import com.example.data.model.VitalRangeStatus
import com.example.data.model.VitalType
import com.example.testing.FakeAiEntitlementProvider
import com.example.testing.WellPlusTestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsLabAndReassessmentModuleTest {

    @Test
    fun abnormalVital_triggersNonNormalRangeStatusForProactiveReassessment() {
        val highBp = WellPlusTestFixtures.sampleVital(
            type = VitalType.BLOOD_PRESSURE,
            primary = 152.0,
            secondary = 96.0
        )
        assertNotEquals(VitalRangeStatus.NORMAL, highBp.rangeStatus)
    }

    @Test
    fun labBiomarkerSeries_computesDeltaBetweenPanels() {
        val older = WellPlusTestFixtures.sampleLabBiomarker(
            id = "lab-old",
            markerName = "HbA1c",
            value = 6.1,
            status = LabBiomarkerStatus.BORDERLINE,
            dateKey = "2026-05-10"
        )
        val newer = WellPlusTestFixtures.sampleLabBiomarker(
            id = "lab-new",
            markerName = "HbA1c",
            value = 5.5,
            status = LabBiomarkerStatus.OPTIMAL,
            dateKey = "2026-11-10"
        )
        val ordered = listOf(older, newer).sortedByDescending { it.panelDateKey }
        assertEquals("lab-new", ordered.first().id)
        val delta = ordered[0].value - ordered[1].value
        assertTrue(delta < 0.0)
    }

    @Test
    fun fakeEntitlementProvider_respectsLabAndReassessmentFlagsAndQuota() = runTest {
        val fake = FakeAiEntitlementProvider(dailyLimit = 2, labBiomarkerAiEnabled = true)
        assertTrue(fake.labBiomarkerAiEnabled)
        assertTrue(fake.proactiveReassessmentEnabled)
        fake.tryConsumeDailyQuota()
        fake.tryConsumeDailyQuota()
        assertEquals(0, fake.computeRemaining(com.example.core.SanctuaryDeviceSettings()))
    }
}
