package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.core.AiEntitlementProvider
import com.example.core.AiQuotaAndCacheManager
import com.example.core.BillingEntitlementGateway
import com.example.core.DateKeyProvider
import com.example.core.HealthConnectManager
import com.example.core.HealthConnectSyncScheduler
import com.example.core.LocalAiEntitlementProvider
import com.example.core.PlayBillingManager
import com.example.core.RegionalEmergencyDirectory
import com.example.core.ReminderScheduler
import com.example.core.RemoteConfigSafetyFlagProvider
import com.example.core.SanctuaryPreferencesStore
import com.example.core.WidgetRefreshBridge
import com.example.core.WidgetSnapshotStore
import com.example.data.model.BloodType
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareTask
import com.example.data.model.Consultation
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthConnectAvailability
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.MentalAssessment
import com.example.data.model.VitalReading
import com.example.data.model.WellPlusTier
import com.example.data.repository.AccountDataPortabilityRepository
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.ConsultationRepository
import com.example.data.repository.DEFAULT_HISTORY_PAGE_SIZE
import com.example.data.repository.FirestoreSessionHelper
import com.example.data.repository.LabBiomarkerRepository
import com.example.data.repository.LiveHeadPage
import com.example.data.repository.MedicationRepository
import com.example.data.repository.MentalAndBodyRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.VitalRepository
import com.example.data.repository.ai.ChatActionAndCitationAiService
import com.example.data.repository.ai.FollowUpChatAiService
import com.example.data.repository.ai.LabAndReassessmentAiService
import com.example.data.repository.ai.MedicationAiService
import com.example.data.repository.ai.SoapPreVisitAiService
import com.example.data.repository.ai.SymptomTriageAiService
import com.example.data.repository.ai.WellnessInsightsAiService
import com.example.feature.bodycycle.BodyCycleViewModel
import com.example.feature.consult.ConsultViewModel
import com.example.feature.healthconnect.HealthConnectViewModel
import com.example.feature.history.HistoryViewModel
import com.example.feature.medications.MedicationsViewModel
import com.example.feature.mental.MentalWellbeingViewModel
import com.example.feature.profile.ProfileViewModel
import com.example.feature.today.TodayViewModel
import com.example.feature.vitals.VitalsViewModel
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.HealthConnectUiState
import com.example.ui.viewmodel.asCloudUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-scoped manual dependency container (constructor injection).
 *
 * Owns ONE shared hot Firestore `StateFlow` per collection so multiple feature ViewModels
 * (Today, Meds, Consult, History, Vitals, Mental, BodyCycle, HealthConnect, Profile)
 * never open duplicate snapshot listeners for the same user path.
 *
 * Call [close] via `DisposableEffect(user.uid)` when signing out or switching accounts.
 */
