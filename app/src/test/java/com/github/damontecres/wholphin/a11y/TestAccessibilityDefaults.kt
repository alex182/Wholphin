package com.github.damontecres.wholphin.a11y

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.preferences.AccessibilityPreference
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.AppPreferencesSerializer
import com.github.damontecres.wholphin.preferences.FocusRingColor
import com.github.damontecres.wholphin.preferences.accessibilityPreferences
import com.github.damontecres.wholphin.preferences.updateAccessibilityPreferences
import com.github.damontecres.wholphin.ui.components.focusRingRect
import com.github.damontecres.wholphin.ui.theme.A11yLayout
import com.github.damontecres.wholphin.ui.theme.AccessibilitySettings
import com.github.damontecres.wholphin.ui.theme.toSettings
import org.junit.Assert
import org.junit.Test

/**
 * The fork's core rule: with every accessibility setting at its default, the app behaves exactly
 * like upstream Wholphin.
 */
class TestAccessibilityDefaults {
    @Test
    fun `Unset preferences resolve to upstream behaviour`() {
        // An install upgraded from upstream has no accessibility data at all
        val settings = AccessibilityPreferences.getDefaultInstance().toSettings()
        Assert.assertEquals(AccessibilitySettings.Default, settings)
        Assert.assertFalse(settings.focusRingEnabled)
        Assert.assertNull(settings.textScale)
        Assert.assertEquals(1f, settings.uiZoom)
        Assert.assertFalse(settings.reduceMotion)
    }

    @Test
    fun `Serializer defaults resolve to upstream behaviour`() {
        val prefs = AppPreferencesSerializer().defaultValue
        Assert.assertEquals(AccessibilitySettings.Default, prefs.accessibilityPreferences.toSettings())
    }

    @Test
    fun `Old data without accessibility field parses with defaults`() {
        val old =
            AppPreferences
                .newBuilder()
                .apply { debugLogging = true }
                .build()
                .toByteArray()
        val parsed = AppPreferences.parseFrom(old)
        Assert.assertFalse(parsed.hasAccessibilityPreferences())
        Assert.assertEquals(AccessibilitySettings.Default, parsed.accessibilityPreferences.toSettings())
    }

    @Test
    fun `Preference UI defaults match stored defaults`() {
        val prefs = AppPreferencesSerializer().defaultValue
        accessibilityPreferences
            .flatMap { group -> group.preferences + group.conditionalPreferences.flatMap { it.preferences } }
            .forEach { pref ->
                Assert.assertEquals(
                    "Default mismatch for preference with title id ${pref.title}",
                    pref.defaultValue,
                    pref.getter(prefs),
                )
            }
    }

    @Test
    fun `Settings round trip`() {
        val prefs =
            AppPreferencesSerializer()
                .defaultValue
                .updateAccessibilityPreferences {
                    focusRingEnabled = true
                    focusRingThicknessDp = 6
                    focusRingColor = FocusRingColor.FOCUS_RING_YELLOW
                    textScalePercent = 150
                    uiZoomPercent = 125
                    reduceMotion = true
                }
        val settings = AppPreferences.parseFrom(prefs.toByteArray()).accessibilityPreferences.toSettings()
        Assert.assertTrue(settings.focusRingEnabled)
        Assert.assertEquals(6.dp, settings.focusRingThickness)
        Assert.assertEquals(Color(0xFFFFEB3B), settings.focusRingColor)
        Assert.assertEquals(1.5f, settings.textScale)
        Assert.assertEquals(1.25f, settings.uiZoom)
        Assert.assertTrue(settings.reduceMotion)
    }

    @Test
    fun `Zoom of 100 percent is stored as the default`() {
        val prefs = AppPreferencesSerializer().defaultValue
        val updated = AccessibilityPreference.UiZoom.setter(prefs, 100)
        Assert.assertEquals(0, updated.accessibilityPreferences.uiZoomPercent)
        Assert.assertEquals(100, AccessibilityPreference.UiZoom.getter(updated))
    }

    @Test
    fun `Focus ring skips invisible and full screen elements`() {
        // Normal card
        Assert.assertNotNull(focusRingRect(100, 100, 300, 400, 1920, 1080))
        // Zero-height placeholder used to restore focus
        Assert.assertNull(focusRingRect(0, 500, 1920, 500, 1920, 1080))
        // Full screen player surface
        Assert.assertNull(focusRingRect(0, 0, 1920, 1080, 1920, 1080))
        // A wide row that is not full height still gets a ring
        Assert.assertNotNull(focusRingRect(0, 200, 1920, 300, 1920, 1080))
        // View not laid out yet
        Assert.assertNull(focusRingRect(0, 0, 100, 100, 0, 0))
    }

    @Test
    fun `Headers only shrink when zoomed`() {
        try {
            A11yLayout.uiZoom = 1f
            Assert.assertEquals(1f, A11yLayout.headerScale)
            Assert.assertFalse(A11yLayout.compactHeaders)
            A11yLayout.uiZoom = 1.25f
            Assert.assertTrue(A11yLayout.headerScale < 1f)
            Assert.assertTrue(A11yLayout.compactHeaders)
            A11yLayout.uiZoom = 1.4f
            Assert.assertTrue(A11yLayout.headerScale >= .3f)
        } finally {
            A11yLayout.uiZoom = 1f
        }
    }
}
