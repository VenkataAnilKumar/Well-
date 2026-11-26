package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.SanaNotificationChannels
import com.example.core.loadDownscaledBitmap
import com.example.data.model.AiFeatureArea
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.ScannedPrescriptionDraft
import com.example.ui.components.EmptySanctuaryState
import com.example.ui.components.HealthEmptyIllustrationKind
import com.example.ui.components.PiiRedactionPreviewSheet
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.GlassShimmerCard
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.InteractionCheckUiState
import com.example.ui.viewmodel.PrescriptionScanUiState
import kotlinx.coroutines.launch

@Composable
fun TodayMedicationsCard(
    todayDateKey: String,
    medications: List<MedicationItem>,
    logs: List<MedicationLog>,
    onLogDose: (MedicationItem, MedicationLogStatus) -> Unit,
    onOpenMedicationsManager: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val activeMeds = remember(medications) { medications.filter { it.active } }
    val todayLogsByMedId = remember(logs, todayDateKey) {
        logs.filter { it.dateKey == todayDateKey }.associateBy { it.medicationId }
    }
    val takenToday = activeMeds.count { todayLogsByMedId[it.id]?.parsedStatus == MedicationLogStatus.TAKEN }

    SanctuaryCard(
        modifier = Modifier.testTag("home_medications_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenMedicationsManager),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = sanaColors.medsTeal.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = sanaColors.medsTeal,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Medications & Prescriptions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (activeMeds.isEmpty()) {
                            "Log prescriptions & schedule daily intake reminders"
                        } else {
                            "$takenToday of ${activeMeds.size} taken today"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Manage medications",
                tint = sanaColors.medsTeal
            )
        }

        if (activeMeds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (takenToday.toFloat() / activeMeds.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeMeds.take(3).forEach { med ->
                    val status = todayLogsByMedId[med.id]?.parsedStatus
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sanaColors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = med.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        if (med.dosage.isNotBlank()) append("${med.dosage} • ")
                                        append(med.parsedSlot.label)
                                        if (med.parsedSlot != MedicationScheduleSlot.AS_NEEDED) {
                                            append(" • ${med.formattedReminderTime()}")
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    val next = if (status == MedicationLogStatus.TAKEN) {
                                        MedicationLogStatus.SKIPPED
                                    } else {
                                        MedicationLogStatus.TAKEN
                                    }
                                    onLogDose(med, next)
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("home_med_log_${med.id}")
                            ) {
                                Icon(
                                    imageVector = if (status == MedicationLogStatus.TAKEN) {
                                        Icons.Filled.Check
                                    } else {
                                        Icons.Filled.Medication
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (status) {
                                        MedicationLogStatus.TAKEN -> "Taken"
                                        MedicationLogStatus.SKIPPED -> "Skipped"
                                        null -> "Take"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenMedicationsManager,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_medications_screen_button")
            ) {
                Text("Open Medication Tracker (${medications.size})")
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onOpenMedicationsManager,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_medications_screen_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.meds_add_action))
            }
        }
    }
}

@Composable
fun MedicationsScreen(
    todayDateKey: String,
    profile: HealthProfile? = null,
    medicationsState: CloudUiState<List<MedicationItem>>,
    logsState: CloudUiState<List<MedicationLog>>,
    interactionState: InteractionCheckUiState = InteractionCheckUiState.Idle,
    prescriptionScanState: PrescriptionScanUiState = PrescriptionScanUiState.Idle,
    remindersEnabled: Boolean = false,
    autoMaskPiiBeforeScan: Boolean = true,
    pendingRxCameraBitmap: Bitmap? = null,
    onConsumePendingRxCameraBitmap: () -> Unit = {},
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onToggleReminders: (Boolean) -> Unit = {},
    onSendTestReminder: () -> Unit = {},
    onRunInteractionCheck: () -> Unit = {},
    onLaunchRxCamera: () -> Unit = {},
    onScanPrescriptionPhoto: (Bitmap) -> Unit = {},
    onClearPrescriptionScan: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onSaveMedication: (
        existingId: String?,
        name: String,
        dosage: String,
        slot: MedicationScheduleSlot,
        hour: Int,
        minute: Int,
        instructions: String,
        active: Boolean,
        enableRemindersIfRequested: Boolean
    ) -> Unit,
    onToggleActive: (MedicationItem) -> Unit,
    onDeleteMedication: (String) -> Unit,
    onLogDose: (MedicationItem, MedicationLogStatus) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sanaColors = LocalSanaColors.current
    val medications = (medicationsState as? CloudUiState.Success)?.data.orEmpty()
    val logs = (logsState as? CloudUiState.Success)?.data.orEmpty()
    val todayLogs = remember(logs, todayDateKey) {
        logs.filter { it.dateKey == todayDateKey }.associateBy { it.medicationId }
    }

    var editingMedication by remember { mutableStateOf<MedicationItem?>(null) }
    var pendingScannedDraft by remember { mutableStateOf<ScannedPrescriptionDraft?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isDecodingGalleryPhoto by remember { mutableStateOf(false) }
    var pendingPiiRxBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasNotificationPermission by remember(context) {
        mutableStateOf(SanaNotificationChannels.hasNotificationPermission(context))
    }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(pendingRxCameraBitmap) {
        if (pendingRxCameraBitmap != null) {
            if (autoMaskPiiBeforeScan) {
                pendingPiiRxBitmap = pendingRxCameraBitmap
            } else {
                onScanPrescriptionPhoto(pendingRxCameraBitmap)
            }
            onConsumePendingRxCameraBitmap()
        }
    }

    LaunchedEffect(prescriptionScanState) {
        if (prescriptionScanState is PrescriptionScanUiState.Ready) {
            editingMedication = null
            pendingScannedDraft = prescriptionScanState.draft
            showAddDialog = true
        }
    }

    val rxGalleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecodingGalleryPhoto = true
            scope.launch {
                val bitmap = loadDownscaledBitmap(context, uri)
                isDecodingGalleryPhoto = false
                if (bitmap != null) {
                    if (autoMaskPiiBeforeScan) {
                        pendingPiiRxBitmap = bitmap
                    } else {
                        onScanPrescriptionPhoto(bitmap)
                    }
                } else {
                    onShowMessage("Could not load selected image. Please try another photo.")
                }
            }
        }
    }

    pendingPiiRxBitmap?.let { rawBitmap ->
        PiiRedactionPreviewSheet(
            sourceBitmap = rawBitmap,
            scanPurposeTitle = "Prescription Bottle Label Scanner",
            confirmButtonLabel = "Scan Redacted Label",
            onDismiss = { pendingPiiRxBitmap = null },
            onConfirmUpload = { finalBitmap ->
                pendingPiiRxBitmap = null
                onScanPrescriptionPhoto(finalBitmap)
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || SanaNotificationChannels.hasNotificationPermission(context)
        val action = pendingNotificationAction
        pendingNotificationAction = null
        if (granted) {
            action?.invoke()
        } else {
            onShowMessage("Notification permission is required to deliver daily medication push alerts.")
        }
    }

    fun requestNotificationPermissionAndRun(action: () -> Unit) {
        if (SanaNotificationChannels.hasNotificationPermission(context) ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            hasNotificationPermission = true
            action()
        } else {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val activeMeds = remember(medications) {
        medications
            .filter { it.active }
            .sortedBy { it.reminderHour * 60 + it.reminderMinute }
    }
    val pausedMeds = remember(medications) { medications.filter { !it.active } }
    val groupedActiveMeds = remember(activeMeds) {
        MedicationScheduleSlot.entries
            .mapNotNull { slot ->
                val list = activeMeds.filter { it.parsedSlot == slot }
                if (list.isEmpty()) null else slot to list
            }
    }
    val adherencePct = remember(logs) {
        if (logs.isEmpty()) null else {
            val taken = logs.count { it.parsedStatus == MedicationLogStatus.TAKEN }
            ((taken.toDouble() / logs.size) * 100).toInt()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showBackButton) {
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("meds_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = stringResource(R.string.meds_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.meds_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        editingMedication = null
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("meds_add_new_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Rx")
                }
            }
        }

        // WorkManager Daily Push Reminder Controls
        item {
            DailyRemindersControlCard(
                remindersEnabled = remindersEnabled,
                hasNotificationPermission = hasNotificationPermission,
                medications = medications,
                onToggleReminders = { enable ->
                    if (enable) {
                        requestNotificationPermissionAndRun { onToggleReminders(true) }
                    } else {
                        onToggleReminders(false)
                    }
                },
                onSendTestAlert = {
                    requestNotificationPermissionAndRun { onSendTestReminder() }
                }
            )
        }

        // Adherence KPI Summary
        item {
            SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MedStatBox(
                        label = "Active Rx",
                        value = "${activeMeds.size}",
                        modifier = Modifier.weight(1f)
                    )
                    MedStatBox(
                        label = "Taken Today",
                        value = "${activeMeds.count { todayLogs[it.id]?.parsedStatus == MedicationLogStatus.TAKEN }}/${activeMeds.size}",
                        modifier = Modifier.weight(1f)
                    )
                    MedStatBox(
                        label = "Adherence",
                        value = adherencePct?.let { "$it%" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Module 1A: AI Prescription Bottle / Label Scanner (CameraX + Zero-Permission Photo Picker)
        item {
            MedicationPrescriptionScannerCard(
                scanState = prescriptionScanState,
                isDecodingGalleryPhoto = isDecodingGalleryPhoto,
                onScanWithCamera = onLaunchRxCamera,
                onPickFromGallery = {
                    rxGalleryPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onDismissError = onClearPrescriptionScan
            )
        }

        // Module 1B: AI Drug-Drug, Allergy & Lifestyle Interaction Checker
        item {
            val totalActiveForCheck = activeMeds.size + profile?.medications.orEmpty().count { pm ->
                activeMeds.none { it.name.equals(pm.trim(), ignoreCase = true) }
            }
            MedicationInteractionCheckerCard(
                activeMedicationCount = totalActiveForCheck,
                profileAllergyCount = profile?.allergies.orEmpty().size,
                interactionState = interactionState,
                onRunCheck = onRunInteractionCheck,
                onReportAiResponse = onReportAiResponse
            )
        }

        if (medicationsState is CloudUiState.Loading) {
            items(3) {
                GlassShimmerCard(lines = 2)
            }
        } else if (medicationsState is CloudUiState.Error) {
            item {
                SanctuaryErrorBanner(message = medicationsState.message)
            }
        } else if (medications.isEmpty()) {
            item {
                EmptySanctuaryState(
                    illustrationKind = HealthEmptyIllustrationKind.MEDICATIONS,
                    title = "No prescriptions tracked yet",
                    subtitle = "Log your daily prescriptions or supplements, set exact intake reminder times, and check off doses with WorkManager notifications.",
                    actionLabel = stringResource(R.string.meds_add_action),
                    actionTestTag = "meds_empty_add_button",
                    onAction = {
                        editingMedication = null
                        showAddDialog = true
                    }
                )
            }
        } else {
            groupedActiveMeds.forEach { (slot, slotMeds) ->
                item(key = "slot_header_${slot.name}") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${slot.label.uppercase()} (${slotMeds.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(slotMeds, key = { it.id }) { med ->
                    MedicationScheduleCard(
                        item = med,
                        todayStatus = todayLogs[med.id]?.parsedStatus,
                        reminderActive = remindersEnabled && med.parsedSlot != MedicationScheduleSlot.AS_NEEDED,
                        onLogDose = { status -> onLogDose(med, status) },
                        onEdit = {
                            editingMedication = med
                            showAddDialog = true
                        },
                        onToggleActive = { onToggleActive(med) },
                        onDelete = { onDeleteMedication(med.id) }
                    )
                }
            }

            if (pausedMeds.isNotEmpty()) {
                item {
                    Text(
                        text = "PAUSED (${pausedMeds.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(pausedMeds, key = { it.id }) { med ->
                    MedicationScheduleCard(
                        item = med,
                        todayStatus = null,
                        reminderActive = false,
                        onLogDose = {},
                        onEdit = {
                            editingMedication = med
                            showAddDialog = true
                        },
                        onToggleActive = { onToggleActive(med) },
                        onDelete = { onDeleteMedication(med.id) }
                    )
                }
            }

            item {
                SevenDayAdherenceCard(
                    activeMedicationCount = activeMeds.size,
                    logs = logs
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showAddDialog) {
        MedicationEditorDialog(
            existing = editingMedication,
            scannedDraft = pendingScannedDraft,
            remindersCurrentlyEnabled = remindersEnabled,
            onDismiss = {
                showAddDialog = false
                pendingScannedDraft = null
                onClearPrescriptionScan()
            },
            onSave = { name, dosage, slot, hour, minute, instructions, enableReminders ->
                val performSave = {
                    onSaveMedication(
                        editingMedication?.id,
                        name,
                        dosage,
                        slot,
                        hour,
                        minute,
                        instructions,
                        editingMedication?.active ?: true,
                        enableReminders
                    )
                    showAddDialog = false
                    pendingScannedDraft = null
                    onClearPrescriptionScan()
                }
                if (enableReminders) {
                    requestNotificationPermissionAndRun(performSave)
                } else {
                    performSave()
                }
            }
        )
    }
}

@Composable
private fun MedStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SanaTheme.glass.cardFillElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SanaTheme.glass.borderMid)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MedicationScheduleCard(
    item: MedicationItem,
    todayStatus: MedicationLogStatus?,
    reminderActive: Boolean,
    onLogDose: (MedicationLogStatus) -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    SanctuaryCard(
        containerColor = if (item.active) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.testTag("med_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = if (item.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = if (item.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.dosage.isNotBlank()) {
                        Text(
                            text = item.dosage,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (reminderActive) Icons.Filled.NotificationsActive else Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = if (reminderActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (item.parsedSlot == MedicationScheduleSlot.AS_NEEDED) {
                            item.parsedSlot.label
                        } else {
                            "${item.formattedReminderTime()}${if (reminderActive) " • Alert on" else ""}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminderActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.testTag("med_edit_${item.id}")) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit prescription", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onToggleActive, modifier = Modifier.testTag("med_pause_${item.id}")) {
                Icon(
                    imageVector = if (item.active) Icons.Filled.PauseCircleOutline else Icons.Filled.PlayCircleOutline,
                    contentDescription = if (item.active) "Pause prescription" else "Resume prescription",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("med_delete_${item.id}")) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (item.instructions.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (item.active) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isTaken = todayStatus == MedicationLogStatus.TAKEN
                val isSkipped = todayStatus == MedicationLogStatus.SKIPPED
                FilledTonalButton(
                    onClick = { onLogDose(MedicationLogStatus.TAKEN) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("med_mark_taken_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (isTaken) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isTaken) "Taken Today" else "Mark Taken")
                }
                OutlinedButton(
                    onClick = { onLogDose(MedicationLogStatus.SKIPPED) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("med_mark_skipped_${item.id}")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isSkipped) "Skipped" else "Skip")
                }
            }
        }
    }
}
