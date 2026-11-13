package com.github.damontecres.wholphin.a11y

import androidx.compose.ui.unit.sp
import com.github.damontecres.wholphin.preferences.A11yFont
import com.github.damontecres.wholphin.preferences.AccentColor
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.components.artworkColorMatrix
import com.github.damontecres.wholphin.ui.theme.AccessibilitySettings
import com.github.damontecres.wholphin.ui.theme.AppTypography
import com.github.damontecres.wholphin.ui.theme.getThemeColors
import com.github.damontecres.wholphin.ui.theme.toColor
import com.github.damontecres.wholphin.ui.theme.withAccessibility
import com.github.damontecres.wholphin.util.KeyRepeatThrottle
import org.junit.Assert
import org.junit.Test

class TestAccessibilityHelpers {
    @Test
    fun `Typography is untouched by default`() {
        Assert.assertSame(AppTypography, AppTypography.withAccessibility(AccessibilitySettings.Default))
    }

    @Test
    fun `Minimum text size raises small styles only`() {
        val settings = AccessibilitySettings(minTextSizeSp = 18)
        val result = AppTypography.withAccessibility(settings)
        listOf(result.labelSmall, result.bodySmall, result.bodyMedium, result.titleSmall).forEach {
            Assert.assertTrue(it.fontSize.value >= 18f)
        }
        // Styles that were already large keep their size
        Assert.assertEquals(AppTypography.displayLarge.fontSize, result.displayLarge.fontSize)
        // Line height grows with the text so lines do not overlap
        Assert.assertTrue(result.labelSmall.lineHeight.value >= result.labelSmall.fontSize.value)
    }

    @Test
    fun `Line height and letter spacing settings apply`() {
        val result = AppTypography.withAccessibility(AccessibilitySettings(lineHeightScale = 1.5f, letterSpacingEm = .06f))
        Assert.assertEquals(AppTypography.bodyMedium.lineHeight.value * 1.5f, result.bodyMedium.lineHeight.value, .01f)
        Assert.assertTrue(result.bodyMedium.letterSpacing.isEm)
        Assert.assertEquals(AppTypography.bodyMedium.fontSize, result.bodyMedium.fontSize)
    }

    @Test
    fun `Unspecified sizes are left alone`() {
        val style =
            androidx.compose.ui.text
                .TextStyle()
                .withAccessibility(AccessibilitySettings(minTextSizeSp = 20), null)
        Assert.assertFalse(style.fontSize.value > 0 && style.fontSize == 20.sp)
    }

    @Test
    fun `Font setting is part of typography changes`() {
        Assert.assertTrue(AccessibilitySettings(font = A11yFont.A11Y_FONT_ATKINSON).changesTypography)
        Assert.assertFalse(AccessibilitySettings.Default.changesTypography)
    }

    @Test
    fun `Artwork is unchanged by default`() {
        Assert.assertNull(artworkColorMatrix(0f, 0f))
    }

    @Test
    fun `Artwork dimming and desaturation`() {
        val dim = artworkColorMatrix(.5f, 0f)!!
        Assert.assertEquals(.5f, dim[0, 0], .001f)
        Assert.assertEquals(.5f, dim[1, 1], .001f)
        Assert.assertEquals(1f, dim[3, 3], .001f)
        val grey = artworkColorMatrix(0f, 1f)!!
        // Fully desaturated: every output channel is the same mix of the inputs
        for (column in 0..2) {
            Assert.assertEquals(grey[0, column], grey[1, column], .001f)
            Assert.assertEquals(grey[1, column], grey[2, column], .001f)
        }
    }

    @Test
    fun `Accent colours are readable on every regular theme`() {
        AccentColor.entries.mapNotNull { it.toColor() }.forEach { accent ->
            AppThemeColors.entries.filter { it != AppThemeColors.UNRECOGNIZED }.forEach { theme ->
                val scheme = getThemeColors(theme).darkScheme
                val ratio = TestThemeContrast.contrastRatio(accent, scheme.background)
                Assert.assertTrue("Accent $accent on $theme background is %.2f:1".format(ratio), ratio >= TestThemeContrast.AA_TEXT)
            }
            // Text on an accent-coloured button is black
            Assert.assertTrue(
                TestThemeContrast.contrastRatio(androidx.compose.ui.graphics.Color.Black, accent) >= TestThemeContrast.AAA_TEXT,
            )
        }
        Assert.assertNull(AccentColor.ACCENT_DEFAULT.toColor())
    }

    @Test
    fun `Key repeat is not throttled by default`() {
        KeyRepeatThrottle.intervalMs = 0
        repeat(5) { Assert.assertFalse(KeyRepeatThrottle.shouldDrop(true, it, 1000L + it * 50)) }
    }

    @Test
    fun `Key repeat throttling drops fast repeats but never the first press`() {
        try {
            KeyRepeatThrottle.intervalMs = 250
            Assert.assertFalse(KeyRepeatThrottle.shouldDrop(true, 0, 1000))
            Assert.assertTrue(KeyRepeatThrottle.shouldDrop(true, 1, 1050))
            Assert.assertTrue(KeyRepeatThrottle.shouldDrop(true, 2, 1100))
            Assert.assertFalse(KeyRepeatThrottle.shouldDrop(true, 3, 1260))
            Assert.assertTrue(KeyRepeatThrottle.shouldDrop(true, 4, 1300))
            // A fresh press is always accepted
            Assert.assertFalse(KeyRepeatThrottle.shouldDrop(true, 0, 1310))
            // Other keys are never dropped
            Assert.assertFalse(KeyRepeatThrottle.shouldDrop(false, 5, 1320))
        } finally {
            KeyRepeatThrottle.intervalMs = 0
        }
    }
}
