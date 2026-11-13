package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility

/**
 * Draws a thick, high-contrast outline around whichever element currently has focus.
 *
 * Instead of adding a modifier to every focusable component, this asks the hosting view for the
 * bounds of the focused element on each frame and draws on top of everything. This means it covers
 * every screen (including ones added upstream later) from a single place.
 *
 * Does nothing unless enabled in Settings > Accessibility.
 */
@Composable
fun FocusRingOverlay(modifier: Modifier = Modifier.fillMaxSize()) {
    val a11y = LocalAccessibility.current
    if (!a11y.focusRingEnabled) return

    val view = LocalView.current
    var focusRect by remember { mutableStateOf<Rect?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(view) {
        val rect = android.graphics.Rect()
        while (true) {
            withFrameNanos { }
            val newRect =
                if (view.hasFocus() && view.hasWindowFocus()) {
                    view.getFocusedRect(rect)
                    focusRingRect(
                        left = rect.left,
                        top = rect.top,
                        right = rect.right,
                        bottom = rect.bottom,
                        viewWidth = view.width,
                        viewHeight = view.height,
                    )
                } else {
                    null
                }
            if (newRect != focusRect) {
                focusRect = newRect
            }
        }
    }

    val color = a11y.focusRingColor ?: MaterialTheme.colorScheme.border
    Canvas(
        modifier =
            modifier
                .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        focusRect?.let {
            drawFocusRing(
                rect = it.translate(-origin.x, -origin.y),
                color = color,
                thickness = a11y.focusRingThickness,
            )
        }
    }
}

/**
 * Decides whether a focused element's bounds should get a ring. Returns null for elements that
 * are invisible placeholders (zero size) or that cover most of the screen (e.g. the video player
 * while the controls are hidden).
 */
fun focusRingRect(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    viewWidth: Int,
    viewHeight: Int,
): Rect? {
    val width = right - left
    val height = bottom - top
    if (width < MIN_SIZE_PX || height < MIN_SIZE_PX) return null
    if (viewWidth <= 0 || viewHeight <= 0) return null
    // Something covering most of the screen is a container (a page that is still loading, or the
    // video player), not an item. Outlining it would not help anyone find the focus.
    val areaFraction = (width.toFloat() * height) / (viewWidth.toFloat() * viewHeight)
    if (areaFraction >= MAX_AREA_FRACTION) return null
    return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}

private const val MIN_SIZE_PX = 4
private const val MAX_AREA_FRACTION = .5f

private val RingGap = 2.dp
private val RingContrastStroke = 2.dp
private val RingCornerRadius = 10.dp

private fun DrawScope.drawFocusRing(
    rect: Rect,
    color: Color,
    thickness: Dp,
) {
    val gap = RingGap.toPx()
    val stroke = thickness.toPx()
    val contrast = RingContrastStroke.toPx()
    val baseRadius = RingCornerRadius.toPx()

    // A dark line on both sides of the colored ring keeps it visible on any artwork
    val total = stroke + contrast * 2
    val outset = gap + total / 2
    val contrastColor = if (color.luminanceIsHigh()) Color.Black else Color.White

    fun ring(
        width: Float,
        ringColor: Color,
    ) {
        drawRoundRect(
            color = ringColor,
            topLeft = Offset(rect.left - outset, rect.top - outset),
            size = Size(rect.width + outset * 2, rect.height + outset * 2),
            cornerRadius = CornerRadius(baseRadius + outset),
            style = Stroke(width = width),
        )
    }
    ring(total, contrastColor)
    ring(stroke, color)
}

private fun Color.luminanceIsHigh(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.4f
