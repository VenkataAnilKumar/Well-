package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class LongitudinalInsightsResult(
    val headline: String,
    val trajectoryLabel: String,
    val executiveSummary: String,
    val correlations: List<String>,
    val medicationAndSafetyNotes: List<String>,
    val actionableRecommendations: List<String>,
    val clinicianDiscussionPoints: List<String>,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

enum class HighlightCategory(val label: String) {
    SLEEP("Sleep"),
    MOOD("Mood & Energy"),
    MEDICATIONS("Medications"),
    VITALS("Vitals"),
    MENTAL_WELLBEING("Mental Wellbeing")
}

enum class TrendDirection(val label: String) {
    UP("Trending Up"),
    DOWN("Trending Down"),
    STEADY("Steady"),
    BASELINE("7-Day Baseline")
}

enum class TrendSentiment {
    POSITIVE,
    NEUTRAL,
    ATTENTION
}

data class HealthHighlightItem(
    val id: String,
    val category: HighlightCategory,
    val title: String,
    val headline: String,
    val narrative: String,
    val direction: TrendDirection,
    val sentiment: TrendSentiment,
    val currentValueLabel: String,
    val currentPeriodLabel: String,
    val previousValueLabel: String? = null,
    val previousPeriodLabel: String? = null,
    val currentBarFraction: Float,
    val previousBarFraction: Float? = null,
    val deltaBadgeText: String,
    val sampleCountText: String
)

data class SummaryMicroInsight(
    val headline: String,
    val summary: String,
    val focusAction: String,
    val watchNote: String,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

enum class AiFeatureArea(val displayLabel: String) {
    SYMPTOM_TRIAGE("Symptom Assessment"),
    FOLLOW_UP_CHAT("AI Follow-Up Chat"),
    DRUG_INTERACTION("Medication Safety Check"),
    MENTAL_REFLECTION("Mental Wellbeing Reflection"),
    HIGHLIGHTS_INSIGHT("7-Day Highlights Insight"),
    LONGITUDINAL_SYNTHESIS("30-Day Health Synthesis"),
    BODY_CYCLE_INSIGHT("Body & Cycle Insights"),
    HEALTH_CONNECT_BRIEF("Wearable & Recovery Brief"),
    LAB_BIOMARKER_SCAN("Lab Biomarker Extraction"),
    PROACTIVE_REASSESSMENT("Proactive Risk Reassessment"),
    SOAP_PREVISIT_BRIEF("SOAP Pre-Visit Brief"),
    CHAT_ACTION_SUGGESTION("Follow-Up Chat Action")
}

enum class AiSafetyReportReason(val label: String, val helper: String) {
    INACCURATE("Inaccurate medical info", "Factually incorrect or misleading health detail"),
    UNSAFE_ADVICE("Unsafe suggestion", "Harmful advice, dosage change, or allergy conflict"),
    MISSED_URGENCY("Missed urgent warning", "Downplayed severe or emergency symptoms"),
    OFFENSIVE("Inappropriate or offensive", "Insensitive, alarming, or inappropriate language"),
    OTHER("Other safety concern", "Any other quality or safety issue")
}

@Keep
@IgnoreExtraProperties
data class AiSafetyReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val featureArea: String = AiFeatureArea.SYMPTOM_TRIAGE.name,
    val reason: String = AiSafetyReportReason.INACCURATE.name,
    val contentSnippet: String = "",
    val userNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
