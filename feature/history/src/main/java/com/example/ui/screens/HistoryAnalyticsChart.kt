package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.Consultation
import com.example.data.model.DailyCheckIn
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val WINDOW_DAYS = 30

private enum class AnalyticsChartView {
    TIMELINE,
    BREAKDOWN
}

private data class DailyAnalyticsPoint(
    val dateKey: String,
    val shortLabel: String,
    val moodLevel: Int?,
    val symptomCount: Int,
    val maxSeverity: Int?
)

private data class SymptomFrequencyItem(
    val label: String,
    val count: Int
)

private data class ThirtyDayAnalyticsModel(
    val days: List<DailyAnalyticsPoint>,
    val avgMood: Double?,
    val loggedMoodDays: Int,
    val totalSymptomEntries: Int,
    val peakDailySymptoms: Int,
    val topSymptoms: List<SymptomFrequencyItem>
)

private fun buildThirtyDayAnalytics(
    consultations: List<Consultation>,
    checkIns: List<DailyCheckIn>
): ThirtyDayAnalyticsModel {
    val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

    val checkInByKey = checkIns.associateBy { it.dateKey }
    val consultationsByKey = consultations.groupBy { entry ->
        val date = (entry.createdAt ?: entry.updatedAt ?: Timestamp.now()).toDate()
        keyFormatter.format(date)
    }

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -(WINDOW_DAYS - 1))
    }

    val validDateKeys = HashSet<String>(WINDOW_DAYS)
    val points = ArrayList<DailyAnalyticsPoint>(WINDOW_DAYS)

    repeat(WINDOW_DAYS) {
        val date = calendar.time
        val key = keyFormatter.format(date)
        validDateKeys.add(key)

        val checkIn = checkInByKey[key]
        val dayConsults = consultationsByKey[key].orEmpty()
        points.add(
            DailyAnalyticsPoint(
                dateKey = key,
                shortLabel = labelFormatter.format(date),
                moodLevel = checkIn?.moodLevel?.toInt()?.coerceIn(1, 5),
                symptomCount = dayConsults.size,
                maxSeverity = dayConsults.maxOfOrNull { it.severity.toInt() }
            )
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    val moods = points.mapNotNull { it.moodLevel }
    val avgMood = if (moods.isEmpty()) null else moods.average()
    val totalSymptoms = points.sumOf { it.symptomCount }
    val peakDaily = points.maxOfOrNull { it.symptomCount } ?: 0

    // Frequency breakdown across consultations within the 30-day window
    val freqMap = linkedMapOf<String, Int>()
    consultations.forEach { entry ->
        val date = (entry.createdAt ?: entry.updatedAt ?: Timestamp.now()).toDate()
        val key = keyFormatter.format(date)
        if (key in validDateKeys) {
            if (entry.associatedSymptoms.isNotEmpty()) {
                entry.associatedSymptoms.forEach { symptom ->
                    val clean = symptom.trim()
                    if (clean.isNotEmpty()) {
                        freqMap[clean] = (freqMap[clean] ?: 0) + 1
                    }
                }
            } else if (entry.bodyArea.isNotBlank()) {
                val clean = entry.bodyArea.trim()
                freqMap[clean] = (freqMap[clean] ?: 0) + 1
            }
        }
    }

    val topSymptoms = freqMap.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(6)
        .map { SymptomFrequencyItem(label = it.key, count = it.value) }

    return ThirtyDayAnalyticsModel(
        days = points,
        avgMood = avgMood,
        loggedMoodDays = moods.size,
        totalSymptomEntries = totalSymptoms,
        peakDailySymptoms = peakDaily,
        topSymptoms = topSymptoms
    )
}

@Composable
internal fun ThirtyDayWellnessAnalyticsCard(
    consultations: List<Consultation>,
    checkIns: List<DailyCheckIn>,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val analytics = remember(consultations, checkIns) {
        buildThirtyDayAnalytics(consultations = consultations, checkIns = checkIns)
    }

    var activeView by remember { mutableStateOf(AnalyticsChartView.TIMELINE) }
    var selectedDayIndex by remember(analytics.days) {
        val lastActive = analytics.days.indexOfLast { it.moodLevel != null || it.symptomCount > 0 }
        mutableStateOf(if (lastActive >= 0) lastActive else null)
    }

    val hasAnyData = analytics.loggedMoodDays > 0 || analytics.totalSymptomEntries > 0

    SanctuaryCard(
        modifier = modifier.testTag("history_30d_analytics_card")
    ) {
        // Header + View Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_30d_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.history_30d_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chart Mode Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeView == AnalyticsChartView.TIMELINE,
                onClick = { activeView = AnalyticsChartView.TIMELINE },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.history_30d_tab_timeline),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("history_30d_mode_timeline")
            )
            FilterChip(
                selected = activeView == AnalyticsChartView.BREAKDOWN,
                onClick = { activeView = AnalyticsChartView.BREAKDOWN },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.history_30d_tab_breakdown),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("history_30d_mode_breakdown")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 30-Day Summary Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMetricPill(
                label = "Avg Mood",
                value = analytics.avgMood?.let { String.format(Locale.US, "%.1f/5", it) } ?: "—",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            AnalyticsMetricPill(
                label = "30d Symptoms",
                value = "${analytics.totalSymptomEntries}",
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            AnalyticsMetricPill(
                label = "Mood Logs",
                value = "${analytics.loggedMoodDays}/30",
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasAnyData) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, sanaColors.subtleDivider)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No 30-day entries yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Log a daily wellness check-in or complete a symptom consultation to visualize your 30-day mood and symptom frequency trends here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (activeView == AnalyticsChartView.TIMELINE) {
            // Selected Day Interactive Tooltip Banner
            val inspectedDay = selectedDayIndex?.let { analytics.days.getOrNull(it) }
            if (inspectedDay != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_30d_tooltip"),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inspectedDay.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val moodText = inspectedDay.moodLevel?.let { lvl ->
                            "${WellnessMoodOption.fromLevel(lvl).label} ($lvl/5)"
                        } ?: "No mood log"
                        val sympText = when (inspectedDay.symptomCount) {
                            0 -> "0 symptoms"
                            1 -> "1 entry" + (inspectedDay.maxSeverity?.let { " • Sev $it/10" } ?: "")
                            else -> "${inspectedDay.symptomCount} entries" + (inspectedDay.maxSeverity?.let { " • Max $it/10" } ?: "")
                        }
                        Text(
                            text = "$moodText  •  $sympText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.history_30d_legend_mood)
                )
                ChartLegendItem(
                    color = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.history_30d_legend_symptoms)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual-Series 30-Day Native Canvas Chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-Axis Mood Scale Labels (5..1)
                Column(
                    modifier = Modifier
                        .height(148.dp)
                        .padding(end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                ThirtyDayDualSeriesCanvas(
                    days = analytics.days,
                    peakDailySymptoms = analytics.peakDailySymptoms,
                    selectedIndex = selectedDayIndex,
                    onSelectIndex = { selectedDayIndex = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp)
                        .testTag("history_30d_dual_chart")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-Axis Date Anchors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = analytics.days.firstOrNull()?.shortLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = analytics.days.getOrNull(WINDOW_DAYS / 2)?.shortLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // 30-Day Top Symptom Frequency Horizontal Bar Chart
            if (analytics.topSymptoms.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, sanaColors.subtleDivider)
                ) {
                    Text(
                        text = "No symptom consultations recorded in the last 30 days. Complete a guided symptom check to see recurring frequency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                val maxFreq = analytics.topSymptoms.maxOf { it.count }.coerceAtLeast(1)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("history_30d_symptom_breakdown")
                ) {
                    analytics.topSymptoms.forEachIndexed { idx, item ->
                        val barColor = when (idx) {
                            0 -> MaterialTheme.colorScheme.tertiary
                            1 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.count} ${if (item.count == 1) "entry" else "entries"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = barColor
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (item.count.toFloat() / maxFreq.toFloat()).coerceIn(0.08f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LocalSanaColors.current.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
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
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThirtyDayDualSeriesCanvas(
    days: List<DailyAnalyticsPoint>,
    peakDailySymptoms: Int,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val moodLineColor = MaterialTheme.colorScheme.primary
    val symptomBarColor = MaterialTheme.colorScheme.tertiary
    val gridColor = LocalSanaColors.current.subtleDivider
    val cursorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val summaryDescription = remember(days) {
        val moodCount = days.count { it.moodLevel != null }
        val symptomSum = days.sumOf { it.symptomCount }
        "30-day mood and symptom frequency chart with $moodCount mood logs and $symptomSum symptom entries"
    }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = summaryDescription }
            .pointerInput(days.size) {
                detectTapGestures { offset ->
                    if (days.isNotEmpty() && size.width > 0) {
                        val idx = ((offset.x / size.width.toFloat()) * (days.size - 1))
                            .roundToInt()
                            .coerceIn(0, days.lastIndex)
                        onSelectIndex(idx)
                    }
                }
            }
            .pointerInput(days.size) {
                detectHorizontalDragGestures { change, _ ->
                    if (days.isNotEmpty() && size.width > 0) {
                        val idx = ((change.position.x / size.width.toFloat()) * (days.size - 1))
                            .roundToInt()
                            .coerceIn(0, days.lastIndex)
                        onSelectIndex(idx)
                    }
                }
            }
    ) {
        if (days.isEmpty()) return@Canvas

        val padX = 8.dp.toPx()
        val padY = 10.dp.toPx()
        val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
        val chartH = (size.height - padY * 2f).coerceAtLeast(1f)
        val stepX = if (days.size > 1) chartW / (days.size - 1) else chartW

        // 1. Subtle horizontal reference grid lines (Mood levels 5, 3, 1)
        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = padY + chartH * fraction
            drawLine(
                color = gridColor,
                start = Offset(padX, y),
                end = Offset(size.width - padX, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Selected Day Vertical Highlight Cursor
        selectedIndex?.let { sel ->
            if (sel in days.indices) {
                val cursorX = padX + sel * stepX
                drawLine(
                    color = cursorColor,
                    start = Offset(cursorX, padY),
                    end = Offset(cursorX, size.height - padY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // 3. Symptom Frequency Bars (Terracotta Clay rounded bars anchored at baseline)
        val barScaleMax = peakDailySymptoms.coerceAtLeast(3).toFloat()
        val barWidth = (stepX * 0.54f).coerceIn(4.dp.toPx(), 10.dp.toPx())
        days.forEachIndexed { idx, day ->
            if (day.symptomCount > 0) {
                val cx = padX + idx * stepX
                val heightFraction = (day.symptomCount / barScaleMax).coerceIn(0.18f, 0.85f)
                val barH = chartH * heightFraction
                val topY = (size.height - padY) - barH
                val isSelected = idx == selectedIndex
                drawRoundRect(
                    color = symptomBarColor.copy(alpha = if (isSelected) 0.92f else 0.58f),
                    topLeft = Offset(cx - barWidth / 2f, topY),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        // 4. Mood Line + Soft Gradient Area Fill (Warm Teal across logged mood days)
        val moodCoords = days.mapIndexedNotNull { idx, day ->
            val level = day.moodLevel ?: return@mapIndexedNotNull null
            val x = padX + idx * stepX
            val norm = (level - 1) / 4f
            val y = padY + (1f - norm) * chartH
            idx to Offset(x, y)
        }

        if (moodCoords.size >= 2) {
            val fillPath = Path().apply {
                moveTo(moodCoords.first().second.x, size.height - padY)
                moodCoords.forEach { (_, pt) -> lineTo(pt.x, pt.y) }
                lineTo(moodCoords.last().second.x, size.height - padY)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        moodLineColor.copy(alpha = 0.22f),
                        moodLineColor.copy(alpha = 0.02f)
                    ),
                    startY = padY,
                    endY = size.height - padY
                )
            )

            val linePath = Path().apply {
                moveTo(moodCoords.first().second.x, moodCoords.first().second.y)
                for (i in 1 until moodCoords.size) {
                    val prev = moodCoords[i - 1].second
                    val curr = moodCoords[i].second
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = linePath,
                color = moodLineColor,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 5. Mood Nodes
        moodCoords.forEach { (dayIdx, pt) ->
            val isSelected = dayIdx == selectedIndex
            if (isSelected) {
                drawCircle(
                    color = moodLineColor.copy(alpha = 0.24f),
                    radius = 9.dp.toPx(),
                    center = pt
                )
            }
            drawCircle(
                color = surfaceColor,
                radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = moodLineColor,
                radius = if (isSelected) 3.8.dp.toPx() else 2.6.dp.toPx(),
                center = pt
            )
        }
    }
}
