package com.aicomp.ui.theme

import androidx.compose.ui.graphics.Color

// Core brand colors — matches the Login screen's warm pink/violet glow
val PinkPrimary = Color(0xFFE91E8C)
val PinkPrimaryDark = Color(0xFFB0176B)
val VioletContainerLight = Color(0xFFEDE4FF)

val AccentPink = Color(0xFFFF6F91)
val AccentTeal = Color(0xFF29B6A6)

// Dark theme surfaces — same navy family as the Login gradient
// (#1A1A2E -> #16213E -> #0F3460), so every screen feels continuous with it.
val DarkBackground = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF222846)
val DarkSurfaceVariant = Color(0xFF2B3158)

// Light theme surfaces — kept only as a fallback, unused now that the app
// always renders dark (see Theme.kt).
val LightBackground = Color(0xFFFAF9FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1EEFB)

// Text
val TextPrimaryDark = Color(0xFFF5F3FA)
val TextSecondaryDark = Color(0xFFB4B1C7)
val TextPrimaryLight = Color(0xFF1C1B22)
val TextSecondaryLight = Color(0xFF6B6879)
