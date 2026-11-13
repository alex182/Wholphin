package com.github.damontecres.wholphin.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.preferences.A11yFont
import com.github.damontecres.wholphin.preferences.AccentColor
import com.github.damontecres.wholphin.preferences.AccessibilityPreference
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.FocusCue
import com.github.damontecres.wholphin.preferences.FocusRingColor
import com.github.damontecres.wholphin.preferences.HighContrastMode
import com.github.damontecres.wholphin.ui.theme.colors.HighContrastThemeColors

/**
 * Resolved accessibility settings for use by composables. Read via [LocalAccessibility].
 *
 * The default instance is upstream behaviour, except that titles are plain text.
 */
@Immutable
data class AccessibilitySettings(
    val focusRingEnabled: Boolean = false,
    val focusRingThickness: Dp = AccessibilityPreference.DEFAULT_FOCUS_RING_THICKNESS.dp,
    /** null means use the theme's border color */
    val focusRingColor: Color? = Color.White,
    val focusCue: FocusCue = FocusCue.FOCUS_CUE_NONE,
    /** 0 to 0.6: how strongly to darken everything except the focused item */
    val dimUnfocused: Float = 0f,
    val speakFocusedItem: Boolean = false,
    /** null means follow the system font scale */
    val textScale: Float? = null,
    val uiZoom: Float = 1f,
    /** 0 means no minimum */
    val minTextSizeSp: Int = 0,
    val font: A11yFont = A11yFont.A11Y_FONT_DEFAULT,
    /** Multiplier for line height, 1 means unchanged */
    val lineHeightScale: Float = 1f,
    /** Extra letter spacing in em, 0 means unchanged */
    val letterSpacingEm: Float = 0f,
    val alwaysShowCardTitles: Boolean = false,
    val wrapCardTitles: Boolean = false,
    /** 0 means no limit */
    val maxHomeRows: Int = 0,
    val hideTechnicalDetails: Boolean = false,
    val reduceMotion: Boolean = false,
    val highContrast: HighContrastMode = HighContrastMode.HIGH_CONTRAST_OFF,
    /** null means keep the theme's accent colour */
    val accentColor: Color? = null,
    /** 0 to 0.9: how strongly to cover the backdrop image with the background colour */
    val backdropDim: Float = 0f,
    /** 0 to 0.5: how much to darken artwork */
    val artworkDim: Float = 0f,
    /** 0 to 1: how much colour to remove from artwork */
    val artworkDesaturate: Float = 0f,
    val highVisibilityStatus: Boolean = false,
    val showProgressPercent: Boolean = false,
    /**
     * Show media titles as text in the standard font, never as logo artwork.
     * On by default: the one deliberate difference from upstream.
     */
    val plainTextTitles: Boolean = true,
    /** Screen reader: cards announce only their title, without year and watched status */
    val cardAnnouncementsTitleOnly: Boolean = false,
) {
    /** The high-contrast colours to use instead of the application theme, if enabled */
    val highContrastColors: ThemeColors?
        get() =
            when (highContrast) {
                HighContrastMode.HIGH_CONTRAST_WHITE_ON_BLACK -> HighContrastThemeColors.WhiteOnBlack
                HighContrastMode.HIGH_CONTRAST_YELLOW_ON_BLACK -> HighContrastThemeColors.YellowOnBlack
                HighContrastMode.HIGH_CONTRAST_BLACK_ON_WHITE -> HighContrastThemeColors.BlackOnWhite
                HighContrastMode.HIGH_CONTRAST_COLOR_BLIND -> HighContrastThemeColors.ColorBlind
                else -> null
            }

    /** Whether the app-wide focus overlay has anything to draw */
    val focusOverlayActive: Boolean
        get() = focusRingEnabled || focusCue != FocusCue.FOCUS_CUE_NONE || dimUnfocused > 0f

    /** Whether text styles need to be changed at all */
    val changesTypography: Boolean
        get() =
            minTextSizeSp > 0 || font != A11yFont.A11Y_FONT_DEFAULT || lineHeightScale != 1f || letterSpacingEm != 0f

    companion object {
        val Default = AccessibilitySettings()
    }
}

val LocalAccessibility = staticCompositionLocalOf { AccessibilitySettings.Default }

/** Accent colour choices. All are light enough to read on every dark theme (see TestThemeContrast). */
fun AccentColor.toColor(): Color? =
    when (this) {
        AccentColor.ACCENT_ORANGE -> Color(0xFFFFB74D)
        AccentColor.ACCENT_CYAN -> Color(0xFF4DD0E1)
        AccentColor.ACCENT_LIME -> Color(0xFFC6FF00)
        AccentColor.ACCENT_PINK -> Color(0xFFFF80AB)
        AccentColor.ACCENT_WHITE -> Color(0xFFFFFFFF)
        else -> null
    }

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
        focusCue = if (focusCue == FocusCue.UNRECOGNIZED) FocusCue.FOCUS_CUE_NONE else focusCue,
        dimUnfocused = dimUnfocusedPercent.coerceIn(0, 60) / 100f,
        speakFocusedItem = speakFocusedItem,
        textScale = textScalePercent.takeIf { it > 0 }?.let { it.coerceIn(50, 300) / 100f },
        uiZoom = (uiZoomPercent.takeIf { it > 0 } ?: 100).coerceIn(50, 200) / 100f,
        minTextSizeSp = minTextSizeSp.coerceIn(0, 40),
        font = if (appFont == A11yFont.UNRECOGNIZED) A11yFont.A11Y_FONT_DEFAULT else appFont,
        lineHeightScale = 1f + lineHeightPercent.coerceIn(0, 100) / 100f,
        letterSpacingEm = letterSpacingPermille.coerceIn(0, 300) / 1000f,
        alwaysShowCardTitles = alwaysShowCardTitles,
        wrapCardTitles = wrapCardTitles,
        maxHomeRows = maxHomeRows.coerceAtLeast(0),
        hideTechnicalDetails = hideTechnicalDetails,
        reduceMotion = reduceMotion,
        plainTextTitles = !allowLogoTitles,
        cardAnnouncementsTitleOnly = cardAnnouncementsTitleOnly,
        backdropDim = backdropDimPercent.coerceIn(0, 90) / 100f,
        accentColor = accentColor.toColor(),
        artworkDim = artworkDimPercent.coerceIn(0, 50) / 100f,
        artworkDesaturate = artworkDesaturatePercent.coerceIn(0, 100) / 100f,
        highVisibilityStatus = highVisibilityStatus,
        showProgressPercent = showProgressPercent,
        highContrast = if (highContrast == HighContrastMode.UNRECOGNIZED) HighContrastMode.HIGH_CONTRAST_OFF else highContrast,
    )
