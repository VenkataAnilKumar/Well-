package com.example.core

import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthHighlightItem
import com.example.data.model.HighlightCategory
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.TrendDirection
import com.example.data.model.TrendSentiment
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deterministic 7-day vs prior 7-day Apple Health-style Highlights & Trends engine.
 * Computes honest, data-grounded deltas across Sleep, Mood & Energy, Medication Adherence,
 * Vitals (Blood Pressure, Heart Rate, SpO2), and Mental Wellbeing screenings.
 */
object HealthHighlightsEngine {

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun computeHighlights(
        todayDateKey: String,
        checkIns: List<DailyCheckIn>,
        vitals: List<VitalReading>,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        mentalAssessments: List<MentalAssessment>
    ): List<HealthHighlightItem> {
        val anchorDate = parseDateKey(todayDateKey) ?: Date()
        val currentWindowKeys = buildDateKeySet(anchorDate, startOffsetDays = 0, count = 7)
        val priorWindowKeys = buildDateKeySet(anchorDate, startOffsetDays = 7, count = 7)

        val endOfTodayMillis = endOfDayMillis(anchorDate)
        val currentStartMillis = endOfTodayMillis - (7L * MILLIS_PER_DAY)
        val priorStartMillis = endOfTodayMillis - (14L * MILLIS_PER_DAY)

        val results = mutableListOf<HealthHighlightItem>()

        computeSleepHighlight(checkIns, currentWindowKeys, priorWindowKeys)?.let(results::add)
        computeMoodHighlight(checkIns, currentWindowKeys, priorWindowKeys)?.let(results::add)
        computeMedicationHighlight(
            medications = medications,
            logs = medicationLogs,
            currentKeys = currentWindowKeys,
            priorKeys = priorWindowKeys
        )?.let(results::add)

        // Up to 2 vital highlights (prioritizing Blood Pressure, Heart Rate, then Blood Oxygen)
        val vitalCandidates = listOf(VitalType.BLOOD_PRESSURE, VitalType.HEART_RATE, VitalType.SPO2)
            .mapNotNull { type ->
                computeVitalHighlight(
                    type = type,
                    vitals = vitals,
                    currentStartMillis = currentStartMillis,
                    priorStartMillis = priorStartMillis,
                    endOfTodayMillis = endOfTodayMillis
                )
            }
        results.addAll(vitalCandidates.take(2))

        computeMentalWellbeingHighlight(mentalAssessments)?.let(results::add)

        // Prioritize real week-over-week shifts and attention callouts first
        return results.sortedBy { item ->
            val hasComparison = item.previousValueLabel != null
            when {
                hasComparison && item.sentiment == TrendSentiment.ATTENTION -> 0
                hasComparison && item.sentiment == TrendSentiment.POSITIVE -> 1
                hasComparison -> 2
                item.sentiment == TrendSentiment.ATTENTION -> 3
                else -> 4
            }
        }
    }

