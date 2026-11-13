package com.github.damontecres.wholphin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import com.github.damontecres.wholphin.preferences.ArtworkShape
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

/**
 * Accessibility: a replacement for [ContentScale.FillBounds] that only stretches artwork when
 * its shape is close to the card's. Artwork with a clearly different shape (a wide video
 * thumbnail in a tall poster card) is cropped or shown whole instead of being distorted.
 */
class NoStretchContentScale(
    private val mismatched: ContentScale,
) : ContentScale {
    override fun computeScaleFactor(
        srcSize: Size,
        dstSize: Size,
    ): ScaleFactor {
        val fill = ContentScale.FillBounds.computeScaleFactor(srcSize, dstSize)
        return if (stretchIsNoticeable(fill.scaleX, fill.scaleY)) mismatched.computeScaleFactor(srcSize, dstSize) else fill
    }

    companion object {
        /** Stretching by up to this much in one direction is not noticeable */
        const val MAX_STRETCH = 1.12f

        fun stretchIsNoticeable(
            scaleX: Float,
            scaleY: Float,
        ): Boolean {
            if (scaleX <= 0f || scaleY <= 0f || scaleX.isNaN() || scaleY.isNaN()) return false
            return maxOf(scaleX / scaleY, scaleY / scaleX) > MAX_STRETCH
        }

        val Crop = NoStretchContentScale(ContentScale.Crop)
        val Fit = NoStretchContentScale(ContentScale.Fit)
    }
}

/** Applies the "Posters with the wrong shape" setting to a card's image scaling. No change by default. */
@Composable
fun a11yContentScale(contentScale: ContentScale): ContentScale {
    if (contentScale != ContentScale.FillBounds) return contentScale
    return when (LocalAccessibility.current.artworkShape) {
        ArtworkShape.ARTWORK_CROP -> NoStretchContentScale.Crop
        ArtworkShape.ARTWORK_FIT -> NoStretchContentScale.Fit
        else -> contentScale
    }
}
