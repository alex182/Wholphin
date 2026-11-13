package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility

/**
 * Drop-in replacement for [androidx.compose.ui.window.Dialog] that also draws the accessibility
 * focus outline inside the dialog. Dialogs are separate windows, so the app-wide
 * [FocusRingOverlay] cannot reach into them.
 *
 * Files only need to swap their `Dialog` import to use this. With the focus outline turned off,
 * this is exactly the standard dialog.
 */
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        if (LocalAccessibility.current.focusRingEnabled) {
            Box {
                content()
                FocusRingOverlay(Modifier.matchParentSize())
            }
        } else {
            content()
        }
    }
}
