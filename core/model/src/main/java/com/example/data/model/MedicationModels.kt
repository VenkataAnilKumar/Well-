package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class MedicationScheduleSlot(
    val label: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    MORNING("Morning", 8, 0),
    MIDDAY("Midday", 13, 0),
    EVENING("Evening", 19, 0),
    BEDTIME("Bedtime", 22, 0),
    AS_NEEDED("As Needed", 9, 0);

    companion object {
        fun fromRaw(raw: String?): MedicationScheduleSlot =
            entries.firstOrNull { it.name == raw } ?: MORNING
    }
}

enum class MedicationLogStatus(val label: String) {
    TAKEN("Taken"),
    SKIPPED("Skipped");

    companion object {
        fun fromRaw(raw: String?): MedicationLogStatus =
            entries.firstOrNull { it.name == raw } ?: TAKEN
    }
}

@Keep
@IgnoreExtraProperties
data class MedicationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val scheduleSlot: String = MedicationScheduleSlot.MORNING.name,
    val reminderHour: Long = 8L,
    val reminderMinute: Long = 0L,
    val instructions: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedSlot: MedicationScheduleSlot
        get() = MedicationScheduleSlot.fromRaw(scheduleSlot)

    fun formattedReminderTime(): String {
        val h24 = reminderHour.toInt().coerceIn(0, 23)
        val m = reminderMinute.toInt().coerceIn(0, 59)
        val amPm = if (h24 >= 12) "PM" else "AM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
    }
}

@Keep
@IgnoreExtraProperties
data class MedicationLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val medicationId: String = "",
    val medicationName: String = "",
    val status: String = MedicationLogStatus.TAKEN.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: MedicationLogStatus
        get() = MedicationLogStatus.fromRaw(status)
}

enum class InteractionSeverity(val label: String, val guidance: String) {
    CRITICAL("Critical", "Do not combine without immediate doctor or pharmacist guidance"),
    SERIOUS("Serious", "Review promptly with your prescribing clinician or pharmacist"),
    MODERATE("Moderate", "Monitor closely and confirm timing or dosing with your pharmacist"),
    NONE("No Major Conflicts", "No major drug, allergy, or condition conflicts identified");

    companion object {
        fun fromRaw(raw: String?): InteractionSeverity =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: MODERATE
    }
}

data class DrugInteractionAlert(
    val pairLabel: String,
    val severity: InteractionSeverity,
    val summary: String,
    val recommendation: String
)

data class MedicationInteractionReport(
    val overallStatus: String,
    val highestSeverity: InteractionSeverity,
    val executiveSummary: String,
    val interactions: List<DrugInteractionAlert>,
    val lifestyleAndFoodNotes: List<String>,
    val pharmacistQuestions: List<String>,
    val checkedMedicationCount: Int,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

data class ScannedPrescriptionDraft(
    val name: String,
    val dosage: String,
    val scheduleSlot: MedicationScheduleSlot,
    val reminderHour: Int,
    val reminderMinute: Int,
    val instructions: String,
    val confidenceNote: String
)
