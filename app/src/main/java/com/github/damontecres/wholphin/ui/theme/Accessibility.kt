package com.github.damontecres.wholphin.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.preferences.AccessibilityPreference
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.FocusRingColor
import com.github.damontecres.wholphin.preferences.HighContrastMode
import com.github.damontecres.wholphin.ui.theme.colors.HighContrastThemeColors

/**
 * Resolved accessibility settings for use by composables. Read via [LocalAccessibility].
 *
 * The default instance is exactly upstream behaviour.
 */
@Immutable
data class AccessibilitySettings(
    val focusRingEnabled: Boolean = false,
    val focusRingThickness: Dp = AccessibilityPreference.DEFAULT_FOCUS_RING_THICKNESS.dp,
    /** null means use the theme's border color */
    val focusRingColor: Color? = Color.White,
    /** null means follow the system font scale */
    val textScale: Float? = null,
    val uiZoom: Float = 1f,
    val reduceMotion: Boolean = false,
    val highContrast: HighContrastMode = HighContrastMode.HIGH_CONTRAST_OFF,
    /** 0 to 0.9: how strongly to cover the backdrop image with the background colour */
    val backdropDim: Float = 0f,
    /** Show media titles as text in the standard font, never as logo artwork */
    val plainTextTitles: Boolean = false,
) {
    /** The high-contrast colours to use instead of the application theme, if enabled */
    val highContrastColors: ThemeColors?
        get() =
            when (highContrast) {
                HighContrastMode.HIGH_CONTRAST_WHITE_ON_BLACK -> HighContrastThemeColors.WhiteOnBlack
                HighContrastMode.HIGH_CONTRAST_YELLOW_ON_BLACK -> HighContrastThemeColors.YellowOnBlack
                HighContrastMode.HIGH_CONTRAST_BLACK_ON_WHITE -> HighContrastThemeColors.BlackOnWhite
                else -> null
            }

    companion object {
        val Default = AccessibilitySettings()
    }
}

val LocalAccessibility = staticCompositionLocalOf { AccessibilitySettings.Default }

fun AccessibilityPreferences.toSettings(): AccessibilitySettings =
    AccessibilitySettings(
        focusRingEnabled = focusRingEnabled,
        focusRingThickness =
            (focusRingThicknessDp.takeIf { it > 0 } ?: AccessibilityPreference.DEFAULT_FOCUS_RING_THICKNESS)
                .coerceIn(1, 16)
                .dp,
        focusRingColor =
            when (focusRingColor) {
                FocusRingColor.FOCUS_RING_YELLOW -> Color(0xFFFFEB3B)
                FocusRingColor.FOCUS_RING_CYAN -> Color(0xFF00E5FF)
                FocusRingColor.FOCUS_RING_MAGENTA -> Color(0xFFFF40FF)
                FocusRingColor.FOCUS_RING_THEME -> null
                else -> Color.White
            },
        textScale = textScalePercent.takeIf { it > 0 }?.let { it.coerceIn(50, 300) / 100f },
        uiZoom = (uiZoomPercent.takeIf { it > 0 } ?: 100).coerceIn(50, 200) / 100f,
        reduceMotion = reduceMotion,
        plainTextTitles = plainTextTitles,
        backdropDim = backdropDimPercent.coerceIn(0, 90) / 100f,
        highContrast = if (highContrast == HighContrastMode.UNRECOGNIZED) HighContrastMode.HIGH_CONTRAST_OFF else highContrast,
    )
