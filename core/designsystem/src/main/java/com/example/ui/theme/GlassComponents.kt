package com.example.ui.theme

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import java.util.Locale
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Controls blur vs. lightweight frosted scrim rendering so scrolling LazyColumns
 * stay locked at 60/120fps on every Android version (API 24–36).
 */
enum class GlassRenderMode {
    /**
     * High-performance translucent frosted glass without real-time RenderEffect.
     * Required for LazyColumn items and devices below API 31.
     */
    SCROLL_SAFE_FROSTED,

    /**
     * Applies native API 31+ RenderEffect blur to an isolated ambient backing layer
     * behind crisp foreground content (for floating nav bars, static heroes, modals, and sheets)
     * on hardware-accelerated GPUs. Automatically falls back to high-opacity frosted scrim on
     * API 24–30 and software/virtualized GPU environments to prevent SurfaceSyncGroup timeouts.
     */
    REALTIME_BLUR_SURFACE
}

private val isVirtualOrSoftwareGpu: Boolean by lazy {
    val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
    val model = Build.MODEL.lowercase(Locale.US)
    val product = Build.PRODUCT.lowercase(Locale.US)
    val hardware = Build.HARDWARE.lowercase(Locale.US)
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
    fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        fingerprint.contains("emulator") ||
        fingerprint.contains("cuttlefish") ||
        fingerprint.contains("vsoc") ||
        model.contains("emulator") ||
        model.contains("google_sdk") ||
        model.contains("cuttlefish") ||
        manufacturer.contains("genymotion") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        hardware.contains("cutf") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        product.contains("cuttlefish") ||
        product.contains("vsoc")
}

/**
 * Returns true when hardware-accelerated Android 12+ (API 31) RenderEffect blur is supported
 * without stalling RenderThread on software/virtualized GPU pipelines.
 */
fun supportsRealtimeGlassBlur(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isVirtualOrSoftwareGpu

/**
 * Applies a cached API 31+ RenderEffect blur to a decorative background layer only on
 * hardware-accelerated GPUs. Never applies directly to text/icon foreground content.
 */
fun Modifier.glassBackdropBlur(radiusPx: Float = 16f): Modifier =
    if (supportsRealtimeGlassBlur() && radiusPx > 0f) {
        composed {
            val cachedEffect = remember(radiusPx) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AndroidRenderEffect
                        .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                } else {
                    null
                }
            }
            if (cachedEffect != null) {
                Modifier.graphicsLayer { renderEffect = cachedEffect }
            } else {
                Modifier
            }
        }
    } else {
        this
    }

/**
 * Serene, GPU-lightweight ambient radial mesh backdrop for Frosted Daylight (Light)
 * and Deep Twilight Slate (Dark) themes. Uses drawWithCache so radial gradient shaders
 * are allocated once per layout size rather than on every frame.
 */
@Composable
fun GlassAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = SanaTheme.glass
    val isDark = SanaTheme.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glass.backdropBase)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val minDim = size.minDimension.coerceAtLeast(1f)

                val mintCenter = Offset(w * 0.14f, h * 0.08f)
                val mintRadius = minDim * 0.85f
                val mintBrush = Brush.radialGradient(
                    colors = listOf(
                        glass.orbMint.copy(alpha = glass.orbAlpha),
                        glass.orbMint.copy(alpha = 0f)
                    ),
                    center = mintCenter,
                    radius = mintRadius
                )

                val indigoCenter = Offset(w * 0.92f, h * 0.34f)
                val indigoRadius = minDim * 0.78f
                val indigoBrush = Brush.radialGradient(
                    colors = listOf(
                        glass.orbIndigo.copy(alpha = glass.orbAlpha * if (isDark) 0.95f else 0.72f),
                        glass.orbIndigo.copy(alpha = 0f)
                    ),
                    center = indigoCenter,
                    radius = indigoRadius
                )

                val coralCenter = Offset(w * 0.12f, h * 0.78f)
                val coralRadius = minDim * 0.72f
                val coralBrush = Brush.radialGradient(
                    colors = listOf(
                        glass.orbCoral.copy(alpha = glass.orbAlpha * if (isDark) 0.55f else 0.45f),
                        glass.orbCoral.copy(alpha = 0f)
                    ),
                    center = coralCenter,
                    radius = coralRadius
                )

                onDrawBehind {
                    drawCircle(brush = mintBrush, radius = mintRadius, center = mintCenter)
                    drawCircle(brush = indigoBrush, radius = indigoRadius, center = indigoCenter)
                    drawCircle(brush = coralBrush, radius = coralRadius, center = coralCenter)
                }
            },
        content = content
    )
}

/**
 * Core reusable Glassmorphism card component for Well+.
 * Features:
 * - Semi-transparent tinted glass fill (White @ 0.08f in Dark / White @ 0.72f in Light)
 * - Rounded 22.dp corners (configurable 20.dp–24.dp)
 * - 1.dp light-refraction diagonal hairline border (White 0.30 -> 0.05 in Dark; luminous frosted hairline in Light)
 * - Optional semantic accent tint for health category cards
 * - Performance guardrail: defaults to SCROLL_SAFE_FROSTED for smooth 60/120fps LazyColumn scrolling,
 *   with optional REALTIME_BLUR_SURFACE for static hero surfaces on API 31+.
 */
