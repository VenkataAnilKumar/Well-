package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.FreeSmartSuggestionsEngine
import com.example.core.LegalPolicyConfig
import com.example.core.RegionalEmergencyDirectory
import com.example.core.VoiceAndEmergencyHelper
import com.example.data.model.AiFeatureArea
import com.example.data.model.CareCategory
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.MentalSeverityBand
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.EmptySanctuaryState
import com.example.ui.components.HealthEmptyIllustrationKind
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.GlassStatusPill
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.MentalReflectionUiState

@Composable
fun TodayMentalWellbeingCard(
    assessments: List<MentalAssessment>,
    onOpenMentalWellbeing: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val latestPhq9 = remember(assessments) {
        assessments.firstOrNull { it.parsedType == MentalAssessmentType.PHQ9 }
    }
    val latestGad7 = remember(assessments) {
        assessments.firstOrNull { it.parsedType == MentalAssessmentType.GAD7 }
    }

    SanctuaryCard(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenMentalWellbeing)
            .testTag("home_mental_wellbeing_card")
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
                    color = sanaColors.sleepIndigo.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = sanaColors.sleepIndigo,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.mental_card_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (assessments.isEmpty()) {
                            stringResource(R.string.mental_card_subtitle)
                        } else {
                            "${assessments.size} screening(s) logged • AI coping guidance"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open mental wellbeing screenings",
                tint = sanaColors.sleepIndigo
            )
        }

        if (latestPhq9 != null || latestGad7 != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MentalMiniBadge(
                    label = "PHQ-9",
                    assessment = latestPhq9,
                    modifier = Modifier.weight(1f)
                )
                MentalMiniBadge(
                    label = "GAD-7",
                    assessment = latestGad7,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onOpenMentalWellbeing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_open_mental_button")
            ) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take PHQ-9 or GAD-7 Check-In")
            }
        }
    }
}

