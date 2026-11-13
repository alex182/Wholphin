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

    /**
     * The colour-blind friendly mode must keep its three roles (text, accent, background) apart
     * for the common kinds of colour blindness, not just for typical vision.
     */
    @Test
    fun `Color-blind friendly mode stays distinguishable under simulated colour blindness`() {
        val scheme = HighContrastThemeColors.ColorBlind.darkScheme
        colorBlindnessMatrices.forEach { (name, matrix) ->
            val background = simulate(scheme.background, matrix)
            val text = simulate(scheme.onBackground, matrix)
            val accent = simulate(scheme.border, matrix)
            val textRatio = contrastRatio(text, background)
            val accentRatio = contrastRatio(accent, background)
            Assert.assertTrue("$name: text on background is %.2f:1".format(textRatio), textRatio >= AAA_TEXT)
            Assert.assertTrue("$name: accent on background is %.2f:1".format(accentRatio), accentRatio >= AA_TEXT)
            // The accent must not collapse into the text colour: compare how different they look
            val difference =
                maxOf(
                    kotlin.math.abs(accent.red - text.red),
                    kotlin.math.abs(accent.green - text.green),
                    kotlin.math.abs(accent.blue - text.blue),
                )
            Assert.assertTrue("$name: accent and text differ by only %.2f".format(difference), difference >= .2f)
        }
    }

    companion object {
        /**
         * Simulation matrices for complete protanopia, deuteranopia and tritanopia
         * (Machado, Oliveira and Fernandes, 2009), applied to linear RGB.
         */
        val colorBlindnessMatrices =
            mapOf(
                "protanopia" to
                    floatArrayOf(0.152286f, 1.052583f, -0.204868f, 0.114503f, 0.786281f, 0.099216f, -0.003882f, -0.048116f, 1.051998f),
                "deuteranopia" to
                    floatArrayOf(0.367322f, 0.860646f, -0.227968f, 0.280085f, 0.672501f, 0.047413f, -0.011820f, 0.042940f, 0.968881f),
                "tritanopia" to
                    floatArrayOf(1.255528f, -0.076749f, -0.178779f, -0.078411f, 0.930809f, 0.147602f, 0.004733f, 0.691367f, 0.303900f),
            )

        private fun toLinear(c: Float): Float = channel(c).toFloat()

        private fun toSrgb(c: Float): Float {
            val v = c.coerceIn(0f, 1f)
            return if (v <= 0.0031308f) v * 12.92f else (1.055 * v.toDouble().pow(1 / 2.4) - 0.055).toFloat()
        }

        fun simulate(
            color: Color,
            m: FloatArray,
        ): Color {
            val r = toLinear(color.red)
            val g = toLinear(color.green)
            val b = toLinear(color.blue)
            return Color(
                red = toSrgb(m[0] * r + m[1] * g + m[2] * b),
                green = toSrgb(m[3] * r + m[4] * g + m[5] * b),
                blue = toSrgb(m[6] * r + m[7] * g + m[8] * b),
            )
        }

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
