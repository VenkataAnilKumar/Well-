package com.example.core

import com.example.data.model.VitalType
import com.example.testing.WellPlusTestFixtures
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalSafetyEngineModuleTest {

    @Test
    fun evaluateVital_flagsHypertensiveCrisisDeterministically() {
        val crisisVital = WellPlusTestFixtures.sampleVital(
            type = VitalType.BLOOD_PRESSURE,
            primary = 186.0,
            secondary = 122.0,
            unit = "mmHg"
        )
        val alert = ClinicalSafetyEngine.evaluateVital(
            vitalType = crisisVital.parsedType,
            primaryValue = crisisVital.primaryValue,
            secondaryValue = crisisVital.secondaryValue,
            unit = crisisVital.unit,
            region = HealthRegionDirectory.US
        )
        assertNotNull(alert)
        assertTrue(alert!!.immediateSteps.isNotEmpty())
    }

    @Test
    fun evaluateVital_doesNotTriggerCrisisForNormalHeartRate() {
        val alert = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.HEART_RATE,
            primaryValue = 72.0,
            secondaryValue = null,
            unit = "bpm",
            region = HealthRegionDirectory.CA
        )
        assertNull(alert)
    }

    @Test
    fun evaluateSymptoms_detectsEmergencyChestPainAndDyspnea() {
        val flag = ClinicalSafetyEngine.evaluateSymptoms(
            symptomText = "Sudden crushing chest pain radiating to left arm and shortness of breath",
            associatedSymptoms = listOf("Shortness of breath"),
            severity = 9
        )
        assertNotNull(flag)
    }
}
