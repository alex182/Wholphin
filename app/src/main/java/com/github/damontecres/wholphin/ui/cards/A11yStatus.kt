package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility
import kotlin.math.roundToInt

/**
 * Accessibility: extra watch-progress indicators for a card. Call it after the normal progress
 * bar so it draws on top. Draws nothing unless enabled in Settings > Accessibility.
 *
 * - "High-visibility status indicators": a taller white bar on a black track, which does not
 *   depend on the theme's colours.
 * - "Show progress as a number": the percentage as text.
 */
@Composable
fun BoxScope.A11yProgressExtras(watchedPercent: Double?) {
    val a11y = LocalAccessibility.current
    if (!a11y.highVisibilityStatus && !a11y.showProgressPercent) return
    if (watchedPercent == null || watchedPercent <= 0 || watchedPercent >= 100) return
    val barHeight = Cards.playedPercentHeight + 4.dp
    if (a11y.highVisibilityStatus) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(barHeight)
                    .background(Color.Black),
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(2.dp)
                        .fillMaxHeight()
                        .fillMaxWidth((watchedPercent / 100.0).toFloat())
                        .background(Color.White),
            )
        }
    }
    if (a11y.showProgressPercent) {
        Text(
            text = "${watchedPercent.roundToInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = barHeight + 4.dp)
                    .background(Color.Black, RoundedCornerShape(25))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
