package com.example.data.repository

import android.graphics.Bitmap
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.BodyMeasurement
import com.example.data.model.ChatMessage
import com.example.data.model.Consultation
import com.example.data.model.ConsultationType
import com.example.data.model.CycleLog
import com.example.data.model.CyclePredictionSummary
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthConnectSnapshot
import com.example.data.model.HealthHighlightItem
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MentalAssessment
import com.example.data.model.MessageRole
import com.example.data.model.VitalReading
import com.example.data.repository.ai.FollowUpChatAiService
import com.example.data.repository.ai.MedicationAiService
import com.example.data.repository.ai.SymptomTriageAiService
import com.example.data.repository.ai.WellnessInsightsAiService

/**
 * Thin compatibility facade delegating to the 4 domain-scoped Firebase AI Logic services.
 * Feature ViewModels inject the specific AI service they require via [com.example.di.WellPlusAppContainer].
 */
class AiDoctorRepository(
    val triageService: SymptomTriageAiService = SymptomTriageAiService(),
    val followUpChatService: FollowUpChatAiService = FollowUpChatAiService(),
    val medicationAiService: MedicationAiService = MedicationAiService(),
    val wellnessInsightsService: WellnessInsightsAiService = WellnessInsightsAiService()
) {
    suspend fun analyzeConsultation(
        type: ConsultationType,
        bodyArea: String,
        symptomSummary: String,
        severity: Int,
        duration: String,
        associatedSymptoms: List<String>,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem> = emptyList(),
        recentVitals: List<VitalReading> = emptyList(),
        photoBitmap: Bitmap?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = triageService.analyzeConsultation(
        type, bodyArea, symptomSummary, severity, duration, associatedSymptoms,
        profile, activeMedications, recentVitals, photoBitmap, region
    )

    suspend fun generateLongitudinalInsights(
        profile: HealthProfile?,
        consultations: List<Consultation>,
        checkIns: List<DailyCheckIn>,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        vitals: List<VitalReading>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = wellnessInsightsService.generateLongitudinalInsights(
        profile, consultations, checkIns, medications, medicationLogs, vitals, region
    )

    fun streamFollowUpReply(
        consultation: Consultation,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem> = emptyList(),
        existingMessages: List<ChatMessage>,
        userMessage: String,
        photoBitmap: Bitmap?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = followUpChatService.streamFollowUpReply(
        consultation, profile, activeMedications, existingMessages, userMessage, photoBitmap, region
    )

    suspend fun analyzeMedicationInteractions(
        medications: List<MedicationItem>,
        profile: HealthProfile?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = medicationAiService.analyzeMedicationInteractions(medications, profile, region)

    suspend fun scanPrescriptionLabel(photoBitmap: Bitmap) =
        medicationAiService.scanPrescriptionLabel(photoBitmap)

    suspend fun generateMentalWellbeingReflection(
        assessment: MentalAssessment,
        recentCheckIns: List<DailyCheckIn>,
        profile: HealthProfile?
    ) = wellnessInsightsService.generateMentalWellbeingReflection(assessment, recentCheckIns, profile)

    suspend fun generateSummaryMicroInsight(
        highlights: List<HealthHighlightItem>,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = wellnessInsightsService.generateSummaryMicroInsight(
        highlights, profile, activeMedications, region
    )

    suspend fun generateBodyAndCycleInsight(
        measurements: List<BodyMeasurement>,
        cycleLogs: List<CycleLog>,
        cyclePrediction: CyclePredictionSummary?,
        cycleEnabled: Boolean,
        useMetric: Boolean,
        profile: HealthProfile?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = wellnessInsightsService.generateBodyAndCycleInsight(
        measurements, cycleLogs, cyclePrediction, cycleEnabled, useMetric, profile, region
    )

    suspend fun generateHealthConnectBrief(
        snapshot: HealthConnectSnapshot,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ) = wellnessInsightsService.generateHealthConnectBrief(
        snapshot, profile, activeMedications, region
    )
}

/**
 * Normalizes consultation follow-up history so Gemini `startChat(history)` always receives
 * strictly alternating `"user"` / `"model"` turns ending with `"model"` before sending
 * the next `"user"` message. Handles orphaned user turns left behind by prior network errors
 * or timeouts, and merges any adjacent same-role turns cleanly.
 */
fun normalizeFollowUpChatTurns(
    seedUserText: String,
    seedModelText: String,
    existingMessages: List<ChatMessage>,
    newUserMessage: String
): Pair<List<Pair<String, String>>, String> {
    val merged = mutableListOf<Pair<String, String>>()
    merged.add("user" to seedUserText.trim())
    merged.add("model" to seedModelText.trim())

    existingMessages.forEach { msg ->
        val text = msg.content.trim()
        if (text.isEmpty()) return@forEach
        val roleKey = if (msg.parsedRole == MessageRole.USER) "user" else "model"
        val last = merged.lastOrNull()
        if (last != null && last.first == roleKey) {
            merged[merged.lastIndex] = roleKey to "${last.second}\n\n$text"
        } else {
            merged.add(roleKey to text)
        }
    }

    var outgoingPrompt = newUserMessage.trim()
    if (merged.lastOrNull()?.first == "user") {
        val trailingOrphan = merged.removeAt(merged.lastIndex).second
        outgoingPrompt = if (trailingOrphan.equals(outgoingPrompt, ignoreCase = true)) {
            outgoingPrompt
        } else {
            "$trailingOrphan\n\n$outgoingPrompt"
        }
    }

    return merged.toList() to outgoingPrompt
}
