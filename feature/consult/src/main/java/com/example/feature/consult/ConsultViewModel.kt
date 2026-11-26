package com.example.feature.consult

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ClinicalSafetyEngine
import com.example.core.ReminderScheduler
import com.example.core.TrustedMedicalCitationValidator
import com.example.data.model.CareCategory
import com.example.data.model.ChatMessage
import com.example.data.model.Consultation
import com.example.data.model.ConsultationType
import com.example.data.model.FollowUpStatus
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MessageRole
import com.example.data.model.ProposedChatAction
import com.example.data.model.ProposedChatActionStatus
import com.example.data.model.ProposedChatActionType
import com.example.data.model.TrustedMedicalCitation
import com.example.data.model.UrgencyLevel
import com.example.data.model.VitalReading
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.ConsultationRepository
import com.example.data.repository.LiveHeadPage
import com.example.data.repository.MedicationRepository
import com.example.data.repository.VitalRepository
import com.example.data.repository.ai.ChatActionAndCitationAiService
import com.example.data.repository.ai.FollowUpChatAiService
import com.example.data.repository.ai.LabAndReassessmentAiService
import com.example.data.repository.ai.SymptomTriageAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.AnalysisState
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.ClarifyingQuestionsUiState
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.ConsultViewMode
import com.example.ui.viewmodel.IntakeDraft
import com.example.ui.viewmodel.ProactiveReassessmentUiState
import com.example.ui.viewmodel.asCloudUiStateRaw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ConsultVM"

