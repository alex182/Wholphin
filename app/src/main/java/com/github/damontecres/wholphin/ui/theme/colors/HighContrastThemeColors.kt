package com.github.damontecres.wholphin.ui.theme.colors

import androidx.compose.ui.graphics.Color
import com.github.damontecres.wholphin.ui.theme.ThemeColors

/**
 * High-contrast themes for low-vision users (Settings > Accessibility > High contrast).
 *
 * Every text/background pair is at least 7:1 (WCAG AAA). This is enforced by
 * TestThemeContrast, so check that test when changing any colour here.
 */
class HighContrastThemeColors(
    /** Page background */
    private val background: Color,
    /** Main text colour */
    private val foreground: Color,
    /** Slightly raised surfaces such as cards and dialogs */
    private val raised: Color,
    /** More strongly raised surfaces such as chips and selected-but-unfocused items */
    private val raisedStrong: Color,
    private val error: Color,
    private val errorContainer: Color,
    /** Colour for highlights such as the focus border; defaults to the text colour */
    private val accent: Color = foreground,
) : ThemeColors {
    private val scheme =
        androidx.tv.material3.darkColorScheme(
            primary = accent,
            onPrimary = background,
            primaryContainer = accent,
            onPrimaryContainer = background,
            secondary = accent,
            onSecondary = background,
            secondaryContainer = raisedStrong,
            onSecondaryContainer = foreground,
            tertiary = accent,
            onTertiary = background,
            tertiaryContainer = raisedStrong,
            onTertiaryContainer = foreground,
            error = error,
            onError = background,
            errorContainer = errorContainer,
            onErrorContainer = foreground,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = raised,
            onSurfaceVariant = foreground,
            surfaceTint = accent,
            scrim = background,
            // Focused items are drawn inverted: the strongest possible cue
            inverseSurface = foreground,
            inverseOnSurface = background,
            inversePrimary = background,
            border = accent,
            borderVariant = accent,
        )

    private val materialScheme =
        androidx.compose.material3.darkColorScheme(
            primary = accent,
            onPrimary = background,
            primaryContainer = accent,
            onPrimaryContainer = background,
            secondary = accent,
            onSecondary = background,
            secondaryContainer = raisedStrong,
            onSecondaryContainer = foreground,
            tertiary = accent,
            onTertiary = background,
            tertiaryContainer = raisedStrong,
            onTertiaryContainer = foreground,
            error = error,
            onError = background,
            errorContainer = errorContainer,
            onErrorContainer = foreground,
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = raised,
            onSurfaceVariant = foreground,
            scrim = background,
            inverseSurface = foreground,
            inverseOnSurface = background,
            inversePrimary = background,
            outline = accent,
            outlineVariant = accent,
        )

    // The app always uses its "dark" slot, so both slots carry the same colours
    override val lightSchemeMaterial = materialScheme
    override val darkSchemeMaterial = materialScheme
    override val lightScheme = scheme
    override val darkScheme = scheme

    companion object {
        val WhiteOnBlack =
            HighContrastThemeColors(
                background = Color(0xFF000000),
                foreground = Color(0xFFFFFFFF),
                raised = Color(0xFF1C1C1C),
                raisedStrong = Color(0xFF2E2E2E),
                error = Color(0xFFFF9A8F),
                errorContainer = Color(0xFF5C0000),
            )

        val YellowOnBlack =
            HighContrastThemeColors(
                background = Color(0xFF000000),
                foreground = Color(0xFFFFEB00),
                raised = Color(0xFF1C1C1C),
                raisedStrong = Color(0xFF2E2E2E),
                error = Color(0xFFFF9A8F),
                errorContainer = Color(0xFF4A0000),
            )

        val BlackOnWhite =
            HighContrastThemeColors(
                background = Color(0xFFFFFFFF),
                foreground = Color(0xFF000000),
                raised = Color(0xFFEDEDED),
                raisedStrong = Color(0xFFDADADA),
                error = Color(0xFF8B0000),
                errorContainer = Color(0xFFFFD6D6),
            )

        /**
         * Dark blue with white text and an orange accent. Blue and orange stay clearly different
         * for people with red-green (protan, deutan) and blue-yellow (tritan) colour blindness,
         * and nothing relies on telling red from green.
         */
        val ColorBlind =
            HighContrastThemeColors(
                background = Color(0xFF0A1128),
                foreground = Color(0xFFFFFFFF),
                raised = Color(0xFF16213F),
                raisedStrong = Color(0xFF22315A),
                error = Color(0xFFFFB3A7),
                errorContainer = Color(0xFF5C0000),
                accent = Color(0xFFFFB000),
            )
    }
}
