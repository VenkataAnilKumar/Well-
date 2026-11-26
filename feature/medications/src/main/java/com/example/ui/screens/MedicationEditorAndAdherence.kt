package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.res.stringResource
import com.example.core.designsystem.R
import com.example.core.ReminderScheduler
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.ScannedPrescriptionDraft
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun DailyRemindersControlCard(
    remindersEnabled: Boolean,
    hasNotificationPermission: Boolean,
    medications: List<MedicationItem>,
    onToggleReminders: (Boolean) -> Unit,
    onSendTestAlert: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val scheduledCount = remember(medications) {
        medications.count { it.active && it.parsedSlot != MedicationScheduleSlot.AS_NEEDED }
    }
    val nextSummary = remember(medications) {
        ReminderScheduler.nextScheduledMedicationSummary(medications)
    }

    SanctuaryCard(
        containerColor = if (remindersEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        borderColor = if (remindersEnabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        } else {
            sanaColors.cardBorder
        },
        modifier = Modifier.testTag("meds_reminders_control_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (remindersEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (remindersEnabled) {
                            Icons.Filled.NotificationsActive
                        } else {
                            Icons.Filled.NotificationsOff
                        },
                        contentDescription = null,
                        tint = if (remindersEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Intake Push Reminders",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        remindersEnabled && scheduledCount > 0 ->
                            "$scheduledCount prescription alert(s) scheduled via WorkManager"
                        remindersEnabled ->
                            "Active — add a scheduled prescription to receive daily alerts"
                        else ->
                            "Turn on local WorkManager notifications for daily doses"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = remindersEnabled,
                onCheckedChange = onToggleReminders,
                modifier = Modifier.testTag("meds_reminders_switch")
            )
        }

        if (remindersEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        !hasNotificationPermission -> "Notification permission needed to display alerts"
                        nextSummary != null -> nextSummary
                        else -> "Waiting for your first scheduled dose"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasNotificationPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onSendTestAlert,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("meds_test_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Test Alert", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun SevenDayAdherenceCard(
    activeMedicationCount: Int,
    logs: List<MedicationLog>
) {
    val sanaColors = LocalSanaColors.current
    val daySummaries = remember(logs, activeMedicationCount) {
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
        val byDate = logs.groupBy { it.dateKey }
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dateKey = keyFormat.format(cal.time)
            val label = dayLabelFormat.format(cal.time).take(2)
            val entries = byDate[dateKey].orEmpty()
            val takenCount = entries.count { it.parsedStatus == MedicationLogStatus.TAKEN }
            val skippedCount = entries.count { it.parsedStatus == MedicationLogStatus.SKIPPED }
            Triple(label, takenCount, skippedCount)
        }
    }

    SanctuaryCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("meds_adherence_history_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "7-Day Intake History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daySummaries.forEach { (dayLabel, takenCount, skippedCount) ->
                val allTaken = activeMedicationCount > 0 && takenCount >= activeMedicationCount
                val partialTaken = takenCount > 0 && !allTaken
                val bgColor = when {
                    allTaken -> sanaColors.selfCareBg
                    partialTaken -> MaterialTheme.colorScheme.primaryContainer
                    skippedCount > 0 -> sanaColors.doctorSoonBg
                    else -> MaterialTheme.colorScheme.surface
                }
                val fgColor = when {
                    allTaken -> sanaColors.selfCareFg
                    partialTaken -> MaterialTheme.colorScheme.primary
                    skippedCount > 0 -> sanaColors.doctorSoonFg
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = bgColor,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, if (takenCount > 0) fgColor.copy(alpha = 0.35f) else sanaColors.cardBorder),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                allTaken -> Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "All doses taken",
                                    tint = fgColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                takenCount > 0 -> Text(
                                    text = "$takenCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = fgColor
                                )
                                skippedCount > 0 -> Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dose skipped",
                                    tint = fgColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                else -> Text(
                                    text = "·",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = fgColor
                                )
                            }
                        }
                    }
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val recentLogs = remember(logs) { logs.take(6) }
        if (recentLogs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = sanaColors.subtleDivider)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recentLogs.forEach { log ->
                    val isTaken = log.parsedStatus == MedicationLogStatus.TAKEN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.medicationName.ifBlank { "Medication" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = log.dateKey,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (isTaken) sanaColors.selfCareBg else sanaColors.doctorSoonBg,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = log.parsedStatus.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTaken) sanaColors.selfCareFg else sanaColors.doctorSoonFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MedicationEditorDialog(
    existing: MedicationItem?,
    scannedDraft: ScannedPrescriptionDraft? = null,
    remindersCurrentlyEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        dosage: String,
        slot: MedicationScheduleSlot,
        hour: Int,
        minute: Int,
        instructions: String,
        enableReminders: Boolean
    ) -> Unit
) {
    val initialSlot = existing?.parsedSlot ?: scannedDraft?.scheduleSlot ?: MedicationScheduleSlot.MORNING
    var name by remember(existing, scannedDraft) {
        mutableStateOf(existing?.name ?: scannedDraft?.name.orEmpty())
    }
    var dosage by remember(existing, scannedDraft) {
        mutableStateOf(existing?.dosage ?: scannedDraft?.dosage.orEmpty())
    }
    var slot by remember(existing, scannedDraft) {
        mutableStateOf(initialSlot)
    }
    var reminderHour by remember(existing, scannedDraft) {
        mutableIntStateOf(
            existing?.reminderHour?.toInt()
                ?: scannedDraft?.reminderHour
                ?: initialSlot.defaultHour
        )
    }
    var reminderMinute by remember(existing, scannedDraft) {
        mutableIntStateOf(
            existing?.reminderMinute?.toInt()
                ?: scannedDraft?.reminderMinute
                ?: initialSlot.defaultMinute
        )
    }
    var instructions by remember(existing, scannedDraft) {
        mutableStateOf(existing?.instructions ?: scannedDraft?.instructions.orEmpty())
    }
    var enableReminders by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }

    val formattedTime = remember(reminderHour, reminderMinute) {
        MedicationItem(
            reminderHour = reminderHour.toLong(),
            reminderMinute = reminderMinute.toLong()
        ).formattedReminderTime()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    existing != null -> "Edit Prescription"
                    scannedDraft != null -> "Review Scanned Prescription"
                    else -> "Log Prescription or Supplement"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (scannedDraft != null) {
                    val sanaColors = LocalSanaColors.current
                    Surface(
                        color = sanaColors.medsTeal.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, sanaColors.medsTeal.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("dialog_med_scan_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = sanaColors.medsTeal,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.meds_ai_scan_verify_banner),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (scannedDraft.confidenceNote.isNotBlank()) {
                                    Text(
                                        text = scannedDraft.confidenceNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text("Prescription / Medication Name") },
                    placeholder = { Text("e.g., Lisinopril, Metformin, Vitamin D3") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_med_name")
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { if (it.length <= 60) dosage = it },
                    label = { Text("Dosage & Strength") },
                    placeholder = { Text("e.g., 10 mg, 500 mg, 1 tablet") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_med_dosage")
                )

                Text("Schedule Window", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MedicationScheduleSlot.entries.forEach { option ->
                        FilterChip(
                            selected = slot == option,
                            onClick = {
                                slot = option
                                reminderHour = option.defaultHour
                                reminderMinute = option.defaultMinute
                            },
                            label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("dialog_med_slot_${option.name}")
                        )
                    }
                }

                if (slot != MedicationScheduleSlot.AS_NEEDED) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Daily Reminder Time",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formattedTime,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("dialog_med_pick_time_button")
                                ) {
                                    Text("Change Time", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    7 to 0,
                                    8 to 0,
                                    12 to 30,
                                    18 to 0,
                                    21 to 0,
                                    22 to 0
                                ).forEach { (h, m) ->
                                    val presetLabel = MedicationItem(
                                        reminderHour = h.toLong(),
                                        reminderMinute = m.toLong()
                                    ).formattedReminderTime()
                                    FilterChip(
                                        selected = reminderHour == h && reminderMinute == m,
                                        onClick = {
                                            reminderHour = h
                                            reminderMinute = m
                                        },
                                        label = { Text(presetLabel, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    if (!remindersCurrentlyEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = enableReminders,
                                onCheckedChange = { enableReminders = it },
                                modifier = Modifier.testTag("dialog_med_enable_reminders_checkbox")
                            )
                            Text(
                                text = "Schedule daily push notification via WorkManager",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Text(
                        text = "As-needed prescriptions are tracked for logging and AI safety without a daily alarm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { if (it.length <= 240) instructions = it },
                    label = { Text("Intake Instructions (optional)") },
                    placeholder = { Text("Take with food, full glass of water...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_med_instructions")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        dosage,
                        slot,
                        reminderHour,
                        reminderMinute,
                        instructions,
                        !remindersCurrentlyEnabled && enableReminders && slot != MedicationScheduleSlot.AS_NEEDED
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("dialog_med_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Daily Reminder Time", style = MaterialTheme.typography.titleLarge) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderHour = timeState.hour
                        reminderMinute = timeState.minute
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("dialog_time_picker_confirm")
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
