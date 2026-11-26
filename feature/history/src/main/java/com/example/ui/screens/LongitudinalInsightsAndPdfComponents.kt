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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.DoctorExportOptions
import com.example.data.model.AiFeatureArea
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.InsightsUiState

@Composable
fun LongitudinalInsightsCard(
    insightsState: InsightsUiState,
    hasAnyHealthData: Boolean,
    onGenerateInsights: () -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    val sanaColors = LocalSanaColors.current

    SanctuaryCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("longitudinal_insights_card")
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
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.insights_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Correlates sleep, mood, vitals, medications, and symptoms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (insightsState is InsightsUiState.Success) {
                TextButton(
                    onClick = onGenerateInsights,
                    modifier = Modifier.testTag("refresh_insights_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (insightsState) {
            is InsightsUiState.Idle -> {
                Text(
                    text = if (hasAnyHealthData) {
                        "Run a private Gemini synthesis across your recent check-ins, vitals, medication adherence, and consultations."
                    } else {
                        "Log a check-in, vital sign, or symptom consultation to unlock your personalized 30-day synthesis."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onGenerateInsights,
                    enabled = hasAnyHealthData,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_insights_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.insights_generate))
                }
            }

            is InsightsUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Synthesizing your 30-day wellness patterns, vitals, and medication context…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is InsightsUiState.Error -> {
                SanctuaryErrorBanner(
                    message = insightsState.message,
                    onRetry = onGenerateInsights
                )
            }

            is InsightsUiState.Success -> {
                val res = insightsState.result
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = res.trajectoryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = res.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = res.executiveSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (res.correlations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = sanaColors.subtleDivider)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Patterns Observed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    res.correlations.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (res.medicationAndSafetyNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sanaColors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Medication & Allergy Awareness",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            res.medicationAndSafetyNotes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (res.actionableRecommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Next-Week Focus",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    res.actionableRecommendations.forEach { rec ->
                        Text(
                            text = "• $rec",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (res.clinicianDiscussionPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Clinician Talking Points",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    res.clinicianDiscussionPoints.forEach { pt ->
                        Text(
                            text = "• $pt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                AiSafetyFooter(
                    featureArea = AiFeatureArea.LONGITUDINAL_SYNTHESIS,
                    contentSnippet = "${res.headline}: ${res.executiveSummary}",
                    onReportClick = onReportAiResponse
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorPdfExportDialog(
    isExporting: Boolean,
    isSynthesizingSoap: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmExport: (DoctorExportOptions) -> Unit
) {
    var daysWindow by remember { mutableIntStateOf(30) }
    var includeSoapAiBrief by remember { mutableStateOf(true) }
    var includeProfile by remember { mutableStateOf(true) }
    var includeMedications by remember { mutableStateOf(true) }
    var includeVitals by remember { mutableStateOf(true) }
    var includeLabBiomarkers by remember { mutableStateOf(true) }
    var includeConsultations by remember { mutableStateOf(true) }
    var includeWellness by remember { mutableStateOf(true) }
    var includeMentalWellbeing by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.pdf_export_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Generate a structured clinical brief to print or share before your next doctor visit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Timeframe", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(14, 30, 90).forEach { days ->
                        FilterChip(
                            selected = daysWindow == days,
                            onClick = { daysWindow = days },
                            label = { Text("Last ${days}d") }
                        )
                    }
                }
                ExportOptionCheckbox("AI SOAP Pre-Visit Brief (Page 1)", includeSoapAiBrief) { includeSoapAiBrief = it }
                ExportOptionCheckbox("Clinical Profile & Allergies", includeProfile) { includeProfile = it }
                ExportOptionCheckbox("Active Medications & Adherence", includeMedications) { includeMedications = it }
                ExportOptionCheckbox("Recent Vitals & Ranges", includeVitals) { includeVitals = it }
                ExportOptionCheckbox("Recent Lab Biomarkers", includeLabBiomarkers) { includeLabBiomarkers = it }
                ExportOptionCheckbox("Daily Wellness Averages", includeWellness) { includeWellness = it }
                ExportOptionCheckbox("Mental Wellbeing (PHQ-9 / GAD-7)", includeMentalWellbeing) { includeMentalWellbeing = it }
                ExportOptionCheckbox("Symptom History & Doctor Questions", includeConsultations) { includeConsultations = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmExport(
                        DoctorExportOptions(
                            daysWindow = daysWindow,
                            includeProfile = includeProfile,
                            includeMedications = includeMedications,
                            includeVitals = includeVitals,
                            includeLabBiomarkers = includeLabBiomarkers,
                            includeConsultations = includeConsultations,
                            includeWellnessSummary = includeWellness,
                            includeMentalWellbeing = includeMentalWellbeing,
                            includeSoapAiBrief = includeSoapAiBrief
                        )
                    )
                },
                enabled = !isExporting,
                modifier = Modifier.testTag("confirm_export_pdf_button")
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSynthesizingSoap) "Synthesizing SOAP…" else "Generating PDF…")
                } else {
                    Text("Generate & Share PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExporting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExportOptionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
