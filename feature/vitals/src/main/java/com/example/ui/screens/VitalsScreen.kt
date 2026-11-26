package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.AiFeatureArea
import com.example.data.model.LabBiomarker
import com.example.data.model.LabBiomarkerCategory
import com.example.data.model.LabBiomarkerStatus
import com.example.data.model.VitalRangeStatus
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import com.example.ui.components.EmptySanctuaryState
import com.example.ui.components.HealthEmptyIllustrationKind
import com.example.ui.components.PaginatedListFooter
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.formatTimestamp
import com.example.ui.components.healthIconRes
import com.example.ui.theme.GlassMetricValueRow
import com.example.ui.theme.GlassShimmerCard
import com.example.ui.theme.GlassStatusPill
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.LabScanUiState
import com.example.ui.viewmodel.PaginationUiState
import com.example.ui.viewmodel.ProactiveReassessmentUiState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayVitalsCard(
    vitals: List<VitalReading>,
    region: HealthRegionProfile = HealthRegionDirectory.US,
    onOpenVitalsScreen: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val latestByType = remember(vitals) {
        listOf(VitalType.BLOOD_PRESSURE, VitalType.HEART_RATE, VitalType.TEMPERATURE, VitalType.SPO2)
            .mapNotNull { type -> vitals.firstOrNull { it.parsedType == type } }
            .take(3)
    }

    SanctuaryCard(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenVitalsScreen)
            .testTag("home_vitals_card")
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
                    color = sanaColors.heartPink.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonitorHeart,
                        contentDescription = null,
                        tint = sanaColors.heartPink,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Vitals",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (vitals.isEmpty()) {
                            "Log BP, heart rate, temperature, SpO₂, or glucose"
                        } else {
                            "${vitals.size} readings synced"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Vitals",
                tint = sanaColors.heartPink
            )
        }

        if (latestByType.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                latestByType.forEach { reading ->
                    val statusColor = when (reading.rangeStatus) {
                        VitalRangeStatus.NORMAL -> sanaColors.selfCareFg
                        VitalRangeStatus.ELEVATED -> sanaColors.doctorSoonFg
                        VitalRangeStatus.LOW -> sanaColors.doctorSoonFg
                        VitalRangeStatus.HIGH -> sanaColors.urgentFg
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, sanaColors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = reading.parsedType.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = reading.formattedReadingForRegion(region),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = reading.rangeStatus.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onOpenVitalsScreen,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_vitals_screen_button")
        ) {
            Icon(Icons.Filled.MonitorHeart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (vitals.isEmpty()) "Log First Vital" else "Log & View Trends")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VitalsScreen(
    vitalsState: CloudUiState<List<VitalReading>>,
    region: HealthRegionProfile = HealthRegionDirectory.US,
    onBack: () -> Unit,
    onAddVital: (VitalType, Double, Double?, String) -> Unit,
    onDeleteVital: (String) -> Unit,
    paginationState: PaginationUiState = PaginationUiState(),
    onLoadMore: () -> Unit = {},
    labBiomarkersState: CloudUiState<List<LabBiomarker>> = CloudUiState.Success(emptyList()),
    labScanState: LabScanUiState = LabScanUiState.Idle,
    isSavingLabBatch: Boolean = false,
    autoMaskPiiBeforeScan: Boolean = true,
    pendingLabCameraBitmap: Bitmap? = null,
    onConsumePendingLabCameraBitmap: () -> Unit = {},
    onLaunchLabCamera: () -> Unit = {},
    onScanLabBitmap: (Bitmap) -> Unit = {},
    onToggleExtractedMarker: (Int) -> Unit = {},
    onSaveExtractedMarkers: () -> Unit = {},
    onClearLabScan: () -> Unit = {},
    onAddManualLabBiomarker: (String, LabBiomarkerCategory, Double, String, String, LabBiomarkerStatus, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteLabBiomarker: (String) -> Unit = {},
    abnormalVitalForReassessment: VitalReading? = null,
    vitalReassessmentState: ProactiveReassessmentUiState = ProactiveReassessmentUiState.Idle,
    onRunAbnormalVitalReassessment: () -> Unit = {},
    onDismissVitalReassessment: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBack)

    val haptics = LocalHapticFeedback.current
    val allVitals = (vitalsState as? CloudUiState.Success)?.data.orEmpty()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Vitals, 1 = Lab Biomarkers
    var selectedType by remember { mutableStateOf(VitalType.BLOOD_PRESSURE) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showManualLabDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(pendingLabCameraBitmap) {
        if (pendingLabCameraBitmap != null) {
            selectedTab = 1
        }
    }

    val filtered = remember(allVitals, selectedType) {
        allVitals.filter { it.parsedType == selectedType }
    }
    val chartPoints = remember(filtered) { filtered.take(14).reversed() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("vitals_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
                Button(
                    onClick = {
                        if (selectedTab == 0) showLogDialog = true else showManualLabDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("vitals_log_new_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedTab == 0) stringResource(R.string.vitals_log_action)
                        else stringResource(R.string.labs_add_manual)
                    )
                }
            }
        }

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
                    Text(
                        text = stringResource(R.string.vitals_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.vitals_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track readings against reference ranges and extract lab biomarkers for Doctor Visit PDFs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("vitals_region_unit_badge")
                ) {
                    Text(
                        text = if (region.isCanada) "CA Metric" else "US Units",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Vitals vs. Lab Biomarkers Segmented Tab Switcher
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(Icons.Filled.MonitorHeart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("vitals_tab_vitals")
                ) {
                    Text(stringResource(R.string.vitals_tab_vitals))
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(Icons.Filled.Biotech, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("vitals_tab_labs")
                ) {
                    Text(stringResource(R.string.vitals_tab_labs))
                }
            }
        }

        if (selectedTab == 1) {
            item {
                LabBiomarkersSectionContent(
                    labBiomarkersState = labBiomarkersState,
                    labScanState = labScanState,
                    isSavingLabBatch = isSavingLabBatch,
                    autoMaskPiiBeforeScan = autoMaskPiiBeforeScan,
                    pendingLabCameraBitmap = pendingLabCameraBitmap,
                    onConsumePendingLabCameraBitmap = onConsumePendingLabCameraBitmap,
                    onLaunchLabCamera = onLaunchLabCamera,
                    onScanLabBitmap = onScanLabBitmap,
                    onToggleExtractedMarker = onToggleExtractedMarker,
                    onSaveExtractedMarkers = onSaveExtractedMarkers,
                    onClearLabScan = onClearLabScan,
                    onOpenManualDialog = { showManualLabDialog = true },
                    onDeleteBiomarker = onDeleteLabBiomarker,
                    onShowMessage = onShowMessage,
                    onReportAiResponse = onReportAiResponse
                )
            }
        } else {
            if (abnormalVitalForReassessment != null) {
                item(key = "abnormal_vital_reassessment") {
                    VitalsProactiveReassessmentBanner(
                        abnormalVital = abnormalVitalForReassessment,
                        region = region,
                        reassessmentState = vitalReassessmentState,
                        onRunReassessment = onRunAbnormalVitalReassessment,
                        onDismiss = onDismissVitalReassessment,
                        onReportAiResponse = onReportAiResponse
                    )
                }
            }

            item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VitalType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(type.healthIconRes()),
                                contentDescription = type.label,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(type.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("vital_chip_${type.name}")
                    )
                }
            }
        }

        // Clinical Range & Trend Chart Card
        item {
            SanctuaryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(selectedType.healthIconRes()),
                                contentDescription = selectedType.label,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = selectedType.label,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Typical reference: ${selectedType.normalRangeLabelFor(region)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    filtered.firstOrNull()?.let { latest ->
                        VitalStatusBadge(status = latest.rangeStatus)
                    }
                }

                if (chartPoints.size >= 2) {
                    Spacer(modifier = Modifier.height(14.dp))
                    VitalTrendCanvas(
                        vitalType = selectedType,
                        points = chartPoints,
                        region = region,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("vitals_trend_canvas")
                    )
                    if (selectedType == VitalType.BLOOD_PRESSURE) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Teal: Systolic • Coral: Diastolic • Shaded: Typical normal systolic range",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Log at least 2 ${selectedType.label.lowercase()} readings to see your trend line.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (vitalsState is CloudUiState.Loading) {
            items(3) {
                GlassShimmerCard(lines = 2)
            }
        } else if (vitalsState is CloudUiState.Error) {
            item {
                SanctuaryErrorBanner(message = vitalsState.message)
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptySanctuaryState(
                    illustrationKind = HealthEmptyIllustrationKind.VITALS,
                    title = "No ${selectedType.label} readings yet",
                    subtitle = "Tap \"Log Vital\" to record your ${selectedType.label.lowercase()} (${selectedType.unitFor(region)}).",
                    actionLabel = "Log ${selectedType.shortLabel}",
                    actionTestTag = "vitals_empty_log_button",
                    onAction = { showLogDialog = true }
                )
            }
        } else {
            items(filtered, key = { it.id }) { reading ->
                val targetUnit = reading.parsedType.unitFor(region)
                val formattedNumber = reading.parsedType.formatValueForUnit(
                    primary = reading.normalizedValueForRegion(region),
                    secondary = reading.secondaryValue,
                    unit = targetUnit
                )
                SanctuaryCard(modifier = Modifier.testTag("vital_row_${reading.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            GlassMetricValueRow(
                                value = formattedNumber,
                                unit = targetUnit,
                                valueStyle = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = formatTimestamp(reading.recordedAt ?: reading.createdAt),
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (reading.note.isNotBlank()) {
                                Text(
                                    text = reading.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            VitalStatusBadge(status = reading.rangeStatus)
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDeleteVital(reading.id)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("vital_delete_${reading.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_delete_item),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

            if (vitalsState is CloudUiState.Success &&
                (paginationState.hasMore || paginationState.isLoadingMore || paginationState.errorMessage != null)
            ) {
                item(key = "vitals_pagination_footer") {
                    PaginatedListFooter(
                        paginationState = paginationState,
                        onLoadMore = onLoadMore,
                        buttonTestTag = "vitals_load_more_button"
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showLogDialog) {
        LogVitalDialog(
            initialType = selectedType,
            region = region,
            onDismiss = { showLogDialog = false },
            onConfirm = { type, primary, secondary, note ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedType = type
                onAddVital(type, primary, secondary, note)
                showLogDialog = false
            }
        )
    }

    if (showManualLabDialog) {
        ManualLabBiomarkerDialog(
            onDismiss = { showManualLabDialog = false },
            onConfirm = { name, cat, value, unit, refRange, status, dateKey, note ->
                onAddManualLabBiomarker(name, cat, value, unit, refRange, status, dateKey, note)
                showManualLabDialog = false
            }
        )
    }
}

@Composable
private fun VitalStatusBadge(status: VitalRangeStatus) {
    val sanaColors = LocalSanaColors.current
    val (label, accent) = when (status) {
        VitalRangeStatus.NORMAL -> "Optimal" to sanaColors.selfCareFg
        VitalRangeStatus.ELEVATED -> "Elevated" to sanaColors.doctorSoonFg
        VitalRangeStatus.LOW -> "Low" to sanaColors.doctorSoonFg
        VitalRangeStatus.HIGH -> "High" to sanaColors.urgentFg
    }
    GlassStatusPill(label = label, accentColor = accent)
}

@Composable
private fun VitalTrendCanvas(
    vitalType: VitalType,
    points: List<VitalReading>,
    region: HealthRegionProfile,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val isDark = SanaTheme.isDark
    val glass = SanaTheme.glass
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = LocalSanaColors.current.heartPink
    val bandColor = LocalSanaColors.current.selfCareFg.copy(alpha = if (isDark) 0.14f else 0.10f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = glass.borderMid

    var inspectedIndex by remember(points, vitalType) { mutableStateOf<Int?>(null) }
    val activePoint = inspectedIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()

    val (normalLow, normalHigh) = remember(vitalType, region) {
        when (vitalType) {
            VitalType.TEMPERATURE -> if (region.usesCelsiusTemp) 36.1 to 37.3 else vitalType.normalLow to vitalType.normalHigh
            VitalType.BLOOD_GLUCOSE -> if (region.usesMmolGlucose) 3.9 to 7.8 else vitalType.normalLow to vitalType.normalHigh
            else -> vitalType.normalLow to vitalType.normalHigh
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Interactive frosted glass tooltip header
        if (activePoint != null) {
            val unit = activePoint.parsedType.unitFor(region)
            val numStr = activePoint.parsedType.formatValueForUnit(
                primary = activePoint.normalizedValueForRegion(region),
                secondary = activePoint.secondaryValue,
                unit = unit
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = glass.cardFillElevated,
                border = BorderStroke(1.dp, glass.borderMid),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vitals_chart_tooltip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = if (inspectedIndex != null) {
                                formatTimestamp(activePoint.recordedAt ?: activePoint.createdAt)
                            } else {
                                "Latest • Tap or drag chart to inspect"
                            },
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        GlassMetricValueRow(
                            value = numStr,
                            unit = unit,
                            valueStyle = MaterialTheme.typography.titleLarge
                        )
                    }
                    VitalStatusBadge(status = activePoint.rangeStatus)
                }
            }
        }

        Canvas(
            modifier = modifier
                .pointerInput(points) {
                    fun selectForX(touchX: Float) {
                        if (points.size < 2) return
                        val padX = 14.dp.toPx()
                        val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
                        val frac = ((touchX - padX) / chartW).coerceIn(0f, 1f)
                        val idx = (frac * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                        if (inspectedIndex != idx) {
                            inspectedIndex = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    detectTapGestures { offset -> selectForX(offset.x) }
                }
                .pointerInput(points) {
                    fun selectForX(touchX: Float) {
                        if (points.size < 2) return
                        val padX = 14.dp.toPx()
                        val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
                        val frac = ((touchX - padX) / chartW).coerceIn(0f, 1f)
                        val idx = (frac * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                        if (inspectedIndex != idx) {
                            inspectedIndex = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        selectForX(change.position.x)
                    }
                }
        ) {
            if (points.size < 2) return@Canvas
            val padX = 14.dp.toPx()
            val padY = 14.dp.toPx()
            val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
            val chartH = (size.height - padY * 2f).coerceAtLeast(1f)
            val chartBottom = padY + chartH

            val normalizedPrimaries = points.map { it.normalizedValueForRegion(region) }
            val allValues = buildList {
                normalizedPrimaries.forEach { add(it) }
                points.forEach { pt -> pt.secondaryValue?.let { add(it) } }
                if (vitalType != VitalType.WEIGHT) {
                    add(normalLow)
                    add(normalHigh)
                }
            }
            val minRaw = allValues.minOrNull() ?: 0.0
            val maxRaw = allValues.maxOrNull() ?: 100.0
            val pad = ((maxRaw - minRaw) * 0.14).coerceAtLeast(if (region.usesMmolGlucose || region.usesCelsiusTemp) 0.5 else 3.0)
            val minVal = (minRaw - pad).coerceAtLeast(0.0)
            val maxVal = (maxRaw + pad).coerceAtLeast(minVal + 1.0)
            val span = (maxVal - minVal).coerceAtLeast(0.5)

            fun yFor(value: Double): Float {
                val norm = ((value - minVal) / span).toFloat().coerceIn(0f, 1f)
                return padY + (1f - norm) * chartH
            }

            // Normal range frosted shaded band
            if (vitalType != VitalType.WEIGHT) {
                val topBand = yFor(normalHigh)
                val bottomBand = yFor(normalLow)
                drawRect(
                    color = bandColor,
                    topLeft = Offset(padX, topBand),
                    size = Size(chartW, (bottomBand - topBand).coerceAtLeast(2f))
                )
            }

            // Subtle translucent grid lines
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = padY + chartH * frac
                drawLine(gridColor, Offset(padX, y), Offset(size.width - padX, y), strokeWidth = 1.dp.toPx())
            }

            val primaryOffsets = normalizedPrimaries.mapIndexed { idx, value ->
                val x = padX + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartW
                Offset(x, yFor(value))
            }

            val linePath = Path().apply {
                moveTo(primaryOffsets.first().x, primaryOffsets.first().y)
                for (i in 1 until primaryOffsets.size) {
                    val prev = primaryOffsets[i - 1]
                    val curr = primaryOffsets[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }

            // Translucent glass gradient area wash beneath the primary curve
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(primaryOffsets.last().x, chartBottom)
                lineTo(primaryOffsets.first().x, chartBottom)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isDark) 0.30f else 0.20f),
                        primaryColor.copy(alpha = 0.0f)
                    ),
                    startY = padY,
                    endY = chartBottom
                )
            )

            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            if (vitalType.hasSecondaryValue) {
                val secOffsets = points.mapIndexedNotNull { idx, pt ->
                    pt.secondaryValue?.let { v ->
                        val x = padX + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartW
                        Offset(x, yFor(v))
                    }
                }
                if (secOffsets.size >= 2) {
                    val secPath = Path().apply {
                        moveTo(secOffsets.first().x, secOffsets.first().y)
                        for (i in 1 until secOffsets.size) {
                            val prev = secOffsets[i - 1]
                            val curr = secOffsets[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = secPath,
                        color = secondaryColor,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    secOffsets.forEach { pt ->
                        drawCircle(surfaceColor, radius = 4.5.dp.toPx(), center = pt)
                        drawCircle(secondaryColor, radius = 3.dp.toPx(), center = pt)
                    }
                }
            }

            // Vertical hairline & glowing focal end-cap on inspected or latest point
            val focusIdx = (inspectedIndex ?: primaryOffsets.lastIndex).coerceIn(0, primaryOffsets.lastIndex)
            val focusPt = primaryOffsets[focusIdx]
            if (inspectedIndex != null) {
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.55f), Color.Transparent),
                        startY = padY,
                        endY = chartBottom
                    ),
                    start = Offset(focusPt.x, padY),
                    end = Offset(focusPt.x, chartBottom),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            primaryOffsets.forEachIndexed { idx, pt ->
                if (idx == focusIdx) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.55f),
                                primaryColor.copy(alpha = 0.0f)
                            ),
                            center = pt,
                            radius = 12.dp.toPx()
                        ),
                        radius = 12.dp.toPx(),
                        center = pt
                    )
                    drawCircle(Color.White, radius = 5.5.dp.toPx(), center = pt)
                    drawCircle(primaryColor, radius = 3.8.dp.toPx(), center = pt)
                } else {
                    drawCircle(surfaceColor, radius = 4.5.dp.toPx(), center = pt)
                    drawCircle(primaryColor, radius = 3.dp.toPx(), center = pt)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogVitalDialog(
    initialType: VitalType,
    region: HealthRegionProfile,
    onDismiss: () -> Unit,
    onConfirm: (VitalType, Double, Double?, String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var primaryText by remember { mutableStateOf("") }
    var secondaryText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val activeUnit = type.unitFor(region)
    val minAllowed = type.minAllowedFor(region)
    val maxAllowed = type.maxAllowedFor(region)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Vital Sign", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VitalType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                validationError = null
                            },
                            label = { Text(option.shortLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text(
                    text = "Typical range: ${type.normalRangeLabelFor(region)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = primaryText,
                        onValueChange = {
                            primaryText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6)
                            validationError = null
                        },
                        label = {
                            Text(
                                if (type.hasSecondaryValue) "Systolic ($activeUnit)"
                                else "${type.shortLabel} ($activeUnit)"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_vital_primary")
                    )
                    if (type.hasSecondaryValue) {
                        OutlinedTextField(
                            value = secondaryText,
                            onValueChange = {
                                secondaryText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(6)
                                validationError = null
                            },
                            label = { Text("Diastolic") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_vital_secondary")
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 300) note = it },
                    label = { Text("Context note (optional)") },
                    placeholder = { Text("Resting morning, after walk, fasting...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_vital_note")
                )
                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val primary = primaryText.toDoubleOrNull()
                    val secondary = if (type.hasSecondaryValue) secondaryText.toDoubleOrNull() else null
                    when {
                        primary == null || primary < minAllowed || primary > maxAllowed -> {
                            val minFmt = if (minAllowed % 1.0 == 0.0) minAllowed.toInt().toString() else String.format(Locale.US, "%.1f", minAllowed)
                            val maxFmt = if (maxAllowed % 1.0 == 0.0) maxAllowed.toInt().toString() else String.format(Locale.US, "%.1f", maxAllowed)
                            validationError = "Enter a valid ${type.shortLabel} between $minFmt and $maxFmt $activeUnit."
                        }
                        type.hasSecondaryValue && (secondary == null || secondary < 30.0 || secondary > 200.0) -> {
                            validationError = "Enter a valid diastolic value between 30 and 200."
                        }
                        else -> onConfirm(type, primary, secondary, note)
                    }
                },
                modifier = Modifier.testTag("dialog_vital_save_button")
            ) {
                Text("Save Reading")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
