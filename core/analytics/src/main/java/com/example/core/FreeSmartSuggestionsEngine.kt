package com.example.core

import com.example.data.model.CareCategory
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthConnectSnapshot
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.MentalSeverityBand
import com.example.data.model.MentalWellbeingReflection
import com.example.data.model.PhysicalActivityLevel
import com.example.data.model.VitalRangeStatus
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import java.util.Locale

enum class SmartSuggestionCategory(val label: String, val careCategory: CareCategory) {
    VITALS("Vitals", CareCategory.RECOVERY),
    MEDICATIONS("Medications", CareCategory.MEDICATION),
    SLEEP("Sleep", CareCategory.RECOVERY),
    HYDRATION("Hydration", CareCategory.HYDRATION),
    MOVEMENT("Activity", CareCategory.RECOVERY),
    MENTAL_WELLBEING("Mind", CareCategory.RECOVERY),
    VISIT_PREP("Visit Prep", CareCategory.CUSTOM)
}

enum class SmartSuggestionActionTarget(val buttonLabel: String) {
    NONE(""),
    CHECK_IN("Log Check-In"),
    VITALS("Open Vitals"),
    MEDICATIONS("Open Meds"),
    MENTAL_WELLBEING("Wellbeing"),
    DOCTOR_PDF("Doctor PDF")
}

data class SmartWellnessSuggestion(
    val id: String,
    val title: String,
    val reason: String,
    val category: SmartSuggestionCategory,
    val checklistTask: String,
    val isHighPriority: Boolean = false,
    val target: SmartSuggestionActionTarget = SmartSuggestionActionTarget.NONE
)

/**
 * 100% free, zero-latency, offline-capable on-device health suggestion engine.
 * Computes personalized, non-diagnostic wellness suggestions and score-matched mental wellbeing
 * coping steps strictly from the user's real logged metrics — ensuring every user gets complete
 * guidance even without paying, without internet access, or with Cloud AI disabled.
 */
object FreeSmartSuggestionsEngine {