class WellPlusAppContainer(
    private val appContext: Context,
    val currentUserId: String,
    val defaultDisplayName: String,
    val accountEmail: String
) {
    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val databaseId: String = appContext.getString(R.string.firestore_database_id)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    private val firestoreSession = FirestoreSessionHelper(firestore)

    // Domain repositories
    val profileRepository = ProfileRepository(firestoreSession)
    val consultationRepository = ConsultationRepository(firestoreSession)
    val checkInAndCareRepository = CheckInAndCareRepository(firestoreSession)
    val medicationRepository = MedicationRepository(firestoreSession)
    val vitalRepository = VitalRepository(firestoreSession)
    val labBiomarkerRepository = LabBiomarkerRepository(firestoreSession)
    val mentalAndBodyRepository = MentalAndBodyRepository(firestoreSession)
    val portabilityRepository = AccountDataPortabilityRepository(firestoreSession)

    // Domain AI services
    val triageAiService = SymptomTriageAiService()
    val followUpChatAiService = FollowUpChatAiService()
    val medicationAiService = MedicationAiService()
    val wellnessInsightsAiService = WellnessInsightsAiService()
    val labAndReassessmentAiService = LabAndReassessmentAiService()
    val chatActionAndCitationAiService = ChatActionAndCitationAiService()
    val soapPreVisitAiService = SoapPreVisitAiService()

    // Local preferences, Google Play Billing v9, Firebase Remote Config safety flags, entitlements, widget store, and Health Connect
    val prefsStore = SanctuaryPreferencesStore(appContext)
    val billingGateway: BillingEntitlementGateway = PlayBillingManager(appContext).apply {
        connectAndRefresh()
    }
    val subscriptionTierFlow: StateFlow<WellPlusTier> = billingGateway.subscriptionState
        .map { it.tier }
        .distinctUntilChanged()
        .stateIn(
            scope = containerScope,
            started = SharingStarted.Eagerly,
            initialValue = WellPlusTier.FREE
        )
    val remoteConfigFlags = RemoteConfigSafetyFlagProvider().apply { initializeAndFetchAsync() }
    val entitlementProvider: AiEntitlementProvider = LocalAiEntitlementProvider(
        quotaAndCacheManager = AiQuotaAndCacheManager(prefsStore) { remoteConfigFlags.currentFlags },
        subscriptionTierProvider = { billingGateway.subscriptionState.value.tier }
    )
    val widgetSnapshotStore = WidgetSnapshotStore(appContext)
    val dateKeyProvider = DateKeyProvider()
    val healthConnectManager: HealthConnectManager = HealthConnectManager.getInstance(appContext)

    // Shared hot user-scoped Firestore streams (1 listener per collection)
    val profileState: StateFlow<CloudUiState<HealthProfile?>> =
        profileRepository.observeProfile(currentUserId)
            .asCloudUiState(containerScope, "Failed to load health profile", "WellPlusProfile")

    val consultationsHeadState: StateFlow<CloudUiState<LiveHeadPage<Consultation>>> =
        consultationRepository.observeConsultationsHead(currentUserId, DEFAULT_HISTORY_PAGE_SIZE)
            .asCloudUiState(containerScope, "Failed to load consultation history", "WellPlusConsults")

    val consultationsHeadListState: StateFlow<CloudUiState<List<Consultation>>> =
        consultationsHeadState
            .map { state ->
                when (state) {
                    is CloudUiState.Loading -> CloudUiState.Loading
                    is CloudUiState.Error -> CloudUiState.Error(state.message)
                    is CloudUiState.Success -> CloudUiState.Success(state.data.items)
                }
            }
            .stateIn(
                scope = containerScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = CloudUiState.Loading
            )

    val checkInsState: StateFlow<CloudUiState<List<DailyCheckIn>>> =
        checkInAndCareRepository.observeCheckIns(currentUserId)
            .asCloudUiState(containerScope, "Failed to load daily check-ins", "WellPlusCheckIns")

    val careTasksState: StateFlow<CloudUiState<List<CareTask>>> =
        checkInAndCareRepository.observeCareTasks(currentUserId)
            .asCloudUiState(containerScope, "Failed to load care checklist", "WellPlusCareTasks")

    val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>> =
        medicationRepository.observeMedications(currentUserId)
            .asCloudUiState(containerScope, "Failed to load medications", "WellPlusMeds")

    val medicationLogsState: StateFlow<CloudUiState<List<MedicationLog>>> =
        medicationRepository.observeMedicationLogs(currentUserId)
            .asCloudUiState(containerScope, "Failed to load medication adherence", "WellPlusMedLogs")

    val vitalsHeadState: StateFlow<CloudUiState<LiveHeadPage<VitalReading>>> =
        vitalRepository.observeVitalsHead(currentUserId, DEFAULT_HISTORY_PAGE_SIZE)
            .asCloudUiState(containerScope, "Failed to load vitals", "WellPlusVitals")

    val vitalsHeadListState: StateFlow<CloudUiState<List<VitalReading>>> =
        vitalsHeadState
            .map { state ->
                when (state) {
                    is CloudUiState.Loading -> CloudUiState.Loading
                    is CloudUiState.Error -> CloudUiState.Error(state.message)
                    is CloudUiState.Success -> CloudUiState.Success(state.data.items)
                }
            }
            .stateIn(
                scope = containerScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = CloudUiState.Loading
            )

    val mentalAssessmentsState: StateFlow<CloudUiState<List<MentalAssessment>>> =
        mentalAndBodyRepository.observeMentalAssessments(currentUserId)
            .asCloudUiState(containerScope, "Failed to load mental wellbeing screenings", "WellPlusMental")

    val bodyMeasurementsState: StateFlow<CloudUiState<List<BodyMeasurement>>> =
        mentalAndBodyRepository.observeBodyMeasurements(currentUserId)
            .asCloudUiState(containerScope, "Failed to load body measurements", "WellPlusBody")

    val cycleLogsState: StateFlow<CloudUiState<List<CycleLog>>> =
        mentalAndBodyRepository.observeCycleLogs(currentUserId)
            .asCloudUiState(containerScope, "Failed to load cycle tracking logs", "WellPlusCycle")

    val labBiomarkersState: StateFlow<CloudUiState<List<LabBiomarker>>> =
        labBiomarkerRepository.observeLabBiomarkers(currentUserId)
            .asCloudUiState(containerScope, "Failed to load lab biomarkers", "WellPlusLabBiomarkers")

    // Shared session coordinator
    val session = SessionCoordinator(
        appContext = appContext,
        sessionScope = containerScope,
        currentUserId = currentUserId,
        defaultDisplayName = defaultDisplayName,
        accountEmail = accountEmail,
        prefsStore = prefsStore,
        entitlementProvider = entitlementProvider,
        dateKeyProvider = dateKeyProvider,
        mentalAndBodyRepository = mentalAndBodyRepository,
        profileState = profileState,
        subscriptionTierFlow = subscriptionTierFlow
    )

    // Shared Health Connect UI state so Today, Vitals write-back, and Health Connect screen stay in sync
    internal val mutableHealthConnectState = MutableStateFlow(
        HealthConnectUiState(
            frameworkSourceLabel = HealthConnectManager.frameworkSourceLabel()
        )
    )
    val healthConnectState: StateFlow<HealthConnectUiState> = mutableHealthConnectState.asStateFlow()

    init {
        refreshHealthConnectInBackground(autoSyncIfGranted = true)
        containerScope.launch {
            combine(
                session.deviceSettings.map { it.medRemindersEnabled }.distinctUntilChanged(),
                medicationsState
            ) { remindersEnabled, medsUiState ->
                if (medsUiState is CloudUiState.Success) {
                    remindersEnabled to medsUiState.data
                } else {
                    null
                }
            }.collect { pair ->
                if (pair != null) {
                    withContext(Dispatchers.IO) {
                        ReminderScheduler.syncMedicationReminders(
                            context = appContext,
                            enabled = pair.first,
                            medications = pair.second
                        )
                    }
                }
            }
        }
        // Keep periodic 6h Health Connect WorkManager schedule aligned with user preference
        containerScope.launch {
            session.deviceSettings
                .map { it.backgroundHcSyncEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    withContext(Dispatchers.IO) {
                        HealthConnectSyncScheduler.syncPeriodicSchedule(appContext, enabled)
                    }
                }
        }
        // Keep Home Screen Glance widgets synced with today's mood, next dose, and Health Connect snapshot
        containerScope.launch {
            combine(checkInsState, session.todayDateKeyFlow) { ciState, todayKey ->
                (ciState as? CloudUiState.Success)?.data?.let { it to todayKey }
            }.collect { pair ->
                if (pair != null) {
                    val (list, todayKey) = pair
                    val todayEntry = list.firstOrNull { it.dateKey == todayKey }
                    val moodLevel = todayEntry?.moodLevel?.toInt() ?: 0
                    val moodLabel = when (moodLevel) {
                        5 -> "Thriving"
                        4 -> "Calm"
                        3 -> "Steady"
                        2 -> "Tense"
                        1 -> "Low"
                        else -> ""
                    }
                    withContext(Dispatchers.IO) {
                        widgetSnapshotStore.updateMood(
                            dateKey = todayKey,
                            moodLevel = moodLevel,
                            moodLabel = moodLabel,
                            streakDays = list.size.coerceAtMost(31)
                        )
                        WidgetRefreshBridge.notifyDataChanged(appContext)
                    }
                }
            }
        }
        containerScope.launch {
            combine(
                medicationsState,
                medicationLogsState,
                session.todayDateKeyFlow
            ) { medsState, logsState, todayKey ->
                val meds = (medsState as? CloudUiState.Success)?.data ?: return@combine null
                val logs = (logsState as? CloudUiState.Success)?.data ?: return@combine null
                Triple(meds, logs, todayKey)
            }.collect { triple ->
                if (triple != null) {
                    val (meds, logs, todayKey) = triple
                    val scheduledActive = meds.filter {
                        it.active && it.parsedSlot != MedicationScheduleSlot.AS_NEEDED
                    }.sortedBy { it.parsedSlot.ordinal }
                    val takenIds = logs
                        .filter { it.dateKey == todayKey && it.parsedStatus == MedicationLogStatus.TAKEN }
                        .map { it.medicationId }
                        .toSet()
                    val nextDue = scheduledActive.firstOrNull { it.id !in takenIds }
                    withContext(Dispatchers.IO) {
                        widgetSnapshotStore.updateMedicationSummary(
                            nextMedId = nextDue?.id.orEmpty(),
                            nextMedName = nextDue?.name.orEmpty(),
                            nextMedDosage = nextDue?.dosage.orEmpty(),
                            nextMedTimeLabel = nextDue?.parsedSlot?.label.orEmpty(),
                            takenTodayCount = scheduledActive.count { it.id in takenIds },
                            activeMedsCount = scheduledActive.size
                        )
                        WidgetRefreshBridge.notifyDataChanged(appContext)
                    }
                }
            }
        }
        containerScope.launch {
            healthConnectState
                .map { it.snapshot }
                .distinctUntilChanged()
                .collect { snap ->
                    if (snap != null) {
                        withContext(Dispatchers.IO) {
                            widgetSnapshotStore.updateHealthConnectSummary(
                                stepsToday = snap.stepsToday,
                                heartRateBpm = snap.heartRate?.latestBpm,
                                syncedAtMillis = snap.syncedAtMillis
                            )
                            WidgetRefreshBridge.notifyDataChanged(appContext)
                        }
                    }
                }
        }
        // Keep opt-in Emergency Medical ID Home Screen Glance Widget synced (default OFF)
        containerScope.launch {
            combine(
                session.deviceSettings,
                profileState,
                medicationsState
            ) { settings, profUiState, medsUiState ->
                Triple(
                    settings,
                    (profUiState as? CloudUiState.Success)?.data,
                    (medsUiState as? CloudUiState.Success)?.data.orEmpty()
                )
            }.collect { (settings, profile, meds) ->
                val enabled = settings.showEmergencyIdOnWidget
                val regionProfile = RegionalEmergencyDirectory.resolve(
                    appContext,
                    settings.emergencyRegionOverride.ifBlank { profile?.preferredEmergencyCountry.orEmpty() }
                )
                val medNames = buildList {
                    meds.filter { it.active }.forEach { med ->
                        add(if (med.dosage.isNotBlank()) "${med.name} ${med.dosage}" else med.name)
                    }
                    profile?.medications.orEmpty().forEach { name ->
                        if (none { it.startsWith(name, ignoreCase = true) }) add(name)
                    }
                }
                withContext(Dispatchers.IO) {
                    widgetSnapshotStore.updateEmergencyId(
                        enabled = enabled,
                        displayName = profile?.displayName?.ifBlank { defaultDisplayName }
                            ?: defaultDisplayName.ifBlank { "Well+ Patient" },
                        bloodType = BloodType.fromCode(profile?.bloodType).label,
                        allergiesSummary = profile?.allergies.orEmpty().joinToString(", "),
                        medsSummary = medNames.take(4).joinToString(", "),
                        contactLabel = profile?.emergencyContactName.orEmpty(),
                        contactPhone = profile?.emergencyContactPhone.orEmpty(),
                        emergencyDialNumber = regionProfile.emergencyNumber
                    )
                    WidgetRefreshBridge.notifyDataChanged(appContext)
                }
            }
        }
    }

    fun refreshHealthConnectInBackground(autoSyncIfGranted: Boolean = true) {
        containerScope.launch {
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
            if (autoSyncIfGranted &&
                granted.intersect(HealthConnectManager.readPermissions).isNotEmpty() &&
                !mutableHealthConnectState.value.isSyncing
            ) {
                mutableHealthConnectState.update { it.copy(isSyncing = true, errorMessage = null) }
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
    }

    // Typed ViewModelProvider.Factory builders for the 9 focused feature ViewModels
    fun todayViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            TodayViewModel(
                session = session,
                checkInAndCareRepository = checkInAndCareRepository,
                wellnessInsightsAiService = wellnessInsightsAiService,
                profileState = profileState,
                medicationsState = medicationsState
            )
        }
    }

    fun consultViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ConsultViewModel(
                appContext = appContext,
                session = session,
                consultationRepository = consultationRepository,
                checkInAndCareRepository = checkInAndCareRepository,
                medicationRepository = medicationRepository,
                vitalRepository = vitalRepository,
                triageAiService = triageAiService,
                followUpChatAiService = followUpChatAiService,
                labAndReassessmentAiService = labAndReassessmentAiService,
                chatActionAndCitationAiService = chatActionAndCitationAiService,
                profileState = profileState,
                medicationsState = medicationsState,
                vitalsHeadState = vitalsHeadState,
                consultationsHeadState = consultationsHeadState
            )
        }
    }

    fun medicationsViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            MedicationsViewModel(
                appContext = appContext,
                session = session,
                medicationRepository = medicationRepository,
                medicationAiService = medicationAiService,
                profileState = profileState,
                medicationsState = medicationsState
            )
        }
    }

    fun vitalsViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            VitalsViewModel(
                session = session,
                vitalRepository = vitalRepository,
                labBiomarkerRepository = labBiomarkerRepository,
                labAndReassessmentAiService = labAndReassessmentAiService,
                healthConnectManager = healthConnectManager,
                healthConnectState = healthConnectState,
                profileState = profileState,
                medicationsState = medicationsState,
                vitalsHeadState = vitalsHeadState,
                labBiomarkersState = labBiomarkersState
            )
        }
    }

    fun mentalWellbeingViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            MentalWellbeingViewModel(
                session = session,
                mentalAndBodyRepository = mentalAndBodyRepository,
                checkInAndCareRepository = checkInAndCareRepository,
                wellnessInsightsAiService = wellnessInsightsAiService,
                profileState = profileState,
                checkInsState = checkInsState
            )
        }
    }

    fun bodyCycleViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            BodyCycleViewModel(
                session = session,
                mentalAndBodyRepository = mentalAndBodyRepository,
                checkInAndCareRepository = checkInAndCareRepository,
                wellnessInsightsAiService = wellnessInsightsAiService,
                profileState = profileState,
                bodyMeasurementsState = bodyMeasurementsState,
                cycleLogsState = cycleLogsState
            )
        }
    }

    fun healthConnectViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HealthConnectViewModel(
                appContext = appContext,
                session = session,
                healthConnectManager = healthConnectManager,
                mutableHealthConnectState = mutableHealthConnectState,
                vitalRepository = vitalRepository,
                checkInAndCareRepository = checkInAndCareRepository,
                wellnessInsightsAiService = wellnessInsightsAiService,
                profileState = profileState,
                medicationsState = medicationsState
            )
        }
    }

    fun historyViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HistoryViewModel(
                appContext = appContext,
                session = session,
                consultationRepository = consultationRepository,
                wellnessInsightsAiService = wellnessInsightsAiService,
                soapPreVisitAiService = soapPreVisitAiService,
                profileState = profileState,
                consultationsHeadState = consultationsHeadState,
                checkInsState = checkInsState,
                medicationsState = medicationsState,
                medicationLogsState = medicationLogsState,
                vitalsHeadState = vitalsHeadState,
                labBiomarkersState = labBiomarkersState,
                mentalAssessmentsState = mentalAssessmentsState
            )
        }
    }

    fun profileViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ProfileViewModel(
                appContext = appContext,
                session = session,
                profileRepository = profileRepository,
                portabilityRepository = portabilityRepository,
                profileState = profileState,
                consultationsHeadState = consultationsHeadState,
                checkInsState = checkInsState,
                careTasksState = careTasksState,
                medicationsState = medicationsState,
                medicationLogsState = medicationLogsState,
                vitalsHeadState = vitalsHeadState,
                mentalAssessmentsState = mentalAssessmentsState,
                bodyMeasurementsState = bodyMeasurementsState,
                cycleLogsState = cycleLogsState
            )
        }
    }

    fun close() {
        billingGateway.disconnect()
        session.close()
        containerScope.cancel()
    }
}
