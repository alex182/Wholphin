package com.github.damontecres.wholphin.util

import android.view.KeyEvent

/**
 * Accessibility: slows down how fast focus moves while a direction button is held, by dropping
 * some of the repeated key events. The first press always goes through.
 */
object KeyRepeatThrottle {
    /** Minimum time between accepted repeats, or 0 for upstream behaviour (no throttling) */
    @Volatile
    var intervalMs: Long = 0

    private var lastAcceptedMs: Long = 0

    private val directionKeys =
        setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        )

    fun shouldDrop(event: KeyEvent): Boolean =
        shouldDrop(
            isDirectionKeyDown = event.action == KeyEvent.ACTION_DOWN && event.keyCode in directionKeys,
            repeatCount = event.repeatCount,
            eventTimeMs = event.eventTime,
        )

    fun shouldDrop(
        isDirectionKeyDown: Boolean,
        repeatCount: Int,
        eventTimeMs: Long,
    ): Boolean {
        val interval = intervalMs
        if (interval <= 0 || !isDirectionKeyDown) return false
        if (repeatCount == 0) {
            lastAcceptedMs = eventTimeMs
            return false
        }
        if (eventTimeMs - lastAcceptedMs < interval) return true
        lastAcceptedMs = eventTimeMs
        return false
    }
}
