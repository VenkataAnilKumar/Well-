package com.example

import com.example.core.ClinicalSafetyEngine
import com.example.core.HealthRegionCode
import com.example.core.HealthRegionDirectory
import com.example.data.model.VitalRangeStatus
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthRegionAndSafetyTest {

    @Test
    fun unitConversions_roundTripAccurately() {
        assertEquals(37.0, HealthRegionDirectory.fahrenheitToCelsius(98.6), 0.05)
        assertEquals(98.6, HealthRegionDirectory.celsiusToFahrenheit(37.0), 0.05)

        assertEquals(5.55, HealthRegionDirectory.mgDlToMmolL(100.0), 0.05)
        assertEquals(100.0, HealthRegionDirectory.mmolLToMgDl(5.55), 0.5)

        assertEquals(70.0, HealthRegionDirectory.lbsToKg(154.32), 0.1)
        assertEquals(154.32, HealthRegionDirectory.kgToLbs(70.0), 0.2)
    }

    @Test
    fun canadaRegion_includes811AndMetricUnits() {
        val ca = HealthRegionDirectory.CA
        assertEquals(HealthRegionCode.CA, ca.code)
        assertTrue(ca.isCanada)
        assertTrue(ca.usesCelsiusTemp)
        assertTrue(ca.usesMmolGlucose)
        assertTrue(ca.usesMetricBody)
        assertEquals("811", ca.nurseAdviceNumber)
        assertNotNull(ca.nurseAdviceLabel)

        val us = HealthRegionDirectory.US
        assertEquals(HealthRegionCode.US, us.code)
        assertTrue(us.isUnitedStates)
        assertNull(us.nurseAdviceNumber)
    }

    @Test
    fun vitalReading_crossRegionFormattingAndRangeEvaluation() {
        // US-saved normal temperature (98.6 °F) displayed in Canada should read 37.0 °C and stay NORMAL
        val usTemp = VitalReading(
            vitalType = VitalType.TEMPERATURE.name,
            primaryValue = 98.6,
            unit = "°F"
        )
        assertEquals(VitalRangeStatus.NORMAL, usTemp.rangeStatus)
        assertEquals("37.0 °C", usTemp.formattedReadingForRegion(HealthRegionDirectory.CA))
        assertEquals("98.6 °F", usTemp.formattedReadingForRegion(HealthRegionDirectory.US))

        // Canada-saved fasting glucose (5.3 mmol/L) should evaluate NORMAL and convert to ~95 mg/dL in US
        val caGlucose = VitalReading(
            vitalType = VitalType.BLOOD_GLUCOSE.name,
            primaryValue = 5.3,
            unit = "mmol/L"
        )
        assertEquals(VitalRangeStatus.NORMAL, caGlucose.rangeStatus)
        assertEquals("5.3 mmol/L", caGlucose.formattedReadingForRegion(HealthRegionDirectory.CA))
        assertEquals("95 mg/dL", caGlucose.formattedReadingForRegion(HealthRegionDirectory.US))

        // Canada low glucose (2.7 mmol/L ~ 49 mg/dL) -> LOW
        val caLowGlucose = VitalReading(
            vitalType = VitalType.BLOOD_GLUCOSE.name,
            primaryValue = 2.7,
            unit = "mmol/L"
        )
        assertEquals(VitalRangeStatus.LOW, caLowGlucose.rangeStatus)

        // Canada high fever (38.5 °C ~ 101.3 °F) -> HIGH
        val caFever = VitalReading(
            vitalType = VitalType.TEMPERATURE.name,
            primaryValue = 38.5,
            unit = "°C"
        )
        assertEquals(VitalRangeStatus.HIGH, caFever.rangeStatus)
    }

    @Test
    fun clinicalSafetyEngine_triggersCriticalAlertsInBothUsAndCanadaUnits() {
        // US critical hypoglycemia & hyperglycemia
        val usLow = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_GLUCOSE,
            primaryValue = 50.0,
            unit = "mg/dL",
            region = HealthRegionDirectory.US
        )
        assertNotNull(usLow)
        assertTrue(usLow!!.formattedReading.contains("mg/dL"))

        val usHigh = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_GLUCOSE,
            primaryValue = 410.0,
            unit = "mg/dL",
            region = HealthRegionDirectory.US
        )
        assertNotNull(usHigh)

        // Canada critical hypoglycemia (2.7 mmol/L) & hyperglycemia (23.0 mmol/L)
        val caLow = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_GLUCOSE,
            primaryValue = 2.7,
            unit = "mmol/L",
            region = HealthRegionDirectory.CA
        )
        assertNotNull(caLow)
        assertTrue(caLow!!.formattedReading.contains("mmol/L"))

        val caHigh = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_GLUCOSE,
            primaryValue = 23.0,
            unit = "mmol/L",
            region = HealthRegionDirectory.CA
        )
        assertNotNull(caHigh)
        assertTrue(caHigh!!.formattedReading.contains("mmol/L"))

        // Canada normal glucose (5.4 mmol/L) must NOT trigger a critical alert
        val caNormal = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.BLOOD_GLUCOSE,
            primaryValue = 5.4,
            unit = "mmol/L",
            region = HealthRegionDirectory.CA
        )
        assertNull(caNormal)

        // Canada hyperpyrexia (40.2 °C) and hypothermia (34.8 °C)
        val caHighFever = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.TEMPERATURE,
            primaryValue = 40.2,
            unit = "°C",
            region = HealthRegionDirectory.CA
        )
        assertNotNull(caHighFever)
        assertTrue(caHighFever!!.formattedReading.contains("°C"))

        val caHypothermia = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.TEMPERATURE,
            primaryValue = 34.8,
            unit = "°C",
            region = HealthRegionDirectory.CA
        )
        assertNotNull(caHypothermia)

        // Normal Canada body temperature (36.8 °C) must NOT trigger a critical alert
        val caNormalTemp = ClinicalSafetyEngine.evaluateVital(
            vitalType = VitalType.TEMPERATURE,
            primaryValue = 36.8,
            unit = "°C",
            region = HealthRegionDirectory.CA
        )
        assertNull(caNormalTemp)
    }
}
