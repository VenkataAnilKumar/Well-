package com.example.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class SanctuaryThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Frosted Light"),
    DARK("dark", "Twilight Dark");

    companion object {
        fun fromKey(key: String?): SanctuaryThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

object SanctuaryThemePrefs {
    private const val PREFS_NAME = "sana_care_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun load(context: Context): SanctuaryThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SanctuaryThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null))
    }

    fun save(context: Context, mode: SanctuaryThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
    }
}

val LightSanctuaryScheme = lightColorScheme(
    primary = SanctuaryTeal,
    onPrimary = SanctuarySurface,
    primaryContainer = SanctuarySeafoam,
    onPrimaryContainer = SanctuaryTealDark,
    inversePrimary = SanctuaryInversePrimary,
    secondary = BotanicalSage,
    onSecondary = SanctuarySurface,
    secondaryContainer = BotanicalSageContainer,
    onSecondaryContainer = BotanicalSageOnContainer,
    tertiary = TerracottaClay,
    onTertiary = SanctuarySurface,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = TerracottaOnContainer,
    background = SanctuaryCream,
    onBackground = InkCharcoal,
    surface = SanctuarySurface,
    onSurface = InkCharcoal,
    surfaceVariant = SanctuaryLinen,
    onSurfaceVariant = InkSlate,
    surfaceTint = SanctuaryTeal,
    inverseSurface = SanctuaryInverseSurface,
    inverseOnSurface = SanctuaryInverseOnSurface,
    surfaceBright = SanctuarySurface,
    surfaceDim = SanctuarySand,
    surfaceContainerLowest = SanctuarySurface,
    surfaceContainerLow = SanctuarySurfaceLow,
    surfaceContainer = SanctuaryCreamWarm,
    surfaceContainerHigh = SanctuaryLinen,
    surfaceContainerHighest = SanctuarySand,
    outline = SanctuaryOutline,
    outlineVariant = SanctuaryOutlineSoft,
    scrim = SanctuaryScrim,
    error = UrgencyEmergency,
    onError = SanctuarySurface,
    errorContainer = UrgencyEmergencyBg,
    onErrorContainer = UrgencyEmergency
)

val DarkSanctuaryScheme = darkColorScheme(
    primary = DarkSeafoam,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = DarkSeafoamContainer,
    onPrimaryContainer = DarkSeafoamOnContainer,
    inversePrimary = SanctuaryTeal,
    secondary = DarkSage,
    onSecondary = DarkEvergreen,
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = DarkSageOnContainer,
    tertiary = DarkTerracotta,
    onTertiary = Color(0xFF4C0519),
    tertiaryContainer = DarkTerracottaContainer,
    onTertiaryContainer = DarkTerracottaOnContainer,
    background = DarkEvergreen,
    onBackground = DarkParchment,
    surface = DarkEvergreenSurface,
    onSurface = DarkParchment,
    surfaceVariant = DarkEvergreenElevated,
    onSurfaceVariant = DarkMutedText,
    surfaceTint = DarkSeafoam,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    surfaceBright = DarkEvergreenHighest,
    surfaceDim = DarkEvergreenLowest,
    surfaceContainerLowest = DarkEvergreenLowest,
    surfaceContainerLow = DarkEvergreenSurface,
    surfaceContainer = DarkEvergreenElevated,
    surfaceContainerHigh = DarkEvergreenHigh,
    surfaceContainerHighest = DarkEvergreenHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSoft,
    scrim = SanctuaryScrim,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = UrgencyEmergencyDarkBg,
    onErrorContainer = DarkOnErrorContainer
)

@Immutable
data class SanaSemanticColors(
    val selfCareFg: Color,
    val selfCareBg: Color,
    val doctorSoonFg: Color,
    val doctorSoonBg: Color,
    val urgentFg: Color,
    val urgentBg: Color,
    val emergencyFg: Color,
    val emergencyBg: Color,
    val cardBorder: Color,
    val warmGlow: Color,
    val sanctuaryMist: Color,
    val warmAccent: Color,
    val subtleDivider: Color,
    val heartPink: Color,
    val medsTeal: Color,
    val sleepIndigo: Color,
    val activityOrange: Color,
    val healthyGreen: Color
)

/**
 * Centralized Glassmorphism visual tokens shared across cards, floating bars,
 * modals, bottom sheets, and ambient backdrops for both Light and Dark themes.
 */
@Immutable
data class GlassTokens(
    val cardFill: Color,
    val cardFillElevated: Color,
    val floatingBarFill: Color,
    val floatingBarFallbackFill: Color,
    val modalScrimFill: Color,
    val borderStart: Color,
    val borderMid: Color,
    val borderEnd: Color,
    val topSpecularHighlight: Color,
    val backdropBase: Color,
    val orbMint: Color,
    val orbIndigo: Color,
    val orbCoral: Color,
    val orbAlpha: Float,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val cardCornerRadius: Dp = 22.dp,
    val floatingCornerRadius: Dp = 24.dp,
    val borderWidth: Dp = 1.dp
)