    private fun computeSleepHighlight(
        checkIns: List<DailyCheckIn>,
        currentKeys: Set<String>,
        priorKeys: Set<String>
    ): HealthHighlightItem? {
        val current = checkIns.filter { it.dateKey in currentKeys && it.sleepHours > 0.0 }
        val prior = checkIns.filter { it.dateKey in priorKeys && it.sleepHours > 0.0 }
        if (current.isEmpty()) return null

        val currAvg = current.map { it.sleepHours }.average()
        val currFormatted = String.format(Locale.US, "%.1f hr", currAvg)

        if (prior.isEmpty()) {
            val sentiment = when {
                currAvg in 7.0..9.0 -> TrendSentiment.POSITIVE
                currAvg < 6.0 -> TrendSentiment.ATTENTION
                else -> TrendSentiment.NEUTRAL
            }
            val headline = when (sentiment) {
                TrendSentiment.POSITIVE -> "Resting $currFormatted/night this week"
                TrendSentiment.ATTENTION -> "Sleep averaging $currFormatted — below 7 hr goal"
                TrendSentiment.NEUTRAL -> "Averaging $currFormatted of sleep"
            }
            return HealthHighlightItem(
                id = "highlight_sleep",
                category = HighlightCategory.SLEEP,
                title = "Sleep Duration",
                headline = headline,
                narrative = "Based on ${current.size} check-in(s) over the past 7 days. Keep logging next week to unlock week-over-week trends.",
                direction = TrendDirection.BASELINE,
                sentiment = sentiment,
                currentValueLabel = currFormatted,
                currentPeriodLabel = "Last 7 Days",
                currentBarFraction = (currAvg / 10.0).toFloat().coerceIn(0.20f, 1.0f),
                deltaBadgeText = "7d Avg",
                sampleCountText = "${current.size} day(s) logged"
            )
        }

        val prevAvg = prior.map { it.sleepHours }.average()
        val prevFormatted = String.format(Locale.US, "%.1f hr", prevAvg)
        val delta = currAvg - prevAvg
        val deltaMinutes = (abs(delta) * 60.0).roundToInt()

        val direction = when {
            delta >= 0.35 -> TrendDirection.UP
            delta <= -0.35 -> TrendDirection.DOWN
            else -> TrendDirection.STEADY
        }
        val sentiment = when {
            direction == TrendDirection.UP && currAvg <= 9.5 -> TrendSentiment.POSITIVE
            direction == TrendDirection.DOWN && currAvg < 6.8 -> TrendSentiment.ATTENTION
            direction == TrendDirection.STEADY && currAvg in 7.0..9.0 -> TrendSentiment.POSITIVE
            currAvg < 6.0 -> TrendSentiment.ATTENTION
            else -> TrendSentiment.NEUTRAL
        }

        val deltaBadge = when (direction) {
            TrendDirection.UP -> String.format(Locale.US, "+%.1f hr", delta)
            TrendDirection.DOWN -> String.format(Locale.US, "%.1f hr", delta)
            else -> "Steady"
        }
        val headline = when (direction) {
            TrendDirection.UP -> "Sleeping ~$deltaMinutes min more per night"
            TrendDirection.DOWN -> "Sleeping ~$deltaMinutes min less per night"
            else -> "Sleep holding steady at $currFormatted"
        }
        val narrative = when (direction) {
            TrendDirection.UP -> "Your 7-day average rose to $currFormatted compared to $prevFormatted the prior week."
            TrendDirection.DOWN -> "Your 7-day average dipped to $currFormatted compared to $prevFormatted the prior week."
            else -> "Consistent rest around $currFormatted across both the current and prior 7-day periods."
        }

        val maxBar = maxOf(currAvg, prevAvg, 8.5)
        return HealthHighlightItem(
            id = "highlight_sleep",
            category = HighlightCategory.SLEEP,
            title = "Sleep Duration",
            headline = headline,
            narrative = narrative,
            direction = direction,
            sentiment = sentiment,
            currentValueLabel = currFormatted,
            currentPeriodLabel = "Last 7 Days",
            previousValueLabel = prevFormatted,
            previousPeriodLabel = "Prior 7 Days",
            currentBarFraction = (currAvg / maxBar).toFloat().coerceIn(0.18f, 1.0f),
            previousBarFraction = (prevAvg / maxBar).toFloat().coerceIn(0.18f, 1.0f),
            deltaBadgeText = deltaBadge,
            sampleCountText = "${current.size} vs ${prior.size} days"
        )
    }

