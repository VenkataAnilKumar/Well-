package com.example.feature.mental

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CareCategory
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.MentalAndBodyRepository
import com.example.data.repository.ai.WellnessInsightsAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.MentalReflectionUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MentalWellbeingVM"

class MentalWellbeingViewModel(
    private val session: SessionCoordinator,
    private val mentalAndBodyRepository: MentalAndBodyRepository,
    private val checkInAndCareRepository: CheckInAndCareRepository,
    private val wellnessInsightsAiService: WellnessInsightsAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val checkInsState: StateFlow<CloudUiState<List<DailyCheckIn>>>
) : ViewModel() {

    private val _mentalReflectionState = MutableStateFlow<MentalReflectionUiState>(MentalReflectionUiState.Idle)
    val mentalReflectionState: StateFlow<MentalReflectionUiState> = _mentalReflectionState.asStateFlow()

    fun submitMentalAssessment(
        type: MentalAssessmentType,
        answers: List<Int>,
        onSaved: (String) -> Unit
    ) {
        val totalScore = answers.sum().coerceIn(0, type.maxScore)
        val band = type.evaluateBand(totalScore)
        val crisisFlagged = type.isCrisisTriggered(answers, totalScore)

        viewModelScope.launch {
            val createRes = mentalAndBodyRepository.createMentalAssessment(
                userId = session.currentUserId,
                type = type,
                totalScore = totalScore,
                severityBand = band,
                itemScores = answers,
                crisisFlagged = crisisFlagged
            )
            createRes.onSuccess { newId ->
                session.showMessage("${type.shortLabel} screening saved ($totalScore/${type.maxScore} — ${band.label}).")
                onSaved(newId)
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save mental wellbeing screening.")
            }
        }
    }

    fun regenerateMentalReflection(assessment: MentalAssessment) {
        if (_mentalReflectionState.value is MentalReflectionUiState.Loading) return
        val recentCheckIns = (checkInsState.value as? CloudUiState.Success)?.data.orEmpty()
        val profile = (profileState.value as? CloudUiState.Success)?.data
        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _mentalReflectionState.value = MentalReflectionUiState.Error(assessment.id, blockReason)
                return@launch
            }
            _mentalReflectionState.value = MentalReflectionUiState.Loading(assessment.id)
            try {
                val reflection = wellnessInsightsAiService.generateMentalWellbeingReflection(
                    assessment = assessment,
                    recentCheckIns = recentCheckIns,
                    profile = profile
                )
                mentalAndBodyRepository.updateMentalAssessmentReflection(
                    userId = session.currentUserId,
                    assessmentId = assessment.id,
                    aiReflection = reflection.empatheticSummary,
                    aiCopingSteps = reflection.gentleCopingSteps
                )
                _mentalReflectionState.value = MentalReflectionUiState.Success(assessment.id, reflection)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Mental wellbeing AI reflection failed", e)
                _mentalReflectionState.value = MentalReflectionUiState.Error(
                    assessmentId = assessment.id,
                    message = e.localizedMessage ?: "Unable to generate AI coping reflection right now."
                )
            }
        }
    }

    fun deleteMentalAssessment(assessmentId: String) {
        viewModelScope.launch {
            val res = mentalAndBodyRepository.deleteMentalAssessment(session.currentUserId, assessmentId)
            res.onSuccess {
                if ((_mentalReflectionState.value as? MentalReflectionUiState.Success)?.assessmentId == assessmentId) {
                    _mentalReflectionState.value = MentalReflectionUiState.Idle
                }
                session.showMessage("Wellbeing screening removed.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove screening.")
            }
        }
    }

    fun addCopingStep(title: String, category: CareCategory) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = checkInAndCareRepository.addCareTask(
                userId = session.currentUserId,
                title = title,
                category = category
            )
            result.onSuccess {
                session.showMessage("Care task added.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not add care task.")
            }
        }
    }
}
