package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.R
import com.example.data.model.HealthProfile
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.ClarifyingQuestionsUiState
import com.example.ui.viewmodel.IntakeDraft
import kotlin.math.roundToInt

data class BodyAreaOption(
    val label: String,
    val icon: ImageVector,
    val quickPrompts: List<String>,
    val suggestedAssociated: List<String>
)

internal val INTAKE_BODY_AREAS = listOf(
    BodyAreaOption(
        label = "Head & Neuro",
        icon = Icons.Filled.Psychology,
        quickPrompts = listOf(
            "Throbbing headache behind my eyes",
            "Lightheadedness when standing up",
            "Sinus pressure and brain fog"
        ),
        suggestedAssociated = listOf("Light sensitivity", "Dizziness", "Nausea", "Neck stiffness")
    ),
    BodyAreaOption(
        label = "Throat & ENT",
        icon = Icons.Filled.Sick,
        quickPrompts = listOf(
            "Scratchy sore throat when swallowing",
            "Nasal congestion and sinus pressure",
            "Mild ear fullness or ringing"
        ),
        suggestedAssociated = listOf("Fever", "Cough", "Swollen glands", "Runny nose")
    ),
    BodyAreaOption(
        label = "Chest & Breathing",
        icon = Icons.Filled.Air,
        quickPrompts = listOf(
            "Dry cough worse at night",
            "Mild chest tightness with exertion",
            "Wheezing or shallow breathing"
        ),
        suggestedAssociated = listOf("Shortness of breath", "Cough", "Chest tightness", "Fatigue")
    ),
    BodyAreaOption(
        label = "Digestive",
        icon = Icons.Filled.Restaurant,
        quickPrompts = listOf(
            "Upper stomach burning after eating",
            "Cramping and bloating since morning",
            "Mild nausea and reduced appetite"
        ),
        suggestedAssociated = listOf("Nausea", "Loss of appetite", "Heartburn", "Fatigue")
    ),
    BodyAreaOption(
        label = "Skin & Rash",
        icon = Icons.Filled.Healing,
        quickPrompts = listOf(
            "Itchy red patches on my arms",
            "Dry irritated skin flare-up",
            "Localized swelling after insect bite"
        ),
        suggestedAssociated = listOf("Rash or swelling", "Itching", "Warmth at site", "Fever")
    ),
    BodyAreaOption(
        label = "Joints & Muscles",
        icon = Icons.Filled.AccessibilityNew,
        quickPrompts = listOf(
            "Lower back stiffness after sitting",
            "Aching shoulder with overhead reach",
            "Sore knee after walking or stairs"
        ),
        suggestedAssociated = listOf("Body aches", "Joint swelling", "Reduced mobility", "Fatigue")
    ),
    BodyAreaOption(
        label = "General & Fatigue",
        icon = Icons.Filled.Spa,
        quickPrompts = listOf(
            "Persistent afternoon exhaustion",
            "Low-grade fever and chills",
            "Trouble falling or staying asleep"
        ),
        suggestedAssociated = listOf("Fatigue", "Fever", "Chills", "Sleep trouble")
    )
)

private val INTAKE_DURATIONS = listOf(
    "Just started",
    "Today",
    "2–3 days",
    "1+ week",
    "Ongoing"
)

private val ONSET_PATTERNS = listOf(
    "Sudden",
    "Gradual",
    "After activity",
    "Woke up with it"
)

private val PROGRESSION_TRENDS = listOf(
    "Getting worse",
    "Staying steady",
    "Comes & goes",
    "Improving"
)

private val GENERAL_ASSOCIATED_SYMPTOMS = listOf(
    "Fever",
    "Fatigue",
    "Headache",
    "Nausea",
    "Dizziness",
    "Chills",
    "Cough",
    "Body aches",
    "Shortness of breath",
    "Sleep trouble",
    "Loss of appetite",
    "Rash or swelling"
)

