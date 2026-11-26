package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.HealthRegionProfile
import com.example.core.designsystem.R
import com.example.core.loadDownscaledBitmap
import com.example.data.model.AiFeatureArea
import com.example.data.model.LabBiomarker
import com.example.data.model.LabBiomarkerCategory
import com.example.data.model.LabBiomarkerStatus
import com.example.data.model.ProactiveReassessmentResult
import com.example.data.model.VitalReading
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.EmptySanctuaryState
import com.example.ui.components.HealthEmptyIllustrationKind
import com.example.ui.components.PiiRedactionPreviewSheet
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.GlassMetricValueRow
import com.example.ui.theme.GlassShimmerCard
import com.example.ui.theme.GlassStatusPill
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.LabScanUiState
import com.example.ui.viewmodel.ProactiveReassessmentUiState
import kotlinx.coroutines.launch

@Composable
internal fun VitalsProactiveReassessmentBanner(
    abnormalVital: VitalReading,
    region: HealthRegionProfile,
    reassessmentState: ProactiveReassessmentUiState,
    onRunReassessment: () -> Unit,
    onDismiss: () -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    SanctuaryCard(
        containerColor = sanaColors.doctorSoonBg.copy(alpha = 0.55f),
        borderColor = sanaColors.doctorSoonFg.copy(alpha = 0.35f),
        modifier = Modifier.testTag("vitals_proactive_reassessment_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = sanaColors.doctorSoonFg,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.reassess_vital_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${abnormalVital.parsedType.label}: ${abnormalVital.formattedReadingForRegion(region)} (${abnormalVital.rangeStatus.label})",
                        style = MaterialTheme.typography.labelMedium,
                        color = sanaColors.doctorSoonFg
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("vitals_reassess_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss reassessment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (reassessmentState) {
            is ProactiveReassessmentUiState.Idle -> {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.reassess_vital_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRunReassessment,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("vitals_run_reassessment_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reassess_cta))
                }
            }

            is ProactiveReassessmentUiState.Loading -> {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Text(
                        text = stringResource(R.string.reassess_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            is ProactiveReassessmentUiState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                SanctuaryErrorBanner(message = reassessmentState.message, onRetry = onRunReassessment)
            }

            is ProactiveReassessmentUiState.Success -> {
                Spacer(modifier = Modifier.height(10.dp))
                ProactiveReassessmentResultBody(
                    result = reassessmentState.result,
                    onReportAiResponse = onReportAiResponse
                )
            }
        }
    }
}

@Composable
private fun ProactiveReassessmentResultBody(
    result: ProactiveReassessmentResult,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UrgencyBadge(urgencyLevel = result.urgencyLevel)
        Text(
            text = result.headline,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = result.clinicalReasoning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (result.immediateNextSteps.isNotEmpty()) {
            Text(
                text = "Immediate Next Steps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            result.immediateNextSteps.forEach { step ->
                Text(
                    text = "• $step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (result.redFlagsToWatch.isNotEmpty()) {
            Text(
                text = "Seek Urgent Care If You Notice",
                style = MaterialTheme.typography.labelMedium,
                color = LocalSanaColors.current.urgentFg
            )
            result.redFlagsToWatch.forEach { flag ->
                Text(
                    text = "• $flag",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (result.doctorPrepQuestion.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Ask your clinician: \"${result.doctorPrepQuestion}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        AiSafetyFooter(
            featureArea = AiFeatureArea.PROACTIVE_REASSESSMENT,
            contentSnippet = "${result.headline}: ${result.clinicalReasoning}",
            onReportClick = onReportAiResponse
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LabBiomarkersSectionContent(
    labBiomarkersState: CloudUiState<List<LabBiomarker>>,
    labScanState: LabScanUiState,
    isSavingLabBatch: Boolean,
    autoMaskPiiBeforeScan: Boolean = true,
    pendingLabCameraBitmap: Bitmap? = null,
    onConsumePendingLabCameraBitmap: () -> Unit = {},
    onLaunchLabCamera: () -> Unit,
    onScanLabBitmap: (Bitmap) -> Unit,
    onToggleExtractedMarker: (Int) -> Unit,
    onSaveExtractedMarkers: () -> Unit,
    onClearLabScan: () -> Unit,
    onOpenManualDialog: () -> Unit,
    onDeleteBiomarker: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDecodingGallery by remember { mutableStateOf(false) }
    var pendingPiiBitmap by remember { mutableStateOf<Bitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(pendingLabCameraBitmap) {
        if (pendingLabCameraBitmap != null) {
            if (autoMaskPiiBeforeScan) {
                pendingPiiBitmap = pendingLabCameraBitmap
            } else {
                onScanLabBitmap(pendingLabCameraBitmap)
            }
            onConsumePendingLabCameraBitmap()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecodingGallery = true
            scope.launch {
                val bmp = loadDownscaledBitmap(context, uri)
                isDecodingGallery = false
                if (bmp != null) {
                    if (autoMaskPiiBeforeScan) {
                        pendingPiiBitmap = bmp
                    } else {
                        onScanLabBitmap(bmp)
                    }
                } else {
                    onShowMessage("Could not decode the selected lab photo. Please try another image.")
                }
            }
        }
    }

    pendingPiiBitmap?.let { rawBitmap ->
        PiiRedactionPreviewSheet(
            sourceBitmap = rawBitmap,
            scanPurposeTitle = "Lab Report Biomarker Scanner",
            confirmButtonLabel = "Scan Redacted Lab Report",
            onDismiss = { pendingPiiBitmap = null },
            onConfirmUpload = { finalBitmap ->
                pendingPiiBitmap = null
                onScanLabBitmap(finalBitmap)
            }
        )
    }

    val allMarkers = (labBiomarkersState as? CloudUiState.Success)?.data.orEmpty()
    val groupedByMarker = remember(allMarkers) {
        allMarkers.groupBy { it.markerName.trim().lowercase() }
            .values
            .map { list -> list.sortedByDescending { it.panelDateKey } }
            .sortedByDescending { it.firstOrNull()?.panelDateKey.orEmpty() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // AI Vision Lab Scanner Banner
        SanctuaryCard(modifier = Modifier.testTag("labs_scan_banner_card")) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Biotech,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.labs_scan_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.labs_scan_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLaunchLabCamera,
                    enabled = labScanState !is LabScanUiState.Scanning && !isDecodingGallery,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("labs_scan_camera_button")
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.labs_scan_camera), maxLines = 1)
                }
                FilledTonalButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = labScanState !is LabScanUiState.Scanning && !isDecodingGallery,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("labs_scan_gallery_button")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.labs_scan_gallery), maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenManualDialog,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("labs_add_manual_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.labs_add_manual))
            }
        }

        // Scanning / Review State Card
        when (labScanState) {
            is LabScanUiState.Scanning -> {
                SanctuaryCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.testTag("labs_scanning_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = stringResource(R.string.labs_scanning_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            is LabScanUiState.Error -> {
                SanctuaryErrorBanner(message = labScanState.message)
            }

            is LabScanUiState.Success -> {
                val panel = labScanState.result
                SanctuaryCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier.testTag("labs_review_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extracted Lab Panel (${panel.panelDateKey})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearLabScan,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss scan")
                        }
                    }
                    Text(
                        text = panel.panelSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.labs_review_banner),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    panel.biomarkers.forEachIndexed { idx, draft ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, LocalSanaColors.current.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = draft.selected,
                                    onCheckedChange = { onToggleExtractedMarker(idx) },
                                    modifier = Modifier.testTag("lab_draft_checkbox_$idx")
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draft.markerName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${draft.value} ${draft.unit}" +
                                            if (draft.referenceRangeText.isNotBlank()) " • Ref: ${draft.referenceRangeText}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LabStatusPill(status = draft.status)
                            }
                        }
                    }
                    if (panel.clinicianDiscussionPoints.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Questions for Your Doctor",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        panel.clinicianDiscussionPoints.forEach { pt ->
                            Text(
                                text = "• $pt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSaveExtractedMarkers,
                        enabled = !isSavingLabBatch && panel.biomarkers.any { it.selected },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("labs_save_extracted_button")
                    ) {
                        if (isSavingLabBatch) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.labs_save_selected))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AiSafetyFooter(
                        featureArea = AiFeatureArea.LAB_BIOMARKER_SCAN,
                        contentSnippet = panel.panelSummary,
                        onReportClick = onReportAiResponse
                    )
                }
            }

            LabScanUiState.Idle -> Unit
        }

        // Cloud Biomarker Trend Cards
        when (labBiomarkersState) {
            is CloudUiState.Loading -> {
                GlassShimmerCard(lines = 2)
                GlassShimmerCard(lines = 2)
            }

            is CloudUiState.Error -> {
                SanctuaryErrorBanner(message = labBiomarkersState.message)
            }

            is CloudUiState.Success -> {
                if (groupedByMarker.isEmpty()) {
                    EmptySanctuaryState(
                        illustrationKind = HealthEmptyIllustrationKind.VITALS,
                        title = "No lab biomarkers tracked yet",
                        subtitle = "Scan a bloodwork printout with AI Vision or add markers like HbA1c, LDL, Vitamin D, or TSH manually.",
                        actionLabel = stringResource(R.string.labs_add_manual),
                        actionTestTag = "labs_empty_add_button",
                        onAction = onOpenManualDialog
                    )
                } else {
                    groupedByMarker.forEach { series ->
                        val latest = series.first()
                        val previous = series.getOrNull(1)
                        SanctuaryCard(modifier = Modifier.testTag("lab_marker_card_${latest.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = latest.parsedCategory.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = latest.markerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LabStatusPill(status = latest.parsedStatus)
                                    IconButton(
                                        onClick = { onDeleteBiomarker(latest.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("lab_delete_${latest.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = stringResource(R.string.cd_delete_item),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            GlassMetricValueRow(
                                value = latest.formattedValue,
                                unit = latest.unit,
                                valueStyle = MaterialTheme.typography.headlineSmall
                            )
                            val metaParts = buildList {
                                if (latest.panelDateKey.isNotBlank()) add("Date: ${latest.panelDateKey}")
                                if (latest.referenceRangeText.isNotBlank()) add("Ref: ${latest.referenceRangeText}")
                                if (previous != null) {
                                    val delta = latest.value - previous.value
                                    val sign = if (delta >= 0) "+" else ""
                                    add("Prev ${previous.formattedValue} (${sign}${String.format("%.1f", delta)})")
                                }
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (latest.labNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latest.labNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (series.size > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = LocalSanaColors.current.subtleDivider)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    series.take(5).forEach { pt ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${pt.panelDateKey}: ${pt.formattedValue} ${pt.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    }
}

@Composable
private fun LabStatusPill(status: LabBiomarkerStatus) {
    val sanaColors = LocalSanaColors.current
    val accent = when (status) {
        LabBiomarkerStatus.OPTIMAL -> sanaColors.selfCareFg
        LabBiomarkerStatus.BORDERLINE -> sanaColors.doctorSoonFg
        LabBiomarkerStatus.LOW -> sanaColors.doctorSoonFg
        LabBiomarkerStatus.HIGH -> sanaColors.urgentFg
    }
    GlassStatusPill(label = status.label, accentColor = accent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManualLabBiomarkerDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        markerName: String,
        category: LabBiomarkerCategory,
        value: Double,
        unit: String,
        referenceRangeText: String,
        status: LabBiomarkerStatus,
        panelDateKey: String,
        labNote: String
    ) -> Unit
) {
    var markerName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(LabBiomarkerCategory.METABOLIC) }
    var valueText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mg/dL") }
    var refRange by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(LabBiomarkerStatus.OPTIMAL) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Lab Biomarker", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = markerName,
                    onValueChange = { if (it.length <= 80) markerName = it; error = null },
                    label = { Text("Biomarker (e.g., HbA1c, LDL, Vitamin D, TSH)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_lab_name_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(8); error = null },
                        label = { Text("Value") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_lab_value_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { if (it.length <= 24) unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("dialog_lab_unit_input")
                    )
                }
                OutlinedTextField(
                    value = refRange,
                    onValueChange = { if (it.length <= 80) refRange = it },
                    label = { Text("Reference Range (optional)") },
                    placeholder = { Text("e.g., 4.0 - 5.6 %") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LabBiomarkerStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        LabBiomarkerCategory.METABOLIC,
                        LabBiomarkerCategory.LIPIDS,
                        LabBiomarkerCategory.THYROID,
                        LabBiomarkerCategory.VITAMINS,
                        LabBiomarkerCategory.CBC
                    ).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 300) note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val numeric = valueText.toDoubleOrNull()
                    when {
                        markerName.trim().isEmpty() -> error = "Enter a biomarker name."
                        numeric == null || numeric < 0.0 || numeric > 100000.0 -> error = "Enter a valid numeric value."
                        else -> onConfirm(
                            markerName.trim(),
                            category,
                            numeric,
                            unit.trim().ifBlank { "units" },
                            refRange.trim(),
                            status,
                            "",
                            note.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("dialog_lab_save_button")
            ) {
                Text("Save Biomarker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
