package com.github.damontecres.wholphin.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Layout adjustments needed when the interface is zoomed (Settings > Accessibility).
 *
 * This is observable state held outside the composition so that plain objects with fixed sizes
 * (e.g. HeaderUtils) can adapt without every caller changing.
 */
object A11yLayout {
    var uiZoom by mutableFloatStateOf(1f)

    /** False when the "Stop scrolling text" setting is on; read by Modifier.enableMarquee */
    var scrollingText by mutableStateOf(true)

    /** True while the video player is on screen, where dimming around the focus is not wanted */
    var inPlayer by mutableStateOf(false)

    /** Text size multiplier including line spacing; taller text makes each row of cards taller */
    var textHeightScale by mutableFloatStateOf(1f)

    /** True when titles under cards take two lines */
    var wrapCardTitles by mutableStateOf(false)

    /** TV screens are 540dp tall before zooming */
    private const val SCREEN_HEIGHT_DP = 540f

    /** Header height and its top + bottom padding at 100% zoom */
    private const val HEADER_HEIGHT_DP = 180f
    private const val HEADER_PADDING_DP = 80f

    /** Space that must stay free below the header so one full row of cards fits */
    private const val ROW_SPACE_DP = SCREEN_HEIGHT_DP - HEADER_HEIGHT_DP - HEADER_PADDING_DP

    /** Text in a row of cards at normal size: the row's heading, a card title line, the subtitle */
    private const val ROW_HEADING_DP = 28f
    private const val CARD_TITLE_LINE_DP = 24f
    private const val CARD_SUBTITLE_DP = 20f

    private const val MIN_SCALE = .3f

    /** Extra height a row of cards needs because of larger text or two-line titles */
    private val rowTextExtraDp: Float
        get() {
            val normal = ROW_HEADING_DP + CARD_TITLE_LINE_DP + CARD_SUBTITLE_DP
            val titleLines = if (wrapCardTitles) 2 else 1
            val now = textHeightScale * (ROW_HEADING_DP + CARD_TITLE_LINE_DP * titleLines + CARD_SUBTITLE_DP)
            return (now - normal).coerceAtLeast(0f)
        }

    /** How many dp the header and its padding must give up so a full row of cards fits below */
    private val headerDeficitDp: Float
        get() {
            val available = SCREEN_HEIGHT_DP / uiZoom.coerceAtLeast(.1f) - ROW_SPACE_DP - rowTextExtraDp
            return (HEADER_HEIGHT_DP + HEADER_PADDING_DP - available).coerceAtLeast(0f)
        }

    /** The padding around page headers gives up space first. Exactly 1 with default settings. */
    val headerPaddingScale: Float
        get() = ((HEADER_PADDING_DP - headerDeficitDp) / HEADER_PADDING_DP).coerceIn(MIN_SCALE, 1f)

    /**
     * How much to shrink page headers so a full row of cards still fits on screen below them,
     * once the padding has given up what it can. Exactly 1 with default settings.
     */
    val headerScale: Float
        get() {
            val fromPadding = HEADER_PADDING_DP * (1f - headerPaddingScale)
            val remaining = (headerDeficitDp - fromPadding).coerceAtLeast(0f)
            return ((HEADER_HEIGHT_DP - remaining) / HEADER_HEIGHT_DP).coerceIn(MIN_SCALE, 1f)
        }

    /** True when there is no longer room for the multi-line description in headers */
    val compactHeaders: Boolean get() = uiZoom >= 1.2f || headerScale < 1f
}
