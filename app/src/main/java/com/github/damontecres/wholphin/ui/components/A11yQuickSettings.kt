package com.github.damontecres.wholphin.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.QuickShortcut
import com.github.damontecres.wholphin.ui.preferences.ComposablePreference
import com.github.damontecres.wholphin.ui.setup.accessibilityWizardPreferences
import com.github.damontecres.wholphin.ui.theme.A11yLayout
import com.github.damontecres.wholphin.ui.tryRequestFocus

/**
 * Accessibility: a small panel of the most useful accessibility settings that opens from
 * anywhere by holding a remote button (chosen in Settings > Accessibility, off by default).
 */
object A11yQuickSettings {
    var visible by mutableStateOf(false)

    /** Set from the user's preferences */
    @Volatile
    var shortcut: QuickShortcut = QuickShortcut.QUICK_SHORTCUT_OFF

    /**
     * Returns true if this key event opened the panel and should not be handled further.
     */
    fun handleKey(event: KeyEvent): Boolean =
        handleKey(
            keyCode = event.keyCode,
            isLongPress = event.action == KeyEvent.ACTION_DOWN && event.isLongPress,
            inPlayer = A11yLayout.inPlayer,
        )

    fun handleKey(
        keyCode: Int,
        isLongPress: Boolean,
        inPlayer: Boolean,
    ): Boolean {
        if (!isLongPress || visible) return false
        val wanted =
            when (shortcut) {
                QuickShortcut.QUICK_SHORTCUT_MENU -> keyCode == KeyEvent.KEYCODE_MENU

                QuickShortcut.QUICK_SHORTCUT_INFO -> keyCode == KeyEvent.KEYCODE_INFO

                // Holding play/pause is left to the player while a video is on screen
                QuickShortcut.QUICK_SHORTCUT_PLAY_PAUSE -> !inPlayer && keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE

                else -> false
            }
        if (wanted) visible = true
        return wanted
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun A11yQuickSettingsDialog(
    preferences: AppPreferences,
    onUpdate: (update: (AppPreferences) -> AppPreferences) -> Unit,
) {
    if (!A11yQuickSettings.visible) return
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }
    Dialog(onDismissRequest = { A11yQuickSettings.visible = false }) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .width(420.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.a11y_quick_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.heightIn(max = 380.dp),
            ) {
                itemsIndexed(accessibilityWizardPreferences) { index, pref ->
                    pref as AppPreference<AppPreferences, Any?>
                    ComposablePreference(
                        preference = pref,
                        value = pref.getter(preferences),
                        onValueChange = { newValue -> onUpdate { pref.setter(it, newValue) } },
                        onNavigate = {},
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                }
            }
        }
    }
}
