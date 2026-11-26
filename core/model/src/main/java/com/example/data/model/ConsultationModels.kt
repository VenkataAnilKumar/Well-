package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ConsultationType(val label: String) {
    SYMPTOM_CHECK("Symptom Check"),
    PHOTO_ANALYSIS("Photo & Lab");

    companion object {
        fun fromRaw(raw: String?): ConsultationType =
            entries.firstOrNull { it.name == raw } ?: SYMPTOM_CHECK
    }
}

enum class UrgencyLevel(val label: String, val guidance: String) {
    SELF_CARE("Self-Care", "Manageable at home with monitoring"),
    DOCTOR_SOON("See a Doctor Soon", "Schedule a non-urgent clinician visit"),
    URGENT("Urgent Care", "Seek prompt same-day medical evaluation"),
    EMERGENCY("Seek Emergency Care", "Call emergency services or go to the ER now");

    companion object {
        fun fromRaw(raw: String?): UrgencyLevel =
            entries.firstOrNull { it.name == raw } ?: DOCTOR_SOON
    }
}

enum class FollowUpStatus(val label: String, val summary: String) {
    PENDING("Needs Check-In", "Track how symptoms progress"),
    IMPROVING("Improving", "Feeling better since intake"),
    UNCHANGED("Unchanged", "Symptoms staying steady"),
    WORSENING("Worsening", "Symptoms intensifying — consider clinician review"),
    RESOLVED("Resolved", "Symptoms cleared up");

    companion object {
        fun fromRaw(raw: String?): FollowUpStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class MessageRole {
    USER,
    DOCTOR;

    companion object {
        fun fromRaw(raw: String?): MessageRole =
            entries.firstOrNull { it.name == raw } ?: DOCTOR
    }
}

@Keep
@IgnoreExtraProperties
data class Consultation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val consultationType: String = ConsultationType.SYMPTOM_CHECK.name,
    val title: String = "",
    val bodyArea: String = "",
    val symptomSummary: String = "",
    val severity: Long = 5L,
    val duration: String = "",
    val associatedSymptoms: List<String> = emptyList(),
    val hadPhotoAttachment: Boolean = false,
    val urgencyLevel: String = UrgencyLevel.DOCTOR_SOON.name,
    val aiSummary: String = "",
    val possibleCauses: List<String> = emptyList(),
    val careSuggestions: List<String> = emptyList(),
    val redFlags: List<String> = emptyList(),
    val doctorQuestions: List<String> = emptyList(),
    val followUpStatus: String = FollowUpStatus.PENDING.name,
    val followUpNote: String = "",
    @ServerTimestamp val followUpUpdatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedUrgency: UrgencyLevel
        get() = UrgencyLevel.fromRaw(urgencyLevel)

    val parsedType: ConsultationType
        get() = ConsultationType.fromRaw(consultationType)

    val parsedFollowUpStatus: FollowUpStatus
        get() = FollowUpStatus.fromRaw(followUpStatus)
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val consultationId: String = "",
    val role: String = MessageRole.DOCTOR.name,
    val content: String = "",
    val hadPhotoAttachment: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val parsedRole: MessageRole
        get() = MessageRole.fromRaw(role)
}

data class StructuredTriageResult(
    val title: String,
    val urgencyLevel: UrgencyLevel,
    val aiSummary: String,
    val possibleCauses: List<String>,
    val careSuggestions: List<String>,
    val redFlags: List<String>,
    val doctorQuestions: List<String>
)
