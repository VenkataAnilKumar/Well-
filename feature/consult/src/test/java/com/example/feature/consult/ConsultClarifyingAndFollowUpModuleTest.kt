package com.example.feature.consult

import com.example.data.model.FollowUpStatus
import com.example.testing.WellPlusTestFixtures
import com.example.ui.viewmodel.IntakeDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsultClarifyingAndFollowUpModuleTest {

    @Test
    fun consultation_parsesWorseningFollowUpForProactiveTrigger() {
        val consult = WellPlusTestFixtures.sampleConsultation(followUp = FollowUpStatus.WORSENING)
        assertEquals(FollowUpStatus.WORSENING, consult.parsedFollowUpStatus)
    }

    @Test
    fun intakeDraft_retainsMultipleClarifyingAnswersInPreparedBrief() {
        val draft = IntakeDraft(
            bodyArea = "Chest & Breathing",
            symptomDescription = "Dry cough for 4 days",
            severity = 5,
            duration = "2–3 days",
            clarifyingAnswers = mapOf(
                "Do you have fever above 100.4°F?" to "No",
                "Does coughing worsen when lying flat?" to "Yes"
            )
        )
        val summary = draft.buildPreparedSymptomSummary()
        assertTrue(summary.contains("Dry cough for 4 days"))
        assertTrue(summary.contains("Does coughing worsen when lying flat?: Yes"))
    }
}
