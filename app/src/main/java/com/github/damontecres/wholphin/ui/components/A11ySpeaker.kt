package com.github.damontecres.wholphin.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import com.github.damontecres.wholphin.ui.theme.A11yLayout
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Accessibility: "Speak selected item" reads the focused item aloud using the device's
 * text-to-speech voice, for people who want spoken labels without running a full screen reader.
 *
 * It reads the same description a screen reader would. It stays silent while TalkBack is
 * running (TalkBack already speaks) and during video playback.
 */
@Composable
fun SpeakFocusedItem() {
    if (!LocalAccessibility.current.speakFocusedItem) return
    val context = LocalContext.current
    val view = LocalView.current
    val speaker = remember { Speaker(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }
    LaunchedEffect(view) {
        var lastSeen: String? = null
        var lastSpoken: String? = null
        while (true) {
            delay(POLL_MS)
            if (A11yLayout.inPlayer || isScreenReaderRunning(context)) {
                lastSeen = null
                continue
            }
            val label =
                runCatching { focusedLabel(view as? RootForTest) }
                    .onFailure { Timber.w(it, "Could not read the focused item") }
                    .getOrNull()
            // Speak once the focus has rested on the same item for one more poll
            if (label != null && label == lastSeen && label != lastSpoken) {
                speaker.speak(label)
                lastSpoken = label
            }
            if (label == null) lastSpoken = null
            lastSeen = label
        }
    }
}

private const val POLL_MS = 350L

fun isScreenReaderRunning(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    return manager?.isEnabled == true && manager.isTouchExplorationEnabled
}

private fun focusedLabel(root: RootForTest?): String? {
    root ?: return null
    val focused =
        root.semanticsOwner
            .getAllSemanticsNodes(mergingEnabled = true)
            .lastOrNull { it.config.getOrNull(SemanticsProperties.Focused) == true }
            ?: return null
    return labelOf(focused.config)
}

/** What to say for an element: its description if it has one, otherwise its visible text */
fun labelOf(config: SemanticsConfiguration): String? =
    spokenLabel(
        descriptions = config.getOrNull(SemanticsProperties.ContentDescription),
        texts = config.getOrNull(SemanticsProperties.Text)?.map { it.text },
        state = config.getOrNull(SemanticsProperties.StateDescription),
    )

fun spokenLabel(
    descriptions: List<String>?,
    texts: List<String>?,
    state: String?,
): String? {
    val main =
        descriptions?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?: texts?.filter { it.isNotBlank() && it.any { c -> c.isLetterOrDigit() } }?.takeIf { it.isNotEmpty() }
    val parts = main.orEmpty() + listOfNotNull(state?.takeIf { it.isNotBlank() })
    return parts.joinToString(", ").takeIf { it.isNotBlank() }
}

private class Speaker(
    context: Context,
) {
    private var ready = false
    private var pending: String? = null
    private val tts: TextToSpeech =
        TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (!ready) Timber.w("Text-to-speech is not available (status %s)", status)
            pending?.takeIf { ready }?.let { speak(it) }
            pending = null
        }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        Timber.d("Speaking focused item: %s", text)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "a11y-focus")
    }

    fun shutdown() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }
}
