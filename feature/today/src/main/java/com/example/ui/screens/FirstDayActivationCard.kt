package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.ui.theme.LocalSanaColors

/**
 * First-30-Second "Aha!" Activation Card for new users.
 * Guides first-session users through 3 quick wins (Check-In, Vitals/Meds, Health Connect)
 * and disappears automatically once all 3 are complete or dismissed.
 */
@Composable
fun FirstDayActivationCard(
    hasLoggedCheckIn: Boolean,
    hasLoggedVitalOrMed: Boolean,
    hasConnectedHealthConnect: Boolean,
    onStartCheckIn: () -> Unit,
    onLogVitalOrMed: () -> Unit,
    onConnectWearables: () -> Unit,
    onDismiss: () -> Unit
) {
    val completedCount = listOf(
        hasLoggedCheckIn,
        hasLoggedVitalOrMed,
        hasConnectedHealthConnect
    ).count { it }

    if (completedCount >= 3) return

    val sanaColors = LocalSanaColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (completedCount / 3f).coerceIn(0.06f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "activation_progress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_first_day_activation_card"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.activation_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$completedCount of 3 quick wins completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("activation_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.activation_dismiss_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivationStepRow(
                    icon = Icons.Filled.Spa,
                    accentColor = sanaColors.sleepIndigo,
                    title = stringResource(R.string.activation_step_checkin_title),
                    subtitle = stringResource(R.string.activation_step_checkin_sub),
                    completed = hasLoggedCheckIn,
                    testTag = "activation_step_checkin",
                    onClick = onStartCheckIn
                )
                ActivationStepRow(
                    icon = Icons.Filled.Favorite,
                    accentColor = sanaColors.heartPink,
                    title = stringResource(R.string.activation_step_vital_title),
                    subtitle = stringResource(R.string.activation_step_vital_sub),
                    completed = hasLoggedVitalOrMed,
                    testTag = "activation_step_vital",
                    onClick = onLogVitalOrMed
                )
                ActivationStepRow(
                    icon = Icons.Filled.Sync,
                    accentColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.activation_step_hc_title),
                    subtitle = stringResource(R.string.activation_step_hc_sub),
                    completed = hasConnectedHealthConnect,
                    testTag = "activation_step_hc",
                    onClick = onConnectWearables
                )
            }
        }
    }
}

@Composable
private fun ActivationStepRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    completed: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !completed, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (completed) {
            sanaColors.selfCareBg.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            0.5.dp,
            if (completed) sanaColors.selfCareFg.copy(alpha = 0.28f) else sanaColors.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (completed) "Completed" else "Pending",
                tint = if (completed) sanaColors.selfCareFg else accentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!completed) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Start $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
