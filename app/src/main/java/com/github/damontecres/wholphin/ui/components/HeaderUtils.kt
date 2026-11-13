package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.ui.theme.A11yLayout

object HeaderUtils {
    // Accessibility: these shrink when the interface is zoomed or text is enlarged so a row of
    // cards still fits below the header. With default settings the scales are exactly 1.
    val topPadding get() = 48.dp * A11yLayout.headerPaddingScale
    val bottomPadding get() = 32.dp * A11yLayout.headerPaddingScale
    val startPadding = 8.dp

    val padding get() = PaddingValues(top = topPadding, bottom = bottomPadding, start = startPadding)

    val height get() = 180.dp * A11yLayout.headerScale

    val logoHeight get() = 60.dp * A11yLayout.headerScale.coerceAtLeast(.6f)

    val modifier
        get() =
            Modifier
                .padding(padding)
                .height(height)
}
