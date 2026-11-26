package com.example.data.model

import com.example.ui.viewmodel.IntakeDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabBiomarkerAndIntakeModelTest {

    @Test
    fun labBiomarker_formatsWholeAndDecimalValuesCleanly() {
        val whole = LabBiomarker(markerName = "Vitamin D", value = 42.0, unit = "ng/mL")
        val decimal = LabBiomarker(markerName = "HbA1c", value = 5.7, unit = "%")
        assertEquals("42", whole.formattedValue)
        assertEquals("5.7", decimal.formattedValue)
    }

    @Test
    fun labBiomarker_parsesEnumsWithSafeFallback() {
        assertEquals(LabBiomarkerCategory.LIPIDS, LabBiomarkerCategory.fromRaw("lipids"))
        assertEquals(LabBiomarkerCategory.OTHER, LabBiomarkerCategory.fromRaw("unknown_xyz"))
        assertEquals(LabBiomarkerStatus.HIGH, LabBiomarkerStatus.fromRaw("HIGH"))
        assertEquals(LabBiomarkerStatus.OPTIMAL, LabBiomarkerStatus.fromRaw(null))
    }

    @Test
    fun intakeDraft_serializesClarifyingAnswersIntoPreparedSummary() {
        val draft = IntakeDraft(
            symptomDescription = "Throbbing headache behind left eye",
            onsetPattern = "Gradual",
            progression = "Getting worse",
            triggersOrRelief = "Bright screen light",
            clarifyingAnswers = mapOf(
                "Any visual aura or flashing lights?" to "Yes",
                "Any neck stiffness or fever?" to "No"
            )
        )
        val summary = draft.buildPreparedSymptomSummary()
        assertTrue(summary.contains("Throbbing headache behind left eye"))
        assertTrue(summary.contains("Clarifying Q&A:"))
        assertTrue(summary.contains("Any visual aura or flashing lights?: Yes"))
        assertTrue(summary.contains("Any neck stiffness or fever?: No"))
    }

    @Test
    fun aiFeatureArea_includesNewP1ReportAreas() {
        val names = AiFeatureArea.entries.map { it.name }.toSet()
        assertTrue("LAB_BIOMARKER_SCAN" in names)
        assertTrue("PROACTIVE_REASSESSMENT" in names)
    }
}
