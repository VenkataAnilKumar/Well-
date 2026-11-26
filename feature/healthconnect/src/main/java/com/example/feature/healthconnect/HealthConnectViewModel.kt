package com.example.feature.healthconnect

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ClinicalSafetyEngine
import com.example.core.HealthConnectManager
import com.example.core.HealthConnectSyncScheduler
import com.example.data.model.CareCategory
import com.example.data.model.HealthConnectAiBrief
import com.example.data.model.HealthConnectAvailability
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.VitalType
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.VitalRepository
import com.example.data.repository.ai.WellnessInsightsAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.HealthConnectAiUiState
import com.example.ui.viewmodel.HealthConnectUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HealthConnectVM"

class HealthConnectViewModel(
    private val appContext: Context,
    private val session: SessionCoordinator,
    private val healthConnectManager: HealthConnectManager,
    private val mutableHealthConnectState: MutableStateFlow<HealthConnectUiState>,
    private val vitalRepository: VitalRepository,
    private val checkInAndCareRepository: CheckInAndCareRepository,
    private val wellnessInsightsAiService: WellnessInsightsAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>
) : ViewModel() {

    val healthConnectState: StateFlow<HealthConnectUiState> = mutableHealthConnectState.asStateFlow()

    val backgroundHcSyncEnabled: StateFlow<Boolean> = session.deviceSettings
        .map { it.backgroundHcSyncEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = session.deviceSettings.value.backgroundHcSyncEnabled
        )

    private val _healthConnectAiState = MutableStateFlow<HealthConnectAiUiState>(HealthConnectAiUiState.Idle)
    val healthConnectAiState: StateFlow<HealthConnectAiUiState> = _healthConnectAiState.asStateFlow()

    fun setBackgroundHcSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setBackgroundHcSyncEnabled(enabled)
            HealthConnectSyncScheduler.syncPeriodicSchedule(appContext, enabled)
            session.showMessage(
                if (enabled) "Auto-sync scheduled every 6 hours for Health Connect & widgets."
                else "Background Health Connect auto-sync turned off."
            )
        }
    }

    fun refreshHealthConnectStatus(autoSyncIfGranted: Boolean = false) {
        viewModelScope.launch {
            val init = withContext(Dispatchers.IO) { healthConnectManager.initialize() }
            if (init.availability != HealthConnectAvailability.AVAILABLE || !init.isClientInitialized) {
                mutableHealthConnectState.update {
                    it.copy(
                        availability = init.availability,
                        isClientInitialized = init.isClientInitialized,
                        frameworkSourceLabel = init.frameworkSourceLabel,
                        grantedPermissions = emptySet(),
                        isSyncing = false
                    )
                }
                return@launch
            }
            val granted = healthConnectManager.getGrantedPermissions()
            mutableHealthConnectState.update {
                it.copy(
                    availability = init.availability,
                    isClientInitialized = init.isClientInitialized,
                    frameworkSourceLabel = init.frameworkSourceLabel,
                    grantedPermissions = granted
                )
            }
            if (autoSyncIfGranted && granted.intersect(HealthConnectManager.readPermissions).isNotEmpty()) {
                syncHealthConnectNow(silent = true)
            }
        }
    }

    fun onHealthConnectPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            val init = withContext(Dispatchers.IO) { healthConnectManager.initialize() }
            val matched = granted.intersect(HealthConnectManager.allPermissions)
            mutableHealthConnectState.update {
                it.copy(
                    availability = init.availability,
                    isClientInitialized = init.isClientInitialized,
                    frameworkSourceLabel = init.frameworkSourceLabel,
                    grantedPermissions = granted,
                    errorMessage = null
                )
            }
            if (matched.isNotEmpty()) {
                val readCount = granted.intersect(HealthConnectManager.readPermissions).size
                val writeCount = granted.intersect(HealthConnectManager.writePermissions).size
                session.showMessage("Health Connect permissions updated ($readCount read, $writeCount write).")
                if (readCount > 0) {
                    syncHealthConnectNow(silent = true)
                }
            } else {
                session.showMessage("No Health Connect permissions were granted.")
            }
        }
    }

    fun syncHealthConnectNow(silent: Boolean = false) {
        if (mutableHealthConnectState.value.isSyncing) return
        viewModelScope.launch {
            val init = withContext(Dispatchers.IO) { healthConnectManager.initialize() }
            if (init.availability != HealthConnectAvailability.AVAILABLE || !init.isClientInitialized) {
                mutableHealthConnectState.update {
                    it.copy(
                        availability = init.availability,
                        isClientInitialized = init.isClientInitialized,
                        frameworkSourceLabel = init.frameworkSourceLabel,
                        isSyncing = false,
                        errorMessage = if (init.availability == HealthConnectAvailability.UPDATE_REQUIRED) {
                            "Install or update Health Connect from Google Play to sync wearable data."
                        } else {
                            "Health Connect is not supported on this Android version."
                        }
                    )
                }
                return@launch
            }

            mutableHealthConnectState.update {
                it.copy(
                    availability = init.availability,
                    isClientInitialized = init.isClientInitialized,
                    frameworkSourceLabel = init.frameworkSourceLabel,
                    isSyncing = true,
                    errorMessage = null
                )
            }

            val res = healthConnectManager.syncHealthData()
            res.onSuccess { snapshot ->
                mutableHealthConnectState.update {
                    it.copy(
                        availability = init.availability,
                        isClientInitialized = true,
                        frameworkSourceLabel = init.frameworkSourceLabel,
                        grantedPermissions = snapshot.grantedPermissions,
                        isSyncing = false,
                        snapshot = snapshot,
                        errorMessage = null
                    )
                }
                if (!silent) {
                    session.showMessage(
                        if (snapshot.hasAnyMetricData) {
                            "Synced latest data from Health Connect."
                        } else {
                            "Connected to Health Connect — no recent records found yet."
                        }
                    )
                }
            }.onFailure { err ->
                mutableHealthConnectState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = err.localizedMessage ?: "Could not sync Health Connect right now."
                    )
                }
            }
        }
    }

    fun writeStepsToHealthConnect(steps: Long, durationMinutes: Long = 15L) {
        viewModelScope.launch {
            val res = healthConnectManager.writeSteps(count = steps, durationMinutes = durationMinutes)
            res.onSuccess {
                session.showMessage("Wrote $steps steps to Android Health Connect.")
                syncHealthConnectNow(silent = true)
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not write steps to Health Connect.")
            }
        }
    }

    fun writeHeartRateToHealthConnect(bpm: Long) {
        viewModelScope.launch {
            val res = healthConnectManager.writeHeartRate(bpm = bpm)
            res.onSuccess {
                session.showMessage("Wrote $bpm bpm heart rate to Android Health Connect.")
                syncHealthConnectNow(silent = true)
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not write heart rate to Health Connect.")
            }
        }
    }

    fun writeSampleDataToHealthConnect() {
        viewModelScope.launch {
            val res = healthConnectManager.writeSampleHealthData()
            res.onSuccess { summary ->
                val parts = buildList {
                    summary.stepsWritten?.let { add("$it steps") }
                    summary.heartRateBpmWritten?.let { add("$it bpm HR") }
                }
                session.showMessage("Wrote sample data (${parts.joinToString(" & ")}) to Health Connect.")
                syncHealthConnectNow(silent = true)
            }.onFailure { err ->
                session.showMessage(
                    err.localizedMessage ?: "Could not write sample health data to Health Connect."
                )
            }
        }
    }

    fun importHealthConnectHeartRateToVitals() {
        val hr = mutableHealthConnectState.value.snapshot?.heartRate ?: return
        importVitalFromHealthConnect(
            vitalType = VitalType.HEART_RATE,
            primaryValue = hr.latestBpm.toDouble(),
            secondaryValue = null
        )
    }

    fun importHealthConnectBloodPressureToVitals() {
        val bp = mutableHealthConnectState.value.snapshot?.latestBloodPressure ?: return
        importVitalFromHealthConnect(
            vitalType = VitalType.BLOOD_PRESSURE,
            primaryValue = bp.systolicMmHg,
            secondaryValue = bp.diastolicMmHg
        )
    }

    private fun importVitalFromHealthConnect(
        vitalType: VitalType,
        primaryValue: Double,
        secondaryValue: Double?
    ) {
        val activeRegion = session.activeHealthRegion.value
        val resolvedUnit = vitalType.unitFor(activeRegion)
        viewModelScope.launch {
            val res = vitalRepository.addVitalReading(
                userId = session.currentUserId,
                vitalType = vitalType,
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                note = "Synced from Android Health Connect",
                unit = resolvedUnit
            )
            res.onSuccess {
                val formatted = vitalType.formatValueForUnit(primaryValue, secondaryValue, resolvedUnit)
                session.showMessage("${vitalType.label} logged ($formatted $resolvedUnit).")
                ClinicalSafetyEngine.evaluateVital(
                    vitalType = vitalType,
                    primaryValue = primaryValue,
                    secondaryValue = secondaryValue,
                    unit = resolvedUnit,
                    region = activeRegion
                )?.let { criticalAlert ->
                    session.emitCriticalVitalAlert(criticalAlert)
                }
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not log vital reading.")
            }
        }
    }

    fun generateHealthConnectAiBrief() {
        if (_healthConnectAiState.value is HealthConnectAiUiState.Loading) return
        val snapshot = mutableHealthConnectState.value.snapshot
        if (snapshot == null || !snapshot.hasAnyMetricData) {
            _healthConnectAiState.value = HealthConnectAiUiState.Error(
                "Sync at least one metric (steps, heart rate, sleep, or blood pressure) from Health Connect first."
            )
            return
        }
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeMeds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val activeRegion = session.activeHealthRegion.value
        val entitlements = session.entitlementProvider

        val fingerprint = entitlements.fingerprintOf(
            "hc_brief",
            activeRegion.code.code,
            snapshot.stepsToday,
            snapshot.heartRate?.latestBpm,
            snapshot.heartRate?.sampleCount,
            snapshot.sleep?.lastNightHours,
            snapshot.latestBloodPressure?.formattedValue
        )
        entitlements.getCached<HealthConnectAiBrief>("hc_brief", fingerprint)?.let { cached ->
            _healthConnectAiState.value = HealthConnectAiUiState.Success(cached)
            session.showMessage("Loaded cached wearable brief (0 daily AI credits used).")
            return
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _healthConnectAiState.value = HealthConnectAiUiState.Error(blockReason)
                return@launch
            }
            _healthConnectAiState.value = HealthConnectAiUiState.Loading
            try {
                val brief = wellnessInsightsAiService.generateHealthConnectBrief(
                    snapshot = snapshot,
                    profile = profile,
                    activeMedications = activeMeds,
                    region = activeRegion
                )
                entitlements.putCached("hc_brief", fingerprint, brief)
                _healthConnectAiState.value = HealthConnectAiUiState.Success(brief)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Health Connect AI brief failed", e)
                _healthConnectAiState.value = HealthConnectAiUiState.Error(
                    e.localizedMessage ?: "Unable to generate AI wearable brief right now."
                )
            }
        }
    }

    fun addTipToChecklist(title: String, category: CareCategory) {
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