private val LightSemanticColors = SanaSemanticColors(
    selfCareFg = UrgencySelfCare,
    selfCareBg = UrgencySelfCareBg,
    doctorSoonFg = UrgencyDoctorSoon,
    doctorSoonBg = UrgencyDoctorSoonBg,
    urgentFg = UrgencyUrgent,
    urgentBg = UrgencyUrgentBg,
    emergencyFg = UrgencyEmergency,
    emergencyBg = UrgencyEmergencyBg,
    cardBorder = SanctuaryOutlineSoft,
    warmGlow = SanctuarySeafoamSoft,
    sanctuaryMist = SanctuaryCreamWarm,
    warmAccent = TerracottaClay,
    subtleDivider = Color(0x1F0F172A),
    heartPink = HealthSoftCoralLight,
    medsTeal = HealthMintTealLight,
    sleepIndigo = HealthCalmIndigoLight,
    activityOrange = HealthWarmAmberLight,
    healthyGreen = HealthOptimalEmeraldLight
)

private val DarkSemanticColors = SanaSemanticColors(
    selfCareFg = UrgencySelfCareDarkFg,
    selfCareBg = UrgencySelfCareDarkBg,
    doctorSoonFg = UrgencyDoctorSoonDarkFg,
    doctorSoonBg = UrgencyDoctorSoonDarkBg,
    urgentFg = UrgencyUrgentDarkFg,
    urgentBg = UrgencyUrgentDarkBg,
    emergencyFg = UrgencyEmergencyDarkFg,
    emergencyBg = UrgencyEmergencyDarkBg,
    cardBorder = Color(0x26FFFFFF),
    warmGlow = DarkWarmGlow,
    sanctuaryMist = DarkEvergreenElevated,
    warmAccent = DarkTerracotta,
    subtleDivider = Color(0x1FFFFFFF),
    heartPink = HealthSoftCoralDark,
    medsTeal = HealthMintTealDark,
    sleepIndigo = HealthCalmIndigoDark,
    activityOrange = HealthWarmAmberDark,
    healthyGreen = HealthOptimalEmeraldDark
)

private val LightGlassTokens = GlassTokens(
    cardFill = Color.White.copy(alpha = 0.72f),
    cardFillElevated = Color.White.copy(alpha = 0.84f),
    floatingBarFill = Color.White.copy(alpha = 0.78f),
    floatingBarFallbackFill = Color.White.copy(alpha = 0.92f),
    modalScrimFill = Color(0xFFF6FAFC).copy(alpha = 0.92f),
    borderStart = Color.White.copy(alpha = 0.92f),
    borderMid = Color.White.copy(alpha = 0.45f),
    borderEnd = Color(0xFF0F172A).copy(alpha = 0.08f),
    topSpecularHighlight = Color.White.copy(alpha = 0.65f),
    backdropBase = GlassLightBackdropBase,
    orbMint = GlassLightOrbMint,
    orbIndigo = GlassLightOrbIndigo,
    orbCoral = GlassLightOrbCoral,
    orbAlpha = 0.20f,
    shimmerBase = Color(0xFFE2ECF2),
    shimmerHighlight = Color(0xFFF8FBFD)
)

private val DarkGlassTokens = GlassTokens(
    cardFill = Color.White.copy(alpha = 0.08f),
    cardFillElevated = Color(0xFF16223A).copy(alpha = 0.76f),
    floatingBarFill = Color(0xFF111B30).copy(alpha = 0.74f),
    floatingBarFallbackFill = Color(0xFF131E34).copy(alpha = 0.92f),
    modalScrimFill = Color(0xFF111B30).copy(alpha = 0.92f),
    borderStart = Color.White.copy(alpha = 0.30f),
    borderMid = Color.White.copy(alpha = 0.14f),
    borderEnd = Color.White.copy(alpha = 0.05f),
    topSpecularHighlight = Color.White.copy(alpha = 0.14f),
    backdropBase = GlassDarkBackdropBase,
    orbMint = GlassDarkOrbMint,
    orbIndigo = GlassDarkOrbIndigo,
    orbCoral = GlassDarkOrbCoral,
    orbAlpha = 0.18f,
    shimmerBase = Color(0xFF18243C),
    shimmerHighlight = Color(0xFF263859)
)

val LocalSanaColors = staticCompositionLocalOf { LightSemanticColors }
val LocalGlassTokens = staticCompositionLocalOf { LightGlassTokens }
val LocalSanctuaryDarkTheme = staticCompositionLocalOf { false }

val SanaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

object SanaTheme {
    val colors: SanaSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSanaColors.current

    val glass: GlassTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassTokens.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalSanctuaryDarkTheme.current
}

@Composable
fun SanaCareTheme(
    themeMode: SanctuaryThemeMode = SanctuaryThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        SanctuaryThemeMode.SYSTEM -> isSystemInDarkTheme()
        SanctuaryThemeMode.LIGHT -> false
        SanctuaryThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkSanctuaryScheme else LightSanctuaryScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val glassTokens = if (darkTheme) DarkGlassTokens else LightGlassTokens

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.LaunchedEffect(view, darkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSanaColors provides semanticColors,
        LocalGlassTokens provides glassTokens,
        LocalSanctuaryDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SanaShapes,
            content = content
        )
    }
}