    private fun computeMoodHighlight(
        checkIns: List<DailyCheckIn>,
        currentKeys: Set<String>,
        priorKeys: Set<String>
    ): HealthHighlightItem? {
        val current = checkIns.filter { it.dateKey in currentKeys }
        val prior = checkIns.filter { it.dateKey in priorKeys }
        if (current.isEmpty()) return null

        val currMood = current.map { it.moodLevel.toDouble() }.average()
        val currEnergy = current.map { it.energyLevel.toDouble() }.average()
        val currFormatted = String.format(Locale.US, "%.1f / 5", currMood)
        val energyNote = String.format(Locale.US, "Energy %.1f/5", currEnergy)

        if (prior.isEmpty()) {
            val sentiment = when {
                currMood >= 3.8 -> TrendSentiment.POSITIVE
                currMood < 2.6 -> TrendSentiment.ATTENTION
                else -> TrendSentiment.NEUTRAL
            }
            val headline = when {
                currMood >= 4.0 -> "Strong, upbeat mood this week"
                currMood >= 3.0 -> "Balanced mood at $currFormatted"
                else -> "Lower mood noted ($currFormatted)"
            }
            return HealthHighlightItem(
                id = "highlight_mood",
                category = HighlightCategory.MOOD,
                title = "Mood & Energy",
                headline = headline,
                narrative = "$energyNote across ${current.size} check-in(s) over the past 7 days.",
                direction = TrendDirection.BASELINE,
                sentiment = sentiment,
                currentValueLabel = currFormatted,
                currentPeriodLabel = "Last 7 Days",
                currentBarFraction = (currMood / 5.0).toFloat().coerceIn(0.20f, 1.0f),
                deltaBadgeText = "7d Avg",
                sampleCountText = "${current.size} check-in(s)"
            )
        }

        val prevMood = prior.map { it.moodLevel.toDouble() }.average()
        val prevFormatted = String.format(Locale.US, "%.1f / 5", prevMood)
        val delta = currMood - prevMood

        val direction = when {
            delta >= 0.35 -> TrendDirection.UP
            delta <= -0.35 -> TrendDirection.DOWN
            else -> TrendDirection.STEADY
        }
        val sentiment = when {
            direction == TrendDirection.UP -> TrendSentiment.POSITIVE
            direction == TrendDirection.DOWN -> TrendSentiment.ATTENTION
            currMood >= 3.8 -> TrendSentiment.POSITIVE
            currMood < 2.6 -> TrendSentiment.ATTENTION
            else -> TrendSentiment.NEUTRAL
        }

        val deltaBadge = when (direction) {
            TrendDirection.UP -> String.format(Locale.US, "+%.1f pts", delta)
            TrendDirection.DOWN -> String.format(Locale.US, "%.1f pts", delta)
            else -> "Steady"
        }
        val headline = when (direction) {
            TrendDirection.UP -> "Mood lifted compared to last week"
            TrendDirection.DOWN -> "Mood dipped over the last 7 days"
            else -> "Mood staying steady at $currFormatted"
        }
        val narrative = when (direction) {
            TrendDirection.UP -> "Averaging $currFormatted ($energyNote), up from $prevFormatted the prior week."
            TrendDirection.DOWN -> "Averaging $currFormatted ($energyNote), down from $prevFormatted the prior week."
            else -> "Consistent emotional tone around $currFormatted ($energyNote) over the past two weeks."
        }

        return HealthHighlightItem(
            id = "highlight_mood",
            category = HighlightCategory.MOOD,
            title = "Mood & Energy",
            headline = headline,
            narrative = narrative,
            direction = direction,
            sentiment = sentiment,
            currentValueLabel = currFormatted,
            currentPeriodLabel = "Last 7 Days",
            previousValueLabel = prevFormatted,
            previousPeriodLabel = "Prior 7 Days",
            currentBarFraction = (currMood / 5.0).toFloat().coerceIn(0.18f, 1.0f),
            previousBarFraction = (prevMood / 5.0).toFloat().coerceIn(0.18f, 1.0f),
            deltaBadgeText = deltaBadge,
            sampleCountText = "${current.size} vs ${prior.size} days"
        )
    }

