package com.github.damontecres.wholphin.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.BuiltInKeyboard
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility

/**
 * Accessibility: an on-screen keyboard drawn by the app itself.
 *
 * With TalkBack on, Android TV's own on-screen keyboard cannot be reached from a text field in
 * this app: the direction buttons move TalkBack's focus between the app's controls and never
 * enter the keyboard. This keyboard is made of ordinary buttons, so a screen reader can move
 * between the keys and read each one, and it follows the app's text size, zoom and contrast.
 */
object A11yKeyboardLayout {
    val digits = "1234567890".map { it.toString() }
    val letters =
        listOf(
            "qwertyuiop".map { it.toString() },
            "asdfghjkl'".map { it.toString() },
            "zxcvbnm,.?".map { it.toString() },
        )
    val symbols =
        listOf(
            "!@#$%^&*()".map { it.toString() },
            "-_=+[]{};\"".map { it.toString() },
            "~`\\|<>€£¥•".map { it.toString() },
        )

    /** Always visible: the characters needed for server addresses and e-mail style user names */
    val common = listOf(".", ":", "/", "-", "_", "@")

    /** What [typed] does to [text]; null [typed] means delete the last character */
    fun apply(
        text: String,
        typed: String?,
    ): String = if (typed == null) text.dropLast(1) else text + typed
}

