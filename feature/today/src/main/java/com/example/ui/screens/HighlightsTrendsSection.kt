package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.AiFeatureArea
import com.example.data.model.CareCategory
import com.example.data.model.HealthHighlightItem
import com.example.data.model.HighlightCategory
import com.example.data.model.SummaryMicroInsight
import com.example.data.model.TrendDirection
import com.example.data.model.TrendSentiment
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.SummaryInsightUiState

/**
 * Apple Health-style automatic "Highlights & Trends" section on Summary,
 * displaying deterministic 7-day vs prior 7-day comparisons and an optional
 * on-demand Gemini AI Micro-Insight banner.
 */
@Composable
fun SummaryHighlightsTrendsSection(
    highlights: List<HealthHighlightItem>,
    insightState: SummaryInsightUiState,
    cloudAiEnabled: Boolean = true,
    aiQuotaRemaining: Int = 25,
    onGenerateMicroInsight: (List<HealthHighlightItem>) -> Unit,
    onDismissMicroInsight: () -> Unit,
    onAddActionToChecklist: (String, CareCategory) -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenVitals: () -> Unit,
    onOpenMentalWellbeing: () -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    val sanaColors = LocalSanaColors.current
    var showAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_highlights_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header: "Highlights" + on-demand AI Micro-Insight pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.highlights_section_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (highlights.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${highlights.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (highlights.isNotEmpty() && cloudAiEnabled) {
                val isLoading = insightState is SummaryInsightUiState.Loading
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isLoading) {
                            onGenerateMicroInsight(highlights)
                        }
                        .testTag("highlights_ai_insight_button"),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (insightState is SummaryInsightUiState.Success) {
                                stringResource(R.string.highlights_ai_refresh_cta)
                            } else {
                                "${stringResource(R.string.highlights_ai_cta)} • $aiQuotaRemaining free"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // On-demand Gemini AI Micro-Insight Banner
        AnimatedVisibility(
            visible = insightState !is SummaryInsightUiState.Idle,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            when (insightState) {
                is SummaryInsightUiState.Loading -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("highlights_ai_loading_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.highlights_ai_loading_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.highlights_ai_loading_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is SummaryInsightUiState.Success -> {
                    SummaryMicroInsightCard(
                        insight = insightState.insight,
                        onRefresh = { onGenerateMicroInsight(highlights) },
                        onDismiss = onDismissMicroInsight,
                        onAddFocusAction = { action ->
                            onAddActionToChecklist(action, CareCategory.AI_SUGGESTION)
                        },
                        onReportAiResponse = onReportAiResponse
                    )
                }

                is SummaryInsightUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SanctuaryErrorBanner(message = insightState.message)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismissMicroInsight) {
                                Text("Dismiss")
                            }
                            TextButton(onClick = { onGenerateMicroInsight(highlights) }) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                SummaryInsightUiState.Idle -> Unit
            }
        }

        // Empty State or Stacked Highlight Cards
        if (highlights.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("highlights_empty_card"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(0.5.dp, sanaColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.highlights_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.highlights_empty_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenCheckIn,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("highlights_empty_checkin_button")
                        ) {
                            Text("Log Check-In")
                        }
                        OutlinedButton(
                            onClick = onOpenVitals,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("highlights_empty_vital_button")
                        ) {
                            Text("Log Vital")
                        }
                    }
                }
            }
        } else {
            val visibleHighlights = if (showAll || highlights.size <= 2) {
                highlights
            } else {
                highlights.take(2)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleHighlights.forEach { item ->
                    HighlightTrendCard(
                        item = item,
                        onClick = {
                            when (item.category) {
                                HighlightCategory.SLEEP,
                                HighlightCategory.MOOD -> onOpenCheckIn()
                                HighlightCategory.MEDICATIONS -> onOpenMedications()
                                HighlightCategory.VITALS -> onOpenVitals()
                                HighlightCategory.MENTAL_WELLBEING -> onOpenMentalWellbeing()
                            }
                        }
                    )
                }

                if (highlights.size > 2) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showAll = !showAll }
                            .testTag("highlights_toggle_all_button"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showAll) {
                                    "Show Fewer Highlights"
                                } else {
                                    "Show All ${highlights.size} Highlights & Trends"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMicroInsightCard(
    insight: SummaryMicroInsight,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onAddFocusAction: (String) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var actionAdded by remember(insight.generatedAtMillis, insight.focusAction) {
        mutableStateOf(false)
    }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
            sanaColors.sleepIndigo.copy(alpha = 0.08f)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("highlights_ai_success_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.highlights_ai_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("highlights_ai_refresh_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh AI micro-insight",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("highlights_ai_dismiss_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss AI micro-insight",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = insight.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = insight.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
            )

            if (insight.watchNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = sanaColors.doctorSoonBg,
                    border = BorderStroke(0.5.dp, sanaColors.doctorSoonFg.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = sanaColors.doctorSoonFg,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = insight.watchNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = sanaColors.doctorSoonFg
                        )
                    }
                }
            }

            if (insight.focusAction.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = insight.focusAction,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !actionAdded) {
                                    actionAdded = true
                                    onAddFocusAction(insight.focusAction)
                                }
                                .testTag("highlights_ai_add_checklist_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (actionAdded) {
                                sanaColors.selfCareBg
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (actionAdded) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = if (actionAdded) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (actionAdded) "Added" else "Checklist",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (actionAdded) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.highlights_ai_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AiSafetyFooter(
                featureArea = AiFeatureArea.HIGHLIGHTS_INSIGHT,
                contentSnippet = "${insight.headline}: ${insight.summary}",
                generatedAtMillis = insight.generatedAtMillis,
                onReportClick = onReportAiResponse
            )
        }
    }
}

