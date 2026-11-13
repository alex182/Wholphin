package com.github.damontecres.wholphin.preferences

import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.preferences.ConditionalPreferences
import com.github.damontecres.wholphin.ui.preferences.PreferenceGroup
import com.github.damontecres.wholphin.ui.preferences.PreferenceScreenOption

/**
 * Settings added by the accessibility fork. All of them live in [AccessibilityPreferences].
 *
 * Every setting's default must match upstream behaviour (see WholphinAccessibility.proto).
 */
object AccessibilityPreference {
    val Settings =
        AppDestinationPreference<AppPreferences>(
            title = R.string.a11y_settings,
            summary = R.string.a11y_settings_summary,
            destination = Destination.Settings(PreferenceScreenOption.ACCESSIBILITY),
        )

    // region Focus

    val FocusRingEnabled =
        AppSwitchPreference<AppPreferences>(
            title = R.string.a11y_focus_ring,
            defaultValue = false,
            getter = { it.accessibilityPreferences.focusRingEnabled },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { focusRingEnabled = value }
            },
            summary = R.string.a11y_focus_ring_summary,
        )

    const val DEFAULT_FOCUS_RING_THICKNESS = 4
    val FocusRingThickness =
        AppSliderPreference<AppPreferences>(
            title = R.string.a11y_focus_ring_thickness,
            defaultValue = DEFAULT_FOCUS_RING_THICKNESS.toLong(),
            min = 2,
            max = 8,
            interval = 1,
            getter = {
                it.accessibilityPreferences.focusRingThicknessDp
                    .takeIf { v -> v > 0 }
                    ?.toLong() ?: DEFAULT_FOCUS_RING_THICKNESS.toLong()
            },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { focusRingThicknessDp = value.toInt() }
            },
            summarizer = { value -> value?.let { "${it}dp" } },
        )

    val FocusRingColorPref =
        AppChoicePreference<AppPreferences, FocusRingColor>(
            title = R.string.a11y_focus_ring_color,
            defaultValue = FocusRingColor.FOCUS_RING_WHITE,
            displayValues = R.array.a11y_focus_ring_colors,
            getter = { it.accessibilityPreferences.focusRingColor },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { focusRingColor = value }
            },
            indexToValue = { FocusRingColor.forNumber(it) ?: FocusRingColor.FOCUS_RING_WHITE },
            valueToIndex = { if (it != FocusRingColor.UNRECOGNIZED) it.number else 0 },
        )

    // endregion

    // region Text & layout

    /** Text scale choices in percent. 0 means follow the system font scale. */
    val textScaleValues = listOf(0, 85, 100, 115, 130, 150, 175, 200)
    val TextScale =
        AppChoicePreference<AppPreferences, Int>(
            title = R.string.a11y_text_size,
            defaultValue = 0,
            displayValues = R.array.a11y_text_sizes,
            getter = { it.accessibilityPreferences.textScalePercent },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { textScalePercent = value }
            },
            indexToValue = { textScaleValues.getOrElse(it) { 0 } },
            valueToIndex = { textScaleValues.indexOf(it).coerceAtLeast(0) },
        )

    /** Interface zoom choices in percent. Stored as 0 for 100% so the proto default is upstream. */
    val uiZoomValues = listOf(100, 110, 125, 140)
    val UiZoom =
        AppChoicePreference<AppPreferences, Int>(
            title = R.string.a11y_ui_zoom,
            defaultValue = 100,
            displayValues = R.array.a11y_ui_zooms,
            getter = {
                it.accessibilityPreferences.uiZoomPercent
                    .takeIf { v -> v > 0 } ?: 100
            },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { uiZoomPercent = if (value == 100) 0 else value }
            },
            indexToValue = { uiZoomValues.getOrElse(it) { 100 } },
            valueToIndex = { uiZoomValues.indexOf(it).coerceAtLeast(0) },
        )

    // endregion

    // region Motion

    val ReduceMotion =
        AppSwitchPreference<AppPreferences>(
            title = R.string.a11y_reduce_motion,
            defaultValue = false,
            getter = { it.accessibilityPreferences.reduceMotion },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { reduceMotion = value }
            },
            summary = R.string.a11y_reduce_motion_summary,
        )

    // endregion
}

val accessibilityPreferences =
    listOf(
        PreferenceGroup(
            title = R.string.a11y_group_focus,
            preferences =
                listOf(
                    AccessibilityPreference.FocusRingEnabled,
                ),
            conditionalPreferences =
                listOf(
                    ConditionalPreferences(
                        condition = { it.accessibilityPreferences.focusRingEnabled },
                        preferences =
                            listOf(
                                AccessibilityPreference.FocusRingThickness,
                                AccessibilityPreference.FocusRingColorPref,
                            ),
                    ),
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_text_layout,
            preferences =
                listOf(
                    AccessibilityPreference.TextScale,
                    AccessibilityPreference.UiZoom,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_motion,
            preferences =
                listOf(
                    AccessibilityPreference.ReduceMotion,
                ),
        ),
    )