    fun computeDailySuggestions(
        todayDateKey: String,
        todayCheckIn: DailyCheckIn?,
        recentCheckIns: List<DailyCheckIn>,
        vitals: List<VitalReading>,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        mentalAssessments: List<MentalAssessment>,
        healthConnectSnapshot: HealthConnectSnapshot?,
        region: HealthRegionProfile = HealthRegionDirectory.US,
        maxSuggestions: Int = 3
    ): List<SmartWellnessSuggestion> {
        val candidates = mutableListOf<SmartWellnessSuggestion>()

        // 1. Elevated or out-of-range latest vital reading (highest priority)
        val latestBp = vitals.firstOrNull { it.parsedType == VitalType.BLOOD_PRESSURE }
        if (latestBp != null && latestBp.rangeStatus != VitalRangeStatus.NORMAL) {
            val formatted = latestBp.formattedReadingForRegion(region)
            candidates += SmartWellnessSuggestion(
                id = "vital_bp_recheck",
                title = "Recheck blood pressure after 5 min rest",
                reason = "Latest BP was $formatted (${latestBp.rangeStatus.label}). Sit quietly with feet flat and arm supported at heart level, and bring your log to your clinician.",
                category = SmartSuggestionCategory.VITALS,
                checklistTask = "Take calm seated blood pressure recheck after 5 minutes of rest",
                isHighPriority = latestBp.rangeStatus == VitalRangeStatus.HIGH,
                target = SmartSuggestionActionTarget.VITALS
            )
        } else {
            val abnormalVital = vitals.take(6).firstOrNull { it.rangeStatus != VitalRangeStatus.NORMAL }
            if (abnormalVital != null) {
                val formatted = abnormalVital.formattedReadingForRegion(region)
                candidates += SmartWellnessSuggestion(
                    id = "vital_${abnormalVital.parsedType.name.lowercase(Locale.US)}",
                    title = "Follow up on ${abnormalVital.parsedType.shortLabel}",
                    reason = "Recent reading was $formatted (${abnormalVital.rangeStatus.label}). Log a follow-up reading to track whether it returns to baseline.",
                    category = SmartSuggestionCategory.VITALS,
                    checklistTask = "Log a follow-up ${abnormalVital.parsedType.shortLabel} check and note any symptoms",
                    isHighPriority = abnormalVital.rangeStatus == VitalRangeStatus.HIGH || abnormalVital.rangeStatus == VitalRangeStatus.LOW,
                    target = SmartSuggestionActionTarget.VITALS
                )
            }
        }

        // 2. Pending scheduled medications today
        val activeMeds = medications.filter { it.active }
        val todayLogsByMed = medicationLogs
            .filter { it.dateKey == todayDateKey }
            .associateBy { it.medicationId }
        val pendingMeds = activeMeds.filter { med ->
            todayLogsByMed[med.id] == null
        }
        if (pendingMeds.isNotEmpty()) {
            val names = pendingMeds.take(2).joinToString(" & ") { it.name }
            val extra = if (pendingMeds.size > 2) " (+${pendingMeds.size - 2} more)" else ""
            candidates += SmartWellnessSuggestion(
                id = "meds_pending_today",
                title = "Stay on track with today's doses",
                reason = "${pendingMeds.size} scheduled medication(s) still open today: $names$extra.",
                category = SmartSuggestionCategory.MEDICATIONS,
                checklistTask = "Confirm and log today's scheduled medication doses ($names)",
                isHighPriority = true,
                target = SmartSuggestionActionTarget.MEDICATIONS
            )
        }

        // 3. Sleep recovery check (today or 7-day average < 7.0 hrs)
        val recentSleepHours = todayCheckIn?.sleepHours
            ?: recentCheckIns.take(7).map { it.sleepHours }.takeIf { it.isNotEmpty() }?.average()
        if (recentSleepHours != null && recentSleepHours < 7.0) {
            val hrsLabel = String.format(Locale.US, "%.1f", recentSleepHours)
            candidates += SmartWellnessSuggestion(
                id = "sleep_wind_down",
                title = "Protect a 30-min evening wind-down",
                reason = "Recent sleep averaged $hrsLabel hrs (adult target is 7–9 hrs). Dim screens 30 minutes before bed to support steadier recovery.",
                category = SmartSuggestionCategory.SLEEP,
                checklistTask = "Start a 30-minute screen-free wind-down before bedtime tonight",
                isHighPriority = recentSleepHours < 5.5,
                target = SmartSuggestionActionTarget.CHECK_IN
            )
        }

        // 4. Hydration check (today's check-in < 6 cups)
        if (todayCheckIn != null && todayCheckIn.hydrationGlasses < 6) {
            val remaining = (8 - todayCheckIn.hydrationGlasses).coerceAtLeast(1)
            candidates += SmartWellnessSuggestion(
                id = "hydration_pace",
                title = "Add $remaining glasses of water today",
                reason = "${todayCheckIn.hydrationGlasses} of 8 glasses logged so far. Steady hydration supports focus, blood pressure, and energy.",
                category = SmartSuggestionCategory.HYDRATION,
                checklistTask = "Drink 2 glasses of water with your next meal or break",
                target = SmartSuggestionActionTarget.CHECK_IN
            )
        }

        // 5. Recent elevated PHQ-9 / GAD-7 mental wellbeing check
        val latestMental = mentalAssessments.firstOrNull()
        if (latestMental != null && latestMental.parsedBand != MentalSeverityBand.MINIMAL) {
            candidates += SmartWellnessSuggestion(
                id = "mental_grounding",
                title = "Take a 5-minute calming reset",
                reason = "Your latest ${latestMental.parsedType.shortLabel} (${latestMental.parsedBand.label}) suggests extra stress or low energy. Short breathing or daylight walks help.",
                category = SmartSuggestionCategory.MENTAL_WELLBEING,
                checklistTask = "Pause for 5 minutes of slow 4-4-6 breathing or a short daylight walk",
                isHighPriority = latestMental.parsedBand == MentalSeverityBand.MODERATELY_SEVERE ||
                    latestMental.parsedBand == MentalSeverityBand.SEVERE,
                target = SmartSuggestionActionTarget.MENTAL_WELLBEING
            )
        }

        // 6. Health Connect daily steps or logged check-in physical activity nudge
        val stepsToday = healthConnectSnapshot?.stepsToday
        if (stepsToday != null && stepsToday in 1L..3999L) {
            candidates += SmartWellnessSuggestion(
                id = "movement_short_walk",
                title = "Take a 10-minute brisk walk",
                reason = "$stepsToday steps synced today. A brief 10-minute walk adds ~1,000 steps and supports cardiovascular health.",
                category = SmartSuggestionCategory.MOVEMENT,
                checklistTask = "Go for a relaxed 10-minute walk outdoors or indoors today",
                target = SmartSuggestionActionTarget.NONE
            )
        } else if (todayCheckIn != null &&
            (todayCheckIn.parsedActivityLevel == PhysicalActivityLevel.SEDENTARY || todayCheckIn.activityMinutes in 1L..14L)
        ) {
            candidates += SmartWellnessSuggestion(
                id = "movement_checkin_boost",
                title = "Add a 15-minute movement break",
                reason = "Today's check-in logged ${todayCheckIn.parsedActivityLevel.label.lowercase(Locale.US)} activity (${todayCheckIn.activityMinutes} min). A light walk or stretch boosts circulation and mood.",
                category = SmartSuggestionCategory.MOVEMENT,
                checklistTask = "Take a 15-minute walk or gentle mobility stretch today",
                target = SmartSuggestionActionTarget.CHECK_IN
            )
        }

        // 7. Daily Check-In prompt if user hasn't checked in today yet
        if (todayCheckIn == null) {
            candidates += SmartWellnessSuggestion(
                id = "daily_checkin_prompt",
                title = "Log today's daily wellness check-in",
                reason = "Recording your mood, sleep, and physical activity saves to your cloud journal and powers automatic 7-day trend highlights.",
                category = SmartSuggestionCategory.SLEEP,
                checklistTask = "Complete today's mood, sleep, and physical activity check-in",
                target = SmartSuggestionActionTarget.CHECK_IN
            )
        }

        // 8. Visit prep / Doctor PDF suggestion when user has logged vitals or meds
        if (candidates.size < maxSuggestions && (vitals.isNotEmpty() || activeMeds.isNotEmpty())) {
            val coverageWord = if (region.code == HealthRegionCode.CA) "health card" else "insurance"
            candidates += SmartWellnessSuggestion(
                id = "visit_prep_pdf",
                title = "Keep a ready Doctor Visit PDF",
                reason = "Your logged vitals, medications, and $coverageWord details can export in one tap for clinic visits.",
                category = SmartSuggestionCategory.VISIT_PREP,
                checklistTask = "Review and export an updated Doctor Visit Summary PDF before your next appointment",
                target = SmartSuggestionActionTarget.DOCTOR_PDF
            )
        }

        // 9. Helpful Day-1 starter guidance if user is completely new
        if (candidates.size < maxSuggestions && vitals.isEmpty()) {
            candidates += SmartWellnessSuggestion(
                id = "starter_baseline_vital",
                title = "Record one baseline vital sign",
                reason = "Logging a resting heart rate or blood pressure in ${region.code.shortCode} units unlocks trend charts and range alerts.",
                category = SmartSuggestionCategory.VITALS,
                checklistTask = "Log a baseline blood pressure or resting heart rate reading",
                target = SmartSuggestionActionTarget.VITALS
            )
        }

        return candidates
            .distinctBy { it.id }
            .sortedByDescending { it.isHighPriority }
            .take(maxSuggestions)
    }

