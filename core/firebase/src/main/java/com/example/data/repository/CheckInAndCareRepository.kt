package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.CareCategory
import com.example.data.model.CareTask
import com.example.data.model.DailyCheckIn
import com.example.data.model.PhysicalActivityLevel
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

class CheckInAndCareRepository(
    private val session: FirestoreSessionHelper
) {
    private fun checkInsCol(uid: String) = session.userDoc(uid).collection("checkIns")
    private fun careTasksCol(uid: String) = session.userDoc(uid).collection("careTasks")

    fun observeCheckIns(userId: String): Flow<List<DailyCheckIn>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/checkIns"
        emitAll(
            checkInsCol(uid)
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(31)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyCheckIn::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCareTasks(userId: String): Flow<List<CareTask>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/careTasks"
        emitAll(
            careTasksCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CareTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

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
    ): Result<Unit> {
        val path = "users/$userId/checkIns/$dateKey"
        return try {
            val uid = session.requireUserId(userId)
            require(dateKey.length in 8..16) { "Invalid date key" }
            val safeMood = moodLevel.coerceIn(1L, 5L)
            val safeEnergy = energyLevel.coerceIn(1L, 5L)
            val safeSleep = sleepHours.coerceIn(0.0, 24.0)
            val safeHydration = hydrationGlasses.coerceIn(0L, 30L)
            val safeActivityMinutes = activityMinutes.coerceIn(0L, 600L)
            val safeNotes = notes.trim().take(600)

            val docRef = checkInsCol(uid).document(dateKey)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "moodLevel" to safeMood,
                        "energyLevel" to safeEnergy,
                        "sleepHours" to safeSleep,
                        "hydrationGlasses" to safeHydration,
                        "activityLevel" to activityLevel.name,
                        "activityMinutes" to safeActivityMinutes,
                        "notes" to safeNotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "moodLevel" to safeMood,
                        "energyLevel" to safeEnergy,
                        "sleepHours" to safeSleep,
                        "hydrationGlasses" to safeHydration,
                        "activityLevel" to activityLevel.name,
                        "activityMinutes" to safeActivityMinutes,
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

    suspend fun addCareTask(
        userId: String,
        title: String,
        category: CareCategory,
        sourceConsultationId: String? = null
    ): Result<Unit> {
        var path = "users/$userId/careTasks"
        return try {
            val uid = session.requireUserId(userId)
            val safeTitle = title.trim().take(200)
            require(safeTitle.isNotEmpty()) { "Task title cannot be blank" }
            val docRef = careTasksCol(uid).document()
            path = docRef.path

            val rawPayload = mapOf<String, Any?>(
                "userId" to uid,
                "title" to safeTitle,
                "category" to category.name,
                "completed" to false,
                "sourceConsultationId" to sourceConsultationId?.takeIf { it.isNotBlank() }?.take(128),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(rawPayload.filterValues { it != null }).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleCareTask(
        userId: String,
        taskId: String,
        completed: Boolean
    ): Result<Unit> {
        val path = "users/$userId/careTasks/$taskId"
        return try {
            val uid = session.requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID cannot be blank" }
            careTasksCol(uid).document(taskId).update(
                mapOf(
                    "completed" to completed,
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

    suspend fun deleteCareTask(userId: String, taskId: String): Result<Unit> {
        val path = "users/$userId/careTasks/$taskId"
        return try {
            val uid = session.requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID cannot be blank" }
            careTasksCol(uid).document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
