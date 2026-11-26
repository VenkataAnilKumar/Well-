package com.example

import com.example.core.ClinicalSafetyEngine
import com.example.core.CycleAnalytics
import com.example.core.HealthHighlightsEngine
import com.example.core.HealthRegionDirectory
import com.example.core.ReminderScheduler
import com.example.data.model.BmiCategory
import com.example.data.model.BodyMeasurement
import com.example.data.model.ChatMessage
import com.example.data.model.ConsultationType
import com.example.data.model.CycleFlowLevel
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.HighlightCategory
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.MentalSeverityBand
import com.example.data.model.MessageRole
import com.example.data.model.PhysicalActivityLevel
import com.example.data.model.TrendDirection
import com.example.data.model.TrendSentiment
import com.example.data.model.VitalType
import com.example.data.repository.normalizeFollowUpChatTurns
import com.example.ui.screens.GeneralHealthStatus
import com.example.ui.screens.WellnessNoteCodec
import com.example.ui.viewmodel.IntakeDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end JVM verification suite covering core Well+ clinical, wellness,
 * AI chat turn normalization, safety guardrails, cycle/body analytics,
 * highlights, and reminder workflows.
 */
class EndToEndWorkflowAuditTest {

    @Test
    fun followUpChatNormalization_recoversCleanlyAfterOrphanedUserTurn() {
        val existing = listOf(
            ChatMessage(id = "1", role = MessageRole.USER.name, content = "Should I ice my ankle?"),
            ChatMessage(id = "2", role = MessageRole.DOCTOR.name, content = "Yes, 15 minutes at a time."),
            // Simulate a previously failed request where USER message was saved without DOCTOR reply
            ChatMessage(id = "3", role = MessageRole.USER.name, content = "What if swelling lasts 3 days?")
        )

        val (history, outgoing) = normalizeFollowUpChatTurns(
            seedUserText = "Seed context",
            seedModelText = "Initial triage",
            existingMessages = existing,
            newUserMessage = "Also, can I walk on it?"
        )

        // History must strictly alternate user -> model and end on model
        assertEquals("user", history.first().first)
        assertEquals("model", history.last().first)
        for (i in 1 until history.size) {
            assertTrue(
                "Adjacent turns at index $i must alternate roles",
                history[i].first != history[i - 1].first
            )
        }
        // Trailing orphan message must be folded into the outgoing user prompt
        assertTrue(outgoing.contains("What if swelling lasts 3 days?"))
        assertTrue(outgoing.contains("Also, can I walk on it?"))
    }

    @Test
    fun followUpChatNormalization_mergesAdjacentModelTurnsAndDeduplicatesRetry() {
        val existing = listOf(
            // Simulate takeLast(14) starting with a DOCTOR message immediately after seedModelText
            ChatMessage(id = "1", role = MessageRole.DOCTOR.name, content = "Previous reply chunk."),
            ChatMessage(id = "2", role = MessageRole.USER.name, content = "Same retry question")
        )

        val (history, outgoing) = normalizeFollowUpChatTurns(
            seedUserText = "Seed user",
            seedModelText = "Seed model",
            existingMessages = existing,
            newUserMessage = "Same retry question"
        )

        assertEquals(2, history.size)
        assertEquals("user", history[0].first)
        assertEquals("model", history[1].first)
        assertTrue(history[1].second.contains("Seed model"))
        assertTrue(history[1].second.contains("Previous reply chunk."))
        // Identical retry should not duplicate the text
        assertEquals("Same retry question", outgoing)
    }

    @Test
    fun symptomCheckerIntakeDraft_buildsStructuredSummaryWithContext() {
        val emptyDraft = IntakeDraft()
        assertFalse(emptyDraft.hasDraftContent)

        val populated = IntakeDraft(
            mode = ConsultationType.SYMPTOM_CHECK,
            bodyArea = "Chest & Breathing",
            symptomDescription = "Dry cough since Tuesday evening",
            severity = 5,
            duration = "2–3 days",
            onsetPattern = "Gradual",
            progression = "Getting slightly better",
            triggersOrRelief = "Warm tea helps",
            associatedSymptoms = setOf("Fatigue", "Sore throat")
        )
        assertTrue(populated.hasDraftContent)
        val summary = populated.buildPreparedSymptomSummary()
        assertTrue(summary.contains("Dry cough since Tuesday evening"))
        assertTrue(summary.contains("Onset: Gradual"))
        assertTrue(summary.contains("Trend: Getting slightly better"))
        assertTrue(summary.contains("Triggers/Relief: Warm tea helps"))
    }

