package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.MotionDurationScale

/**
 * Controls the speed of every animation in the app.
 *
 * Normally Compose follows the system "animator duration scale" developer setting. This replaces
 * that so the app can also turn animations off from Settings > Accessibility > Reduce motion.
 * A scale of zero makes animations jump straight to their end state.
 */
object A11yMotionDurationScale : MotionDurationScale {
    /** Set from the user's accessibility preferences */
    @Volatile
    var reduceMotion: Boolean = false

    /** The system animator duration scale, refreshed when the activity resumes */
    @Volatile
    var systemScale: Float = 1f

    override val scaleFactor: Float
        get() = if (reduceMotion) 0f else systemScale
}
