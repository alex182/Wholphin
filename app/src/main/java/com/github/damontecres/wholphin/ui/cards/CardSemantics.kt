package com.github.damontecres.wholphin.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.theme.LocalAccessibility
import kotlin.math.roundToInt

/**
 * Accessibility: what a screen reader says for a media card.
 *
 * Poster cards usually show only artwork, so without this TalkBack has nothing to read.
 * The words for the status parts are passed in so this stays a plain function that is easy to test.
 */
fun buildCardDescription(
    name: String?,
    details: String?,
    watched: Boolean,
    watchedPercent: Double?,
    favorite: Boolean,
    unwatchedCount: Int,
    titleOnly: Boolean,
    watchedLabel: String,
    percentWatchedLabel: (Int) -> String,
    favoriteLabel: String,
    unwatchedLabel: (Int) -> String,
): String {
    val title = name?.takeIf { it.isNotBlank() } ?: return ""
    if (titleOnly) return title
    return buildList {
        add(title)
        details?.takeIf { it.isNotBlank() && it != title }?.let { add(it) }
        val percent = watchedPercent?.roundToInt() ?: 0
        when {
            watched -> add(watchedLabel)
            percent in 1..99 -> add(percentWatchedLabel(percent))
        }
        if (unwatchedCount > 0) add(unwatchedLabel(unwatchedCount))
        if (favorite) add(favoriteLabel)
    }.joinToString(", ")
}

/**
 * Builds the screen reader description for a card using the user's "Card announcements" setting
 */
@Composable
fun rememberCardDescription(
    name: String?,
    details: String?,
    watched: Boolean,
    watchedPercent: Double?,
    favorite: Boolean,
    unwatchedCount: Int = 0,
): String {
    val titleOnly = LocalAccessibility.current.cardAnnouncementsTitleOnly
    val watchedLabel = stringResource(R.string.a11y_watched)
    val favoriteLabel = stringResource(R.string.a11y_favorite)
    val percentTemplate = stringResource(R.string.a11y_percent_watched)
    val unwatchedTemplate = stringResource(R.string.a11y_unwatched_count)
    return remember(name, details, watched, watchedPercent, favorite, unwatchedCount, titleOnly) {
        buildCardDescription(
            name = name,
            details = details,
            watched = watched,
            watchedPercent = watchedPercent,
            favorite = favorite,
            unwatchedCount = unwatchedCount,
            titleOnly = titleOnly,
            watchedLabel = watchedLabel,
            percentWatchedLabel = { percentTemplate.format(it) },
            favoriteLabel = favoriteLabel,
            unwatchedLabel = { unwatchedTemplate.format(it) },
        )
    }
}

/**
 * Sets what a screen reader says for this card. Does nothing for an empty description.
 *
 * Everything inside the card (icon glyphs, count badges, fallback text) is replaced by the
 * description, which already covers it.
 */
fun Modifier.cardDescription(description: String): Modifier =
    if (description.isEmpty()) this else clearAndSetSemantics { contentDescription = description }
