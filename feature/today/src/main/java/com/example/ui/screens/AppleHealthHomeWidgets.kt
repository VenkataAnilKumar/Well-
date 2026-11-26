package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.VoiceAndEmergencyHelper
import com.example.data.model.BloodType
import com.example.data.model.Consultation
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.VitalReading
import com.example.ui.theme.LocalSanaColors
import java.util.Locale

/**
 * Apple Health-inspired 2x2 Pinned Summary Metric Cards with category colors & mini sparklines.
 */
@Composable
fun AppleSummaryHighlightsGrid(
    todayDateKey: String,
    vitals: List<VitalReading>,
    medications: List<MedicationItem>,
    medicationLogs: List<MedicationLog>,
    checkIns: List<DailyCheckIn>,
    consultations: List<Consultation>,
    onOpenVitals: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenConsult: () -> Unit
) {
    val sanaColors = LocalSanaColors.current

    val latestVital = vitals.firstOrNull()
    val vitalSparkPoints = remember(vitals, latestVital) {
        if (latestVital == null) emptyList()
        else vitals
            .filter { it.parsedType == latestVital.parsedType }
            .take(7)
            .reversed()
            .map { it.primaryValue.toFloat() }
    }

    val activeMeds = remember(medications) { medications.filter { it.active } }
    val takenToday = remember(activeMeds, medicationLogs, todayDateKey) {
        val byMed = medicationLogs.filter { it.dateKey == todayDateKey }.associateBy { it.medicationId }
        activeMeds.count { byMed[it.id]?.parsedStatus == MedicationLogStatus.TAKEN }
    }

    val todayCheckIn = remember(checkIns, todayDateKey) {
        checkIns.firstOrNull { it.dateKey == todayDateKey }
    }
    val sleepBars = remember(checkIns) {
        checkIns.take(7).reversed().map { it.sleepHours.toFloat().coerceIn(1f, 12f) }
    }

    val latestConsult = consultations.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pinned",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "All Categories",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenVitals)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        // Row 1: Vitals (Heart Pink) + Medications (Meds Teal)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PinnedMetricCard(
                categoryLabel = "Vitals",
                icon = Icons.Filled.Favorite,
                accentColor = sanaColors.heartPink,
                primaryValue = latestVital?.let {
                    it.parsedType.formatValue(it.primaryValue, it.secondaryValue)
                } ?: "—",
                unitLabel = latestVital?.let {
                    it.unit.ifBlank { it.parsedType.defaultUnit }
                } ?: "No readings",
                caption = latestVital?.parsedType?.shortLabel ?: "Tap to log",
                onClick = onOpenVitals,
                testTag = "pinned_vitals_card",
                modifier = Modifier.weight(1f)
            ) {
                MiniLineSparkline(
                    points = vitalSparkPoints,
                    color = sanaColors.heartPink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }

            PinnedMetricCard(
                categoryLabel = "Medications",
                icon = Icons.Filled.Medication,
                accentColor = sanaColors.medsTeal,
                primaryValue = if (activeMeds.isEmpty()) "0" else "$takenToday/${activeMeds.size}",
                unitLabel = if (activeMeds.isEmpty()) "active" else "taken",
                caption = if (activeMeds.isEmpty()) "Add prescription" else "Daily schedule",
                onClick = onOpenMedications,
                testTag = "pinned_meds_card",
                modifier = Modifier.weight(1f)
            ) {
                MiniMedAdherenceDots(
                    total = activeMeds.size.coerceIn(1, 6),
                    completed = takenToday.coerceAtMost(6),
                    hasMeds = activeMeds.isNotEmpty(),
                    color = sanaColors.medsTeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }
        }

        // Row 2: Sleep & Mood (Indigo) + AI Health (System Blue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PinnedMetricCard(
                categoryLabel = "Sleep",
                icon = Icons.Filled.Bedtime,
                accentColor = sanaColors.sleepIndigo,
                primaryValue = todayCheckIn?.let {
                    String.format(Locale.US, "%.1f", it.sleepHours)
                } ?: checkIns.firstOrNull()?.let {
                    String.format(Locale.US, "%.1f", it.sleepHours)
                } ?: "—",
                unitLabel = if (checkIns.isNotEmpty()) "hr" else "No log",
                caption = if (todayCheckIn != null) "Logged today" else "Daily check-in",
                onClick = onOpenCheckIn,
                testTag = "pinned_sleep_card",
                modifier = Modifier.weight(1f)
            ) {
                MiniBarSparkline(
                    values = sleepBars,
                    maxValue = 10f,
                    color = sanaColors.sleepIndigo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }

            PinnedMetricCard(
                categoryLabel = "AI Triage",
                icon = Icons.Filled.AutoAwesome,
                accentColor = MaterialTheme.colorScheme.primary,
                primaryValue = if (latestConsult != null) {
                    "${consultations.size}"
                } else {
                    "Ready"
                },
                unitLabel = if (latestConsult != null) {
                    if (consultations.size == 1) "brief" else "briefs"
                } else {
                    ""
                },
                caption = latestConsult?.parsedUrgency?.label ?: "Start check",
                onClick = onOpenConsult,
                testTag = "pinned_ai_card",
                modifier = Modifier.weight(1f)
            ) {
                MiniPillStatusTrack(
                    filledFraction = if (latestConsult != null) 0.78f else 0.35f,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }
        }
    }
}

