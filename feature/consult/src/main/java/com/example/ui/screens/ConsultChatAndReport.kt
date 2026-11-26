package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.R
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.VoiceAndEmergencyHelper
import com.example.core.loadDownscaledBitmap
import com.example.data.model.AiFeatureArea
import com.example.data.model.ChatMessage
import com.example.data.model.Consultation
import com.example.data.model.FollowUpStatus
import com.example.data.model.MessageRole
import com.example.data.model.ProposedChatAction
import com.example.data.model.ProposedChatActionStatus
import com.example.data.model.TrustedMedicalCitation
import com.example.data.model.UrgencyLevel
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.EmergencyAlertBanner
import com.example.ui.components.MedicalDisclaimerNote
import com.example.ui.components.PiiRedactionPreviewSheet
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.UrgencyBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.components.rememberWellPlusTtsController
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.ProactiveReassessmentUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsultationReportView(
    consultation: Consultation,
    addedSuggestionKeys: Set<String>,
    messageCount: Int,
    healthRegion: HealthRegionProfile = HealthRegionDirectory.US,
    citations: List<TrustedMedicalCitation> = emptyList(),
    onShowMessage: (String) -> Unit = {},
    onBackToIntake: () -> Unit,
    onOpenFollowUpChat: () -> Unit,
    onAddSuggestionToChecklist: (String) -> Unit,
    onOpenEmergencySheet: () -> Unit = {},
    onUpdateFollowUp: (FollowUpStatus, String) -> Unit = { _, _ -> },
    onScheduleFollowUpReminder: () -> Unit = {},
    worseningReassessmentState: ProactiveReassessmentUiState = ProactiveReassessmentUiState.Idle,
    onRunWorseningReassessment: (String) -> Unit = {},
    onDismissWorseningReassessment: () -> Unit = {},
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBackToIntake)
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val ttsController = rememberWellPlusTtsController()
    var selectedFollowUp by remember(consultation.id, consultation.followUpStatus) {
        mutableStateOf(consultation.parsedFollowUpStatus)
    }
    var followUpNote by remember(consultation.id, consultation.followUpNote) {
        mutableStateOf(consultation.followUpNote)
    }

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
                    onClick = onBackToIntake,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("report_new_check_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Check")
                }
                Text(
                    text = formatTimestamp(consultation.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            EmergencyAlertBanner(urgencyLevel = consultation.parsedUrgency)
        }

        if (consultation.parsedUrgency == UrgencyLevel.URGENT ||
            consultation.parsedUrgency == UrgencyLevel.EMERGENCY
        ) {
            item {
                val isEmergency = consultation.parsedUrgency == UrgencyLevel.EMERGENCY
                Button(
                    onClick = onOpenEmergencySheet,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmergency) sanaColors.emergencyFg else sanaColors.urgentFg,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("report_open_emergency_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEmergency) {
                            "Open Immediate Emergency Actions"
                        } else {
                            "View Urgent Care & Emergency Contacts"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (healthRegion.isCanada && consultation.parsedUrgency != UrgencyLevel.SELF_CARE) {
            item {
                FilledTonalButton(
                    onClick = {
                        VoiceAndEmergencyHelper.openDialer(context, healthRegion.nurseAdviceNumber ?: "811")
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = sanaColors.medsTeal.copy(alpha = 0.14f),
                        contentColor = sanaColors.medsTeal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("report_call_811_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SupportAgent,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_call_811),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Clinical Summary Card
        item {
            SanctuaryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UrgencyBadge(urgencyLevel = consultation.parsedUrgency)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${consultation.bodyArea} • ${consultation.severity}/10",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = consultation.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = consultation.parsedUrgency.guidance,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = consultation.aiSummary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (consultation.hadPhotoAttachment) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Includes multimodal photo analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val reportUtteranceId = "report_${consultation.id}"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpeakReadoutPill(
                        isSpeaking = ttsController.speakingId == reportUtteranceId,
                        onToggle = {
                            val readoutText = buildString {
                                append(consultation.title).append(". ")
                                append(consultation.parsedUrgency.label).append(". ")
                                append(consultation.aiSummary).append(" ")
                                if (consultation.careSuggestions.isNotEmpty()) {
                                    append("Care suggestions: ")
                                    append(consultation.careSuggestions.joinToString(". "))
                                }
                            }
                            ttsController.toggleSpeak(
                                id = reportUtteranceId,
                                rawText = readoutText,
                                onUnavailable = onShowMessage
                            )
                        },
                        modifier = Modifier.testTag("report_tts_listen_button")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                AiSafetyFooter(
                    featureArea = AiFeatureArea.SYMPTOM_TRIAGE,
                    contentSnippet = "${consultation.title}: ${consultation.aiSummary}",
                    onReportClick = onReportAiResponse
                )
            }
        }

        // Follow-up Chat CTA
        item {
            Button(
                onClick = onOpenFollowUpChat,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("open_followup_chat_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (messageCount > 0) {
                        "Continue Follow-Up ($messageCount)"
                    } else {
                        stringResource(R.string.action_start_followup)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Personalized Care Suggestions (One-Tap Add to Checklist)
        if (consultation.careSuggestions.isNotEmpty()) {
            item {
                SanctuaryCard {
                    Text(
                        text = "Personalized Care Suggestions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any suggestion to add it to your Today Care Checklist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        consultation.careSuggestions.forEachIndexed { index, suggestion ->
                            val key = "${consultation.id}::$suggestion"
                            val isAdded = key in addedSuggestionKeys
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalButton(
                                        onClick = { onAddSuggestionToChecklist(suggestion) },
                                        enabled = !isAdded,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("add_suggestion_$index")
                                    ) {
                                        Icon(
                                            imageVector = if (isAdded) Icons.Filled.Check else Icons.Filled.AddTask,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAdded) {
                                                stringResource(R.string.action_added_to_checklist)
                                            } else {
                                                "Add"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Possible Causes
        if (consultation.possibleCauses.isNotEmpty()) {
            item {
                SanctuaryCard {
                    Text(
                        text = "Possibilities to Discuss",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        consultation.possibleCauses.forEach { cause ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .size(16.dp)
                                )
                                Text(
                                    text = cause,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Watch-Out Red Flags
        if (consultation.redFlags.isNotEmpty()) {
            item {
                val sanaColors = LocalSanaColors.current
                SanctuaryCard(
                    containerColor = sanaColors.urgentBg.copy(alpha = 0.55f),
                    borderColor = sanaColors.urgentFg.copy(alpha = 0.35f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = sanaColors.urgentFg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "When to Seek Immediate Care",
                            style = MaterialTheme.typography.titleMedium,
                            color = sanaColors.urgentFg
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        consultation.redFlags.forEach { flag ->
                            Text(
                                text = "• $flag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenEmergencySheet,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sanaColors.urgentFg.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("redflags_emergency_actions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalHospital,
                            contentDescription = null,
                            tint = sanaColors.urgentFg,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Emergency Contacts & Local Dialer",
                            color = sanaColors.urgentFg
                        )
                    }
                }
            }
        }

        // Questions for Your Doctor
        if (consultation.doctorQuestions.isNotEmpty()) {
            item {
                SanctuaryCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Questions for Your Clinician",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        consultation.doctorQuestions.forEach { question ->
                            Text(
                                text = "• $question",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Trusted Medical References
        if (citations.isNotEmpty()) {
            item(key = "report_trusted_citations") {
                TrustedCitationsSection(
                    citations = citations,
                    onShowMessage = onShowMessage
                )
            }
        }

        // Symptom Recovery Follow-Up Loop Card
        item {
            SanctuaryCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.testTag("consultation_followup_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Symptom Recovery Follow-Up",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record how these symptoms evolve for your doctor brief & 30-day AI insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        FollowUpStatus.IMPROVING,
                        FollowUpStatus.UNCHANGED,
                        FollowUpStatus.WORSENING,
                        FollowUpStatus.RESOLVED
                    ).forEach { status ->
                        FilterChip(
                            selected = selectedFollowUp == status,
                            onClick = { selectedFollowUp = status },
                            label = { Text(status.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("followup_status_${status.name}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = followUpNote,
                    onValueChange = { if (it.length <= 500) followUpNote = it },
                    placeholder = { Text("Optional recovery note (e.g., eased after rest and hydration)...") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("followup_note_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onUpdateFollowUp(selectedFollowUp, followUpNote) },
                        enabled = selectedFollowUp != FollowUpStatus.PENDING || followUpNote.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_followup_outcome_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Outcome")
                    }
                    OutlinedButton(
                        onClick = onScheduleFollowUpReminder,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("schedule_followup_48h_button")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("48h Nudge")
                    }
                }
            }
        }

        // Proactive Worsening Symptom Reassessment Card
        if (consultation.parsedFollowUpStatus == FollowUpStatus.WORSENING ||
            selectedFollowUp == FollowUpStatus.WORSENING ||
            worseningReassessmentState !is ProactiveReassessmentUiState.Idle
        ) {
            item(key = "worsening_reassessment_card") {
                SanctuaryCard(
                    containerColor = sanaColors.urgentBg.copy(alpha = 0.45f),
                    borderColor = sanaColors.urgentFg.copy(alpha = 0.35f),
                    modifier = Modifier.testTag("worsening_reassessment_card")
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
                                tint = sanaColors.urgentFg,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.reassess_worsening_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (worseningReassessmentState is ProactiveReassessmentUiState.Success) {
                            IconButton(
                                onClick = onDismissWorseningReassessment,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss reassessment")
                            }
                        }
                    }

                    when (worseningReassessmentState) {
                        is ProactiveReassessmentUiState.Idle -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.reassess_worsening_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onRunWorseningReassessment(followUpNote) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("worsening_reassess_button")
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
                            SanctuaryErrorBanner(
                                message = worseningReassessmentState.message,
                                onRetry = { onRunWorseningReassessment(followUpNote) }
                            )
                        }

                        is ProactiveReassessmentUiState.Success -> {
                            val res = worseningReassessmentState.result
                            Spacer(modifier = Modifier.height(10.dp))
                            UrgencyBadge(urgencyLevel = res.urgencyLevel)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = res.headline,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = res.clinicalReasoning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (res.immediateNextSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Updated Next Steps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                res.immediateNextSteps.forEach { step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "• $step",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val key = "${consultation.id}::$step"
                                        val isAdded = key in addedSuggestionKeys
                                        FilledTonalButton(
                                            onClick = { onAddSuggestionToChecklist(step) },
                                            enabled = !isAdded,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                if (isAdded) "Added" else "Add",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                            if (res.redFlagsToWatch.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Watch-Out Red Flags",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = sanaColors.urgentFg
                                )
                                res.redFlagsToWatch.forEach { flag ->
                                    Text(
                                        text = "• $flag",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (res.doctorPrepQuestion.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Ask your clinician: \"${res.doctorPrepQuestion}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            if (res.urgencyLevel == UrgencyLevel.URGENT || res.urgencyLevel == UrgencyLevel.EMERGENCY) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onOpenEmergencySheet,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = sanaColors.emergencyFg,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Urgent Care & Emergency Actions")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AiSafetyFooter(
                                featureArea = AiFeatureArea.PROACTIVE_REASSESSMENT,
                                contentSnippet = "${res.headline}: ${res.clinicalReasoning}",
                                onReportClick = onReportAiResponse
                            )
                        }
                    }
                }
            }
        }

        item {
            MedicalDisclaimerNote()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun FollowUpChatView(
    consultation: Consultation,
    messagesState: CloudUiState<List<ChatMessage>>,
    chatState: ChatUiState,
    proposedActions: List<ProposedChatAction> = emptyList(),
    citations: List<TrustedMedicalCitation> = emptyList(),
    autoMaskPiiBeforeScan: Boolean = true,
    onConfirmProposedAction: (String) -> Unit = {},
    onDismissProposedAction: (String) -> Unit = {},
    pendingCameraPhoto: Pair<Uri, Bitmap>? = null,
    onConsumePendingCameraPhoto: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onBackToReport: () -> Unit,
    onSendMessage: (String, Bitmap?) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBackToReport)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val ttsController = rememberWellPlusTtsController()
    val voiceUnavailableText = stringResource(R.string.voice_unavailable)

    var inputText by remember { mutableStateOf("") }
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPiiPhoto by remember { mutableStateOf<Pair<Uri, Bitmap>?>(null) }
    var isDecodingPhoto by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = VoiceAndEmergencyHelper.extractSpokenText(result.data)
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "${inputText.trim()} $spoken".take(4000)
            }
        }
    }

    LaunchedEffect(pendingCameraPhoto) {
        if (pendingCameraPhoto != null) {
            if (autoMaskPiiBeforeScan) {
                pendingPiiPhoto = pendingCameraPhoto
            } else {
                attachedUri = pendingCameraPhoto.first
                attachedBitmap = pendingCameraPhoto.second
            }
            isDecodingPhoto = false
            onConsumePendingCameraPhoto()
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecodingPhoto = true
            scope.launch {
                val decoded = loadDownscaledBitmap(context, uri)
                isDecodingPhoto = false
                if (decoded == null) {
                    onShowMessage("Could not decode the selected photo. Please try another image.")
                } else if (autoMaskPiiBeforeScan) {
                    pendingPiiPhoto = uri to decoded
                } else {
                    attachedUri = uri
                    attachedBitmap = decoded
                }
            }
        }
    }

    pendingPiiPhoto?.let { (uri, rawBitmap) ->
        PiiRedactionPreviewSheet(
            sourceBitmap = rawBitmap,
            scanPurposeTitle = "Follow-Up Chat photo attachment",
            confirmButtonLabel = "Attach Redacted Photo",
            onDismiss = { pendingPiiPhoto = null },
            onConfirmUpload = { finalBitmap ->
                attachedUri = uri
                attachedBitmap = finalBitmap
                pendingPiiPhoto = null
            }
        )
    }

    val messages = (messagesState as? CloudUiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size, chatState.streamingDoctorReply.length) {
        val totalItems = messages.size + (if (chatState.isSending) 2 else 1)
        if (totalItems > 0) {
            listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBackToReport,
                    modifier = Modifier.testTag("chat_back_to_report_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consultation.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                    Text(
                        text = "Follow-Up Chat • Profile synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                UrgencyBadge(urgencyLevel = consultation.parsedUrgency)
            }
        }

        if (messagesState is CloudUiState.Error) {
            SanctuaryErrorBanner(
                message = messagesState.message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        val chatError = chatState.errorMessage
        if (chatError != null) {
            SanctuaryErrorBanner(
                message = chatError,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Context anchor card
            item {
                DoctorBubble(
                    content = "I've reviewed your report for \"${consultation.title}\" (${consultation.parsedUrgency.label}) along with your health profile. Ask any follow-up questions about recovery steps, medication compatibility, or what to expect next.",
                    isStreaming = false
                )
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.parsedRole == MessageRole.USER) {
                    UserBubble(
                        content = msg.content,
                        hadPhoto = msg.hadPhotoAttachment
                    )
                } else {
                    val msgSpeechId = "chat_msg_${msg.id}"
                    DoctorBubble(
                        content = msg.content,
                        isStreaming = false,
                        isSpeaking = ttsController.speakingId == msgSpeechId,
                        onToggleSpeak = {
                            ttsController.toggleSpeak(
                                id = msgSpeechId,
                                rawText = msg.content,
                                onUnavailable = onShowMessage
                            )
                        },
                        onReportClick = {
                            onReportAiResponse(AiFeatureArea.FOLLOW_UP_CHAT, msg.content)
                        }
                    )
                }
            }

            if (chatState.isSending) {
                item {
                    DoctorBubble(
                        content = chatState.streamingDoctorReply.ifBlank { "Thinking carefully..." },
                        isStreaming = true
                    )
                }
            }

            // One-tap confirmation cards for AI-proposed actions
            val visibleActions = proposedActions.filter { it.status != ProposedChatActionStatus.DISMISSED }
            if (visibleActions.isNotEmpty() && !chatState.isSending) {
                items(visibleActions, key = { "action_${it.id}" }) { action ->
                    ProposedChatActionCard(
                        action = action,
                        onConfirm = { onConfirmProposedAction(action.id) },
                        onDismiss = { onDismissProposedAction(action.id) },
                        onReportAiResponse = onReportAiResponse
                    )
                }
            }

            if (citations.isNotEmpty() && !chatState.isSending) {
                item(key = "chat_trusted_citations") {
                    TrustedCitationsSection(
                        citations = citations,
                        onShowMessage = onShowMessage
                    )
                }
            }
        }

        // Attached photo preview strip
        if (attachedUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = attachedUri,
                        contentDescription = "Attached follow-up photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = if (isDecodingPhoto) "Preparing photo..." else "Photo attached for AI review",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            attachedUri = null
                            attachedBitmap = null
                            isDecodingPhoto = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_remove_photo)
                        )
                    }
                }
            }
        }

        // Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onOpenCamera,
                    enabled = !chatState.isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_camera_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = stringResource(R.string.cd_take_camera_photo)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !chatState.isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.cd_attach_photo)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        try {
                            voiceLauncher.launch(
                                VoiceAndEmergencyHelper.buildVoiceDictationIntent("Ask a follow-up question")
                            )
                        } catch (_: ActivityNotFoundException) {
                            onShowMessage(voiceUnavailableText)
                        }
                    },
                    enabled = !chatState.isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_voice_dictate_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.cd_voice_dictate)
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { if (it.length <= 4000) inputText = it },
                    placeholder = { Text("Ask a follow-up...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                FilledIconButton(
                    onClick = {
                        val outgoingText = inputText
                        val outgoingBmp = attachedBitmap
                        inputText = ""
                        attachedUri = null
                        attachedBitmap = null
                        onSendMessage(outgoingText, outgoingBmp)
                    },
                    enabled = !chatState.isSending && !isDecodingPhoto && (inputText.isNotBlank() || attachedBitmap != null),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_send_button")
                ) {
                    if (chatState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send_message)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserBubble(
    content: String,
    hadPhoto: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hadPhoto) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Photo shared",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DoctorBubble(
    content: String,
    isStreaming: Boolean,
    isSpeaking: Boolean = false,
    onToggleSpeak: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.HealthAndSafety,
                    contentDescription = "AI Doctor",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Well+ AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!isStreaming && onToggleSpeak != null) {
                        SpeakReadoutPill(
                            isSpeaking = isSpeaking,
                            onToggle = onToggleSpeak
                        )
                    }
                }
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isStreaming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Responding...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (onReportClick != null) {
                    AiSafetyFooter(
                        featureArea = AiFeatureArea.FOLLOW_UP_CHAT,
                        contentSnippet = content,
                        onReportClick = { _, _ -> onReportClick() }
                    )
                }
            }
        }
    }
}
