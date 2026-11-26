package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Light Well+ Glassmorphism Palette — Soft Frosted Daylight
// Airy daylight mist canvas (#EEF4F7) + frosted white glass cards (White @ 72%)
// Semantic health accents: Mint/Teal, Soft Coral, Calm Indigo
// =============================================================================

// Brand Primary — Deep Trustworthy Clinical Teal (WCAG AA/AAA on frosted daylight)
val SanctuaryTeal = Color(0xFF0F766E)
val SanctuaryTealDeep = Color(0xFF115E59)
val SanctuaryTealDark = Color(0xFF083B38)
val SanctuarySeafoam = Color(0xFFCCFBF1)
val SanctuarySeafoamSoft = Color(0xFFEEF4F7)

// Soft Frosted Daylight Canvas & Surface Stack
val SanctuarySurface = Color(0xFFFFFFFF)
val SanctuarySurfaceLow = Color(0xFFF4F8FA)
val SanctuaryCream = Color(0xFFEEF4F7)
val SanctuaryCreamWarm = Color(0xFFE6EFF4)
val SanctuaryLinen = Color(0xFFDCE7EE)
val SanctuarySand = Color(0xFFD2E0E9)
val SanctuarySandDeep = Color(0xFFBCCDD8)
val SanctuaryOutline = Color(0xFF94A3B8)
val SanctuaryOutlineSoft = Color(0xFFD3E1EA)

// Calm Indigo Secondary (Sleep, Mindfulness & Reflection)
val BotanicalSage = Color(0xFF4F46E5)
val BotanicalSageContainer = Color(0xFFE0E7FF)
val BotanicalSageOnContainer = Color(0xFF1E1B4B)

// Soft Coral Tertiary (Heart Rate, Vitals & Medical ID)
val TerracottaClay = Color(0xFFE11D48)
val TerracottaContainer = Color(0xFFFFE4E6)
val TerracottaOnContainer = Color(0xFF4C0519)

// Semantic Health Accents — Light Mode (Accessible AA/AAA on frosted glass)
val HealthMintTealLight = Color(0xFF0D9488)
val HealthSoftCoralLight = Color(0xFFE11D48)
val HealthCalmIndigoLight = Color(0xFF4F46E5)
val HealthWarmAmberLight = Color(0xFFD97706)
val HealthOptimalEmeraldLight = Color(0xFF15803D)

// Legacy aliases preserved for full backward compatibility across screens
val AppleHeartPink = HealthSoftCoralLight
val AppleMedsTeal = HealthMintTealLight
val AppleSleepIndigo = HealthCalmIndigoLight
val AppleActivityOrange = HealthWarmAmberLight
val AppleHealthyGreen = HealthOptimalEmeraldLight

// Ambient Daylight Mesh Orbs (Soft refraction backing behind frosted cards)
val GlassLightBackdropBase = Color(0xFFEEF4F7)
val GlassLightOrbMint = Color(0xFF2DD4BF)
val GlassLightOrbIndigo = Color(0xFF818CF8)
val GlassLightOrbCoral = Color(0xFFFB7185)

// High-Contrast Slate Inks (WCAG AAA on frosted white)
val InkCharcoal = Color(0xFF0F172A)
val InkSlate = Color(0xFF475569)
val SanctuaryInverseSurface = Color(0xFF0F172A)
val SanctuaryInverseOnSurface = Color(0xFFF1F5F9)
val SanctuaryInversePrimary = Color(0xFF2DD4BF)
val SanctuaryScrim = Color(0xFF020617)

// =============================================================================
// Dark Well+ Glassmorphism Palette — Deep Twilight Slate
// Deep twilight navy-slate (#0B1120) + translucent glass fills (White @ 8%)
// =============================================================================

