package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.HealthConnectDailyHeartRate
import com.example.ui.theme.LocalSanaColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Interactive 7-day weekly heart rate trend line chart inspired by Recharts' Cartesian line/area
 * chart (dashed horizontal gridlines, Y-axis domain scaling, smooth cubic curve, daily min-max
 * variability bars, active dot cursor, and touch/drag tooltip readout).
 */
@Composable
fun WeeklyHeartRateTrendChart(
    dailyTrend: List<HealthConnectDailyHeartRate>,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val heartColor = sanaColors.heartPink
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val defaultSelectedIndex = remember(dailyTrend) {
        val lastPopulated = dailyTrend.indexOfLast { it.avgBpm != null }
        when {
            lastPopulated >= 0 -> lastPopulated
            dailyTrend.isNotEmpty() -> dailyTrend.lastIndex
            else -> 0
        }
    }
    var selectedIndex by remember(dailyTrend) { mutableIntStateOf(defaultSelectedIndex) }
    val clampedIndex = selectedIndex.coerceIn(0, (dailyTrend.size - 1).coerceAtLeast(0))
    val selectedDay = dailyTrend.getOrNull(clampedIndex)

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = clampedIndex.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hr_trend_cursor"
    )

    val hasAnyData = remember(dailyTrend) { dailyTrend.any { it.avgBpm != null } }

    // Compute clean 10-bpm Y-axis domain bounds across all daily min/max/avg values
    val yAxisBounds = remember(dailyTrend) {
        val allValues = dailyTrend.flatMap { day ->
            listOfNotNull(day.minBpm, day.avgBpm, day.maxBpm)
        }
        if (allValues.isEmpty()) {
            Triple(50L, 80L, 110L)
        } else {
            val rawMin = allValues.min()
            val rawMax = allValues.max()
            val minBound = max(30L, (floor((rawMin - 6).toDouble() / 10.0) * 10.0).toLong())
            var maxBound = (ceil((rawMax + 6).toDouble() / 10.0) * 10.0).toLong()
            if (maxBound - minBound < 20L) {
                maxBound = minBound + 20L
            }
            val midBound = (minBound + maxBound) / 2L
            Triple(minBound, midBound, maxBound)
        }
    }

    val chartContentDesc = stringResource(R.string.cd_hc_hr_weekly_trend_chart)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hc_hr_weekly_trend_chart")
            .semantics { contentDescription = chartContentDesc }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header + inline legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.hc_hr_trend_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.hc_hr_trend_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDotItem(
                        color = heartColor,
                        label = stringResource(R.string.hc_hr_trend_legend_avg)
                    )
                    LegendBarItem(
                        color = heartColor.copy(alpha = 0.28f),
                        label = stringResource(R.string.hc_hr_trend_legend_range)
                    )
                }
            }

            if (!hasAnyData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.hc_hr_trend_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Recharts-style dynamic tooltip readout for the active day
                if (selectedDay != null) {
                    HeartRateTooltipBar(
                        day = selectedDay,
                        accent = heartColor
                    )
                }

                // Cartesian Y-axis ticks + interactive line/area Canvas
                val (yMin, yMid, yMax) = yAxisBounds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Y-axis ticks
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$yMax",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                        Text(
                            text = "$yMid",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                        Text(
                            text = "$yMin",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(dailyTrend) {
                                val count = dailyTrend.size
                                if (count <= 0) return@pointerInput

                                fun updateSelectionForX(x: Float) {
                                    val horizontalPad = 14.dp.toPx()
                                    val usableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                                    val fraction = ((x - horizontalPad) / usableWidth).coerceIn(0f, 1f)
                                    val idx = if (count == 1) 0 else (fraction * (count - 1)).roundToInt()
                                    selectedIndex = idx.coerceIn(0, count - 1)
                                }

                                detectTapGestures { offset ->
                                    updateSelectionForX(offset.x)
                                }
                            }
                            .pointerInput(dailyTrend) {
                                val count = dailyTrend.size
                                if (count <= 0) return@pointerInput

                                fun updateSelectionForX(x: Float) {
                                    val horizontalPad = 14.dp.toPx()
                                    val usableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                                    val fraction = ((x - horizontalPad) / usableWidth).coerceIn(0f, 1f)
                                    val idx = if (count == 1) 0 else (fraction * (count - 1)).roundToInt()
                                    selectedIndex = idx.coerceIn(0, count - 1)
                                }

                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    updateSelectionForX(change.position.x)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val count = dailyTrend.size
                            if (count == 0) return@Canvas

                            val topPad = 12.dp.toPx()
                            val bottomPad = 12.dp.toPx()
                            val horizontalPad = 14.dp.toPx()
                            val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                            val plotWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                            val yRange = (yMax - yMin).coerceAtLeast(1L).toFloat()

                            fun xForIndex(index: Float): Float {
                                return if (count == 1) {
                                    horizontalPad + plotWidth / 2f
                                } else {
                                    horizontalPad + (index / (count - 1).toFloat()) * plotWidth
                                }
                            }

                            fun yForBpm(bpm: Long): Float {
                                val normalized = ((bpm - yMin).toFloat() / yRange).coerceIn(0f, 1f)
                                return topPad + plotHeight * (1f - normalized)
                            }

                            // 1. Recharts-style dashed horizontal Cartesian gridlines
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            listOf(yMax, yMid, yMin).forEach { tick ->
                                val y = yForBpm(tick)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            // 2. Vertical active cursor guideline
                            val cursorX = xForIndex(animatedSelectedIndex)
                            drawLine(
                                color = heartColor.copy(alpha = 0.4f),
                                start = Offset(cursorX, topPad),
                                end = Offset(cursorX, size.height - bottomPad),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )

                            // 3. Daily min-max range capsules (subtle variability band per day)
                            val capsuleWidth = 8.dp.toPx()
                            dailyTrend.forEachIndexed { idx, day ->
                                val dayMin = day.minBpm
                                val dayMax = day.maxBpm
                                if (dayMin != null && dayMax != null && dayMax > dayMin) {
                                    val x = xForIndex(idx.toFloat())
                                    val yTop = yForBpm(dayMax)
                                    val yBottom = yForBpm(dayMin)
                                    val isSelected = idx == clampedIndex
                                    drawRoundRect(
                                        color = heartColor.copy(alpha = if (isSelected) 0.30f else 0.16f),
                                        topLeft = Offset(x - capsuleWidth / 2f, yTop),
                                        size = Size(capsuleWidth, (yBottom - yTop).coerceAtLeast(4.dp.toPx())),
                                        cornerRadius = CornerRadius(capsuleWidth / 2f, capsuleWidth / 2f)
                                    )
                                }
                            }

                            // 4. Collect populated points for smooth curve & gradient fill
                            val populatedPoints = dailyTrend.mapIndexedNotNull { idx, day ->
                                day.avgBpm?.let { bpm ->
                                    idx to Offset(xForIndex(idx.toFloat()), yForBpm(bpm))
                                }
                            }

                            if (populatedPoints.size >= 2) {
                                // Group contiguous days into smooth solid segments; bridge gaps with dashed lines
                                val segments = mutableListOf<MutableList<Pair<Int, Offset>>>()
                                populatedPoints.forEach { point ->
                                    val current = segments.lastOrNull()
                                    if (current != null && point.first == current.last().first + 1) {
                                        current.add(point)
                                    } else {
                                        segments.add(mutableListOf(point))
                                    }
                                }

                                // Dashed bridge across missing days so overall weekly trajectory stays legible
                                for (s in 0 until segments.lastIndex) {
                                    val from = segments[s].last().second
                                    val to = segments[s + 1].first().second
                                    drawLine(
                                        color = heartColor.copy(alpha = 0.45f),
                                        start = from,
                                        end = to,
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }

                                val baselineY = size.height - bottomPad
                                segments.forEach { segment ->
                                    if (segment.size >= 2) {
                                        val linePath = Path().apply {
                                            moveTo(segment.first().second.x, segment.first().second.y)
                                            for (i in 0 until segment.lastIndex) {
                                                val p0 = segment[i].second
                                                val p1 = segment[i + 1].second
                                                val midX = (p0.x + p1.x) / 2f
                                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                            }
                                        }

                                        val fillPath = Path().apply {
                                            addPath(linePath)
                                            lineTo(segment.last().second.x, baselineY)
                                            lineTo(segment.first().second.x, baselineY)
                                            close()
                                        }

                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    heartColor.copy(alpha = 0.24f),
                                                    heartColor.copy(alpha = 0.02f)
                                                ),
                                                startY = topPad,
                                                endY = baselineY
                                            )
                                        )

                                        drawPath(
                                            path = linePath,
                                            color = heartColor,
                                            style = Stroke(
                                                width = 2.75.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }

                            // 5. Data point dots & Recharts activeDot halo
                            populatedPoints.forEach { (idx, point) ->
                                val isSelected = idx == clampedIndex
                                if (isSelected) {
                                    drawCircle(
                                        color = heartColor.copy(alpha = 0.22f),
                                        radius = 11.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = 6.5.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = heartColor,
                                        radius = 4.5.dp.toPx(),
                                        center = point
                                    )
                                } else {
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = 4.5.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = heartColor,
                                        radius = 4.5.dp.toPx(),
                                        center = point,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive X-axis day strip (aligned with canvas plot area, >=48dp touch height)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 42.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dailyTrend.forEachIndexed { idx, day ->
                        val isSelected = idx == clampedIndex
                        val hasDayData = day.avgBpm != null
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedIndex = idx }
                                .testTag("hc_hr_day_${day.dayLabel.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) heartColor.copy(alpha = 0.14f) else Color.Transparent
                                ) {
                                    Text(
                                        text = day.dayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> heartColor
                                            hasDayData -> MaterialTheme.colorScheme.onSurface
                                            else -> mutedColor.copy(alpha = 0.65f)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasDayData) {
                                                if (isSelected) heartColor else heartColor.copy(alpha = 0.4f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
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
private fun HeartRateTooltipBar(
    day: HealthConnectDailyHeartRate,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hc_hr_trend_tooltip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${day.dayLabel} • ${day.dateKey}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (day.avgBpm != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${day.avgBpm} bpm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = "avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.hc_hr_trend_no_samples),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (day.avgBpm != null) {
                Column(horizontalAlignment = Alignment.End) {
                    if (day.minBpm != null && day.maxBpm != null) {
                        Text(
                            text = "${day.minBpm}–${day.maxBpm} bpm",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${day.sampleCount} ${if (day.sampleCount == 1) "sample" else "samples"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
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
private fun LegendBarItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
