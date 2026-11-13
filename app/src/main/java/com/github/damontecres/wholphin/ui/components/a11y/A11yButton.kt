package com.github.damontecres.wholphin.ui.components.a11y

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonGlow
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility

/**
 * Drop-in replacement for [androidx.tv.material3.Button] that draws the focused button in the
 * "Menu highlight color" accessibility setting. With the setting at its default it is identical.
 *
 * Use by importing this instead of the tv-material one; callers that pass their own colours are
 * unaffected.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    scale: ButtonScale = ButtonDefaults.scale(),
    glow: ButtonGlow = ButtonDefaults.glow(),
    shape: ButtonShape = ButtonDefaults.shape(),
    colors: ButtonColors = highlightButtonColors(),
    tonalElevation: Dp = 0.dp,
    border: ButtonBorder = ButtonDefaults.border(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = androidx.tv.material3.Button(
    onClick = onClick,
    modifier = modifier,
    onLongClick = onLongClick,
    enabled = enabled,
    scale = scale,
    glow = glow,
    shape = shape,
    colors = colors,
    tonalElevation = tonalElevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content,
)

@Composable
fun highlightButtonColors(): ButtonColors {
    val highlight = LocalAccessibility.current.activeHighlightColor ?: return ButtonDefaults.colors()
    return ButtonDefaults.colors(
        focusedContainerColor = highlight,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor = highlight,
        pressedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )
}
