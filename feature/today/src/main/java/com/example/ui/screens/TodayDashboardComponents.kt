package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.core.DayHabitDot
import com.example.core.WellnessStreakAndShareHelper
import com.example.data.model.BloodType
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareTask
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MentalAssessment
import com.example.data.model.VitalReading
import com.example.ui.components.GlassGoalCelebrationBanner
import com.example.ui.theme.GlassHealthCard
import com.example.ui.theme.GlassRenderMode
import com.example.ui.theme.GlassStatusPill
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.HealthConnectUiState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hero Daily Wellness Ring Anchor Card.
 * Synthesizes real logged daily habits (Daily Check-In, active Meds taken, Care Checklist)
 * into a single motivating circular ring with a streak badge and a privacy-safe milestone share action.
 */
@Composable
fun DailyWellnessRingHeroCard(
    todayDateKey: String,
    todayCheckIn: DailyCheckIn?,
    checkIns: List<DailyCheckIn>,
    isSavingCheckIn: Boolean = false,
    medications: List<MedicationItem>,
    medicationLogs: List<MedicationLog>,
    careTasks: List<CareTask>,
    stepsToday: Long?,
    onQuickLogMood: (WellnessMoodOption) -> Unit = {},
    onOpenCheckIn: () -> Unit,
    onOpenMedications: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val sanaColors = LocalSanaColors.current

    val streakSummary = remember(todayDateKey, checkIns) {
        WellnessStreakAndShareHelper.computeStreakSummary(todayDateKey, checkIns)
    }
    val activeMeds = remember(medications) { medications.filter { it.active } }
    val takenMedsToday = remember(activeMeds, medicationLogs, todayDateKey) {
        val byMed = medicationLogs.filter { it.dateKey == todayDateKey }.associateBy { it.medicationId }
        activeMeds.count { byMed[it.id]?.parsedStatus == MedicationLogStatus.TAKEN }
    }
    val completedTasks = remember(careTasks) { careTasks.count { it.completed } }

    // Real deterministic habit score (only counts categories the user actually participates in)
    val (completedGoals, totalGoals) = remember(
        todayCheckIn,
        activeMeds.size,
        takenMedsToday,
        careTasks.size,
        completedTasks
    ) {
        var total = 1 // Daily Check-In is always the core daily habit
        var done = if (todayCheckIn != null) 1 else 0
        if (activeMeds.isNotEmpty()) {
            total += 1
            if (takenMedsToday >= activeMeds.size) done += 1
        }
        if (careTasks.isNotEmpty()) {
            total += 1
            if (completedTasks >= careTasks.size) done += 1
        }
        done to total
    }

    val isGoalReached = completedGoals == totalGoals && totalGoals > 0
    var previouslyReached by remember { mutableStateOf(isGoalReached) }
    LaunchedEffect(isGoalReached) {
        if (isGoalReached && !previouslyReached) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previouslyReached = isGoalReached
    }

    val rawProgress = if (totalGoals > 0) completedGoals.toFloat() / totalGoals.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "daily_wellness_ring_progress"
    )
    val percentInt = (rawProgress * 100f).toInt().coerceIn(0, 100)
    val animatedPercentInt = (animatedProgress * 100f).toInt().coerceIn(0, 100)

    val heroGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
            sanaColors.medsTeal.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.surface
        )
    )

    val headlineText = when {
        completedGoals == totalGoals -> stringResource(R.string.today_ring_complete_title)
        completedGoals > 0 -> stringResource(R.string.today_ring_progress_title)
        else -> stringResource(R.string.today_ring_start_title)
    }

    val subtitleText = buildString {
        if (todayCheckIn == null) {
            append("Log today's mood, sleep & physical activity to sync to your cloud journal")
        } else if (activeMeds.isNotEmpty() && takenMedsToday < activeMeds.size) {
            append("${activeMeds.size - takenMedsToday} medication dose(s) left today")
        } else if (careTasks.isNotEmpty() && completedTasks < careTasks.size) {
            append("${careTasks.size - completedTasks} care habit(s) remaining")
        } else {
            append("All $totalGoals daily wellness habits checked off")
        }
    }

    val shareChooserTitle = stringResource(R.string.today_share_chooser_title)
    fun shareVisualMilestoneCard() {
        val intent = WellnessStreakAndShareHelper.createMilestoneShareIntent(
            context = context,
            chooserTitle = shareChooserTitle,
            percentComplete = percentInt,
            streakSummary = streakSummary,
            completedGoals = completedGoals,
            totalGoals = totalGoals
        )
        context.startActivity(intent)
    }

    GlassHealthCard(
        modifier = Modifier.testTag("home_daily_ring_hero_card"),
        cornerRadius = 22.dp,
        accentTint = MaterialTheme.colorScheme.primary,
        renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar: Eyebrow + Real Consecutive Streak Pill + Visual PNG Share Button
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
                        text = stringResource(R.string.today_ring_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (streakSummary.consecutiveDays > 0) {
                        GlassStatusPill(
                            label = "${streakSummary.consecutiveDays}d streak",
                            accentColor = sanaColors.activityOrange,
                            modifier = Modifier.testTag("home_streak_pill"),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Active wellness streak",
                                    tint = sanaColors.activityOrange,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                    } else if (isGoalReached) {
                        GlassStatusPill(
                            label = "Goal Reached",
                            accentColor = sanaColors.healthyGreen
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        shareVisualMilestoneCard()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_share_streak_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.today_share_streak_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Middle: Animated Ring + Status & Live Micro-Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WellnessProgressRing(
                    progress = animatedProgress,
                    percentText = "$animatedPercentInt%",
                    ringColor = if (completedGoals == totalGoals) {
                        sanaColors.healthyGreen
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    secondaryRingColor = sanaColors.medsTeal,
                    modifier = Modifier.size(88.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniStatusChip(
                            label = if (todayCheckIn != null) "Check-In ✓" else "Check-In",
                            completed = todayCheckIn != null,
                            accentColor = sanaColors.sleepIndigo
                        )
                        if (activeMeds.isNotEmpty()) {
                            MiniStatusChip(
                                label = "Meds $takenMedsToday/${activeMeds.size}",
                                completed = takenMedsToday >= activeMeds.size,
                                accentColor = sanaColors.medsTeal
                            )
                        } else if (stepsToday != null && stepsToday > 0L) {
                            val formattedSteps = NumberFormat.getIntegerInstance().format(stepsToday)
                            MiniStatusChip(
                                label = "$formattedSteps steps",
                                completed = stepsToday >= 6000L,
                                accentColor = sanaColors.activityOrange
                            )
                        }
                    }
                }
            }

            // Daily Goal Celebration Banner when 100% of daily habits are reached
            AnimatedVisibility(visible = completedGoals == totalGoals && totalGoals > 0) {
                GlassGoalCelebrationBanner(
                    title = "Daily Goal Reached",
                    subtitle = if (streakSummary.consecutiveDays > 1) {
                        "${streakSummary.consecutiveDays}-day wellness rhythm locked in."
                    } else {
                        "Every habit for today is complete."
                    }
                )
            }

            // 7-Day Visual Habit Consistency Strip (M T W T F S S)
            SevenDayHabitStrip(days = streakSummary.last7Days)

            // Instant 1-Tap Inline Quick Mood Bar when Today isn't logged yet
            AnimatedVisibility(visible = todayCheckIn == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.today_quick_mood_prompt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WellnessMoodOption.entries.forEach { mood ->
                            val moodTint = when (mood) {
                                WellnessMoodOption.LOW -> sanaColors.urgentFg
                                WellnessMoodOption.TENSE -> sanaColors.activityOrange
                                WellnessMoodOption.STEADY -> sanaColors.sleepIndigo
                                WellnessMoodOption.CALM -> MaterialTheme.colorScheme.primary
                                WellnessMoodOption.THRIVING -> sanaColors.healthyGreen
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !isSavingCheckIn) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onQuickLogMood(mood)
                                    }
                                    .testTag("quick_mood_chip_${mood.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = SanaTheme.glass.cardFillElevated,
                                border = BorderStroke(1.dp, moodTint.copy(alpha = 0.32f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = mood.icon,
                                        contentDescription = "Log mood ${mood.label}",
                                        tint = moodTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = mood.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Logged Today Summary Strip (Mood • Sleep • Physical Activity)
            AnimatedVisibility(visible = todayCheckIn != null) {
                if (todayCheckIn != null) {
                    val loggedMood = WellnessMoodOption.fromLevel(todayCheckIn.moodLevel.toInt())
                    val loggedActivity = todayCheckIn.parsedActivityLevel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_logged_wellness_summary"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniStatusChip(
                            label = "Mood: ${loggedMood.label}",
                            completed = true,
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        MiniStatusChip(
                            label = String.format(Locale.US, "%.1fh sleep", todayCheckIn.sleepHours),
                            completed = todayCheckIn.sleepHours >= 7.0,
                            accentColor = sanaColors.sleepIndigo
                        )
                        MiniStatusChip(
                            label = if (todayCheckIn.activityMinutes > 0L) {
                                "${loggedActivity.shortLabel} ${todayCheckIn.activityMinutes}m"
                            } else {
                                loggedActivity.shortLabel
                            },
                            completed = todayCheckIn.activityMinutes >= 30L,
                            accentColor = sanaColors.activityOrange
                        )
                    }
                }
            }

            // Primary Action CTA (contextual next step or full check-in details)
            val isMedsNext = todayCheckIn != null && activeMeds.isNotEmpty() && takenMedsToday < activeMeds.size
            Button(
                onClick = if (isMedsNext) onOpenMedications else onOpenCheckIn,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_checkin_screen_button")
            ) {
                if (isMedsNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_health_medication),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_health_checkin),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isMedsNext -> "Log Remaining Medications"
                        todayCheckIn != null -> "Update Mood, Sleep & Activity"
                        else -> "Daily Check-In (Mood, Sleep & Activity)"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SevenDayHabitStrip(
    days: List<DayHabitDot>
) {
    val glass = SanaTheme.glass
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = glass.cardFill,
        border = BorderStroke(1.dp, glass.borderMid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = day.dayLetter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (day.isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = when {
                            day.completed -> MaterialTheme.colorScheme.primary
                            day.isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        border = if (day.isToday && !day.completed) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (day.completed) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Logged",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
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
private fun WellnessProgressRing(
    progress: Float,
    percentText: String,
    ringColor: Color,
    secondaryRingColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = SanaTheme.isDark
    val trackColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 10.dp.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = arcSize.width / 2f

            // Subtle frosted background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            val sweep = (progress * 360f).coerceAtLeast(if (progress > 0f) 18f else 0f)
            if (sweep > 0f) {
                // Soft translucent luminous halo behind active arc
                drawArc(
                    color = ringColor.copy(alpha = if (isDark) 0.28f else 0.18f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Smooth translucent gradient progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ringColor, secondaryRingColor, ringColor),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )

                // Glowing specular end-cap at arc leading tip
                val endRad = Math.toRadians((-90f + sweep).toDouble())
                val tipX = center.x + (radius * cos(endRad)).toFloat()
                val tipY = center.y + (radius * sin(endRad)).toFloat()
                val tipCenter = Offset(tipX, tipY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            secondaryRingColor.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = tipCenter,
                        radius = strokePx * 0.95f
                    ),
                    radius = strokePx * 0.95f,
                    center = tipCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.92f),
                    radius = strokePx * 0.24f,
                    center = tipCenter
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentText,
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniStatusChip(
    label: String,
    completed: Boolean,
    accentColor: Color
) {
    val sanaColors = LocalSanaColors.current
    val pillAccent = if (completed) sanaColors.selfCareFg else accentColor
    Surface(
        color = pillAccent.copy(alpha = if (SanaTheme.isDark) 0.20f else 0.13f),
        shape = CircleShape,
        border = BorderStroke(1.dp, pillAccent.copy(alpha = if (SanaTheme.isDark) 0.36f else 0.24f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.tabular(),
            fontWeight = FontWeight.SemiBold,
            color = pillAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

private data class QuickActionItem(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector? = null,
    @DrawableRes val drawableRes: Int? = null,
    val accentColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Horizontal thumb-zone Quick Action Pill Rail so top user intents are 1 tap away above the fold.
 */
@Composable
fun QuickActionPillRail(
    onStartSymptomCheck: () -> Unit,
    onStartPhotoAnalysis: () -> Unit,
    onOpenVitals: () -> Unit,
    onOpenDailyCheckIn: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenPdfExport: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val primary = MaterialTheme.colorScheme.primary

    val items = listOf(
        QuickActionItem(
            id = "symptoms",
            labelRes = R.string.today_quick_symptom,
            icon = Icons.Rounded.MedicalServices,
            accentColor = primary,
            testTag = "home_check_symptoms_card",
            onClick = onStartSymptomCheck
        ),
        QuickActionItem(
            id = "photo",
            labelRes = R.string.today_quick_photo,
            icon = Icons.Rounded.DocumentScanner,
            accentColor = sanaColors.medsTeal,
            testTag = "home_photo_analysis_card",
            onClick = onStartPhotoAnalysis
        ),
        QuickActionItem(
            id = "vital",
            labelRes = R.string.today_quick_vital,
            drawableRes = R.drawable.ic_health_heart_rate,
            accentColor = sanaColors.heartPink,
            testTag = "quick_pill_vital",
            onClick = onOpenVitals
        ),
        QuickActionItem(
            id = "checkin",
            labelRes = R.string.today_quick_checkin,
            drawableRes = R.drawable.ic_health_checkin,
            accentColor = sanaColors.sleepIndigo,
            testTag = "home_daily_checkin_card",
            onClick = onOpenDailyCheckIn
        ),
        QuickActionItem(
            id = "meds",
            labelRes = R.string.today_quick_meds,
            drawableRes = R.drawable.ic_health_medication,
            accentColor = sanaColors.healthyGreen,
            testTag = "quick_pill_meds",
            onClick = onOpenMedications
        ),
        QuickActionItem(
            id = "pdf",
            labelRes = R.string.today_quick_pdf,
            icon = Icons.Rounded.PictureAsPdf,
            accentColor = sanaColors.activityOrange,
            testTag = "quick_pill_pdf",
            onClick = onOpenPdfExport
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.today_quick_actions_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(items, key = { it.id }) { action ->
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = action.onClick)
                        .testTag(action.testTag),
                    color = SanaTheme.glass.cardFill,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(SanaTheme.glass.borderStart, SanaTheme.glass.borderEnd)
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = action.accentColor.copy(alpha = 0.14f),
                            shape = CircleShape
                        ) {
                            when {
                                action.drawableRes != null -> Icon(
                                    painter = painterResource(action.drawableRes),
                                    contentDescription = null,
                                    tint = action.accentColor,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(17.dp)
                                )
                                action.icon != null -> Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = action.accentColor,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(17.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(action.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contextual inline dose logger: appears ONLY when active medications are still pending today,
 * enabling 1-tap medication completion directly from the Today screen.
 */
@Composable
fun PendingMedsQuickLogCard(
    todayDateKey: String,
    medications: List<MedicationItem>,
    logs: List<MedicationLog>,
    onLogDose: (MedicationItem, MedicationLogStatus) -> Unit,
    onOpenMedications: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val haptics = LocalHapticFeedback.current
    val pendingMeds = remember(medications, logs, todayDateKey) {
        val loggedIds = logs
            .filter { it.dateKey == todayDateKey }
            .map { it.medicationId }
            .toSet()
        medications.filter { it.active && it.id !in loggedIds }
    }

    if (pendingMeds.isEmpty()) return

    GlassHealthCard(
        modifier = Modifier.testTag("home_pending_meds_quick_card"),
        cornerRadius = 22.dp,
        accentTint = sanaColors.medsTeal,
        borderColor = sanaColors.medsTeal.copy(alpha = 0.34f),
        renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        painter = painterResource(R.drawable.ic_health_medication),
                        contentDescription = "Medications due today",
                        tint = sanaColors.medsTeal,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "Due Today (${pendingMeds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenMedications)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            pendingMeds.take(3).forEach { med ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SanaTheme.glass.cardFillElevated,
                    border = BorderStroke(1.dp, SanaTheme.glass.borderMid)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = med.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val detail = listOf(med.dosage, med.parsedSlot.label, med.formattedReminderTime())
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            if (detail.isNotBlank()) {
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLogDose(med, MedicationLogStatus.TAKEN)
                                }
                                .testTag("quick_take_med_${med.id}"),
                            color = sanaColors.medsTeal,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Mark ${med.name} taken",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Take",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact 2x3 Apple Health-style Bento Grid uniting all 6 core health modules
 * without endless vertical card stacking.
 */
@Composable
fun HealthHubBentoGrid(
    todayDateKey: String,
    profile: HealthProfile?,
    vitals: List<VitalReading>,
    medications: List<MedicationItem>,
    medicationLogs: List<MedicationLog>,
    mentalAssessments: List<MentalAssessment>,
    bodyMeasurements: List<BodyMeasurement>,
    cycleLogs: List<CycleLog>,
    cycleTrackingEnabled: Boolean,
    useMetricBodyUnits: Boolean,
    healthConnectState: HealthConnectUiState,
    healthRegion: com.example.core.HealthRegionProfile = com.example.core.HealthRegionDirectory.US,
    onOpenVitals: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onOpenMentalWellbeing: () -> Unit,
    onOpenBodyAndCycle: () -> Unit,
    onOpenMedicalId: () -> Unit
) {
    val sanaColors = LocalSanaColors.current

    val latestVital = vitals.firstOrNull()
    val vitalValue = latestVital?.formattedReadingForRegion(healthRegion) ?: "Tap to log"
    val vitalSub = latestVital?.parsedType?.shortLabel ?: "BP, HR, SpO₂"

    val activeMeds = remember(medications) { medications.filter { it.active } }
    val takenMedsToday = remember(activeMeds, medicationLogs, todayDateKey) {
        val byMed = medicationLogs.filter { it.dateKey == todayDateKey }.associateBy { it.medicationId }
        activeMeds.count { byMed[it.id]?.parsedStatus == MedicationLogStatus.TAKEN }
    }
    val medsValue = if (activeMeds.isEmpty()) "Add plan" else "$takenMedsToday / ${activeMeds.size} taken"
    val medsSub = if (activeMeds.isEmpty()) "Rx & reminders" else "Active today"

    val hcSteps = healthConnectState.snapshot?.stepsToday
    val hcValue = when {
        hcSteps != null && hcSteps > 0L -> "${NumberFormat.getIntegerInstance().format(hcSteps)} steps"
        healthConnectState.hasAnyGrantedPermission -> "Synced"
        else -> "Connect"
    }
    val hcSub = if (healthConnectState.hasAnyGrantedPermission) "Wearables live" else "Steps, HR, sleep"

    val latestMental = mentalAssessments.firstOrNull()
    val mentalValue = latestMental?.let { "${it.parsedType.shortLabel}: ${it.totalScore}/${it.maxScore}" } ?: "2-min check"
    val mentalSub = latestMental?.parsedBand?.label ?: "PHQ-9 & GAD-7"

    val latestBody = bodyMeasurements.firstOrNull()
    val bodyValue = when {
        latestBody != null && latestBody.weightKg > 0.0 -> {
            if (useMetricBodyUnits) {
                String.format(Locale.US, "%.1f kg", latestBody.weightKg)
            } else {
                String.format(Locale.US, "%.1f lb", latestBody.weightLbs)
            }
        }
        cycleTrackingEnabled && cycleLogs.isNotEmpty() -> "${cycleLogs.size} cycle logs"
        else -> "Track body"
    }
    val bodySub = if (latestBody != null && latestBody.bmi > 0.0) {
        String.format(Locale.US, "BMI %.1f", latestBody.bmi)
    } else if (cycleTrackingEnabled) {
        "Weight & cycle"
    } else {
        "Weight & BMI"
    }

    val bloodLabel = profile?.let { BloodType.fromCode(it.bloodType) }
        ?.takeIf { it != BloodType.UNKNOWN }
        ?.label
    val medIdValue = bloodLabel?.let { "Blood $it" }
        ?: if (profile?.hasEmergencyContact == true) "ICE Ready" else "Set up ICE"
    val medIdSub = buildList {
        val allergyCount = profile?.allergies?.size ?: 0
        if (allergyCount > 0) add("$allergyCount allergies")
        if (profile?.hasEmergencyContact == true) add("Contact saved")
    }.joinToString(" • ").ifBlank { "Emergency card" }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.today_health_hub_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Row 1: Vitals + Medications
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HubBentoTile(
                title = "Vitals",
                value = vitalValue,
                subtitle = vitalSub,
                drawableRes = R.drawable.ic_health_heart_rate,
                accentColor = sanaColors.heartPink,
                testTag = "home_vitals_card",
                onClick = onOpenVitals,
                modifier = Modifier.weight(1f)
            )
            HubBentoTile(
                title = "Medications",
                value = medsValue,
                subtitle = medsSub,
                drawableRes = R.drawable.ic_health_medication,
                accentColor = sanaColors.medsTeal,
                testTag = "home_medications_card",
                onClick = onOpenMedications,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Health Connect + Mental Wellbeing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HubBentoTile(
                title = "Health Connect",
                value = hcValue,
                subtitle = hcSub,
                drawableRes = R.drawable.ic_health_steps,
                accentColor = MaterialTheme.colorScheme.primary,
                testTag = "home_health_connect_card",
                onClick = onOpenHealthConnect,
                modifier = Modifier.weight(1f)
            )
            HubBentoTile(
                title = "Mind & Mood",
                value = mentalValue,
                subtitle = mentalSub,
                drawableRes = R.drawable.ic_health_sleep,
                accentColor = sanaColors.sleepIndigo,
                testTag = "home_mental_wellbeing_card",
                onClick = onOpenMentalWellbeing,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Body & Cycle + Medical ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HubBentoTile(
                title = "Body & Cycle",
                value = bodyValue,
                subtitle = bodySub,
                drawableRes = R.drawable.ic_health_calories,
                accentColor = sanaColors.activityOrange,
                testTag = "home_body_cycle_card",
                onClick = onOpenBodyAndCycle,
                modifier = Modifier.weight(1f)
            )
            HubBentoTile(
                title = "Medical ID",
                value = medIdValue,
                subtitle = medIdSub,
                icon = Icons.Rounded.LocalHospital,
                accentColor = sanaColors.heartPink,
                testTag = "home_medical_id_card",
                onClick = onOpenMedicalId,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HubBentoTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector? = null,
    @DrawableRes drawableRes: Int? = null,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassHealthCard(
        modifier = modifier.testTag(testTag),
        cornerRadius = 20.dp,
        accentTint = accentColor,
        renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = if (SanaTheme.isDark) 0.20f else 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    when {
                        drawableRes != null -> Icon(
                            painter = painterResource(drawableRes),
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(19.dp)
                        )
                        icon != null -> Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(19.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Open $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
