package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
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

class VitalRepository(
    private val session: FirestoreSessionHelper
) {
    private fun vitalsCol(uid: String) = session.userDoc(uid).collection("vitals")

    /**
     * Streams the newest [pageSize] vital readings in real time (Live-Head) together with the tail
     * [DocumentSnapshot] cursor for on-demand pagination of earlier history.
     */
    fun observeVitalsHead(
        userId: String,
        pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE
    ): Flow<LiveHeadPage<VitalReading>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/vitals"
        emitAll(
            vitalsCol(uid)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(pageSize)
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.documents
                    val items = snapshot.toObjects(
                        VitalReading::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    LiveHeadPage(
                        items = items,
                        lastCursor = docs.lastOrNull(),
                        hasMore = docs.size >= pageSize.toInt()
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    /**
     * Fetches the next page of older vital readings strictly after [startAfter].
     */
    suspend fun fetchOlderVitals(
        userId: String,
        startAfter: DocumentSnapshot,
        pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE
    ): Result<PagedResult<VitalReading>> {
        val path = "users/$userId/vitals"
        return try {
            val uid = session.requireUserId(userId)
            val snap = vitalsCol(uid)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .startAfter(startAfter)
                .limit(pageSize)
                .get()
                .await()
            val docs = snap.documents
            val items = snap.toObjects(
                VitalReading::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(
                PagedResult(
                    items = items,
                    lastCursor = docs.lastOrNull(),
                    hasMore = docs.size >= pageSize.toInt()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addVitalReading(
        userId: String,
        vitalType: VitalType,
        primaryValue: Double,
        secondaryValue: Double?,
        note: String,
        unit: String = vitalType.defaultUnit
    ): Result<Unit> {
        var path = "users/$userId/vitals"
        return try {
            val uid = session.requireUserId(userId)
            require(primaryValue > 0.0 && primaryValue <= 1000.0) {
                "Primary reading is outside supported physiological bounds."
            }
            if (secondaryValue != null) {
                require(secondaryValue > 0.0 && secondaryValue <= 500.0) {
                    "Secondary reading is outside supported physiological bounds."
                }
            }
            val safeUnit = unit.trim().ifBlank { vitalType.defaultUnit }.take(20)
            val docRef = vitalsCol(uid).document()
            path = docRef.path

            val rawPayload = mapOf<String, Any?>(
                "userId" to uid,
                "vitalType" to vitalType.name,
                "primaryValue" to primaryValue,
                "secondaryValue" to if (vitalType.hasSecondaryValue) secondaryValue else null,
                "unit" to safeUnit,
                "note" to note.trim().take(300),
                "recordedAt" to FieldValue.serverTimestamp(),
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

    suspend fun deleteVitalReading(userId: String, vitalId: String): Result<Unit> {
        val path = "users/$userId/vitals/$vitalId"
        return try {
            val uid = session.requireUserId(userId)
            require(vitalId.isNotBlank()) { "Vital ID cannot be blank" }
            vitalsCol(uid).document(vitalId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
