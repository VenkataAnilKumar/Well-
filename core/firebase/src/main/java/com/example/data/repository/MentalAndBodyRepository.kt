package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AiFeatureArea
import com.example.data.model.AiSafetyReportReason
import com.example.data.model.BodyMeasurement
import com.example.data.model.CycleFlowLevel
import com.example.data.model.CycleLog
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.MentalSeverityBand
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MentalAndBodyRepository(
    private val session: FirestoreSessionHelper
) {
    private fun mentalAssessmentsCol(uid: String) = session.userDoc(uid).collection("mentalAssessments")
    private fun bodyMeasurementsCol(uid: String) = session.userDoc(uid).collection("bodyMeasurements")
    private fun cycleLogsCol(uid: String) = session.userDoc(uid).collection("cycleLogs")
    private fun aiSafetyReportsCol(uid: String) = session.userDoc(uid).collection("aiSafetyReports")

    fun observeMentalAssessments(userId: String): Flow<List<MentalAssessment>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/mentalAssessments"
        emitAll(
            mentalAssessmentsCol(uid)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MentalAssessment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeBodyMeasurements(userId: String): Flow<List<BodyMeasurement>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/bodyMeasurements"
        emitAll(
            bodyMeasurementsCol(uid)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BodyMeasurement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCycleLogs(userId: String): Flow<List<CycleLog>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/cycleLogs"
        emitAll(
            cycleLogsCol(uid)
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(120)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CycleLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createMentalAssessment(
        userId: String,
        type: MentalAssessmentType,
        totalScore: Int,
        severityBand: MentalSeverityBand,
        itemScores: List<Int>,
        crisisFlagged: Boolean
    ): Result<String> {
        var path = "users/$userId/mentalAssessments"
        return try {
            val uid = session.requireUserId(userId)
            val safeScores = itemScores.map { it.coerceIn(0, 3).toLong() }
            require(safeScores.size in 7..9) { "Assessment requires 7 to 9 item scores." }
            val safeTotal = totalScore.coerceIn(0, type.maxScore).toLong()
            val docRef = mentalAssessmentsCol(uid).document()
            path = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "assessmentType" to type.name,
                "totalScore" to safeTotal,
                "maxScore" to type.maxScore.toLong(),
                "severityBand" to severityBand.name,
                "itemScores" to safeScores,
                "crisisFlagged" to crisisFlagged,
                "aiReflection" to "",
                "aiCopingSteps" to emptyList<String>(),
                "recordedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMentalAssessmentReflection(
        userId: String,
        assessmentId: String,
        aiReflection: String,
        aiCopingSteps: List<String>
    ): Result<Unit> {
        val path = "users/$userId/mentalAssessments/$assessmentId"
        return try {
            val uid = session.requireUserId(userId)
            require(assessmentId.isNotBlank()) { "Assessment ID cannot be blank" }
            mentalAssessmentsCol(uid).document(assessmentId).update(
                mapOf(
                    "aiReflection" to aiReflection.trim().take(2000),
                    "aiCopingSteps" to session.sanitizeList(aiCopingSteps, 8),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMentalAssessment(userId: String, assessmentId: String): Result<Unit> {
        val path = "users/$userId/mentalAssessments/$assessmentId"
        return try {
            val uid = session.requireUserId(userId)
            require(assessmentId.isNotBlank()) { "Assessment ID cannot be blank" }
            mentalAssessmentsCol(uid).document(assessmentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addBodyMeasurement(
        userId: String,
        weightKg: Double,
        heightCm: Double,
        waistCm: Double,
        note: String
    ): Result<Unit> {
        var path = "users/$userId/bodyMeasurements"
        return try {
            val uid = session.requireUserId(userId)
            require(weightKg in 10.0..400.0) { "Weight must be between 10 kg and 400 kg." }
            val safeHeight = if (heightCm > 0.0) heightCm.coerceIn(50.0, 280.0) else 0.0
            val safeWaist = if (waistCm > 0.0) waistCm.coerceIn(30.0, 300.0) else 0.0
            val computedBmi = BodyMeasurement.computeBmi(weightKg, safeHeight).coerceIn(0.0, 120.0)
            val roundedWeight = kotlin.math.round(weightKg * 10.0) / 10.0
            val roundedHeight = kotlin.math.round(safeHeight * 10.0) / 10.0
            val roundedWaist = kotlin.math.round(safeWaist * 10.0) / 10.0

            val docRef = bodyMeasurementsCol(uid).document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "weightKg" to roundedWeight,
                    "heightCm" to roundedHeight,
                    "bmi" to computedBmi,
                    "waistCm" to roundedWaist,
                    "note" to note.trim().take(300),
                    "recordedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBodyMeasurement(userId: String, measurementId: String): Result<Unit> {
        val path = "users/$userId/bodyMeasurements/$measurementId"
        return try {
            val uid = session.requireUserId(userId)
            require(measurementId.isNotBlank()) { "Measurement ID cannot be blank" }
            bodyMeasurementsCol(uid).document(measurementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCycleLog(
        userId: String,
        dateKey: String,
        flowLevel: CycleFlowLevel,
        symptoms: List<String>,
        moodTag: String,
        notes: String
    ): Result<Unit> {
        val path = "users/$userId/cycleLogs/$dateKey"
        return try {
            val uid = session.requireUserId(userId)
            require(dateKey.length in 8..16) { "Invalid date key" }
            val cleanSymptoms = session.sanitizeList(symptoms, 12)
            val safeMood = moodTag.trim().take(40)
            val safeNotes = notes.trim().take(400)

            val docRef = cycleLogsCol(uid).document(dateKey)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "flowLevel" to flowLevel.name,
                        "symptoms" to cleanSymptoms,
                        "moodTag" to safeMood,
                        "notes" to safeNotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "flowLevel" to flowLevel.name,
                        "symptoms" to cleanSymptoms,
                        "moodTag" to safeMood,
                        "notes" to safeNotes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCycleLog(userId: String, dateKey: String): Result<Unit> {
        val path = "users/$userId/cycleLogs/$dateKey"
        return try {
            val uid = session.requireUserId(userId)
            require(dateKey.isNotBlank()) { "Date key cannot be blank" }
            cycleLogsCol(uid).document(dateKey).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitAiSafetyReport(
        userId: String,
        featureArea: AiFeatureArea,
        reason: AiSafetyReportReason,
        contentSnippet: String,
        userNote: String
    ): Result<Unit> {
        var path = "users/$userId/aiSafetyReports"
        return try {
            val uid = session.requireUserId(userId)
            val docRef = aiSafetyReportsCol(uid).document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "featureArea" to featureArea.name,
                    "reason" to reason.name,
                    "contentSnippet" to contentSnippet.trim().take(400),
                    "userNote" to userNote.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
