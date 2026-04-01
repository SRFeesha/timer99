package com.timer99.app.model

import androidx.compose.ui.graphics.Color

enum class Palette(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
) {
    INDIGO("Indigo",   Color(0xFF6366F1), Color(0xFF818CF8)),
    VIOLET("Violet",   Color(0xFF7C3AED), Color(0xFFA78BFA)),
    EMERALD("Emerald", Color(0xFF059669), Color(0xFF34D399)),
    ROSE("Rose",       Color(0xFFE11D48), Color(0xFFFB7185)),
    AMBER("Amber",     Color(0xFFD97706), Color(0xFFFCD34D)),
    SKY("Sky",         Color(0xFF0284C7), Color(0xFF38BDF8)),
    CRIMSON("Crimson", Color(0xFFDC2626), Color(0xFFF87171)),
    SLATE("Slate",     Color(0xFF7C8598), Color(0xFF94A3B8)),
    ;
    companion object {
        val DEFAULT_PALETTE: Palette = INDIGO
    }
}
