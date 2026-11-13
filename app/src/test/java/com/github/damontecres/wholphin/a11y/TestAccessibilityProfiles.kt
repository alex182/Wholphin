package com.github.damontecres.wholphin.a11y

import android.view.KeyEvent
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.AppPreferencesSerializer
import com.github.damontecres.wholphin.preferences.HighContrastMode
import com.github.damontecres.wholphin.preferences.QuickShortcut
import com.github.damontecres.wholphin.preferences.applyAccessibilityToAllProfiles
import com.github.damontecres.wholphin.preferences.switchAccessibilityProfile
import com.github.damontecres.wholphin.preferences.updateAccessibilityPreferences
import com.github.damontecres.wholphin.ui.components.A11yQuickSettings
import org.junit.Assert
import org.junit.Test

class TestAccessibilityProfiles {
    private val defaults: AppPreferences = AppPreferencesSerializer().defaultValue

    @Test
    fun `Nothing happens when per-profile settings are off`() {
        val prefs = defaults.updateAccessibilityPreferences { textScalePercent = 150 }
        Assert.assertSame(prefs, prefs.switchAccessibilityProfile("alice"))
        Assert.assertSame(prefs, prefs.switchAccessibilityProfile(null))
    }

    @Test
    fun `Each profile keeps its own settings`() {
        var prefs =
            defaults.updateAccessibilityPreferences {
                perProfileSettings = true
                setupWizardDone = true
            }
        // Alice signs in and sets large text
        prefs = prefs.switchAccessibilityProfile("alice")
        prefs = prefs.updateAccessibilityPreferences { textScalePercent = 175 }
        // Bob signs in: starts from what was there, then chooses high contrast and normal text
        prefs = prefs.switchAccessibilityProfile("bob")
        Assert.assertEquals(175, prefs.accessibilityPreferences.textScalePercent)
        prefs =
            prefs.updateAccessibilityPreferences {
                textScalePercent = 0
                highContrast = HighContrastMode.HIGH_CONTRAST_YELLOW_ON_BLACK
            }
        // Back to Alice: her large text returns and Bob's contrast is gone
        prefs = prefs.switchAccessibilityProfile("alice")
        Assert.assertEquals(175, prefs.accessibilityPreferences.textScalePercent)
        Assert.assertEquals(HighContrastMode.HIGH_CONTRAST_OFF, prefs.accessibilityPreferences.highContrast)
        // And Bob again
        prefs = prefs.switchAccessibilityProfile("bob")
        Assert.assertEquals(0, prefs.accessibilityPreferences.textScalePercent)
        Assert.assertEquals(HighContrastMode.HIGH_CONTRAST_YELLOW_ON_BLACK, prefs.accessibilityPreferences.highContrast)
        // Device-level flags survive every switch
        Assert.assertTrue(prefs.accessibilityPreferences.perProfileSettings)
        Assert.assertTrue(prefs.accessibilityPreferences.setupWizardDone)
    }

    @Test
    fun `Same user again changes nothing`() {
        val prefs =
            defaults
                .updateAccessibilityPreferences { perProfileSettings = true }
                .switchAccessibilityProfile("alice")
        Assert.assertSame(prefs, prefs.switchAccessibilityProfile("alice"))
    }

    @Test
    fun `Apply to all profiles`() {
        var prefs = defaults.updateAccessibilityPreferences { perProfileSettings = true }
        prefs = prefs.switchAccessibilityProfile("alice")
        prefs = prefs.switchAccessibilityProfile("bob")
        prefs = prefs.updateAccessibilityPreferences { reduceMotion = true }.applyAccessibilityToAllProfiles()
        prefs = prefs.switchAccessibilityProfile("alice")
        Assert.assertTrue(prefs.accessibilityPreferences.reduceMotion)
    }

    @Test
    fun `Quick settings shortcut is off by default and only reacts to the chosen long press`() {
        val before = A11yQuickSettings.shortcut
        try {
            A11yQuickSettings.visible = false
            A11yQuickSettings.shortcut = QuickShortcut.QUICK_SHORTCUT_OFF
            Assert.assertFalse(A11yQuickSettings.handleKey(KeyEvent.KEYCODE_MENU, isLongPress = true, inPlayer = false))

            A11yQuickSettings.shortcut = QuickShortcut.QUICK_SHORTCUT_MENU
            // A short press is left alone
            Assert.assertFalse(A11yQuickSettings.handleKey(KeyEvent.KEYCODE_MENU, isLongPress = false, inPlayer = false))
            Assert.assertFalse(A11yQuickSettings.handleKey(KeyEvent.KEYCODE_INFO, isLongPress = true, inPlayer = false))
            Assert.assertTrue(A11yQuickSettings.handleKey(KeyEvent.KEYCODE_MENU, isLongPress = true, inPlayer = false))
            Assert.assertTrue(A11yQuickSettings.visible)

            A11yQuickSettings.visible = false
            A11yQuickSettings.shortcut = QuickShortcut.QUICK_SHORTCUT_PLAY_PAUSE
            Assert.assertFalse(A11yQuickSettings.handleKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, isLongPress = true, inPlayer = true))
            Assert.assertTrue(A11yQuickSettings.handleKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, isLongPress = true, inPlayer = false))
        } finally {
            A11yQuickSettings.visible = false
            A11yQuickSettings.shortcut = before
        }
    }
}