/** Whether a screen reader such as TalkBack is running, kept up to date while on screen */
@Composable
fun rememberScreenReaderRunning(): Boolean {
    val context = LocalContext.current
    var running by remember { mutableStateOf(isScreenReaderRunning(context)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { running = isScreenReaderRunning(context) }
        manager?.addTouchExplorationStateChangeListener(listener)
        running = isScreenReaderRunning(context)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return running
}

/** Whether text fields should use the built-in keyboard instead of the system one */
@Composable
fun useBuiltInKeyboard(): Boolean =
    when (LocalAccessibility.current.builtInKeyboard) {
        BuiltInKeyboard.BUILT_IN_KEYBOARD_ALWAYS -> true
        BuiltInKeyboard.BUILT_IN_KEYBOARD_NEVER -> false
        else -> rememberScreenReaderRunning()
    }

/**
 * Stands in for a text field when the built-in keyboard is in use: a button showing the current
 * text which opens [BuiltInKeyboardDialog] when selected.
 */
@Composable
fun BuiltInKeyboardField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    minHeight: Dp = 40.dp,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    val shown = if (isPassword) "•".repeat(text.length) else text
    val spoken =
        when {
            text.isEmpty() -> stringResource(R.string.a11y_keyboard_field_empty)
            isPassword -> stringResource(R.string.a11y_keyboard_field_password, text.length)
            else -> stringResource(R.string.a11y_keyboard_field, text)
        }
    Surface(
        onClick = { showKeyboard = true },
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(if (maxLines > 1) RoundedCornerShape(8.dp) else CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor =
                    if (isError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = .75f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)
                    },
                contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        interactionSource = interactionSource,
        modifier =
            modifier
                .defaultMinSize(minWidth = 280.dp, minHeight = minHeight)
                .semantics { contentDescription = spoken },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            leadingIcon?.invoke()
            Text(
                text = shown,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (showKeyboard) {
        BuiltInKeyboardDialog(
            text = text,
            onTextChange = onTextChange,
            isPassword = isPassword,
            onDismissRequest = { showKeyboard = false },
            onDone = {
                showKeyboard = false
                onDone?.invoke()
            },
        )
    }
}

@Composable
fun BuiltInKeyboardDialog(
    text: String,
    onTextChange: (String) -> Unit,
    isPassword: Boolean,
    onDismissRequest: () -> Unit,
    onDone: () -> Unit,
) {
    val currentText by rememberUpdatedState(text)
    var shift by remember { mutableStateOf(false) }
    var symbols by remember { mutableStateOf(false) }
    val view = LocalView.current
    val deletedLabel = stringResource(R.string.a11y_keyboard_deleted)
    val spaceLabel = stringResource(R.string.a11y_keyboard_space)
    val capitalLabel = stringResource(R.string.a11y_keyboard_capital)
    val keyboardTitle = stringResource(R.string.a11y_keyboard_title)
    val doneLabel =
        when {
            text.isEmpty() -> stringResource(R.string.a11y_keyboard_done)
            isPassword -> stringResource(R.string.a11y_keyboard_done_password, text.length)
            else -> stringResource(R.string.a11y_keyboard_done_text, text)
        }

    fun type(character: String?) {
        if (character == null && currentText.isEmpty()) return
        onTextChange(A11yKeyboardLayout.apply(currentText, character))
        // Echo what happened, as the system keyboard does. Passwords are never read back.
        val echo =
            when {
                character == null -> deletedLabel
                isPassword -> null
                character == " " -> spaceLabel
                else -> character
            }
        @Suppress("DEPRECATION")
        echo?.let { view.announceForAccessibility(it) }
    }

    val firstKey = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(KeySpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .padding(bottom = 16.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        // Announced by screen readers when the keyboard opens
                        .semantics { paneTitle = keyboardTitle },
            ) {
                // What has been typed so far. Hidden from screen readers so their focus starts on
                // the first key, where the remote's focus is; the "Done" key reads the text back.
                Text(
                    text = if (isPassword) "•".repeat(text.length) else text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .width(KeySize * 10 + KeySpacing * 9)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clearAndSetSemantics { },
                )
                KeyRow {
                    A11yKeyboardLayout.digits.forEachIndexed { index, key ->
                        CharacterKey(
                            key,
                            onClick = { type(key) },
                            modifier = if (index == 0) Modifier.focusRequester(firstKey) else Modifier,
                        )
                    }
                }
                val rows = if (symbols) A11yKeyboardLayout.symbols else A11yKeyboardLayout.letters
                rows.forEach { row ->
                    KeyRow {
                        row.forEach { key ->
                            val character = if (shift && !symbols) key.uppercase() else key
                            CharacterKey(
                                character,
                                onClick = { type(character) },
                                description = if (shift && !symbols && key != character) "$capitalLabel $character" else null,
                            )
                        }
                    }
                }
                KeyRow {
                    ActionKey(
                        label = stringResource(if (shift) R.string.a11y_keyboard_shift_on else R.string.a11y_keyboard_shift),
                        short = "⇧",
                        highlighted = shift,
                        onClick = { shift = !shift },
                    )
                    ActionKey(
                        label = stringResource(if (symbols) R.string.a11y_keyboard_letters else R.string.a11y_keyboard_symbols),
                        short = if (symbols) "abc" else "#+=",
                        onClick = { symbols = !symbols },
                    )
                    A11yKeyboardLayout.common.forEach { key -> CharacterKey(key, onClick = { type(key) }) }
                    ActionKey(label = spaceLabel, short = "␣", onClick = { type(" ") })
                    ActionKey(label = stringResource(R.string.a11y_keyboard_delete), short = "⌫", onClick = { type(null) })
                }
                Surface(
                    onClick = onDone,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                    modifier =
                        Modifier
                            .width(KeySize * 10 + KeySpacing * 9)
                            .semantics { contentDescription = doneLabel },
                ) {
                    Text(
                        text = stringResource(R.string.a11y_keyboard_done),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }
}

private val KeySize = 40.dp
private val KeySpacing = 4.dp

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KeySpacing)) { content() }
}

@Composable
private fun CharacterKey(
    character: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier =
            modifier
                .defaultMinSize(minWidth = KeySize, minHeight = KeySize)
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.defaultMinSize(KeySize, KeySize)) {
            Text(
                text = character,
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
        }
    }
}

@Composable
private fun ActionKey(
    label: String,
    short: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors =
            if (highlighted) {
                ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                ClickableSurfaceDefaults.colors()
            },
        modifier =
            Modifier
                .defaultMinSize(minWidth = KeySize, minHeight = KeySize)
                .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.defaultMinSize(KeySize, KeySize)) {
            Text(
                text = short,
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
        }
    }
}
