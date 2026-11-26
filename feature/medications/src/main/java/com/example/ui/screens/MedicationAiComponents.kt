package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.AiFeatureArea
import com.example.data.model.DrugInteractionAlert
import com.example.data.model.InteractionSeverity
import com.example.data.model.MedicationInteractionReport
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.InteractionCheckUiState
import com.example.ui.viewmodel.PrescriptionScanUiState

@Composable
internal fun MedicationPrescriptionScannerCard(
    scanState: PrescriptionScanUiState,
    isDecodingGalleryPhoto: Boolean,
    onScanWithCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onDismissError: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val isBusy = scanState is PrescriptionScanUiState.Scanning || isDecodingGalleryPhoto

    SanctuaryCard(
        modifier = Modifier.testTag("meds_ai_scanner_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = sanaColors.medsTeal.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = null,
                        tint = sanaColors.medsTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.meds_ai_scan_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = sanaColors.medsTeal.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "AI Vision",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = sanaColors.medsTeal,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.meds_ai_scan_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isBusy) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.5.dp, sanaColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = sanaColors.medsTeal,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isDecodingGalleryPhoto) {
                            "Preparing prescription photo…"
                        } else {
                            stringResource(R.string.meds_ai_scan_loading)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onScanWithCamera,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sanaColors.medsTeal,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("meds_scan_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.meds_ai_scan_camera),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = onPickFromGallery,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("meds_scan_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.meds_ai_scan_gallery),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (scanState is PrescriptionScanUiState.Error) {
            Spacer(modifier = Modifier.height(10.dp))
            SanctuaryErrorBanner(
                message = scanState.message,
                onRetry = onDismissError
            )
        }
    }
}

@Composable
internal fun MedicationInteractionCheckerCard(
    activeMedicationCount: Int,
    profileAllergyCount: Int,
    interactionState: InteractionCheckUiState,
    onRunCheck: () -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    val sanaColors = LocalSanaColors.current
    val isLoading = interactionState is InteractionCheckUiState.Loading

    SanctuaryCard(
        modifier = Modifier.testTag("meds_interaction_checker_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.meds_ai_interaction_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append("Screens $activeMedicationCount active medication")
                        if (activeMedicationCount != 1) append("s")
                        if (profileAllergyCount > 0) {
                            append(" against $profileAllergyCount known allerg")
                            append(if (profileAllergyCount == 1) "y" else "ies")
                        } else {
                            append(" for drug, allergy & food conflicts")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (interactionState) {
            InteractionCheckUiState.Idle -> {
                Button(
                    onClick = onRunCheck,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("meds_check_interactions_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.meds_ai_interaction_cta),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            InteractionCheckUiState.Loading -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.meds_ai_interaction_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            is InteractionCheckUiState.Error -> {
                SanctuaryErrorBanner(
                    message = interactionState.message,
                    onRetry = onRunCheck
                )
            }

            is InteractionCheckUiState.Success -> {
                InteractionReportBody(
                    report = interactionState.report,
                    isRefreshing = isLoading,
                    onRecheck = onRunCheck,
                    onReportAiResponse = onReportAiResponse
                )
            }
        }
    }
}

@Composable
private fun InteractionReportBody(
    report: MedicationInteractionReport,
    isRefreshing: Boolean,
    onRecheck: () -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val (bannerBg, bannerFg, severityIcon) = severityVisuals(report.highestSeverity)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Apple Health–style severity summary banner
        Surface(
            color = bannerBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, bannerFg.copy(alpha = 0.28f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            imageVector = severityIcon,
                            contentDescription = report.highestSeverity.label,
                            tint = bannerFg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = report.overallStatus,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = bannerFg
                        )
                    }
                    Surface(
                        color = bannerFg.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = report.highestSeverity.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = bannerFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = report.executiveSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Individual interaction pairs
        if (report.interactions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "IDENTIFIED FACTORS (${report.interactions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                report.interactions.forEach { alert ->
                    InteractionAlertRow(alert = alert)
                }
            }
        }

        // Lifestyle, alcohol, grapefruit & timing notes
        if (report.lifestyleAndFoodNotes.isNotEmpty()) {
            HorizontalDivider(color = sanaColors.subtleDivider)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = sanaColors.medsTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Food, Alcohol & Timing Notes",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                report.lifestyleAndFoodNotes.forEach { note ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = sanaColors.medsTeal,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                        ) {}
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Questions for pharmacist
        if (report.pharmacistQuestions.isNotEmpty()) {
            HorizontalDivider(color = sanaColors.subtleDivider)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalPharmacy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ask Your Pharmacist",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                report.pharmacistQuestions.forEach { question ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                        ) {}
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Footer disclaimer + Re-check button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.meds_ai_interaction_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onRecheck,
                enabled = !isRefreshing,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("meds_recheck_interactions_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.meds_ai_interaction_recheck),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        AiSafetyFooter(
            featureArea = AiFeatureArea.DRUG_INTERACTION,
            contentSnippet = "${report.overallStatus}: ${report.executiveSummary}",
            onReportClick = onReportAiResponse
        )
    }
}

@Composable
private fun InteractionAlertRow(alert: DrugInteractionAlert) {
    val sanaColors = LocalSanaColors.current
    val (bg, fg, icon) = severityVisuals(alert.severity)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = alert.pairLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = alert.severity.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (alert.summary.isNotBlank()) {
                Text(
                    text = alert.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (alert.recommendation.isNotBlank()) {
                Text(
                    text = "Next step: ${alert.recommendation}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun severityVisuals(severity: InteractionSeverity): Triple<Color, Color, ImageVector> {
    val sanaColors = LocalSanaColors.current
    return when (severity) {
        InteractionSeverity.CRITICAL -> Triple(
            sanaColors.emergencyBg,
            sanaColors.emergencyFg,
            Icons.Filled.Error
        )
        InteractionSeverity.SERIOUS -> Triple(
            sanaColors.urgentBg,
            sanaColors.urgentFg,
            Icons.Filled.Warning
        )
        InteractionSeverity.MODERATE -> Triple(
            sanaColors.doctorSoonBg,
            sanaColors.doctorSoonFg,
            Icons.Filled.Info
        )
        InteractionSeverity.NONE -> Triple(
            sanaColors.selfCareBg,
            sanaColors.selfCareFg,
            Icons.Filled.CheckCircle
        )
    }
}
