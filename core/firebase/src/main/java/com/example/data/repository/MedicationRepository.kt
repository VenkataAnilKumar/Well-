package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
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

class MedicationRepository(
    private val session: FirestoreSessionHelper
) {
    private fun medicationsCol(uid: String) = session.userDoc(uid).collection("medications")
    private fun medicationLogsCol(uid: String) = session.userDoc(uid).collection("medicationLogs")

    fun observeMedications(userId: String): Flow<List<MedicationItem>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/medications"
        emitAll(
            medicationsCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MedicationItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMedicationLogs(userId: String): Flow<List<MedicationLog>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/medicationLogs"
        emitAll(
            medicationLogsCol(uid)
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MedicationLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

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
    ): Result<Unit> {
        var path = "users/$userId/medications"
        return try {
            val uid = session.requireUserId(userId)
            val safeName = name.trim().take(100)
            require(safeName.isNotEmpty()) { "Medication name cannot be blank" }
            val safeDosage = dosage.trim().take(60)
            val safeHour = reminderHour.coerceIn(0, 23).toLong()
            val safeMinute = reminderMinute.coerceIn(0, 59).toLong()
            val safeInstructions = instructions.trim().take(240)

            if (!existingId.isNullOrBlank()) {
                val docRef = medicationsCol(uid).document(existingId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to safeName,
                        "dosage" to safeDosage,
                        "scheduleSlot" to scheduleSlot.name,
                        "reminderHour" to safeHour,
                        "reminderMinute" to safeMinute,
                        "instructions" to safeInstructions,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = medicationsCol(uid).document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to safeName,
                        "dosage" to safeDosage,
                        "scheduleSlot" to scheduleSlot.name,
                        "reminderHour" to safeHour,
                        "reminderMinute" to safeMinute,
                        "instructions" to safeInstructions,
                        "active" to active,
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

    suspend fun setMedicationActive(
        userId: String,
        medicationId: String,
        active: Boolean
    ): Result<Unit> {
        val path = "users/$userId/medications/$medicationId"
        return try {
            val uid = session.requireUserId(userId)
            require(medicationId.isNotBlank()) { "Medication ID cannot be blank" }
            medicationsCol(uid).document(medicationId).update(
                mapOf(
                    "active" to active,
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

    suspend fun deleteMedication(userId: String, medicationId: String): Result<Unit> {
        val path = "users/$userId/medications/$medicationId"
        return try {
            val uid = session.requireUserId(userId)
            require(medicationId.isNotBlank()) { "Medication ID cannot be blank" }
            medicationsCol(uid).document(medicationId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun logMedicationDose(
        userId: String,
        dateKey: String,
        medication: MedicationItem,
        status: MedicationLogStatus
    ): Result<Unit> = logMedicationDoseById(
        userId = userId,
        dateKey = dateKey,
        medicationId = medication.id,
        medicationName = medication.name,
        status = status
    )

    /**
     * Rules-compliant upsert preserving `createdAt` on existing logs. Shared by both the
     * interactive Medications UI and the background notification Mark Taken worker.
     */
    suspend fun logMedicationDoseById(
        userId: String,
        dateKey: String,
        medicationId: String,
        medicationName: String,
        status: MedicationLogStatus
    ): Result<Unit> {
        val logId = buildMedicationLogId(dateKey, medicationId)
        val path = "users/$userId/medicationLogs/$logId"
        return try {
            val uid = session.requireUserId(userId)
            require(dateKey.length in 8..16) { "Invalid date key" }
            require(medicationId.isNotBlank()) { "Medication ID cannot be blank" }
            val safeName = medicationName.trim().ifBlank { "Medication" }.take(100)

            val docRef = medicationLogsCol(uid).document(logId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "medicationName" to safeName,
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "medicationId" to medicationId.take(128),
                        "medicationName" to safeName,
                        "status" to status.name,
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

    companion object {
        fun buildMedicationLogId(dateKey: String, medicationId: String): String {
            val safeMedId = medicationId
                .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                .take(80)
            return "${dateKey}_$safeMedId".take(128)
        }
    }
}
