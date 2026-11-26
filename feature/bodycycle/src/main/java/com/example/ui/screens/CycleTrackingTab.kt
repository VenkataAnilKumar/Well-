package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.CycleAnalytics
import com.example.data.model.CycleFlowLevel
import com.example.data.model.CycleLog
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.CloudUiState

private val CYCLE_SYMPTOM_OPTIONS = listOf(
    "Cramps",
    "Bloating",
    "Headache",
    "Fatigue",
    "Backache",
    "Mood Shift",
    "Cravings",
    "Tenderness"
)

private val CYCLE_MOOD_OPTIONS = listOf(
    "Calm",
    "Energetic",
    "Sensitive",
    "Low Energy",
    "Irritable"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CycleTrackingTabContent(
    todayDateKey: String,
    cycleTrackingEnabled: Boolean,
    typicalCycleLengthDays: Int,
    cycleLogsState: CloudUiState<List<CycleLog>>,
    onToggleCycleTracking: (Boolean) -> Unit,
    onUpdateTypicalCycleLength: (Int) -> Unit,
    onSaveCycleLog: (dateKey: String, flow: CycleFlowLevel, symptoms: List<String>, mood: String, notes: String) -> Unit,
    onDeleteCycleLog: (String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val logs = (cycleLogsState as? CloudUiState.Success)?.data.orEmpty()

    if (!cycleTrackingEnabled) {
        SanctuaryCard(modifier = Modifier.testTag("cycle_opt_in_card")) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = sanaColors.heartPink.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = sanaColors.heartPink,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.cycle_opt_in_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.cycle_opt_in_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onToggleCycleTracking(true) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = sanaColors.heartPink,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cycle_enable_button")
            ) {
                Text("Enable Cycle Tracking")
            }
        }
        return
    }

    val prediction = remember(logs, typicalCycleLengthDays, todayDateKey) {
        CycleAnalytics.computePrediction(
            logs = logs,
            typicalCycleLengthDays = typicalCycleLengthDays,
            todayDateKey = todayDateKey
        )
    }
    val todayExisting = remember(logs, todayDateKey) {
        logs.firstOrNull { it.dateKey == todayDateKey }
    }

    var selectedFlow by remember(todayDateKey, todayExisting?.id, todayExisting?.flowLevel) {
        mutableStateOf(todayExisting?.parsedFlow ?: CycleFlowLevel.NONE)
    }
    var selectedSymptoms by remember(todayDateKey, todayExisting?.id, todayExisting?.symptoms) {
        mutableStateOf(todayExisting?.symptoms?.toSet() ?: emptySet())
    }
    var selectedMood by remember(todayDateKey, todayExisting?.id, todayExisting?.moodTag) {
        mutableStateOf(todayExisting?.moodTag.orEmpty())
    }
    var notes by remember(todayDateKey, todayExisting?.id, todayExisting?.notes) {
        mutableStateOf(todayExisting?.notes.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (cycleLogsState is CloudUiState.Error) {
            SanctuaryErrorBanner(message = cycleLogsState.message)
        }

        // Cycle Phase & Prediction Window Card
        SanctuaryCard(
            borderColor = sanaColors.heartPink.copy(alpha = 0.28f),
            modifier = Modifier.testTag("cycle_prediction_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "CYCLE OVERVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = sanaColors.heartPink
                    )
                    Text(
                        text = prediction.currentCycleDay?.let { "Cycle Day $it" } ?: "Ready to Track",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = prediction.currentPhaseLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = cycleTrackingEnabled,
                        onCheckedChange = onToggleCycleTracking,
                        modifier = Modifier.testTag("cycle_active_switch")
                    )
                }
            }

            val nextPeriodLabel = prediction.nextPeriodDateLabel
            if (nextPeriodLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = sanaColors.heartPink.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Next Window",
                                style = MaterialTheme.typography.labelSmall,
                                color = sanaColors.heartPink
                            )
                            Text(
                                text = nextPeriodLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val daysLeft = prediction.daysUntilNextPeriod
                            if (daysLeft != null) {
                                Text(
                                    text = when {
                                        daysLeft > 0 -> "in ~$daysLeft day(s)"
                                        daysLeft == 0 -> "estimated today"
                                        else -> "${-daysLeft}d past estimate"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Est. Ovulatory Window",
                                style = MaterialTheme.typography.labelSmall,
                                color = sanaColors.medsTeal
                            )
                            Text(
                                text = prediction.estimatedFertileWindowLabel ?: "—",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${prediction.estimatedCycleLength}-day cycle avg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = sanaColors.subtleDivider)
            Spacer(modifier = Modifier.height(8.dp))

            // Typical Cycle Length Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Typical Cycle Length",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Used until 2+ periods are logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateTypicalCycleLength((typicalCycleLengthDays - 1).coerceAtLeast(21)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease cycle length")
                    }
                    Text(
                        text = "${typicalCycleLengthDays}d",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onUpdateTypicalCycleLength((typicalCycleLengthDays + 1).coerceAtMost(45)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase cycle length")
                    }
                }
            }

            Text(
                text = stringResource(R.string.cycle_prediction_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Daily Cycle Logger Card (Today)
        SanctuaryCard(modifier = Modifier.testTag("cycle_daily_logger_card")) {
            Text(
                text = "Log Today ($todayDateKey)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Flow Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CycleFlowLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedFlow == level,
                        onClick = { selectedFlow = level },
                        label = { Text(level.label) },
                        modifier = Modifier.testTag("cycle_flow_${level.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Symptoms",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CYCLE_SYMPTOM_OPTIONS.forEach { symptom ->
                    val checked = symptom in selectedSymptoms
                    FilterChip(
                        selected = checked,
                        onClick = {
                            selectedSymptoms = if (checked) selectedSymptoms - symptom else selectedSymptoms + symptom
                        },
                        label = { Text(symptom) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Mood",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CYCLE_MOOD_OPTIONS.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = if (selectedMood == mood) "" else mood },
                        label = { Text(mood) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 400) notes = it },
                label = { Text("Private Note (optional)") },
                placeholder = { Text("Comfort measures, energy, sleep…") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    onSaveCycleLog(
                        todayDateKey,
                        selectedFlow,
                        selectedSymptoms.toList(),
                        selectedMood,
                        notes
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = sanaColors.heartPink,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cycle_save_log_button")
            ) {
                Text(if (todayExisting != null) "Update Today's Cycle Log" else "Save Today's Cycle Log")
            }
        }

        // Recent Cycle Logs
        if (logs.isNotEmpty()) {
            Text(
                text = "Recent Cycle Logs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.take(14).forEach { log ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, sanaColors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = if (log.parsedFlow.countsAsPeriod) {
                                    sanaColors.heartPink.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WaterDrop,
                                    contentDescription = null,
                                    tint = if (log.parsedFlow.countsAsPeriod) {
                                        sanaColors.heartPink
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${log.dateKey}  •  ${log.parsedFlow.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val detail = buildList {
                                    if (log.moodTag.isNotBlank()) add(log.moodTag)
                                    if (log.symptoms.isNotEmpty()) add(log.symptoms.joinToString(", "))
                                }.joinToString(" • ")
                                if (detail.isNotBlank()) {
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteCycleLog(log.dateKey) },
                                modifier = Modifier.size(48.dp)
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
