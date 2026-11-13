package com.github.damontecres.wholphin.a11y

import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.AppChoicePreference
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.AppPreferencesSerializer
import com.github.damontecres.wholphin.preferences.AppSliderPreference
import com.github.damontecres.wholphin.preferences.AppSwitchPreference
import com.github.damontecres.wholphin.preferences.accessibilityPreferences
import org.junit.Assert
import org.junit.Test

/**
 * Enforces the fork's rule that everything is configurable: every field stored in
 * [AccessibilityPreferences] must be changeable from a setting on the Accessibility screen.
 */
class TestSettingsCoverage {
    /** Fields that are deliberately not on the Accessibility screen, with the reason */
    private val notOnAccessibilityScreen =
        mapOf<String, String>(
            // "SubtitleFont" to "Shown on the subtitle style page instead",
        )

    /** Stored field names, e.g. "FocusRingEnabled", from the generated FOO_FIELD_NUMBER constants */
    private val storedFields: List<String> =
        AccessibilityPreferences::class.java.fields
            .map { it.name }
            .filter { it.endsWith("_FIELD_NUMBER") }
            .map { constant ->
                constant
                    .removeSuffix("_FIELD_NUMBER")
                    .lowercase()
                    .split("_")
                    .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
            }

    private fun read(
        prefs: AccessibilityPreferences,
        field: String,
    ): Any? {
        val cls = AccessibilityPreferences::class.java
        // Enum fields also have getFooValue(), which works for unknown values too
        val getter =
            runCatching { cls.getMethod("get${field}Value") }.getOrNull()
                ?: runCatching { cls.getMethod("get$field") }.getOrNull()
                ?: cls.getMethod("get${field}Map")
        return getter.invoke(prefs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun changeToNonDefault(
        pref: AppPreference<AppPreferences, out Any?>,
        prefs: AppPreferences,
    ): AppPreferences? =
        when (pref) {
            is AppSwitchPreference -> {
                pref.setter(prefs, !pref.getter(prefs))
            }

            is AppChoicePreference<*, *> -> {
                pref as AppChoicePreference<AppPreferences, Any?>
                pref.setter(prefs, pref.indexToValue(1))
            }

            is AppSliderPreference -> {
                val current = pref.getter(prefs)
                pref.setter(prefs, if (current != pref.max) pref.max else pref.min)
            }

            else -> {
                null
            }
        }

    @Test
    fun `Stored fields are discovered`() {
        Assert.assertTrue(storedFields.toString(), storedFields.size >= 10)
        Assert.assertTrue("FocusRingEnabled" in storedFields)
    }

    @Test
    fun `Every stored accessibility field has a setting on the Accessibility screen`() {
        val defaults = AppPreferencesSerializer().defaultValue
        val allPrefs =
            accessibilityPreferences
                .flatMap { group -> group.preferences + group.conditionalPreferences.flatMap { it.preferences } }
        val covered = mutableSetOf<String>()
        allPrefs.forEach { pref ->
            val changed = changeToNonDefault(pref, defaults) ?: return@forEach
            storedFields.forEach { field ->
                if (read(defaults.accessibilityPreferences, field) != read(changed.accessibilityPreferences, field)) {
                    covered += field
                }
            }
        }
        val missing = storedFields.filter { it !in covered && it !in notOnAccessibilityScreen }
        Assert.assertTrue(
            "These stored accessibility fields have no setting on the Accessibility screen: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `Each setting changes only accessibility data unless it is a repeated upstream setting`() {
        // Mark the accessibility message as present so that only real changes show up as differences
        val defaults =
            AppPreferencesSerializer()
                .defaultValue
                .toBuilder()
                .setAccessibilityPreferences(AccessibilityPreferences.getDefaultInstance())
                .build()
        val allPrefs =
            accessibilityPreferences
                .flatMap { group -> group.preferences + group.conditionalPreferences.flatMap { it.preferences } }
        var forkSettings = 0
        allPrefs.forEach { pref ->
            val changed = changeToNonDefault(pref, defaults) ?: return@forEach
            if (changed.accessibilityPreferences != defaults.accessibilityPreferences) {
                forkSettings++
                // A fork setting must not touch anything upstream owns
                val restored =
                    changed
                        .toBuilder()
                        .setAccessibilityPreferences(defaults.accessibilityPreferences)
                        .build()
                Assert.assertEquals("Setting with title id ${pref.title} changed upstream data", defaults, restored)
            }
        }
        Assert.assertTrue(forkSettings >= 10)
    }
}
