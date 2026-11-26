package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.FreeSmartSuggestionsEngine
import com.example.core.HealthHighlightsEngine
import com.example.core.SmartSuggestionActionTarget
import com.example.data.model.AiFeatureArea
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareCategory
import com.example.data.model.CareTask
import com.example.data.model.Consultation
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.FollowUpStatus
import com.example.data.model.HealthHighlightItem
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MentalAssessment
import com.example.data.model.VitalReading
import com.example.ui.components.FreeSmartSuggestionsCard
import com.example.ui.components.GlassProfileAvatar
import com.example.ui.components.MedicalDisclaimerNote
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.UrgencyBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.HealthConnectUiState
import com.example.ui.viewmodel.SummaryInsightUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    displayName: String,
    todayDateKey: String,
    profileState: CloudUiState<HealthProfile?>,
    consultationsState: CloudUiState<List<Consultation>>,
    checkInsState: CloudUiState<List<DailyCheckIn>>,
    careTasksState: CloudUiState<List<CareTask>>,
    medicationsState: CloudUiState<List<MedicationItem>> = CloudUiState.Success(emptyList()),
    medicationLogsState: CloudUiState<List<MedicationLog>> = CloudUiState.Success(emptyList()),
    vitalsState: CloudUiState<List<VitalReading>> = CloudUiState.Success(emptyList()),
    mentalAssessmentsState: CloudUiState<List<MentalAssessment>> = CloudUiState.Success(emptyList()),
    bodyMeasurementsState: CloudUiState<List<BodyMeasurement>> = CloudUiState.Success(emptyList()),
    cycleLogsState: CloudUiState<List<CycleLog>> = CloudUiState.Success(emptyList()),
    cycleTrackingEnabled: Boolean = false,
    typicalCycleLengthDays: Int = 28,
    useMetricBodyUnits: Boolean = false,
    healthRegion: com.example.core.HealthRegionProfile = com.example.core.HealthRegionDirectory.US,
    healthConnectState: HealthConnectUiState = HealthConnectUiState(),
    summaryInsightState: SummaryInsightUiState = SummaryInsightUiState.Idle,
    cloudAiEnabled: Boolean = true,
    aiQuotaRemaining: Int = 25,
    isSavingCheckIn: Boolean = false,
    onQuickLogMood: (WellnessMoodOption) -> Unit = {},
    onGenerateSummaryInsight: (List<HealthHighlightItem>) -> Unit = {},
    onDismissSummaryInsight: () -> Unit = {},
    onStartSymptomCheck: () -> Unit,
    onStartPhotoAnalysis: () -> Unit,
    onOpenDailyCheckIn: () -> Unit,
    onOpenMentalWellbeing: () -> Unit = {},
    onOpenBodyAndCycle: () -> Unit = {},
    onOpenHealthConnect: () -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onLogMedicationDose: (MedicationItem, MedicationLogStatus) -> Unit = { _, _ -> },
    onOpenVitals: () -> Unit = {},
    onOpenPdfExport: () -> Unit = {},
    onQuickUpdateFollowUp: (String, FollowUpStatus) -> Unit = { _, _ -> },
    onOpenProfile: () -> Unit,
    onOpenConsultation: (String) -> Unit,
    onAddCareTask: (String, CareCategory) -> Unit,
    onToggleCareTask: (CareTask) -> Unit,
    onDeleteCareTask: (String) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    val sanaColors = LocalSanaColors.current
    val profile = (profileState as? CloudUiState.Success)?.data
    val consultations = (consultationsState as? CloudUiState.Success)?.data.orEmpty()
    val checkIns = (checkInsState as? CloudUiState.Success)?.data.orEmpty()
    val careTasks = (careTasksState as? CloudUiState.Success)?.data.orEmpty()
    val medications = (medicationsState as? CloudUiState.Success)?.data.orEmpty()
    val medicationLogs = (medicationLogsState as? CloudUiState.Success)?.data.orEmpty()
    val vitals = (vitalsState as? CloudUiState.Success)?.data.orEmpty()
    val mentalAssessments = (mentalAssessmentsState as? CloudUiState.Success)?.data.orEmpty()
    val bodyMeasurements = (bodyMeasurementsState as? CloudUiState.Success)?.data.orEmpty()
    val cycleLogs = (cycleLogsState as? CloudUiState.Success)?.data.orEmpty()

    val todayCheckIn = remember(checkIns, todayDateKey) {
        checkIns.firstOrNull { it.dateKey == todayDateKey }
    }
    val pendingFollowUpConsultation = remember(consultations) {
        consultations.firstOrNull { it.parsedFollowUpStatus == FollowUpStatus.PENDING }
    }
    val computedHighlights = remember(
        todayDateKey,
        checkIns,
        vitals,
        medications,
        medicationLogs,
        mentalAssessments
    ) {
        HealthHighlightsEngine.computeHighlights(
            todayDateKey = todayDateKey,
            checkIns = checkIns,
            vitals = vitals,
            medications = medications,
            medicationLogs = medicationLogs,
            mentalAssessments = mentalAssessments
        )
    }
    val freeSuggestions = remember(
        todayDateKey,
        todayCheckIn,
        checkIns,
        vitals,
        medications,
        medicationLogs,
        mentalAssessments,
        healthConnectState.snapshot,
        healthRegion
    ) {
        FreeSmartSuggestionsEngine.computeDailySuggestions(
            todayDateKey = todayDateKey,
            todayCheckIn = todayCheckIn,
            recentCheckIns = checkIns,
            vitals = vitals,
            medications = medications,
            medicationLogs = medicationLogs,
            mentalAssessments = mentalAssessments,
            healthConnectSnapshot = healthConnectState.snapshot,
            region = healthRegion
        )
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showMedicalIdDialog by remember { mutableStateOf(false) }

    val formattedDateEyebrow = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()).uppercase(Locale.US)
    }
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val firstName = (profile?.displayName?.takeIf { it.isNotBlank() } ?: displayName)
        .substringBefore(" ")
        .ifBlank { "there" }
    val avatarInitial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "W"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Calm Editorial Header + Quick ICE Pill + Profile Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$formattedDateEyebrow  •  ${healthRegion.code.shortCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$greeting, $firstName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(CircleShape)
                            .clickable { showMedicalIdDialog = true }
                            .testTag("home_header_medical_id_pill"),
                        color = sanaColors.heartPink.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalHospital,
                                contentDescription = "Open Medical ID",
                                tint = sanaColors.heartPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ID",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = sanaColors.heartPink
                            )
                        }
                    }

                    GlassProfileAvatar(
                        initials = avatarInitial,
                        contentDescriptionText = "Open Profile & Settings",
                        visualSize = 44.dp,
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("home_profile_avatar_button")
                    )
                }
            }
        }

        // 2. Hero Daily Wellness Ring Card (Habit Loop Anchor + 7-Day Strip + 1-Tap Mood + PNG Share)
        item {
            if (checkInsState is CloudUiState.Error) {
                SanctuaryErrorBanner(message = checkInsState.message)
            } else {
                DailyWellnessRingHeroCard(
                    todayDateKey = todayDateKey,
                    todayCheckIn = todayCheckIn,
                    checkIns = checkIns,
                    isSavingCheckIn = isSavingCheckIn,
                    medications = medications,
                    medicationLogs = medicationLogs,
                    careTasks = careTasks,
                    stepsToday = healthConnectState.snapshot?.stepsToday,
                    onQuickLogMood = onQuickLogMood,
                    onOpenCheckIn = onOpenDailyCheckIn,
                    onOpenMedications = onOpenMedications
                )
            }
        }

        // 3. Quick Action Pill Rail
        item {
            QuickActionPillRail(
                onStartSymptomCheck = onStartSymptomCheck,
                onStartPhotoAnalysis = onStartPhotoAnalysis,
                onOpenVitals = onOpenVitals,
                onOpenDailyCheckIn = onOpenDailyCheckIn,
                onOpenMedications = onOpenMedications,
                onOpenPdfExport = onOpenPdfExport
            )
        }

        // 4A. Contextual Priority: Personalize Profile Banner (shown only until clinical basics added)
        if (profileState is CloudUiState.Success && (profile == null || !profile.hasClinicalContext)) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenProfile)
                        .testTag("complete_profile_banner"),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Complete your health profile",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Add allergies, coverage, or conditions for Medical ID & PDFs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open profile",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // 4B. Contextual Priority: Pending Symptom Follow-Up Check-In
        if (pendingFollowUpConsultation != null) {
            item {
                SanctuaryCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                    modifier = Modifier.testTag("home_pending_followup_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Symptom Follow-Up Check-In",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How is \"${pendingFollowUpConsultation.title}\" feeling now?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            FollowUpStatus.IMPROVING,
                            FollowUpStatus.UNCHANGED,
                            FollowUpStatus.WORSENING,
                            FollowUpStatus.RESOLVED
                        ).forEach { status ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onQuickUpdateFollowUp(pendingFollowUpConsultation.id, status)
                                },
                                label = { Text(status.label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("home_followup_${status.name}")
                            )
                        }
                    }
                }
            }
        }

        // 4C. Contextual Priority: Due Today 1-Tap Medication Logger (renders only when doses are pending)
        if (medicationsState is CloudUiState.Error) {
            item { SanctuaryErrorBanner(message = medicationsState.message) }
        } else {
            item {
                PendingMedsQuickLogCard(
                    todayDateKey = todayDateKey,
                    medications = medications,
                    logs = medicationLogs,
                    onLogDose = onLogMedicationDose,
                    onOpenMedications = onOpenMedications
                )
            }
        }

        // 4D. Free Forever On-Device Smart Suggestions (Zero AI Cost, Works Offline)
        if (freeSuggestions.isNotEmpty()) {
            item {
                FreeSmartSuggestionsCard(
                    suggestions = freeSuggestions,
                    onAddToChecklist = onAddCareTask,
                    onActionTarget = { target ->
                        when (target) {
                            SmartSuggestionActionTarget.CHECK_IN -> onOpenDailyCheckIn()
                            SmartSuggestionActionTarget.VITALS -> onOpenVitals()
                            SmartSuggestionActionTarget.MEDICATIONS -> onOpenMedications()
                            SmartSuggestionActionTarget.MENTAL_WELLBEING -> onOpenMentalWellbeing()
                            SmartSuggestionActionTarget.DOCTOR_PDF -> onOpenPdfExport()
                            SmartSuggestionActionTarget.NONE -> Unit
                        }
                    }
                )
            }
        }

        // 5. Automatic 7-Day Highlights & Optional On-Demand AI Micro-Insight
        item {
            SummaryHighlightsTrendsSection(
                highlights = computedHighlights,
                insightState = summaryInsightState,
                cloudAiEnabled = cloudAiEnabled,
                aiQuotaRemaining = aiQuotaRemaining,
                onGenerateMicroInsight = onGenerateSummaryInsight,
                onDismissMicroInsight = onDismissSummaryInsight,
                onAddActionToChecklist = onAddCareTask,
                onOpenCheckIn = onOpenDailyCheckIn,
                onOpenMedications = onOpenMedications,
                onOpenVitals = onOpenVitals,
                onOpenMentalWellbeing = onOpenMentalWellbeing,
                onReportAiResponse = onReportAiResponse
            )
        }

        // 6. Compact 2x3 Health Hub Bento Grid (Replaces 6 Verbose Stacked Module Cards)
        item {
            val moduleError = listOf(vitalsState, mentalAssessmentsState, bodyMeasurementsState)
                .filterIsInstance<CloudUiState.Error>()
                .firstOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (moduleError != null) {
                    SanctuaryErrorBanner(message = moduleError.message)
                }
                HealthHubBentoGrid(
                    todayDateKey = todayDateKey,
                    profile = profile,
                    vitals = vitals,
                    medications = medications,
                    medicationLogs = medicationLogs,
                    mentalAssessments = mentalAssessments,
                    bodyMeasurements = bodyMeasurements,
                    cycleLogs = cycleLogs,
                    cycleTrackingEnabled = cycleTrackingEnabled,
                    useMetricBodyUnits = useMetricBodyUnits,
                    healthConnectState = healthConnectState,
                    healthRegion = healthRegion,
                    onOpenVitals = onOpenVitals,
                    onOpenMedications = onOpenMedications,
                    onOpenHealthConnect = onOpenHealthConnect,
                    onOpenMentalWellbeing = onOpenMentalWellbeing,
                    onOpenBodyAndCycle = onOpenBodyAndCycle,
                    onOpenMedicalId = { showMedicalIdDialog = true }
                )
            }
        }

        // 7. Personal Care Checklist with 1-Tap Starter Habits when Empty
        item {
            if (careTasksState is CloudUiState.Error) {
                SanctuaryErrorBanner(message = careTasksState.message)
            } else {
                CareChecklistSection(
                    tasks = careTasks,
                    onOpenAddDialog = { showAddTaskDialog = true },
                    onQuickAddStarter = onAddCareTask,
                    onToggleTask = onToggleCareTask,
                    onDeleteTask = onDeleteCareTask
                )
            }
        }

        // 8. Latest Consultation Shortcut
        if (consultations.isNotEmpty()) {
            item {
                val latest = consultations.first()
                SanctuaryCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onOpenConsultation(latest.id) }
                        .testTag("home_recent_consultation_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Consultation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatTimestamp(latest.updatedAt ?: latest.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = latest.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = latest.aiSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UrgencyBadge(urgencyLevel = latest.parsedUrgency)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Open report",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            MedicalDisclaimerNote()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAddTaskDialog) {
        AddCareTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, category ->
                onAddCareTask(title, category)
                showAddTaskDialog = false
            }
        )
    }

    if (showMedicalIdDialog) {
        MedicalIdDetailDialog(
            displayName = displayName,
            profile = profile,
            activeMedications = medications,
            healthRegion = healthRegion,
            onDismiss = { showMedicalIdDialog = false },
            onEditProfile = onOpenProfile
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareChecklistSection(
    tasks: List<CareTask>,
    onOpenAddDialog: () -> Unit,
    onQuickAddStarter: (String, CareCategory) -> Unit,
    onToggleTask: (CareTask) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val completedCount = tasks.count { it.completed }
    val starterHabits = remember {
        listOf(
            "Drink 8 glasses of water" to CareCategory.HYDRATION,
            "15-minute brisk walk" to CareCategory.RECOVERY,
            "Screen-free wind-down" to CareCategory.RECOVERY
        )
    }

    SanctuaryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Care Checklist",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (tasks.isEmpty()) {
                        "Tap a starter habit or add your own"
                    } else {
                        "$completedCount of ${tasks.size} completed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onOpenAddDialog,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("add_care_task_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "One-tap daily starters:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    starterHabits.forEachIndexed { idx, (label, category) ->
                        SuggestionChip(
                            onClick = { onQuickAddStarter(label, category) },
                            label = { Text("+ $label", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("starter_habit_chip_$idx")
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (task.completed) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTask(task) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("care_task_row_${task.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { onToggleTask(task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (task.completed) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textDecoration = if (task.completed) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    }
                                )
                                Text(
                                    text = task.parsedCategory.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onDeleteTask(task.id) },
                                modifier = Modifier.testTag("delete_care_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_delete_item),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCareTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, CareCategory) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CareCategory.RECOVERY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Care Task", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 200) title = it },
                    label = { Text("What would you like to remember?") },
                    placeholder = { Text("Take evening magnesium, 15m walk...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_care_task_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CareCategory.RECOVERY,
                        CareCategory.HYDRATION,
                        CareCategory.MEDICATION,
                        CareCategory.CUSTOM
                    ).forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, selectedCategory) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("dialog_save_care_task_button")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
