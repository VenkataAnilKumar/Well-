package com.example.feature.vitals

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ClinicalSafetyEngine
import com.example.core.HealthConnectManager
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.LabBiomarkerCategory
import com.example.data.model.LabBiomarkerStatus
import com.example.data.model.MedicationItem
import com.example.data.model.UrgencyLevel
import com.example.data.model.VitalRangeStatus
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import com.example.data.repository.DEFAULT_HISTORY_PAGE_SIZE
import com.example.data.repository.LabBiomarkerRepository
import com.example.data.repository.LiveHeadPage
import com.example.data.repository.PaginationMerger
import com.example.data.repository.VitalRepository
import com.example.data.repository.ai.LabAndReassessmentAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.HealthConnectUiState
import com.example.ui.viewmodel.LabScanUiState
import com.example.ui.viewmodel.PaginationUiState
import com.example.ui.viewmodel.ProactiveReassessmentUiState
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "VitalsVM"

class VitalsViewModel(
    private val session: SessionCoordinator,
    private val vitalRepository: VitalRepository,
    private val labBiomarkerRepository: LabBiomarkerRepository,
    private val labAndReassessmentAiService: LabAndReassessmentAiService,
    private val healthConnectManager: HealthConnectManager,
    private val healthConnectState: StateFlow<HealthConnectUiState>,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>,
    private val vitalsHeadState: StateFlow<CloudUiState<LiveHeadPage<VitalReading>>>,
    val labBiomarkersState: StateFlow<CloudUiState<List<LabBiomarker>>>
) : ViewModel() {

    private val _olderVitals = MutableStateFlow<List<VitalReading>>(emptyList())
    private val _olderCursor = MutableStateFlow<DocumentSnapshot?>(null)
    private val _olderHasMore = MutableStateFlow<Boolean?>(null)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _paginationError = MutableStateFlow<String?>(null)

    private val _labScanState = MutableStateFlow<LabScanUiState>(LabScanUiState.Idle)
    val labScanState: StateFlow<LabScanUiState> = _labScanState.asStateFlow()

    private val _isSavingLabBatch = MutableStateFlow(false)
    val isSavingLabBatch: StateFlow<Boolean> = _isSavingLabBatch.asStateFlow()

    private val _abnormalVitalForReassessment = MutableStateFlow<VitalReading?>(null)
    val abnormalVitalForReassessment: StateFlow<VitalReading?> = _abnormalVitalForReassessment.asStateFlow()

    private val _vitalReassessmentState =
        MutableStateFlow<ProactiveReassessmentUiState>(ProactiveReassessmentUiState.Idle)
    val vitalReassessmentState: StateFlow<ProactiveReassessmentUiState> = _vitalReassessmentState.asStateFlow()

    val vitalsState: StateFlow<CloudUiState<List<VitalReading>>> =
        combine(vitalsHeadState, _olderVitals) { headUiState, older ->
            when (headUiState) {
                is CloudUiState.Loading -> CloudUiState.Loading
                is CloudUiState.Error -> CloudUiState.Error(headUiState.message)
                is CloudUiState.Success -> CloudUiState.Success(
                    PaginationMerger.mergeLiveHeadAndTail(
                        liveHead = headUiState.data.items,
                        olderTail = older,
                        idSelector = { it.id }
                    )
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CloudUiState.Loading
        )

    val paginationState: StateFlow<PaginationUiState> =
        combine(
            vitalsHeadState,
            _olderHasMore,
            _isLoadingMore,
            _paginationError
        ) { headUiState, olderHasMore, loadingMore, err ->
            val headPage = (headUiState as? CloudUiState.Success)?.data
            val hasMore = olderHasMore ?: (headPage?.hasMore == true)
            PaginationUiState(
                hasMore = hasMore && (headPage?.lastCursor != null || _olderCursor.value != null),
                isLoadingMore = loadingMore,
                errorMessage = err
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = PaginationUiState()
        )

    fun loadMoreVitals() {
        if (_isLoadingMore.value) return
        val headPage = (vitalsHeadState.value as? CloudUiState.Success)?.data ?: return
        val cursor = _olderCursor.value ?: headPage.lastCursor ?: return
        val canLoadMore = _olderHasMore.value ?: headPage.hasMore
        if (!canLoadMore) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            _paginationError.value = null
            try {
                val res = vitalRepository.fetchOlderVitals(
                    userId = session.currentUserId,
                    startAfter = cursor,
                    pageSize = DEFAULT_HISTORY_PAGE_SIZE
                )
                res.onSuccess { page ->
                    _olderVitals.update { existing ->
                        PaginationMerger.appendOlderPage(
                            existingTail = existing,
                            newPage = page.items,
                            idSelector = { it.id }
                        )
                    }
                    if (page.lastCursor != null) {
                        _olderCursor.value = page.lastCursor
                    }
                    _olderHasMore.value = page.hasMore
                }.onFailure { err ->
                    _paginationError.value = err.localizedMessage
                        ?: "Could not load earlier vital readings."
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun addVitalReading(
        vitalType: VitalType,
        primaryValue: Double,
        secondaryValue: Double?,
        note: String,
        unit: String? = null
    ) {
        val activeRegion = session.activeHealthRegion.value
        val resolvedUnit = unit?.takeIf { it.isNotBlank() } ?: vitalType.unitFor(activeRegion)
        viewModelScope.launch {
            val res = vitalRepository.addVitalReading(
                userId = session.currentUserId,
                vitalType = vitalType,
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                note = note,
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

                // Offer proactive AI reassessment when an out-of-range vital is logged
                val loggedReading = VitalReading(
                    userId = session.currentUserId,
                    vitalType = vitalType.name,
                    primaryValue = primaryValue,
                    secondaryValue = secondaryValue,
                    unit = resolvedUnit,
                    note = note.trim()
                )
                if (loggedReading.rangeStatus != VitalRangeStatus.NORMAL &&
                    session.entitlementProvider.proactiveReassessmentEnabled
                ) {
                    _abnormalVitalForReassessment.value = loggedReading
                    _vitalReassessmentState.value = ProactiveReassessmentUiState.Idle
                }

                if (note != "Synced from Android Health Connect") {
                    val granted = healthConnectState.value.grantedPermissions
                    when (vitalType) {
                        VitalType.HEART_RATE -> if (HealthConnectManager.PERMISSION_WRITE_HEART_RATE in granted) {
                            healthConnectManager.writeHeartRate(primaryValue.toLong())
                        }
                        VitalType.BLOOD_PRESSURE -> if (
                            secondaryValue != null &&
                            HealthConnectManager.PERMISSION_WRITE_BLOOD_PRESSURE in granted
                        ) {
                            healthConnectManager.writeBloodPressure(primaryValue, secondaryValue)
                        }
                        else -> Unit
                    }
                }
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not log vital reading.")
            }
        }
    }

    fun deleteVitalReading(vitalId: String) {
        viewModelScope.launch {
            val res = vitalRepository.deleteVitalReading(session.currentUserId, vitalId)
            res.onSuccess {
                _olderVitals.update { existing -> existing.filterNot { it.id == vitalId } }
                session.showMessage("Vital reading removed.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove reading.")
            }
        }
    }

    // --- Proactive Abnormal Vital Reassessment ---

    fun runAbnormalVitalReassessment() {
        val vital = _abnormalVitalForReassessment.value ?: return
        if (_vitalReassessmentState.value is ProactiveReassessmentUiState.Loading) return
        if (!session.entitlementProvider.proactiveReassessmentEnabled) {
            _vitalReassessmentState.value = ProactiveReassessmentUiState.Error(
                "Proactive reassessment is temporarily unavailable."
            )
            return
        }

        val activeRegion = session.activeHealthRegion.value
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeMeds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val recentVitals = (vitalsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty()
        val deterministicCritical = ClinicalSafetyEngine.evaluateVital(
            vitalType = vital.parsedType,
            primaryValue = vital.primaryValue,
            secondaryValue = vital.secondaryValue,
            unit = vital.unit.ifBlank { vital.parsedType.unitFor(activeRegion) },
            region = activeRegion
        )
        if (deterministicCritical != null) {
            session.emitCriticalVitalAlert(deterministicCritical)
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _vitalReassessmentState.value = ProactiveReassessmentUiState.Error(blockReason)
                return@launch
            }
            _vitalReassessmentState.value = ProactiveReassessmentUiState.Loading
            try {
                val aiResult = labAndReassessmentAiService.reassessAbnormalVital(
                    vital = vital,
                    recentVitals = recentVitals,
                    profile = profile,
                    activeMedications = activeMeds,
                    region = activeRegion
                )
                // One-way safety ratchet: Gemini can never downgrade a deterministic critical vital alert
                val safeResult = if (deterministicCritical != null) {
                    aiResult.copy(
                        urgencyLevel = UrgencyLevel.EMERGENCY,
                        redFlagsToWatch = (listOf(deterministicCritical.headline) + deterministicCritical.immediateSteps + aiResult.redFlagsToWatch)
                            .distinct()
                            .take(6)
                    )
                } else {
                    aiResult
                }
                _vitalReassessmentState.value = ProactiveReassessmentUiState.Success(safeResult)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Abnormal vital reassessment failed", e)
                _vitalReassessmentState.value = ProactiveReassessmentUiState.Error(
                    e.localizedMessage ?: "Unable to reassess this vital reading right now."
                )
            }
        }
    }

    fun dismissVitalReassessment() {
        _abnormalVitalForReassessment.value = null
        _vitalReassessmentState.value = ProactiveReassessmentUiState.Idle
    }

    // --- Lab Biomarker Vision Extraction & Cloud CRUD ---

    fun scanLabReportPhoto(bitmap: Bitmap) {
        if (_labScanState.value is LabScanUiState.Scanning) return
        if (!session.entitlementProvider.labBiomarkerAiEnabled) {
            _labScanState.value = LabScanUiState.Error(
                "AI lab report extraction is temporarily paused. You can still add biomarkers manually."
            )
            return
        }
        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _labScanState.value = LabScanUiState.Error(blockReason)
                return@launch
            }
            _labScanState.value = LabScanUiState.Scanning
            try {
                val extracted = labAndReassessmentAiService.extractLabBiomarkers(
                    photoBitmap = bitmap,
                    region = session.activeHealthRegion.value
                )
                if (extracted.biomarkers.isEmpty()) {
                    _labScanState.value = LabScanUiState.Error(
                        "No numeric biomarkers could be read clearly. Try a flatter, well-lit photo or add values manually."
                    )
                } else {
                    _labScanState.value = LabScanUiState.Success(extracted)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Lab report extraction failed", e)
                _labScanState.value = LabScanUiState.Error(
                    e.localizedMessage ?: "Could not scan lab report right now."
                )
            }
        }
    }

    fun toggleExtractedBiomarker(index: Int) {
        val current = _labScanState.value as? LabScanUiState.Success ?: return
        val updated = current.result.biomarkers.mapIndexed { idx, item ->
            if (idx == index) item.copy(selected = !item.selected) else item
        }
        _labScanState.value = LabScanUiState.Success(current.result.copy(biomarkers = updated))
    }

    fun saveExtractedBiomarkers() {
        val current = _labScanState.value as? LabScanUiState.Success ?: return
        if (_isSavingLabBatch.value) return
        val chosenCount = current.result.biomarkers.count { it.selected }
        if (chosenCount == 0) {
            session.showMessage("Select at least one biomarker to save.")
            return
        }
        viewModelScope.launch {
            _isSavingLabBatch.value = true
            try {
                val res = labBiomarkerRepository.addLabBiomarkersBatch(
                    userId = session.currentUserId,
                    panelDateKey = current.result.panelDateKey,
                    drafts = current.result.biomarkers
                )
                res.onSuccess {
                    _labScanState.value = LabScanUiState.Idle
                    session.showMessage("Saved $chosenCount lab biomarker${if (chosenCount == 1) "" else "s"} to cloud history.")
                }.onFailure { err ->
                    session.showMessage(err.localizedMessage ?: "Could not save lab biomarkers.")
                }
            } finally {
                _isSavingLabBatch.value = false
            }
        }
    }

    fun clearLabScan() {
        _labScanState.value = LabScanUiState.Idle
    }

    fun addManualLabBiomarker(
        markerName: String,
        category: LabBiomarkerCategory,
        value: Double,
        unit: String,
        referenceRangeText: String,
        status: LabBiomarkerStatus,
        panelDateKey: String,
        labNote: String
    ) {
        viewModelScope.launch {
            val res = labBiomarkerRepository.addLabBiomarker(
                userId = session.currentUserId,
                markerName = markerName,
                category = category,
                value = value,
                unit = unit,
                referenceRangeText = referenceRangeText,
                status = status,
                panelDateKey = panelDateKey.ifBlank { session.todayDateKey },
                labNote = labNote
            )
            res.onSuccess {
                session.showMessage("$markerName saved to Lab Biomarkers.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save lab biomarker.")
            }
        }
    }

    fun deleteLabBiomarker(biomarkerId: String) {
        viewModelScope.launch {
            val res = labBiomarkerRepository.deleteLabBiomarker(session.currentUserId, biomarkerId)
            res.onSuccess {
                session.showMessage("Lab biomarker removed.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove lab biomarker.")
            }
        }
    }
}
