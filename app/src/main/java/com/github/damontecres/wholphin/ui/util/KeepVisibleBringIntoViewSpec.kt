package com.github.damontecres.wholphin.ui.util

import androidx.compose.foundation.gestures.BringIntoViewSpec
import kotlin.math.abs

/**
 * Scrolls only as far as needed to make the requested area visible, and not at all if it already
 * is. Unlike the TV default, it doesn't pivot every focused item to a fixed spot on screen.
 *
 * Useful for a page that scrolls vertically and contains horizontal rows: with the TV default,
 * every focus change in a row also asks the page to scroll, which fights any other scroll request
 * and makes the page jump when moving quickly.
 */
object KeepVisibleBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        val trailingEdge = offset + size
        return when {
            // Already fully visible
            offset >= 0 && trailingEdge <= containerSize -> 0f

            // Bigger than the container and already covering it
            offset < 0 && trailingEdge > containerSize -> 0f

            // Move whichever edge is closer
            abs(offset) < abs(trailingEdge - containerSize) -> offset

            else -> trailingEdge - containerSize
        }
    }
}
