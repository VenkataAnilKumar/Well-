package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.SmartSuggestionActionTarget
import com.example.core.SmartSuggestionCategory
import com.example.core.SmartWellnessSuggestion
import com.example.data.model.CareCategory
import com.example.ui.theme.GlassHealthCard
import com.example.ui.theme.GlassRenderMode
import com.example.ui.theme.GlassStatusPill
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme

@Composable
fun FreeSmartSuggestionsCard(
    suggestions: List<SmartWellnessSuggestion>,
    onAddToChecklist: (String, CareCategory) -> Unit,
    onActionTarget: (SmartSuggestionActionTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    val sanaColors = LocalSanaColors.current
    val haptic = LocalHapticFeedback.current
    var addedIds by remember { mutableStateOf(emptySet<String>()) }

    GlassHealthCard(
        modifier = modifier.testTag("today_free_smart_suggestions_card"),
        renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
        accentTint = sanaColors.medsTeal
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
                Surface(
                    color = sanaColors.medsTeal.copy(alpha = if (SanaTheme.isDark) 0.20f else 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = sanaColors.medsTeal,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Smart Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Based on your logged data • works offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            GlassStatusPill(
                label = "Free",
                accentColor = sanaColors.healthyGreen
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            suggestions.forEachIndexed { index, item ->
                val accent = categoryAccent(item.category)
                val icon = categoryIcon(item.category)
                val isAdded = item.id in addedIds

                if (index > 0) {
                    HorizontalDivider(color = sanaColors.subtleDivider)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_suggestion_item_${item.id}"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = accent.copy(alpha = if (SanaTheme.isDark) 0.18f else 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = item.category.label,
                                tint = accent,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(18.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (item.isHighPriority) {
                                    GlassStatusPill(
                                        label = "Priority",
                                        accentColor = sanaColors.heartPink
                                    )
                                }
                            }
                            Text(
                                text = item.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (!isAdded) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    addedIds = addedIds + item.id
                                    onAddToChecklist(item.checklistTask, item.category.careCategory)
                                }
                            },
                            enabled = !isAdded,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("smart_suggestion_add_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isAdded) "Added" else "Add to Plan",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (item.target != SmartSuggestionActionTarget.NONE) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onActionTarget(item.target)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("smart_suggestion_action_${item.id}")
                            ) {
                                Text(
                                    text = item.target.buttonLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
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
private fun categoryAccent(category: SmartSuggestionCategory): Color {
    val sanaColors = LocalSanaColors.current
    return when (category) {
        SmartSuggestionCategory.VITALS -> sanaColors.heartPink
        SmartSuggestionCategory.MEDICATIONS -> sanaColors.medsTeal
        SmartSuggestionCategory.SLEEP -> sanaColors.sleepIndigo
        SmartSuggestionCategory.HYDRATION -> sanaColors.medsTeal
        SmartSuggestionCategory.MOVEMENT -> sanaColors.healthyGreen
        SmartSuggestionCategory.MENTAL_WELLBEING -> sanaColors.sleepIndigo
        SmartSuggestionCategory.VISIT_PREP -> MaterialTheme.colorScheme.primary
    }
}

private fun categoryIcon(category: SmartSuggestionCategory): ImageVector = when (category) {
    SmartSuggestionCategory.VITALS -> Icons.Rounded.MonitorHeart
    SmartSuggestionCategory.MEDICATIONS -> Icons.Rounded.Medication
    SmartSuggestionCategory.SLEEP -> Icons.Rounded.Bedtime
    SmartSuggestionCategory.HYDRATION -> Icons.Rounded.WaterDrop
    SmartSuggestionCategory.MOVEMENT -> Icons.AutoMirrored.Rounded.DirectionsWalk
    SmartSuggestionCategory.MENTAL_WELLBEING -> Icons.Rounded.SelfImprovement
    SmartSuggestionCategory.VISIT_PREP -> Icons.Rounded.Description
}
