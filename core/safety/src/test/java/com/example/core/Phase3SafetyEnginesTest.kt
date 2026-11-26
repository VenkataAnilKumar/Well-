package com.example.core

import com.example.data.model.CareCategory
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.ProposedChatAction
import com.example.data.model.ProposedChatActionType
import com.example.data.model.TrustedMedicalCitation
import com.example.data.model.VitalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3SafetyEnginesTest {

    @Test
    fun piiClassifier_detectsSsnSinMrnPhoneEmailAndDob() {
        assertTrue(PiiCategory.SSN in PiiPatternClassifier.detectCategories("SSN: 123-45-6789"))
        assertTrue(PiiCategory.SSN in PiiPatternClassifier.detectCategories("SIN 123-456-789"))
        assertTrue(PiiCategory.MRN in PiiPatternClassifier.detectCategories("MRN: PT-998812"))
        assertTrue(PiiCategory.MRN in PiiPatternClassifier.detectCategories("OHIP # 4412-901-882"))
        assertTrue(PiiCategory.PHONE in PiiPatternClassifier.detectCategories("Call (415) 555-0192"))
        assertTrue(PiiCategory.EMAIL in PiiPatternClassifier.detectCategories("patient.lin@example.org"))
        assertTrue(PiiCategory.DOB in PiiPatternClassifier.detectCategories("DOB: 04/19/1988"))

        // Clinical lab results must NOT be falsely flagged as PII
        assertTrue(PiiPatternClassifier.detectCategories("HbA1c 5.4 % Reference 4.0-5.6").isEmpty())
        assertTrue(PiiPatternClassifier.detectCategories("LDL Cholesterol 112 mg/dL").isEmpty())
    }

    @Test
    fun citationValidator_allowsOfficialHttpsAndRejectsSpoofedOrHttpUrls() {
        assertTrue(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://medlineplus.gov/headache.html"))
        assertTrue(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://www.cdc.gov/respiratory-viruses/index.html"))
        assertTrue(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://www.canada.ca/en/health-canada.html"))
        assertTrue(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://www.mayoclinic.org/diseases-conditions"))

        // Reject non-HTTPS, spoofed subdomains, userinfo phishing, and unapproved domains
        assertFalse(TrustedMedicalCitationValidator.isAllowedHttpsUrl("http://medlineplus.gov/headache.html"))
        assertFalse(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://medlineplus.gov.evil.com/phish"))
        assertFalse(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://cdc.gov@evil.example/test"))
        assertFalse(TrustedMedicalCitationValidator.isAllowedHttpsUrl("https://random-health-blog.biz/cure"))

        val filtered = TrustedMedicalCitationValidator.filterTrusted(
            listOf(
                TrustedMedicalCitation("Headache Guide", "NIH MedlinePlus", "https://medlineplus.gov/headache.html"),
                TrustedMedicalCitation("Duplicate", "NIH MedlinePlus", "https://medlineplus.gov/headache.html"),
                TrustedMedicalCitation("Untrusted", "Blog", "https://not-trusted.example/article")
            )
        )
        assertEquals(1, filtered.size)
        assertEquals("https://medlineplus.gov/headache.html", filtered.first().url)

        val canadaFallback = TrustedMedicalCitationValidator.fallbackReferencesForTopic("Chest", isCanada = true)
        assertTrue(canadaFallback.any { it.url.contains("canada.ca") })
        assertTrue(canadaFallback.all { TrustedMedicalCitationValidator.isAllowedHttpsUrl(it.url) })
    }

    @Test
    fun proposedChatActionValidator_enforcesPhysiologicalAndFirestoreBounds() {
        val validBp = ProposedChatActionValidator.sanitize(
            ProposedChatAction(
                id = "a1",
                actionType = ProposedChatActionType.LOG_VITAL,
                title = "Log blood pressure 122/78",
                subtitle = "Resting reading",
                vitalType = VitalType.BLOOD_PRESSURE,
                vitalPrimaryValue = 122.0,
                vitalSecondaryValue = 78.0
            )
        )
        assertNotNull(validBp)
        assertEquals(122.0, validBp?.vitalPrimaryValue)
        assertEquals(78.0, validBp?.vitalSecondaryValue)

        // Diastolic higher than systolic must be rejected
        val invertedBp = ProposedChatActionValidator.sanitize(
            ProposedChatAction(
                id = "a2",
                actionType = ProposedChatActionType.LOG_VITAL,
                title = "Bad BP",
                subtitle = "",
                vitalType = VitalType.BLOOD_PRESSURE,
                vitalPrimaryValue = 70.0,
                vitalSecondaryValue = 120.0
            )
        )
        assertNull(invertedBp)

        // SpO2 > 100% must be rejected
        val impossibleSpo2 = ProposedChatActionValidator.sanitize(
            ProposedChatAction(
                id = "a3",
                actionType = ProposedChatActionType.LOG_VITAL,
                title = "Bad SpO2",
                subtitle = "",
                vitalType = VitalType.SPO2,
                vitalPrimaryValue = 145.0
            )
        )
        assertNull(impossibleSpo2)

        // Medication fields clamp cleanly to Firestore string bounds
        val med = ProposedChatActionValidator.sanitize(
            ProposedChatAction(
                id = "a4",
                actionType = ProposedChatActionType.ADD_MEDICATION,
                title = "Add Ibuprofen",
                subtitle = "200 mg with food",
                medicationName = "I".repeat(140),
                medicationDosage = "200 mg",
                medicationSlot = MedicationScheduleSlot.MIDDAY,
                careCategory = CareCategory.MEDICATION
            )
        )
        assertNotNull(med)
        assertEquals(100, med?.medicationName?.length)
    }
}