    @Test
    fun dailyWellnessCheckIn_noteCodecRoundTripAndActivityMapping() {
        val signals = setOf("Well Rested", "Hydrated", "Active Movement")
        val encoded = WellnessNoteCodec.encode(
            status = GeneralHealthStatus.FEELING_GREAT,
            signals = signals,
            reflection = "Morning jog felt energizing."
        )
        val decoded = WellnessNoteCodec.decode(encoded)
        assertEquals(GeneralHealthStatus.FEELING_GREAT, decoded.generalStatus)
        assertEquals(signals, decoded.signals)
        assertEquals("Morning jog felt energizing.", decoded.reflection)

        assertEquals(PhysicalActivityLevel.ACTIVE, PhysicalActivityLevel.fromRaw("ACTIVE"))
        assertEquals(PhysicalActivityLevel.MODERATE, PhysicalActivityLevel.fromRaw("UNKNOWN_VALUE"))
    }

    @Test
    fun mentalWellbeing_phq9AndGad7ScoringAndCrisisFlags() {
        // PHQ-9 minimal score (0)
        assertEquals(MentalSeverityBand.MINIMAL, MentalAssessmentType.PHQ9.evaluateBand(3))
        assertEquals(MentalSeverityBand.MILD, MentalAssessmentType.PHQ9.evaluateBand(7))
        assertEquals(MentalSeverityBand.MODERATE, MentalAssessmentType.PHQ9.evaluateBand(12))
        assertEquals(MentalSeverityBand.MODERATELY_SEVERE, MentalAssessmentType.PHQ9.evaluateBand(17))
        assertEquals(MentalSeverityBand.SEVERE, MentalAssessmentType.PHQ9.evaluateBand(21))

        // PHQ-9 item 9 positive triggers crisis even when total score is low
        val item9Positive = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1)
        assertTrue(MentalAssessmentType.PHQ9.isCrisisTriggered(item9Positive, totalScore = 1))

        // PHQ-9 severe total (>= 20) triggers crisis even if item 9 is 0
        val highScoreZeroItem9 = listOf(3, 3, 3, 3, 3, 3, 2, 0, 0)
        assertTrue(MentalAssessmentType.PHQ9.isCrisisTriggered(highScoreZeroItem9, totalScore = 20))

