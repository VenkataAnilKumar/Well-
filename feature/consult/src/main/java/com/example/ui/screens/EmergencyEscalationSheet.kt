package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.LegalPolicyConfig
import com.example.core.RegionalEmergencyDirectory
import com.example.core.VoiceAndEmergencyHelper
import com.example.data.model.Consultation
import com.example.data.model.HealthProfile
import com.example.data.model.UrgencyLevel
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyEscalationSheet(
    consultation: Consultation,
    profile: HealthProfile?,
    healthRegion: HealthRegionProfile = HealthRegionDirectory.US,
    emergencyRegionOverride: String = "",
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEmergency = consultation.parsedUrgency == UrgencyLevel.EMERGENCY
    val accentFg = if (isEmergency) sanaColors.emergencyFg else sanaColors.urgentFg
    val accentBg = if (isEmergency) sanaColors.emergencyBg else sanaColors.urgentBg
    val region = remember(context, emergencyRegionOverride, profile?.preferredEmergencyCountry) {
        val code = emergencyRegionOverride.ifBlank { profile?.preferredEmergencyCountry.orEmpty() }
        RegionalEmergencyDirectory.resolve(context, code)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SanaTheme.glass.modalScrimFill
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = accentBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, accentFg.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isEmergency) Icons.Filled.LocalHospital else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = accentFg,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isEmergency) "SEEK EMERGENCY CARE NOW" else "PROMPT MEDICAL EVALUATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentFg
                        )
                        Text(
                            text = stringResource(R.string.emergency_sheet_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = consultation.parsedUrgency.guidance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // One-tap zero-permission ACTION_DIAL localized emergency button
            Button(
                onClick = {
                    if (!VoiceAndEmergencyHelper.openDialer(context, region.emergencyNumber)) {
                        onShowMessage("Dialer unavailable on this device. Call ${region.emergencyLabel}.")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = sanaColors.emergencyFg,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("emergency_dial_911_button")
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Call ${region.emergencyLabel}",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            val nurseNumber = region.nurseAdviceNumber ?: healthRegion.nurseAdviceNumber
            val nurseLabel = region.nurseAdviceLabel ?: healthRegion.nurseAdviceLabel
            if (nurseNumber != null && nurseLabel != null) {
                FilledTonalButton(
                    onClick = {
                        if (!VoiceAndEmergencyHelper.openDialer(context, nurseNumber)) {
                            onShowMessage("Dialer unavailable. Call $nurseLabel.")
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = sanaColors.medsTeal.copy(alpha = 0.14f),
                        contentColor = sanaColors.medsTeal
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("emergency_dial_811_button")
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_call_811),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = "Region: ${region.countryName} (change anytime in Profile)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (region.crisisNumber.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        if (!VoiceAndEmergencyHelper.openDialer(context, region.crisisNumber)) {
                            onShowMessage("Dialer unavailable. Crisis support: ${region.crisisLabel}")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("emergency_dial_crisis_button")
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crisis Support: ${region.crisisLabel}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (!LegalPolicyConfig.openValidatedHttpsUrl(context, region.internationalCrisisDirectoryUrl)) {
                            onShowMessage("Visit findahelpline.com for free confidential crisis support.")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("emergency_open_intl_crisis_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Find Local Crisis Helpline (findahelpline.com)")
                }
            }

            if (profile?.hasEmergencyContact == true) {
                FilledTonalButton(
                    onClick = {
                        if (!VoiceAndEmergencyHelper.openDialer(context, profile.emergencyContactPhone)) {
                            onShowMessage("Call ${profile.emergencyContactName}: ${profile.emergencyContactPhone}")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("emergency_dial_contact_button")
                ) {
                    Icon(Icons.Filled.ContactPhone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val contactLabel = profile.emergencyContactName.ifBlank { profile.emergencyContactPhone }
                    Text(
                        text = "${stringResource(R.string.emergency_call_contact)}: $contactLabel",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Dispatcher / Triage Nurse Ready Card
            Surface(
                color = SanaTheme.glass.cardFill,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SanaTheme.glass.borderMid)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "READY TO SHARE WITH TRIAGE / DISPATCHER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${consultation.title} • ${consultation.bodyArea} • Severity ${consultation.severity}/10 (${consultation.duration})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = consultation.symptomSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile != null && profile.allergies.isNotEmpty()) {
                        Text(
                            text = "Allergies: ${profile.allergies.joinToString(", ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentFg
                        )
                    }
                }
            }

            if (consultation.redFlags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Red-Flag Warning Signs",
                        style = MaterialTheme.typography.titleSmall,
                        color = accentFg
                    )
                    consultation.redFlags.forEach { flag ->
                        Text(
                            text = "• $flag",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("emergency_dismiss_button")
            ) {
                Text("Return to Consultation Report")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