    /**
     * Produces an immediate, compassionate, non-diagnostic coping guide for any PHQ-9 or GAD-7
     * assessment with zero latency, zero internet requirement, and $0 API cost.
     */
    fun computeMentalWellbeingGuidance(
        assessment: MentalAssessment,
        recentCheckIns: List<DailyCheckIn>,
        region: RegionalEmergencyProfile? = null
    ): MentalWellbeingReflection {
        val type = assessment.parsedType
        val band = assessment.parsedBand
        val crisisLabel = region?.crisisLabel?.takeIf { it.isNotBlank() } ?: "988"

        val elevatedIndices = assessment.itemScores
            .mapIndexedNotNull { idx, score -> if (score >= 2L) idx else null }

        val sleepNote = recentCheckIns.take(7)
            .map { it.sleepHours }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.takeIf { it < 6.5 }
            ?.let { avg ->
                String.format(
                    Locale.US,
                    " Your recent sleep has averaged %.1f hours, which can amplify emotional fatigue.",
                    avg
                )
            }
            .orEmpty()

        val summary = when {
            assessment.crisisFlagged || band == MentalSeverityBand.SEVERE ->
                "Thank you for taking a moment to check in honestly on your ${type.shortLabel} (${assessment.totalScore}/${assessment.maxScore}). " +
                    "What you are carrying right now sounds heavy, and you do not have to navigate it alone. " +
                    "Please consider reaching out to a doctor, counselor, trusted person, or free 24/7 support via $crisisLabel.$sleepNote"

            band == MentalSeverityBand.MODERATELY_SEVERE ->
                "Your ${type.shortLabel} score (${assessment.totalScore}/${assessment.maxScore}) shows these feelings have been weighing on you frequently over the past two weeks. " +
                    "This is a helpful signal to share your results with a primary care clinician or therapist who can offer tailored support.$sleepNote"

            band == MentalSeverityBand.MODERATE ->
                "Your ${type.shortLabel} check-in (${assessment.totalScore}/${assessment.maxScore}) points to noticeable strain over the past two weeks. " +
                    "Small daily anchors—like steadier rest, brief daylight movement, and talking things through with a clinician or trusted friend—can help lighten the load.$sleepNote"

            band == MentalSeverityBand.MILD ->
                "Your ${type.shortLabel} score (${assessment.totalScore}/${assessment.maxScore}) reflects mild day-to-day stress or low periods. " +
                    "Gentle preventive routines and checking back in next week can help you notice what restores your balance.$sleepNote"

            else ->
                "Your ${type.shortLabel} check-in (${assessment.totalScore}/${assessment.maxScore}) sits in the minimal range—a reassuring snapshot of steady emotional balance. " +
                    "Continuing the sleep, movement, and connection habits that work for you helps protect this baseline.$sleepNote"
        }

        val steps = buildList {
            if (type == MentalAssessmentType.GAD7) {
                add("Try 3 rounds of 4-4-6 slow breathing (inhale 4s, hold 4s, exhale 6s) when tension rises")
                add("Write down 1 worry on paper and set a 10-minute limit to revisit it tomorrow")
                if (elevatedIndices.contains(3) || elevatedIndices.contains(4)) {
                    add("Schedule a 10-minute screen-free break to unclench shoulders and stretch")
                } else {
                    add("Step outside for 10 minutes of daylight and notice 5 calm things around you")
                }
            } else {
                add("Choose one tiny 5-minute win today (shower, open curtains, or step outside for sunlight)")
                if (elevatedIndices.contains(2) || elevatedIndices.contains(3)) {
                    add("Protect a consistent bedtime wind-down and drink water before afternoon caffeine")
                } else {
                    add("Send a brief check-in text to one supportive friend or family member")
                }
                add("Take a gentle 10-minute walk at your own pace without performance pressure")
            }

            if (band == MentalSeverityBand.MODERATE ||
                band == MentalSeverityBand.MODERATELY_SEVERE ||
                band == MentalSeverityBand.SEVERE ||
                assessment.crisisFlagged
            ) {
                add("Export your Well+ summary and schedule a check-in with a doctor or counselor")
            }
        }.distinct().take(4)

        val talkingPoints = buildList {
            add("Share your ${type.shortLabel} score (${assessment.totalScore}/${assessment.maxScore} — ${band.label}) and how long these patterns have lasted.")
            val topQuestion = elevatedIndices.firstOrNull()?.let { type.questions.getOrNull(it) }
            if (!topQuestion.isNullOrBlank()) {
                add("Mention the most frequent day-to-day challenge you flagged: \"$topQuestion\".")
            }
            if (band == MentalSeverityBand.MODERATE ||
                band == MentalSeverityBand.MODERATELY_SEVERE ||
                band == MentalSeverityBand.SEVERE
            ) {
                add("Ask about evidence-based support options such as counseling, sleep/stress strategies, or medical check-ups.")
            } else {
                add("Ask what early signs or routine check-in schedule is best for tracking your wellbeing.")
            }
        }

        val encourageSupport = assessment.crisisFlagged ||
            band == MentalSeverityBand.MODERATE ||
            band == MentalSeverityBand.MODERATELY_SEVERE ||
            band == MentalSeverityBand.SEVERE

        return MentalWellbeingReflection(
            empatheticSummary = summary,
            gentleCopingSteps = steps,
            clinicianTalkingPoints = talkingPoints,
            encourageProfessionalSupport = encourageSupport
        )
    }
}
