package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.CriticalVitalAlert
import com.example.core.LegalDocumentType
import com.example.core.LegalPolicyConfig
import com.example.core.RegionalEmergencyDirectory
import com.example.core.SymptomRedFlagResult
import com.example.core.VoiceAndEmergencyHelper
import com.example.data.model.AiFeatureArea
import com.example.data.model.AiSafetyReportReason
import com.example.data.model.HealthProfile
import com.example.ui.theme.LocalSanaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Play Generative AI Policy compliant provenance & reporting footer attached to every AI output.
 */
@Composable
fun AiSafetyFooter(
    featureArea: AiFeatureArea,
    contentSnippet: String,
    generatedAtMillis: Long? = null,
    onReportClick: (AiFeatureArea, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeLabel = remember(generatedAtMillis) {
        generatedAtMillis?.takeIf { it > 0L }?.let {
            SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(it))
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.ai_safety_provenance_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (timeLabel != null) {
                        Text(
                            text = "Generated $timeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            TextButton(
                onClick = { onReportClick(featureArea, contentSnippet.take(400)) },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("report_ai_${featureArea.name.lowercase(Locale.US)}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = stringResource(R.string.cd_report_ai_response),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.ai_report_action),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Structured modal bottom sheet allowing users to report inaccurate, unsafe, or offensive AI outputs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSafetyReportSheet(
    featureArea: AiFeatureArea,
    contentSnippet: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AiSafetyReportReason, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf(AiSafetyReportReason.INACCURATE) }
    var userNote by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GppMaybe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.ai_report_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Feature: ${featureArea.displayLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(R.string.ai_report_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (contentSnippet.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${contentSnippet.take(180)}${if (contentSnippet.length > 180) "…" else ""}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AiSafetyReportReason.entries.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(enabled = !isSubmitting) { selectedReason = reason }
                            .testTag("ai_report_reason_${reason.name.lowercase(Locale.US)}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { if (!isSubmitting) selectedReason = reason }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reason.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = reason.helper,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = userNote,
                onValueChange = { userNote = it.take(500) },
                label = { Text("Optional details (what seemed inaccurate or unsafe?)") },
                minLines = 2,
                maxLines = 4,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_report_note_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSubmit(selectedReason, userNote) },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("ai_report_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Report")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * Deterministic critical vital-sign emergency alert modal with localized emergency dialing.
 */
@Composable
fun CriticalVitalAlertDialog(
    alert: CriticalVitalAlert,
    profile: HealthProfile?,
    emergencyRegionOverride: String,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val region = remember(context, emergencyRegionOverride, profile?.preferredEmergencyCountry) {
        val code = emergencyRegionOverride.ifBlank { profile?.preferredEmergencyCountry.orEmpty() }
        RegionalEmergencyDirectory.resolve(context, code)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.LocalHospital,
                contentDescription = null,
                tint = sanaColors.emergencyFg,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = alert.headline,
                color = sanaColors.emergencyFg,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = sanaColors.emergencyBg,
                    border = BorderStroke(1.dp, sanaColors.emergencyFg.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LOGGED READING: ${alert.formattedReading}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = sanaColors.emergencyFg
                        )
                        Text(
                            text = alert.guidance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    alert.immediateSteps.forEach { step ->
                        Text(
                            text = "• $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = {
                        if (!VoiceAndEmergencyHelper.openDialer(context, region.emergencyNumber)) {
                            onShowMessage("Dialer unavailable. Call ${region.emergencyLabel}.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sanaColors.emergencyFg,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("critical_vital_call_emergency_button")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call ${region.emergencyLabel}")
                }

                val nurseNumber = region.nurseAdviceNumber
                if (!nurseNumber.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = {
                            if (!VoiceAndEmergencyHelper.openDialer(context, nurseNumber)) {
                                onShowMessage("Dialer unavailable. Call ${region.nurseAdviceLabel ?: nurseNumber}.")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("critical_vital_call_811_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(region.nurseAdviceLabel ?: "Call 811 Nurse Line")
                    }
                }

                if (profile?.hasEmergencyContact == true) {
                    FilledTonalButton(
                        onClick = {
                            if (!VoiceAndEmergencyHelper.openDialer(context, profile.emergencyContactPhone)) {
                                onShowMessage("Call ${profile.emergencyContactName}: ${profile.emergencyContactPhone}")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val label = profile.emergencyContactName.ifBlank { profile.emergencyContactPhone }
                        Text("Call $label")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("critical_vital_dismiss_button")
            ) {
                Text("I Understand")
            }
        }
    )
}

/**
 * Deterministic pre-AI symptom red-flag banner/modal triggered immediately before or independently of Gemini.
 */
@Composable
fun SymptomRedFlagDialog(
    redFlag: SymptomRedFlagResult,
    profile: HealthProfile?,
    emergencyRegionOverride: String,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val region = remember(context, emergencyRegionOverride, profile?.preferredEmergencyCountry) {
        val code = emergencyRegionOverride.ifBlank { profile?.preferredEmergencyCountry.orEmpty() }
        RegionalEmergencyDirectory.resolve(context, code)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = sanaColors.emergencyFg,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = redFlag.headline,
                color = sanaColors.emergencyFg,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = redFlag.rationale,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    redFlag.immediateSteps.forEach { step ->
                        Text(
                            text = "• $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (redFlag.isMentalHealthCrisis && region.crisisNumber.isNotBlank()) {
                    Button(
                        onClick = {
                            if (!VoiceAndEmergencyHelper.openDialer(context, region.crisisNumber)) {
                                onShowMessage("Call or text ${region.crisisLabel}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sanaColors.emergencyFg,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("red_flag_call_crisis_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call ${region.crisisLabel}")
                    }
                }

                Button(
                    onClick = {
                        if (!VoiceAndEmergencyHelper.openDialer(context, region.emergencyNumber)) {
                            onShowMessage("Dialer unavailable. Call ${region.emergencyLabel}.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (redFlag.isMentalHealthCrisis) MaterialTheme.colorScheme.errorContainer else sanaColors.emergencyFg,
                        contentColor = if (redFlag.isMentalHealthCrisis) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("red_flag_call_emergency_button")
                ) {
                    Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call ${region.emergencyLabel}")
                }

                if (redFlag.isMentalHealthCrisis) {
                    OutlinedButton(
                        onClick = {
                            if (!LegalPolicyConfig.openValidatedHttpsUrl(context, region.internationalCrisisDirectoryUrl)) {
                                onShowMessage("Visit findahelpline.com for free confidential crisis support worldwide.")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("International Crisis Directory")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Dismiss")
            }
        }
    )
}

/**
 * Non-blocking offline banner shown when device connectivity drops.
 */
@Composable
fun OfflineStatusBanner(
    isOnline: Boolean,
    urgentSafetyNotice: String? = null,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !urgentSafetyNotice.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = sanaColors.urgentBg,
                border = BorderStroke(1.dp, sanaColors.urgentFg.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("urgent_safety_notice_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = sanaColors.urgentFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = urgentSafetyNotice.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isOnline,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_status_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.offline_mode_banner),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * In-app scrollable legal policy viewer with validated HTTPS web link launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalPolicyViewerSheet(
    document: LegalDocumentType,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last updated: ${document.lastUpdated}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = document.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            document.sections.forEach { (heading, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Official HTTPS URL: ${document.publicHttpsUrl}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Button(
                onClick = {
                    if (!LegalPolicyConfig.openValidatedHttpsUrl(context, document.publicHttpsUrl)) {
                        onShowMessage("No browser available. Official URL: ${document.publicHttpsUrl}")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("legal_open_https_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Official Web Page (HTTPS)")
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Close")
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
