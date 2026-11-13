package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.A11yFont

/** Fonts chosen for legibility. All are under the SIL Open Font License (see FONT_LICENSES.md). */
fun A11yFont.toFontFamily(): FontFamily? =
    when (this) {
        A11yFont.A11Y_FONT_ATKINSON -> {
            FontFamily(
                Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
                Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
            )
        }

        A11yFont.A11Y_FONT_LEXEND -> {
            FontFamily(
                listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
                    Font(
                        R.font.lexend_variable,
                        weight,
                        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
                    )
                },
            )
        }

        A11yFont.A11Y_FONT_OPEN_DYSLEXIC -> {
            FontFamily(
                Font(R.font.opendyslexic_regular, FontWeight.Normal),
                Font(R.font.opendyslexic_bold, FontWeight.Bold),
            )
        }

        else -> {
            null
        }
    }

/**
 * Applies the font, minimum size, line height and letter spacing settings to one text style.
 * With default settings the style is returned unchanged.
 */
fun TextStyle.withAccessibility(
    settings: AccessibilitySettings,
    fontFamily: FontFamily?,
): TextStyle {
    if (!settings.changesTypography) return this
    val size =
        if (settings.minTextSizeSp > 0 && fontSize.isSpecified && fontSize.isSp && fontSize.value < settings.minTextSizeSp) {
            settings.minTextSizeSp.sp
        } else {
            fontSize
        }
    // Keep the same proportions when the size is raised, then apply the extra line height
    val sizeRatio = if (fontSize.isSpecified && fontSize.value > 0f) size.value / fontSize.value else 1f
    val newLineHeight =
        if (lineHeight.isSpecified && lineHeight.isSp) {
            (lineHeight.value * sizeRatio * settings.lineHeightScale).sp
        } else {
            lineHeight
        }
    return copy(
        fontFamily = fontFamily ?: this.fontFamily,
        fontSize = size,
        lineHeight = newLineHeight,
        letterSpacing = if (settings.letterSpacingEm > 0f) settings.letterSpacingEm.em else letterSpacing,
    )
}

/** Applies the accessibility text settings to every style in the app's typography */
fun Typography.withAccessibility(settings: AccessibilitySettings): Typography {
    if (!settings.changesTypography) return this
    val family = settings.font.toFontFamily()

    fun TextStyle.a11y() = withAccessibility(settings, family)
    return copy(
        displayLarge = displayLarge.a11y(),
        displayMedium = displayMedium.a11y(),
        displaySmall = displaySmall.a11y(),
        headlineLarge = headlineLarge.a11y(),
        headlineMedium = headlineMedium.a11y(),
        headlineSmall = headlineSmall.a11y(),
        titleLarge = titleLarge.a11y(),
        titleMedium = titleMedium.a11y(),
        titleSmall = titleSmall.a11y(),
        bodyLarge = bodyLarge.a11y(),
        bodyMedium = bodyMedium.a11y(),
        bodySmall = bodySmall.a11y(),
        labelLarge = labelLarge.a11y(),
        labelMedium = labelMedium.a11y(),
        labelSmall = labelSmall.a11y(),
    )
}
