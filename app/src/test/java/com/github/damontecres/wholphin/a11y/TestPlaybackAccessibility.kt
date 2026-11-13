package com.github.damontecres.wholphin.a11y

import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.services.audio
import com.github.damontecres.wholphin.services.chooseAudioDescription
import com.github.damontecres.wholphin.services.preferSdh
import com.github.damontecres.wholphin.services.subtitle
import com.github.damontecres.wholphin.ui.playback.controllerTimeoutMsOrNull
import com.github.damontecres.wholphin.ui.playback.playerControlsScale
import com.github.damontecres.wholphin.ui.playback.skipButtonTimeoutSeconds
import com.github.damontecres.wholphin.util.A11ySubtitles
import org.junit.Assert
import org.junit.Test

class TestPlaybackAccessibility {
    private fun prefs(block: AccessibilityPreferences.Builder.() -> Unit = {}) = AccessibilityPreferences.newBuilder().apply(block).build()

    @Test
    fun `Player defaults match upstream`() {
        val defaults = prefs()
        Assert.assertNull(defaults.controllerTimeoutMsOrNull())
        Assert.assertEquals(10, defaults.skipButtonTimeoutSeconds())
        Assert.assertEquals(1f, defaults.playerControlsScale())
    }

    @Test
    fun `Player settings apply`() {
        val changed =
            prefs {
                controllerTimeoutS = 30
                skipButtonTimeoutS = 60
                playerControlsScalePercent = 150
            }
        Assert.assertEquals(30_000L, changed.controllerTimeoutMsOrNull())
        Assert.assertEquals(60, changed.skipButtonTimeoutSeconds())
        Assert.assertEquals(1.5f, changed.playerControlsScale())
    }

    @Test
    fun `Sound cues are found`() {
        fun cues(text: String) = A11ySubtitles.findSoundCues(text).map { text.substring(it) }
        Assert.assertEquals(listOf("[door slams]"), cues("[door slams] Who's there?"))
        Assert.assertEquals(listOf("(laughing)"), cues("Stop it! (laughing)"))
        Assert.assertEquals(listOf("♪ upbeat music ♪"), cues("♪ upbeat music ♪"))
        Assert.assertEquals(listOf("[SIGHS]", "[PHONE RINGS]"), cues("[SIGHS] Fine.\n[PHONE RINGS]"))
        Assert.assertEquals(emptyList<String>(), cues("Just ordinary speech."))
    }

    @Test
    fun `Cues are untouched when the options are off`() {
        val before = A11ySubtitles.atTop to A11ySubtitles.soundCueStyle
        try {
            A11ySubtitles.update(prefs())
            val cues = emptyList<androidx.media3.common.text.Cue>()
            Assert.assertSame(cues, A11ySubtitles.transform(cues))
        } finally {
            A11ySubtitles.atTop = before.first
            A11ySubtitles.soundCueStyle = before.second
        }
    }

    @Test
    fun `Audio description titles are recognised`() {
        listOf("English - Audio Description", "Descriptive Audio", "English AD 5.1", "Audio-described", "DVS", "For the visually impaired")
            .forEach { Assert.assertTrue(it, A11ySubtitles.isAudioDescription(it)) }
        listOf("English 5.1", "Director's Commentary", "Adventure mix", "Dolby Digital", null)
            .forEach { Assert.assertFalse("$it", A11ySubtitles.isAudioDescription(it)) }
    }

    @Test
    fun `Audio description is chosen in the preferred language`() {
        val streams =
            listOf(
                audio(0, "eng", default = true, title = "English 5.1"),
                audio(1, "eng", title = "English - Audio Description"),
                audio(2, "spa", title = "Spanish Audio Description"),
            )
        Assert.assertEquals(1, chooseAudioDescription(streams, "eng")?.index)
        Assert.assertEquals(2, chooseAudioDescription(streams, "spa")?.index)
        // No described track in French: leave the normal choice alone
        Assert.assertNull(chooseAudioDescription(streams, "fra"))
        // No language preference: take the first described track
        Assert.assertEquals(1, chooseAudioDescription(streams, null)?.index)
        Assert.assertNull(chooseAudioDescription(streams.take(1), "eng"))
    }

    @Test
    fun `SDH subtitles in the preferred language move to the front`() {
        val streams =
            listOf(
                subtitle(0, "eng", default = true),
                subtitle(1, "eng", forced = true),
                subtitle(2, "eng", sdh = true),
                subtitle(3, "spa", sdh = true),
            )
        Assert.assertEquals(listOf(2, 0, 1, 3), preferSdh(streams, "eng").map { it.index })
        // Nothing changes when there is no SDH track in the preferred language
        Assert.assertEquals(listOf(0, 1, 2, 3), preferSdh(streams, "fra").map { it.index })
    }

    @Test
    fun `Photosensitivity tags are recognised`() {
        Assert.assertTrue(A11ySubtitles.hasPhotosensitivityTag(listOf("thriller", "Photosensitive warning")))
        Assert.assertTrue(A11ySubtitles.hasPhotosensitivityTag(listOf("strobe lights")))
        Assert.assertTrue(A11ySubtitles.hasPhotosensitivityTag(listOf("epilepsy")))
        Assert.assertFalse(A11ySubtitles.hasPhotosensitivityTag(listOf("comedy", "family")))
        Assert.assertFalse(A11ySubtitles.hasPhotosensitivityTag(null))
    }
}
