package com.github.damontecres.wholphin.a11y

import com.github.damontecres.wholphin.ui.cards.buildCardDescription
import com.github.damontecres.wholphin.ui.components.spokenLabel
import org.junit.Assert
import org.junit.Test

class TestCardDescription {
    private fun describe(
        name: String? = "The Matrix",
        details: String? = "1999",
        watched: Boolean = false,
        watchedPercent: Double? = null,
        favorite: Boolean = false,
        unwatchedCount: Int = 0,
        titleOnly: Boolean = false,
    ) = buildCardDescription(
        name = name,
        details = details,
        watched = watched,
        watchedPercent = watchedPercent,
        favorite = favorite,
        unwatchedCount = unwatchedCount,
        titleOnly = titleOnly,
        watchedLabel = "watched",
        percentWatchedLabel = { "$it percent watched" },
        favoriteLabel = "favorite",
        unwatchedLabel = { "$it unwatched" },
    )

    @Test
    fun `Title and details`() {
        Assert.assertEquals("The Matrix, 1999", describe())
    }

    @Test
    fun `Watched state`() {
        Assert.assertEquals("The Matrix, 1999, watched", describe(watched = true))
        // Fully watched wins over a leftover percentage
        Assert.assertEquals("The Matrix, 1999, watched", describe(watched = true, watchedPercent = 42.0))
    }

    @Test
    fun `Partly watched is rounded`() {
        Assert.assertEquals("The Matrix, 1999, 60 percent watched", describe(watchedPercent = 59.6))
        Assert.assertEquals("The Matrix, 1999", describe(watchedPercent = 0.0))
        Assert.assertEquals("The Matrix, 1999", describe(watchedPercent = 100.0))
    }

    @Test
    fun `Favorite and unwatched count`() {
        Assert.assertEquals(
            "Lost, 2004, 3 unwatched, favorite",
            describe(name = "Lost", details = "2004", unwatchedCount = 3, favorite = true),
        )
    }

    @Test
    fun `Title only setting`() {
        Assert.assertEquals("The Matrix", describe(watched = true, favorite = true, titleOnly = true))
    }

    @Test
    fun `Missing parts are left out`() {
        Assert.assertEquals("The Matrix", describe(details = null))
        Assert.assertEquals("The Matrix", describe(details = " "))
        // Details that only repeat the title are not read twice
        Assert.assertEquals("The Matrix", describe(details = "The Matrix"))
        Assert.assertEquals("", describe(name = null))
        Assert.assertEquals("", describe(name = ""))
    }

    @Test
    fun `Spoken label prefers the description over visible text`() {
        Assert.assertEquals("The Matrix, 1999", spokenLabel(listOf("The Matrix, 1999"), listOf("ignored"), null))
        Assert.assertEquals("Settings", spokenLabel(null, listOf("Settings"), null))
        Assert.assertEquals("Reduce motion, On", spokenLabel(null, listOf("Reduce motion"), "On"))
    }

    @Test
    fun `Spoken label skips icon glyphs and blanks`() {
        // Icon-font glyphs have no letters or digits, so they are not read out
        Assert.assertEquals("Movies", spokenLabel(null, listOf("\uf008", "Movies"), null))
        Assert.assertNull(spokenLabel(null, listOf("\uf008", " "), null))
        Assert.assertNull(spokenLabel(listOf(""), null, null))
        Assert.assertNull(spokenLabel(null, null, null))
    }
}
