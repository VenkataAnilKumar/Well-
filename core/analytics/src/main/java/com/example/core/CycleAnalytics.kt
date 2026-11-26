package com.example.core

import com.example.data.model.CycleLog
import com.example.data.model.CyclePredictionSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Deterministic cycle pattern estimator for informational wellness tracking.
 * Detects menstrual start dates from logged LIGHT/MEDIUM/HEAVY flow clusters
 * and estimates cycle day, phase, and upcoming window.
 */
object CycleAnalytics {

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun computePrediction(
        logs: List<CycleLog>,
        typicalCycleLengthDays: Int,
        todayDateKey: String
    ): CyclePredictionSummary {
        val safeTypical = typicalCycleLengthDays.coerceIn(21, 45)
        val today = parseDateKey(todayDateKey) ?: Date()

        // Sort ascending by dateKey to cluster consecutive period days into distinct cycle starts
        val periodDates = logs
            .filter { it.parsedFlow.countsAsPeriod }
            .mapNotNull { log -> parseDateKey(log.dateKey)?.let { log.dateKey to it } }
            .distinctBy { it.first }
            .sortedBy { it.second.time }

        if (periodDates.isEmpty()) {
            return CyclePredictionSummary(
                currentCycleDay = null,
                estimatedCycleLength = safeTypical,
                currentPhaseLabel = "Log Period Days to Begin",
                lastPeriodStartDateKey = null,
                nextPeriodDateLabel = null,
                daysUntilNextPeriod = null,
                estimatedFertileWindowLabel = null,
                loggedPeriodStartsCount = 0
            )
        }

        // A new period start is any period day > 10 days after the previous logged period day
        val periodStarts = mutableListOf<Pair<String, Date>>()
        var lastSeenPeriodDate: Date? = null
        for ((key, date) in periodDates) {
            val prev = lastSeenPeriodDate
            if (prev == null || daysBetween(prev, date) > 10) {
                periodStarts.add(key to date)
            }
            lastSeenPeriodDate = date
        }

        // Average observed cycle length across consecutive starts (filtering valid 18..55 day gaps)
        val observedLengths = periodStarts.zipWithNext { a, b -> daysBetween(a.second, b.second) }
            .filter { it in 18..55 }
        val estimatedLength = if (observedLengths.isNotEmpty()) {
            observedLengths.average().roundToInt().coerceIn(21, 45)
        } else {
            safeTypical
        }

        val lastStart = periodStarts.last()
        val elapsedSinceStart = daysBetween(lastStart.second, today).coerceAtLeast(0)
        val cycleDay = elapsedSinceStart + 1

        val nextCal = Calendar.getInstance().apply {
            time = lastStart.second
            add(Calendar.DAY_OF_YEAR, estimatedLength)
        }
        val daysUntilNext = daysBetween(today, nextCal.time)
        val displayFmt = SimpleDateFormat("MMM d", Locale.US)

        // Estimated ovulation ~14 days prior to next expected period (standard wellness estimate)
        val ovulationDay = (estimatedLength - 14).coerceIn(8, estimatedLength - 7)
        val fertileStartCal = Calendar.getInstance().apply {
            time = lastStart.second
            add(Calendar.DAY_OF_YEAR, (ovulationDay - 4).coerceAtLeast(5))
        }
        val fertileEndCal = Calendar.getInstance().apply {
            time = lastStart.second
            add(Calendar.DAY_OF_YEAR, (ovulationDay + 1).coerceAtMost(estimatedLength - 3))
        }
        val fertileWindow = "${displayFmt.format(fertileStartCal.time)} – ${displayFmt.format(fertileEndCal.time)}"

        val phase = when {
            cycleDay <= 5 -> "Menstrual Phase"
            cycleDay < (ovulationDay - 3) -> "Follicular Phase"
            cycleDay in (ovulationDay - 3)..(ovulationDay + 1) -> "Ovulatory Window"
            cycleDay <= estimatedLength + 3 -> "Luteal Phase"
            else -> "Cycle Window Passed"
        }

        return CyclePredictionSummary(
            currentCycleDay = cycleDay,
            estimatedCycleLength = estimatedLength,
            currentPhaseLabel = phase,
            lastPeriodStartDateKey = lastStart.first,
            nextPeriodDateLabel = displayFmt.format(nextCal.time),
            daysUntilNextPeriod = daysUntilNext,
            estimatedFertileWindowLabel = fertileWindow,
            loggedPeriodStartsCount = periodStarts.size
        )
    }

    private fun parseDateKey(dateKey: String): Date? = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
    } catch (_: Exception) {
        null
    }

    private fun daysBetween(start: Date, end: Date): Int =
        ((end.time - start.time).toDouble() / MILLIS_PER_DAY).roundToInt()
}
