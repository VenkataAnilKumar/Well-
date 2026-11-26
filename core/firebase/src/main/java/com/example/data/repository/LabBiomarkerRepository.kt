package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ExtractedBiomarkerDraft
import com.example.data.model.LabBiomarker
import com.example.data.model.LabBiomarkerCategory
import com.example.data.model.LabBiomarkerStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LabBiomarkerRepository(
    private val session: FirestoreSessionHelper
) {
    private fun biomarkersCol(uid: String) = session.userDoc(uid).collection("labBiomarkers")

    fun observeLabBiomarkers(userId: String): Flow<List<LabBiomarker>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/labBiomarkers"
        emitAll(
            biomarkersCol(uid)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        LabBiomarker::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addLabBiomarker(
        userId: String,
        markerName: String,
        category: LabBiomarkerCategory,
        value: Double,
        unit: String,
        referenceRangeText: String,
        status: LabBiomarkerStatus,
        panelDateKey: String,
        labNote: String
    ): Result<Unit> = addLabBiomarkersBatch(
        userId = userId,
        panelDateKey = panelDateKey,
        drafts = listOf(
            ExtractedBiomarkerDraft(
                markerName = markerName,
                category = category,
                value = value,
                unit = unit,
                referenceRangeText = referenceRangeText,
                status = status,
                labNote = labNote,
                selected = true
            )
        )
    )

    suspend fun addLabBiomarkersBatch(
        userId: String,
        panelDateKey: String,
        drafts: List<ExtractedBiomarkerDraft>
    ): Result<Unit> {
        val path = "users/$userId/labBiomarkers"
        return try {
            val uid = session.requireUserId(userId)
            val chosen = drafts.filter { it.selected }
            require(chosen.isNotEmpty()) { "Select at least one biomarker to save." }
            val fallbackDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val safeDateKey = panelDateKey.trim()
                .takeIf { it.length in 8..16 }
                ?: fallbackDate

            val batch = session.db.batch()
            val col = biomarkersCol(uid)
            chosen.take(25).forEach { draft ->
                val cleanName = draft.markerName.trim().take(80)
                require(cleanName.isNotEmpty()) { "Biomarker name cannot be blank." }
                require(draft.value in 0.0..100000.0) { "Biomarker value out of supported bounds." }
                val cleanUnit = draft.unit.trim().ifBlank { "units" }.take(24)
                val docRef = col.document()
                val payload = mapOf<String, Any>(
                    "userId" to uid,
                    "markerName" to cleanName,
                    "category" to draft.category.name,
                    "value" to draft.value,
                    "unit" to cleanUnit,
                    "referenceRangeText" to draft.referenceRangeText.trim().take(80),
                    "status" to draft.status.name,
                    "panelDateKey" to safeDateKey,
                    "labNote" to draft.labNote.trim().take(400),
                    "recordedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLabBiomarker(userId: String, biomarkerId: String): Result<Unit> {
        val path = "users/$userId/labBiomarkers/$biomarkerId"
        return try {
            val uid = session.requireUserId(userId)
            require(biomarkerId.isNotBlank()) { "Biomarker ID cannot be blank." }
            biomarkersCol(uid).document(biomarkerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