@Composable
private fun PinnedMetricCard(
    categoryLabel: String,
    icon: ImageVector,
    accentColor: Color,
    primaryValue: String,
    unitLabel: String,
    caption: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    sparkline: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.5.dp, LocalSanaColors.current.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open $categoryLabel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = primaryValue,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unitLabel.isNotBlank()) {
                        Text(
                            text = unitLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            sparkline()
        }
    }
}

@Composable
private fun MiniLineSparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        if (points.size < 2) {
            val midY = h * 0.55f
            drawLine(
                color = trackColor,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = color.copy(alpha = 0.45f),
                radius = 5f,
                center = Offset(w * 0.72f, midY)
            )
            return@Canvas
        }
        val minV = points.minOrNull() ?: 0f
        val maxV = points.maxOrNull() ?: 1f
        val span = (maxV - minV).coerceAtLeast(1f)
        val padY = 5f
        val usableH = (h - padY * 2f).coerceAtLeast(4f)
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val path = Path()
        points.forEachIndexed { idx, v ->
            val x = idx * stepX
            val normalized = (v - minV) / span
            val y = h - padY - (normalized * usableH)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val lastNorm = (points.last() - minV) / span
        val lastY = h - padY - (lastNorm * usableH)
        drawCircle(color = color, radius = 5.5f, center = Offset(w, lastY))
    }
}

@Composable
private fun MiniBarSparkline(
    values: List<Float>,
    maxValue: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val barCount = 7
        val gap = 6f
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(4f)
        val padded = List(barCount) { idx ->
            val offset = barCount - values.size
            if (idx >= offset && values.isNotEmpty()) values[idx - offset] else 0f
        }
        padded.forEachIndexed { idx, v ->
            val x = idx * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 2f),
                size = Size(barWidth, size.height - 4f),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            if (v > 0f) {
                val frac = (v / maxValue).coerceIn(0.18f, 1f)
                val barH = (size.height - 4f) * frac
                drawRoundRect(
                    color = if (idx == barCount - 1) color else color.copy(alpha = 0.55f),
                    topLeft = Offset(x, size.height - 2f - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
private fun MiniMedAdherenceDots(
    total: Int,
    completed: Int,
    hasMeds: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val count = total.coerceIn(1, 6)
        val gap = 6f
        val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(8f)
        val barH = 8f
        val y = (size.height - barH) / 2f
        repeat(count) { idx ->
            val x = idx * (barW + gap)
            val filled = hasMeds && idx < completed
            drawRoundRect(
                color = if (filled) color else trackColor,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f)
            )
        }
    }
}

@Composable
private fun MiniPillStatusTrack(
    filledFraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val barH = 8f
        val y = (size.height - barH) / 2f
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, y),
            size = Size(size.width, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f)
        )
        drawRoundRect(
            color = color.copy(alpha = 0.85f),
            topLeft = Offset(0f, y),
            size = Size(size.width * filledFraction.coerceIn(0.15f, 1f), barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f)
        )
    }
}

/**
 * Apple Health-style Medical ID Emergency Card shown on Summary.
 */
