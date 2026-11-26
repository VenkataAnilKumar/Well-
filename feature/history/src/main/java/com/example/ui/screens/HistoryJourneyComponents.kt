package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.Consultation
import com.example.data.model.DailyCheckIn
import com.example.data.model.FollowUpStatus
import com.example.data.model.UrgencyLevel
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.UrgencyBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.LocalSanaColors
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale

internal fun historyTimeBucket(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val nowYear = now.get(Calendar.YEAR)
    val nowDay = now.get(Calendar.DAY_OF_YEAR)
    val targetYear = target.get(Calendar.YEAR)
    val targetDay = target.get(Calendar.DAY_OF_YEAR)

    return when {
        nowYear == targetYear && nowDay == targetDay -> "Today"
        nowYear == targetYear && nowDay - targetDay == 1 -> "Yesterday"
        now.timeInMillis - target.timeInMillis in 0L..(7L * 24L * 60L * 60L * 1000L) -> "This Week"
        nowYear == targetYear && now.get(Calendar.MONTH) == target.get(Calendar.MONTH) -> "This Month"
        else -> "Earlier"
    }
}

@Composable
internal fun HealthJourneyOverviewCard(
    consultations: List<Consultation>,
    checkIns: List<DailyCheckIn>
) {
    val sanaColors = LocalSanaColors.current
    val avgSeverity = remember(consultations) {
        if (consultations.isEmpty()) null else consultations.take(10).map { it.severity }.average()
    }
    val avgSleep = remember(checkIns) {
        if (checkIns.isEmpty()) null else checkIns.take(7).map { it.sleepHours }.average()
    }
    val topBodyArea = remember(consultations) {
        consultations
            .groupingBy { it.bodyArea }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    // Chronological order (oldest -> newest) for sparkline visualization
    val trendPoints = remember(consultations) {
        consultations.take(10).reversed()
    }

    SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
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
                    imageVector = Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Health Journey Pulse",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (topBodyArea != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Top focus: $topBodyArea",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JourneyStatTile(
                label = "Entries",
                value = "${consultations.size}",
                modifier = Modifier.weight(1f)
            )
            JourneyStatTile(
                label = "Avg Severity",
                value = avgSeverity?.let { String.format(Locale.US, "%.1f/10", it) } ?: "—",
                modifier = Modifier.weight(1.1f)
            )
            JourneyStatTile(
                label = "Check-Ins",
                value = "${checkIns.size}",
                modifier = Modifier.weight(1f)
            )
            JourneyStatTile(
                label = "Avg Sleep",
                value = avgSleep?.let { String.format(Locale.US, "%.1fh", it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
        }

        if (trendPoints.size >= 2) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = sanaColors.subtleDivider)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Symptom Severity Trend (last ${trendPoints.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1 mild — 10 severe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SeverityTrendChart(
                points = trendPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .testTag("history_severity_trend_chart")
            )
        }
    }
}

@Composable
private fun JourneyStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeverityTrendChart(
    points: List<Consultation>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = LocalSanaColors.current.subtleDivider
    val surfaceColor = MaterialTheme.colorScheme.surface
    val sanaColors = LocalSanaColors.current

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val padX = 12.dp.toPx()
        val padY = 10.dp.toPx()
        val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
        val chartH = (size.height - padY * 2f).coerceAtLeast(1f)

        // Subtle horizontal reference lines at severity 3, 6, 9
        listOf(0.2f, 0.5f, 0.8f).forEach { fraction ->
            val y = padY + chartH * fraction
            drawLine(
                color = gridColor,
                start = Offset(padX, y),
                end = Offset(size.width - padX, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val coords = points.mapIndexed { idx, entry ->
            val x = padX + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartW
            val norm = ((entry.severity.coerceIn(1L, 10L) - 1L) / 9f)
            val y = padY + (1f - norm) * chartH
            Offset(x, y)
        }

        // Soft vertical gradient fill under severity curve
        val fillPath = Path().apply {
            moveTo(coords.first().x, size.height - padY)
            coords.forEach { pt -> lineTo(pt.x, pt.y) }
            lineTo(coords.last().x, size.height - padY)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    primaryColor.copy(alpha = 0.02f)
                ),
                startY = padY,
                endY = size.height - padY
            )
        )

        // Main connecting severity line
        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Urgency-colored data point markers
        coords.forEachIndexed { idx, pt ->
            val dotColor = when (points[idx].parsedUrgency) {
                UrgencyLevel.SELF_CARE -> sanaColors.selfCareFg
                UrgencyLevel.DOCTOR_SOON -> sanaColors.doctorSoonFg
                UrgencyLevel.URGENT -> sanaColors.urgentFg
                UrgencyLevel.EMERGENCY -> sanaColors.emergencyFg
            }
            drawCircle(
                color = surfaceColor,
                radius = 5.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = dotColor,
                radius = 3.8.dp.toPx(),
                center = pt
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConsultationTimelineCard(
    consultation: Consultation,
    onOpenReport: () -> Unit,
    onOpenChat: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val severityTint: Color = when (consultation.severity.toInt()) {
        in 1..3 -> sanaColors.selfCareFg
        in 4..6 -> sanaColors.doctorSoonFg
        in 7..8 -> sanaColors.urgentFg
        else -> sanaColors.emergencyFg
    }

    SanctuaryCard(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenReport)
            .testTag("history_card_${consultation.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyBadge(urgencyLevel = consultation.parsedUrgency)
            Text(
                text = formatTimestamp(consultation.updatedAt ?: consultation.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = consultation.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${consultation.bodyArea} • ${consultation.duration}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Severity ${consultation.severity}/10",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = severityTint
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (consultation.severity / 10f).coerceIn(0.1f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = severityTint,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = consultation.aiSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        val followUpStatus = consultation.parsedFollowUpStatus
        if (consultation.associatedSymptoms.isNotEmpty() ||
            consultation.hadPhotoAttachment ||
            followUpStatus != FollowUpStatus.PENDING
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (followUpStatus != FollowUpStatus.PENDING) {
                    val outcomeBg = when (followUpStatus) {
                        FollowUpStatus.RESOLVED, FollowUpStatus.IMPROVING -> sanaColors.selfCareBg
                        FollowUpStatus.UNCHANGED -> sanaColors.doctorSoonBg
                        FollowUpStatus.WORSENING -> sanaColors.urgentBg
                        FollowUpStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val outcomeFg = when (followUpStatus) {
                        FollowUpStatus.RESOLVED, FollowUpStatus.IMPROVING -> sanaColors.selfCareFg
                        FollowUpStatus.UNCHANGED -> sanaColors.doctorSoonFg
                        FollowUpStatus.WORSENING -> sanaColors.urgentFg
                        FollowUpStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = outcomeBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, outcomeFg.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Outcome: ${followUpStatus.label}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = outcomeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (consultation.hadPhotoAttachment) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                consultation.associatedSymptoms.take(3).forEach { symptom ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, sanaColors.cardBorder)
                    ) {
                        Text(
                            text = symptom,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                val overflow = consultation.associatedSymptoms.size - 3
                if (overflow > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "+$overflow more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = sanaColors.subtleDivider)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onOpenReport,
                    modifier = Modifier.testTag("history_open_report_${consultation.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Report")
                }
                TextButton(
                    onClick = onOpenChat,
                    modifier = Modifier.testTag("history_resume_chat_${consultation.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Follow-Up")
                }
            }

            IconButton(
                onClick = onRequestDelete,
                modifier = Modifier.testTag("history_delete_${consultation.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WellnessCheckInHistoryCard(
    entry: DailyCheckIn,
    isToday: Boolean
) {
    val sanaColors = LocalSanaColors.current
    val mood = WellnessMoodOption.fromLevel(entry.moodLevel.toInt())
    val parsed = remember(entry.notes) { WellnessNoteCodec.decode(entry.notes) }
    val energyLabels = listOf("Drained", "Tired", "Moderate", "Good", "Vibrant")
    val energyLabel = energyLabels[(entry.energyLevel.toInt() - 1).coerceIn(0, 4)]

    SanctuaryCard(
        modifier = Modifier.testTag("history_checkin_${entry.dateKey}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = mood.icon,
                            contentDescription = mood.label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (isToday) "Today (${entry.dateKey})" else entry.dateKey,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mood: ${mood.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = parsed.generalStatus.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = energyLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = sanaColors.sleepIndigo,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = String.format(Locale.US, "%.1fh sleep", entry.sleepHours),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = activityLevelIcon(entry.parsedActivityLevel),
                    contentDescription = "Physical activity",
                    tint = sanaColors.activityOrange,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (entry.activityMinutes > 0L) {
                        "${entry.parsedActivityLevel.shortLabel} • ${entry.activityMinutes}m"
                    } else {
                        entry.parsedActivityLevel.shortLabel
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = sanaColors.medsTeal,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${entry.hydrationGlasses} cups",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (parsed.signals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                parsed.signals.forEach { signal ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, sanaColors.cardBorder)
                    ) {
                        Text(
                            text = signal,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (parsed.reflection.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = parsed.reflection,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