@OptIn(ExperimentalCoroutinesApi::class)
class ConsultViewModel(
    private val appContext: Context,
    private val session: SessionCoordinator,
    private val consultationRepository: ConsultationRepository,
    private val checkInAndCareRepository: CheckInAndCareRepository,
    private val medicationRepository: MedicationRepository,
    private val vitalRepository: VitalRepository,
    private val triageAiService: SymptomTriageAiService,
    private val followUpChatAiService: FollowUpChatAiService,
    private val labAndReassessmentAiService: LabAndReassessmentAiService,
    private val chatActionAndCitationAiService: ChatActionAndCitationAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>,
    private val vitalsHeadState: StateFlow<CloudUiState<LiveHeadPage<VitalReading>>>,
    consultationsHeadState: StateFlow<CloudUiState<LiveHeadPage<Consultation>>>
) : ViewModel() {

    private val _selectedConsultationId = MutableStateFlow<String?>(null)
    val selectedConsultationId: StateFlow<String?> = _selectedConsultationId.asStateFlow()

    /**
     * Holds a consultation resolved outside the newest 30-item live-head window
     * (e.g. opened from a paginated history page or deep link).
     */
    private val _fallbackConsultation = MutableStateFlow<Consultation?>(null)

    val activeConsultation: StateFlow<Consultation?> = combine(
        consultationsHeadState,
        _selectedConsultationId,
        _fallbackConsultation
    ) { headUiState, selectedId, fallback ->
        if (selectedId.isNullOrBlank()) {
            null
        } else {
            val headMatch = (headUiState as? CloudUiState.Success)
                ?.data
                ?.items
                ?.firstOrNull { it.id == selectedId }
            headMatch ?: fallback?.takeIf { it.id == selectedId }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null
    )

    val activeMessagesState: StateFlow<CloudUiState<List<ChatMessage>>> =
        _selectedConsultationId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) {
                    flowOf(CloudUiState.Success(emptyList()))
                } else {
                    consultationRepository.observeMessages(session.currentUserId, id)
                        .asCloudUiStateRaw("Failed to load conversation", TAG)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = CloudUiState.Success(emptyList())
            )

    private val _consultViewMode = MutableStateFlow(ConsultViewMode.INTAKE)
    val consultViewMode: StateFlow<ConsultViewMode> = _consultViewMode.asStateFlow()

    private val _intakeDraft = MutableStateFlow(IntakeDraft())
    val intakeDraft: StateFlow<IntakeDraft> = _intakeDraft.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _emergencyAlertConsultation = MutableStateFlow<Consultation?>(null)
    val emergencyAlertConsultation: StateFlow<Consultation?> = _emergencyAlertConsultation.asStateFlow()

    private val _addedSuggestionKeys = MutableStateFlow<Set<String>>(emptySet())
    val addedSuggestionKeys: StateFlow<Set<String>> = _addedSuggestionKeys.asStateFlow()

    private val _clarifyingQuestionsState =
        MutableStateFlow<ClarifyingQuestionsUiState>(ClarifyingQuestionsUiState.Idle)
    val clarifyingQuestionsState: StateFlow<ClarifyingQuestionsUiState> =
        _clarifyingQuestionsState.asStateFlow()

    private val _worseningReassessmentState =
        MutableStateFlow<ProactiveReassessmentUiState>(ProactiveReassessmentUiState.Idle)
    val worseningReassessmentState: StateFlow<ProactiveReassessmentUiState> =
        _worseningReassessmentState.asStateFlow()

    private val _proposedChatActions = MutableStateFlow<List<ProposedChatAction>>(emptyList())
    val proposedChatActions: StateFlow<List<ProposedChatAction>> = _proposedChatActions.asStateFlow()

    private val _consultCitations = MutableStateFlow<List<TrustedMedicalCitation>>(emptyList())
    val consultCitations: StateFlow<List<TrustedMedicalCitation>> = _consultCitations.asStateFlow()

    fun startNewSymptomCheck() {
        _intakeDraft.value = IntakeDraft(mode = ConsultationType.SYMPTOM_CHECK, step = 0)
        _analysisState.value = AnalysisState.Idle
        _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Idle
        _worseningReassessmentState.value = ProactiveReassessmentUiState.Idle
        _proposedChatActions.value = emptyList()
        _consultCitations.value = emptyList()
        _selectedConsultationId.value = null
        _fallbackConsultation.value = null
        _consultViewMode.value = ConsultViewMode.INTAKE
    }

    fun startNewPhotoAnalysis() {
        _intakeDraft.value = IntakeDraft(
            mode = ConsultationType.PHOTO_ANALYSIS,
            step = 3,
            bodyArea = "Photo / Lab Report",
            severity = 3,
            duration = "Recent"
        )
        _analysisState.value = AnalysisState.Idle
        _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Idle
        _worseningReassessmentState.value = ProactiveReassessmentUiState.Idle
        _proposedChatActions.value = emptyList()
        _consultCitations.value = emptyList()
        _selectedConsultationId.value = null
        _fallbackConsultation.value = null
        _consultViewMode.value = ConsultViewMode.INTAKE
    }

    fun openConsultation(
        consultationId: String,
        openChatDirectly: Boolean = false,
        knownConsultation: Consultation? = null
    ) {
        if (_selectedConsultationId.value != consultationId) {
            _worseningReassessmentState.value = ProactiveReassessmentUiState.Idle
            _proposedChatActions.value = emptyList()
            val topicArea = knownConsultation?.bodyArea.orEmpty()
            _consultCitations.value = TrustedMedicalCitationValidator.fallbackReferencesForTopic(
                bodyArea = topicArea,
                isCanada = session.activeHealthRegion.value.isCanada
            )
        }
        _selectedConsultationId.value = consultationId
        _fallbackConsultation.value = knownConsultation
        _chatState.value = ChatUiState()
        _consultViewMode.value = if (openChatDirectly) ConsultViewMode.CHAT else ConsultViewMode.REPORT

        if (knownConsultation == null) {
            viewModelScope.launch {
                val res = consultationRepository.getConsultation(session.currentUserId, consultationId)
                res.onSuccess { fetched ->
                    if (fetched != null && _selectedConsultationId.value == consultationId) {
                        _fallbackConsultation.value = fetched
                        _consultCitations.value = TrustedMedicalCitationValidator.fallbackReferencesForTopic(
                            bodyArea = fetched.bodyArea,
                            isCanada = session.activeHealthRegion.value.isCanada
                        )
                    }
                }
            }
        }
    }

    fun onConsultationDeleted(consultationId: String) {
        if (_selectedConsultationId.value == consultationId) {
            _selectedConsultationId.value = null
            _fallbackConsultation.value = null
            _consultViewMode.value = ConsultViewMode.INTAKE
        }
    }

    fun switchConsultMode(mode: ConsultViewMode) {
        _consultViewMode.value = mode
        if (mode == ConsultViewMode.INTAKE) {
            _analysisState.value = AnalysisState.Idle
        }
    }

    fun updateIntake(transform: (IntakeDraft) -> IntakeDraft) {
        _intakeDraft.update(transform)
        if (_analysisState.value is AnalysisState.Error) {
            _analysisState.value = AnalysisState.Idle
        }
    }

    fun toggleAssociatedSymptom(symptom: String) {
        val clean = symptom.trim().take(80)
        if (clean.isEmpty()) return
        _intakeDraft.update { draft ->
            val updated = if (clean in draft.associatedSymptoms) {
                draft.associatedSymptoms - clean
            } else if (draft.associatedSymptoms.size < 20) {
                draft.associatedSymptoms + clean
            } else {
                draft.associatedSymptoms
            }
            draft.copy(associatedSymptoms = updated)
        }
    }

    fun setIntakePhoto(uri: Uri?, bitmap: Bitmap?) {
        _intakeDraft.update { it.copy(photoUri = uri, photoBitmap = bitmap) }
    }

    fun generateClarifyingQuestions() {
        if (_clarifyingQuestionsState.value is ClarifyingQuestionsUiState.Loading) return
        if (!session.entitlementProvider.clarifyingQuestionsEnabled) {
            _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Error(
                "AI clarifying questions are temporarily paused. You can proceed straight to analysis."
            )
            return
        }
        val draft = _intakeDraft.value
        if (draft.symptomDescription.isBlank()) {
            _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Error(
                "Describe your main symptom in Step 1 first so Well+ can tailor clarifying questions."
            )
            return
        }
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeRegion = session.activeHealthRegion.value
        val entitlements = session.entitlementProvider
        val fingerprint = entitlements.fingerprintOf(
            "clarify_q",
            activeRegion.code.code,
            draft.bodyArea,
            draft.symptomDescription.trim(),
            draft.severity,
            draft.duration
        )
        entitlements.getCached<List<com.example.data.model.ClarifyingQuestionItem>>("clarify_q", fingerprint)?.let { cached ->
            _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Success(cached)
            return
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Error(blockReason)
                return@launch
            }
            _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Loading
            try {
                val questions = triageAiService.generateClarifyingQuestions(
                    bodyArea = draft.bodyArea,
                    symptomDescription = draft.symptomDescription,
                    severity = draft.severity,
                    duration = draft.duration,
                    profile = profile,
                    region = activeRegion
                )
                if (questions.isEmpty()) {
                    _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Error(
                        "No extra clarifying questions needed — you can continue straight to Review."
                    )
                } else {
                    entitlements.putCached("clarify_q", fingerprint, questions)
                    _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Success(questions)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Clarifying questions generation failed", e)
                _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Error(
                    e.localizedMessage ?: "Could not generate clarifying questions right now."
                )
            }
        }
    }

    fun answerClarifyingQuestion(questionText: String, answer: String) {
        val cleanQ = questionText.trim().take(160)
        val cleanA = answer.trim().take(120)
        if (cleanQ.isEmpty() || cleanA.isEmpty()) return
        _intakeDraft.update { draft ->
            val updated = draft.clarifyingAnswers.toMutableMap()
            if (updated[cleanQ] == cleanA) {
                updated.remove(cleanQ)
            } else {
                updated[cleanQ] = cleanA
            }
            draft.copy(clarifyingAnswers = updated)
        }
    }

    fun clearClarifyingQuestions() {
        _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Idle
    }

    fun submitIntakeForAnalysis() {
        val draft = _intakeDraft.value
        val hasDescription = draft.symptomDescription.isNotBlank()
        val hasPhoto = draft.photoBitmap != null

        if (!hasDescription && !hasPhoto) {
            _analysisState.value = AnalysisState.Error(
                "Please describe your symptoms or attach a photo before analyzing."
            )
            return
        }

        val preparedSummary = draft.buildPreparedSymptomSummary()
        val currentProfile = (profileState.value as? CloudUiState.Success)?.data
        val activeMeds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val recentVitals = (vitalsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty()

        // Layer 3: Deterministic pre-AI red-flag safety check (always instant, works offline, never quota-limited)
        val deterministicRedFlag = ClinicalSafetyEngine.evaluateSymptoms(
            symptomText = preparedSummary,
            associatedSymptoms = draft.associatedSymptoms.toList(),
            severity = draft.severity
        )
        if (deterministicRedFlag != null) {
            session.emitSymptomRedFlag(deterministicRedFlag)
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _analysisState.value = AnalysisState.Error(blockReason)
                return@launch
            }
            _analysisState.value = AnalysisState.Analyzing
            try {
                val aiResult = triageAiService.analyzeConsultation(
                    type = draft.mode,
                    bodyArea = draft.bodyArea,
                    symptomSummary = preparedSummary,
                    severity = draft.severity,
                    duration = draft.duration,
                    associatedSymptoms = draft.associatedSymptoms.toList(),
                    profile = currentProfile,
                    activeMedications = activeMeds,
                    recentVitals = recentVitals,
                    photoBitmap = draft.photoBitmap,
                    region = session.activeHealthRegion.value
                )

                // One-way safety ratchet: Gemini can NEVER downgrade a deterministic emergency red flag
                val structuredResult = if (deterministicRedFlag != null) {
                    aiResult.copy(
                        urgencyLevel = UrgencyLevel.EMERGENCY,
                        redFlags = (listOf(deterministicRedFlag.headline) + deterministicRedFlag.immediateSteps + aiResult.redFlags)
                            .distinct()
                            .take(15)
                    )
                } else {
                    aiResult
                }

                val saveResult = consultationRepository.createConsultation(
                    userId = session.currentUserId,
                    type = draft.mode,
                    bodyArea = draft.bodyArea,
                    symptomSummary = preparedSummary,
                    severity = draft.severity.toLong(),
                    duration = draft.duration,
                    associatedSymptoms = draft.associatedSymptoms.toList(),
                    hadPhotoAttachment = hasPhoto,
                    analysis = structuredResult
                )

                saveResult.onSuccess { newId ->
                    val created = Consultation(
                        id = newId,
                        userId = session.currentUserId,
                        consultationType = draft.mode.name,
                        title = structuredResult.title,
                        bodyArea = draft.bodyArea,
                        symptomSummary = preparedSummary,
                        severity = draft.severity.toLong(),
                        duration = draft.duration,
                        associatedSymptoms = draft.associatedSymptoms.toList(),
                        hadPhotoAttachment = hasPhoto,
                        urgencyLevel = structuredResult.urgencyLevel.name,
                        aiSummary = structuredResult.aiSummary,
                        possibleCauses = structuredResult.possibleCauses,
                        careSuggestions = structuredResult.careSuggestions,
                        redFlags = structuredResult.redFlags,
                        doctorQuestions = structuredResult.doctorQuestions
                    )
                    _selectedConsultationId.value = newId
                    _fallbackConsultation.value = created
                    _proposedChatActions.value = emptyList()
                    _consultCitations.value = TrustedMedicalCitationValidator.fallbackReferencesForTopic(
                        bodyArea = created.bodyArea,
                        isCanada = session.activeHealthRegion.value.isCanada
                    )
                    _analysisState.value = AnalysisState.Idle
                    _clarifyingQuestionsState.value = ClarifyingQuestionsUiState.Idle
                    _consultViewMode.value = ConsultViewMode.REPORT
                    _intakeDraft.value = IntakeDraft()

                    if (session.deviceSettings.value.followUpRemindersEnabled) {
                        ReminderScheduler.scheduleConsultationFollowUp(
                            context = appContext,
                            consultationId = newId,
                            consultationTitle = structuredResult.title,
                            delayHours = 48L
                        )
                    }

                    if (structuredResult.urgencyLevel == UrgencyLevel.URGENT ||
                        structuredResult.urgencyLevel == UrgencyLevel.EMERGENCY
                    ) {
                        _emergencyAlertConsultation.value = created
                    }
                }.onFailure { err ->
                    _analysisState.value = AnalysisState.Error(
                        err.localizedMessage ?: "Analysis succeeded, but saving to your history failed."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Symptom analysis failed", e)
                _analysisState.value = AnalysisState.Error(
                    e.localizedMessage ?: "Unable to complete AI analysis right now. Please try again."
                )
            }
        }
    }

    fun updateConsultationFollowUp(
        consultationId: String,
        status: FollowUpStatus,
        note: String
    ) {
        viewModelScope.launch {
            val res = consultationRepository.updateConsultationFollowUp(
                userId = session.currentUserId,
                consultationId = consultationId,
                status = status,
                note = note
            )
            res.onSuccess {
                _fallbackConsultation.update { current ->
                    if (current?.id == consultationId) {
                        current.copy(followUpStatus = status.name, followUpNote = note.trim().take(600))
                    } else {
                        current
                    }
                }
                if (status == FollowUpStatus.WORSENING) {
                    _worseningReassessmentState.value = ProactiveReassessmentUiState.Idle
                    session.showMessage("Marked as Worsening — tap Run AI Reassessment below for updated guidance.")
                } else {
                    _worseningReassessmentState.value = ProactiveReassessmentUiState.Idle
                    session.showMessage("Symptom outcome updated: ${status.label}.")
                }
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save follow-up status.")
            }
        }
    }

    fun runWorseningReassessment(consultation: Consultation, followUpNote: String) {
        if (_worseningReassessmentState.value is ProactiveReassessmentUiState.Loading) return
        if (!session.entitlementProvider.proactiveReassessmentEnabled) {
            _worseningReassessmentState.value = ProactiveReassessmentUiState.Error(
                "Proactive reassessment is temporarily unavailable."
            )
            return
        }

        val combinedText = listOf(consultation.symptomSummary, followUpNote)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        val deterministicRedFlag = ClinicalSafetyEngine.evaluateSymptoms(
            symptomText = combinedText,
            associatedSymptoms = consultation.associatedSymptoms,
            severity = consultation.severity.toInt().coerceIn(1, 10)
        )
        if (deterministicRedFlag != null) {
            session.emitSymptomRedFlag(deterministicRedFlag)
        }

        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeMeds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val recentVitals = (vitalsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty()
        val activeRegion = session.activeHealthRegion.value

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _worseningReassessmentState.value = ProactiveReassessmentUiState.Error(blockReason)
                return@launch
            }
            _worseningReassessmentState.value = ProactiveReassessmentUiState.Loading
            try {
                val aiResult = labAndReassessmentAiService.reassessWorseningConsultation(
                    consultation = consultation,
                    followUpNote = followUpNote,
                    profile = profile,
                    activeMedications = activeMeds,
                    recentVitals = recentVitals,
                    region = activeRegion
                )
                val safeResult = if (deterministicRedFlag != null) {
                    aiResult.copy(
                        urgencyLevel = UrgencyLevel.EMERGENCY,
                        redFlagsToWatch = (listOf(deterministicRedFlag.headline) + deterministicRedFlag.immediateSteps + aiResult.redFlagsToWatch)
                            .distinct()
                            .take(6)
                    )
                } else {
                    aiResult
                }
                _worseningReassessmentState.value = ProactiveReassessmentUiState.Success(safeResult)
                if (safeResult.urgencyLevel == UrgencyLevel.URGENT ||
                    safeResult.urgencyLevel == UrgencyLevel.EMERGENCY
                ) {
                    _emergencyAlertConsultation.value = consultation.copy(
                        urgencyLevel = safeResult.urgencyLevel.name,
                        redFlags = (safeResult.redFlagsToWatch + consultation.redFlags).distinct().take(10)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Worsening consultation reassessment failed", e)
                _worseningReassessmentState.value = ProactiveReassessmentUiState.Error(
                    e.localizedMessage ?: "Unable to complete reassessment right now."
                )
            }
        }
    }

    fun dismissWorseningReassessment() {
        _worseningReassessmentState.value = ProactiveReassessmentUiState.Idle
    }

    fun sendFollowUpMessage(
        consultation: Consultation,
        text: String,
        photoBitmap: Bitmap?
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && photoBitmap == null) return
        if (_chatState.value.isSending) return

        val existing = (activeMessagesState.value as? CloudUiState.Success)?.data.orEmpty()
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeMeds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val userVisibleText = trimmed.ifEmpty { "Shared a follow-up photo for review." }

        ClinicalSafetyEngine.evaluateSymptoms(symptomText = userVisibleText)?.let { flag ->
            session.emitSymptomRedFlag(flag)
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _chatState.value = ChatUiState(isSending = false, errorMessage = blockReason)
                return@launch
            }
            _chatState.value = ChatUiState(isSending = true, streamingDoctorReply = "", errorMessage = null)
            val saveUserResult = consultationRepository.addChatMessage(
                userId = session.currentUserId,
                consultationId = consultation.id,
                role = MessageRole.USER,
                content = userVisibleText,
                hadPhotoAttachment = photoBitmap != null
            )
            if (saveUserResult.isFailure) {
                _chatState.value = ChatUiState(
                    isSending = false,
                    errorMessage = saveUserResult.exceptionOrNull()?.localizedMessage
                        ?: "Could not send message."
                )
                return@launch
            }

            var finalReply = ""
            try {
                followUpChatAiService.streamFollowUpReply(
                    consultation = consultation,
                    profile = profile,
                    activeMedications = activeMeds,
                    existingMessages = existing,
                    userMessage = userVisibleText,
                    photoBitmap = photoBitmap,
                    region = session.activeHealthRegion.value
                ).collect { partial ->
                    finalReply = partial
                    _chatState.update { it.copy(streamingDoctorReply = partial) }
                }

                if (finalReply.isNotBlank()) {
                    val saveDoctorResult = consultationRepository.addChatMessage(
                        userId = session.currentUserId,
                        consultationId = consultation.id,
                        role = MessageRole.DOCTOR,
                        content = finalReply,
                        hadPhotoAttachment = false
                    )
                    if (saveDoctorResult.isFailure) {
                        _chatState.value = ChatUiState(
                            isSending = false,
                            errorMessage = "Reply received, but failed to save to cloud history."
                        )
                    } else {
                        _chatState.value = ChatUiState(isSending = false)
                        extractPostTurnExtras(
                            consultation = consultation,
                            userMessage = userVisibleText,
                            doctorReply = finalReply
                        )
                    }
                } else {
                    _chatState.value = ChatUiState(
                        isSending = false,
                        errorMessage = "AI Doctor returned an empty response."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Follow-up streaming failed", e)
                _chatState.value = ChatUiState(
                    isSending = false,
                    errorMessage = e.localizedMessage ?: "Failed to get follow-up response."
                )
            }
        }
    }

    fun addSuggestionToCareChecklist(consultationId: String, suggestion: String) {
        val key = "$consultationId::$suggestion"
        viewModelScope.launch {
            val result = checkInAndCareRepository.addCareTask(
                userId = session.currentUserId,
                title = suggestion,
                category = CareCategory.AI_SUGGESTION,
                sourceConsultationId = consultationId
            )
            result.onSuccess {
                _addedSuggestionKeys.update { it + key }
                session.showMessage("Added to your Care Checklist.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not add to checklist.")
            }
        }
    }

    fun scheduleFollowUpNudge(consultation: Consultation) {
        ReminderScheduler.scheduleConsultationFollowUp(
            context = appContext,
            consultationId = consultation.id,
            consultationTitle = consultation.title,
            delayHours = 48L
        )
        session.showMessage("48-hour symptom check-in reminder scheduled.")
    }

    private fun extractPostTurnExtras(
        consultation: Consultation,
        userMessage: String,
        doctorReply: String
    ) {
        viewModelScope.launch {
            val extras = chatActionAndCitationAiService.extractChatTurnExtras(
                consultation = consultation,
                userMessage = userMessage,
                doctorReply = doctorReply,
                region = session.activeHealthRegion.value
            )
            if (_selectedConsultationId.value == consultation.id) {
                if (extras.proposedActions.isNotEmpty()) {
                    _proposedChatActions.value = extras.proposedActions
                }
                if (extras.citations.isNotEmpty()) {
                    _consultCitations.value = extras.citations
                }
            }
        }
    }

    fun confirmProposedAction(actionId: String) {
        val target = _proposedChatActions.value.firstOrNull { it.id == actionId } ?: return
        if (target.status != ProposedChatActionStatus.PENDING) return
        val activeConsult = activeConsultation.value

        _proposedChatActions.update { list ->
            list.map { if (it.id == actionId) it.copy(status = ProposedChatActionStatus.SAVING) else it }
        }

        viewModelScope.launch {
            val result: Result<Unit> = when (target.actionType) {
                ProposedChatActionType.ADD_CARE_TASK -> {
                    checkInAndCareRepository.addCareTask(
                        userId = session.currentUserId,
                        title = target.title,
                        category = target.careCategory,
                        sourceConsultationId = activeConsult?.id.orEmpty()
                    )
                }

                ProposedChatActionType.ADD_MEDICATION -> {
                    medicationRepository.saveMedication(
                        userId = session.currentUserId,
                        name = target.medicationName.ifBlank { target.title },
                        dosage = target.medicationDosage,
                        scheduleSlot = target.medicationSlot,
                        reminderHour = target.medicationSlot.defaultHour,
                        reminderMinute = target.medicationSlot.defaultMinute,
                        instructions = target.medicationInstructions,
                        active = true
                    )
                }

                ProposedChatActionType.LOG_VITAL -> {
                    val primary = target.vitalPrimaryValue
                    if (primary == null) {
                        Result.failure(IllegalArgumentException("Missing vital value."))
                    } else {
                        val saveRes = vitalRepository.addVitalReading(
                            userId = session.currentUserId,
                            vitalType = target.vitalType,
                            primaryValue = primary,
                            secondaryValue = target.vitalSecondaryValue,
                            note = target.vitalNote.ifBlank { "Confirmed from AI Follow-Up Chat" }
                        )
                        if (saveRes.isSuccess) {
                            ClinicalSafetyEngine.evaluateVital(
                                vitalType = target.vitalType,
                                primaryValue = primary,
                                secondaryValue = target.vitalSecondaryValue
                            )?.let { alert -> session.emitCriticalVitalAlert(alert) }
                        }
                        saveRes
                    }
                }

                ProposedChatActionType.SCHEDULE_FOLLOWUP -> {
                    if (activeConsult != null) {
                        ReminderScheduler.scheduleConsultationFollowUp(
                            context = appContext,
                            consultationId = activeConsult.id,
                            consultationTitle = activeConsult.title,
                            delayHours = 48L
                        )
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalStateException("No active consultation selected."))
                    }
                }
            }

            result.onSuccess {
                _proposedChatActions.update { list ->
                    list.map {
                        if (it.id == actionId) it.copy(status = ProposedChatActionStatus.CONFIRMED) else it
                    }
                }
                val confirmationMsg = when (target.actionType) {
                    ProposedChatActionType.ADD_CARE_TASK -> "Added \"${target.title}\" to your Care Checklist."
                    ProposedChatActionType.ADD_MEDICATION -> "Saved \"${target.medicationName.ifBlank { target.title }}\" to Medications."
                    ProposedChatActionType.LOG_VITAL -> "Logged ${target.vitalType.label} to Vitals."
                    ProposedChatActionType.SCHEDULE_FOLLOWUP -> "48-hour follow-up check-in scheduled."
                }
                session.showMessage(confirmationMsg)
            }.onFailure { err ->
                _proposedChatActions.update { list ->
                    list.map {
                        if (it.id == actionId) it.copy(status = ProposedChatActionStatus.PENDING) else it
                    }
                }
                session.showMessage(err.localizedMessage ?: "Could not save confirmed action.")
            }
        }
    }

    fun dismissProposedAction(actionId: String) {
        _proposedChatActions.update { list ->
            list.filterNot { it.id == actionId }
        }
    }

    fun openEmergencySheetFor(consultation: Consultation) {
        _emergencyAlertConsultation.value = consultation
    }

    fun dismissEmergencyAlert() {
        _emergencyAlertConsultation.value = null
    }
}
