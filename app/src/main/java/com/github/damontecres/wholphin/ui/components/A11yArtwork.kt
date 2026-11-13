package com.github.damontecres.wholphin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility

/**
 * Accessibility: the colour change for posters, banners and backdrops when the user has asked
 * for dimmer or less colourful artwork. Returns null (no change) by default.
 */
fun artworkColorMatrix(
    dim: Float,
    desaturate: Float,
): ColorMatrix? {
    if (dim <= 0f && desaturate <= 0f) return null
    val brightness = (1f - dim).coerceIn(0f, 1f)
    val matrix = ColorMatrix().apply { setToSaturation((1f - desaturate).coerceIn(0f, 1f)) }
    // Scale the red, green and blue rows (including their offsets) to darken
    for (row in 0..2) {
        for (column in 0..4) {
            matrix[row, column] = matrix[row, column] * brightness
        }
    }
    return matrix
}

@Composable
fun rememberArtworkColorFilter(): ColorFilter? {
    val a11y = LocalAccessibility.current
    return remember(a11y.artworkDim, a11y.artworkDesaturate) {
        artworkColorMatrix(a11y.artworkDim, a11y.artworkDesaturate)?.let { ColorFilter.colorMatrix(it) }
    }
}
