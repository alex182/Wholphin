package com.github.damontecres.wholphin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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
    val themeColors = getThemeColors(appThemeColors)

    val colorScheme =
        when {
            darkTheme -> themeColors.darkScheme
            else -> themeColors.lightScheme
        }
    // Accessibility: text size and interface zoom are applied by overriding the density
    val baseDensity = LocalDensity.current
    val density =
        remember(baseDensity, accessibility.uiZoom, accessibility.textScale) {
            Density(
                density = baseDensity.density * accessibility.uiZoom,
                fontScale = accessibility.textScale ?: baseDensity.fontScale,
            )
        }
    SideEffect { A11yLayout.uiZoom = accessibility.uiZoom }
    CompositionLocalProvider(
        LocalTheme provides appThemeColors,
        LocalAccessibility provides accessibility,
        LocalDensity provides density,
    ) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = if (darkTheme) themeColors.darkSchemeMaterial else themeColors.lightSchemeMaterial,
            typography = androidx.compose.material3.Typography(),
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = AppTypography,
                content = content,
            )
        }
    }
}
