package com.github.damontecres.wholphin.a11y

import com.github.damontecres.wholphin.preferences.SubtitlePreferences
import com.github.damontecres.wholphin.preferences.resetSubtitles
import com.github.damontecres.wholphin.ui.preferences.subtitle.SubtitlePreset
import com.github.damontecres.wholphin.ui.preferences.subtitle.update
import org.junit.Assert
import org.junit.Test

class TestSubtitlePresets {
    private val defaults =
        SubtitlePreferences
            .newBuilder()
            .apply { resetSubtitles() }
            .build()

    @Test
    fun `Upstream defaults show as the Classic preset`() {
        Assert.assertEquals(SubtitlePreset.CLASSIC, SubtitlePreset.matching(defaults))
        Assert.assertEquals(SubtitlePreset.CLASSIC, SubtitlePreset.Preference.getter(defaults))
    }

    @Test
    fun `Each preset is recognised after being applied`() {
        SubtitlePreset.entries.filter { it != SubtitlePreset.CUSTOM }.forEach { preset ->
            val applied = SubtitlePreset.Preference.setter(defaults, preset)
            Assert.assertEquals(preset, SubtitlePreset.matching(applied))
        }
    }

    @Test
    fun `Presets are all different from each other`() {
        val styles = SubtitlePreset.entries.filter { it != SubtitlePreset.CUSTOM }.map { it.applyTo(defaults) }
        Assert.assertEquals(styles.size, styles.toSet().size)
    }

    @Test
    fun `Changing a setting by hand shows as Custom and Custom changes nothing`() {
        val tweaked = SubtitlePreset.HIGH_VISIBILITY.applyTo(defaults).update { fontSize = 33 }
        Assert.assertEquals(SubtitlePreset.CUSTOM, SubtitlePreset.matching(tweaked))
        Assert.assertEquals(tweaked, SubtitlePreset.CUSTOM.applyTo(tweaked))
    }

    @Test
    fun `Presets keep non-style settings`() {
        val prefs =
            defaults.update {
                useSeparateHdr = true
                imageSubtitleOpacity = 40
            }
        val applied = SubtitlePreset.LOW_VISION.applyTo(prefs)
        Assert.assertTrue(applied.useSeparateHdr)
        Assert.assertEquals(40, applied.imageSubtitleOpacity)
    }

    @Test
    fun `Accessible presets are larger than the default`() {
        Assert.assertTrue(SubtitlePreset.HIGH_VISIBILITY.applyTo(defaults).fontSize > defaults.fontSize)
        Assert.assertTrue(
            SubtitlePreset.LOW_VISION.applyTo(defaults).fontSize > SubtitlePreset.HIGH_VISIBILITY.applyTo(defaults).fontSize,
        )
    }
}
