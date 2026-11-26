package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.VoiceAndEmergencyHelper
import com.example.core.loadDownscaledBitmap
import com.example.data.model.ConsultationType
import com.example.data.model.HealthProfile
import com.example.ui.components.MedicalDisclaimerNote
import com.example.ui.components.PiiRedactionPreviewSheet
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.AnalysisState
import com.example.ui.viewmodel.ClarifyingQuestionsUiState
import com.example.ui.viewmodel.IntakeDraft
import kotlinx.coroutines.launch

private const val TOTAL_WIZARD_STEPS = 4

@Composable
fun GuidedIntakeScreen(
    draft: IntakeDraft,
    profile: HealthProfile?,
    analysisState: AnalysisState,
    autoMaskPiiBeforeScan: Boolean = true,
    pendingCameraPhoto: Pair<android.net.Uri, android.graphics.Bitmap>? = null,
    onConsumePendingCameraPhoto: () -> Unit = {},
    clarifyingQuestionsState: ClarifyingQuestionsUiState = ClarifyingQuestionsUiState.Idle,
    onGenerateClarifyingQuestions: () -> Unit = {},
    onAnswerClarifyingQuestion: (String, String) -> Unit = { _, _ -> },
    onClearClarifyingQuestions: () -> Unit = {},
    onUpdateDraft: ((IntakeDraft) -> IntakeDraft) -> Unit,
    onToggleAssociatedSymptom: (String) -> Unit,
    onOpenCamera: (CameraGuideMode) -> Unit,
    onPhotoSelected: (android.net.Uri?, android.graphics.Bitmap?) -> Unit,
    onShowMessage: (String) -> Unit = {},
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sanaColors = LocalSanaColors.current
    var isDecodingPhoto by remember { mutableStateOf(false) }
    var pendingPiiPhoto by remember { mutableStateOf<Pair<android.net.Uri, android.graphics.Bitmap>?>(null) }
    val voiceUnavailableText = stringResource(R.string.voice_unavailable)

    androidx.compose.runtime.LaunchedEffect(pendingCameraPhoto) {
        if (pendingCameraPhoto != null) {
            if (autoMaskPiiBeforeScan) {
                pendingPiiPhoto = pendingCameraPhoto
            } else {
                onPhotoSelected(pendingCameraPhoto.first, pendingCameraPhoto.second)
            }
            onConsumePendingCameraPhoto()
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = VoiceAndEmergencyHelper.extractSpokenText(result.data)
            if (!spoken.isNullOrBlank()) {
                onUpdateDraft { current ->
                    val merged = if (current.symptomDescription.isBlank()) {
                        spoken
                    } else {
                        "${current.symptomDescription.trim()} $spoken"
                    }.take(2000)
                    current.copy(symptomDescription = merged)
                }
            }
        }
    }

    fun launchVoiceDictation(prompt: String) {
        try {
            voiceLauncher.launch(VoiceAndEmergencyHelper.buildVoiceDictationIntent(prompt))
        } catch (_: ActivityNotFoundException) {
            onShowMessage(voiceUnavailableText)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isDecodingPhoto = true
            scope.launch {
                val bitmap = loadDownscaledBitmap(context, uri)
                isDecodingPhoto = false
                if (bitmap == null) {
                    onShowMessage("Could not decode the selected photo. Please try another image.")
                } else if (autoMaskPiiBeforeScan) {
                    pendingPiiPhoto = uri to bitmap
                } else {
                    onPhotoSelected(uri, bitmap)
                }
            }
        }
    }

    pendingPiiPhoto?.let { (uri, rawBitmap) ->
        PiiRedactionPreviewSheet(
            sourceBitmap = rawBitmap,
            scanPurposeTitle = if (draft.mode == ConsultationType.PHOTO_ANALYSIS) {
                "Photo & Lab Report AI Analysis"
            } else {
                "Symptom Intake Photo Attachment"
            },
            confirmButtonLabel = "Use Redacted Photo",
            onDismiss = { pendingPiiPhoto = null },
            onConfirmUpload = { finalBitmap ->
                onPhotoSelected(uri, finalBitmap)
                pendingPiiPhoto = null
            }
        )
    }

    val isAnalyzing = analysisState is AnalysisState.Analyzing
    val isPhotoMode = draft.mode == ConsultationType.PHOTO_ANALYSIS
    val currentStep = draft.step.coerceIn(0, TOTAL_WIZARD_STEPS - 1)
    val isFinalStep = isPhotoMode || currentStep == TOTAL_WIZARD_STEPS - 1

    // Step back inside the guided intake wizard before exiting the Consult tab
    BackHandler(enabled = !isPhotoMode && currentStep > 0 && !isAnalyzing) {
        onUpdateDraft { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            text = "AI DOCTOR CONSULTATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isPhotoMode) "Photo & Lab Analysis" else "Guided Symptom Intake",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isPhotoMode) {
                                "Share a photo of a visible symptom, skin concern, or lab report for plain-language AI analysis."
                            } else {
                                "Walk step by step through your symptoms so Well+ prepares a complete clinical brief for AI triage."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (draft.hasDraftContent && !isAnalyzing) {
                        TextButton(
                            onClick = {
                                onClearClarifyingQuestions()
                                onUpdateDraft {
                                    IntakeDraft(
                                        mode = it.mode,
                                        step = if (it.mode == ConsultationType.PHOTO_ANALYSIS) 3 else 0,
                                        bodyArea = if (it.mode == ConsultationType.PHOTO_ANALYSIS) {
                                            "Photo / Lab Report"
                                        } else {
                                            "Head & Neuro"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.testTag("intake_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.intake_reset_draft))
                        }
                    }
                }
            }

            // Consultation Mode Switcher
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isPhotoMode,
                        onClick = {
                            if (!isAnalyzing) {
                                onUpdateDraft {
                                    it.copy(
                                        mode = ConsultationType.SYMPTOM_CHECK,
                                        step = 0,
                                        bodyArea = if (it.bodyArea == "Photo / Lab Report") {
                                            "Head & Neuro"
                                        } else {
                                            it.bodyArea
                                        }
                                    )
                                }
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                Icons.Filled.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("mode_symptom_check")
                    ) {
                        Text("Symptom Wizard")
                    }
                    SegmentedButton(
                        selected = isPhotoMode,
                        onClick = {
                            if (!isAnalyzing) {
                                onUpdateDraft {
                                    it.copy(
                                        mode = ConsultationType.PHOTO_ANALYSIS,
                                        step = TOTAL_WIZARD_STEPS - 1,
                                        bodyArea = "Photo / Lab Report"
                                    )
                                }
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                Icons.Filled.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("mode_photo_lab")
                    ) {
                        Text("Photo & Lab")
                    }
                }
            }

            // Interactive 4-Step Wizard Stepper
            if (!isPhotoMode) {
                item {
                    IntakeWizardStepper(
                        currentStep = currentStep,
                        canAdvancePastFirst = draft.symptomDescription.isNotBlank(),
                        enabled = !isAnalyzing,
                        onStepSelected = { target ->
                            onUpdateDraft { it.copy(step = target.coerceIn(0, TOTAL_WIZARD_STEPS - 1)) }
                        }
                    )
                }
            }

            // Personal Health Profile Context Banner
            if (profile != null && profile.hasClinicalContext) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Tailored to your Health Profile (${profile.allergies.size} allergies, ${profile.medications.size} meds, ${profile.conditions.size} conditions)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (analysisState is AnalysisState.Error) {
                item {
                    SanctuaryErrorBanner(
                        message = analysisState.message,
                        onRetry = onSubmit
                    )
                }
            }

            // Animated Step Body
            item {
                AnimatedContent(
                    targetState = if (isPhotoMode) 99 else currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter = fadeIn(spring()) + slideInHorizontally(spring()) { w ->
                            if (forward) w / 6 else -w / 6
                        }
                        val exit = fadeOut(spring()) + slideOutHorizontally(spring()) { w ->
                            if (forward) -w / 6 else w / 6
                        }
                        enter togetherWith exit
                    },
                    label = "intake_wizard_step_content"
                ) { targetStep ->
                    when (targetStep) {
                        0 -> IntakeBodyAndComplaintStep(
                            draft = draft,
                            onUpdateDraft = onUpdateDraft,
                            onVoiceDictate = { launchVoiceDictation("Describe your symptoms") }
                        )

                        1 -> IntakeSeverityAndTimingStep(
                            draft = draft,
                            onUpdateDraft = onUpdateDraft
                        )

                        2 -> IntakeAssociatedContextStep(
                            draft = draft,
                            clarifyingQuestionsState = clarifyingQuestionsState,
                            onGenerateClarifyingQuestions = onGenerateClarifyingQuestions,
                            onAnswerClarifyingQuestion = onAnswerClarifyingQuestion,
                            onUpdateDraft = onUpdateDraft,
                            onToggleAssociatedSymptom = onToggleAssociatedSymptom
                        )

                        else -> IntakePhotoAndReviewStep(
                            draft = draft,
                            profile = profile,
                            isPhotoMode = isPhotoMode,
                            isDecodingPhoto = isDecodingPhoto,
                            onUpdateDraft = onUpdateDraft,
                            onJumpToStep = { stepIdx ->
                                onUpdateDraft { it.copy(step = stepIdx.coerceIn(0, TOTAL_WIZARD_STEPS - 1)) }
                            },
                            onLaunchCamera = {
                                onOpenCamera(
                                    if (isPhotoMode) CameraGuideMode.LAB_DOCUMENT else CameraGuideMode.SYMPTOM
                                )
                            },
                            onLaunchPicker = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearPhoto = { onPhotoSelected(null, null) },
                            onVoiceDictate = { launchVoiceDictation("Add context or questions for this photo") }
                        )
                    }
                }
            }

            item {
                MedicalDisclaimerNote()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Docked Step Navigation / AI Analysis Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = sanaColors.subtleDivider)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (isAnalyzing) {
                        SanctuaryCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Analyzing your prepared health intake…",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Cross-checking symptoms, red flags, and your health profile.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (isFinalStep) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isPhotoMode) {
                                OutlinedButton(
                                    onClick = { onUpdateDraft { it.copy(step = TOTAL_WIZARD_STEPS - 2) } },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("intake_back_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back")
                                }
                            }
                            Button(
                                onClick = onSubmit,
                                enabled = !isDecodingPhoto && (draft.symptomDescription.isNotBlank() || draft.photoBitmap != null),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("analyze_symptoms_button")
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPhotoMode) "Analyze Photo with AI" else "Analyze with AI Doctor",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = {
                                        onUpdateDraft { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("intake_back_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back")
                                }
                            }
                            Button(
                                onClick = {
                                    onUpdateDraft {
                                        it.copy(step = (it.step + 1).coerceAtMost(TOTAL_WIZARD_STEPS - 1))
                                    }
                                },
                                enabled = currentStep != 0 || draft.symptomDescription.isNotBlank(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("intake_next_button")
                            ) {
                                Text(
                                    text = if (currentStep == 2) "Review & Photo" else "Continue",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntakeWizardStepper(
    currentStep: Int,
    canAdvancePastFirst: Boolean,
    enabled: Boolean,
    onStepSelected: (Int) -> Unit
) {
    val stepLabels = listOf(
        stringResource(R.string.intake_step_area),
        stringResource(R.string.intake_step_severity),
        stringResource(R.string.intake_step_context),
        stringResource(R.string.intake_step_review)
    )
    val sanaColors = LocalSanaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, sanaColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepLabels.forEachIndexed { idx, label ->
                val isCompleted = idx < currentStep
                val isActive = idx == currentStep
                val isClickable = enabled && (idx == 0 || canAdvancePastFirst)

                val circleBg by animateColorAsState(
                    targetValue = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    animationSpec = spring(),
                    label = "step_bg_$idx"
                )
                val circleFg = when {
                    isActive -> MaterialTheme.colorScheme.onPrimary
                    isCompleted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isClickable) { onStepSelected(idx) }
                        .padding(vertical = 4.dp)
                        .testTag("wizard_step_$idx"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = circleBg,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Step ${idx + 1} completed",
                                    tint = circleFg,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = circleFg
                                )
                            }
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