    private fun computeMedicationHighlight(
        medications: List<MedicationItem>,
        logs: List<MedicationLog>,
        currentKeys: Set<String>,
        priorKeys: Set<String>
    ): HealthHighlightItem? {
        if (medications.none { it.active } && logs.isEmpty()) return null

        val currentLogs = logs.filter { it.dateKey in currentKeys }
        val priorLogs = logs.filter { it.dateKey in priorKeys }
        if (currentLogs.isEmpty()) return null

        val currTaken = currentLogs.count { it.parsedStatus == MedicationLogStatus.TAKEN }
        val currPct = ((currTaken.toDouble() / currentLogs.size) * 100.0).roundToInt().coerceIn(0, 100)

        if (priorLogs.isEmpty()) {
            val sentiment = when {
                currPct >= 85 -> TrendSentiment.POSITIVE
                currPct < 70 -> TrendSentiment.ATTENTION
                else -> TrendSentiment.NEUTRAL
            }
            val headline = when {
                currPct >= 90 -> "Strong medication consistency ($currPct%)"
                currPct >= 75 -> "Medication adherence at $currPct%"
                else -> "Several doses skipped ($currPct% taken)"
            }
            return HealthHighlightItem(
                id = "highlight_meds",
                category = HighlightCategory.MEDICATIONS,
                title = "Medication Adherence",
                headline = headline,
                narrative = "$currTaken of ${currentLogs.size} logged doses marked taken in the last 7 days.",
                direction = TrendDirection.BASELINE,
                sentiment = sentiment,
                currentValueLabel = "$currPct%",
                currentPeriodLabel = "Last 7 Days",
                currentBarFraction = (currPct / 100f).coerceIn(0.15f, 1.0f),
                deltaBadgeText = "$currTaken/${currentLogs.size} doses",
                sampleCountText = "${currentLogs.size} logged dose(s)"
            )
        }

        val prevTaken = priorLogs.count { it.parsedStatus == MedicationLogStatus.TAKEN }
        val prevPct = ((prevTaken.toDouble() / priorLogs.size) * 100.0).roundToInt().coerceIn(0, 100)
        val delta = currPct - prevPct

        val direction = when {
            delta >= 5 -> TrendDirection.UP
            delta <= -5 -> TrendDirection.DOWN
            else -> TrendDirection.STEADY
        }
        val sentiment = when {
            direction == TrendDirection.UP || currPct >= 85 -> TrendSentiment.POSITIVE
            direction == TrendDirection.DOWN && currPct < 80 -> TrendSentiment.ATTENTION
            currPct < 70 -> TrendSentiment.ATTENTION
            else -> TrendSentiment.NEUTRAL
        }

        val deltaBadge = when (direction) {
            TrendDirection.UP -> "+$delta%"
            TrendDirection.DOWN -> "$delta%"
            else -> "Steady"
        }
        val headline = when (direction) {
            TrendDirection.UP -> "Adherence improved to $currPct%"
            TrendDirection.DOWN -> "Adherence dipped to $currPct% this week"
            else -> "Adherence holding steady at $currPct%"
        }
        val narrative = "$currTaken/${currentLogs.size} doses taken in the last 7 days vs $prevPct% ($prevTaken/${priorLogs.size}) the prior week."

        return HealthHighlightItem(
            id = "highlight_meds",
            category = HighlightCategory.MEDICATIONS,
            title = "Medication Adherence",
            headline = headline,
            narrative = narrative,
            direction = direction,
            sentiment = sentiment,
            currentValueLabel = "$currPct%",
            currentPeriodLabel = "Last 7 Days",
            previousValueLabel = "$prevPct%",
            previousPeriodLabel = "Prior 7 Days",
            currentBarFraction = (currPct / 100f).coerceIn(0.15f, 1.0f),
            previousBarFraction = (prevPct / 100f).coerceIn(0.15f, 1.0f),
            deltaBadgeText = deltaBadge,
            sampleCountText = "${currentLogs.size} vs ${priorLogs.size} doses"
        )
    }

