package com.example.data.model

import androidx.annotation.Keep
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class VitalRangeStatus(val label: String) {
    NORMAL("In Range"),
    ELEVATED("Elevated"),
    HIGH("High"),
    LOW("Low")
}

enum class VitalType(
    val label: String,
    val shortLabel: String,
    val defaultUnit: String,
    val normalRangeLabel: String,
    val minAllowed: Double,
    val maxAllowed: Double,
    val normalLow: Double,
    val normalHigh: Double,
    val hasSecondaryValue: Boolean = false
) {
    BLOOD_PRESSURE(
        label = "Blood Pressure",
        shortLabel = "BP",
        defaultUnit = "mmHg",
        normalRangeLabel = "90–120 / 60–80 mmHg",
        minAllowed = 50.0,
        maxAllowed = 260.0,
        normalLow = 90.0,
        normalHigh = 120.0,
        hasSecondaryValue = true
    ),
    HEART_RATE(
        label = "Heart Rate",
        shortLabel = "HR",
        defaultUnit = "bpm",
        normalRangeLabel = "60–100 bpm",
        minAllowed = 30.0,
        maxAllowed = 230.0,
        normalLow = 60.0,
        normalHigh = 100.0
    ),
    TEMPERATURE(
        label = "Temperature",
        shortLabel = "Temp",
        defaultUnit = "°F",
        normalRangeLabel = "97.0–99.5 °F",
        minAllowed = 90.0,
        maxAllowed = 109.0,
        normalLow = 97.0,
        normalHigh = 99.5
    ),
    SPO2(
        label = "Blood Oxygen",
        shortLabel = "SpO₂",
        defaultUnit = "%",
        normalRangeLabel = "95–100%",
        minAllowed = 60.0,
        maxAllowed = 100.0,
        normalLow = 95.0,
        normalHigh = 100.0
    ),
    WEIGHT(
        label = "Body Weight",
        shortLabel = "Weight",
        defaultUnit = "lbs",
        normalRangeLabel = "Track trend over time",
        minAllowed = 20.0,
        maxAllowed = 800.0,
        normalLow = 80.0,
        normalHigh = 300.0
    ),
    BLOOD_GLUCOSE(
        label = "Blood Glucose",
        shortLabel = "Glucose",
        defaultUnit = "mg/dL",
        normalRangeLabel = "70–140 mg/dL",
        minAllowed = 20.0,
        maxAllowed = 600.0,
        normalLow = 70.0,
        normalHigh = 140.0
    );

    fun unitFor(region: HealthRegionProfile): String = when (this) {
        TEMPERATURE -> if (region.usesCelsiusTemp) "°C" else "°F"
        BLOOD_GLUCOSE -> if (region.usesMmolGlucose) "mmol/L" else "mg/dL"
        WEIGHT -> if (region.usesMetricBody) "kg" else "lbs"
        else -> defaultUnit
    }

    fun normalRangeLabelFor(region: HealthRegionProfile): String = when (this) {
        TEMPERATURE -> if (region.usesCelsiusTemp) "36.1–37.5 °C" else "97.0–99.5 °F"
        BLOOD_GLUCOSE -> if (region.usesMmolGlucose) "3.9–7.8 mmol/L" else "70–140 mg/dL"
        else -> normalRangeLabel
    }

    fun minAllowedFor(region: HealthRegionProfile): Double = when (this) {
        TEMPERATURE -> if (region.usesCelsiusTemp) 32.0 else 90.0
        BLOOD_GLUCOSE -> if (region.usesMmolGlucose) 1.0 else 20.0
        WEIGHT -> if (region.usesMetricBody) 10.0 else 20.0
        else -> minAllowed
    }

    fun maxAllowedFor(region: HealthRegionProfile): Double = when (this) {
        TEMPERATURE -> if (region.usesCelsiusTemp) 43.0 else 109.0
        BLOOD_GLUCOSE -> if (region.usesMmolGlucose) 35.0 else 600.0
        WEIGHT -> if (region.usesMetricBody) 365.0 else 800.0
        else -> maxAllowed
    }

    fun evaluate(primary: Double, secondary: Double? = null): VitalRangeStatus =
        evaluateWithUnit(primary, secondary, defaultUnit)

    fun evaluateWithUnit(
        primary: Double,
        secondary: Double? = null,
        unit: String = defaultUnit
    ): VitalRangeStatus = when (this) {
        BLOOD_PRESSURE -> {
            val dia = secondary ?: 75.0
            when {
                primary >= 140.0 || dia >= 90.0 -> VitalRangeStatus.HIGH
                primary >= 121.0 || dia >= 81.0 -> VitalRangeStatus.ELEVATED
                primary < 90.0 || dia < 60.0 -> VitalRangeStatus.LOW
                else -> VitalRangeStatus.NORMAL
            }
        }
        HEART_RATE -> when {
            primary < 55.0 -> VitalRangeStatus.LOW
            primary > 115.0 -> VitalRangeStatus.HIGH
            primary > 100.0 -> VitalRangeStatus.ELEVATED
            else -> VitalRangeStatus.NORMAL
        }
        TEMPERATURE -> {
            val tempF = if (HealthRegionDirectory.isCelsiusUnit(unit, primary)) {
                HealthRegionDirectory.celsiusToFahrenheit(primary)
            } else {
                primary
            }
            when {
                tempF < 96.5 -> VitalRangeStatus.LOW
                tempF >= 100.4 -> VitalRangeStatus.HIGH
                tempF > 99.5 -> VitalRangeStatus.ELEVATED
                else -> VitalRangeStatus.NORMAL
            }
        }
        SPO2 -> when {
            primary < 92.0 -> VitalRangeStatus.HIGH
            primary < 95.0 -> VitalRangeStatus.LOW
            else -> VitalRangeStatus.NORMAL
        }
        BLOOD_GLUCOSE -> {
            val mgDl = if (HealthRegionDirectory.isMmolGlucoseUnit(unit, primary)) {
                HealthRegionDirectory.mmolLToMgDl(primary)
            } else {
                primary
            }
            when {
                mgDl < 70.0 -> VitalRangeStatus.LOW
                mgDl >= 200.0 -> VitalRangeStatus.HIGH
                mgDl > 140.0 -> VitalRangeStatus.ELEVATED
                else -> VitalRangeStatus.NORMAL
            }
        }
        WEIGHT -> VitalRangeStatus.NORMAL
    }

    fun formatValue(primary: Double, secondary: Double? = null): String =
        formatValueForUnit(primary, secondary, defaultUnit)

    fun formatValueForUnit(
        primary: Double,
        secondary: Double? = null,
        unit: String = defaultUnit
    ): String = when (this) {
        BLOOD_PRESSURE -> {
            val dia = secondary?.toInt()
            if (dia != null) "${primary.toInt()}/$dia" else "${primary.toInt()}"
        }
        HEART_RATE, SPO2 -> "${primary.toInt()}"
        BLOOD_GLUCOSE -> {
            if (HealthRegionDirectory.isMmolGlucoseUnit(unit, primary)) {
                String.format(Locale.US, "%.1f", primary)
            } else {
                "${primary.toInt()}"
            }
        }
        TEMPERATURE, WEIGHT -> String.format(Locale.US, "%.1f", primary)
    }

    companion object {
        fun fromRaw(raw: String?): VitalType =
            entries.firstOrNull { it.name == raw } ?: BLOOD_PRESSURE
    }
}