@Composable
fun GlassHealthCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SanaTheme.glass.cardCornerRadius,
    containerColor: Color? = null,
    accentTint: Color? = null,
    borderColor: Color? = null,
    renderMode: GlassRenderMode = GlassRenderMode.SCROLL_SAFE_FROSTED,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = SanaTheme.glass
    val isDark = SanaTheme.isDark
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val baseFill = containerColor ?: when {
        renderMode == GlassRenderMode.REALTIME_BLUR_SURFACE && !supportsRealtimeGlassBlur() ->
            glass.cardFillElevated
        else -> glass.cardFill
    }

    val refractionBorderBrush = remember(
        borderColor,
        glass.borderStart,
        glass.borderMid,
        glass.borderEnd
    ) {
        if (borderColor != null) {
            Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = (borderColor.alpha * 1.15f).coerceAtMost(1f)),
                    borderColor.copy(alpha = (borderColor.alpha * 0.45f).coerceAtLeast(0.08f))
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    glass.borderStart,
                    glass.borderMid,
                    glass.borderEnd
                )
            )
        }
    }

    val surfaceWashBrush = remember(baseFill, accentTint, isDark, glass.topSpecularHighlight) {
        val topTint = if (accentTint != null) {
            accentTint.copy(alpha = if (isDark) 0.14f else 0.08f)
        } else {
            glass.topSpecularHighlight.copy(alpha = if (isDark) 0.06f else 0.22f)
        }
        val bottomTint = if (accentTint != null) {
            accentTint.copy(alpha = if (isDark) 0.04f else 0.02f)
        } else {
            Color.Transparent
        }
        Brush.verticalGradient(colors = listOf(topTint, bottomTint))
    }

    val interactiveModifier = if (onClick != null) {
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(baseFill, shape)
            .then(
                if (renderMode == GlassRenderMode.REALTIME_BLUR_SURFACE && supportsRealtimeGlassBlur()) {
                    Modifier
                } else {
                    Modifier.background(surfaceWashBrush, shape)
                }
            )
            .border(width = glass.borderWidth, brush = refractionBorderBrush, shape = shape)
            .then(interactiveModifier)
    ) {
        // On physical API 31+ hardware GPUs, blur an isolated ambient shimmer layer behind crisp content
        if (renderMode == GlassRenderMode.REALTIME_BLUR_SURFACE && supportsRealtimeGlassBlur()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .glassBackdropBlur(radiusPx = 18f)
                    .background(surfaceWashBrush)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Floating frosted glass dock container for bottom navigation bars, action bars, and sticky headers.
 * Uses API 31+ RenderEffect on an isolated frosted background layer and a high-opacity legible
 * scrim fallback on API 24–30.
 */
@Composable
fun GlassFloatingBarContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SanaTheme.glass.floatingCornerRadius),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = SanaTheme.glass
    val fill = if (supportsRealtimeGlassBlur()) {
        glass.floatingBarFill
    } else {
        glass.floatingBarFallbackFill
    }
    val borderBrush = remember(glass.borderStart, glass.borderMid, glass.borderEnd) {
        Brush.linearGradient(
            colors = listOf(glass.borderStart, glass.borderMid, glass.borderEnd)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(width = glass.borderWidth, brush = borderBrush, shape = shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .glassBackdropBlur(radiusPx = 16f)
                .background(fill)
        )
        content()
    }
}

/**
 * Frosted glass container for modals, alerts, and bottom sheet chrome with guaranteed WCAG AA contrast.
 */
@Composable
fun GlassModalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SanaTheme.glass.floatingCornerRadius),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = SanaTheme.glass
    val borderBrush = remember(glass.borderStart, glass.borderMid, glass.borderEnd) {
        Brush.linearGradient(
            colors = listOf(glass.borderStart, glass.borderMid, glass.borderEnd)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(width = glass.borderWidth, brush = borderBrush, shape = shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .glassBackdropBlur(radiusPx = 16f)
                .background(glass.modalScrimFill)
        )
        content()
    }
}

/**
 * Translucent semantic status pill for vitals & daily goals (e.g., Optimal, Elevated, Goal Reached)
 * with WCAG AA legible foreground contrast on both Frosted Daylight and Deep Twilight Slate surfaces.
 */
@Composable
fun GlassStatusPill(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val isDark = SanaTheme.isDark
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = if (isDark) 0.20f else 0.13f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.42f else 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            leadingContent?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

/**
 * Scannable vital/health metric value with tabular numerals ("tnum") and baseline-aligned unit label.
 */
@Composable
fun GlassMetricValueRow(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = value,
            style = valueStyle.tabular(),
            fontWeight = FontWeight.Bold,
            color = valueColor,
            modifier = Modifier.alignByBaseline()
        )
        if (unit.isNotBlank()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = unitColor,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

/**
 * Calm, low-contrast frosted shimmer bar for smooth health data loading skeletons.
 * Animates via RenderNode alpha in graphicsLayer with zero per-frame shader allocations.
 */
@Composable
fun GlassShimmerBar(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp
) {
    val glass = SanaTheme.glass
    val transition = rememberInfiniteTransition(label = "glass_shimmer")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glass_shimmer_alpha"
    )
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Box(
        modifier = modifier
            .graphicsLayer { alpha = pulseAlpha }
            .background(color = glass.shimmerBase, shape = shape)
    )
}

/**
 * Reusable calm frosted glass shimmer card used during Firestore / Health Connect loading states.
 */
@Composable
fun GlassShimmerCard(
    modifier: Modifier = Modifier,
    lines: Int = 2
) {
    GlassHealthCard(
        modifier = modifier,
        renderMode = GlassRenderMode.SCROLL_SAFE_FROSTED
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassShimmerBar(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp),
                cornerRadius = 12.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassShimmerBar(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(14.dp),
                    cornerRadius = 7.dp
                )
                GlassShimmerBar(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(11.dp),
                    cornerRadius = 6.dp
                )
            }
        }
        if (lines > 2) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassShimmerBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                cornerRadius = 14.dp
            )
        }
    }
}

