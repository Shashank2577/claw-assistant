package com.phoneclaw.ai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Design Tokens from .pen file
val AccentAmber = Color(0xFFF59E0B)
val AccentAmberLight = Color(0xFFFCD34D)
val AccentBlue = Color(0xFF0EA5E9)
val AccentBlueLight = Color(0xFF7DD3FC)
val AccentGreen = Color(0xFF10B981)
val AccentGreenLight = Color(0xFF6EE7B7)
val AccentPink = Color(0xFFDB2777)
val AccentPinkLight = Color(0xFFF472B6)
val AccentViolet = Color(0xFF7C3AED)
val AccentVioletLight = Color(0xFFA78BFA)
val AccentRed = Color(0xFFC46B5E)

val CanvasBg = Color(0xFFF4F1FA)
val SurfaceCard = Color(0xFFEDEAF4)
val SurfacePressed = Color(0xFFE5E1EF)
val SurfaceInverse = Color(0xFF2A2635)

val ForegroundPrimary = Color(0xFF332F3A)
val ForegroundSecondary = Color(0xFF635F69)
val ForegroundMuted = Color(0xFF8E8A95)
val ForegroundInverse = Color(0xFFFFFFFF)

val BorderMuted = Color(0x10000000)
val BorderLight = Color(0x40FFFFFF)

// Shadow colors (mapping for reference, though Compose uses elevation)
val ShadowButtonDrop = Color(0x4D7C3AED)
val ShadowCardDrop = Color(0x33A096B4)

// Chat specific
val UserBubbleLight = AccentViolet
val UserBubbleDark = AccentViolet
val AssistantBubbleLight = ForegroundInverse
val AssistantBubbleDark = Color(0xFF2A2635)

val LocalBadgeGreen = AccentGreen
val CloudBadgeBlue = AccentBlue
val ToolResultBorderLight = AccentViolet
val ToolResultBorderDark = AccentViolet

val CodeBlockBg = Color(0xFF1E1033)
val CodeBlockText = Color(0xFFA5F3C4)

val ChipGradientStart = AccentViolet
val ChipGradientEnd = AccentVioletLight

val SectionLabel = ForegroundMuted

// Material3 light color scheme
val LightColorScheme = lightColorScheme(
    primary = AccentViolet,
    onPrimary = ForegroundInverse,
    primaryContainer = AccentVioletLight,
    onPrimaryContainer = ForegroundPrimary,

    secondary = AccentBlue,
    onSecondary = ForegroundInverse,
    secondaryContainer = AccentBlueLight,
    onSecondaryContainer = ForegroundPrimary,

    tertiary = AccentGreen,
    onTertiary = ForegroundInverse,
    tertiaryContainer = AccentGreenLight,
    onTertiaryContainer = ForegroundPrimary,

    background = CanvasBg,
    onBackground = ForegroundPrimary,

    surface = ForegroundInverse,
    onSurface = ForegroundPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = ForegroundSecondary,

    outline = ForegroundMuted,
    outlineVariant = BorderMuted,

    error = Color(0xFFC46B5E),
    onError = ForegroundInverse,
)

// Material3 dark color scheme (Approximation based on design system logic)
val DarkColorScheme = darkColorScheme(
    primary = AccentVioletLight,
    onPrimary = SurfaceInverse,
    primaryContainer = AccentViolet,
    onPrimaryContainer = ForegroundInverse,

    secondary = AccentBlueLight,
    onSecondary = SurfaceInverse,
    secondaryContainer = AccentBlue,
    onSecondaryContainer = ForegroundInverse,

    background = SurfaceInverse,
    onBackground = ForegroundInverse,

    surface = Color(0xFF332F3A),
    onSurface = ForegroundInverse,
    surfaceVariant = Color(0xFF45404D),
    onSurfaceVariant = ForegroundInverse,

    outline = ForegroundMuted,
    outlineVariant = BorderLight,

    error = Color(0xFFC46B5E),
    onError = ForegroundInverse,
)
