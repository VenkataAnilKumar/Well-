package com.example.core

import com.example.data.model.CycleFlowLevel
import com.example.testing.WellPlusTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleAndHighlightsModuleTest {

    @Test
    fun cycleAnalytics_estimatesNextPeriodFromFlowStarts() {
        val logs = listOf(
            WellPlusTestFixtures.sampleCycleLog("2026-10-01", CycleFlowLevel.MEDIUM),
            WellPlusTestFixtures.sampleCycleLog("2026-10-02", CycleFlowLevel.HEAVY),
            WellPlusTestFixtures.sampleCycleLog("2026-10-29", CycleFlowLevel.MEDIUM),
            WellPlusTestFixtures.sampleCycleLog("2026-10-30", CycleFlowLevel.LIGHT)
        )
        val summary = CycleAnalytics.computePrediction(
            logs = logs,
            typicalCycleLengthDays = 28,
            todayDateKey = "2026-11-10"
        )
        assertNotNull(summary)
        assertEquals(28, summary.estimatedCycleLength)
        assertEquals(2, summary.loggedPeriodStartsCount)
        assertEquals("2026-10-29", summary.lastPeriodStartDateKey)
        assertTrue(summary.nextPeriodDateLabel != null)
    }

    @Test
    fun cycleAnalytics_handlesEmptyLogsGracefully() {
        val summary = CycleAnalytics.computePrediction(
            logs = emptyList(),
            typicalCycleLengthDays = 30,
            todayDateKey = "2026-11-10"
        )
        assertEquals(30, summary.estimatedCycleLength)
        assertEquals(0, summary.loggedPeriodStartsCount)
        assertEquals(null, summary.currentCycleDay)
        assertEquals(null, summary.nextPeriodDateLabel)
    }
}
