package com.example.feature.history

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DoctorExportOptions
import com.example.core.DoctorVisitPdfExporter
import com.example.data.model.Consultation
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.LongitudinalInsightsResult
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MentalAssessment
import com.example.data.model.SoapPreVisitBrief
import com.example.data.model.VitalReading
import com.example.data.repository.ConsultationRepository
import com.example.data.repository.DEFAULT_HISTORY_PAGE_SIZE
import com.example.data.repository.LiveHeadPage
import com.example.data.repository.PaginationMerger
import com.example.data.repository.ai.SoapPreVisitAiService
import com.example.data.repository.ai.WellnessInsightsAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.InsightsUiState
import com.example.ui.viewmodel.PaginationUiState
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

private const val TAG = "HistoryVM"

class HistoryViewModel(
    private val appContext: Context,
    private val session: SessionCoordinator,
    private val consultationRepository: ConsultationRepository,
    private val wellnessInsightsAiService: WellnessInsightsAiService,
    private val soapPreVisitAiService: SoapPreVisitAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val consultationsHeadState: StateFlow<CloudUiState<LiveHeadPage<Consultation>>>,
    private val checkInsState: StateFlow<CloudUiState<List<DailyCheckIn>>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>,
    private val medicationLogsState: StateFlow<CloudUiState<List<MedicationLog>>>,
    private val vitalsHeadState: StateFlow<CloudUiState<LiveHeadPage<VitalReading>>>,
    private val labBiomarkersState: StateFlow<CloudUiState<List<LabBiomarker>>>,
    private val mentalAssessmentsState: StateFlow<CloudUiState<List<MentalAssessment>>>
) : ViewModel() {

    private val _olderConsultations = MutableStateFlow<List<Consultation>>(emptyList())
    private val _olderCursor = MutableStateFlow<DocumentSnapshot?>(null)
    private val _olderHasMore = MutableStateFlow<Boolean?>(null)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _paginationError = MutableStateFlow<String?>(null)

    private val _insightsState = MutableStateFlow<InsightsUiState>(InsightsUiState.Idle)
    val insightsState: StateFlow<InsightsUiState> = _insightsState.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _isSynthesizingSoap = MutableStateFlow(false)
    val isSynthesizingSoap: StateFlow<Boolean> = _isSynthesizingSoap.asStateFlow()

    val consultationsState: StateFlow<CloudUiState<List<Consultation>>> =
        combine(consultationsHeadState, _olderConsultations) { headUiState, older ->
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
            consultationsHeadState,
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

    fun loadMoreConsultations() {
        if (_isLoadingMore.value) return
        val headPage = (consultationsHeadState.value as? CloudUiState.Success)?.data ?: return
        val cursor = _olderCursor.value ?: headPage.lastCursor ?: return
        val canLoadMore = _olderHasMore.value ?: headPage.hasMore
        if (!canLoadMore) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            _paginationError.value = null
            try {
                val res = consultationRepository.fetchOlderConsultations(
                    userId = session.currentUserId,
                    startAfter = cursor,
                    pageSize = DEFAULT_HISTORY_PAGE_SIZE
                )
                res.onSuccess { page ->
                    _olderConsultations.update { existing ->
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
                        ?: "Could not load earlier consultations."
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun findLoadedConsultation(consultationId: String): Consultation? =
        (consultationsState.value as? CloudUiState.Success)
            ?.data
            ?.firstOrNull { it.id == consultationId }

    fun deleteConsultation(
        consultationId: String,
        onDeleted: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = consultationRepository.deleteConsultation(session.currentUserId, consultationId)
            result.onSuccess {
                _olderConsultations.update { existing -> existing.filterNot { it.id == consultationId } }
                onDeleted(consultationId)
                session.showMessage("Consultation removed from history.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not delete consultation.")
            }
        }
    }

    fun generateLongitudinalInsights() {
        if (_insightsState.value is InsightsUiState.Loading) return
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val consultations = (consultationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val checkIns = (checkInsState.value as? CloudUiState.Success)?.data.orEmpty()
        val medications = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val medLogs = (medicationLogsState.value as? CloudUiState.Success)?.data.orEmpty()
        val vitals = (vitalsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty()

        if (consultations.isEmpty() && checkIns.isEmpty() && vitals.isEmpty() && medications.isEmpty()) {
            _insightsState.value = InsightsUiState.Error(
                "Log at least one daily check-in, vital sign, medication, or consultation to synthesize 30-day insights."
            )
            return
        }

        val activeRegion = session.activeHealthRegion.value
        val entitlements = session.entitlementProvider
        val fingerprint = entitlements.fingerprintOf(
            "longitudinal_30d",
            activeRegion.code.code,
            consultations.size, consultations.firstOrNull()?.id,
            checkIns.size, checkIns.firstOrNull()?.dateKey,
            medications.size, medLogs.size,
            vitals.size, vitals.firstOrNull()?.id
        )
        entitlements.getCached<LongitudinalInsightsResult>("longitudinal_30d", fingerprint)?.let { cached ->
            _insightsState.value = InsightsUiState.Success(cached)
            session.showMessage("Loaded cached 30-day synthesis (0 daily AI credits used).")
            return
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _insightsState.value = InsightsUiState.Error(blockReason)
                return@launch
            }
            _insightsState.value = InsightsUiState.Loading
            try {
                val result = wellnessInsightsAiService.generateLongitudinalInsights(
                    profile = profile,
                    consultations = consultations,
                    checkIns = checkIns,
                    medications = medications,
                    medicationLogs = medLogs,
                    vitals = vitals,
                    region = activeRegion
                )
                entitlements.putCached("longitudinal_30d", fingerprint, result)
                _insightsState.value = InsightsUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Longitudinal insights failed", e)
                _insightsState.value = InsightsUiState.Error(
                    e.localizedMessage ?: "Unable to generate 30-day health synthesis right now."
                )
            }
        }
    }

    fun exportDoctorVisitPdf(
        options: DoctorExportOptions,
        onShareIntentReady: (Intent) -> Unit
    ) {
        if (_isExportingPdf.value) return
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val consultations = (consultationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val checkIns = (checkInsState.value as? CloudUiState.Success)?.data.orEmpty()
        val medications = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val medLogs = (medicationLogsState.value as? CloudUiState.Success)?.data.orEmpty()
        val vitals = (vitalsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty()
        val labBiomarkers = (labBiomarkersState.value as? CloudUiState.Success)?.data.orEmpty()
        val mentalAssessments = (mentalAssessmentsState.value as? CloudUiState.Success)?.data.orEmpty()
        val activeRegion = session.activeHealthRegion.value

        viewModelScope.launch {
            _isExportingPdf.value = true
            try {
                var soapBrief: SoapPreVisitBrief? = null
                val hasClinicalData = consultations.isNotEmpty() || vitals.isNotEmpty() ||
                    labBiomarkers.isNotEmpty() || medications.isNotEmpty() || checkIns.isNotEmpty()
                if (options.includeSoapAiBrief && hasClinicalData) {
                    val entitlements = session.entitlementProvider
                    val soapFingerprint = entitlements.fingerprintOf(
                        "soap_brief",
                        options.daysWindow,
                        activeRegion.code.code,
                        consultations.size, consultations.firstOrNull()?.id,
                        vitals.size, vitals.firstOrNull()?.id,
                        labBiomarkers.size, labBiomarkers.firstOrNull()?.id,
                        medications.size, medLogs.size
                    )
                    val cached = entitlements.getCached<SoapPreVisitBrief>("soap_brief", soapFingerprint)
                    if (cached != null) {
                        soapBrief = cached
                    } else {
                        val blockReason = session.guardCloudAiAndConsumeQuota()
                        if (blockReason == null) {
                            _isSynthesizingSoap.value = true
                            try {
                                val generated = soapPreVisitAiService.generateSoapBrief(
                                    profile = profile,
                                    medications = medications,
                                    medicationLogs = medLogs,
                                    vitals = vitals,
                                    labBiomarkers = labBiomarkers,
                                    consultations = consultations,
                                    checkIns = checkIns,
                                    daysWindow = options.daysWindow,
                                    region = activeRegion
                                )
                                entitlements.putCached("soap_brief", soapFingerprint, generated)
                                soapBrief = generated
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.w(TAG, "SOAP brief synthesis skipped; exporting standard PDF", e)
                                session.showMessage("Exporting standard PDF brief (AI SOAP summary unavailable).")
                            } finally {
                                _isSynthesizingSoap.value = false
                            }
                        } else {
                            session.showMessage("Exporting standard PDF brief (offline or AI quota reached).")
                        }
                    }
                }

                val res = DoctorVisitPdfExporter.generateAndCreateShareIntent(
                    context = appContext,
                    accountEmail = session.accountEmail,
                    profile = profile,
                    medications = medications,
                    medicationLogs = medLogs,
                    vitals = vitals,
                    consultations = consultations,
                    checkIns = checkIns,
                    mentalAssessments = mentalAssessments,
                    labBiomarkers = labBiomarkers,
                    soapBrief = soapBrief,
                    options = options,
                    region = activeRegion
                )
                res.onSuccess { shareIntent ->
                    onShareIntentReady(shareIntent)
                }.onFailure { err ->
                    session.showMessage(err.localizedMessage ?: "Could not generate Doctor Visit PDF.")
                }
            } finally {
                _isSynthesizingSoap.value = false
                _isExportingPdf.value = false
            }
        }
    }
}
