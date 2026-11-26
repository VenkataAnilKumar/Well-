package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.VitalType
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme

/**
 * Maps each core clinical vital type to its matching uniform-stroke custom vector drawable.
 */
@DrawableRes
fun VitalType.healthIconRes(): Int = when (this) {
    VitalType.BLOOD_PRESSURE -> R.drawable.ic_health_blood_pressure
    VitalType.HEART_RATE -> R.drawable.ic_health_heart_rate
    VitalType.TEMPERATURE -> R.drawable.ic_health_temperature
    VitalType.SPO2 -> R.drawable.ic_health_spo2
    VitalType.BLOOD_GLUCOSE -> R.drawable.ic_health_glucose
    VitalType.WEIGHT -> R.drawable.ic_health_calories
}

/**
 * Semantic categories for lightweight, resolution-independent frosted-glass empty state art.
 */
enum class HealthEmptyIllustrationKind {
    VITALS,
    MEDICATIONS,
    HISTORY,
    CHECK_IN,
    MENTAL_WELLBEING,
    BODY_CYCLE,
    HEALTH_CONNECT
}

/**
 * Lightweight, resolution-independent vector illustration for empty states across Well+.
 * Uses translucent frosted layers, specular glass arcs, and semantic health accents
 * (Mint/Teal, Soft Coral, Calm Indigo) with zero bitmap memory overhead.
 */
@Composable
fun GlassEmptyStateIllustration(
    kind: HealthEmptyIllustrationKind,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp
) {
    val sanaColors = LocalSanaColors.current
    val glass = SanaTheme.glass
    val isDark = SanaTheme.isDark

    val (primaryAccent, secondaryAccent, centerIconRes, semanticDesc) = when (kind) {
        HealthEmptyIllustrationKind.VITALS -> Quadruple(
            sanaColors.heartPink,
            sanaColors.medsTeal,
            R.drawable.ic_health_heart_rate,
            "No vitals logged illustration"
        )
        HealthEmptyIllustrationKind.MEDICATIONS -> Quadruple(
            sanaColors.medsTeal,
            sanaColors.sleepIndigo,
            R.drawable.ic_health_medication,
            "Medication schedule illustration"
        )
        HealthEmptyIllustrationKind.HISTORY -> Quadruple(
            sanaColors.sleepIndigo,
            sanaColors.medsTeal,
            R.drawable.ic_health_checkin,
            "Health history illustration"
        )
        HealthEmptyIllustrationKind.CHECK_IN -> Quadruple(
            sanaColors.medsTeal,
            sanaColors.heartPink,
            R.drawable.ic_health_checkin,
            "Daily check-in illustration"
        )
        HealthEmptyIllustrationKind.MENTAL_WELLBEING -> Quadruple(
            sanaColors.sleepIndigo,
            sanaColors.medsTeal,
            R.drawable.ic_health_sleep,
            "Mindfulness and mood illustration"
        )
        HealthEmptyIllustrationKind.BODY_CYCLE -> Quadruple(
            sanaColors.heartPink,
            sanaColors.sleepIndigo,
            R.drawable.ic_health_calories,
            "Body metrics illustration"
        )
        HealthEmptyIllustrationKind.HEALTH_CONNECT -> Quadruple(
            sanaColors.medsTeal,
            sanaColors.heartPink,
            R.drawable.ic_health_steps,
            "Health Connect sync illustration"
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = semanticDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.minDimension / 2f

            // Soft translucent radial aura behind glass disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = if (isDark) 0.30f else 0.22f),
                        secondaryAccent.copy(alpha = if (isDark) 0.12f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // Secondary offset floating frosted pane
            val backSize = this.size.minDimension * 0.56f
            drawRoundRect(
                color = secondaryAccent.copy(alpha = if (isDark) 0.16f else 0.12f),
                topLeft = Offset(cx - backSize * 0.30f, cy - backSize * 0.64f),
                size = Size(backSize, backSize),
                cornerRadius = CornerRadius(backSize * 0.32f, backSize * 0.32f)
            )

            // Primary centered frosted glass medallion
            val cardDim = this.size.minDimension * 0.62f
            val cardTopLeft = Offset(cx - cardDim / 2f, cy - cardDim / 2f)
            drawRoundRect(
                color = if (isDark) Color.White.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.80f),
                topLeft = cardTopLeft,
                size = Size(cardDim, cardDim),
                cornerRadius = CornerRadius(cardDim * 0.32f, cardDim * 0.32f)
            )

            // Diagonal glass light-refraction hairline border
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(glass.borderStart, glass.borderMid, glass.borderEnd),
                    start = cardTopLeft,
                    end = Offset(cardTopLeft.x + cardDim, cardTopLeft.y + cardDim)
                ),
                topLeft = cardTopLeft,
                size = Size(cardDim, cardDim),
                cornerRadius = CornerRadius(cardDim * 0.32f, cardDim * 0.32f),
                style = Stroke(width = 1.25.dp.toPx())
            )

            // Subtle floating decorative refraction orb dots
            drawCircle(
                color = primaryAccent.copy(alpha = if (isDark) 0.72f else 0.55f),
                radius = 3.5.dp.toPx(),
                center = Offset(cx + cardDim * 0.52f, cy - cardDim * 0.42f)
            )
            drawCircle(
                color = secondaryAccent.copy(alpha = if (isDark) 0.65f else 0.50f),
                radius = 2.5.dp.toPx(),
                center = Offset(cx - cardDim * 0.56f, cy + cardDim * 0.36f)
            )
        }

        Icon(
            painter = painterResource(centerIconRes),
            contentDescription = null,
            tint = primaryAccent,
            modifier = Modifier.size(size * 0.34f)
        )
    }
}

