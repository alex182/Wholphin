package com.github.damontecres.wholphin.a11y

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.damontecres.wholphin.preferences.A11yFont
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.AppPreferencesSerializer
import com.github.damontecres.wholphin.preferences.HighContrastMode
import com.github.damontecres.wholphin.preferences.updateAccessibilityPreferences
import com.github.damontecres.wholphin.ui.setup.AccessibilityWizard
import com.github.damontecres.wholphin.ui.theme.WholphinTheme
import com.github.damontecres.wholphin.ui.theme.toSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Renders the accessibility setup screen (which uses the same components as the settings pages)
 * under the most demanding settings, to catch crashes and content that disappears.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class TestAccessibilityRendering {
    @get:Rule
    val composeRule = createComposeRule()

    private fun render(block: AccessibilityPreferences.Builder.() -> Unit) {
        val prefs: AppPreferences = AppPreferencesSerializer().defaultValue.updateAccessibilityPreferences(block)
        composeRule.setContent {
            WholphinTheme(accessibility = prefs.accessibilityPreferences.toSettings()) {
                AccessibilityWizard(
                    preferences = prefs,
                    onUpdate = {},
                    onDone = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithText("Accessibility setup").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun `Default settings`() = render {}

    @Test
    fun `Largest text and zoom`() =
        render {
            textScalePercent = 200
            uiZoomPercent = 140
            minTextSizeSp = 20
        }

    @Test
    fun `High contrast with a legibility font and wide spacing`() =
        render {
            highContrast = HighContrastMode.HIGH_CONTRAST_BLACK_ON_WHITE
            appFont = A11yFont.A11Y_FONT_OPEN_DYSLEXIC
            lineHeightPercent = 50
            letterSpacingPermille = 120
        }

    @Test
    fun `Every high contrast mode and font`() {
        // One composition per test, so cover the remaining combinations by resolving settings
        HighContrastMode.entries.filter { it != HighContrastMode.UNRECOGNIZED }.forEach { mode ->
            A11yFont.entries.filter { it != A11yFont.UNRECOGNIZED }.forEach { font ->
                AccessibilityPreferences
                    .newBuilder()
                    .setHighContrast(mode)
                    .setAppFont(font)
                    .build()
                    .toSettings()
            }
        }
        render {
            highContrast = HighContrastMode.HIGH_CONTRAST_COLOR_BLIND
            appFont = A11yFont.A11Y_FONT_LEXEND
            focusRingEnabled = true
        }
    }
}