        // GAD-7 bands
        assertEquals(MentalSeverityBand.MINIMAL, MentalAssessmentType.GAD7.evaluateBand(4))
        assertEquals(MentalSeverityBand.MILD, MentalAssessmentType.GAD7.evaluateBand(8))
        assertEquals(MentalSeverityBand.MODERATE, MentalAssessmentType.GAD7.evaluateBand(12))
        assertEquals(MentalSeverityBand.SEVERE, MentalAssessmentType.GAD7.evaluateBand(16))
        assertFalse(MentalAssessmentType.GAD7.isCrisisTriggered(listOf(3, 3, 3, 3, 3, 3, 3), 21))
    }

    @Test
    fun clinicalSafetyEngine_detectsSymptomRedFlagsAndCriticalVitals() {
        val chestPainAlert = ClinicalSafetyEngine.evaluateSymptoms(
            symptomText = "Crushing chest pain radiating to left arm",
            associatedSymptoms = listOf("Shortness of breath"),
            severity = 9
        )
        assertNotNull(chestPainAlert)
        assertFalse(chestPainAlert!!.isMentalHealthCrisis)

        val mentalCrisisAlert = ClinicalSafetyEngine.evaluateSymptoms(
            symptomText = "I feel hopeless and want to end my life",
            associatedSymptoms = emptyList(),
            severity = 8
        )
        assertNotNull(mentalCrisisAlert)
        assertTrue(mentalCrisisAlert!!.isMentalHealthCrisis)

        val benignSymptoms = ClinicalSafetyEngine.evaluateSymptoms(
            symptomText = "Mild runny nose and seasonal sneezing",
            associatedSymptoms = listOf("Sneezing"),
            severity = 2
        )
        assertNull(benignSymptoms)

        val criticalBp = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_PRESSURE,
            primaryValue = 188.0,
            secondaryValue = 122.0,
            region = HealthRegionDirectory.US
        )
        assertNotNull(criticalBp)

        val normalBp = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_PRESSURE,
            primaryValue = 118.0,
            secondaryValue = 76.0,
            region = HealthRegionDirectory.US
        )
        assertNull(normalBp)
    }

    @Test
    fun bodyMeasurementsAndCycleAnalytics_computeBmiAndPredictions() {
        val bmi = BodyMeasurement.computeBmi(weightKg = 70.0, heightCm = 175.0)
        assertEquals(22.9, bmi, 0.1)
        assertEquals(BmiCategory.HEALTHY, BmiCategory.fromBmi(bmi))
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCategory.fromBmi(17.5))
        assertEquals(BmiCategory.OVERWEIGHT, BmiCategory.fromBmi(27.0))
        assertEquals(BmiCategory.OBESE, BmiCategory.fromBmi(32.5))

        val cycleLogs = listOf(
            CycleLog(id = "2026-10-01", dateKey = "2026-10-01", flowLevel = CycleFlowLevel.MEDIUM.name),
            CycleLog(id = "2026-10-02", dateKey = "2026-10-02", flowLevel = CycleFlowLevel.HEAVY.name),
            CycleLog(id = "2026-10-29", dateKey = "2026-10-29", flowLevel = CycleFlowLevel.MEDIUM.name),
            CycleLog(id = "2026-10-30", dateKey = "2026-10-30", flowLevel = CycleFlowLevel.LIGHT.name)
        )
        val prediction = CycleAnalytics.computePrediction(
            logs = cycleLogs,
            typicalCycleLengthDays = 28,
            todayDateKey = "2026-11-05"
        )
        assertEquals(28, prediction.estimatedCycleLength)
        assertEquals(8, prediction.currentCycleDay)
        assertNotNull(prediction.nextPeriodDateLabel)
        assertNotNull(prediction.estimatedFertileWindowLabel)
    }

    @Test
    fun healthHighlightsEngine_computesWeekOverWeekDeltasAcrossDomains() {
        val checkIns = listOf(
            // Current 7 days (2026-11-10..2026-11-16): avg sleep 8.0h, mood 4.5
            DailyCheckIn(dateKey = "2026-11-16", moodLevel = 5, energyLevel = 4, sleepHours = 8.0),
            DailyCheckIn(dateKey = "2026-11-15", moodLevel = 4, energyLevel = 4, sleepHours = 8.0),
            // Prior 7 days (2026-11-03..2026-11-09): avg sleep 6.5h, mood 3.0
            DailyCheckIn(dateKey = "2026-11-08", moodLevel = 3, energyLevel = 3, sleepHours = 6.5),
            DailyCheckIn(dateKey = "2026-11-07", moodLevel = 3, energyLevel = 3, sleepHours = 6.5)
        )
        val meds = listOf(
            MedicationItem(id = "m1", name = "Vitamin D", active = true, scheduleSlot = MedicationScheduleSlot.MORNING.name)
        )
        val medLogs = listOf(
            MedicationLog(medicationId = "m1", dateKey = "2026-11-16", status = MedicationLogStatus.TAKEN.name),
            MedicationLog(medicationId = "m1", dateKey = "2026-11-15", status = MedicationLogStatus.TAKEN.name),
            MedicationLog(medicationId = "m1", dateKey = "2026-11-08", status = MedicationLogStatus.SKIPPED.name),
            MedicationLog(medicationId = "m1", dateKey = "2026-11-07", status = MedicationLogStatus.TAKEN.name)
        )
        val mental = listOf(
            MentalAssessment(id = "a2", assessmentType = MentalAssessmentType.GAD7.name, totalScore = 4, maxScore = 21),
            MentalAssessment(id = "a1", assessmentType = MentalAssessmentType.GAD7.name, totalScore = 11, maxScore = 21)
        )

        val highlights = HealthHighlightsEngine.computeHighlights(
            todayDateKey = "2026-11-16",
            checkIns = checkIns,
            vitals = emptyList(),
            medications = meds,
            medicationLogs = medLogs,
            mentalAssessments = mental
        )

        val sleep = highlights.firstOrNull { it.category == HighlightCategory.SLEEP }
        assertNotNull(sleep)
        assertEquals(TrendDirection.UP, sleep!!.direction)
        assertEquals(TrendSentiment.POSITIVE, sleep.sentiment)

        val mood = highlights.firstOrNull { it.category == HighlightCategory.MOOD }
        assertNotNull(mood)
        assertEquals(TrendDirection.UP, mood!!.direction)

        val medAdherence = highlights.firstOrNull { it.category == HighlightCategory.MEDICATIONS }
        assertNotNull(medAdherence)
        assertEquals("100%", medAdherence!!.currentValueLabel)
        assertEquals("50%", medAdherence.previousValueLabel)
        assertEquals(TrendDirection.UP, medAdherence.direction)

        val mentalHighlight = highlights.firstOrNull { it.category == HighlightCategory.MENTAL_WELLBEING }
        assertNotNull(mentalHighlight)
        assertEquals(TrendDirection.DOWN, mentalHighlight!!.direction)
        assertEquals(TrendSentiment.POSITIVE, mentalHighlight.sentiment)
    }

    @Test
    fun reminderScheduler_deepLinkDestinationsAndNextSummary() {
        assertEquals("medications", ReminderScheduler.DEST_MEDICATIONS)
        assertEquals("check_in", ReminderScheduler.DEST_CHECK_IN)
        assertEquals("follow_up", ReminderScheduler.DEST_FOLLOW_UP)

        val asNeededOnly = listOf(
            MedicationItem(id = "prn", name = "Ibuprofen", active = true, scheduleSlot = MedicationScheduleSlot.AS_NEEDED.name)
        )
        assertNull(ReminderScheduler.nextScheduledMedicationSummary(asNeededOnly))

        val scheduled = listOf(
            MedicationItem(
                id = "m1",
                name = "Lisinopril",
                active = true,
                scheduleSlot = MedicationScheduleSlot.MORNING.name,
                reminderHour = 8,
                reminderMinute = 30
            )
        )
        val summary = ReminderScheduler.nextScheduledMedicationSummary(scheduled)
        assertNotNull(summary)
        assertTrue(summary!!.contains("Lisinopril"))
    }
}
