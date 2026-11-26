package com.example.data.repository.ai

import android.util.Log
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.BodyAndCycleInsight
import com.example.data.model.BodyMeasurement
import com.example.data.model.Consultation
import com.example.data.model.CycleLog
import com.example.data.model.CyclePredictionSummary
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthConnectAiBrief
import com.example.data.model.HealthConnectSnapshot
import com.example.data.model.HealthHighlightItem
import com.example.data.model.HealthProfile
import com.example.data.model.LongitudinalInsightsResult
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalWellbeingReflection
import com.example.data.model.SummaryMicroInsight
import com.example.data.model.VitalReading
import com.example.data.repository.ai.AiServiceSupport.toStringList
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

class WellnessInsightsAiService {

    private val longitudinalSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(
                    description = "Warm, concise 5-10 word headline capturing the user's 30-day wellness trajectory."
                ),
                "trajectoryLabel" to Schema.enumeration(
                    values = listOf("Steady & Balanced", "Improving Trend", "Watch Closely", "Clinician Review Advised"),
                    description = "High-level trajectory badge."
                ),
                "executiveSummary" to Schema.string(
                    description = "Clear 2-4 sentence synthesis combining sleep, mood, hydration, vitals, medications, and symptom entries over the past 30 days. Non-diagnostic framing."
                ),
                "correlations" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 4 concrete patterns spotted across sleep, hydration, mood, vitals, or recurring symptoms."
                ),
                "medicationAndSafetyNotes" to Schema.array(
                    items = Schema.string(),
                    description = "1 to 3 helpful observations about medication adherence, allergy awareness, or potential OTC conflicts to discuss with a pharmacist or doctor."
                ),
                "actionableRecommendations" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 gentle, practical wellness habits for the coming week."
                ),
                "clinicianDiscussionPoints" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 4 structured talking points to share during the user's next doctor appointment."
                )
            )
        )
    }

    private val insightsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = longitudinalSchema
                temperature = 0.35f
                maxOutputTokens = 1536
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Longitudinal Health Analyst.
                    Given up to 30 days of a user's wellness check-ins (mood, energy, sleep, hydration), vital sign readings, active medications and adherence, and past symptom consultations with recovery outcomes, produce a serene, insightful, NON-DIAGNOSTIC personal health synthesis.
                    Focus on honest, data-grounded observations—never fabricate trends not present in the data. Always cross-check active medications and allergies for helpful pharmacist/doctor discussion notes.
                    """.trimIndent()
                )
            }
        )
    }

    private val mentalReflectionSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "empatheticSummary" to Schema.string(
                    description = "Warm, non-judgmental, NON-DIAGNOSTIC 2-3 sentence reflection acknowledging the user's PHQ-9 or GAD-7 score band and any elevated themes (such as sleep, worry, or energy)."
                ),
                "gentleCopingSteps" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 small, low-friction, evidence-based self-care or grounding habits (each concise enough to add directly to a daily care checklist)."
                ),
                "clinicianTalkingPoints" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 helpful discussion points the user can bring to a doctor, therapist, or counselor."
                ),
                "encourageProfessionalSupport" to Schema.boolean(
                    description = "True whenever score is Moderate, Moderately Severe, Severe, or crisisFlagged is true."
                )
            )
        )
    }

    private val mentalReflectionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mentalReflectionSchema
                temperature = 0.35f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Compassionate Mental Wellbeing Companion.
                    Given a user's PHQ-9 or GAD-7 self-screening results and optional recent sleep/mood check-ins, provide a warm, calm, trauma-informed, NON-DIAGNOSTIC reflection and small practical coping steps.
                    Safety rules:
                    - Never diagnose clinical depression, generalized anxiety disorder, or any psychiatric condition.
                    - If crisisFlagged is true or severity is MODERATELY_SEVERE or SEVERE, gently affirm their courage in checking in and clearly encourage connecting with a trusted clinician, loved one, or 24/7 crisis support (988).
                    - Keep suggestions realistic, compassionate, and small (e.g., 5-minute grounding, sunlight walk, consistent bedtime wind-down) rather than overwhelming.
                    """.trimIndent()
                )
            }
        )
    }

    private val summaryMicroInsightSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(
                    description = "Crisp, Apple Health-style 4-8 word headline summarizing the most meaningful 7-day trend."
                ),
                "summary" to Schema.string(
                    description = "Concise, encouraging 2-3 sentence non-diagnostic synthesis connecting the user's detected 7-day trends across sleep, mood, vitals, and medications."
                ),
                "focusAction" to Schema.string(
                    description = "One specific, realistic daily wellness action short enough (under 80 characters) to add directly to a care checklist."
                ),
                "watchNote" to Schema.string(
                    description = "Optional brief 1-sentence caution if a vital or wellbeing trend warrants monitoring or clinician discussion; empty string if everything looks steady."
                )
            ),
            optionalProperties = listOf("watchNote")
        )
    }

    private val summaryMicroInsightModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = summaryMicroInsightSchema
                temperature = 0.35f
                maxOutputTokens = 768
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Daily Summary Trend Companion.
                    Given deterministically computed 7-day vs prior 7-day health highlights (sleep, mood/energy, medication adherence, blood pressure/heart rate/SpO2, and mental wellbeing screenings), craft a crisp, calm, NON-DIAGNOSTIC micro-insight for the user's Summary dashboard.
                    Rules:
                    - Ground every observation strictly in the numbers provided; never invent data or claim a medical diagnosis.
                    - Highlight helpful cross-signal patterns when genuinely present (for example, stronger sleep lining up with steadier mood or consistent medication adherence).
                    - Keep it brief, scannable, warm, and immediately actionable.
                    """.trimIndent()
                )
            }
        )
    }

    private val bodyCycleInsightSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(
                    description = "Warm, supportive, body-neutral 4-9 word headline capturing the user's recent body or cycle patterns."
                ),
                "bodyTrendNote" to Schema.string(
                    description = "Clear, respectful 2-sentence reflection on weight, BMI, or waist trends (or empty string if no body measurements logged)."
                ),
                "cyclePatternNote" to Schema.string(
                    description = "Supportive 2-sentence observation on cycle length regularity, common symptoms, or phase comfort (empty string if cycle tracking is disabled or unlogged)."
                ),
                "gentleRecommendations" to Schema.array(
                    items = Schema.string(),
                    description = "3 practical, supportive wellness habits (hydration, movement, sleep, comfort care) short enough to add to a care checklist."
                ),
                "clinicianTopics" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 constructive questions to discuss with a healthcare provider or OB-GYN if helpful."
                )
            ),
            optionalProperties = listOf("bodyTrendNote", "cyclePatternNote")
        )
    }

    private val bodyCycleInsightModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = bodyCycleInsightSchema
                temperature = 0.35f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Body & Cycle Wellness Companion.
                    Provide calm, compassionate, body-neutral, NON-DIAGNOSTIC pattern observations based strictly on logged body measurements (weight, height, BMI, waist) and optional menstrual cycle logs.
                    Safety & tone rules:
                    - Use supportive, non-judgmental language — never shame weight or BMI numbers; note that BMI is a general screening index rather than a complete health measure.
                    - If cycle logs are provided, note patterns in flow or recurring symptoms gently and never present cycle estimates as contraception or a fertility/hormonal diagnosis.
                    - If rapid unexplained weight swings, very heavy bleeding, or persistent cycle irregularity appear, kindly recommend discussing them with a clinician.
                    """.trimIndent()
                )
            }
        )
    }

    private val healthConnectBriefSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(
                    description = "Warm, concise 4-9 word headline summarizing the user's synced wearable & Health Connect patterns."
                ),
                "activityAndRecoverySummary" to Schema.string(
                    description = "2-sentence observation connecting synced daily steps/activity with rest and recovery based strictly on provided numbers."
                ),
                "cardioAndSleepNote" to Schema.string(
                    description = "2-sentence reflection on synced heart rate range, sleep duration/stages, and blood pressure readings (or a brief note on what metrics are not yet synced)."
                ),
                "actionableTips" to Schema.array(
                    items = Schema.string(),
                    description = "3 specific, practical, checklist-friendly habits tailored to the synced metrics (e.g., walking goal, wind-down routine, hydration, follow-up BP check)."
                ),
                "clinicianNote" to Schema.string(
                    description = "Optional 1-sentence note if elevated BP, unusual resting heart rate, or short sleep warrants sharing with a clinician; empty string if readings look steady."
                )
            ),
            optionalProperties = listOf("clinicianNote")
        )
    }

    private val healthConnectBriefModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = healthConnectBriefSchema
                temperature = 0.35f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Wearable & Health Connect Wellness Analyst.
                    Given real synced metrics from Android Health Connect (steps, heart rate, sleep duration/stages, blood pressure) plus optional health profile context, produce a calm, encouraging, NON-DIAGNOSTIC wellness brief.
                    Rules:
                    - Base every statement strictly on the synced figures provided; never invent readings or claim a diagnosis.
                    - If blood pressure is elevated (e.g. >=140/90) or heart rate is outside typical resting ranges, gently suggest confirming with a validated monitor and discussing with a healthcare provider.
                    - Keep recommendations practical, brief, and directly addable to a daily care checklist.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateLongitudinalInsights(
        profile: HealthProfile?,
        consultations: List<Consultation>,
        checkIns: List<DailyCheckIn>,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        vitals: List<VitalReading>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): LongitudinalInsightsResult {
        val activeMeds = medications.filter { it.active }
        val takenCount = medicationLogs.count { it.parsedStatus == MedicationLogStatus.TAKEN }
        val adherenceSummary = if (medicationLogs.isNotEmpty()) {
            val pct = ((takenCount.toDouble() / medicationLogs.size) * 100).toInt()
            "$pct% ($takenCount of ${medicationLogs.size} logged doses taken)"
        } else {
            "No dose logs recorded yet"
        }

        val prompt = buildString {
            appendLine("Analyze this user's 30-day health sanctuary record and generate a structured longitudinal synthesis.")
            appendLine(region.aiPromptContext)
            appendLine("Health Profile: ${profile?.toPromptContext(activeMeds) ?: "Not completed"}")
            appendLine("Active Medications (${activeMeds.size}): ${activeMeds.joinToString(", ") { "${it.name} ${it.dosage} [${it.parsedSlot.label}]".trim() }.ifBlank { "None" }}")
            appendLine("Medication Adherence: $adherenceSummary")

            if (checkIns.isNotEmpty()) {
                val avgMood = checkIns.map { it.moodLevel }.average()
                val avgEnergy = checkIns.map { it.energyLevel }.average()
                val avgSleep = checkIns.map { it.sleepHours }.average()
                val avgHydration = checkIns.map { it.hydrationGlasses }.average()
                val avgActivityMin = checkIns.map { it.activityMinutes }.average()
                appendLine(
                    String.format(
                        Locale.US,
                        "Daily Wellness Check-Ins (%d days): Avg Mood %.1f/5, Avg Energy %.1f/5, Avg Sleep %.1fh, Avg Hydration %.1f cups, Avg Physical Activity %.0f min/day",
                        checkIns.size, avgMood, avgEnergy, avgSleep, avgHydration, avgActivityMin
                    )
                )
                checkIns.take(10).forEach { ci ->
                    appendLine(
                        "  - ${ci.dateKey}: mood ${ci.moodLevel}/5, energy ${ci.energyLevel}/5, sleep ${ci.sleepHours}h, water ${ci.hydrationGlasses} cups, activity ${ci.parsedActivityLevel.label} (${ci.activityMinutes}m)"
                    )
                }
            } else {
                appendLine("Daily Wellness Check-Ins: None logged yet.")
            }

            if (vitals.isNotEmpty()) {
                appendLine("Logged Vitals (${vitals.size} recent readings):")
                vitals.take(12).forEach { v ->
                    appendLine("  - ${v.parsedType.label}: ${v.formattedReadingForRegion(region)} (${v.rangeStatus.label})${if (v.note.isNotBlank()) " [${v.note}]" else ""}")
                }
            } else {
                appendLine("Logged Vitals: None logged yet.")
            }

            if (consultations.isNotEmpty()) {
                appendLine("Consultations (${consultations.size} entries):")
                consultations.take(10).forEach { c ->
                    appendLine(
                        "  - ${c.title} (${c.bodyArea}, severity ${c.severity}/10, urgency ${c.parsedUrgency.label}, outcome ${c.parsedFollowUpStatus.label}): ${c.symptomSummary.take(180)}"
                    )
                }
            } else {
                appendLine("Consultations: None logged yet.")
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                    insightsModel.generateContent(prompt)
                }
                val rawJson = response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("AI Health Synthesis returned an empty response.")
                parseLongitudinalInsights(rawJson)
            } catch (e: TimeoutCancellationException) {
                Log.e(AI_LOG_TAG, "Longitudinal insights timed out", e)
                throw IllegalStateException("Health synthesis timed out after 60 seconds. Please try again.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_LOG_TAG, "Longitudinal insights failed", e)
                throw e
            }
        }
    }

    suspend fun generateMentalWellbeingReflection(
        assessment: MentalAssessment,
        recentCheckIns: List<DailyCheckIn>,
        profile: HealthProfile?
    ): MentalWellbeingReflection {
        val type = assessment.parsedType
        val elevatedItems = assessment.itemScores.mapIndexedNotNull { index, score ->
            val qText = type.questions.getOrNull(index)
            if (qText != null && score >= 2L) "$qText (score $score/3)" else null
        }
        val recentSleep = recentCheckIns.take(7).takeIf { it.isNotEmpty() }?.map { it.sleepHours }?.average()
        val recentMood = recentCheckIns.take(7).takeIf { it.isNotEmpty() }?.map { it.moodLevel }?.average()

        val prompt = buildString {
            appendLine("Create a compassionate, non-diagnostic reflection for this mental wellbeing screening.")
            appendLine("Instrument: ${type.title} (${type.shortLabel})")
            appendLine("Score: ${assessment.totalScore} / ${assessment.maxScore} (${assessment.parsedBand.label})")
            appendLine("Crisis/High-Risk Flagged: ${assessment.crisisFlagged}")
            if (elevatedItems.isNotEmpty()) {
                appendLine("Most Frequent Themes Over Past 2 Weeks:")
                elevatedItems.forEach { appendLine("  - $it") }
            }
            if (recentSleep != null && recentMood != null) {
                appendLine(
                    String.format(
                        Locale.US,
                        "Recent 7-Day Check-In Context: Avg Sleep %.1fh, Avg Mood %.1f/5",
                        recentSleep,
                        recentMood
                    )
                )
            }
            if (profile != null && profile.wellnessGoals.isNotEmpty()) {
                appendLine("User Wellness Goals: ${profile.wellnessGoals.joinToString(", ")}")
            }
        }

        val rawJson = AiServiceSupport.generateJsonResilient(
            model = mentalReflectionModel,
            prompt = prompt,
            label = "Mental wellbeing reflection",
            emptyResponseMessage = "Wellbeing reflection returned an empty response. Please try again.",
            timeoutMessage = "Reflection timed out. Please check your connection and try again."
        )
        return parseMentalReflection(rawJson)
    }

    suspend fun generateSummaryMicroInsight(
        highlights: List<HealthHighlightItem>,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): SummaryMicroInsight {
        val prompt = buildString {
            appendLine("Synthesize these detected 7-day health highlights into a brief, encouraging Summary Micro-Insight.")
            appendLine(region.aiPromptContext)
            if (profile != null) {
                appendLine("Health Profile Context: ${profile.toPromptContext(activeMedications)}")
            }
            appendLine("Computed Highlights (${highlights.size}):")
            highlights.forEach { item ->
                val comparison = if (item.previousValueLabel != null) {
                    "${item.currentValueLabel} (${item.currentPeriodLabel}) vs ${item.previousValueLabel} (${item.previousPeriodLabel}) [${item.deltaBadgeText}]"
                } else {
                    "${item.currentValueLabel} (${item.currentPeriodLabel}, ${item.deltaBadgeText})"
                }
                appendLine("  - ${item.title}: $comparison — ${item.headline}. ${item.narrative}")
            }
        }

        val rawJson = AiServiceSupport.generateJsonResilient(
            model = summaryMicroInsightModel,
            prompt = prompt,
            label = "Summary micro-insight",
            emptyResponseMessage = "AI Micro-Insight returned an empty response. Please try again.",
            timeoutMessage = "Micro-insight timed out. Please check your connection and try again."
        )
        return parseSummaryMicroInsight(rawJson)
    }

    suspend fun generateBodyAndCycleInsight(
        measurements: List<BodyMeasurement>,
        cycleLogs: List<CycleLog>,
        cyclePrediction: CyclePredictionSummary?,
        cycleEnabled: Boolean,
        useMetric: Boolean,
        profile: HealthProfile?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): BodyAndCycleInsight {
        val prompt = buildString {
            appendLine("Generate a supportive, body-neutral wellness pattern reflection from these records.")
            appendLine(region.aiPromptContext)
            if (profile != null && profile.hasClinicalContext) {
                appendLine("Profile Context: ${profile.toPromptContext()}")
            }
            if (measurements.isNotEmpty()) {
                val latest = measurements.first()
                val oldest = measurements.last()
                appendLine("Body Measurements (${measurements.size} entries, newest first):")
                appendLine(
                    "  Latest: weight=${latest.formattedWeight(useMetric)}, height=${latest.formattedHeight(useMetric)}, BMI=${if (latest.bmi > 0) latest.bmi else "N/A"} (${latest.bmiCategory.label}), waist=${latest.formattedWaist(useMetric)}"
                )
                if (measurements.size > 1) {
                    val deltaKg = latest.weightKg - oldest.weightKg
                    val deltaStr = if (useMetric) {
                        String.format(Locale.US, "%+.1f kg", deltaKg)
                    } else {
                        String.format(Locale.US, "%+.1f lbs", deltaKg * 2.20462262)
                    }
                    appendLine("  Change across ${measurements.size} logs: $deltaStr")
                }
            } else {
                appendLine("Body Measurements: None logged yet.")
            }

            if (cycleEnabled && cycleLogs.isNotEmpty()) {
                appendLine("Cycle Tracking (${cycleLogs.size} logged days):")
                cyclePrediction?.let { pred ->
                    appendLine(
                        "  Estimated cycle length: ${pred.estimatedCycleLength} days | Current cycle day: ${pred.currentCycleDay ?: "N/A"} | Phase: ${pred.currentPhaseLabel} | Next expected window: ${pred.nextPeriodDateLabel ?: "N/A"}"
                    )
                }
                cycleLogs.take(14).forEach { log ->
                    val symText = log.symptoms.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "none"
                    appendLine("  - ${log.dateKey}: flow=${log.parsedFlow.label}, mood=${log.moodTag.ifBlank { "—" }}, symptoms=$symText")
                }
            } else {
                appendLine("Cycle Tracking: Not enabled or no logs.")
            }
        }

        val rawJson = AiServiceSupport.generateJsonResilient(
            model = bodyCycleInsightModel,
            prompt = prompt,
            label = "Body & cycle insight",
            emptyResponseMessage = "Body & Cycle AI Insight returned an empty response. Please try again.",
            timeoutMessage = "Pattern insight timed out. Please check your connection and try again."
        )
        return parseBodyAndCycleInsight(rawJson)
    }

    suspend fun generateHealthConnectBrief(
        snapshot: HealthConnectSnapshot,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): HealthConnectAiBrief {
        val prompt = buildString {
            appendLine("Synthesize this user's real Android Health Connect metrics into a concise wellness brief.")
            appendLine(region.aiPromptContext)
            appendLine()
            appendLine("=== PROFILE CONTEXT ===")
            if (profile != null) {
                appendLine(profile.toPromptContext(activeMedications))
            } else if (activeMedications.isNotEmpty()) {
                appendLine(
                    "Active tracked medications: " +
                        activeMedications.filter { it.active }.joinToString(", ") { it.name }
                )
            } else {
                appendLine("No profile details provided.")
            }
            appendLine()
            appendLine("=== SYNCED HEALTH CONNECT DATA (LAST 7–30 DAYS) ===")
            if (snapshot.stepsToday != null || snapshot.dailySteps7Days.isNotEmpty()) {
                appendLine("Steps today: ${snapshot.stepsToday ?: 0}")
                snapshot.average7DaySteps?.let { appendLine("7-day average daily steps: $it") }
                if (snapshot.dailySteps7Days.isNotEmpty()) {
                    appendLine(
                        "Daily steps breakdown: " +
                            snapshot.dailySteps7Days.joinToString(", ") { "${it.dayLabel}: ${it.steps}" }
                    )
                }
            } else {
                appendLine("Steps: not synced or no data recorded.")
            }

            snapshot.heartRate?.let { hr ->
                appendLine(
                    "Heart Rate: latest ${hr.latestBpm} bpm" +
                        (hr.avgBpm?.let { ", 7d avg $it bpm" } ?: "") +
                        (if (hr.minBpm != null && hr.maxBpm != null) " (range ${hr.minBpm}–${hr.maxBpm} bpm)" else "") +
                        ", ${hr.sampleCount} samples"
                )
            } ?: appendLine("Heart Rate: not synced or no samples recorded.")

            snapshot.sleep?.let { sl ->
                appendLine(
                    String.format(
                        Locale.US,
                        "Sleep: last night %.1f hrs, 7d avg %.1f hrs (%d sessions)",
                        sl.lastNightHours,
                        sl.average7DayHours,
                        sl.sessionCount
                    )
                )
                if (sl.hasStageBreakdown) {
                    appendLine(
                        "Last night sleep stages: Deep ${sl.deepMinutes}m, REM ${sl.remMinutes}m, Light ${sl.lightMinutes}m, Awake ${sl.awakeMinutes}m"
                    )
                }
            } ?: appendLine("Sleep: not synced or no sleep sessions recorded.")

            snapshot.latestBloodPressure?.let { bp ->
                appendLine(
                    "Latest Blood Pressure: ${bp.formattedValue} mmHg (${bp.clinicalRange.label}); ${snapshot.recentBloodPressures.size} reading(s) in last 30 days"
                )
            } ?: appendLine("Blood Pressure: not synced or no readings recorded.")
        }

        val rawText = AiServiceSupport.generateJsonResilient(
            model = healthConnectBriefModel,
            prompt = prompt,
            label = "Health Connect AI brief",
            emptyResponseMessage = "Gemini returned an empty response for your wearable brief.",
            timeoutMessage = "Health Connect AI brief timed out. Please check your connection and try again."
        )
        return parseHealthConnectBrief(rawText)
    }

    private fun parseMentalReflection(rawJson: String): MentalWellbeingReflection {
        val json = JSONObject(rawJson)
        return MentalWellbeingReflection(
            empatheticSummary = json.optString("empatheticSummary", "")
                .trim()
                .ifBlank { "Thank you for checking in with yourself today. Consider sharing this summary with a clinician if these feelings continue." },
            gentleCopingSteps = json.optJSONArray("gentleCopingSteps").toStringList(),
            clinicianTalkingPoints = json.optJSONArray("clinicianTalkingPoints").toStringList(),
            encourageProfessionalSupport = json.optBoolean("encourageProfessionalSupport", false)
        )
    }

    private fun parseSummaryMicroInsight(rawJson: String): SummaryMicroInsight {
        val json = JSONObject(rawJson)
        return SummaryMicroInsight(
            headline = json.optString("headline", "Your 7-Day Health Snapshot")
                .trim().ifBlank { "Your 7-Day Health Snapshot" },
            summary = json.optString("summary", "")
                .trim().ifBlank { "Your recent check-ins and tracked health metrics are synced. Keep logging daily to deepen your 7-day trend comparisons." },
            focusAction = json.optString("focusAction", "")
                .trim().ifBlank { "Keep your evening wind-down and hydration routine steady today." },
            watchNote = json.optString("watchNote", "").trim()
        )
    }

    private fun parseBodyAndCycleInsight(rawJson: String): BodyAndCycleInsight {
        val json = JSONObject(rawJson)
        return BodyAndCycleInsight(
            headline = json.optString("headline", "Your Body & Wellness Patterns")
                .trim().ifBlank { "Your Body & Wellness Patterns" },
            bodyTrendNote = json.optString("bodyTrendNote", "").trim(),
            cyclePatternNote = json.optString("cyclePatternNote", "").trim(),
            gentleRecommendations = json.optJSONArray("gentleRecommendations").toStringList(),
            clinicianTopics = json.optJSONArray("clinicianTopics").toStringList()
        )
    }

    private fun parseLongitudinalInsights(rawJson: String): LongitudinalInsightsResult {
        val json = JSONObject(rawJson)
        return LongitudinalInsightsResult(
            headline = json.optString("headline", "30-Day Health Sanctuary Synthesis")
                .trim().ifBlank { "30-Day Health Sanctuary Synthesis" },
            trajectoryLabel = json.optString("trajectoryLabel", "Steady & Balanced")
                .trim().ifBlank { "Steady & Balanced" },
            executiveSummary = json.optString("executiveSummary", "")
                .trim().ifBlank { "Continue tracking daily check-ins, vitals, and symptoms to uncover deeper patterns." },
            correlations = json.optJSONArray("correlations").toStringList(),
            medicationAndSafetyNotes = json.optJSONArray("medicationAndSafetyNotes").toStringList(),
            actionableRecommendations = json.optJSONArray("actionableRecommendations").toStringList(),
            clinicianDiscussionPoints = json.optJSONArray("clinicianDiscussionPoints").toStringList()
        )
    }

    private fun parseHealthConnectBrief(rawJson: String): HealthConnectAiBrief {
        val json = JSONObject(rawJson)
        return HealthConnectAiBrief(
            headline = json.optString("headline", "Your Synced Wearable Brief")
                .trim().ifBlank { "Your Synced Wearable Brief" },
            activityAndRecoverySummary = json.optString("activityAndRecoverySummary", "").trim(),
            cardioAndSleepNote = json.optString("cardioAndSleepNote", "").trim(),
            actionableTips = json.optJSONArray("actionableTips").toStringList().take(4),
            clinicianNote = json.optString("clinicianNote", "").trim()
        )
    }
}
