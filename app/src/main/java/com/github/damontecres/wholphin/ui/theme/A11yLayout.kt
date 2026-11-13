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

    /** True while the video player is on screen, where dimming around the focus is not wanted */
    var inPlayer by mutableStateOf(false)

    /** TV screens are 540dp tall before zooming */
    private const val SCREEN_HEIGHT_DP = 540f

    /** Height of the page header plus its padding at 100% zoom */
    private const val HEADER_TOTAL_DP = 260f

    /** Space that must stay free below the header so one full row of cards fits */
    private const val ROW_SPACE_DP = SCREEN_HEIGHT_DP - HEADER_TOTAL_DP

    /**
     * How much to shrink page headers so a full row of cards still fits on screen below them.
     * Exactly 1 when not zoomed.
     */
    val headerScale: Float
        get() {
            val zoom = uiZoom
            if (zoom <= 1f) return 1f
            val available = SCREEN_HEIGHT_DP / zoom - ROW_SPACE_DP
            return (available / HEADER_TOTAL_DP).coerceIn(.3f, 1f)
        }

    /** True when there is no longer room for the multi-line description in headers */
    val compactHeaders: Boolean get() = uiZoom >= 1.2f
}
