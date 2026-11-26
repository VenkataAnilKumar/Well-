package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareTask
import com.example.data.model.Consultation
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MentalAssessment
import com.example.data.model.VitalReading
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Full snapshot of a user's cloud vault fetched on demand for GDPR/HIPAA JSON data portability,
 * ensuring records older than the 30-item live-head UI window are always included.
 */
data class FullUserHealthArchive(
    val profile: HealthProfile?,
    val consultations: List<Consultation>,
    val checkIns: List<DailyCheckIn>,
    val careTasks: List<CareTask>,
    val medications: List<MedicationItem>,
    val medicationLogs: List<MedicationLog>,
    val vitals: List<VitalReading>,
    val mentalAssessments: List<MentalAssessment>,
    val bodyMeasurements: List<BodyMeasurement>,
    val cycleLogs: List<CycleLog>,
    val labBiomarkers: List<LabBiomarker> = emptyList()
)

class AccountDataPortabilityRepository(
    private val session: FirestoreSessionHelper
) {
    suspend fun fetchCompleteArchive(userId: String): Result<FullUserHealthArchive> {
        val path = "users/$userId"
        return try {
            val uid = session.requireUserId(userId)
            val userDoc = session.userDoc(uid)
            val behavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

            val profile = userDoc.collection("profile").document("main").get().await()
                .takeIf { it.exists() }
                ?.toObject(HealthProfile::class.java, behavior)

            val consultations = userDoc.collection("consultations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(Consultation::class.java, behavior)

            val checkIns = userDoc.collection("checkIns")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(DailyCheckIn::class.java, behavior)

            val careTasks = userDoc.collection("careTasks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(CareTask::class.java, behavior)

            val medications = userDoc.collection("medications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(MedicationItem::class.java, behavior)

            val medicationLogs = userDoc.collection("medicationLogs")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(MedicationLog::class.java, behavior)

            val vitals = userDoc.collection("vitals")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(VitalReading::class.java, behavior)

            val mentalAssessments = userDoc.collection("mentalAssessments")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(MentalAssessment::class.java, behavior)

            val bodyMeasurements = userDoc.collection("bodyMeasurements")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(BodyMeasurement::class.java, behavior)

            val cycleLogs = userDoc.collection("cycleLogs")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(CycleLog::class.java, behavior)

            val labBiomarkers = userDoc.collection("labBiomarkers")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(LabBiomarker::class.java, behavior)

            Result.success(
                FullUserHealthArchive(
                    profile = profile,
                    consultations = consultations,
                    checkIns = checkIns,
                    careTasks = careTasks,
                    medications = medications,
                    medicationLogs = medicationLogs,
                    vitals = vitals,
                    mentalAssessments = mentalAssessments,
                    bodyMeasurements = bodyMeasurements,
                    cycleLogs = cycleLogs,
                    labBiomarkers = labBiomarkers
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes all user-owned Firestore documents across all collections & subcollections.
     */
    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = session.requireUserId(userId)
            val userDoc = session.userDoc(uid)
            val refsToDelete = mutableListOf<DocumentReference>()

            val consultsCol = userDoc.collection("consultations")
            val consultsSnap = consultsCol.get().await()
            for (consultDoc in consultsSnap.documents) {
                val msgsSnap = consultsCol.document(consultDoc.id).collection("messages").get().await()
                msgsSnap.documents.forEach { refsToDelete.add(it.reference) }
                refsToDelete.add(consultDoc.reference)
            }

            listOf(
                userDoc.collection("profile"),
                userDoc.collection("checkIns"),
                userDoc.collection("careTasks"),
                userDoc.collection("medications"),
                userDoc.collection("medicationLogs"),
                userDoc.collection("vitals"),
                userDoc.collection("mentalAssessments"),
                userDoc.collection("bodyMeasurements"),
                userDoc.collection("cycleLogs"),
                userDoc.collection("aiSafetyReports"),
                userDoc.collection("labBiomarkers")
            ).forEach { col ->
                val snap = col.get().await()
                snap.documents.forEach { refsToDelete.add(it.reference) }
            }

            session.commitChunkedDeletes(refsToDelete)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
