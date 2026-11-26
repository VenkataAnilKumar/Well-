package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

const val DEFAULT_HISTORY_PAGE_SIZE: Long = 30L

/**
 * Snapshot emission for a bounded live-head query (`limit(pageSize)`), preserving the tail
 * cursor snapshot so subsequent cursor-based pages can start strictly after it.
 */
data class LiveHeadPage<T>(
    val items: List<T>,
    val lastCursor: DocumentSnapshot? = null,
    val hasMore: Boolean = false
)

/**
 * One-shot cursor page fetched via `startAfter(lastCursor).limit(pageSize)`.
 */
data class PagedResult<T>(
    val items: List<T>,
    val lastCursor: DocumentSnapshot? = null,
    val hasMore: Boolean = false
)

/**
 * Pure-Kotlin helper that merges a real-time head page with previously fetched older tail pages,
 * deduplicating by document ID so newly added or updated head items never duplicate tail entries.
 */
object PaginationMerger {
    fun <T> mergeLiveHeadAndTail(
        liveHead: List<T>,
        olderTail: List<T>,
        idSelector: (T) -> String
    ): List<T> {
        if (olderTail.isEmpty()) return liveHead
        if (liveHead.isEmpty()) return olderTail
        val seenIds = HashSet<String>(liveHead.size + olderTail.size)
        val merged = ArrayList<T>(liveHead.size + olderTail.size)
        for (item in liveHead) {
            val id = idSelector(item)
            if (id.isBlank() || seenIds.add(id)) {
                merged.add(item)
            }
        }
        for (item in olderTail) {
            val id = idSelector(item)
            if (id.isBlank() || seenIds.add(id)) {
                merged.add(item)
            }
        }
        return merged
    }

    fun <T> appendOlderPage(
        existingTail: List<T>,
        newPage: List<T>,
        idSelector: (T) -> String
    ): List<T> = mergeLiveHeadAndTail(existingTail, newPage, idSelector)
}

/**
 * Shared Firestore session enforcement, path helpers, list sanitization, and chunked batch deletion
 * reused across all domain repositories.
 */
class FirestoreSessionHelper(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth = Firebase.auth
) {
    fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google before accessing cloud health records.")
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("Active account changed; refusing cross-user operation.")
        }
        return uid
    }

    fun userDoc(uid: String) = db.collection("users").document(uid)

    fun sanitizeList(items: List<String>, maxItems: Int): List<String> =
        items.map { it.trim().take(500) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(maxItems)

    suspend fun commitChunkedDeletes(refs: List<DocumentReference>) {
        if (refs.isEmpty()) return
        refs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
