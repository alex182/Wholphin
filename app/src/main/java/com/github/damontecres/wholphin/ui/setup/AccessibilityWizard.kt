package com.github.damontecres.wholphin.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AccessibilityPreference
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.ui.preferences.ComposablePreference
import com.github.damontecres.wholphin.ui.tryRequestFocus

/** The settings offered by the first-run accessibility setup, in order */
val accessibilityWizardPreferences: List<AppPreference<AppPreferences, out Any?>> =
    listOf(
        AccessibilityPreference.TextScale,
        AccessibilityPreference.UiZoom,
        AccessibilityPreference.HighContrast,
        AccessibilityPreference.FocusRingEnabled,
        AccessibilityPreference.PlainTextTitles,
        AccessibilityPreference.ReduceMotion,
    )

/**
 * Shown once on first launch so that people who need larger text or higher contrast can set
 * that up before anything else. Each choice applies immediately, so the screen itself is the
 * preview. It only changes the normal settings under Settings > Accessibility.
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun AccessibilityWizard(
    preferences: AppPreferences,
    onUpdate: (update: (AppPreferences) -> AppPreferences) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { doneFocusRequester.tryRequestFocus() }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 24.dp),
            modifier =
                Modifier
                    .fillMaxWidth(.6f)
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
        ) {
            item {
                Text(
                    text = stringResource(R.string.a11y_wizard_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.a11y_wizard_body),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                // First in the list and focused at the start, so one press continues without changes
                Button(
                    onClick = onDone,
                    modifier = Modifier.focusRequester(doneFocusRequester),
                ) {
                    Text(
                        text = stringResource(R.string.a11y_wizard_done),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }
            items(accessibilityWizardPreferences) { pref ->
                pref as AppPreference<AppPreferences, Any?>
                ComposablePreference(
                    preference = pref,
                    value = pref.getter(preferences),
                    onValueChange = { newValue -> onUpdate { pref.setter(it, newValue) } },
                    onNavigate = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