    private fun computeVitalHighlight(
        type: VitalType,
        vitals: List<VitalReading>,
        currentStartMillis: Long,
        priorStartMillis: Long,
        endOfTodayMillis: Long
    ): HealthHighlightItem? {
        val matching = vitals.filter { it.parsedType == type }
        if (matching.isEmpty()) return null

        val current = matching.filter { reading ->
            val ts = reading.effectiveMillis()
            ts in currentStartMillis..endOfTodayMillis
        }
        val prior = matching.filter { reading ->
            val ts = reading.effectiveMillis()
            ts in priorStartMillis until currentStartMillis
        }
        if (current.isEmpty()) return null

        val currPrimary = current.map { it.primaryValue }.average()
        val currSecondary = if (type.hasSecondaryValue) {
            current.mapNotNull { it.secondaryValue }.takeIf { it.isNotEmpty() }?.average()
        } else null
        val currFormatted = "${type.formatValue(currPrimary, currSecondary)} ${type.defaultUnit}"
        val rangeStatus = type.evaluate(currPrimary, currSecondary)

        if (prior.isEmpty()) {
            val sentiment = when (rangeStatus) {
                com.example.data.model.VitalRangeStatus.NORMAL -> TrendSentiment.POSITIVE
                com.example.data.model.VitalRangeStatus.ELEVATED -> TrendSentiment.NEUTRAL
                com.example.data.model.VitalRangeStatus.HIGH,
                com.example.data.model.VitalRangeStatus.LOW -> TrendSentiment.ATTENTION
            }
            val headline = "${type.label} averaging $currFormatted (${rangeStatus.label})"
            val barFrac = normalizeVitalForBar(type, currPrimary, currPrimary)
            return HealthHighlightItem(
                id = "highlight_vital_${type.name.lowercase(Locale.US)}",
                category = HighlightCategory.VITALS,
                title = type.label,
                headline = headline,
                narrative = "Based on ${current.size} reading(s) in the last 7 days. Reference: ${type.normalRangeLabel}.",
                direction = TrendDirection.BASELINE,
                sentiment = sentiment,
                currentValueLabel = currFormatted,
                currentPeriodLabel = "Last 7 Days",
                currentBarFraction = barFrac,
                deltaBadgeText = rangeStatus.label,
                sampleCountText = "${current.size} reading(s)"
            )
        }

        val prevPrimary = prior.map { it.primaryValue }.average()
        val prevSecondary = if (type.hasSecondaryValue) {
            prior.mapNotNull { it.secondaryValue }.takeIf { it.isNotEmpty() }?.average()
        } else null
        val prevFormatted = "${type.formatValue(prevPrimary, prevSecondary)} ${type.defaultUnit}"

        val delta = currPrimary - prevPrimary
        val threshold = when (type) {
            VitalType.BLOOD_PRESSURE -> 5.0
            VitalType.HEART_RATE -> 5.0
            VitalType.SPO2 -> 1.5
            else -> 2.0
        }
        val direction = when {
            delta >= threshold -> TrendDirection.UP
            delta <= -threshold -> TrendDirection.DOWN
            else -> TrendDirection.STEADY
        }

        val sentiment = when (type) {
            VitalType.BLOOD_PRESSURE -> when {
                rangeStatus == com.example.data.model.VitalRangeStatus.HIGH -> TrendSentiment.ATTENTION
                direction == TrendDirection.UP && currPrimary >= 125.0 -> TrendSentiment.ATTENTION
                direction == TrendDirection.DOWN && prevPrimary >= 122.0 && currPrimary >= 95.0 -> TrendSentiment.POSITIVE
                rangeStatus == com.example.data.model.VitalRangeStatus.NORMAL -> TrendSentiment.POSITIVE
                else -> TrendSentiment.NEUTRAL
            }
            VitalType.HEART_RATE -> when {
                rangeStatus == com.example.data.model.VitalRangeStatus.HIGH ||
                    rangeStatus == com.example.data.model.VitalRangeStatus.LOW -> TrendSentiment.ATTENTION
                direction == TrendDirection.UP && delta >= 8.0 -> TrendSentiment.ATTENTION
                rangeStatus == com.example.data.model.VitalRangeStatus.NORMAL -> TrendSentiment.POSITIVE
                else -> TrendSentiment.NEUTRAL
            }
            VitalType.SPO2 -> when {
                currPrimary < 95.0 || direction == TrendDirection.DOWN -> TrendSentiment.ATTENTION
                else -> TrendSentiment.POSITIVE
            }
            else -> TrendSentiment.NEUTRAL
        }

        val deltaInt = delta.roundToInt()
        val deltaBadge = when (direction) {
            TrendDirection.UP -> "+$deltaInt ${type.defaultUnit}"
            TrendDirection.DOWN -> "$deltaInt ${type.defaultUnit}"
            else -> "Steady"
        }
        val headline = when (direction) {
            TrendDirection.UP -> "${type.label} trending higher ($currFormatted)"
            TrendDirection.DOWN -> "${type.label} trending lower ($currFormatted)"
            else -> "${type.label} steady at $currFormatted"
        }
        val narrative = "7-day average is $currFormatted (${rangeStatus.label}) compared to $prevFormatted in the prior 7 days."

        val maxVal = maxOf(currPrimary, prevPrimary, type.normalHigh)
        return HealthHighlightItem(
            id = "highlight_vital_${type.name.lowercase(Locale.US)}",
            category = HighlightCategory.VITALS,
            title = type.label,
            headline = headline,
            narrative = narrative,
            direction = direction,
            sentiment = sentiment,
            currentValueLabel = currFormatted,
            currentPeriodLabel = "Last 7 Days",
            previousValueLabel = prevFormatted,
            previousPeriodLabel = "Prior 7 Days",
            currentBarFraction = (currPrimary / maxVal).toFloat().coerceIn(0.20f, 1.0f),
            previousBarFraction = (prevPrimary / maxVal).toFloat().coerceIn(0.20f, 1.0f),
            deltaBadgeText = deltaBadge,
            sampleCountText = "${current.size} vs ${prior.size} logs"
        )
    }

