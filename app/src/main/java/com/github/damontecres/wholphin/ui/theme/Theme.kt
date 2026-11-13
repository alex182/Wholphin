package com.github.damontecres.wholphin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.BlueThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.BoldBlueThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.BrownThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.GreenThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.OledThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.OrangeThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.PurpleThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.RedThemeColors

val LocalTheme =
    compositionLocalOf<AppThemeColors> { AppThemeColors.PURPLE }

fun getThemeColors(appThemeColors: AppThemeColors): ThemeColors =
    when (appThemeColors) {
        AppThemeColors.PURPLE -> PurpleThemeColors
        AppThemeColors.BLUE -> BlueThemeColors
        AppThemeColors.GREEN -> GreenThemeColors
        AppThemeColors.ORANGE -> OrangeThemeColors
        AppThemeColors.OLED_BLACK -> OledThemeColors
        AppThemeColors.BOLD_BLUE -> BoldBlueThemeColors
        AppThemeColors.RED -> RedThemeColors
        AppThemeColors.BROWN -> BrownThemeColors
        AppThemeColors.UNRECOGNIZED -> PurpleThemeColors
    }

@Composable
fun WholphinTheme(
    darkTheme: Boolean = true,
    appThemeColors: AppThemeColors = AppThemeColors.PURPLE,
    accessibility: AccessibilitySettings = AccessibilitySettings.Default,
    content: @Composable () -> Unit,
) {
    // Accessibility: a high-contrast mode replaces the chosen application theme
    val themeColors = accessibility.highContrastColors ?: getThemeColors(appThemeColors)

    val baseColorScheme =
        when {
            darkTheme -> themeColors.darkScheme
            else -> themeColors.lightScheme
        }
    // Accessibility: optional accent colour (not used with high contrast, which has its own)
    val accent = accessibility.accentColor.takeIf { accessibility.highContrastColors == null }
    // Accessibility: optional colour for the focused item in menus and lists, which the
    // components draw with the scheme's inverse surface colour
    val highlight = accessibility.activeHighlightColor
    val colorScheme =
        remember(baseColorScheme, accent, highlight) {
            var scheme = baseColorScheme
            if (accent != null) {
                scheme = scheme.copy(primary = accent, onPrimary = Color.Black, border = accent)
            }
            if (highlight != null) {
                scheme = scheme.copy(inverseSurface = highlight, inverseOnSurface = accessibility.highlightContentColor)
            }
            scheme
        }
    // Accessibility: font, minimum size, line height and letter spacing
    val typography = remember(accessibility) { AppTypography.withAccessibility(accessibility) }
    // Accessibility: text size and interface zoom are applied by overriding the density
    val baseDensity = LocalDensity.current
    val density =
        remember(baseDensity, accessibility.uiZoom, accessibility.textScale) {
            Density(
                density = baseDensity.density * accessibility.uiZoom,
                fontScale = accessibility.textScale ?: baseDensity.fontScale,
            )
        }
    SideEffect {
        A11yLayout.uiZoom = accessibility.uiZoom
        A11yLayout.textHeightScale = density.fontScale * accessibility.lineHeightScale
        A11yLayout.wrapCardTitles = accessibility.wrapCardTitles
        A11yLayout.scrollingText = !accessibility.noScrollingText
    }
    CompositionLocalProvider(
        // Several components special-case the OLED theme because it has a pure black background.
        // The high-contrast modes need the same handling.
        LocalTheme provides if (accessibility.highContrastColors != null) AppThemeColors.OLED_BLACK else appThemeColors,
        LocalAccessibility provides accessibility,
        LocalDensity provides density,
    ) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = if (darkTheme) themeColors.darkSchemeMaterial else themeColors.lightSchemeMaterial,
            typography = androidx.compose.material3.Typography(),
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                content = content,
            )
        }
    }
}