@Composable
private fun HighlightTrendCard(
    item: HealthHighlightItem,
    onClick: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val categoryColor = when (item.category) {
        HighlightCategory.SLEEP -> sanaColors.sleepIndigo
        HighlightCategory.MOOD -> MaterialTheme.colorScheme.primary
        HighlightCategory.MEDICATIONS -> sanaColors.medsTeal
        HighlightCategory.VITALS -> sanaColors.heartPink
        HighlightCategory.MENTAL_WELLBEING -> sanaColors.sleepIndigo
    }
    val categoryIcon: ImageVector = when (item.category) {
        HighlightCategory.SLEEP -> Icons.Filled.Bedtime
        HighlightCategory.MOOD -> Icons.Filled.Spa
        HighlightCategory.MEDICATIONS -> Icons.Filled.Medication
        HighlightCategory.VITALS -> Icons.Filled.Favorite
        HighlightCategory.MENTAL_WELLBEING -> Icons.Filled.Psychology
    }
    val directionIcon: ImageVector = when (item.direction) {
        TrendDirection.UP -> Icons.AutoMirrored.Filled.TrendingUp
        TrendDirection.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
        TrendDirection.STEADY,
        TrendDirection.BASELINE -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    val (badgeBg, badgeFg) = when (item.sentiment) {
        TrendSentiment.POSITIVE -> sanaColors.selfCareBg to sanaColors.selfCareFg
        TrendSentiment.ATTENTION -> sanaColors.urgentBg to sanaColors.urgentFg
        TrendSentiment.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val animatedCurrentFrac by animateFloatAsState(
        targetValue = item.currentBarFraction.coerceIn(0.14f, 1.0f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 360f),
        label = "current_bar_${item.id}"
    )
    val animatedPrevFrac by animateFloatAsState(
        targetValue = (item.previousBarFraction ?: 0f).coerceIn(0f, 1.0f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 360f),
        label = "prev_bar_${item.id}"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(item.id),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Category Header + Delta Pill + Chevron
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
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = badgeBg,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = directionIcon,
                                contentDescription = item.direction.label,
                                tint = badgeFg,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.deltaBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeFg
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Apple Health Narrative Statement
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.narrative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = sanaColors.subtleDivider)

            // Apple Health Two-Period Horizontal Bar Comparison
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComparisonBarRow(
                    periodLabel = item.currentPeriodLabel,
                    valueLabel = item.currentValueLabel,
                    fraction = animatedCurrentFrac,
                    barColor = categoryColor,
                    isPrimary = true
                )

                val prevPeriod = item.previousPeriodLabel
                val prevValue = item.previousValueLabel
                if (prevValue != null && prevPeriod != null) {
                    ComparisonBarRow(
                        periodLabel = prevPeriod,
                        valueLabel = prevValue,
                        fraction = animatedPrevFrac.coerceAtLeast(0.14f),
                        barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                        isPrimary = false
                    )
                }
            }

            Text(
                text = item.sampleCountText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComparisonBarRow(
    periodLabel: String,
    valueLabel: String,
    fraction: Float,
    barColor: Color,
    isPrimary: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.10f, 1.0f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor)
            )
        }
    }
}