private val QUICK_TRIGGER_CHIPS = listOf(
    "Worse with movement",
    "After meals",
    "Screen time / bright light",
    "High stress",
    "Rest helps",
    "Hydration helps"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IntakeBodyAndComplaintStep(
    draft: IntakeDraft,
    onUpdateDraft: ((IntakeDraft) -> IntakeDraft) -> Unit,
    onVoiceDictate: () -> Unit = {}
) {
    val activeArea = remember(draft.bodyArea) {
        INTAKE_BODY_AREAS.firstOrNull { it.label == draft.bodyArea } ?: INTAKE_BODY_AREAS.first()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SanctuaryCard {
            Text(
                text = "Where are you experiencing symptoms?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose the primary body area so Well+ tailors relevant clinical prompts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                INTAKE_BODY_AREAS.forEach { area ->
                    val selected = draft.bodyArea == area.label
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateDraft { it.copy(bodyArea = area.label) } },
                        leadingIcon = {
                            Icon(
                                imageVector = area.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(area.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("body_area_${area.label}")
                    )
                }
            }
        }

        SanctuaryCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Describe what you're feeling",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap a quick starter below, dictate by voice, or type in your own words.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onVoiceDictate,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("intake_voice_dictate_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.cd_voice_dictate)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                activeArea.quickPrompts.forEach { prompt ->
                    SuggestionChip(
                        onClick = {
                            onUpdateDraft { current ->
                                val next = when {
                                    current.symptomDescription.isBlank() -> prompt
                                    current.symptomDescription.contains(prompt, ignoreCase = true) -> current.symptomDescription
                                    else -> "${current.symptomDescription.trim()}. $prompt"
                                }.take(2000)
                                current.copy(symptomDescription = next)
                            }
                        },
                        label = { Text("+ $prompt", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.symptomDescription,
                onValueChange = { value ->
                    if (value.length <= 2000) onUpdateDraft { it.copy(symptomDescription = value) }
                },
                placeholder = {
                    Text("For example: dull ache behind my eyes since waking up, slightly worse near bright screens...")
                },
                minLines = 4,
                maxLines = 6,
                supportingText = {
                    Text(
                        text = "${draft.symptomDescription.length} / 2000 chars",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("symptom_description_input")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IntakeSeverityAndTimingStep(
    draft: IntakeDraft,
    onUpdateDraft: ((IntakeDraft) -> IntakeDraft) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val severityLabel = when (draft.severity) {
        in 1..3 -> "Mild"
        in 4..6 -> "Moderate"
        in 7..8 -> "Strong"
        else -> "Severe"
    }
    val severityColor = when (draft.severity) {
        in 1..3 -> sanaColors.selfCareFg
        in 4..6 -> sanaColors.doctorSoonFg
        in 7..8 -> sanaColors.urgentFg
        else -> sanaColors.emergencyFg
    }
    val severityDescription = when (draft.severity) {
        in 1..3 -> "Noticeable but manageable during normal daily activities."
        in 4..6 -> "Distracting discomfort that limits focus or routine comfort."
        in 7..8 -> "Intense symptoms interfering significantly with rest or work."
        else -> "Severe or distressing — consider urgent in-person care."
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SanctuaryCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Symptom Severity",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = severityColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${draft.severity}/10 • $severityLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = severityDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = draft.severity.toFloat(),
                onValueChange = { v -> onUpdateDraft { it.copy(severity = v.roundToInt().coerceIn(1, 10)) } },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = severityColor,
                    activeTrackColor = severityColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.testTag("severity_slider")
            )

            AnimatedVisibility(visible = draft.severity >= 9) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = sanaColors.emergencyBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, sanaColors.emergencyFg.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = sanaColors.emergencyFg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.intake_high_severity_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        SanctuaryCard {
            Text(
                text = "How long has this lasted?",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                INTAKE_DURATIONS.forEach { duration ->
                    FilterChip(
                        selected = draft.duration == duration,
                        onClick = { onUpdateDraft { it.copy(duration = duration) } },
                        label = { Text(duration, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("duration_$duration")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = sanaColors.subtleDivider)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "How did it start?",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ONSET_PATTERNS.forEach { pattern ->
                    FilterChip(
                        selected = draft.onsetPattern == pattern,
                        onClick = { onUpdateDraft { it.copy(onsetPattern = pattern) } },
                        label = { Text(pattern, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("onset_$pattern")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "How is it changing?",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PROGRESSION_TRENDS.forEach { trend ->
                    FilterChip(
                        selected = draft.progression == trend,
                        onClick = { onUpdateDraft { it.copy(progression = trend) } },
                        label = { Text(trend, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("progression_$trend")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IntakeAssociatedContextStep(
    draft: IntakeDraft,
    clarifyingQuestionsState: ClarifyingQuestionsUiState = ClarifyingQuestionsUiState.Idle,
    onGenerateClarifyingQuestions: () -> Unit = {},
    onAnswerClarifyingQuestion: (String, String) -> Unit = { _, _ -> },
    onUpdateDraft: ((IntakeDraft) -> IntakeDraft) -> Unit,
    onToggleAssociatedSymptom: (String) -> Unit
) {
    val activeArea = remember(draft.bodyArea) {
        INTAKE_BODY_AREAS.firstOrNull { it.label == draft.bodyArea }
    }
    val orderedSymptoms = remember(activeArea) {
        val areaSpecific = activeArea?.suggestedAssociated.orEmpty()
        (areaSpecific + GENERAL_ASSOCIATED_SYMPTOMS).distinct()
    }
    var customSymptom by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SanctuaryCard {
            Text(
                text = "Any accompanying symptoms?",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Optional — select any related symptoms or add your own (up to 20).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                orderedSymptoms.forEach { tag ->
                    val selected = tag in draft.associatedSymptoms
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleAssociatedSymptom(tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("assoc_symptom_$tag")
                    )
                }
            }

            // Custom added symptoms not in the standard chip list
            val customAdded = draft.associatedSymptoms.filter { it !in orderedSymptoms }
            if (customAdded.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    customAdded.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { onToggleAssociatedSymptom(tag) },
                            label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove $tag",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customSymptom,
                    onValueChange = { if (it.length <= 60) customSymptom = it },
                    placeholder = { Text("Add another symptom...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = customSymptom.trim()
                            if (trimmed.isNotEmpty()) {
                                onToggleAssociatedSymptom(trimmed)
                                customSymptom = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_associated_symptom_input")
                )
                FilledTonalIconButton(
                    onClick = {
                        val trimmed = customSymptom.trim()
                        if (trimmed.isNotEmpty()) {
                            onToggleAssociatedSymptom(trimmed)
                            customSymptom = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("add_custom_symptom_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_item)
                    )
                }
            }
        }

        SanctuaryCard {
            Text(
                text = "Triggers or What Helps (optional)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Noting what worsens or relieves symptoms helps the AI Doctor narrow practical guidance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QUICK_TRIGGER_CHIPS.forEach { hint ->
                    SuggestionChip(
                        onClick = {
                            onUpdateDraft { current ->
                                val next = when {
                                    current.triggersOrRelief.isBlank() -> hint
                                    current.triggersOrRelief.contains(hint, ignoreCase = true) -> current.triggersOrRelief
                                    else -> "${current.triggersOrRelief.trim()}, $hint"
                                }.take(300)
                                current.copy(triggersOrRelief = next)
                            }
                        },
                        label = { Text("+ $hint", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.triggersOrRelief,
                onValueChange = { if (it.length <= 300) onUpdateDraft { d -> d.copy(triggersOrRelief = it) } },
                placeholder = { Text("e.g., Worse after long screen sessions, eases in a dark quiet room...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("triggers_relief_input")
            )
        }

        // Dynamic AI Clarifying Questions Card
        SanctuaryCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.testTag("clarifying_questions_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.intake_clarify_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.intake_clarify_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (clarifyingQuestionsState) {
                is ClarifyingQuestionsUiState.Idle -> {
                    FilledTonalButton(
                        onClick = onGenerateClarifyingQuestions,
                        enabled = draft.symptomDescription.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_clarifying_questions_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.intake_clarify_cta))
                    }
                }

                is ClarifyingQuestionsUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = stringResource(R.string.intake_clarify_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ClarifyingQuestionsUiState.Error -> {
                    SanctuaryErrorBanner(
                        message = clarifyingQuestionsState.message,
                        onRetry = onGenerateClarifyingQuestions
                    )
                }

                is ClarifyingQuestionsUiState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        clarifyingQuestionsState.questions.forEachIndexed { qIndex, item ->
                            val chosenAnswer = draft.clarifyingAnswers[item.question]
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${qIndex + 1}. ${item.question}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.clinicalReason.isNotBlank()) {
                                    Text(
                                        text = item.clinicalReason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    item.quickOptions.forEach { option ->
                                        FilterChip(
                                            selected = chosenAnswer == option,
                                            onClick = { onAnswerClarifyingQuestion(item.question, option) },
                                            label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.testTag("clarify_q${qIndex}_opt_$option")
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = onGenerateClarifyingQuestions,
                            modifier = Modifier.testTag("generate_clarifying_questions_button")
                        ) {
                            Text(stringResource(R.string.intake_clarify_refresh))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IntakePhotoAndReviewStep(
    draft: IntakeDraft,
    profile: HealthProfile?,
    isPhotoMode: Boolean,
    isDecodingPhoto: Boolean,
    onUpdateDraft: ((IntakeDraft) -> IntakeDraft) -> Unit,
    onJumpToStep: (Int) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchPicker: () -> Unit,
    onClearPhoto: () -> Unit,
    onVoiceDictate: () -> Unit = {}
) {
    val sanaColors = LocalSanaColors.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SanctuaryCard {
            Text(
                text = if (isPhotoMode) "Capture or Upload Symptom / Lab Photo" else "Optional Visual Attachment",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Take a sharp photo with your camera or choose an existing image of a rash, swelling, or lab report.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (draft.photoUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    AsyncImage(
                        model = draft.photoUri,
                        contentDescription = "Selected photo for analysis",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        IconButton(
                            onClick = onClearPhoto,
                            modifier = Modifier.testTag("clear_intake_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_remove_photo)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onLaunchCamera,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("take_camera_photo_button")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.camera_retake))
                    }
                    OutlinedButton(
                        onClick = onLaunchPicker,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_photo_button")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.camera_pick_gallery))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLaunchCamera,
                        enabled = !isDecodingPhoto,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(54.dp)
                            .testTag("take_camera_photo_button")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.camera_take_photo))
                    }
                    FilledTonalButton(
                        onClick = onLaunchPicker,
                        enabled = !isDecodingPhoto,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(54.dp)
                            .testTag("pick_photo_button")
                    ) {
                        if (isDecodingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.camera_pick_gallery))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Analyzed ephemerally by AI — raw images are never stored in cloud buckets.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isPhotoMode) {
            SanctuaryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Context or Questions About This Photo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = onVoiceDictate,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("photo_context_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.cd_voice_dictate)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.symptomDescription,
                    onValueChange = { value ->
                        if (value.length <= 2000) onUpdateDraft { it.copy(symptomDescription = value) }
                    },
                    placeholder = {
                        Text("e.g., Itchy red patch on forearm for 2 days, or: Please explain my cholesterol & Vitamin D lab values.")
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_context_input")
                )
            }
        } else {
            // Prepared AI Clinical Brief Summary Card
            SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MedicalInformation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Prepared for AI Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ReviewDossierRow(
                    label = "Area & Complaint",
                    value = "${draft.bodyArea}\n${draft.symptomDescription.ifBlank { "No text description added" }}",
                    onEdit = { onJumpToStep(0) },
                    editTag = "review_edit_step_0"
                )
                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                ReviewDossierRow(
                    label = "Severity & Timeline",
                    value = "${draft.severity}/10 • ${draft.duration} • ${draft.onsetPattern} • ${draft.progression}",
                    onEdit = { onJumpToStep(1) },
                    editTag = "review_edit_step_1"
                )
                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                ReviewDossierRow(
                    label = "Associated Context",
                    value = buildString {
                        append(
                            if (draft.associatedSymptoms.isEmpty()) {
                                "No accompanying symptoms selected"
                            } else {
                                draft.associatedSymptoms.joinToString(", ")
                            }
                        )
                        if (draft.triggersOrRelief.isNotBlank()) {
                            append("\nTriggers/Relief: ${draft.triggersOrRelief.trim()}")
                        }
                        if (draft.clarifyingAnswers.isNotEmpty()) {
                            append("\nClarifying Q&A (${draft.clarifyingAnswers.size} answered)")
                        }
                    },
                    onEdit = { onJumpToStep(2) },
                    editTag = "review_edit_step_2"
                )

                if (profile != null && profile.hasClinicalContext) {
                    HorizontalDivider(
                        color = sanaColors.subtleDivider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Health Profile Attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = profile.toPromptContext(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewDossierRow(
    label: String,
    value: String,
    onEdit: () -> Unit,
    editTag: String
) {
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        TextButton(
            onClick = onEdit,
            modifier = Modifier.testTag(editTag)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.intake_edit_step))
        }
    }
}
