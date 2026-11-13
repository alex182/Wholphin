package com.github.damontecres.wholphin.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.ui.components.Dialog
import com.github.damontecres.wholphin.ui.tryRequestFocus

/** How long the player controls stay on screen, or null to use the regular playback setting */
fun AccessibilityPreferences.controllerTimeoutMsOrNull(): Long? = controllerTimeoutS.takeIf { it > 0 }?.let { it * 1000L }

/** How long the skip intro/credits button stays on screen */
fun AccessibilityPreferences.skipButtonTimeoutSeconds(): Int = skipButtonTimeoutS.takeIf { it > 0 } ?: DEFAULT_SKIP_BUTTON_SECONDS

const val DEFAULT_SKIP_BUTTON_SECONDS = 10

/** Scale of the player controls, 1 by default */
fun AccessibilityPreferences.playerControlsScale(): Float = (playerControlsScalePercent.takeIf { it > 0 } ?: 100).coerceIn(50, 200) / 100f

/** Whether the player overlay should draw a solid panel behind the controls */
val LocalA11yPlayerSolidBackground = staticCompositionLocalOf { false }

/** The "Player controls size" scale, so the solid panel can be tall enough for larger controls */
val LocalA11yPlayerControlsScale = staticCompositionLocalOf { 1f }

/** Where the solid panel starts, as a fraction of the screen height from the top */
fun solidPanelStart(scale: Float): Float = (1f - .4f * scale).coerceIn(.2f, .7f)

/**
 * Accessibility: makes the player's on-screen controls (buttons, seek bar, times, seek preview)
 * larger by the "Player controls size" setting. Does nothing at 100%.
 */
@Composable
fun A11yPlayerControlsScale(
    scale: Float,
    solidBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (scale == 1f && !solidBackground) {
        content()
    } else {
        val base = LocalDensity.current
        val scaled = remember(base, scale) { Density(base.density * scale, base.fontScale) }
        CompositionLocalProvider(
            LocalDensity provides scaled,
            LocalA11yPlayerSolidBackground provides solidBackground,
            LocalA11yPlayerControlsScale provides scale,
            content = content,
        )
    }
}

/**
 * Accessibility: shown before playback when "Photosensitivity warning" is on and the title is
 * tagged as containing flashing lights. Playback is paused until the viewer chooses.
 */
@Composable
fun PhotosensitivityWarningDialog(
    onContinue: () -> Unit,
    onGoBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }
    Dialog(onDismissRequest = onGoBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .widthIn(max = 480.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.a11y_photosensitivity_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.a11y_photosensitivity_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Going back is the safe choice, so it is focused first
                Button(onClick = onGoBack, modifier = Modifier.focusRequester(focusRequester)) {
                    Text(stringResource(R.string.a11y_photosensitivity_back))
                }
                Button(onClick = onContinue) {
                    Text(stringResource(R.string.a11y_photosensitivity_continue))
                }
            }
        }
    }
}
