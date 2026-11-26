package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class BiologicalSex(val label: String) {
    UNSPECIFIED("Not specified"),
    FEMALE("Female"),
    MALE("Male"),
    INTERSEX("Intersex"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromRaw(raw: String?): BiologicalSex =
            entries.firstOrNull { it.name == raw } ?: UNSPECIFIED
    }
}

enum class BloodType(val code: String) {
    UNKNOWN("UNKNOWN"),
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    val label: String
        get() = if (this == UNKNOWN) "Unknown" else code

    companion object {
        fun fromCode(raw: String?): BloodType =
            entries.firstOrNull { it.code == raw } ?: UNKNOWN
    }
}

@Keep
@IgnoreExtraProperties
data class HealthProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val age: Long = 0L,
    val biologicalSex: String = BiologicalSex.UNSPECIFIED.name,
    val bloodType: String = BloodType.UNKNOWN.code,
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val wellnessGoals: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val ageConfirmed18Plus: Boolean = false,
    val preferredHealthRegion: String = "",
    val preferredEmergencyCountry: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val coverageProvider: String = "",
    val coverageMemberId: String = "",
    val primaryDoctorName: String = "",
    val preferredPharmacy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val hasClinicalContext: Boolean
        get() = age > 0L ||
            biologicalSex != BiologicalSex.UNSPECIFIED.name ||
            allergies.isNotEmpty() ||
            medications.isNotEmpty() ||
            conditions.isNotEmpty()

    val hasEmergencyContact: Boolean
        get() = emergencyContactPhone.isNotBlank()

    val hasCareTeamOrCoverage: Boolean
        get() = coverageProvider.isNotBlank() ||
            coverageMemberId.isNotBlank() ||
            primaryDoctorName.isNotBlank() ||
            preferredPharmacy.isNotBlank()

    fun toPromptContext(activeTrackedMedications: List<MedicationItem> = emptyList()): String {
        val parts = mutableListOf<String>()
        if (age > 0L) parts += "Age: $age"
        val sex = BiologicalSex.fromRaw(biologicalSex)
        if (sex != BiologicalSex.UNSPECIFIED && sex != BiologicalSex.PREFER_NOT_TO_SAY) {
            parts += "Biological sex: ${sex.label}"
        }
        val bt = BloodType.fromCode(bloodType)
        if (bt != BloodType.UNKNOWN) parts += "Blood type: ${bt.label}"
        if (allergies.isNotEmpty()) parts += "Known allergies: ${allergies.joinToString(", ")}"

        val trackedFormatted = activeTrackedMedications
            .filter { it.active }
            .map { med ->
                if (med.dosage.isNotBlank()) "${med.name} (${med.dosage})" else med.name
            }
        val combinedMeds = (trackedFormatted + medications)
            .distinctBy { it.lowercase(Locale.US) }
        if (combinedMeds.isNotEmpty()) parts += "Current medications: ${combinedMeds.joinToString(", ")}"

        if (conditions.isNotEmpty()) parts += "Ongoing conditions: ${conditions.joinToString(", ")}"
        if (wellnessGoals.isNotEmpty()) parts += "Wellness goals: ${wellnessGoals.joinToString(", ")}"
        return if (parts.isEmpty()) {
            "No personal health profile details provided yet."
        } else {
            parts.joinToString(" | ")
        }
    }
}
