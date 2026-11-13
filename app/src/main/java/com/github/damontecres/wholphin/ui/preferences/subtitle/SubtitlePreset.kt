package com.github.damontecres.wholphin.ui.preferences.subtitle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AppChoicePreference
import com.github.damontecres.wholphin.preferences.BackgroundStyle
import com.github.damontecres.wholphin.preferences.EdgeStyle
import com.github.damontecres.wholphin.preferences.SubtitlePreferences
import com.github.damontecres.wholphin.preferences.resetSubtitles

/**
 * Accessibility: one-click subtitle styles.
 *
 * A preset only sets the existing individual subtitle settings, so each can still be adjusted
 * afterwards (the preset then shows as "Custom").
 */
enum class SubtitlePreset(
    private val block: (SubtitlePreferences.Builder.() -> Unit)?,
) {
    /** Whatever the user has set by hand. Selecting it changes nothing. */
    CUSTOM(null),

    /** The upstream default style */
    CLASSIC({ resetStyle() }),

    /** Large yellow text on a dark box */
    HIGH_VISIBILITY({
        resetStyle()
        fontSize = 40
        fontColor = Color.Yellow.toArgb()
        fontBold = true
        edgeStyle = EdgeStyle.EDGE_SOLID
        edgeColor = Color.Black.toArgb()
        edgeThickness = 6
        backgroundStyle = BackgroundStyle.BG_WRAP
        backgroundColor = Color.Black.toArgb()
        backgroundOpacity = 80
    }),

    /** Extra large white text on a solid black box */
    LOW_VISION({
        resetStyle()
        fontSize = 56
        fontColor = Color.White.toArgb()
        fontBold = true
        edgeStyle = EdgeStyle.EDGE_SOLID
        edgeColor = Color.Black.toArgb()
        edgeThickness = 8
        backgroundStyle = BackgroundStyle.BG_WRAP
        backgroundColor = Color.Black.toArgb()
        backgroundOpacity = 100
    }),

    /** Plain white text with a soft shadow */
    MINIMAL({
        resetStyle()
        edgeStyle = EdgeStyle.EDGE_SHADOW
        backgroundStyle = BackgroundStyle.BG_NONE
    }),
    ;

    fun applyTo(prefs: SubtitlePreferences): SubtitlePreferences = if (block == null) prefs else prefs.update(block)

    companion object {
        /** The preset matching the given settings exactly, or [CUSTOM] */
        fun matching(prefs: SubtitlePreferences): SubtitlePreset =
            entries.firstOrNull { it != CUSTOM && it.applyTo(prefs) == prefs } ?: CUSTOM

        val Preference =
            AppChoicePreference<SubtitlePreferences, SubtitlePreset>(
                title = R.string.a11y_subtitle_preset,
                defaultValue = CLASSIC,
                displayValues = R.array.a11y_subtitle_presets,
                getter = { matching(it) },
                setter = { prefs, value -> value.applyTo(prefs) },
                indexToValue = { entries.getOrElse(it) { CUSTOM } },
                valueToIndex = { it.ordinal },
            )
    }
}

/**
 * Resets the look of the text, keeping settings that are not about style
 * (the separate HDR toggle and image subtitle opacity).
 */
private fun SubtitlePreferences.Builder.resetStyle() {
    val keepImageOpacity = imageSubtitleOpacity
    resetSubtitles()
    imageSubtitleOpacity = keepImageOpacity
}
