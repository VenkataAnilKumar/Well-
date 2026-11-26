package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.PhysicalActivityLevel
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.tabular

private val ACTIVITY_MINUTE_PRESETS = listOf(0, 15, 30, 45, 60, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhysicalActivityCheckInCard(
    selectedLevel: PhysicalActivityLevel,
    activityMinutes: Int,
    onSelectLevel: (PhysicalActivityLevel) -> Unit,
    onChangeMinutes: (Int) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val haptics = LocalHapticFeedback.current
    val accent = sanaColors.activityOrange

    SanctuaryCard(modifier = Modifier.testTag("checkin_activity_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_health_steps),
                    contentDescription = "Physical activity",
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    text = stringResource(R.string.checkin_activity_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
            ) {
                Text(
                    text = "${selectedLevel.shortLabel} • $activityMinutes min",
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "How active was your body today?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 5-level interactive selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PhysicalActivityLevel.entries.forEach { level ->
                val selected = selectedLevel == level
                val bgColor by animateColorAsState(
                    targetValue = if (selected) {
                        accent.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    animationSpec = spring(),
                    label = "activity_bg_${level.name}"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (selected) accent else sanaColors.cardBorder,
                    animationSpec = spring(),
                    label = "activity_border_${level.name}"
                )
                val iconTint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 74.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectLevel(level)
                        }
                        .testTag("checkin_activity_${level.name.lowercase()}"),
                    color = bgColor,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = activityLevelIcon(level),
                            contentDescription = level.label,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = level.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, sanaColors.subtleDivider)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = activityLevelIcon(selectedLevel),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${selectedLevel.label}: ${selectedLevel.caption}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = sanaColors.subtleDivider)
        Spacer(modifier = Modifier.height(12.dp))

        // Active Minutes stepper + presets + 30-min daily goal bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Minutes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChangeMinutes((activityMinutes - 5).coerceAtLeast(0))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("activity_minutes_minus_button")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease active minutes")
                }
                Text(
                    text = "$activityMinutes min",
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = accent
                )
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChangeMinutes((activityMinutes + 5).coerceAtMost(600))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("activity_minutes_plus_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase active minutes")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ACTIVITY_MINUTE_PRESETS.forEach { preset ->
                val isSelected = activityMinutes == preset
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChangeMinutes(preset)
                    },
                    label = {
                        Text(
                            text = if (preset == 0) "Rest (0m)" else "${preset}m",
                            style = MaterialTheme.typography.labelSmall.tabular()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.16f),
                        selectedLabelColor = accent
                    ),
                    modifier = Modifier.testTag("activity_preset_${preset}m")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (activityMinutes / 30f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = if (activityMinutes >= 30) sanaColors.healthyGreen else accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                activityMinutes >= 30 -> "Daily 30-minute active movement target reached."
                activityMinutes == 0 -> "Rest day logged — gentle stretching or a brief walk still counts."
                else -> "${30 - activityMinutes} more min to hit a 30-minute daily movement goal."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
