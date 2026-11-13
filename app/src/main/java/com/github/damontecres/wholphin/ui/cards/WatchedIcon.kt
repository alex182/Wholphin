package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.PreviewTvSpec
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility
import com.github.damontecres.wholphin.ui.theme.LocalTheme
import com.github.damontecres.wholphin.ui.theme.WholphinTheme

@Composable
fun WatchedIcon(
    modifier: Modifier = Modifier,
    padding: Dp = 2.dp,
) {
    // Accessibility: "High-visibility status indicators" uses plain black and white with a
    // thick outline, so the mark does not depend on the theme's colours
    val highVisibility = LocalAccessibility.current.highVisibilityStatus
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = if (highVisibility) Color.White else WatchedIconColor(),
        modifier =
            modifier
                .background(if (highVisibility) Color.Black else WatchedIconBackground(), shape = CircleShape)
                .border(if (highVisibility) 2.dp else .5.dp, if (highVisibility) Color.White else Color.Black, CircleShape)
                .padding(padding),
    )
}

@Composable
fun WatchedIconBackground(): Color =
    when (LocalTheme.current) {
        AppThemeColors.UNRECOGNIZED,
        AppThemeColors.PURPLE,
        AppThemeColors.BLUE,
        AppThemeColors.GREEN,
        AppThemeColors.ORANGE,
        AppThemeColors.BOLD_BLUE,
        AppThemeColors.RED,
        AppThemeColors.BROWN,
        -> MaterialTheme.colorScheme.border.copy(alpha = 1f)

        AppThemeColors.OLED_BLACK -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
fun WatchedIconColor(): Color =
    when (LocalTheme.current) {
        AppThemeColors.UNRECOGNIZED,
        AppThemeColors.PURPLE,
        AppThemeColors.BLUE,
        AppThemeColors.GREEN,
        AppThemeColors.ORANGE,
        AppThemeColors.BOLD_BLUE,
        AppThemeColors.OLED_BLACK,
        AppThemeColors.RED,
        AppThemeColors.BROWN,
        -> Color.White // MaterialTheme.colorScheme.onSurface
    }

@PreviewTvSpec
@Composable
private fun WatchedIconPreview() {
    WholphinTheme {
        WatchedIcon(Modifier.size(64.dp))
    }
}
