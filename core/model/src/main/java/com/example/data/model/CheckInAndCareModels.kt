package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CareCategory(val label: String) {
    AI_SUGGESTION("AI Plan"),
    MEDICATION("Medication"),
    HYDRATION("Hydration"),
    RECOVERY("Recovery"),
    CUSTOM("Personal");

    companion object {
        fun fromRaw(raw: String?): CareCategory =
            entries.firstOrNull { it.name == raw } ?: CUSTOM
    }
}

enum class PhysicalActivityLevel(
    val label: String,
    val shortLabel: String,
    val caption: String,
    val defaultMinutes: Int
) {
    SEDENTARY("Rest / Low", "Rest", "Mostly seated or resting today — gentle stretching helps.", 0),
    LIGHT("Light", "Light", "Casual walking, household movement, or light stretching.", 15),
    MODERATE("Moderate", "Moderate", "Brisk walk, cycling, yoga, or steady aerobic movement.", 30),
    ACTIVE("Active", "Active", "Dedicated workout, run, swim, or active sport session.", 45),
    VIGOROUS("Vigorous", "Vigorous", "High-intensity training or extended endurance session.", 60);

    companion object {
        fun fromRaw(raw: String?): PhysicalActivityLevel =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: MODERATE
    }
}

@Keep
@IgnoreExtraProperties
data class DailyCheckIn(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val moodLevel: Long = 3L,
    val energyLevel: Long = 3L,
    val sleepHours: Double = 7.0,
    val hydrationGlasses: Long = 4L,
    val activityLevel: String = PhysicalActivityLevel.MODERATE.name,
    val activityMinutes: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedActivityLevel: PhysicalActivityLevel
        get() = PhysicalActivityLevel.fromRaw(activityLevel)
}

@Keep
@IgnoreExtraProperties
data class CareTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = CareCategory.CUSTOM.name,
    val completed: Boolean = false,
    val sourceConsultationId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedCategory: CareCategory
        get() = CareCategory.fromRaw(category)
}