/**
 * Translucent glass celebration banner artwork shown when all daily wellness goals are reached.
 */
@Composable
fun GlassGoalCelebrationBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val glass = SanaTheme.glass
    val isDark = SanaTheme.isDark
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        sanaColors.medsTeal.copy(alpha = if (isDark) 0.22f else 0.14f),
                        sanaColors.sleepIndigo.copy(alpha = if (isDark) 0.16f else 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        sanaColors.medsTeal.copy(alpha = if (isDark) 0.50f else 0.38f),
                        glass.borderEnd
                    )
                ),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = "Daily wellness goal reached celebration" }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f

            // Outer translucent aurora ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        sanaColors.medsTeal,
                        sanaColors.sleepIndigo,
                        sanaColors.heartPink,
                        sanaColors.medsTeal
                    )
                ),
                radius = r - 2.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Frosted inner crest
            drawCircle(
                color = sanaColors.medsTeal.copy(alpha = if (isDark) 0.24f else 0.16f),
                radius = r - 5.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Crisp rounded checkmark
            val checkPath = Path().apply {
                moveTo(cx - r * 0.34f, cy + r * 0.02f)
                lineTo(cx - r * 0.08f, cy + r * 0.28f)
                lineTo(cx + r * 0.36f, cy - r * 0.24f)
            }
            drawPath(
                path = checkPath,
                color = sanaColors.medsTeal,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Lightweight, resolution-independent frosted glass vector header illustration for Onboarding steps.
 * Step 0: North America Region & Clinical Safety Shield
 * Step 1: Personal Health Record & Care Team Coverage
 * Step 2: Serene Daily Rhythm, Reminders & Biometric Privacy
 */
@Composable
fun GlassOnboardingStepIllustration(
    step: Int,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val glass = SanaTheme.glass
    val isDark = SanaTheme.isDark

    val (accentA, accentB, iconRes, label) = when (step) {
        0 -> Quadruple(
            sanaColors.medsTeal,
            sanaColors.sleepIndigo,
            R.drawable.ic_health_heart_rate,
            "Region and clinical safety illustration"
        )
        1 -> Quadruple(
            sanaColors.heartPink,
            sanaColors.medsTeal,
            R.drawable.ic_health_medication,
            "Health profile and coverage illustration"
        )
        else -> Quadruple(
            sanaColors.sleepIndigo,
            sanaColors.medsTeal,
            R.drawable.ic_health_checkin,
            "Reminders and privacy illustration"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Ambient soft dual-tone aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentA.copy(alpha = if (isDark) 0.28f else 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(cx - 38.dp.toPx(), cy),
                    radius = 64.dp.toPx()
                ),
                radius = 64.dp.toPx(),
                center = Offset(cx - 38.dp.toPx(), cy)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentB.copy(alpha = if (isDark) 0.24f else 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(cx + 42.dp.toPx(), cy + 6.dp.toPx()),
                    radius = 58.dp.toPx()
                ),
                radius = 58.dp.toPx(),
                center = Offset(cx + 42.dp.toPx(), cy + 6.dp.toPx())
            )

            // Subtle horizon ECG/wave line passing behind the frosted medallion
            val wavePath = Path().apply {
                val left = cx - 108.dp.toPx()
                val right = cx + 108.dp.toPx()
                moveTo(left, cy)
                lineTo(cx - 56.dp.toPx(), cy)
                lineTo(cx - 46.dp.toPx(), cy - 11.dp.toPx())
                lineTo(cx - 36.dp.toPx(), cy + 9.dp.toPx())
                lineTo(cx - 26.dp.toPx(), cy)
                lineTo(cx + 26.dp.toPx(), cy)
                lineTo(cx + 38.dp.toPx(), cy - 8.dp.toPx())
                lineTo(cx + 50.dp.toPx(), cy + 6.dp.toPx())
                lineTo(cx + 60.dp.toPx(), cy)
                lineTo(right, cy)
            }
            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentA.copy(alpha = if (isDark) 0.48f else 0.36f),
                        accentB.copy(alpha = if (isDark) 0.48f else 0.36f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Center frosted glass squircle
            val boxDim = 64.dp.toPx()
            val topLeft = Offset(cx - boxDim / 2f, cy - boxDim / 2f)
            drawRoundRect(
                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.84f),
                topLeft = topLeft,
                size = Size(boxDim, boxDim),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(glass.borderStart, glass.borderMid, glass.borderEnd),
                    start = topLeft,
                    end = Offset(topLeft.x + boxDim, topLeft.y + boxDim)
                ),
                topLeft = topLeft,
                size = Size(boxDim, boxDim),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 1.25.dp.toPx())
            )
        }

        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accentA,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * Translucent frosted-glass profile avatar with layered health gradient,
 * specular hairline rim, and guaranteed 48.dp touch target when interactive.
 */
@Composable
fun GlassProfileAvatar(
    initials: String,
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
    visualSize: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val sanaColors = LocalSanaColors.current
    val glass = SanaTheme.glass
    val isDark = SanaTheme.isDark

    val containerMod = if (onClick != null) {
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescriptionText,
                onClick = onClick
            )
    } else {
        modifier
    }

    Box(
        modifier = containerMod.semantics { contentDescription = contentDescriptionText },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            sanaColors.medsTeal.copy(alpha = if (isDark) 0.34f else 0.24f),
                            sanaColors.sleepIndigo.copy(alpha = if (isDark) 0.36f else 0.24f)
                        )
                    )
                )
                .border(
                    width = 1.25.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(glass.borderStart, glass.borderMid, glass.borderEnd)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.take(2).uppercase(),
                style = if (visualSize >= 56.dp) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
