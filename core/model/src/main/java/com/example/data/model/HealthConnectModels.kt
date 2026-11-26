package com.example.data.model

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    NOT_SUPPORTED
}

data class HealthConnectDailySteps(
    val dayLabel: String,
    val dateKey: String,
    val steps: Long
)

data class HealthConnectDailyHeartRate(
    val dayLabel: String,
    val dateKey: String,
    val avgBpm: Long?,
    val minBpm: Long?,
    val maxBpm: Long?,
    val sampleCount: Int
)

data class HealthConnectHeartRateSummary(
    val latestBpm: Long,
    val latestTimestampMillis: Long,
    val avgBpm: Long?,
    val minBpm: Long?,
    val maxBpm: Long?,
    val sampleCount: Int,
    val dailyTrend7Days: List<HealthConnectDailyHeartRate> = emptyList()
)

data class HealthConnectSleepSummary(
    val lastNightHours: Double,
    val average7DayHours: Double,
    val sessionCount: Int,
    val deepMinutes: Long,
    val remMinutes: Long,
    val lightMinutes: Long,
    val awakeMinutes: Long,
    val lastSessionEndMillis: Long
) {
    val hasStageBreakdown: Boolean
        get() = (deepMinutes + remMinutes + lightMinutes + awakeMinutes) > 0L
}

data class HealthConnectBloodPressureReading(
    val systolicMmHg: Double,
    val diastolicMmHg: Double,
    val timestampMillis: Long
) {
    val formattedValue: String
        get() = "${systolicMmHg.toInt()}/${diastolicMmHg.toInt()}"

    val clinicalRange: VitalRangeStatus
        get() = VitalType.BLOOD_PRESSURE.evaluate(systolicMmHg, diastolicMmHg)
}

data class HealthConnectSnapshot(
    val syncedAtMillis: Long = System.currentTimeMillis(),
    val grantedPermissions: Set<String> = emptySet(),
    val stepsToday: Long? = null,
    val dailySteps7Days: List<HealthConnectDailySteps> = emptyList(),
    val heartRate: HealthConnectHeartRateSummary? = null,
    val sleep: HealthConnectSleepSummary? = null,
    val latestBloodPressure: HealthConnectBloodPressureReading? = null,
    val recentBloodPressures: List<HealthConnectBloodPressureReading> = emptyList(),
    val partialWarnings: List<String> = emptyList()
) {
    val average7DaySteps: Long?
        get() {
            val activeDays = dailySteps7Days.filter { it.steps > 0L }
            return if (activeDays.isEmpty()) null else activeDays.sumOf { it.steps } / activeDays.size
        }

    val hasAnyMetricData: Boolean
        get() = (stepsToday != null && stepsToday > 0L) ||
            dailySteps7Days.any { it.steps > 0L } ||
            heartRate != null ||
            sleep != null ||
            latestBloodPressure != null
}

data class HealthConnectAiBrief(
    val headline: String,
    val activityAndRecoverySummary: String,
    val cardioAndSleepNote: String,
    val actionableTips: List<String>,
    val clinicianNote: String,
    val generatedAtMillis: Long = System.currentTimeMillis()
)
