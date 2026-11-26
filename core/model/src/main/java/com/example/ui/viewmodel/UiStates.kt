package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.data.model.BodyAndCycleInsight
import com.example.data.model.ClarifyingQuestionItem
import com.example.data.model.ConsultationType
import com.example.data.model.ExtractedLabPanelResult
import com.example.data.model.HealthConnectAiBrief
import com.example.data.model.LongitudinalInsightsResult
import com.example.data.model.MedicationInteractionReport
import com.example.data.model.MentalWellbeingReflection
import com.example.data.model.ProactiveReassessmentResult
import com.example.data.model.ScannedPrescriptionDraft
import com.example.data.model.SummaryMicroInsight
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

data class PaginationUiState(
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Analyzing : AnalysisState
    data class Error(val message: String) : AnalysisState
}

sealed interface InsightsUiState {
    data object Idle : InsightsUiState
    data object Loading : InsightsUiState
    data class Success(val result: LongitudinalInsightsResult) : InsightsUiState
    data class Error(val message: String) : InsightsUiState
}

sealed interface InteractionCheckUiState {
    data object Idle : InteractionCheckUiState
    data object Loading : InteractionCheckUiState
    data class Success(val report: MedicationInteractionReport) : InteractionCheckUiState
    data class Error(val message: String) : InteractionCheckUiState
}

sealed interface PrescriptionScanUiState {
    data object Idle : PrescriptionScanUiState
    data object Scanning : PrescriptionScanUiState
    data class Ready(val draft: ScannedPrescriptionDraft) : PrescriptionScanUiState
    data class Error(val message: String) : PrescriptionScanUiState
}

sealed interface MentalReflectionUiState {
    data object Idle : MentalReflectionUiState
    data class Loading(val assessmentId: String) : MentalReflectionUiState
    data class Success(val assessmentId: String, val reflection: MentalWellbeingReflection) : MentalReflectionUiState
    data class Error(val assessmentId: String, val message: String) : MentalReflectionUiState
}

sealed interface SummaryInsightUiState {
    data object Idle : SummaryInsightUiState
    data object Loading : SummaryInsightUiState
    data class Success(val insight: SummaryMicroInsight) : SummaryInsightUiState
    data class Error(val message: String) : SummaryInsightUiState
}

sealed interface BodyCycleInsightUiState {
    data object Idle : BodyCycleInsightUiState
    data object Loading : BodyCycleInsightUiState
    data class Success(val insight: BodyAndCycleInsight) : BodyCycleInsightUiState
    data class Error(val message: String) : BodyCycleInsightUiState
}

sealed interface HealthConnectAiUiState {
    data object Idle : HealthConnectAiUiState
    data object Loading : HealthConnectAiUiState
    data class Success(val brief: HealthConnectAiBrief) : HealthConnectAiUiState
    data class Error(val message: String) : HealthConnectAiUiState
}

sealed interface LabScanUiState {
    data object Idle : LabScanUiState
    data object Scanning : LabScanUiState
    data class Success(val result: ExtractedLabPanelResult) : LabScanUiState
    data class Error(val message: String) : LabScanUiState
}

sealed interface ClarifyingQuestionsUiState {
    data object Idle : ClarifyingQuestionsUiState
    data object Loading : ClarifyingQuestionsUiState
    data class Success(val questions: List<ClarifyingQuestionItem>) : ClarifyingQuestionsUiState
    data class Error(val message: String) : ClarifyingQuestionsUiState
}

sealed interface ProactiveReassessmentUiState {
    data object Idle : ProactiveReassessmentUiState
    data object Loading : ProactiveReassessmentUiState
    data class Success(val result: ProactiveReassessmentResult) : ProactiveReassessmentUiState
    data class Error(val message: String) : ProactiveReassessmentUiState
}

enum class ConsultViewMode {
    INTAKE,
    REPORT,
    CHAT
}

data class IntakeDraft(
    val mode: ConsultationType = ConsultationType.SYMPTOM_CHECK,
    val step: Int = 0,
    val bodyArea: String = "Head & Neuro",
    val symptomDescription: String = "",
    val severity: Int = 4,
    val duration: String = "Today",
    val onsetPattern: String = "Gradual",
    val progression: String = "Staying steady",
    val triggersOrRelief: String = "",
    val associatedSymptoms: Set<String> = emptySet(),
    val clarifyingAnswers: Map<String, String> = emptyMap(),
    val photoUri: Uri? = null,
    val photoBitmap: Bitmap? = null
) {
    val hasDraftContent: Boolean
        get() = symptomDescription.isNotBlank() ||
            associatedSymptoms.isNotEmpty() ||
            triggersOrRelief.isNotBlank() ||
            clarifyingAnswers.isNotEmpty() ||
            photoUri != null

    fun buildPreparedSymptomSummary(): String {
        val base = symptomDescription.trim()
        if (mode == ConsultationType.PHOTO_ANALYSIS) {
            return base.ifBlank { "Attached visual / lab photo for AI review." }.take(2000)
        }
        val details = buildList {
            if (onsetPattern.isNotBlank()) add("Onset: $onsetPattern")
            if (progression.isNotBlank()) add("Trend: $progression")
            if (triggersOrRelief.isNotBlank()) add("Triggers/Relief: ${triggersOrRelief.trim()}")
        }
        val combined = buildString {
            append(base.ifBlank { "Visual symptom or clinical intake review." })
            if (details.isNotEmpty()) {
                append("\n\nClinical Context — ")
                append(details.joinToString(" • "))
            }
            val answered = clarifyingAnswers.entries.filter { it.value.isNotBlank() }
            if (answered.isNotEmpty()) {
                append("\n\nClarifying Q&A:")
                answered.forEach { (q, a) ->
                    append("\n• $q: ${a.trim()}")
                }
            }
        }
        return combined.take(2000)
    }
}

data class ChatUiState(
    val isSending: Boolean = false,
    val streamingDoctorReply: String = "",
    val errorMessage: String? = null
)

fun <T> Flow<T>.asCloudUiStateRaw(
    fallbackError: String,
    logTag: String = "WellPlusStream"
): Flow<CloudUiState<T>> =
    this.map<T, CloudUiState<T>> { CloudUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(logTag, fallbackError, rootError)
            emit(CloudUiState.Error(rootError.message ?: fallbackError))
        }

fun <T> Flow<T>.asCloudUiState(
    scope: CoroutineScope,
    fallbackError: String,
    logTag: String = "WellPlusStream"
): StateFlow<CloudUiState<T>> =
    asCloudUiStateRaw(fallbackError, logTag)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CloudUiState.Loading
        )