@Composable
fun MedicalIdEmergencyCard(
    profile: HealthProfile?,
    activeMedications: List<MedicationItem>,
    onClick: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val bloodTypeLabel = profile?.let { BloodType.fromCode(it.bloodType) }
        ?.takeIf { it != BloodType.UNKNOWN }
        ?.label
    val summaryParts = buildList {
        bloodTypeLabel?.let { add("Blood $it") }
        val allergyCount = profile?.allergies?.size ?: 0
        if (allergyCount > 0) add("$allergyCount allergies")
        val medCount = activeMedications.count { it.active }
        if (medCount > 0) add("$medCount active meds")
        if (profile?.hasEmergencyContact == true) add("ICE ready")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("home_medical_id_card"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.5.dp, sanaColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = sanaColors.heartPink.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = "Medical ID",
                    tint = sanaColors.heartPink,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Medical ID",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (summaryParts.isNotEmpty()) {
                        summaryParts.joinToString(" • ")
                    } else {
                        "Set blood type, allergies & emergency contact"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open Medical ID",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun MedicalIdDetailDialog(
    displayName: String,
    profile: HealthProfile?,
    activeMedications: List<MedicationItem>,
    healthRegion: com.example.core.HealthRegionProfile = com.example.core.HealthRegionDirectory.US,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val personName = profile?.displayName?.takeIf { it.isNotBlank() }
        ?: displayName.ifBlank { "Well+ Member" }
    val bloodType = profile?.let { BloodType.fromCode(it.bloodType) }
        ?.takeIf { it != BloodType.UNKNOWN }
        ?.label ?: "Not set"
    val activeMedNames = activeMedications
        .filter { it.active }
        .map { if (it.dosage.isNotBlank()) "${it.name} (${it.dosage})" else it.name }
    val combinedMeds = (activeMedNames + profile?.medications.orEmpty())
        .distinctBy { it.lowercase(Locale.US) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(0.5.dp, sanaColors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            color = sanaColors.heartPink,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalHospital,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MEDICAL ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = sanaColors.heartPink
                            )
                            Text(
                                text = personName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("medical_id_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Medical ID"
                        )
                    }
                }

                // Core Medical Details Grouped Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, sanaColors.cardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MedicalIdFieldRow(
                            label = "Age & Blood Type",
                            value = buildString {
                                val ageVal = profile?.age ?: 0L
                                if (ageVal > 0L) append("$ageVal yrs") else append("Age not set")
                                append("  •  Blood: $bloodType")
                            }
                        )
                        HorizontalDivider(
                            color = sanaColors.subtleDivider,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MedicalIdFieldRow(
                            label = "Allergies & Reactions",
                            value = profile?.allergies?.takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") ?: "None listed"
                        )
                        HorizontalDivider(
                            color = sanaColors.subtleDivider,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MedicalIdFieldRow(
                            label = "Medical Conditions",
                            value = profile?.conditions?.takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") ?: "None listed"
                        )
                        HorizontalDivider(
                            color = sanaColors.subtleDivider,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MedicalIdFieldRow(
                            label = "Current Medications",
                            value = combinedMeds.takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") ?: "None listed"
                        )
                    }
                }

                // Region-Adaptive Coverage & Care Team Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MedicalIdFieldRow(
                            label = healthRegion.coveragePrimaryLabel,
                            value = buildString {
                                val provider = profile?.coverageProvider?.trim().orEmpty()
                                val memberId = profile?.coverageMemberId?.trim().orEmpty()
                                append(provider.ifBlank { "Not set" })
                                if (memberId.isNotBlank()) append("  •  ID: $memberId")
                            }
                        )
                        HorizontalDivider(
                            color = sanaColors.subtleDivider,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MedicalIdFieldRow(
                            label = healthRegion.primaryClinicianLabel,
                            value = profile?.primaryDoctorName?.takeIf { it.isNotBlank() } ?: "Not set"
                        )
                        HorizontalDivider(
                            color = sanaColors.subtleDivider,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MedicalIdFieldRow(
                            label = healthRegion.pharmacyLabel,
                            value = profile?.preferredPharmacy?.takeIf { it.isNotBlank() } ?: "Not set"
                        )
                    }
                }

                // Emergency Call Actions
                if (profile?.hasEmergencyContact == true) {
                    Button(
                        onClick = {
                            VoiceAndEmergencyHelper.openDialer(context, profile.emergencyContactPhone)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sanaColors.heartPink,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("medical_id_call_contact_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val label = profile.emergencyContactName.ifBlank { profile.emergencyContactPhone }
                        Text("Call Emergency Contact ($label)")
                    }
                }

                val nurseNumber = healthRegion.nurseAdviceNumber
                if (healthRegion.isCanada && !nurseNumber.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            VoiceAndEmergencyHelper.openDialer(context, nurseNumber)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sanaColors.medsTeal.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = sanaColors.medsTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("medical_id_call_811_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call 811 — Free 24/7 Nurse Line")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { VoiceAndEmergencyHelper.openDialer(context, healthRegion.emergencyNumber) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sanaColors.heartPink.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = sanaColors.heartPink),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("medical_id_call_911_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dial ${healthRegion.emergencyNumber}")
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEditProfile()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("medical_id_edit_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Info")
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalIdFieldRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
