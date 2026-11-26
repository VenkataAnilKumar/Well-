package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.DailyCheckIn
import com.example.data.model.PhysicalActivityLevel
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.CloudUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val CHECKIN_ENERGY_LABELS = listOf("Drained", "Tired", "Moderate", "Good", "Vibrant")

private val BODY_COMFORT_SIGNALS = listOf(
    "Well Rested",
    "Clear Mind",
    "Hydrated",
    "Active Movement",
    "Mild Headache",
    "Body Aches",
    "Congestion",
    "Digestive Upset",
    "Stress / Tension"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyCheckInScreen(
    todayDateKey: String,
    existingToday: DailyCheckIn?,
    checkInsState: CloudUiState<List<DailyCheckIn>>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSaveCheckIn: (
        moodLevel: Int,
        energyLevel: Int,
        sleepHours: Double,
        hydrationGlasses: Int,
        activityLevel: PhysicalActivityLevel,
        activityMinutes: Int,
        notes: String
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    val sanaColors = LocalSanaColors.current
    val recentCheckIns = (checkInsState as? CloudUiState.Success)?.data.orEmpty()
    val parsedExisting = remember(existingToday?.notes) {
        WellnessNoteCodec.decode(existingToday?.notes)
    }

    var moodLevel by remember { mutableIntStateOf(existingToday?.moodLevel?.toInt()?.coerceIn(1, 5) ?: 4) }
    var healthStatus by remember { mutableStateOf(parsedExisting.generalStatus) }
    var selectedSignals by remember { mutableStateOf(parsedExisting.signals) }
    var energyLevel by remember { mutableIntStateOf(existingToday?.energyLevel?.toInt()?.coerceIn(1, 5) ?: 3) }
    var sleepHours by remember { mutableDoubleStateOf(existingToday?.sleepHours?.coerceIn(0.0, 24.0) ?: 7.5) }
    var hydrationGlasses by remember { mutableIntStateOf(existingToday?.hydrationGlasses?.toInt()?.coerceIn(0, 30) ?: 5) }
    var activityLevel by remember {
        mutableStateOf(existingToday?.parsedActivityLevel ?: PhysicalActivityLevel.MODERATE)
    }
    var activityMinutes by remember {
        mutableIntStateOf(
            existingToday?.activityMinutes?.toInt()?.coerceIn(0, 600)
                ?: PhysicalActivityLevel.MODERATE.defaultMinutes
        )
    }
    var reflectionNote by remember { mutableStateOf(parsedExisting.reflection) }

    LaunchedEffect(existingToday?.updatedAt) {
        if (existingToday != null) {
            val decoded = WellnessNoteCodec.decode(existingToday.notes)
            moodLevel = existingToday.moodLevel.toInt().coerceIn(1, 5)
            healthStatus = decoded.generalStatus
            selectedSignals = decoded.signals
            energyLevel = existingToday.energyLevel.toInt().coerceIn(1, 5)
            sleepHours = existingToday.sleepHours.coerceIn(0.0, 24.0)
            hydrationGlasses = existingToday.hydrationGlasses.toInt().coerceIn(0, 30)
            activityLevel = existingToday.parsedActivityLevel
            activityMinutes = existingToday.activityMinutes.toInt().coerceIn(0, 600)
            reflectionNote = decoded.reflection
        }
    }

    val formattedDate = remember(todayDateKey) {
        runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(todayDateKey)
            SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(parsed ?: Date())
        }.getOrElse { todayDateKey }
    }

    val selectedMood = WellnessMoodOption.fromLevel(moodLevel)
    val encodedPreviewLength = WellnessNoteCodec.encode(
        status = healthStatus,
        signals = selectedSignals,
        reflection = reflectionNote
    ).length
    val maxReflectionChars = (600 - WellnessNoteCodec.encode(healthStatus, selectedSignals, "").length - 1)
        .coerceAtLeast(120)

    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("checkin_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.checkin_screen_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (recentCheckIns.isEmpty()) "First log" else "${recentCheckIns.size}d pulse",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Scrollable Check-In Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.checkin_screen_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Log your mood, sleep, physical activity, and hydration to save to your cloud wellness journal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (checkInsState is CloudUiState.Error) {
                item {
                    SanctuaryErrorBanner(message = checkInsState.message)
                }
            }

            // 1. Mood Selection Card
            item {
                SanctuaryCard {
                    Text(
                        text = stringResource(R.string.checkin_mood_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WellnessMoodOption.entries.forEach { option ->
                            val selected = moodLevel == option.level
                            val bgColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                animationSpec = spring(),
                                label = "mood_bg_${option.level}"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    sanaColors.cardBorder
                                },
                                animationSpec = spring(),
                                label = "mood_border_${option.level}"
                            )
                            val contentColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { moodLevel = option.level }
                                    .testTag("checkin_mood_${option.level}"),
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
                                        imageVector = option.icon,
                                        contentDescription = option.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
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

                    Spacer(modifier = Modifier.height(12.dp))
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
                                imageVector = selectedMood.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${selectedMood.label}: ${selectedMood.caption}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 1B. Physical Activity Level & Active Minutes Card
            item {
                PhysicalActivityCheckInCard(
                    selectedLevel = activityLevel,
                    activityMinutes = activityMinutes,
                    onSelectLevel = { chosen ->
                        activityLevel = chosen
                        activityMinutes = chosen.defaultMinutes
                    },
                    onChangeMinutes = { updatedMinutes ->
                        val clamped = updatedMinutes.coerceIn(0, 600)
                        activityMinutes = clamped
                        activityLevel = when {
                            clamped == 0 -> PhysicalActivityLevel.SEDENTARY
                            clamped < 20 -> PhysicalActivityLevel.LIGHT
                            clamped < 40 -> PhysicalActivityLevel.MODERATE
                            clamped < 60 -> PhysicalActivityLevel.ACTIVE
                            else -> PhysicalActivityLevel.VIGOROUS
                        }
                    }
                )
            }

            // 2. General Health Status & Body Signals Card
            item {
                SanctuaryCard {
                    Text(
                        text = stringResource(R.string.checkin_status_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How does your body feel overall today?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GeneralHealthStatus.entries.forEach { status ->
                            val selected = healthStatus == status
                            FilterChip(
                                selected = selected,
                                onClick = { healthStatus = status },
                                leadingIcon = {
                                    Icon(
                                        imageVector = status.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = status.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("checkin_status_${status.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = sanaColors.subtleDivider)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.checkin_signals_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Optional — select any comfort signals or mild symptoms you noticed today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BODY_COMFORT_SIGNALS.forEach { signal ->
                            val selected = signal in selectedSignals
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedSignals = if (selected) {
                                        selectedSignals - signal
                                    } else {
                                        selectedSignals + signal
                                    }
                                },
                                label = {
                                    Text(
                                        text = signal,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("checkin_signal_${signal.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 3. Energy, Rest & Hydration Card
            item {
                SanctuaryCard {
                    Text(
                        text = stringResource(R.string.checkin_vitals_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Energy Level
                    Spacer(modifier = Modifier.height(14.dp))
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
                                painter = painterResource(R.drawable.ic_health_calories),
                                contentDescription = "Energy level",
                                tint = sanaColors.activityOrange,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "Energy Level",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = CHECKIN_ENERGY_LABELS[energyLevel - 1],
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CHECKIN_ENERGY_LABELS.forEachIndexed { idx, label ->
                            val level = idx + 1
                            FilterChip(
                                selected = energyLevel == level,
                                onClick = { energyLevel = level },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("checkin_energy_$level")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = sanaColors.subtleDivider)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleep Duration
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
                                painter = painterResource(R.drawable.ic_health_sleep),
                                contentDescription = "Sleep duration",
                                tint = sanaColors.sleepIndigo,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "Sleep Last Night",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sleepHours = (sleepHours - 0.5).coerceAtLeast(0.0)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("sleep_minus_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease sleep")
                            }
                            Text(
                                text = String.format(Locale.US, "%.1f hrs", sleepHours),
                                style = MaterialTheme.typography.titleMedium.tabular(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sleepHours = (sleepHours + 0.5).coerceAtMost(24.0)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("sleep_plus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase sleep")
                            }
                        }
                    }
                    Slider(
                        value = sleepHours.toFloat().coerceIn(0f, 14f),
                        onValueChange = { raw ->
                            val snapped = (raw * 2f).roundToInt() / 2.0
                            sleepHours = snapped.coerceIn(0.0, 24.0)
                        },
                        valueRange = 0f..14f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.testTag("checkin_sleep_slider")
                    )
                    Text(
                        text = when {
                            sleepHours < 6.0 -> "Short rest — prioritize light pacing and hydration today."
                            sleepHours <= 9.0 -> "Restorative sleep range for steady daily recovery."
                            else -> "Extended rest — helpful when your body is recovering."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = sanaColors.subtleDivider)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Hydration
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
                                painter = painterResource(R.drawable.ic_health_hydration),
                                contentDescription = "Hydration glasses",
                                tint = sanaColors.medsTeal,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "Hydration",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hydrationGlasses = (hydrationGlasses - 1).coerceAtLeast(0)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("water_minus_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease hydration")
                            }
                            Text(
                                text = "$hydrationGlasses cups",
                                style = MaterialTheme.typography.titleMedium.tabular(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hydrationGlasses = (hydrationGlasses + 1).coerceAtMost(30)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("water_plus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase hydration")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (hydrationGlasses / 8f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hydrationGlasses >= 8) {
                            "Daily 8-cup hydration goal reached."
                        } else {
                            "${8 - hydrationGlasses} more cup(s) to reach an 8-cup daily goal."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Daily Health Notes Card
            item {
                SanctuaryCard {
                    Text(
                        text = stringResource(R.string.checkin_notes_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = reflectionNote,
                        onValueChange = { if (it.length <= maxReflectionChars) reflectionNote = it },
                        label = { Text("Anything else about how you feel today?") },
                        placeholder = { Text("Slept soundly, light morning stretch, mild stiffness after desk work...") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        supportingText = {
                            Text(
                                text = "$encodedPreviewLength / 600 chars",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkin_notes_input")
                    )
                }
            }

            // 5. Recent Wellness Rhythm Strip
            if (recentCheckIns.isNotEmpty()) {
                item {
                    SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Text(
                            text = "Recent Check-In Rhythm",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentCheckIns.take(4).forEach { entry ->
                                val entryMood = WellnessMoodOption.fromLevel(entry.moodLevel.toInt())
                                val entryParsed = WellnessNoteCodec.decode(entry.notes)
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = entryMood.icon,
                                                    contentDescription = entryMood.label,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (entry.dateKey == todayDateKey) {
                                                    "Today • ${entryParsed.generalStatus.label}"
                                                } else {
                                                    "${entry.dateKey} • ${entryParsed.generalStatus.label}"
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = buildString {
                                                    append(entryMood.label)
                                                    append(" • ")
                                                    append(String.format(Locale.US, "%.1fh sleep", entry.sleepHours))
                                                    append(" • ")
                                                    append(entry.parsedActivityLevel.shortLabel)
                                                    if (entry.activityMinutes > 0L) {
                                                        append(" (${entry.activityMinutes}m)")
                                                    }
                                                    append(" • ${entry.hydrationGlasses} cups")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Docked Frosted Glass Save Footer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SanaTheme.glass.floatingBarFill
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = SanaTheme.glass.borderMid)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val encodedNotes = WellnessNoteCodec.encode(
                                status = healthStatus,
                                signals = selectedSignals,
                                reflection = reflectionNote
                            )
                            onSaveCheckIn(
                                moodLevel,
                                energyLevel,
                                sleepHours,
                                hydrationGlasses,
                                activityLevel,
                                activityMinutes,
                                encodedNotes
                            )
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_checkin_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.checkin_saving_action),
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (existingToday != null) {
                                        R.string.checkin_update_action
                                    } else {
                                        R.string.checkin_save_action
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