    private fun computeMentalWellbeingHighlight(
        assessments: List<MentalAssessment>
    ): HealthHighlightItem? {
        // Find the most recent assessment type that has at least 2 entries for real comparison
        val latest = assessments.firstOrNull() ?: return null
        val sameType = assessments.filter { it.parsedType == latest.parsedType }
        val previous = sameType.getOrNull(1) ?: return null

        val type = latest.parsedType
        val currScore = latest.totalScore.toInt()
        val prevScore = previous.totalScore.toInt()
        val delta = currScore - prevScore

        val direction = when {
            delta >= 2 -> TrendDirection.UP
            delta <= -2 -> TrendDirection.DOWN
            else -> TrendDirection.STEADY
        }
        // For PHQ-9 / GAD-7, lower scores indicate symptom relief (POSITIVE); higher scores need ATTENTION
        val sentiment = when {
            latest.crisisFlagged || delta >= 3 -> TrendSentiment.ATTENTION
            delta <= -2 -> TrendSentiment.POSITIVE
            currScore <= 4 -> TrendSentiment.POSITIVE
            else -> TrendSentiment.NEUTRAL
        }

        val deltaBadge = when {
            delta > 0 -> "+$delta pts"
            delta < 0 -> "$delta pts"
            else -> "Unchanged"
        }
        val headline = when {
            delta <= -2 -> "${type.shortLabel} score improved to $currScore/${type.maxScore}"
            delta >= 2 -> "${type.shortLabel} score rose to $currScore/${type.maxScore}"
            else -> "${type.shortLabel} holding steady (${latest.parsedBand.label})"
        }
        val narrative = "Latest ${type.shortLabel} screening is $currScore/${type.maxScore} (${latest.parsedBand.label}) vs $prevScore/${type.maxScore} previously."

        return HealthHighlightItem(
            id = "highlight_mental_${type.name.lowercase(Locale.US)}",
            category = HighlightCategory.MENTAL_WELLBEING,
            title = type.title,
            headline = headline,
            narrative = narrative,
            direction = direction,
            sentiment = sentiment,
            currentValueLabel = "$currScore / ${type.maxScore}",
            currentPeriodLabel = "Latest",
            previousValueLabel = "$prevScore / ${type.maxScore}",
            previousPeriodLabel = "Previous",
            currentBarFraction = (currScore.toFloat() / type.maxScore.toFloat()).coerceIn(0.12f, 1.0f),
            previousBarFraction = (prevScore.toFloat() / type.maxScore.toFloat()).coerceIn(0.12f, 1.0f),
            deltaBadgeText = deltaBadge,
            sampleCountText = "${sameType.size} screenings"
        )
    }

    private fun normalizeVitalForBar(type: VitalType, value: Double, referenceMax: Double): Float {
        val ceiling = maxOf(referenceMax, type.normalHigh * 1.15)
        return (value / ceiling).toFloat().coerceIn(0.22f, 1.0f)
    }

    private fun VitalReading.effectiveMillis(): Long =
        (recordedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()

    private fun parseDateKey(dateKey: String): Date? = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
    } catch (_: Exception) {
        null
    }

    private fun buildDateKeySet(anchorDate: Date, startOffsetDays: Int, count: Int): Set<String> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        return buildSet(count) {
            for (i in 0 until count) {
                cal.time = anchorDate
                cal.add(Calendar.DAY_OF_YEAR, -(startOffsetDays + i))
                add(fmt.format(cal.time))
            }
        }
    }

    private fun endOfDayMillis(anchorDate: Date): Long {
        val cal = Calendar.getInstance().apply {
            time = anchorDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