val DarkEvergreenLowest = Color(0xFF060A14)
val DarkEvergreen = Color(0xFF0B1120)
val DarkEvergreenSurface = Color(0xFF131C31)
val DarkEvergreenElevated = Color(0xFF1B2742)
val DarkEvergreenHigh = Color(0xFF233252)
val DarkEvergreenHighest = Color(0xFF2D3F66)
val DarkOutline = Color(0xFF3B4D70)
val DarkOutlineSoft = Color(0xFF24324E)

// Luminous Mint-Teal Primary in Dark Mode
val DarkSeafoam = Color(0xFF2DD4BF)
val DarkSeafoamContainer = Color(0xFF0F353A)
val DarkSeafoamOnContainer = Color(0xFFCCFBF1)
val DarkWarmGlow = Color(0xFF0B1120)

// Calm Indigo Secondary in Dark Mode
val DarkSage = Color(0xFF818CF8)
val DarkSageContainer = Color(0xFF1E2252)
val DarkSageOnContainer = Color(0xFFE0E7FF)

// Soft Coral Tertiary in Dark Mode
val DarkTerracotta = Color(0xFFFB7185)
val DarkTerracottaContainer = Color(0xFF471525)
val DarkTerracottaOnContainer = Color(0xFFFFE4E6)

// Semantic Health Accents — Dark Mode (Exact luminous calm health accents)
val HealthMintTealDark = Color(0xFF2DD4BF)
val HealthSoftCoralDark = Color(0xFFFB7185)
val HealthCalmIndigoDark = Color(0xFF818CF8)
val HealthWarmAmberDark = Color(0xFFFBBF24)
val HealthOptimalEmeraldDark = Color(0xFF34D399)

val AppleHeartPinkDark = HealthSoftCoralDark
val AppleMedsTealDark = HealthMintTealDark
val AppleSleepIndigoDark = HealthCalmIndigoDark
val AppleActivityOrangeDark = HealthWarmAmberDark
val AppleHealthyGreenDark = HealthOptimalEmeraldDark

// Ambient Twilight Mesh Orbs
val GlassDarkBackdropBase = Color(0xFF0B1120)
val GlassDarkOrbMint = Color(0xFF2DD4BF)
val GlassDarkOrbIndigo = Color(0xFF818CF8)
val GlassDarkOrbCoral = Color(0xFFFB7185)

val DarkParchment = Color(0xFFF1F5F9)
val DarkMutedText = Color(0xFF94A3B8)
val DarkInverseSurface = Color(0xFFE2E8F0)
val DarkInverseOnSurface = Color(0xFF0F172A)

val DarkError = Color(0xFFFF6B7A)
val DarkOnError = Color(0xFF4C0519)
val DarkOnErrorContainer = Color(0xFFFFE4E6)

// =============================================================================
// Semantic Clinical Urgency Triage Tokens (Color-Blind Safe, Light & Dark)
// =============================================================================

val UrgencySelfCare = Color(0xFF15803D)
val UrgencySelfCareBg = Color(0xFFDCFCE7)
val UrgencySelfCareDarkFg = Color(0xFF34D399)
val UrgencySelfCareDarkBg = Color(0xFF0D3326)

val UrgencyDoctorSoon = Color(0xFFB45309)
val UrgencyDoctorSoonBg = Color(0xFFFEF3C7)
val UrgencyDoctorSoonDarkFg = Color(0xFFFBBF24)
val UrgencyDoctorSoonDarkBg = Color(0xFF3B2A09)

val UrgencyUrgent = Color(0xFFC2410C)
val UrgencyUrgentBg = Color(0xFFFFEDD5)
val UrgencyUrgentDarkFg = Color(0xFFFB923C)
val UrgencyUrgentDarkBg = Color(0xFF421C0D)

val UrgencyEmergency = Color(0xFFBE123C)
val UrgencyEmergencyBg = Color(0xFFFFE4E6)
val UrgencyEmergencyDarkFg = Color(0xFFFB7185)
val UrgencyEmergencyDarkBg = Color(0xFF4A0E20)
