package com.example.data.repository

import android.content.Context
import com.example.core.AppResourceResolver
import com.example.data.model.AiFeatureArea
import com.example.data.model.AiSafetyReportReason
import com.example.data.model.BiologicalSex
import com.example.data.model.BloodType
import com.example.data.model.CareCategory
import com.example.data.model.ConsultationType
import com.example.data.model.CycleFlowLevel
import com.example.data.model.FollowUpStatus
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.MentalAssessmentType
import com.example.data.model.MentalSeverityBand
import com.example.data.model.MessageRole
import com.example.data.model.PhysicalActivityLevel
import com.example.data.model.StructuredTriageResult
import com.example.data.model.VitalType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map

/**
 * Thin compatibility facade delegating to the 7 domain-scoped Firestore repositories.
 * Feature ViewModels inject the specific domain repository they need via [com.example.di.WellPlusAppContainer].
 */
class HealthCloudRepository(
    db: FirebaseFirestore,
    auth: FirebaseAuth = Firebase.auth
) {
    private val session = FirestoreSessionHelper(db, auth)

    val profiles = ProfileRepository(session)
    val consultations = ConsultationRepository(session)
    val checkInsAndCare = CheckInAndCareRepository(session)
    val medications = MedicationRepository(session)
    val vitals = VitalRepository(session)
    val mentalAndBody = MentalAndBodyRepository(session)
    val portability = AccountDataPortabilityRepository(session)

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            AppResourceResolver.getRequiredString(context, "firestore_database_id")
        )
    )

    fun observeProfile(userId: String) = profiles.observeProfile(userId)
    fun observeConsultations(userId: String) =
        consultations.observeConsultationsHead(userId).map { it.items }
    fun observeConsultationsHead(userId: String, pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE) =
        consultations.observeConsultationsHead(userId, pageSize)
    suspend fun fetchOlderConsultations(
        userId: String,
        startAfter: DocumentSnapshot,
        pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE
    ) = consultations.fetchOlderConsultations(userId, startAfter, pageSize)
    fun observeMessages(userId: String, consultationId: String) =
        consultations.observeMessages(userId, consultationId)
    fun observeCheckIns(userId: String) = checkInsAndCare.observeCheckIns(userId)
    fun observeCareTasks(userId: String) = checkInsAndCare.observeCareTasks(userId)
    fun observeMedications(userId: String) = medications.observeMedications(userId)
    fun observeMedicationLogs(userId: String) = medications.observeMedicationLogs(userId)
    fun observeVitals(userId: String) = vitals.observeVitalsHead(userId).map { it.items }
    fun observeVitalsHead(userId: String, pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE) =
        vitals.observeVitalsHead(userId, pageSize)
    suspend fun fetchOlderVitals(
        userId: String,
        startAfter: DocumentSnapshot,
        pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE
    ) = vitals.fetchOlderVitals(userId, startAfter, pageSize)
    fun observeMentalAssessments(userId: String) = mentalAndBody.observeMentalAssessments(userId)
    fun observeBodyMeasurements(userId: String) = mentalAndBody.observeBodyMeasurements(userId)
    fun observeCycleLogs(userId: String) = mentalAndBody.observeCycleLogs(userId)

    suspend fun saveProfile(
        userId: String,
        displayName: String,
        age: Long,
        biologicalSex: BiologicalSex,
        bloodType: BloodType,
        allergies: List<String>,
        medications: List<String>,
        conditions: List<String>,
        wellnessGoals: List<String>,
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        onboardingCompleted: Boolean = true,
        disclaimerAccepted: Boolean = true,
        ageConfirmed18Plus: Boolean = true,
        preferredHealthRegion: String = "",
        preferredEmergencyCountry: String = "",
        coverageProvider: String = "",
        coverageMemberId: String = "",
        primaryDoctorName: String = "",
        preferredPharmacy: String = ""
    ) = profiles.saveProfile(
        userId, displayName, age, biologicalSex, bloodType, allergies, medications, conditions,
        wellnessGoals, emergencyContactName, emergencyContactPhone, onboardingCompleted,
        disclaimerAccepted, ageConfirmed18Plus, preferredHealthRegion, preferredEmergencyCountry,
        coverageProvider, coverageMemberId, primaryDoctorName, preferredPharmacy
    )

    suspend fun createConsultation(
        userId: String,
        type: ConsultationType,
        bodyArea: String,
        symptomSummary: String,
        severity: Long,
        duration: String,
        associatedSymptoms: List<String>,
        hadPhotoAttachment: Boolean,
        analysis: StructuredTriageResult
    ) = consultations.createConsultation(
        userId, type, bodyArea, symptomSummary, severity, duration, associatedSymptoms,
        hadPhotoAttachment, analysis
    )

    suspend fun updateConsultationFollowUp(
        userId: String,
        consultationId: String,
        status: FollowUpStatus,
        note: String
    ) = consultations.updateConsultationFollowUp(userId, consultationId, status, note)

    suspend fun addChatMessage(
        userId: String,
        consultationId: String,
        role: MessageRole,
        content: String,
        hadPhotoAttachment: Boolean = false
    ) = consultations.addChatMessage(userId, consultationId, role, content, hadPhotoAttachment)

    suspend fun deleteConsultation(userId: String, consultationId: String) =
        consultations.deleteConsultation(userId, consultationId)

    suspend fun saveDailyCheckIn(
        userId: String,
        dateKey: String,
        moodLevel: Long,
        energyLevel: Long,
        sleepHours: Double,
        hydrationGlasses: Long,
        notes: String,
        activityLevel: PhysicalActivityLevel = PhysicalActivityLevel.MODERATE,
        activityMinutes: Long = 0L
    ) = checkInsAndCare.saveDailyCheckIn(
        userId, dateKey, moodLevel, energyLevel, sleepHours, hydrationGlasses, notes,
        activityLevel, activityMinutes
    )

    suspend fun addCareTask(
        userId: String,
        title: String,
        category: CareCategory,
        sourceConsultationId: String? = null
    ) = checkInsAndCare.addCareTask(userId, title, category, sourceConsultationId)

    suspend fun toggleCareTask(userId: String, taskId: String, completed: Boolean) =
        checkInsAndCare.toggleCareTask(userId, taskId, completed)

    suspend fun deleteCareTask(userId: String, taskId: String) =
        checkInsAndCare.deleteCareTask(userId, taskId)

    suspend fun saveMedication(
        userId: String,
        existingId: String? = null,
        name: String,
        dosage: String,
        scheduleSlot: MedicationScheduleSlot,
        reminderHour: Int,
        reminderMinute: Int,
        instructions: String,
        active: Boolean = true
    ) = medications.saveMedication(
        userId, existingId, name, dosage, scheduleSlot, reminderHour, reminderMinute, instructions, active
    )

    suspend fun setMedicationActive(userId: String, medicationId: String, active: Boolean) =
        medications.setMedicationActive(userId, medicationId, active)

    suspend fun deleteMedication(userId: String, medicationId: String) =
        medications.deleteMedication(userId, medicationId)

    suspend fun logMedicationDose(
        userId: String,
        dateKey: String,
        medication: MedicationItem,
        status: MedicationLogStatus
    ) = medications.logMedicationDose(userId, dateKey, medication, status)

    suspend fun addVitalReading(
        userId: String,
        vitalType: VitalType,
        primaryValue: Double,
        secondaryValue: Double?,
        note: String,
        unit: String = vitalType.defaultUnit
    ) = vitals.addVitalReading(userId, vitalType, primaryValue, secondaryValue, note, unit)

    suspend fun deleteVitalReading(userId: String, vitalId: String) =
        vitals.deleteVitalReading(userId, vitalId)

    suspend fun createMentalAssessment(
        userId: String,
        type: MentalAssessmentType,
        totalScore: Int,
        severityBand: MentalSeverityBand,
        itemScores: List<Int>,
        crisisFlagged: Boolean
    ) = mentalAndBody.createMentalAssessment(
        userId, type, totalScore, severityBand, itemScores, crisisFlagged
    )

    suspend fun updateMentalAssessmentReflection(
        userId: String,
        assessmentId: String,
        aiReflection: String,
        aiCopingSteps: List<String>
    ) = mentalAndBody.updateMentalAssessmentReflection(
        userId, assessmentId, aiReflection, aiCopingSteps
    )

    suspend fun deleteMentalAssessment(userId: String, assessmentId: String) =
        mentalAndBody.deleteMentalAssessment(userId, assessmentId)

    suspend fun addBodyMeasurement(
        userId: String,
        weightKg: Double,
        heightCm: Double,
        waistCm: Double,
        note: String
    ) = mentalAndBody.addBodyMeasurement(userId, weightKg, heightCm, waistCm, note)

    suspend fun deleteBodyMeasurement(userId: String, measurementId: String) =
        mentalAndBody.deleteBodyMeasurement(userId, measurementId)

    suspend fun saveCycleLog(
        userId: String,
        dateKey: String,
        flowLevel: CycleFlowLevel,
        symptoms: List<String>,
        moodTag: String,
        notes: String
    ) = mentalAndBody.saveCycleLog(userId, dateKey, flowLevel, symptoms, moodTag, notes)

    suspend fun deleteCycleLog(userId: String, dateKey: String) =
        mentalAndBody.deleteCycleLog(userId, dateKey)

    suspend fun submitAiSafetyReport(
        userId: String,
        featureArea: AiFeatureArea,
        reason: AiSafetyReportReason,
        contentSnippet: String,
        userNote: String
    ) = mentalAndBody.submitAiSafetyReport(userId, featureArea, reason, contentSnippet, userNote)

    suspend fun fetchCompleteArchive(userId: String) = portability.fetchCompleteArchive(userId)

    suspend fun deleteAllUserData(userId: String) = portability.deleteAllUserData(userId)
}
