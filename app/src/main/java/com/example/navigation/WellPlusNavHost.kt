package com.example.navigation

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.MainActivity
import com.example.core.HealthConnectManager
import com.example.core.WidgetRefreshBridge
import com.example.core.signOutUser
import com.example.data.model.AiFeatureArea
import com.example.data.model.PhysicalActivityLevel
import com.example.di.WellPlusAppContainer
import com.example.feature.bodycycle.BodyCycleViewModel
import com.example.feature.consult.ConsultViewModel
import com.example.feature.healthconnect.HealthConnectViewModel
import com.example.feature.history.HistoryViewModel
import com.example.feature.medications.MedicationsViewModel
import com.example.feature.mental.MentalWellbeingViewModel
import com.example.feature.profile.ProfileViewModel
import com.example.feature.today.TodayViewModel
import com.example.feature.vitals.VitalsViewModel
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.BodyAndCycleScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.CameraGuideMode
import com.example.ui.screens.ConsultationReportView
import com.example.ui.screens.DailyCheckInScreen
import com.example.ui.screens.EmergencyMedicalIdScreen
import com.example.ui.screens.FollowUpChatView
import com.example.ui.screens.GeneralHealthStatus
import com.example.ui.screens.GuidedIntakeScreen
import com.example.ui.screens.HealthConnectScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MedicationsScreen
import com.example.ui.screens.MentalWellbeingScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.VitalsScreen
import com.example.ui.screens.WellPlusProScreen
import com.example.ui.screens.WellnessMoodOption
import com.example.ui.screens.WellnessNoteCodec
import com.example.ui.theme.GlassShimmerCard
import com.example.ui.theme.SanctuaryThemeMode
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.ConsultViewMode
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class CameraTarget {
    INTAKE,
    FOLLOW_UP_CHAT,
    MEDICATION_SCAN,
    LAB_BIOMARKER_SCAN
}

private data class ActiveCameraRequest(
    val target: CameraTarget,
    val guideMode: CameraGuideMode
)