@Keep
@IgnoreExtraProperties
data class VitalReading(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vitalType: String = VitalType.BLOOD_PRESSURE.name,
    val primaryValue: Double = 0.0,
    val secondaryValue: Double? = null,
    val unit: String = "mmHg",
    val note: String = "",
    @ServerTimestamp val recordedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: VitalType
        get() = VitalType.fromRaw(vitalType)

    val rangeStatus: VitalRangeStatus
        get() = parsedType.evaluateWithUnit(primaryValue, secondaryValue, unit)

    fun normalizedValueForRegion(region: HealthRegionProfile): Double = when (parsedType) {
        VitalType.TEMPERATURE -> {
            val storedCelsius = HealthRegionDirectory.isCelsiusUnit(unit, primaryValue)
            when {
                region.usesCelsiusTemp && !storedCelsius -> HealthRegionDirectory.fahrenheitToCelsius(primaryValue)
                !region.usesCelsiusTemp && storedCelsius -> HealthRegionDirectory.celsiusToFahrenheit(primaryValue)
                else -> primaryValue
            }
        }
        VitalType.BLOOD_GLUCOSE -> {
            val storedMmol = HealthRegionDirectory.isMmolGlucoseUnit(unit, primaryValue)
            when {
                region.usesMmolGlucose && !storedMmol -> HealthRegionDirectory.mgDlToMmolL(primaryValue)
                !region.usesMmolGlucose && storedMmol -> HealthRegionDirectory.mmolLToMgDl(primaryValue)
                else -> primaryValue
            }
        }
        VitalType.WEIGHT -> {
            val storedKg = HealthRegionDirectory.isKgWeightUnit(unit)
            when {
                region.usesMetricBody && !storedKg -> HealthRegionDirectory.lbsToKg(primaryValue)
                !region.usesMetricBody && storedKg -> HealthRegionDirectory.kgToLbs(primaryValue)
                else -> primaryValue
            }
        }
        else -> primaryValue
    }

    fun formattedReading(): String {
        val resolvedUnit = unit.ifBlank { parsedType.defaultUnit }
        return "${parsedType.formatValueForUnit(primaryValue, secondaryValue, resolvedUnit)} $resolvedUnit"
    }

    fun formattedReadingForRegion(region: HealthRegionProfile): String {
        val targetUnit = parsedType.unitFor(region)
        val converted = normalizedValueForRegion(region)
        return "${parsedType.formatValueForUnit(converted, secondaryValue, targetUnit)} $targetUnit"
    }
}
