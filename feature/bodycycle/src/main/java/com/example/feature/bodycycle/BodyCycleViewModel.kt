package com.example.feature.bodycycle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CycleAnalytics
import com.example.data.model.BodyAndCycleInsight
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareCategory
import com.example.data.model.CycleFlowLevel
import com.example.data.model.CycleLog
import com.example.data.model.HealthProfile
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.MentalAndBodyRepository
import com.example.data.repository.ai.WellnessInsightsAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.BodyCycleInsightUiState
import com.example.ui.viewmodel.CloudUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "BodyCycleVM"

class BodyCycleViewModel(
    private val session: SessionCoordinator,
    private val mentalAndBodyRepository: MentalAndBodyRepository,
    private val checkInAndCareRepository: CheckInAndCareRepository,
    private val wellnessInsightsAiService: WellnessInsightsAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val bodyMeasurementsState: StateFlow<CloudUiState<List<BodyMeasurement>>>,
    private val cycleLogsState: StateFlow<CloudUiState<List<CycleLog>>>
) : ViewModel() {

    private val _bodyCycleInsightState = MutableStateFlow<BodyCycleInsightUiState>(BodyCycleInsightUiState.Idle)
    val bodyCycleInsightState: StateFlow<BodyCycleInsightUiState> = _bodyCycleInsightState.asStateFlow()

    fun setCycleTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setCycleTrackingEnabled(enabled)
            session.showMessage(if (enabled) "Cycle Tracking enabled." else "Cycle Tracking turned off.")
        }
    }

    fun setTypicalCycleLengthDays(days: Int) {
        viewModelScope.launch {
            session.prefsStore.setTypicalCycleLengthDays(days)
        }
    }

    fun setUseMetricBodyUnits(useMetric: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setUseMetricBodyUnits(useMetric)
        }
    }

    fun addBodyMeasurement(
        weightKg: Double,
        heightCm: Double,
        waistCm: Double,
        note: String
    ) {
        viewModelScope.launch {
            val res = mentalAndBodyRepository.addBodyMeasurement(
                userId = session.currentUserId,
                weightKg = weightKg,
                heightCm = heightCm,
                waistCm = waistCm,
                note = note
            )
            res.onSuccess {
                session.showMessage("Body measurement logged.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save body measurement.")
            }
        }
    }

    fun deleteBodyMeasurement(measurementId: String) {
        viewModelScope.launch {
            val res = mentalAndBodyRepository.deleteBodyMeasurement(session.currentUserId, measurementId)
            res.onSuccess {
                session.showMessage("Measurement removed.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove measurement.")
            }
        }
    }

    fun saveCycleLog(
        dateKey: String = session.todayDateKey,
        flowLevel: CycleFlowLevel,
        symptoms: List<String>,
        moodTag: String,
        notes: String
    ) {
        viewModelScope.launch {
            val res = mentalAndBodyRepository.saveCycleLog(
                userId = session.currentUserId,
                dateKey = dateKey,
                flowLevel = flowLevel,
                symptoms = symptoms,
                moodTag = moodTag,
                notes = notes
            )
            res.onSuccess {
                session.showMessage("Cycle log saved for $dateKey.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save cycle log.")
            }
        }
    }

    fun deleteCycleLog(dateKey: String) {
        viewModelScope.launch {
            val res = mentalAndBodyRepository.deleteCycleLog(session.currentUserId, dateKey)
            res.onSuccess {
                session.showMessage("Cycle log removed.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove cycle log.")
            }
        }
    }

    fun generateBodyAndCycleInsight() {
        if (_bodyCycleInsightState.value is BodyCycleInsightUiState.Loading) return
        val measurements = (bodyMeasurementsState.value as? CloudUiState.Success)?.data.orEmpty()
        val cycleLogs = (cycleLogsState.value as? CloudUiState.Success)?.data.orEmpty()
        val settings = session.deviceSettings.value
        val profile = (profileState.value as? CloudUiState.Success)?.data

        if (measurements.isEmpty() && (!settings.cycleTrackingEnabled || cycleLogs.isEmpty())) {
            _bodyCycleInsightState.value = BodyCycleInsightUiState.Error(
                "Log at least one body measurement or cycle entry to generate AI pattern insights."
            )
            return
        }

        val prediction = if (settings.cycleTrackingEnabled && cycleLogs.isNotEmpty()) {
            CycleAnalytics.computePrediction(
                logs = cycleLogs,
                typicalCycleLengthDays = settings.typicalCycleLengthDays,
                todayDateKey = session.todayDateKey
            )
        } else null

        val activeRegion = session.activeHealthRegion.value
        val entitlements = session.entitlementProvider
        val fingerprint = entitlements.fingerprintOf(
            "body_cycle",
            activeRegion.code.code,
            measurements.size, measurements.firstOrNull()?.id,
            cycleLogs.size, cycleLogs.firstOrNull()?.dateKey,
            settings.cycleTrackingEnabled, settings.useMetricBodyUnits
        )
        entitlements.getCached<BodyAndCycleInsight>("body_cycle", fingerprint)?.let { cached ->
            _bodyCycleInsightState.value = BodyCycleInsightUiState.Success(cached)
            session.showMessage("Loaded cached body & cycle insight (0 daily AI credits used).")
            return
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _bodyCycleInsightState.value = BodyCycleInsightUiState.Error(blockReason)
                return@launch
            }
            _bodyCycleInsightState.value = BodyCycleInsightUiState.Loading
            try {
                val insight = wellnessInsightsAiService.generateBodyAndCycleInsight(
                    measurements = measurements,
                    cycleLogs = cycleLogs,
                    cyclePrediction = prediction,
                    cycleEnabled = settings.cycleTrackingEnabled,
                    useMetric = settings.useMetricBodyUnits,
                    profile = profile,
                    region = activeRegion
                )
                entitlements.putCached("body_cycle", fingerprint, insight)
                _bodyCycleInsightState.value = BodyCycleInsightUiState.Success(insight)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Body & cycle AI insight failed", e)
                _bodyCycleInsightState.value = BodyCycleInsightUiState.Error(
                    e.localizedMessage ?: "Unable to generate AI pattern insight right now."
                )
            }
        }
    }

    fun clearBodyCycleInsight() {
        _bodyCycleInsightState.value = BodyCycleInsightUiState.Idle
    }

    fun addRecommendationToChecklist(title: String, category: CareCategory) {
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
