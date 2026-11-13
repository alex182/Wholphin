package com.github.damontecres.wholphin.a11y

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.tv.material3.ColorScheme
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.HighContrastThemeColors
import com.github.damontecres.wholphin.ui.theme.getThemeColors
import org.junit.Assert
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Checks colour contrast of the app's themes using the WCAG 2 formula.
 *
 * High-contrast themes must reach AAA (7:1) for text. The regular themes are reported but do
 * not fail the build, since they come from upstream.
 */
class TestThemeContrast {
    private data class Pair(
        val name: String,
        val foreground: Color,
        val background: Color,
    )

    private fun textPairs(scheme: ColorScheme) =
        listOf(
            Pair("onBackground/background", scheme.onBackground, scheme.background),
            Pair("onSurface/surface", scheme.onSurface, scheme.surface),
            Pair("onSurfaceVariant/surfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant),
            Pair("onPrimary/primary", scheme.onPrimary, scheme.primary),
            Pair("onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
            Pair("onSecondary/secondary", scheme.onSecondary, scheme.secondary),
            Pair("onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
            Pair("onTertiary/tertiary", scheme.onTertiary, scheme.tertiary),
            Pair("onTertiaryContainer/tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer),
            Pair("onError/error", scheme.onError, scheme.error),
            Pair("onErrorContainer/errorContainer", scheme.onErrorContainer, scheme.errorContainer),
            Pair("inverseOnSurface/inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface),
            // Text drawn directly on the page in accent or error colours
            Pair("primary/background", scheme.primary, scheme.background),
            Pair("error/background", scheme.error, scheme.background),
            Pair("onSurface/surfaceVariant", scheme.onSurface, scheme.surfaceVariant),
        )

    /** Non-text elements (focus borders) need 3:1 */
    private fun uiPairs(scheme: ColorScheme) =
        listOf(
            Pair("border/background", scheme.border, scheme.background),
            Pair("border/surfaceVariant", scheme.border, scheme.surfaceVariant),
        )

    @Test
    fun `Contrast formula matches known values`() {
        Assert.assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
        Assert.assertEquals(1.0, contrastRatio(Color.Red, Color.Red), 0.01)
        // #767676 on white is the well-known AA boundary colour
        Assert.assertEquals(4.54, contrastRatio(Color(0xFF767676), Color.White), 0.01)
    }

    @Test
    fun `High contrast themes meet WCAG AAA`() {
        val themes =
            mapOf(
                "White on black" to HighContrastThemeColors.WhiteOnBlack,
                "Yellow on black" to HighContrastThemeColors.YellowOnBlack,
                "Black on white" to HighContrastThemeColors.BlackOnWhite,
                "Color-blind friendly" to HighContrastThemeColors.ColorBlind,
            )
        val failures = mutableListOf<String>()
        themes.forEach { (name, theme) ->
            val scheme = theme.darkScheme
            textPairs(scheme).forEach {
                val ratio = contrastRatio(it.foreground, it.background)
                if (ratio < AAA_TEXT) failures += "$name: ${it.name} is %.2f:1, needs $AAA_TEXT:1".format(ratio)
            }
            uiPairs(scheme).forEach {
                val ratio = contrastRatio(it.foreground, it.background)
                if (ratio < UI_MINIMUM) failures += "$name: ${it.name} is %.2f:1, needs $UI_MINIMUM:1".format(ratio)
            }
        }
        Assert.assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `Report contrast of regular themes`() {
        AppThemeColors.entries
            .filter { it != AppThemeColors.UNRECOGNIZED }
            .forEach { theme ->
                val scheme = getThemeColors(theme).darkScheme
                val below =
                    textPairs(scheme)
                        .map { it to contrastRatio(it.foreground, it.background) }
                        .filter { it.second < AA_TEXT }
                println("Theme $theme: ${below.size} text pair(s) below AA ($AA_TEXT:1)")
                below.forEach { (pair, ratio) -> println("  ${pair.name}: %.2f:1".format(ratio)) }
                // The main reading pair must always pass AA
                Assert.assertTrue(
                    "$theme onBackground/background below AA",
                    contrastRatio(scheme.onBackground, scheme.background) >= AA_TEXT,
                )
            }
    }

    companion object {
        const val AAA_TEXT = 7.0
        const val AA_TEXT = 4.5
        const val UI_MINIMUM = 3.0

        private fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

        fun luminance(color: Color): Double = 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

        /** WCAG 2 contrast ratio. A translucent foreground is blended onto the background first. */
        fun contrastRatio(
            foreground: Color,
            background: Color,
        ): Double {
            val fg = luminance(foreground.compositeOver(background))
            val bg = luminance(background)
            return (max(fg, bg) + 0.05) / (min(fg, bg) + 0.05)
        }
    }
}
