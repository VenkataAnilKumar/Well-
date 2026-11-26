package com.example.feature.today

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CareCategory
import com.example.data.model.CareTask
import com.example.data.model.HealthHighlightItem
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.PhysicalActivityLevel
import com.example.data.model.SummaryMicroInsight
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.ai.WellnessInsightsAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.SummaryInsightUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "TodayVM"

class TodayViewModel(
    private val session: SessionCoordinator,
    private val checkInAndCareRepository: CheckInAndCareRepository,
    private val wellnessInsightsAiService: WellnessInsightsAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>
) : ViewModel() {

    private val _isSavingCheckIn = MutableStateFlow(false)
    val isSavingCheckIn: StateFlow<Boolean> = _isSavingCheckIn.asStateFlow()

    private val _summaryInsightState = MutableStateFlow<SummaryInsightUiState>(SummaryInsightUiState.Idle)
    val summaryInsightState: StateFlow<SummaryInsightUiState> = _summaryInsightState.asStateFlow()

    fun saveTodayCheckIn(
        moodLevel: Int,
        energyLevel: Int,
        sleepHours: Double,
        hydrationGlasses: Int,
        notes: String,
        activityLevel: PhysicalActivityLevel = PhysicalActivityLevel.MODERATE,
        activityMinutes: Int = activityLevel.defaultMinutes,
        onSaved: () -> Unit = {}
    ) {
        if (_isSavingCheckIn.value) return
        viewModelScope.launch {
            _isSavingCheckIn.value = true
            try {
                val result = checkInAndCareRepository.saveDailyCheckIn(
                    userId = session.currentUserId,
                    dateKey = session.todayDateKey,
                    moodLevel = moodLevel.toLong(),
                    energyLevel = energyLevel.toLong(),
                    sleepHours = sleepHours,
                    hydrationGlasses = hydrationGlasses.toLong(),
                    notes = notes,
                    activityLevel = activityLevel,
                    activityMinutes = activityMinutes.toLong()
                )
                result.onSuccess {
                    session.showMessage("Today's wellness check-in saved to your cloud journal.")
                    onSaved()
                }.onFailure { err ->
                    session.showMessage(err.localizedMessage ?: "Could not save check-in.")
                }
            } finally {
                _isSavingCheckIn.value = false
            }
        }
    }

    fun addCustomCareTask(title: String, category: CareCategory) {
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

    fun toggleCareTask(task: CareTask) {
        viewModelScope.launch {
            val result = checkInAndCareRepository.toggleCareTask(
                userId = session.currentUserId,
                taskId = task.id,
                completed = !task.completed
            )
            result.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not update task.")
            }
        }
    }

    fun deleteCareTask(taskId: String) {
        viewModelScope.launch {
            val result = checkInAndCareRepository.deleteCareTask(session.currentUserId, taskId)
            result.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove task.")
            }
        }
    }

    fun generateSummaryMicroInsight(highlights: List<HealthHighlightItem>) {
        if (_summaryInsightState.value is SummaryInsightUiState.Loading) return
        if (highlights.isEmpty()) {
            _summaryInsightState.value = SummaryInsightUiState.Error(
                "Log at least one check-in, vital sign, or medication dose to unlock your AI trend micro-insight."
            )
            return
        }
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeMeds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val activeRegion = session.activeHealthRegion.value
        val entitlements = session.entitlementProvider

        val fingerprint = entitlements.fingerprintOf(
            "summary_micro",
            activeRegion.code.code,
            highlights.map { "${it.id}:${it.currentValueLabel}:${it.deltaBadgeText}" }
        )
        entitlements.getCached<SummaryMicroInsight>("summary_micro", fingerprint)?.let { cached ->
            _summaryInsightState.value = SummaryInsightUiState.Success(cached)
            session.showMessage("Loaded cached trend insight (0 daily AI credits used).")
            return
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _summaryInsightState.value = SummaryInsightUiState.Error(blockReason)
                return@launch
            }
            _summaryInsightState.value = SummaryInsightUiState.Loading
            try {
                val insight = wellnessInsightsAiService.generateSummaryMicroInsight(
                    highlights = highlights,
                    profile = profile,
                    activeMedications = activeMeds,
                    region = activeRegion
                )
                entitlements.putCached("summary_micro", fingerprint, insight)
                _summaryInsightState.value = SummaryInsightUiState.Success(insight)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Summary micro-insight failed", e)
                _summaryInsightState.value = SummaryInsightUiState.Error(
                    e.localizedMessage ?: "Unable to generate AI micro-insight right now."
                )
            }
        }
    }

    fun clearSummaryMicroInsight() {
        _summaryInsightState.value = SummaryInsightUiState.Idle
    }
}
