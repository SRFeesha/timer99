package com.timer99.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.timer99.app.model.Palette

@Composable
fun Timer99Theme(
    palette: Palette = Palette.DEFAULT_PALETTE,
    content: @Composable () -> Unit,
) {
    val zen = zenColorSchemeFor(palette)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    CompositionLocalProvider(LocalZen provides zen) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary          = zen.accent,
                onPrimary        = zen.accentForeground,
                // surface == background so WheelPicker fade-out matches the canvas
                surface          = zen.background,
                onSurface        = zen.foreground,
                onSurfaceVariant = zen.foregroundMuted,
                background       = zen.background,
                onBackground     = zen.foreground,
                outline          = zen.border,
            ),
            content = content,
        )
    }
}
