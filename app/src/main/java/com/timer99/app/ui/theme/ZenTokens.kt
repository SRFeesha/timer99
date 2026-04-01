package com.timer99.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.timer99.app.model.Palette

/**
 * Semantic design tokens.
 * Every color in the UI should be sourced from here via LocalZen.current —
 * never hardcoded, always named by role rather than appearance.
 */
data class ZenColorScheme(
    // Backgrounds — deeper to more elevated
    val background: Color,          // app canvas, deepest layer
    val backgroundSubtle: Color,    // cards, sheets, raised surfaces
    val backgroundMuted: Color,     // disabled states, de-emphasised areas

    // Foregrounds
    val foreground: Color,          // primary content — headings, key data
    val foregroundMuted: Color,     // secondary content — labels, metadata
    val foregroundSubtle: Color,    // tertiary content — placeholders, hints

    // Structural borders
    val border: Color,              // default dividers and outlines
    val borderStrong: Color,        // focused / active borders

    // Accent — driven by the selected team's primary color
    val accent: Color,              // primary action and brand color
    val accentForeground: Color,    // text / icons on an accent-coloured background
    val accentSubtle: Color,        // low-opacity accent tint — selection fills
    val accentBorder: Color,        // accent-hued border for active cards

    // Team secondary color
    val accentSecondary: Color,     // second brand colour for gradients and badges
)

val LocalZen = staticCompositionLocalOf<ZenColorScheme> {
    error("LocalZen not provided — wrap content in Timer99Theme")
}

fun zenColorSchemeFor(palette: Palette): ZenColorScheme {
    val p = palette.primary
    val s = palette.secondary
    return ZenColorScheme(
        background       = blend(Color(0xFF080C14), p, 0.04f),
        backgroundSubtle = blend(Color(0xFF0F172A), p, 0.08f),
        backgroundMuted  = blend(Color(0xFF0A0F1E), p, 0.05f),

        foreground       = Color(0xFFF1F5F9),
        foregroundMuted  = Color(0xFF94A3B8),
        foregroundSubtle = Color(0xFF475569),

        border       = blend(Color(0xFF1E293B), p, 0.20f),
        borderStrong = blend(Color(0xFF334155), p, 0.40f),

        accent           = p,
        accentForeground = Color.White,
        accentSubtle     = p.copy(alpha = 0.14f),
        accentBorder     = p.copy(alpha = 0.50f),

        accentSecondary = s,
    )
}

/** Linearly interpolate between [base] and [overlay] by [t] (0–1). */
private fun blend(base: Color, overlay: Color, t: Float) = Color(
    red   = (base.red   + (overlay.red   - base.red)   * t).coerceIn(0f, 1f),
    green = (base.green + (overlay.green - base.green) * t).coerceIn(0f, 1f),
    blue  = (base.blue  + (overlay.blue  - base.blue)  * t).coerceIn(0f, 1f),
    alpha = 1f,
)
