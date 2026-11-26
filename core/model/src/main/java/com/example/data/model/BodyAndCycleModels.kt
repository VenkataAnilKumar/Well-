package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class BmiCategory(val label: String, val rangeLabel: String) {
    UNDERWEIGHT("Underweight", "< 18.5"),
    HEALTHY("Healthy Range", "18.5 – 24.9"),
    OVERWEIGHT("Overweight", "25.0 – 29.9"),
    OBESE("Elevated BMI", "30.0+"),
    UNAVAILABLE("Add Height", "BMI pending");

    companion object {
        fun fromBmi(bmi: Double): BmiCategory = when {
            bmi <= 0.0 -> UNAVAILABLE
            bmi < 18.5 -> UNDERWEIGHT
            bmi < 25.0 -> HEALTHY
            bmi < 30.0 -> OVERWEIGHT
            else -> OBESE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class BodyMeasurement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val bmi: Double = 0.0,
    val waistCm: Double = 0.0,
    val note: String = "",
    @ServerTimestamp val recordedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val weightLbs: Double
        get() = weightKg * 2.20462262

    val waistInches: Double
        get() = waistCm / 2.54

    val bmiCategory: BmiCategory
        get() = BmiCategory.fromBmi(bmi)

    fun formattedWeight(useMetric: Boolean): String = if (useMetric) {
        String.format(Locale.US, "%.1f kg", weightKg)
    } else {
        String.format(Locale.US, "%.1f lbs", weightLbs)
    }

    fun formattedHeight(useMetric: Boolean): String {
        if (heightCm <= 0.0) return "—"
        return if (useMetric) {
            String.format(Locale.US, "%.0f cm", heightCm)
        } else {
            val totalInches = (heightCm / 2.54).toInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            "${feet}ft ${inches}in"
        }
    }

    fun formattedWaist(useMetric: Boolean): String {
        if (waistCm <= 0.0) return "—"
        return if (useMetric) {
            String.format(Locale.US, "%.1f cm", waistCm)
        } else {
            String.format(Locale.US, "%.1f in", waistInches)
        }
    }

    companion object {
        fun computeBmi(weightKg: Double, heightCm: Double): Double {
            if (weightKg <= 0.0 || heightCm < 50.0) return 0.0
            val heightM = heightCm / 100.0
            val raw = weightKg / (heightM * heightM)
            return kotlin.math.round(raw * 10.0) / 10.0
        }
    }
}

enum class CycleFlowLevel(val label: String) {
    NONE("No Flow"),
    SPOTTING("Spotting"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy");

    val countsAsPeriod: Boolean
        get() = this == LIGHT || this == MEDIUM || this == HEAVY

    companion object {
        fun fromRaw(raw: String?): CycleFlowLevel =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: NONE
    }
}

@Keep
@IgnoreExtraProperties
data class CycleLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val flowLevel: String = CycleFlowLevel.NONE.name,
    val symptoms: List<String> = emptyList(),
    val moodTag: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedFlow: CycleFlowLevel
        get() = CycleFlowLevel.fromRaw(flowLevel)
}

data class CyclePredictionSummary(
    val currentCycleDay: Int?,
    val estimatedCycleLength: Int,
    val currentPhaseLabel: String,
    val lastPeriodStartDateKey: String?,
    val nextPeriodDateLabel: String?,
    val daysUntilNextPeriod: Int?,
    val estimatedFertileWindowLabel: String?,
    val loggedPeriodStartsCount: Int
)

data class BodyAndCycleInsight(
    val headline: String,
    val bodyTrendNote: String,
    val cyclePatternNote: String,
    val gentleRecommendations: List<String>,
    val clinicianTopics: List<String>,
    val generatedAtMillis: Long = System.currentTimeMillis()
)
