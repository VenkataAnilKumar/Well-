package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Consultation
import com.example.data.model.ConsultationType
import com.example.data.model.FollowUpStatus
import com.example.data.model.MessageRole
import com.example.data.model.StructuredTriageResult
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

class ConsultationRepository(
    private val session: FirestoreSessionHelper
) {
    private fun consultationsCol(uid: String) =
        session.userDoc(uid).collection("consultations")

    private fun messagesCol(uid: String, consultationId: String) =
        consultationsCol(uid).document(consultationId).collection("messages")

    /**
     * Streams the newest [pageSize] consultations in real time (Live-Head) together with the tail
     * [DocumentSnapshot] cursor for on-demand pagination of earlier history.
     */
    fun observeConsultationsHead(
        userId: String,
        pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE
    ): Flow<LiveHeadPage<Consultation>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/consultations"
        emitAll(
            consultationsCol(uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(pageSize)
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.documents
                    val items = snapshot.toObjects(
                        Consultation::class.java,
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
     * Fetches the next page of older consultations strictly after [startAfter].
     */
    suspend fun fetchOlderConsultations(
        userId: String,
        startAfter: DocumentSnapshot,
        pageSize: Long = DEFAULT_HISTORY_PAGE_SIZE
    ): Result<PagedResult<Consultation>> {
        val path = "users/$userId/consultations"
        return try {
            val uid = session.requireUserId(userId)
            val snap = consultationsCol(uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .startAfter(startAfter)
                .limit(pageSize)
                .get()
                .await()
            val docs = snap.documents
            val items = snap.toObjects(
                Consultation::class.java,
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

    suspend fun getConsultation(userId: String, consultationId: String): Result<Consultation?> {
        val path = "users/$userId/consultations/$consultationId"
        return try {
            val uid = session.requireUserId(userId)
            require(consultationId.isNotBlank()) { "Consultation ID cannot be blank" }
            val snap = consultationsCol(uid).document(consultationId).get().await()
            val item = snap.takeIf { it.exists() }?.toObject(
                Consultation::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeMessages(userId: String, consultationId: String): Flow<List<ChatMessage>> = flow {
        val uid = session.requireUserId(userId)
        val path = "users/$uid/consultations/$consultationId/messages"
        emitAll(
            messagesCol(uid, consultationId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    session.logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

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
    ): Result<String> {
        var path = "users/$userId/consultations"
        return try {
            val uid = session.requireUserId(userId)
            val docRef = consultationsCol(uid).document()
            path = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "consultationType" to type.name,
                "title" to analysis.title.trim().ifBlank { "Health Consultation" }.take(140),
                "bodyArea" to bodyArea.trim().ifBlank { "General" }.take(60),
                "symptomSummary" to symptomSummary.trim().ifBlank { "Photo & symptom review" }.take(2000),
                "severity" to severity.coerceIn(1L, 10L),
                "duration" to duration.trim().ifBlank { "Recent" }.take(60),
                "associatedSymptoms" to session.sanitizeList(associatedSymptoms, 20),
                "hadPhotoAttachment" to hadPhotoAttachment,
                "urgencyLevel" to analysis.urgencyLevel.name,
                "aiSummary" to analysis.aiSummary.trim().ifBlank { "Review complete." }.take(4000),
                "possibleCauses" to session.sanitizeList(analysis.possibleCauses, 12),
                "careSuggestions" to session.sanitizeList(analysis.careSuggestions, 15),
                "redFlags" to session.sanitizeList(analysis.redFlags, 15),
                "doctorQuestions" to session.sanitizeList(analysis.doctorQuestions, 12),
                "followUpStatus" to FollowUpStatus.PENDING.name,
                "followUpNote" to "",
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

    suspend fun updateConsultationFollowUp(
        userId: String,
        consultationId: String,
        status: FollowUpStatus,
        note: String
    ): Result<Unit> {
        val path = "users/$userId/consultations/$consultationId"
        return try {
            val uid = session.requireUserId(userId)
            require(consultationId.isNotBlank()) { "Consultation ID cannot be blank" }
            consultationsCol(uid).document(consultationId).update(
                mapOf(
                    "followUpStatus" to status.name,
                    "followUpNote" to note.trim().take(600),
                    "followUpUpdatedAt" to FieldValue.serverTimestamp(),
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

    suspend fun addChatMessage(
        userId: String,
        consultationId: String,
        role: MessageRole,
        content: String,
        hadPhotoAttachment: Boolean = false
    ): Result<Unit> {
        var path = "users/$userId/consultations/$consultationId/messages"
        return try {
            val uid = session.requireUserId(userId)
            require(consultationId.isNotBlank()) { "Consultation ID cannot be blank" }
            val safeContent = content.trim().take(8000)
            require(safeContent.isNotEmpty()) { "Message content cannot be blank" }

            val msgRef = messagesCol(uid, consultationId).document()
            path = msgRef.path
            val payload = mapOf(
                "userId" to uid,
                "consultationId" to consultationId,
                "role" to role.name,
                "content" to safeContent,
                "hadPhotoAttachment" to hadPhotoAttachment,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(payload).await()
            consultationsCol(uid).document(consultationId)
                .update(mapOf("updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteConsultation(userId: String, consultationId: String): Result<Unit> {
        val path = "users/$userId/consultations/$consultationId"
        return try {
            val uid = session.requireUserId(userId)
            require(consultationId.isNotBlank()) { "Consultation ID cannot be blank" }
            val messages = messagesCol(uid, consultationId).get().await()
            val refs = messages.documents.map { it.reference } +
                consultationsCol(uid).document(consultationId)
            session.commitChunkedDeletes(refs)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