@Composable
fun WellPlusAuthenticatedWorkspace(
    user: FirebaseUser,
    themeMode: SanctuaryThemeMode,
    pendingDeepLink: PendingDeepLink?,
    onConsumeDeepLink: () -> Unit,
    onThemeModeChange: (SanctuaryThemeMode) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val container = remember(user.uid) {
        WellPlusAppContainer(
            appContext = context.applicationContext,
            currentUserId = user.uid,
            defaultDisplayName = user.displayName.orEmpty(),
            accountEmail = user.email.orEmpty()
        )
    }
    DisposableEffect(container) {
        onDispose { container.close() }
    }

    val session = container.session

    // Focused feature ViewModels backed by shared hot user-scoped Firestore streams
    val todayVm: TodayViewModel = viewModel(
        key = "${user.uid}_today",
        factory = container.todayViewModelFactory()
    )
    val consultVm: ConsultViewModel = viewModel(
        key = "${user.uid}_consult",
        factory = container.consultViewModelFactory()
    )
    val medsVm: MedicationsViewModel = viewModel(
        key = "${user.uid}_meds",
        factory = container.medicationsViewModelFactory()
    )
    val vitalsVm: VitalsViewModel = viewModel(
        key = "${user.uid}_vitals",
        factory = container.vitalsViewModelFactory()
    )
    val mentalVm: MentalWellbeingViewModel = viewModel(
        key = "${user.uid}_mental",
        factory = container.mentalWellbeingViewModelFactory()
    )
    val bodyCycleVm: BodyCycleViewModel = viewModel(
        key = "${user.uid}_bodycycle",
        factory = container.bodyCycleViewModelFactory()
    )
    val hcVm: HealthConnectViewModel = viewModel(
        key = "${user.uid}_healthconnect",
        factory = container.healthConnectViewModelFactory()
    )
    val historyVm: HistoryViewModel = viewModel(
        key = "${user.uid}_history",
        factory = container.historyViewModelFactory()
    )
    val profileVm: ProfileViewModel = viewModel(
        key = "${user.uid}_profile",
        factory = container.profileViewModelFactory()
    )

    // Session & shared stream states
    val todayDateKey by session.todayDateKeyFlow.collectAsStateWithLifecycle()
    val deviceSettings by session.deviceSettings.collectAsStateWithLifecycle()
    val activeHealthRegion by session.activeHealthRegion.collectAsStateWithLifecycle()
    val persistedSettingsNullable by container.prefsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val isOnline by session.isOnline.collectAsStateWithLifecycle()
    val subscriptionState by container.billingGateway.subscriptionState.collectAsStateWithLifecycle()
    val aiQuotaRemaining by session.aiQuotaRemaining.collectAsStateWithLifecycle()
    val aiDailyLimit = remember(subscriptionState.tier) { session.aiDailyLimit }
    val snackbarMessage by session.snackbarMessage.collectAsStateWithLifecycle()
    val isSubmittingSafetyReport by session.isSubmittingSafetyReport.collectAsStateWithLifecycle()
    val activeCriticalVitalAlert by session.activeCriticalVitalAlert.collectAsStateWithLifecycle()
    val activeSymptomRedFlag by session.activeSymptomRedFlag.collectAsStateWithLifecycle()

    val profileState by container.profileState.collectAsStateWithLifecycle()
    val consultationsHeadListState by container.consultationsHeadListState.collectAsStateWithLifecycle()
    val checkInsState by container.checkInsState.collectAsStateWithLifecycle()
    val careTasksState by container.careTasksState.collectAsStateWithLifecycle()
    val medicationsState by container.medicationsState.collectAsStateWithLifecycle()
    val medicationLogsState by container.medicationLogsState.collectAsStateWithLifecycle()
    val vitalsHeadListState by container.vitalsHeadListState.collectAsStateWithLifecycle()
    val mentalAssessmentsState by container.mentalAssessmentsState.collectAsStateWithLifecycle()
    val bodyMeasurementsState by container.bodyMeasurementsState.collectAsStateWithLifecycle()
    val cycleLogsState by container.cycleLogsState.collectAsStateWithLifecycle()
    val healthConnectState by container.healthConnectState.collectAsStateWithLifecycle()

    // Feature-scoped states
    val isSavingCheckIn by todayVm.isSavingCheckIn.collectAsStateWithLifecycle()
    val summaryInsightState by todayVm.summaryInsightState.collectAsStateWithLifecycle()

    val activeConsultation by consultVm.activeConsultation.collectAsStateWithLifecycle()
    val activeMessagesState by consultVm.activeMessagesState.collectAsStateWithLifecycle()
    val consultViewMode by consultVm.consultViewMode.collectAsStateWithLifecycle()
    val intakeDraft by consultVm.intakeDraft.collectAsStateWithLifecycle()
    val analysisState by consultVm.analysisState.collectAsStateWithLifecycle()
    val chatState by consultVm.chatState.collectAsStateWithLifecycle()
    val emergencyAlertConsultation by consultVm.emergencyAlertConsultation.collectAsStateWithLifecycle()
    val addedSuggestionKeys by consultVm.addedSuggestionKeys.collectAsStateWithLifecycle()
    val clarifyingQuestionsState by consultVm.clarifyingQuestionsState.collectAsStateWithLifecycle()
    val worseningReassessmentState by consultVm.worseningReassessmentState.collectAsStateWithLifecycle()
    val proposedChatActions by consultVm.proposedChatActions.collectAsStateWithLifecycle()
    val consultCitations by consultVm.consultCitations.collectAsStateWithLifecycle()

    val interactionState by medsVm.interactionState.collectAsStateWithLifecycle()
    val prescriptionScanState by medsVm.prescriptionScanState.collectAsStateWithLifecycle()

    val paginatedVitalsState by vitalsVm.vitalsState.collectAsStateWithLifecycle()
    val vitalsPaginationState by vitalsVm.paginationState.collectAsStateWithLifecycle()
    val labBiomarkersState by vitalsVm.labBiomarkersState.collectAsStateWithLifecycle()
    val labScanState by vitalsVm.labScanState.collectAsStateWithLifecycle()
    val isSavingLabBatch by vitalsVm.isSavingLabBatch.collectAsStateWithLifecycle()
    val abnormalVitalForReassessment by vitalsVm.abnormalVitalForReassessment.collectAsStateWithLifecycle()
    val vitalReassessmentState by vitalsVm.vitalReassessmentState.collectAsStateWithLifecycle()

    val mentalReflectionState by mentalVm.mentalReflectionState.collectAsStateWithLifecycle()
    val bodyCycleInsightState by bodyCycleVm.bodyCycleInsightState.collectAsStateWithLifecycle()
    val healthConnectAiState by hcVm.healthConnectAiState.collectAsStateWithLifecycle()
    val backgroundHcSyncEnabled by hcVm.backgroundHcSyncEnabled.collectAsStateWithLifecycle()

    val paginatedConsultationsState by historyVm.consultationsState.collectAsStateWithLifecycle()
    val historyPaginationState by historyVm.paginationState.collectAsStateWithLifecycle()
    val insightsState by historyVm.insightsState.collectAsStateWithLifecycle()
    val isExportingPdf by historyVm.isExportingPdf.collectAsStateWithLifecycle()
    val isSynthesizingSoap by historyVm.isSynthesizingSoap.collectAsStateWithLifecycle()

    val isSavingOnboarding by profileVm.isSavingOnboarding.collectAsStateWithLifecycle()
    val isExportingJson by profileVm.isExportingJson.collectAsStateWithLifecycle()
    val isDeletingAccount by profileVm.isDeletingAccount.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showPdfExportDialog by rememberSaveable { mutableStateOf(false) }
    var activeCameraRequest by remember { mutableStateOf<ActiveCameraRequest?>(null) }
    var pendingIntakeCameraPhoto by remember { mutableStateOf<Pair<Uri, Bitmap>?>(null) }
    var pendingChatCameraPhoto by remember { mutableStateOf<Pair<Uri, Bitmap>?>(null) }
    var pendingRxCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingLabCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingAiReport by remember { mutableStateOf<Pair<AiFeatureArea, String>?>(null) }

    val reportAiResponse: (AiFeatureArea, String) -> Unit = { area, snippet ->
        pendingAiReport = area to snippet
    }

    fun navigateToTab(tab: WellPlusTab) {
        navController.navigate(tab.routeId) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToDetail(destination: WellPlusRoute) {
        navController.navigate(destination.route) {
            launchSingleTop = true
        }
    }

    val hostActivity = context as? MainActivity
    LaunchedEffect(hostActivity, deviceSettings.screenPrivacyEnabled) {
        val activity = hostActivity ?: return@LaunchedEffect
        activity.updateScreenPrivacyIfNeeded(enabled = deviceSettings.screenPrivacyEnabled)
    }

    var hasCheckedInitialLock by remember { mutableStateOf(false) }
    var isBiometricLocked by remember { mutableStateOf(false) }

    LaunchedEffect(persistedSettingsNullable) {
        val loaded = persistedSettingsNullable
        if (loaded != null && !hasCheckedInitialLock) {
            hasCheckedInitialLock = true
            if (loaded.biometricLockEnabled) {
                isBiometricLocked = true
            }
        }
    }

    val currentProfile = (profileState as? CloudUiState.Success)?.data
    val needsOnboarding = profileState is CloudUiState.Success &&
        (currentProfile == null || !currentProfile.onboardingCompleted)

    // Consume pending notification deep links once gates (loading, biometric lock, onboarding) are clear
    LaunchedEffect(pendingDeepLink, isBiometricLocked, needsOnboarding, profileState) {
        val link = pendingDeepLink ?: return@LaunchedEffect
        if (isBiometricLocked || needsOnboarding || profileState is CloudUiState.Loading) return@LaunchedEffect
        activeCameraRequest = null
        when (link) {
            is PendingDeepLink.Medications -> {
                navigateToTab(WellPlusTab.MEDS)
            }
            is PendingDeepLink.DailyCheckIn -> {
                navigateToTab(WellPlusTab.TODAY)
                navigateToDetail(WellPlusRoute.DailyCheckIn)
            }
            is PendingDeepLink.FollowUp -> {
                if (!link.consultationId.isNullOrBlank()) {
                    consultVm.openConsultation(link.consultationId, openChatDirectly = false)
                    navigateToTab(WellPlusTab.CONSULT)
                } else {
                    navigateToTab(WellPlusTab.HISTORY)
                }
            }
            is PendingDeepLink.EmergencyMedicalId -> {
                navigateToTab(WellPlusTab.PROFILE)
                navigateToDetail(WellPlusRoute.EmergencyMedicalId)
            }
        }
        onConsumeDeepLink()
    }

    val isOnHealthConnectRoute = currentRoute == WellPlusRoute.HealthConnect.route
    DisposableEffect(lifecycleOwner, deviceSettings.biometricLockEnabled, isOnHealthConnectRoute) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && deviceSettings.biometricLockEnabled) {
                isBiometricLocked = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                session.refreshTodayDateKey()
                if (isOnHealthConnectRoute || !HealthConnectManager.getInstance(context).isClientInitialized) {
                    hcVm.refreshHealthConnectStatus(autoSyncIfGranted = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            session.consumeSnackbar()
        }
    }

    val showPrimaryNavigation = WellPlusRoute.isTopLevelTab(currentRoute) &&
        activeCameraRequest == null &&
        !needsOnboarding &&
        !isBiometricLocked &&
        profileState !is CloudUiState.Loading

    val activeMessages = (activeMessagesState as? CloudUiState.Success)?.data.orEmpty()
    val checkIns = (checkInsState as? CloudUiState.Success)?.data.orEmpty()
    val medications = (medicationsState as? CloudUiState.Success)?.data.orEmpty()
    val vitalsHead = (vitalsHeadListState as? CloudUiState.Success)?.data.orEmpty()
    val todayCheckIn = remember(checkIns, todayDateKey) {
        checkIns.firstOrNull { it.dateKey == todayDateKey }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && showPrimaryNavigation) {
                    WellPlusBottomBar(
                        currentRoute = currentRoute,
                        onSelectTab = ::navigateToTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && showPrimaryNavigation) {
                    WellPlusNavigationRail(
                        currentRoute = currentRoute,
                        onSelectTab = ::navigateToTab
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize()
                    ) {
                        val cameraReq = activeCameraRequest
                        if (!isBiometricLocked && cameraReq == null) {
                            OfflineStatusBanner(
                                isOnline = isOnline,
                                urgentSafetyNotice = session.urgentSafetyNotice
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            when {
                                isBiometricLocked -> {
                                    BiometricLockScreen(
                                        onUnlocked = { isBiometricLocked = false }
                                    )
                                }

                                profileState is CloudUiState.Loading -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp, vertical = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        GlassShimmerCard(lines = 3)
                                        GlassShimmerCard(lines = 2)
                                        GlassShimmerCard(lines = 2)
                                    }
                                }

                                needsOnboarding -> {
                                    OnboardingScreen(
                                        defaultDisplayName = user.displayName.orEmpty(),
                                        isSaving = isSavingOnboarding,
                                        onShowMessage = session::showMessage,
                                        onComplete = profileVm::completeOnboarding
                                    )
                                }

                                cameraReq != null -> {
                                    CameraCaptureScreen(
                                        initialGuideMode = cameraReq.guideMode,
                                        onPhotoConfirmed = { uri, bitmap ->
                                            when (cameraReq.target) {
                                                CameraTarget.INTAKE -> pendingIntakeCameraPhoto = uri to bitmap
                                                CameraTarget.FOLLOW_UP_CHAT -> pendingChatCameraPhoto = uri to bitmap
                                                CameraTarget.MEDICATION_SCAN -> pendingRxCameraBitmap = bitmap
                                                CameraTarget.LAB_BIOMARKER_SCAN -> pendingLabCameraBitmap = bitmap
                                            }
                                            activeCameraRequest = null
                                        },
                                        onClose = { activeCameraRequest = null }
                                    )
                                }

                                else -> NavHost(
                                    navController = navController,
                                    startDestination = WellPlusRoute.Today.route,
                                    enterTransition = { EnterTransition.None },
                                    exitTransition = { ExitTransition.None },
                                    popEnterTransition = { EnterTransition.None },
                                    popExitTransition = { ExitTransition.None }
                                ) {
                                    composable(WellPlusRoute.Today.route) {
                                        TodayScreen(
                                            displayName = user.displayName.orEmpty(),
                                            todayDateKey = todayDateKey,
                                            profileState = profileState,
                                            consultationsState = consultationsHeadListState,
                                            checkInsState = checkInsState,
                                            careTasksState = careTasksState,
                                            medicationsState = medicationsState,
                                            medicationLogsState = medicationLogsState,
                                            vitalsState = vitalsHeadListState,
                                            mentalAssessmentsState = mentalAssessmentsState,
                                            bodyMeasurementsState = bodyMeasurementsState,
                                            cycleLogsState = cycleLogsState,
                                            cycleTrackingEnabled = deviceSettings.cycleTrackingEnabled,
                                            typicalCycleLengthDays = deviceSettings.typicalCycleLengthDays,
                                            useMetricBodyUnits = deviceSettings.useMetricBodyUnits,
                                            healthRegion = activeHealthRegion,
                                            healthConnectState = healthConnectState,
                                            summaryInsightState = summaryInsightState,
                                            cloudAiEnabled = deviceSettings.cloudAiEnabled,
                                            aiQuotaRemaining = aiQuotaRemaining,
                                            isSavingCheckIn = isSavingCheckIn,
                                            onQuickLogMood = { mood ->
                                                val status = when (mood) {
                                                    WellnessMoodOption.THRIVING -> GeneralHealthStatus.FEELING_GREAT
                                                    WellnessMoodOption.CALM,
                                                    WellnessMoodOption.STEADY -> GeneralHealthStatus.MOSTLY_WELL
                                                    WellnessMoodOption.TENSE -> GeneralHealthStatus.MILD_DISCOMFORT
                                                    WellnessMoodOption.LOW -> GeneralHealthStatus.FATIGUED
                                                }
                                                val existingDecoded = todayCheckIn?.notes
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let { WellnessNoteCodec.decode(it) }
                                                val encodedNote = WellnessNoteCodec.encode(
                                                    status = status,
                                                    signals = existingDecoded?.signals ?: emptySet(),
                                                    reflection = existingDecoded?.reflection.orEmpty()
                                                )
                                                val existingActivity = todayCheckIn?.parsedActivityLevel
                                                    ?: PhysicalActivityLevel.MODERATE
                                                val existingActivityMin = todayCheckIn?.activityMinutes?.toInt()
                                                    ?: existingActivity.defaultMinutes
                                                todayVm.saveTodayCheckIn(
                                                    moodLevel = mood.level,
                                                    energyLevel = todayCheckIn?.energyLevel?.toInt()?.coerceIn(1, 5)
                                                        ?: mood.level.coerceIn(1, 5),
                                                    sleepHours = todayCheckIn?.sleepHours ?: 7.5,
                                                    hydrationGlasses = todayCheckIn?.hydrationGlasses?.toInt() ?: 6,
                                                    notes = encodedNote,
                                                    activityLevel = existingActivity,
                                                    activityMinutes = existingActivityMin
                                                )
                                            },
                                            onGenerateSummaryInsight = todayVm::generateSummaryMicroInsight,
                                            onDismissSummaryInsight = todayVm::clearSummaryMicroInsight,
                                            onStartSymptomCheck = {
                                                consultVm.startNewSymptomCheck()
                                                navigateToTab(WellPlusTab.CONSULT)
                                            },
                                            onStartPhotoAnalysis = {
                                                consultVm.startNewPhotoAnalysis()
                                                navigateToTab(WellPlusTab.CONSULT)
                                            },
                                            onOpenDailyCheckIn = { navigateToDetail(WellPlusRoute.DailyCheckIn) },
                                            onOpenMentalWellbeing = { navigateToDetail(WellPlusRoute.MentalWellbeing) },
                                            onOpenBodyAndCycle = { navigateToDetail(WellPlusRoute.BodyAndCycle) },
                                            onOpenHealthConnect = { navigateToDetail(WellPlusRoute.HealthConnect) },
                                            onOpenMedications = { navigateToTab(WellPlusTab.MEDS) },
                                            onLogMedicationDose = medsVm::logMedicationDose,
                                            onOpenVitals = { navigateToDetail(WellPlusRoute.Vitals) },
                                            onOpenPdfExport = { showPdfExportDialog = true },
                                            onQuickUpdateFollowUp = { consultationId, status ->
                                                consultVm.updateConsultationFollowUp(consultationId, status, "")
                                            },
                                            onOpenProfile = { navigateToTab(WellPlusTab.PROFILE) },
                                            onOpenConsultation = { id ->
                                                consultVm.openConsultation(id, openChatDirectly = false)
                                                navigateToTab(WellPlusTab.CONSULT)
                                            },
                                            onAddCareTask = todayVm::addCustomCareTask,
                                            onToggleCareTask = todayVm::toggleCareTask,
                                            onDeleteCareTask = todayVm::deleteCareTask,
                                            onReportAiResponse = reportAiResponse
                                        )
                                    }

                                    composable(WellPlusRoute.Meds.route) {
                                        MedicationsScreen(
                                            todayDateKey = todayDateKey,
                                            profile = currentProfile,
                                            medicationsState = medicationsState,
                                            logsState = medicationLogsState,
                                            interactionState = interactionState,
                                            prescriptionScanState = prescriptionScanState,
                                            remindersEnabled = deviceSettings.medRemindersEnabled,
                                            autoMaskPiiBeforeScan = deviceSettings.autoMaskPiiBeforeScan,
                                            pendingRxCameraBitmap = pendingRxCameraBitmap,
                                            onConsumePendingRxCameraBitmap = { pendingRxCameraBitmap = null },
                                            showBackButton = false,
                                            onToggleReminders = medsVm::setMedicationRemindersEnabled,
                                            onSendTestReminder = medsVm::sendTestMedicationReminder,
                                            onRunInteractionCheck = medsVm::runMedicationInteractionCheck,
                                            onLaunchRxCamera = {
                                                activeCameraRequest = ActiveCameraRequest(
                                                    target = CameraTarget.MEDICATION_SCAN,
                                                    guideMode = CameraGuideMode.LAB_DOCUMENT
                                                )
                                            },
                                            onScanPrescriptionPhoto = medsVm::scanPrescriptionBottle,
                                            onClearPrescriptionScan = medsVm::clearPrescriptionScan,
                                            onShowMessage = session::showMessage,
                                            onSaveMedication = medsVm::saveMedication,
                                            onToggleActive = medsVm::toggleMedicationActive,
                                            onDeleteMedication = medsVm::deleteMedication,
                                            onLogDose = medsVm::logMedicationDose,
                                            onReportAiResponse = reportAiResponse
                                        )
                                    }

                                    composable(WellPlusRoute.Consult.route) {
                                        val selected = activeConsultation
                                        when {
                                            consultViewMode == ConsultViewMode.CHAT && selected != null -> {
                                                FollowUpChatView(
                                                    consultation = selected,
                                                    messagesState = activeMessagesState,
                                                    chatState = chatState,
                                                    proposedActions = proposedChatActions,
                                                    citations = consultCitations,
                                                    autoMaskPiiBeforeScan = deviceSettings.autoMaskPiiBeforeScan,
                                                    onConfirmProposedAction = consultVm::confirmProposedAction,
                                                    onDismissProposedAction = consultVm::dismissProposedAction,
                                                    pendingCameraPhoto = pendingChatCameraPhoto,
                                                    onConsumePendingCameraPhoto = { pendingChatCameraPhoto = null },
                                                    onOpenCamera = {
                                                        activeCameraRequest = ActiveCameraRequest(
                                                            target = CameraTarget.FOLLOW_UP_CHAT,
                                                            guideMode = CameraGuideMode.SYMPTOM
                                                        )
                                                    },
                                                    onShowMessage = session::showMessage,
                                                    onBackToReport = { consultVm.switchConsultMode(ConsultViewMode.REPORT) },
                                                    onSendMessage = { text, bmp ->
                                                        consultVm.sendFollowUpMessage(selected, text, bmp)
                                                    },
                                                    onReportAiResponse = reportAiResponse
                                                )
                                            }

                                            consultViewMode == ConsultViewMode.REPORT && selected != null -> {
                                                ConsultationReportView(
                                                    consultation = selected,
                                                    addedSuggestionKeys = addedSuggestionKeys,
                                                    messageCount = activeMessages.size,
                                                    healthRegion = activeHealthRegion,
                                                    citations = consultCitations,
                                                    onShowMessage = session::showMessage,
                                                    onBackToIntake = { consultVm.switchConsultMode(ConsultViewMode.INTAKE) },
                                                    onOpenFollowUpChat = { consultVm.switchConsultMode(ConsultViewMode.CHAT) },
                                                    onAddSuggestionToChecklist = { suggestion ->
                                                        consultVm.addSuggestionToCareChecklist(selected.id, suggestion)
                                                    },
                                                    onOpenEmergencySheet = {
                                                        consultVm.openEmergencySheetFor(selected)
                                                    },
                                                    onUpdateFollowUp = { status, note ->
                                                        consultVm.updateConsultationFollowUp(selected.id, status, note)
                                                    },
                                                    onScheduleFollowUpReminder = {
                                                        consultVm.scheduleFollowUpNudge(selected)
                                                    },
                                                    worseningReassessmentState = worseningReassessmentState,
                                                    onRunWorseningReassessment = { note ->
                                                        consultVm.runWorseningReassessment(selected, note)
                                                    },
                                                    onDismissWorseningReassessment = consultVm::dismissWorseningReassessment,
                                                    onReportAiResponse = reportAiResponse
                                                )
                                            }

                                            else -> {
                                                GuidedIntakeScreen(
                                                    draft = intakeDraft,
                                                    profile = currentProfile,
                                                    analysisState = analysisState,
                                                    autoMaskPiiBeforeScan = deviceSettings.autoMaskPiiBeforeScan,
                                                    pendingCameraPhoto = pendingIntakeCameraPhoto,
                                                    onConsumePendingCameraPhoto = { pendingIntakeCameraPhoto = null },
                                                    clarifyingQuestionsState = clarifyingQuestionsState,
                                                    onGenerateClarifyingQuestions = consultVm::generateClarifyingQuestions,
                                                    onAnswerClarifyingQuestion = consultVm::answerClarifyingQuestion,
                                                    onClearClarifyingQuestions = consultVm::clearClarifyingQuestions,
                                                    onUpdateDraft = consultVm::updateIntake,
                                                    onToggleAssociatedSymptom = consultVm::toggleAssociatedSymptom,
                                                    onOpenCamera = { guideMode ->
                                                        activeCameraRequest = ActiveCameraRequest(
                                                            target = CameraTarget.INTAKE,
                                                            guideMode = guideMode
                                                        )
                                                    },
                                                    onPhotoSelected = consultVm::setIntakePhoto,
                                                    onShowMessage = session::showMessage,
                                                    onSubmit = consultVm::submitIntakeForAnalysis
                                                )
                                            }
                                        }
                                    }

                                    composable(WellPlusRoute.History.route) {
                                        HistoryScreen(
                                            todayDateKey = todayDateKey,
                                            consultationsState = paginatedConsultationsState,
                                            checkInsState = checkInsState,
                                            medications = medications,
                                            vitals = vitalsHead,
                                            insightsState = insightsState,
                                            onGenerateInsights = historyVm::generateLongitudinalInsights,
                                            onOpenPdfExport = { showPdfExportDialog = true },
                                            onOpenConsultation = { id, openChat ->
                                                consultVm.openConsultation(
                                                    consultationId = id,
                                                    openChatDirectly = openChat,
                                                    knownConsultation = historyVm.findLoadedConsultation(id)
                                                )
                                                navigateToTab(WellPlusTab.CONSULT)
                                            },
                                            onDeleteConsultation = { id ->
                                                historyVm.deleteConsultation(id, consultVm::onConsultationDeleted)
                                            },
                                            onStartNewConsult = {
                                                consultVm.startNewSymptomCheck()
                                                navigateToTab(WellPlusTab.CONSULT)
                                            },
                                            onOpenDailyCheckIn = { navigateToDetail(WellPlusRoute.DailyCheckIn) },
                                            onReportAiResponse = reportAiResponse,
                                            paginationState = historyPaginationState,
                                            onLoadMore = historyVm::loadMoreConsultations
                                        )
                                    }

                                    composable(WellPlusRoute.Profile.route) {
                                        ProfileScreen(
                                            accountEmail = user.email.orEmpty(),
                                            accountPhotoUrl = user.photoUrl?.toString(),
                                            defaultDisplayName = user.displayName.orEmpty(),
                                            profileState = profileState,
                                            healthRegion = activeHealthRegion,
                                            deviceSettings = deviceSettings,
                                            subscriptionState = subscriptionState,
                                            aiQuotaRemaining = aiQuotaRemaining,
                                            aiDailyLimit = aiDailyLimit,
                                            isExportingJson = isExportingJson,
                                            isDeletingAccount = isDeletingAccount,
                                            themeMode = themeMode,
                                            onThemeModeChange = onThemeModeChange,
                                            onSelectHealthRegion = profileVm::setHealthRegion,
                                            onToggleBiometricLock = profileVm::setBiometricLockEnabled,
                                            onToggleScreenPrivacy = profileVm::setScreenPrivacyEnabled,
                                            onToggleCloudAi = profileVm::setCloudAiEnabled,
                                            onToggleAutoMaskPii = profileVm::setAutoMaskPiiBeforeScan,
                                            onOpenWellPlusPro = { navigateToDetail(WellPlusRoute.WellPlusPro) },
                                            onOpenEmergencyMedicalId = { navigateToDetail(WellPlusRoute.EmergencyMedicalId) },
                                            onSelectEmergencyRegion = profileVm::setEmergencyRegionOverride,
                                            onToggleCheckInReminder = { enabled -> profileVm.setCheckInReminder(enabled) },
                                            onToggleMedReminders = profileVm::setMedicationRemindersEnabled,
                                            onToggleFollowUpReminders = profileVm::setFollowUpRemindersEnabled,
                                            onOpenPdfExport = { showPdfExportDialog = true },
                                            onExportJsonData = profileVm::exportAllPersonalDataJson,
                                            onShowMessage = session::showMessage,
                                            onSaveProfile = profileVm::saveHealthProfile,
                                            onDeleteAllDataAndAccount = {
                                                profileVm.deleteAllDataAndPrepareSignOut {
                                                    scope.launch {
                                                        container.widgetSnapshotStore.clear()
                                                        WidgetRefreshBridge.notifyDataChanged(context.applicationContext)
                                                        try {
                                                            user.delete().await()
                                                        } catch (_: Exception) {
                                                            // Cloud data is already wiped; recent-login requirement may skip Auth deletion
                                                        } finally {
                                                            signOutUser(
                                                                credentialManager = credentialManager,
                                                                scope = scope
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onSignOut = {
                                                container.widgetSnapshotStore.clear()
                                                WidgetRefreshBridge.notifyDataChanged(context.applicationContext)
                                                signOutUser(
                                                    credentialManager = credentialManager,
                                                    scope = scope
                                                )
                                            }
                                        )
                                    }

                                    composable(WellPlusRoute.DailyCheckIn.route) {
                                        DailyCheckInScreen(
                                            todayDateKey = todayDateKey,
                                            existingToday = todayCheckIn,
                                            checkInsState = checkInsState,
                                            isSaving = isSavingCheckIn,
                                            onBack = { navController.popBackStack() },
                                            onSaveCheckIn = { mood, energy, sleep, hydration, activityLevel, activityMinutes, notes ->
                                                todayVm.saveTodayCheckIn(
                                                    moodLevel = mood,
                                                    energyLevel = energy,
                                                    sleepHours = sleep,
                                                    hydrationGlasses = hydration,
                                                    notes = notes,
                                                    activityLevel = activityLevel,
                                                    activityMinutes = activityMinutes,
                                                    onSaved = { navController.popBackStack() }
                                                )
                                            }
                                        )
                                    }

                                    composable(WellPlusRoute.Vitals.route) {
                                        VitalsScreen(
                                            vitalsState = paginatedVitalsState,
                                            region = activeHealthRegion,
                                            onBack = { navController.popBackStack() },
                                            onAddVital = { type, primary, secondary, note ->
                                                vitalsVm.addVitalReading(
                                                    vitalType = type,
                                                    primaryValue = primary,
                                                    secondaryValue = secondary,
                                                    note = note,
                                                    unit = type.unitFor(activeHealthRegion)
                                                )
                                            },
                                            onDeleteVital = vitalsVm::deleteVitalReading,
                                            paginationState = vitalsPaginationState,
                                            onLoadMore = vitalsVm::loadMoreVitals,
                                            labBiomarkersState = labBiomarkersState,
                                            labScanState = labScanState,
                                            isSavingLabBatch = isSavingLabBatch,
                                            autoMaskPiiBeforeScan = deviceSettings.autoMaskPiiBeforeScan,
                                            pendingLabCameraBitmap = pendingLabCameraBitmap,
                                            onConsumePendingLabCameraBitmap = { pendingLabCameraBitmap = null },
                                            onLaunchLabCamera = {
                                                activeCameraRequest = ActiveCameraRequest(
                                                    target = CameraTarget.LAB_BIOMARKER_SCAN,
                                                    guideMode = CameraGuideMode.LAB_DOCUMENT
                                                )
                                            },
                                            onScanLabBitmap = vitalsVm::scanLabReportPhoto,
                                            onToggleExtractedMarker = vitalsVm::toggleExtractedBiomarker,
                                            onSaveExtractedMarkers = vitalsVm::saveExtractedBiomarkers,
                                            onClearLabScan = vitalsVm::clearLabScan,
                                            onAddManualLabBiomarker = vitalsVm::addManualLabBiomarker,
                                            onDeleteLabBiomarker = vitalsVm::deleteLabBiomarker,
                                            abnormalVitalForReassessment = abnormalVitalForReassessment,
                                            vitalReassessmentState = vitalReassessmentState,
                                            onRunAbnormalVitalReassessment = vitalsVm::runAbnormalVitalReassessment,
                                            onDismissVitalReassessment = vitalsVm::dismissVitalReassessment,
                                            onShowMessage = session::showMessage,
                                            onReportAiResponse = reportAiResponse
                                        )
                                    }

                                    composable(WellPlusRoute.MentalWellbeing.route) {
                                        MentalWellbeingScreen(
                                            profile = currentProfile,
                                            assessmentsState = mentalAssessmentsState,
                                            reflectionState = mentalReflectionState,
                                            recentCheckIns = checkIns,
                                            cloudAiEnabled = deviceSettings.cloudAiEnabled,
                                            aiQuotaRemaining = aiQuotaRemaining,
                                            emergencyRegionOverride = deviceSettings.emergencyRegionOverride,
                                            onBack = { navController.popBackStack() },
                                            onSubmitAssessment = mentalVm::submitMentalAssessment,
                                            onRegenerateReflection = mentalVm::regenerateMentalReflection,
                                            onAddCopingStep = mentalVm::addCopingStep,
                                            onDeleteAssessment = mentalVm::deleteMentalAssessment,
                                            onReportAiResponse = reportAiResponse,
                                            onShowMessage = session::showMessage
                                        )
                                    }

                                    composable(WellPlusRoute.BodyAndCycle.route) {
                                        BodyAndCycleScreen(
                                            todayDateKey = todayDateKey,
                                            measurementsState = bodyMeasurementsState,
                                            cycleLogsState = cycleLogsState,
                                            insightState = bodyCycleInsightState,
                                            cycleTrackingEnabled = deviceSettings.cycleTrackingEnabled,
                                            typicalCycleLengthDays = deviceSettings.typicalCycleLengthDays,
                                            useMetric = deviceSettings.useMetricBodyUnits,
                                            onBack = { navController.popBackStack() },
                                            onToggleUseMetric = bodyCycleVm::setUseMetricBodyUnits,
                                            onToggleCycleTracking = bodyCycleVm::setCycleTrackingEnabled,
                                            onUpdateTypicalCycleLength = bodyCycleVm::setTypicalCycleLengthDays,
                                            onAddMeasurement = bodyCycleVm::addBodyMeasurement,
                                            onDeleteMeasurement = bodyCycleVm::deleteBodyMeasurement,
                                            onSaveCycleLog = bodyCycleVm::saveCycleLog,
                                            onDeleteCycleLog = bodyCycleVm::deleteCycleLog,
                                            onGenerateAiInsight = bodyCycleVm::generateBodyAndCycleInsight,
                                            onAddRecommendationToChecklist = bodyCycleVm::addRecommendationToChecklist,
                                            onReportAiResponse = reportAiResponse
                                        )
                                    }

                                    composable(WellPlusRoute.HealthConnect.route) {
                                        HealthConnectScreen(
                                            hcState = healthConnectState,
                                            aiState = healthConnectAiState,
                                            backgroundSyncEnabled = backgroundHcSyncEnabled,
                                            onToggleBackgroundSync = hcVm::setBackgroundHcSyncEnabled,
                                            onBack = { navController.popBackStack() },
                                            onRefreshStatus = {
                                                hcVm.refreshHealthConnectStatus(autoSyncIfGranted = true)
                                            },
                                            onPermissionsResult = hcVm::onHealthConnectPermissionsResult,
                                            onSyncNow = { hcVm.syncHealthConnectNow(silent = false) },
                                            onImportHeartRateToVitals = hcVm::importHealthConnectHeartRateToVitals,
                                            onImportBloodPressureToVitals = hcVm::importHealthConnectBloodPressureToVitals,
                                            onWriteSteps = hcVm::writeStepsToHealthConnect,
                                            onWriteHeartRate = hcVm::writeHeartRateToHealthConnect,
                                            onWriteSampleData = hcVm::writeSampleDataToHealthConnect,
                                            onGenerateAiBrief = hcVm::generateHealthConnectAiBrief,
                                            onAddTipToChecklist = hcVm::addTipToChecklist,
                                            onReportAiResponse = reportAiResponse,
                                            onShowMessage = session::showMessage
                                        )
                                    }

                                    composable(WellPlusRoute.WellPlusPro.route) {
                                        WellPlusProScreen(
                                            subscriptionState = subscriptionState,
                                            aiQuotaRemaining = aiQuotaRemaining,
                                            aiDailyLimit = aiDailyLimit,
                                            onBack = { navController.popBackStack() },
                                            onPurchasePlan = { activity, offer ->
                                                container.billingGateway.launchPurchase(activity, offer)
                                            },
                                            onRestorePurchases = {
                                                container.billingGateway.restorePurchases()
                                            },
                                            onShowMessage = session::showMessage
                                        )
                                    }

                                    composable(WellPlusRoute.EmergencyMedicalId.route) {
                                        EmergencyMedicalIdScreen(
                                            profile = currentProfile,
                                            defaultDisplayName = user.displayName.orEmpty(),
                                            activeMedications = medications,
                                            healthRegion = activeHealthRegion,
                                            emergencyRegionOverride = deviceSettings.emergencyRegionOverride,
                                            showOnHomeWidget = deviceSettings.showEmergencyIdOnWidget,
                                            onToggleShowOnWidget = profileVm::setShowEmergencyIdOnWidget,
                                            onBack = { navController.popBackStack() },
                                            onShowMessage = session::showMessage
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    WellPlusGlobalOverlays(
        context = context,
        isBiometricLocked = isBiometricLocked,
        currentProfile = currentProfile,
        activeHealthRegion = activeHealthRegion,
        emergencyRegionOverride = deviceSettings.emergencyRegionOverride,
        emergencyAlertConsultation = emergencyAlertConsultation,
        activeCriticalVitalAlert = activeCriticalVitalAlert,
        activeSymptomRedFlag = activeSymptomRedFlag,
        pendingAiReport = pendingAiReport,
        isSubmittingSafetyReport = isSubmittingSafetyReport,
        showPdfExportDialog = showPdfExportDialog,
        isExportingPdf = isExportingPdf,
        isSynthesizingSoap = isSynthesizingSoap,
        onShowMessage = session::showMessage,
        onDismissEmergencyAlert = consultVm::dismissEmergencyAlert,
        onDismissCriticalVitalAlert = session::dismissCriticalVitalAlert,
        onDismissSymptomRedFlag = session::dismissSymptomRedFlag,
        onDismissAiReport = { pendingAiReport = null },
        onSubmitAiReport = { area, reason, snippet, note, onDone ->
            session.submitAiSafetyReport(
                featureArea = area,
                reason = reason,
                contentSnippet = snippet,
                userNote = note,
                onSubmitted = onDone
            )
        },
        onDismissPdfDialog = { showPdfExportDialog = false },
        onConfirmPdfExport = historyVm::exportDoctorVisitPdf
    )
}
