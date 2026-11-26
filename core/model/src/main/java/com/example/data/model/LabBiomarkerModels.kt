package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class LabBiomarkerCategory(val label: String) {
    METABOLIC("Metabolic & Glucose"),
    LIPIDS("Lipids & Cholesterol"),
    THYROID("Thyroid"),
    VITAMINS("Vitamins & Minerals"),
    CBC("Blood Count (CBC)"),
    KIDNEY_LIVER("Kidney & Liver"),
    OTHER("Other Labs");

    companion object {
        fun fromRaw(raw: String?): LabBiomarkerCategory =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: OTHER
    }
}

enum class LabBiomarkerStatus(val label: String) {
    OPTIMAL("In Range"),
    BORDERLINE("Borderline"),
    HIGH("High"),
    LOW("Low");

    companion object {
        fun fromRaw(raw: String?): LabBiomarkerStatus =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: OPTIMAL
    }
}

@Keep
@IgnoreExtraProperties
data class LabBiomarker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val markerName: String = "",
    val category: String = LabBiomarkerCategory.OTHER.name,
    val value: Double = 0.0,
    val unit: String = "",
    val referenceRangeText: String = "",
    val status: String = LabBiomarkerStatus.OPTIMAL.name,
    val panelDateKey: String = "",
    val labNote: String = "",
    @ServerTimestamp val recordedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedCategory: LabBiomarkerCategory
        get() = LabBiomarkerCategory.fromRaw(category)

    val parsedStatus: LabBiomarkerStatus
        get() = LabBiomarkerStatus.fromRaw(status)

    val formattedValue: String
        get() = if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
}

data class ExtractedBiomarkerDraft(
    val markerName: String,
    val category: LabBiomarkerCategory,
    val value: Double,
    val unit: String,
    val referenceRangeText: String,
    val status: LabBiomarkerStatus,
    val labNote: String,
    val selected: Boolean = true
)

data class ExtractedLabPanelResult(
    val panelSummary: String,
    val panelDateKey: String,
    val biomarkers: List<ExtractedBiomarkerDraft>,
    val clinicianDiscussionPoints: List<String> = emptyList()
)

data class ClarifyingQuestionItem(
    val id: String,
    val question: String,
    val quickOptions: List<String>,
    val clinicalReason: String = ""
)

data class ProactiveReassessmentResult(
    val urgencyLevel: UrgencyLevel,
    val headline: String,
    val clinicalReasoning: String,
    val immediateNextSteps: List<String>,
    val redFlagsToWatch: List<String>,
    val doctorPrepQuestion: String,
    val generatedAtMillis: Long = System.currentTimeMillis()
)
