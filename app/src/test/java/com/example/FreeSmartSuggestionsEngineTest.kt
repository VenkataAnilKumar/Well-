package com.example

import com.example.core.FreeSmartSuggestionsEngine
import com.example.core.HealthRegionDirectory
import com.example.core.SmartSuggestionCategory
import com.example.data.model.DailyCheckIn
import com.example.data.model.MedicationItem
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.MentalSeverityBand
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeSmartSuggestionsEngineTest {

    @Test
    fun elevatedBpAndPendingMedsPrioritizedFirst() {
        val highBp = VitalReading(
            id = "bp1",
            vitalType = VitalType.BLOOD_PRESSURE.name,
            primaryValue = 148.0,
            secondaryValue = 94.0,
            unit = "mmHg"
        )
        val med = MedicationItem(
            id = "m1",
            name = "Lisinopril",
            dosage = "10 mg",
            active = true
        )
        val lowSleepCheckIn = DailyCheckIn(
            id = "c1",
            dateKey = "2026-11-22",
            sleepHours = 5.0,
            hydrationGlasses = 3
        )

        val suggestions = FreeSmartSuggestionsEngine.computeDailySuggestions(
            todayDateKey = "2026-11-22",
            todayCheckIn = lowSleepCheckIn,
            recentCheckIns = listOf(lowSleepCheckIn),
            vitals = listOf(highBp),
            medications = listOf(med),
            medicationLogs = emptyList(),
            mentalAssessments = emptyList(),
            healthConnectSnapshot = null,
            region = HealthRegionDirectory.CA
        )

        assertTrue(suggestions.size == 3)
        assertTrue(suggestions.any { it.category == SmartSuggestionCategory.VITALS && it.isHighPriority })
        assertTrue(suggestions.any { it.category == SmartSuggestionCategory.MEDICATIONS && it.isHighPriority })
        assertTrue(suggestions.any { it.category == SmartSuggestionCategory.SLEEP && it.isHighPriority })
    }

    @Test
    fun brandNewUserReceivesHelpfulStarterSuggestions() {
        val suggestions = FreeSmartSuggestionsEngine.computeDailySuggestions(
            todayDateKey = "2026-11-22",
            todayCheckIn = null,
            recentCheckIns = emptyList(),
            vitals = emptyList(),
            medications = emptyList(),
            medicationLogs = emptyList(),
            mentalAssessments = emptyList(),
            healthConnectSnapshot = null,
            region = HealthRegionDirectory.US
        )

        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.id == "daily_checkin_prompt" })
        assertTrue(suggestions.any { it.id == "starter_baseline_vital" })
    }

    @Test
    fun mentalWellbeingGuidanceMatchesSeverityAndCrisisFlag() {
        val severePhq9 = MentalAssessment(
            id = "a1",
            assessmentType = MentalAssessmentType.PHQ9.name,
            totalScore = 20,
            maxScore = 27,
            severityBand = MentalSeverityBand.SEVERE.name,
            itemScores = listOf(2, 2, 3, 3, 2, 2, 2, 2, 2),
            crisisFlagged = true
        )
        val minimalGad7 = MentalAssessment(
            id = "a2",
            assessmentType = MentalAssessmentType.GAD7.name,
            totalScore = 2,
            maxScore = 21,
            severityBand = MentalSeverityBand.MINIMAL.name,
            itemScores = listOf(1, 0, 1, 0, 0, 0, 0),
            crisisFlagged = false
        )

        val severeGuide = FreeSmartSuggestionsEngine.computeMentalWellbeingGuidance(
            assessment = severePhq9,
            recentCheckIns = listOf(DailyCheckIn(dateKey = "2026-11-22", sleepHours = 5.2))
        )
        val minimalGuide = FreeSmartSuggestionsEngine.computeMentalWellbeingGuidance(
            assessment = minimalGad7,
            recentCheckIns = emptyList()
        )

        assertTrue(severeGuide.encourageProfessionalSupport)
        assertTrue(severeGuide.empatheticSummary.contains("988"))
        assertTrue(severeGuide.gentleCopingSteps.size >= 3)
        assertTrue(severeGuide.clinicianTalkingPoints.size >= 2)

        assertFalse(minimalGuide.encourageProfessionalSupport)
        assertTrue(minimalGuide.gentleCopingSteps.isNotEmpty())
    }

    @Test
    fun sedentaryCheckInTriggersMovementSuggestion() {
        val sedentaryCheckIn = DailyCheckIn(
            id = "c2",
            dateKey = "2026-11-22",
            moodLevel = 4,
            energyLevel = 3,
            sleepHours = 7.5,
            hydrationGlasses = 8,
            activityLevel = com.example.data.model.PhysicalActivityLevel.SEDENTARY.name,
            activityMinutes = 0L
        )
        val suggestions = FreeSmartSuggestionsEngine.computeDailySuggestions(
            todayDateKey = "2026-11-22",
            todayCheckIn = sedentaryCheckIn,
            recentCheckIns = listOf(sedentaryCheckIn),
            vitals = emptyList(),
            medications = emptyList(),
            medicationLogs = emptyList(),
            mentalAssessments = emptyList(),
            healthConnectSnapshot = null,
            region = HealthRegionDirectory.US
        )
        assertTrue(suggestions.any { it.id == "movement_checkin_boost" && it.category == SmartSuggestionCategory.MOVEMENT })
    }
}
