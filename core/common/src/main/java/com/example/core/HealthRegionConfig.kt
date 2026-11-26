package com.example.core

import android.content.Context
import java.util.Locale

enum class HealthRegionCode(
    val code: String,
    val displayName: String,
    val shortCode: String
) {
    US("US", "United States", "US"),
    CA("CA", "Canada", "CA");

    companion object {
        fun fromRaw(raw: String?): HealthRegionCode? {
            val normalized = raw?.trim()?.uppercase(Locale.US).orEmpty()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}

data class HealthRegionProfile(
    val code: HealthRegionCode,
    val countryName: String,
    val shortBadge: String,
    val unitSummaryLine: String,
    val usesMetricBody: Boolean,
    val usesCelsiusTemp: Boolean,
    val usesMmolGlucose: Boolean,
    val emergencyNumber: String,
    val emergencyLabel: String,
    val crisisNumber: String,
    val crisisLabel: String,
    val nurseAdviceNumber: String?,
    val nurseAdviceLabel: String?,
    val coverageSectionTitle: String,
    val coveragePrimaryLabel: String,
    val coveragePrimaryPlaceholder: String,
    val coverageIdLabel: String,
    val coverageIdPlaceholder: String,
    val primaryClinicianLabel: String,
    val primaryClinicianPlaceholder: String,
    val pharmacyLabel: String,
    val pharmacyPlaceholder: String,
    val aiPromptContext: String
) {
    val isCanada: Boolean
        get() = code == HealthRegionCode.CA

    val isUnitedStates: Boolean
        get() = code == HealthRegionCode.US
}

/**
 * Centralized US / Canada health region configuration engine.
 * Drives clinical measurement units, Canada 811 nurse advice line availability,
 * Medical ID & Doctor PDF Care Team labels, and Gemini AI localization context.
 */
object HealthRegionDirectory {

    private const val GLUCOSE_CONVERSION_FACTOR = 18.0182
    private const val WEIGHT_LBS_PER_KG = 2.20462262

    val US = HealthRegionProfile(
        code = HealthRegionCode.US,
        countryName = "United States",
        shortBadge = "United States",
        unitSummaryLine = "°F • mg/dL • lbs • 911",
        usesMetricBody = false,
        usesCelsiusTemp = false,
        usesMmolGlucose = false,
        emergencyNumber = "911",
        emergencyLabel = "Emergency Services (911)",
        crisisNumber = "988",
        crisisLabel = "988 Suicide & Crisis Lifeline",
        nurseAdviceNumber = null,
        nurseAdviceLabel = null,
        coverageSectionTitle = "Health Insurance & Care Team",
        coveragePrimaryLabel = "Health Insurance Plan",
        coveragePrimaryPlaceholder = "e.g., BlueCross PPO, Aetna, UnitedHealthcare",
        coverageIdLabel = "Member / Policy ID (Optional)",
        coverageIdPlaceholder = "Optional — kept private in your vault",
        primaryClinicianLabel = "Primary Care Doctor",
        primaryClinicianPlaceholder = "e.g., Dr. Sarah Chen, MD",
        pharmacyLabel = "Preferred Pharmacy",
        pharmacyPlaceholder = "e.g., CVS Pharmacy #1420",
        aiPromptContext = "User Region: United States. Use US clinical units (°F for body temperature, mg/dL for blood glucose, lbs/ft-in for body measurements). Reference US care pathways where helpful (Primary Care Physician / PCP, Urgent Care, Emergency Room / 911, and 988 Suicide & Crisis Lifeline)."
    )

    val CA = HealthRegionProfile(
        code = HealthRegionCode.CA,
        countryName = "Canada",
        shortBadge = "Canada",
        unitSummaryLine = "°C • mmol/L • kg • 811 • 911",
        usesMetricBody = true,
        usesCelsiusTemp = true,
        usesMmolGlucose = true,
        emergencyNumber = "911",
        emergencyLabel = "Emergency Services (911)",
        crisisNumber = "988",
        crisisLabel = "988 Suicide Crisis Helpline",
        nurseAdviceNumber = "811",
        nurseAdviceLabel = "811 Free 24/7 Nurse Advice Line",
        coverageSectionTitle = "Provincial Health Card & Care Team",
        coveragePrimaryLabel = "Provincial Health Card (e.g., OHIP, MSP, AHS)",
        coveragePrimaryPlaceholder = "e.g., OHIP (Ontario), MSP (BC), AHS (Alberta)",
        coverageIdLabel = "Health Card # (Optional)",
        coverageIdPlaceholder = "Optional — kept private in your vault",
        primaryClinicianLabel = "Family Doctor / Clinic",
        primaryClinicianPlaceholder = "e.g., Dr. Marc Tremblay or Local Walk-In Clinic",
        pharmacyLabel = "Preferred Pharmacy",
        pharmacyPlaceholder = "e.g., Shoppers Drug Mart #0824",
        aiPromptContext = "User Region: Canada. Use Canadian metric clinical units (°C for body temperature, mmol/L for blood glucose, kg/cm for body measurements). Reference Canadian care pathways where helpful (Family Doctor, Walk-in Clinic, Pharmacist prescribing for minor ailments, free 24/7 provincial Nurse Advice Line 811, Emergency Department / 911, and 988 Suicide Crisis Helpline)."
    )

    val supportedRegions: List<HealthRegionProfile> = listOf(US, CA)

    fun fromCode(raw: String?): HealthRegionProfile? = when (HealthRegionCode.fromRaw(raw)) {
        HealthRegionCode.US -> US
        HealthRegionCode.CA -> CA
        null -> null
    }

    fun detectSuggestedRegion(context: Context): HealthRegionProfile {
        val detectedCountry = RegionalEmergencyDirectory.detectCountryCode(context)
        return if (detectedCountry.equals("CA", ignoreCase = true)) CA else US
    }

    fun resolve(context: Context, overrideCode: String? = null): HealthRegionProfile {
        return fromCode(overrideCode) ?: detectSuggestedRegion(context)
    }

    // ==================== PURE UNIT CONVERSION HELPERS ====================

    fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32.0) * 5.0 / 9.0

    fun celsiusToFahrenheit(celsius: Double): Double = (celsius * 9.0 / 5.0) + 32.0

    fun mgDlToMmolL(mgDl: Double): Double = mgDl / GLUCOSE_CONVERSION_FACTOR

    fun mmolLToMgDl(mmolL: Double): Double = mmolL * GLUCOSE_CONVERSION_FACTOR

    fun lbsToKg(lbs: Double): Double = lbs / WEIGHT_LBS_PER_KG

    fun kgToLbs(kg: Double): Double = kg * WEIGHT_LBS_PER_KG

    fun isCelsiusUnit(unit: String, value: Double): Boolean {
        val normalized = unit.trim().lowercase(Locale.US)
        return when {
            normalized.contains("c") -> true
            normalized.contains("f") -> false
            else -> value < 45.0
        }
    }

    fun isMmolGlucoseUnit(unit: String, value: Double): Boolean {
        val normalized = unit.trim().lowercase(Locale.US)
        return when {
            normalized.contains("mmol") -> true
            normalized.contains("mg") -> false
            else -> value < 35.0
        }
    }

    fun isKgWeightUnit(unit: String): Boolean {
        val normalized = unit.trim().lowercase(Locale.US)
        return normalized == "kg" || normalized.contains("kilo")
    }
}
