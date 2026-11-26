package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.CycleAnalytics
import com.example.data.model.AiFeatureArea
import com.example.data.model.BmiCategory
import com.example.data.model.BodyAndCycleInsight
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareCategory
import com.example.data.model.CycleFlowLevel
import com.example.data.model.CycleLog
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.EmptySanctuaryState
import com.example.ui.components.HealthEmptyIllustrationKind
import com.example.ui.components.MedicalDisclaimerNote
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.BodyCycleInsightUiState
import com.example.ui.viewmodel.CloudUiState
import java.util.Locale

/**
 * Apple Health-style Summary Card for Body Measurements & Optional Cycle Tracking.
 */
@Composable
fun TodayBodyAndCycleCard(
    todayDateKey: String,
    measurements: List<BodyMeasurement>,
    cycleLogs: List<CycleLog>,
    cycleTrackingEnabled: Boolean,
    typicalCycleLengthDays: Int,
    useMetric: Boolean,
    onOpenBodyAndCycle: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val latest = measurements.firstOrNull()
    val cyclePred = remember(cycleLogs, typicalCycleLengthDays, todayDateKey, cycleTrackingEnabled) {
        if (cycleTrackingEnabled) {
            CycleAnalytics.computePrediction(cycleLogs, typicalCycleLengthDays, todayDateKey)
        } else null
    }

    SanctuaryCard(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenBodyAndCycle)
            .testTag("home_body_cycle_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = sanaColors.sleepIndigo.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessibilityNew,
                        contentDescription = null,
                        tint = sanaColors.sleepIndigo,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.body_cycle_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            latest != null && cyclePred?.currentCycleDay != null ->
                                "${latest.formattedWeight(useMetric)} • Day ${cyclePred.currentCycleDay} (${cyclePred.currentPhaseLabel})"
                            latest != null ->
                                "Latest: ${latest.formattedWeight(useMetric)}${if (latest.bmi > 0) " • BMI ${latest.bmi}" else ""}"
                            cyclePred?.currentCycleDay != null ->
                                "Cycle Day ${cyclePred.currentCycleDay} • ${cyclePred.currentPhaseLabel}"
                            else -> stringResource(R.string.body_cycle_card_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open Body & Cycle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        if (latest != null || (cycleTrackingEnabled && cyclePred?.nextPeriodDateLabel != null)) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (latest != null) {
                    BodySummaryMiniPill(
                        label = "Weight",
                        value = latest.formattedWeight(useMetric),
                        accent = sanaColors.sleepIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    BodySummaryMiniPill(
                        label = "BMI",
                        value = if (latest.bmi > 0) "${latest.bmi} (${latest.bmiCategory.label})" else "Add height",
                        accent = if (latest.bmiCategory == BmiCategory.HEALTHY) sanaColors.healthyGreen else sanaColors.activityOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                val nextPeriodLabel = cyclePred?.nextPeriodDateLabel
                if (cycleTrackingEnabled && nextPeriodLabel != null) {
                    BodySummaryMiniPill(
                        label = "Next Window",
                        value = nextPeriodLabel,
                        accent = sanaColors.heartPink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BodySummaryMiniPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, LocalSanaColors.current.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BodyAndCycleScreen(
    todayDateKey: String,
    measurementsState: CloudUiState<List<BodyMeasurement>>,
    cycleLogsState: CloudUiState<List<CycleLog>>,
    insightState: BodyCycleInsightUiState,
    cycleTrackingEnabled: Boolean,
    typicalCycleLengthDays: Int,
    useMetric: Boolean,
    onBack: () -> Unit,
    onToggleUseMetric: (Boolean) -> Unit,
    onToggleCycleTracking: (Boolean) -> Unit,
    onUpdateTypicalCycleLength: (Int) -> Unit,
    onAddMeasurement: (weightKg: Double, heightCm: Double, waistCm: Double, note: String) -> Unit,
    onDeleteMeasurement: (String) -> Unit,
    onSaveCycleLog: (dateKey: String, flow: CycleFlowLevel, symptoms: List<String>, mood: String, notes: String) -> Unit,
    onDeleteCycleLog: (String) -> Unit,
    onGenerateAiInsight: () -> Unit,
    onAddRecommendationToChecklist: (String, CareCategory) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBack)

    val sanaColors = LocalSanaColors.current
    val measurements = (measurementsState as? CloudUiState.Success)?.data.orEmpty()
    val cycleLogs = (cycleLogsState as? CloudUiState.Success)?.data.orEmpty()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Body, 1 = Cycle
    var showLogDialog by remember { mutableStateOf(false) }

    val latestKnownHeightCm = remember(measurements) {
        measurements.firstOrNull { it.heightCm > 0.0 }?.heightCm ?: 0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("body_cycle_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.body_cycle_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = sanaColors.sleepIndigo
                        )
                        Text(
                            text = stringResource(R.string.body_cycle_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Unit Switch Pill (lbs/in <-> kg/cm)
                FilterChip(
                    selected = useMetric,
                    onClick = { onToggleUseMetric(!useMetric) },
                    label = { Text(if (useMetric) "kg / cm" else "lbs / in") },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("body_unit_toggle_chip")
                )
            }
        }

        // Apple Health Segmented Tab Switcher (Body | Cycle)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Body Measurements", "Cycle Tracking").forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable { selectedTab = idx }
                                .testTag("body_cycle_tab_$idx"),
                            color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(11.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Body Tab Content
        if (selectedTab == 0) {
            if (measurementsState is CloudUiState.Error) {
                item { SanctuaryErrorBanner(message = measurementsState.message) }
            }

            item {
                val latest = measurements.firstOrNull()
                SanctuaryCard(modifier = Modifier.testTag("body_hero_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Current Snapshot",
                                style = MaterialTheme.typography.labelMedium,
                                color = sanaColors.sleepIndigo
                            )
                            Text(
                                text = latest?.formattedWeight(useMetric) ?: "No logs yet",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(
                            onClick = { showLogDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("body_log_measurement_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log")
                        }
                    }

                    if (latest != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BodySummaryMiniPill(
                                label = "Height",
                                value = latest.formattedHeight(useMetric),
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            BodySummaryMiniPill(
                                label = "BMI",
                                value = if (latest.bmi > 0) "${latest.bmi}" else "—",
                                accent = if (latest.bmiCategory == BmiCategory.HEALTHY) sanaColors.healthyGreen else sanaColors.activityOrange,
                                modifier = Modifier.weight(1f)
                            )
                            BodySummaryMiniPill(
                                label = "Waist",
                                value = latest.formattedWaist(useMetric),
                                accent = sanaColors.medsTeal,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (latest.bmi > 0.0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            BmiSpectrumBar(bmi = latest.bmi, category = latest.bmiCategory)
                        }
                    }
                }
            }

            // Weight Trend Chart
            if (measurements.size >= 2) {
                item {
                    WeightTrendChartCard(measurements = measurements, useMetric = useMetric)
                }
            }

            // AI Body & Cycle Pattern Insights Card
            item {
                BodyCycleAiInsightsCard(
                    hasData = measurements.isNotEmpty() || (cycleTrackingEnabled && cycleLogs.isNotEmpty()),
                    insightState = insightState,
                    onGenerate = onGenerateAiInsight,
                    onAddRecommendation = onAddRecommendationToChecklist,
                    onReportAiResponse = onReportAiResponse
                )
            }

            // History Header
            item {
                Text(
                    text = "Measurement History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (measurements.isEmpty()) {
                item {
                    EmptySanctuaryState(
                        illustrationKind = HealthEmptyIllustrationKind.BODY_CYCLE,
                        title = "Track weight, BMI & waist trends",
                        subtitle = "Log your first measurement to auto-calculate BMI and unlock trend charts and AI wellness patterns."
                    )
                }
            } else {
                items(measurements, key = { it.id }) { entry ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("body_history_item_${entry.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = sanaColors.sleepIndigo.copy(alpha = 0.12f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Straighten,
                                    contentDescription = null,
                                    tint = sanaColors.sleepIndigo,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = buildString {
                                        append(entry.formattedWeight(useMetric))
                                        if (entry.bmi > 0) append("  •  BMI ${entry.bmi}")
                                        if (entry.waistCm > 0) append("  •  Waist ${entry.formattedWaist(useMetric)}")
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatTimestamp(entry.recordedAt ?: entry.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.note.isNotBlank()) {
                                    Text(
                                        text = entry.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteMeasurement(entry.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_body_${entry.id}")
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
        } else {
            // Cycle Tracking Tab (modularized in CycleTrackingTab.kt)
            item {
                CycleTrackingTabContent(
                    todayDateKey = todayDateKey,
                    cycleTrackingEnabled = cycleTrackingEnabled,
                    typicalCycleLengthDays = typicalCycleLengthDays,
                    cycleLogsState = cycleLogsState,
                    onToggleCycleTracking = onToggleCycleTracking,
                    onUpdateTypicalCycleLength = onUpdateTypicalCycleLength,
                    onSaveCycleLog = onSaveCycleLog,
                    onDeleteCycleLog = onDeleteCycleLog
                )
            }

            if (cycleTrackingEnabled) {
                item {
                    BodyCycleAiInsightsCard(
                        hasData = measurements.isNotEmpty() || cycleLogs.isNotEmpty(),
                        insightState = insightState,
                        onGenerate = onGenerateAiInsight,
                        onAddRecommendation = onAddRecommendationToChecklist,
                        onReportAiResponse = onReportAiResponse
                    )
                }
            }
        }

        item {
            MedicalDisclaimerNote()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLogDialog) {
        LogBodyMeasurementDialog(
            useMetric = useMetric,
            defaultHeightCm = latestKnownHeightCm,
            onDismiss = { showLogDialog = false },
            onConfirm = { weightKg, heightCm, waistCm, note ->
                onAddMeasurement(weightKg, heightCm, waistCm, note)
                showLogDialog = false
            }
        )
    }
}

@Composable
private fun BmiSpectrumBar(bmi: Double, category: BmiCategory) {
    val sanaColors = LocalSanaColors.current
    val normalized = ((bmi - 14.0) / (38.0 - 14.0)).toFloat().coerceIn(0.02f, 0.98f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BMI Index (${category.label})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = category.rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val barH = 8.dp.toPx()
            val y = (size.height - barH) / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF5AC8FA),
                        sanaColors.healthyGreen,
                        sanaColors.activityOrange,
                        sanaColors.heartPink
                    )
                ),
                topLeft = Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2f, barH / 2f)
            )
            val markerX = size.width * normalized
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(markerX, size.height / 2f)
            )
            drawCircle(
                color = sanaColors.sleepIndigo,
                radius = 4.5.dp.toPx(),
                center = Offset(markerX, size.height / 2f)
            )
        }
    }
}

@Composable
private fun WeightTrendChartCard(
    measurements: List<BodyMeasurement>,
    useMetric: Boolean
) {
    val sanaColors = LocalSanaColors.current
    val ordered = remember(measurements) { measurements.take(14).reversed() }
    val values = remember(ordered, useMetric) {
        ordered.map { if (useMetric) it.weightKg.toFloat() else it.weightLbs.toFloat() }
    }
    val delta = values.last() - values.first()
    val unit = if (useMetric) "kg" else "lbs"

    SanctuaryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weight Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Last ${ordered.size} logged entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = CircleShape
            ) {
                Text(
                    text = String.format(Locale.US, "%+.1f %s", delta, unit),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = sanaColors.sleepIndigo,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            val minV = values.minOrNull() ?: 0f
            val maxV = values.maxOrNull() ?: 1f
            val span = (maxV - minV).coerceAtLeast(1f)
            val padY = 12f
            val usableH = (size.height - padY * 2f).coerceAtLeast(10f)
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()
            values.forEachIndexed { idx, v ->
                val x = idx * stepX
                val y = size.height - padY - (((v - minV) / span) * usableH)
                if (idx == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(sanaColors.sleepIndigo.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                color = sanaColors.sleepIndigo,
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            values.forEachIndexed { idx, v ->
                val x = idx * stepX
                val y = size.height - padY - (((v - minV) / span) * usableH)
                drawCircle(color = sanaColors.sleepIndigo, radius = 5.5f, center = Offset(x, y))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "Min %.1f %s", values.minOrNull() ?: 0f, unit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.US, "Max %.1f %s", values.maxOrNull() ?: 0f, unit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BodyCycleAiInsightsCard(
    hasData: Boolean,
    insightState: BodyCycleInsightUiState,
    onGenerate: () -> Unit,
    onAddRecommendation: (String, CareCategory) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    val sanaColors = LocalSanaColors.current
    SanctuaryCard(
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        modifier = Modifier.testTag("body_cycle_ai_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.body_cycle_ai_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (insightState is BodyCycleInsightUiState.Success) {
                IconButton(
                    onClick = onGenerate,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("body_cycle_ai_refresh")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh AI insights",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (insightState) {
            BodyCycleInsightUiState.Idle -> {
                Text(
                    text = stringResource(R.string.body_cycle_ai_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGenerate,
                    enabled = hasData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("body_cycle_ai_generate_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.body_cycle_ai_cta))
                }
            }

            BodyCycleInsightUiState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Text(
                        text = stringResource(R.string.body_cycle_ai_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is BodyCycleInsightUiState.Error -> {
                SanctuaryErrorBanner(message = insightState.message)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Try Again")
                }
            }

            is BodyCycleInsightUiState.Success -> {
                BodyCycleInsightDetails(
                    insight = insightState.insight,
                    onAddRecommendation = onAddRecommendation,
                    onReportAiResponse = onReportAiResponse
                )
            }
        }
    }
}

@Composable
private fun BodyCycleInsightDetails(
    insight: BodyAndCycleInsight,
    onAddRecommendation: (String, CareCategory) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var addedSet by remember(insight.generatedAtMillis) { mutableStateOf(emptySet<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = insight.headline,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (insight.bodyTrendNote.isNotBlank()) {
            Text(
                text = insight.bodyTrendNote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (insight.cyclePatternNote.isNotBlank()) {
            Surface(
                color = sanaColors.heartPink.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = insight.cyclePatternNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        if (insight.gentleRecommendations.isNotEmpty()) {
            HorizontalDivider(color = sanaColors.subtleDivider)
            Text(
                text = "Supportive Habits",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            insight.gentleRecommendations.forEach { rec ->
                val added = rec in addedSet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• $rec",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !added) {
                                addedSet = addedSet + rec
                                onAddRecommendation(rec, CareCategory.AI_SUGGESTION)
                            },
                        color = if (added) sanaColors.selfCareBg else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (added) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (added) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (added) "Added" else "Checklist",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (added) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        AiSafetyFooter(
            featureArea = AiFeatureArea.BODY_CYCLE_INSIGHT,
            contentSnippet = "${insight.headline}: ${insight.bodyTrendNote}",
            generatedAtMillis = insight.generatedAtMillis,
            onReportClick = onReportAiResponse
        )
    }
}

@Composable
private fun LogBodyMeasurementDialog(
    useMetric: Boolean,
    defaultHeightCm: Double,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double, heightCm: Double, waistCm: Double, note: String) -> Unit
) {
    val defaultHeightText = remember(useMetric, defaultHeightCm) {
        if (defaultHeightCm <= 0.0) ""
        else if (useMetric) String.format(Locale.US, "%.0f", defaultHeightCm)
        else String.format(Locale.US, "%.1f", defaultHeightCm / 2.54)
    }

    var weightText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf(defaultHeightText) }
    var waistText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val parsedWeight = weightText.toDoubleOrNull()
    val parsedHeight = heightText.toDoubleOrNull() ?: 0.0
    val parsedWaist = waistText.toDoubleOrNull() ?: 0.0

    val weightKg = parsedWeight?.let { if (useMetric) it else it / 2.20462262 }
    val heightCm = if (parsedHeight > 0.0) {
        if (useMetric) parsedHeight else parsedHeight * 2.54
    } else 0.0
    val waistCm = if (parsedWaist > 0.0) {
        if (useMetric) parsedWaist else parsedWaist * 2.54
    } else 0.0

    val previewBmi = if (weightKg != null && weightKg in 10.0..400.0 && heightCm in 50.0..280.0) {
        BodyMeasurement.computeBmi(weightKg, heightCm)
    } else 0.0

    val isWeightValid = weightKg != null && weightKg in 10.0..400.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Body Measurement", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(if (useMetric) "Weight (kg) *" else "Weight (lbs) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_body_weight_input")
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text(if (useMetric) "Height (cm, optional for BMI)" else "Height (inches, optional for BMI)") },
                    placeholder = { Text(if (useMetric) "e.g. 170" else "e.g. 67") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_body_height_input")
                )
                if (previewBmi > 0.0) {
                    val cat = BmiCategory.fromBmi(previewBmi)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Auto-calculated BMI: $previewBmi (${cat.label})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = waistText,
                    onValueChange = { waistText = it },
                    label = { Text(if (useMetric) "Waist (cm, optional)" else "Waist (inches, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_body_waist_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 300) note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Morning weigh-in, post-workout…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (weightKg != null && isWeightValid) {
                        onConfirm(weightKg, heightCm, waistCm, note)
                    }
                },
                enabled = isWeightValid,
                modifier = Modifier.testTag("dialog_save_body_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