@Composable
private fun MentalMiniBadge(
    label: String,
    assessment: MentalAssessment?,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = sanaColors.sleepIndigo
            )
            if (assessment != null) {
                Text(
                    text = "${assessment.totalScore}/${assessment.maxScore} • ${assessment.parsedBand.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "Not taken yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MentalWellbeingScreen(
    profile: HealthProfile?,
    assessmentsState: CloudUiState<List<MentalAssessment>>,
    reflectionState: MentalReflectionUiState,
    recentCheckIns: List<DailyCheckIn> = emptyList(),
    cloudAiEnabled: Boolean = true,
    aiQuotaRemaining: Int = 25,
    emergencyRegionOverride: String = "",
    onBack: () -> Unit,
    onSubmitAssessment: (MentalAssessmentType, List<Int>, onSaved: (String) -> Unit) -> Unit,
    onRegenerateReflection: (MentalAssessment) -> Unit,
    onAddCopingStep: (String, CareCategory) -> Unit,
    onDeleteAssessment: (String) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit
) {
    var activeQuizType by remember { mutableStateOf<MentalAssessmentType?>(null) }
    var focusedAssessmentId by remember { mutableStateOf<String?>(null) }
    var addedSteps by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler {
        if (activeQuizType != null) {
            activeQuizType = null
        } else {
            onBack()
        }
    }

    val quizType = activeQuizType
    if (quizType != null) {
        MentalQuestionnaireFlow(
            type = quizType,
            onCancel = { activeQuizType = null },
            onComplete = { answers ->
                onSubmitAssessment(quizType, answers) { savedId ->
                    focusedAssessmentId = savedId
                    activeQuizType = null
                }
            }
        )
        return
    }

    val sanaColors = LocalSanaColors.current
    val assessments = (assessmentsState as? CloudUiState.Success)?.data.orEmpty()
    val focusedAssessment = remember(assessments, focusedAssessmentId) {
        focusedAssessmentId?.let { id -> assessments.firstOrNull { it.id == id } }
            ?: assessments.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mental_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Summary")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.mental_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = sanaColors.sleepIndigo
                )
                Text(
                    text = stringResource(R.string.mental_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.mental_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Launch cards for PHQ-9 & GAD-7
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MentalAssessmentType.entries.forEach { type ->
                    val latest = assessments.firstOrNull { it.parsedType == type }
                    SanctuaryCard(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { activeQuizType = type }
                            .testTag("start_${type.name.lowercase()}_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = sanaColors.sleepIndigo.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (type == MentalAssessmentType.PHQ9) {
                                            Icons.Filled.Psychology
                                        } else {
                                            Icons.Filled.SelfImprovement
                                        },
                                        contentDescription = null,
                                        tint = sanaColors.sleepIndigo,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = type.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (latest != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Last: ${latest.totalScore}/${latest.maxScore} (${latest.parsedBand.label})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = sanaColors.sleepIndigo
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { activeQuizType = type },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("start_${type.name.lowercase()}_button")
                            ) {
                                Text("Start")
                            }
                        }
                    }
                }
            }
        }

        // Focused assessment result + Crisis banner + Instant Free Guide + Optional AI Reflection
        if (focusedAssessment != null) {
            item(key = "focused_${focusedAssessment.id}") {
                FocusedAssessmentDetailCard(
                    assessment = focusedAssessment,
                    profile = profile,
                    recentCheckIns = recentCheckIns,
                    cloudAiEnabled = cloudAiEnabled,
                    aiQuotaRemaining = aiQuotaRemaining,
                    reflectionState = reflectionState,
                    emergencyRegionOverride = emergencyRegionOverride,
                    addedSteps = addedSteps,
                    onRegenerateReflection = { onRegenerateReflection(focusedAssessment) },
                    onAddCopingStep = { step ->
                        onAddCopingStep(step, CareCategory.RECOVERY)
                        addedSteps = addedSteps + step
                    },
                    onReportAiResponse = onReportAiResponse,
                    onShowMessage = onShowMessage
                )
            }
        }

        // Saved History Timeline
        item {
            Text(
                text = "SCREENING HISTORY (${assessments.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (assessmentsState is CloudUiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = sanaColors.sleepIndigo)
                }
            }
        } else if (assessmentsState is CloudUiState.Error) {
            item {
                SanctuaryErrorBanner(message = assessmentsState.message)
            }
        } else if (assessments.isEmpty()) {
            item {
                EmptySanctuaryState(
                    illustrationKind = HealthEmptyIllustrationKind.MENTAL_WELLBEING,
                    title = "No wellbeing screenings yet",
                    subtitle = "Take a private 2-minute PHQ-9 or GAD-7 check-in above to track emotional trends and receive gentle AI coping steps."
                )
            }
        } else {
            items(assessments, key = { it.id }) { item ->
                val isSelected = focusedAssessment?.id == item.id
                val (bandBg, bandFg) = mentalBandColors(item.parsedBand)
                SanctuaryCard(
                    borderColor = if (isSelected) sanaColors.sleepIndigo else sanaColors.cardBorder,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { focusedAssessmentId = item.id }
                        .testTag("mental_history_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${item.parsedType.shortLabel}: ${item.totalScore}/${item.maxScore}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = bandBg,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = item.parsedBand.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = bandFg,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatTimestamp(item.recordedAt ?: item.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteAssessment(item.id) },
                            modifier = Modifier.testTag("mental_delete_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_item),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.mental_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FocusedAssessmentDetailCard(
    assessment: MentalAssessment,
    profile: HealthProfile?,
    recentCheckIns: List<DailyCheckIn>,
    cloudAiEnabled: Boolean,
    aiQuotaRemaining: Int,
    reflectionState: MentalReflectionUiState,
    emergencyRegionOverride: String,
    addedSteps: Set<String>,
    onRegenerateReflection: () -> Unit,
    onAddCopingStep: (String) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val region = remember(context, emergencyRegionOverride, profile?.preferredEmergencyCountry) {
        val code = emergencyRegionOverride.ifBlank { profile?.preferredEmergencyCountry.orEmpty() }
        RegionalEmergencyDirectory.resolve(context, code)
    }
    val (bandBg, bandFg) = mentalBandColors(assessment.parsedBand)
    val showCrisisBanner = assessment.crisisFlagged || assessment.parsedBand == MentalSeverityBand.SEVERE

    val isLoadingThis = reflectionState is MentalReflectionUiState.Loading &&
        reflectionState.assessmentId == assessment.id
    val errorForThis = (reflectionState as? MentalReflectionUiState.Error)
        ?.takeIf { it.assessmentId == assessment.id }
    val liveReflection = (reflectionState as? MentalReflectionUiState.Success)
        ?.takeIf { it.assessmentId == assessment.id }?.reflection

    val hasAiReflection = liveReflection != null || assessment.aiReflection.isNotBlank()
    val instantGuidance = remember(assessment, recentCheckIns, region) {
        FreeSmartSuggestionsEngine.computeMentalWellbeingGuidance(
            assessment = assessment,
            recentCheckIns = recentCheckIns,
            region = region
        )
    }
    val displaySummary = when {
        liveReflection != null -> liveReflection.empatheticSummary
        assessment.aiReflection.isNotBlank() -> assessment.aiReflection
        else -> instantGuidance.empatheticSummary
    }
    val displaySteps = when {
        liveReflection != null -> liveReflection.gentleCopingSteps
        assessment.aiCopingSteps.isNotEmpty() -> assessment.aiCopingSteps
        else -> instantGuidance.gentleCopingSteps
    }
    val displayTalkingPoints = when {
        liveReflection != null -> liveReflection.clinicianTalkingPoints
        else -> instantGuidance.clinicianTalkingPoints
    }

    SanctuaryCard(
        modifier = Modifier.testTag("mental_focused_result_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assessment.parsedType.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTimestamp(assessment.recordedAt ?: assessment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(color = bandBg, shape = RoundedCornerShape(50)) {
                Text(
                    text = assessment.parsedBand.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = bandFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${assessment.totalScore}",
                style = MaterialTheme.typography.displaySmall,
                color = sanaColors.sleepIndigo
            )
            Text(
                text = "/ ${assessment.maxScore}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = {
                (assessment.totalScore.toFloat() / assessment.maxScore.coerceAtLeast(1L).toFloat())
                    .coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = bandFg,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = assessment.parsedBand.guidance,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showCrisisBanner) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = sanaColors.emergencyBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, sanaColors.emergencyFg.copy(alpha = 0.35f)),
                modifier = Modifier.testTag("mental_crisis_support_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = sanaColors.emergencyFg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.mental_crisis_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = sanaColors.emergencyFg
                        )
                    }
                    Text(
                        text = if (region.crisisNumber.isNotBlank()) {
                            "Your responses suggest you may be going through a difficult time. Free, confidential support is available in ${region.countryName} via ${region.crisisLabel}, or call ${region.emergencyLabel} if you feel unsafe."
                        } else {
                            "Your responses suggest you may be going through a difficult time. Free, confidential support is available worldwide via findahelpline.com, or call ${region.emergencyLabel} if you feel unsafe."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (region.crisisNumber.isNotBlank()) {
                                    if (!VoiceAndEmergencyHelper.openDialer(context, region.crisisNumber)) {
                                        onShowMessage("Dialer unavailable. Contact ${region.crisisLabel}.")
                                    }
                                } else {
                                    if (!LegalPolicyConfig.openValidatedHttpsUrl(context, region.internationalCrisisDirectoryUrl)) {
                                        onShowMessage("Visit findahelpline.com for free 24/7 crisis support.")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = sanaColors.emergencyFg,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("mental_call_988_button")
                        ) {
                            Icon(
                                imageVector = if (region.crisisNumber.isNotBlank()) Icons.Filled.Call else Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (region.crisisNumber.isNotBlank()) {
                                    "Call ${region.crisisLabel}"
                                } else {
                                    "Find Local Helpline"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                        if (profile?.hasEmergencyContact == true) {
                            OutlinedButton(
                                onClick = {
                                    if (!VoiceAndEmergencyHelper.openDialer(context, profile.emergencyContactPhone)) {
                                        onShowMessage("Could not open phone dialer.")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("mental_call_ice_button")
                            ) {
                                Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call ICE", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!LegalPolicyConfig.openValidatedHttpsUrl(context, region.internationalCrisisDirectoryUrl)) {
                                onShowMessage("Visit findahelpline.com for free international crisis helplines.")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mental_intl_helpline_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("International Helplines (findahelpline.com)", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = sanaColors.subtleDivider)
        Spacer(modifier = Modifier.height(12.dp))

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
                    imageVector = if (hasAiReflection) Icons.Filled.AutoAwesome else Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (hasAiReflection) sanaColors.sleepIndigo else sanaColors.medsTeal,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (hasAiReflection) {
                        "Personalized AI Reflection"
                    } else {
                        "Coping & Grounding Guide"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!hasAiReflection) {
                GlassStatusPill(
                    label = "Free · Instant",
                    accentColor = sanaColors.healthyGreen
                )
            } else if (cloudAiEnabled && !isLoadingThis) {
                IconButton(
                    onClick = onRegenerateReflection,
                    modifier = Modifier.testTag("mental_refresh_ai_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh AI reflection",
                        tint = sanaColors.sleepIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = displaySummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (displaySteps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Gentle Coping Steps",
                style = MaterialTheme.typography.labelLarge,
                color = sanaColors.sleepIndigo
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displaySteps.forEach { step ->
                    val isAdded = step in addedSteps
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = { if (!isAdded) onAddCopingStep(step) },
                                enabled = !isAdded,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdded) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAdded) "Added" else "Checklist",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
        if (displayTalkingPoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Clinician Talking Points",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            displayTalkingPoints.forEach { pt ->
                Text(
                    text = "• $pt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasAiReflection) {
            Spacer(modifier = Modifier.height(10.dp))
            AiSafetyFooter(
                featureArea = AiFeatureArea.MENTAL_REFLECTION,
                contentSnippet = "${assessment.parsedType.shortLabel}: $displaySummary",
                onReportClick = onReportAiResponse
            )
        }

        if (cloudAiEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            when {
                isLoadingThis -> {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = sanaColors.sleepIndigo,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.mental_ai_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorForThis != null -> {
                    SanctuaryErrorBanner(
                        message = errorForThis.message,
                        onRetry = onRegenerateReflection
                    )
                }

                !hasAiReflection -> {
                    OutlinedButton(
                        onClick = onRegenerateReflection,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sanaColors.sleepIndigo.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("mental_generate_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = sanaColors.sleepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Optional: Deepen with Gemini AI ($aiQuotaRemaining free left)",
                            style = MaterialTheme.typography.labelLarge,
                            color = sanaColors.sleepIndigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MentalQuestionnaireFlow(
    type: MentalAssessmentType,
    onCancel: () -> Unit,
    onComplete: (List<Int>) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val total = type.questions.size
    var currentIndex by remember(type) { mutableIntStateOf(0) }
    val answers = remember(type) { mutableStateListOf<Int?>().apply { repeat(total) { add(null) } } }

    BackHandler(enabled = currentIndex > 0) {
        currentIndex -= 1
    }

    val selectedAnswer = answers[currentIndex]
    val progress = (currentIndex + 1).toFloat() / total.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mental_quiz_cancel_button")
                ) {
                    Text("Cancel")
                }
                Text(
                    text = "${type.shortLabel}  •  ${currentIndex + 1} of $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = sanaColors.sleepIndigo
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = sanaColors.sleepIndigo,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Over the last 2 weeks, how often have you been bothered by:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = type.questions[currentIndex],
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MentalAssessmentType.RESPONSE_OPTIONS.forEach { (scoreValue, label) ->
                    val isSelected = selectedAnswer == scoreValue
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { answers[currentIndex] = scoreValue }
                            .testTag("mental_option_$scoreValue"),
                        color = if (isSelected) {
                            sanaColors.sleepIndigo.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) sanaColors.sleepIndigo else sanaColors.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) sanaColors.sleepIndigo else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = if (isSelected) sanaColors.sleepIndigo else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$scoreValue",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentIndex > 0) {
                OutlinedButton(
                    onClick = { currentIndex -= 1 },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("mental_quiz_prev_button")
                ) {
                    Text("Previous")
                }
            }
            val isLast = currentIndex == total - 1
            Button(
                onClick = {
                    if (isLast) {
                        onComplete(answers.map { it ?: 0 })
                    } else {
                        currentIndex += 1
                    }
                },
                enabled = selectedAnswer != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = sanaColors.sleepIndigo,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .height(50.dp)
                    .testTag(if (isLast) "mental_quiz_submit_button" else "mental_quiz_next_button")
            ) {
                Text(if (isLast) "Complete & Reflect" else "Next")
            }
        }
    }
}

@Composable
private fun mentalBandColors(band: MentalSeverityBand): Pair<Color, Color> {
    val sanaColors = LocalSanaColors.current
    return when (band) {
        MentalSeverityBand.MINIMAL -> sanaColors.selfCareBg to sanaColors.selfCareFg
        MentalSeverityBand.MILD -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        MentalSeverityBand.MODERATE -> sanaColors.doctorSoonBg to sanaColors.doctorSoonFg
        MentalSeverityBand.MODERATELY_SEVERE -> sanaColors.urgentBg to sanaColors.urgentFg
        MentalSeverityBand.SEVERE -> sanaColors.emergencyBg to sanaColors.emergencyFg
    }
}
