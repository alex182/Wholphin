package com.github.damontecres.wholphin.util

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.A11yFont
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.SoundCueStyle

/**
 * Accessibility options for subtitles that upstream's subtitle style does not cover: font,
 * position at the top of the screen, and styling of sound-effect cues such as "[door slams]".
 *
 * Held outside the composition because subtitles are drawn by Android views and by MPV.
 * Updated from the user's preferences by MainActivity. Defaults change nothing.
 */
object A11ySubtitles {
    @Volatile
    var font: A11yFont = A11yFont.A11Y_FONT_DEFAULT

    @Volatile
    var atTop: Boolean = false

    @Volatile
    var soundCueStyle: SoundCueStyle = SoundCueStyle.SOUND_CUE_NONE

    /** MPV only: ignore the styling inside ASS/SSA subtitles and use the user's style */
    @Volatile
    var forceStyle: Boolean = false

    fun update(prefs: AccessibilityPreferences) {
        font = prefs.subtitleFont
        atTop = prefs.subtitlesAtTop
        soundCueStyle = prefs.soundCueStyle
        forceStyle = prefs.forceSubtitleStyle
    }

    /** The typeface for the chosen subtitle font, or null to keep the default */
    fun typeface(
        context: Context,
        bold: Boolean,
        italic: Boolean,
    ): Typeface? {
        val resource =
            when (font) {
                A11yFont.A11Y_FONT_ATKINSON -> if (bold) R.font.atkinson_hyperlegible_bold else R.font.atkinson_hyperlegible_regular
                A11yFont.A11Y_FONT_LEXEND -> R.font.lexend_variable
                A11yFont.A11Y_FONT_OPEN_DYSLEXIC -> if (bold) R.font.opendyslexic_bold else R.font.opendyslexic_regular
                else -> return null
            }
        val base = runCatching { ResourcesCompat.getFont(context, resource) }.getOrNull() ?: return null
        val style =
            when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
        return Typeface.create(base, style)
    }

    /** Matches sound descriptions: [door slams], (laughs), and music between note symbols */
    private val soundCueRegex = Regex("""\[[^\]\n]+]|\([^)\n]+\)|[♪♫][^♪♫\n]*[♪♫]?""")

    /** The ranges of [text] that describe sounds rather than speech */
    fun findSoundCues(text: CharSequence): List<IntRange> = soundCueRegex.findAll(text).map { it.range }.toList()

    private var lastInput: List<Cue>? = null
    private var lastOutput: List<Cue>? = null
    private var lastConfig: Any? = null

    /**
     * Applies the position and sound-cue options to the cues about to be shown.
     * Returns the same list when neither option is on.
     */
    @OptIn(UnstableApi::class)
    fun transform(cues: List<Cue>): List<Cue> {
        val top = atTop
        val style = soundCueStyle
        if (!top && (style == SoundCueStyle.SOUND_CUE_NONE || style == SoundCueStyle.UNRECOGNIZED)) return cues
        val config = top to style
        if (cues === lastInput && config == lastConfig) lastOutput?.let { return it }
        val result =
            cues.map { cue ->
                val text = cue.text ?: return@map cue
                val builder = cue.buildUpon()
                if (top) {
                    builder
                        .setLine(TOP_LINE_FRACTION, Cue.LINE_TYPE_FRACTION)
                        .setLineAnchor(Cue.ANCHOR_TYPE_START)
                }
                val ranges = if (style != SoundCueStyle.SOUND_CUE_NONE) findSoundCues(text) else emptyList()
                if (ranges.isNotEmpty()) {
                    val spannable = SpannableString(text)
                    ranges.forEach { range ->
                        val span: Any =
                            if (style == SoundCueStyle.SOUND_CUE_COLOR) {
                                ForegroundColorSpan(SOUND_CUE_COLOR)
                            } else {
                                StyleSpan(Typeface.ITALIC)
                            }
                        spannable.setSpan(span, range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    builder.setText(spannable)
                }
                builder.build()
            }
        lastInput = cues
        lastOutput = result
        lastConfig = config
        return result
    }

    private const val TOP_LINE_FRACTION = .06f

    /** Light cyan: distinct from white and yellow text and readable on dark backgrounds */
    private const val SOUND_CUE_COLOR = 0xFF80DEEA.toInt()

    /** Titles that mark an audio track as audio description */
    private val audioDescriptionRegex =
        Regex("""audio[ -]?descri|descriptive|described video|\bDVS\b|\bAD\b|visually impaired""", RegexOption.IGNORE_CASE)

    fun isAudioDescription(vararg titles: String?): Boolean = titles.any { it != null && audioDescriptionRegex.containsMatchIn(it) }

    /** Tags that mark a title as risky for photosensitive viewers */
    private val photosensitivityRegex =
        Regex("""photosensitiv|epilep|strob|flashing|seizure""", RegexOption.IGNORE_CASE)

    fun hasPhotosensitivityTag(tags: List<String>?): Boolean = tags.orEmpty().any { photosensitivityRegex.containsMatchIn(it) }
}
