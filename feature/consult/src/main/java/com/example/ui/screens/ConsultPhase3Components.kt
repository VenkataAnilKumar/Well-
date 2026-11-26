package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiFeatureArea
import com.example.data.model.ProposedChatAction
import com.example.data.model.ProposedChatActionStatus
import com.example.data.model.ProposedChatActionType
import com.example.data.model.TrustedMedicalCitation
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors

/**
 * Compact hands-free TTS Listen / Stop pill for Consultation Report and AI Doctor chat replies.
 */
@Composable
fun SpeakReadoutPill(
    isSpeaking: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(999.dp),
        color = if (isSpeaking) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        },
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isSpeaking) Icons.Filled.StopCircle else Icons.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Stop spoken readout" else "Listen to AI response",
                tint = if (isSpeaking) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isSpeaking) "Stop" else "Listen",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSpeaking) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

/**
 * Individual 1-tap confirmation card for an AI-proposed follow-up action.
 * Never mutates Firestore until the user explicitly taps Confirm.
 */
@Composable
fun ProposedChatActionCard(
    action: ProposedChatAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val icon = when (action.actionType) {
        ProposedChatActionType.ADD_CARE_TASK -> Icons.Filled.AddTask
        ProposedChatActionType.ADD_MEDICATION -> Icons.Filled.Medication
        ProposedChatActionType.LOG_VITAL -> Icons.Filled.MonitorHeart
        ProposedChatActionType.SCHEDULE_FOLLOWUP -> Icons.Filled.NotificationsActive
    }

    SanctuaryCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = if (action.status == ProposedChatActionStatus.CONFIRMED) {
            sanaColors.healthyGreen.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        },
        modifier = Modifier.testTag("proposed_action_card_${action.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = action.actionType.label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = action.actionType.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (action.status) {
            ProposedChatActionStatus.CONFIRMED -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = sanaColors.healthyGreen.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Saved",
                            tint = sanaColors.healthyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Saved to your Well+ sanctuary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = sanaColors.healthyGreen
                        )
                    }
                }
            }

            ProposedChatActionStatus.SAVING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Saving confirmed action…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ProposedChatActionStatus.PENDING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .minimumInteractiveComponentSize()
                            .testTag("confirm_proposed_action_${action.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("dismiss_proposed_action_${action.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                AiSafetyFooter(
                    featureArea = AiFeatureArea.CHAT_ACTION_SUGGESTION,
                    contentSnippet = "${action.actionType.label}: ${action.title} — ${action.subtitle}",
                    onReportClick = onReportAiResponse
                )
            }

            ProposedChatActionStatus.DISMISSED -> Unit
        }
    }
}

/**
 * Trusted allowlisted medical references rendered as tappable HTTPS chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrustedCitationsSection(
    citations: List<TrustedMedicalCitation>,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (citations.isEmpty()) return
    val context = LocalContext.current

    SanctuaryCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.testTag("trusted_citations_section")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Trusted Medical References",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Verified public-health sources for deeper reading.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            citations.forEachIndexed { idx, citation ->
                AssistChip(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(citation.url))
                            context.startActivity(intent)
                        }.onFailure {
                            onShowMessage("No browser available to open ${citation.sourceOrganization}.")
                        }
                    },
                    label = {
                        Text(
                            text = "${citation.sourceOrganization}: ${citation.title}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open ${citation.sourceOrganization} reference",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("trusted_citation_chip_$idx")
                )
            }
        }
    }
}
