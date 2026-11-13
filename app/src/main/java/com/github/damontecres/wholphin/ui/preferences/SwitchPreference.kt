package com.github.damontecres.wholphin.ui.preferences

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchColors
import androidx.tv.material3.SwitchDefaults
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility
import com.github.damontecres.wholphin.ui.theme.LocalTheme

@Composable
fun SwitchPreference(
    title: String,
    value: Boolean,
    onClick: () -> Unit,
    summaryOn: String?,
    summaryOff: String?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = SwitchPreference(
    title = title,
    value = value,
    onClick = onClick,
    modifier = modifier,
    summary = if (value) summaryOn else summaryOff,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
)

@Composable
fun SwitchPreference(
    title: String,
    value: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    ListItem(
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        headlineContent = {
            PreferenceTitle(title)
        },
        supportingContent = {
            PreferenceSummary(summary)
        },
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = { onClick.invoke() },
                colors = SwitchColors(),
            )
        },
        interactionSource = interactionSource,
        modifier = modifier,
    )
}

@Composable
fun SwitchColors(): SwitchColors {
    if (LocalAccessibility.current.highContrastColors != null) {
        // Accessibility: high-contrast modes only use the theme's two colours. The row is drawn
        // inverted when focused, so take the colours from the current content colour.
        val scheme = MaterialTheme.colorScheme
        val content = LocalContentColor.current
        val behind = if (content == scheme.inverseOnSurface) scheme.inverseSurface else scheme.surface
        return SwitchDefaults.colors(
            checkedThumbColor = behind,
            checkedTrackColor = content,
            checkedBorderColor = content,
            uncheckedThumbColor = content,
            uncheckedTrackColor = behind,
            uncheckedBorderColor = content,
        )
    }
    val theme = LocalTheme.current
    return when (theme) {
        AppThemeColors.UNRECOGNIZED,
        AppThemeColors.PURPLE,
        AppThemeColors.BLUE,
        -> {
            SwitchDefaults.colors()
        }

        AppThemeColors.GREEN,
        AppThemeColors.ORANGE,
        AppThemeColors.BOLD_BLUE,
        AppThemeColors.OLED_BLACK,
        AppThemeColors.RED,
        AppThemeColors.BROWN,
        -> {
            SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
