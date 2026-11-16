package com.github.damontecres.wholphin.a11y

import com.github.damontecres.wholphin.preferences.AccessibilityPreference
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.preferences.AppPreferencesSerializer
import com.github.damontecres.wholphin.util.A11ySceneDescription
import com.github.damontecres.wholphin.util.A11ySceneDescription.Answer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.PersonKind
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class TestSceneDescription {
    private fun prefs(block: AccessibilityPreferences.Builder.() -> Unit = {}) = AccessibilityPreferences.newBuilder().apply(block).build()

    private fun config(
        scenes: Boolean = true,
        text: Boolean = true,
    ) = A11ySceneDescription.config(
        prefs {
            describeScenes = scenes
            readOnScreenText = text
            descriptionServer = "192.168.1.50"
        },
    )!!

    @Test
    fun `Nothing happens by default or without a server`() {
        Assert.assertNull(A11ySceneDescription.config(prefs()))
        Assert.assertNull(A11ySceneDescription.config(prefs { describeScenes = true }))
        Assert.assertNull(A11ySceneDescription.config(prefs { descriptionServer = "192.168.1.50" }))
    }

    @Test
    fun `Scenes and text can be on separately or together`() {
        val scenesOnly = config(text = false)
        Assert.assertTrue(scenesOnly.describeScenes)
        Assert.assertFalse(scenesOnly.readText)
        val textOnly = config(scenes = false)
        Assert.assertFalse(textOnly.describeScenes)
        Assert.assertTrue(textOnly.readText)
        Assert.assertTrue(config().run { describeScenes && readText })
        Assert.assertEquals(A11ySceneDescription.DEFAULT_MODEL, scenesOnly.model)
        Assert.assertEquals(3_000L, scenesOnly.intervalMs)
    }

    @Test
    fun `The model is told about the title unless that is turned off`() {
        val episode =
            BaseItemDto(
                id = UUID.randomUUID(),
                type = BaseItemKind.EPISODE,
                name = "Identity Crisis",
                seriesName = "Star Trek: The Next Generation",
                parentIndexNumber = 4,
                indexNumber = 18,
                productionYear = 1991,
                overview = "Geordi and a former crew mate\nbegin evolving into bizarre creatures.",
                people =
                    listOf(
                        BaseItemPerson(id = UUID.randomUUID(), name = "LeVar Burton", role = "Geordi La Forge", type = PersonKind.ACTOR),
                        BaseItemPerson(
                            id = UUID.randomUUID(),
                            name = "Maryann Plunkett",
                            role = "Susanna Leijten",
                            type = PersonKind.GUEST_STAR,
                        ),
                        BaseItemPerson(id = UUID.randomUUID(), name = "Winrich Kolbe", role = "", type = PersonKind.DIRECTOR),
                    ),
            )
        val about = A11ySceneDescription.aboutTitle(episode)!!
        Assert.assertEquals(
            "The video is \"Star Trek: The Next Generation\", season 4 episode 18, \"Identity Crisis\" (1991). " +
                "Summary: Geordi and a former crew mate begin evolving into bizarre creatures. " +
                "Characters: Geordi La Forge (played by LeVar Burton); Susanna Leijten (played by Maryann Plunkett).",
            about,
        )
        val on =
            prefs {
                describeScenes = true
                descriptionServer = "gpu"
            }
        val withInfo = A11ySceneDescription.config(on, episode)!!
        Assert.assertTrue(A11ySceneDescription.systemPrompt(withInfo).contains("Geordi La Forge (played by LeVar Burton)"))
        val without = A11ySceneDescription.config(on.toBuilder().setDescribeWithoutTitleInfo(true).build(), episode)!!
        Assert.assertNull(without.about)
        // Not sent when only text is read
        val textOnly =
            on
                .toBuilder()
                .setDescribeScenes(false)
                .setReadOnScreenText(true)
                .build()
        Assert.assertNull(A11ySceneDescription.config(textOnly, episode)!!.about)
        Assert.assertFalse(A11ySceneDescription.systemPrompt(without).contains("Geordi"))
        Assert.assertNull(A11ySceneDescription.aboutTitle(BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE)))
        Assert.assertEquals(
            "The video is \"Alien\" (1979).",
            A11ySceneDescription.aboutTitle(
                BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.MOVIE, name = "Alien", productionYear = 1979),
            ),
        )
    }

    @Test
    fun `Pictures are sent small, and sharper when text is read`() {
        Assert.assertEquals(576 to 324, config().frameSize(1920, 1080))
        Assert.assertEquals(384 to 216, config(text = false).frameSize(1920, 1080))
        // Same number of pixels whatever the shape
        val (w, h) = config().frameSize(1440, 1080)
        Assert.assertEquals(4.0 / 3, w.toDouble() / h, .01)
        Assert.assertEquals(576 * 324.0, (w * h).toDouble(), 2000.0)
    }

    @Test
    fun `A new shot is noticed, a person moving is not`() {
        val shot = IntArray(144) { 60 + it % 16 * 5 }
        Assert.assertTrue(A11ySceneDescription.pictureChanged(null, shot))
        Assert.assertFalse(A11ySceneDescription.pictureChanged(shot, shot.copyOf()))
        // Noise and a small moving area
        val moved = IntArray(144) { if (it in 40..55) 220 else shot[it] + 6 }
        Assert.assertFalse(A11ySceneDescription.pictureChanged(shot, moved))
        // A cut to a bright scene
        Assert.assertTrue(A11ySceneDescription.pictureChanged(shot, IntArray(144) { 200 }))
    }

    @Test
    fun `Server address accepts a bare address or a full URL`() {
        Assert.assertEquals("http://192.168.1.50:11434", A11ySceneDescription.serverUrl("192.168.1.50"))
        Assert.assertEquals("http://192.168.1.50:8000", A11ySceneDescription.serverUrl(" 192.168.1.50:8000/ "))
        Assert.assertEquals("http://gpu.local:11434", A11ySceneDescription.serverUrl("gpu.local"))
        Assert.assertEquals("https://ollama.example.com", A11ySceneDescription.serverUrl("https://ollama.example.com/"))
        Assert.assertEquals("http://host:11434/ollama", A11ySceneDescription.serverUrl("HTTP://host:11434/ollama"))
        Assert.assertEquals("http://[::1]:11434", A11ySceneDescription.serverUrl("[::1]"))
        Assert.assertNull(A11ySceneDescription.serverUrl(""))
        Assert.assertNull(A11ySceneDescription.serverUrl("not an address"))
    }

    @Test
    fun `Request asks only for what is turned on`() {
        fun fields(config: A11ySceneDescription.Config) =
            Json
                .parseToJsonElement(A11ySceneDescription.request(config, "abc", null))
                .jsonObject["format"]!!
                .jsonObject["required"]!!
                .jsonArray
                .map { it.jsonPrimitive.content }
        Assert.assertEquals(listOf("scene"), fields(config(text = false)))
        Assert.assertEquals(listOf("text"), fields(config(scenes = false)))
        Assert.assertEquals(listOf("scene", "text"), fields(config()))
        Assert.assertFalse(A11ySceneDescription.systemPrompt(config(scenes = false)).contains("\"scene\""))
        Assert.assertFalse(A11ySceneDescription.systemPrompt(config(text = false)).contains("\"text\""))

        val request = Json.parseToJsonElement(A11ySceneDescription.request(config(), "abc", "A man walks.")).jsonObject
        Assert.assertEquals("qwen3.5:9b", request["model"]!!.jsonPrimitive.content)
        val user = request["messages"]!!.jsonArray[1].jsonObject
        Assert.assertEquals("abc", user["images"]!!.jsonArray[0].jsonPrimitive.content)
        Assert.assertTrue(user["content"]!!.jsonPrimitive.content.contains("A man walks."))
    }

    @Test
    fun `Answers are read from the server response`() {
        val body =
            """{"model":"m","message":{"role":"assistant","content":"{\n \"scene\": \"A woman opens a door.\",\n \"text\": \"EXIT\"\n}"}}"""
        Assert.assertEquals(Answer("A woman opens a door.", "EXIT"), A11ySceneDescription.parseResponse(body))
        val fenced = """{"message":{"content":"```json\n{\"scene\":\"Rain falls.\",\"text\":\"None.\"}\n```"}}"""
        Assert.assertEquals(Answer("Rain falls.", ""), A11ySceneDescription.parseResponse(fenced))
        val list = """{"message":{"content":"{\"scene\":\"\",\"text\":[\"USS VICTORY\",\"SENSOR LOG\"]}"}}"""
        Assert.assertEquals(Answer("", "USS VICTORY SENSOR LOG"), A11ySceneDescription.parseResponse(list))
        Assert.assertNull(A11ySceneDescription.parseResponse("""{"error":"model not found"}"""))
        Assert.assertNull(A11ySceneDescription.parseResponse("<html>"))
    }

    @Test
    fun `Nothing is said twice`() {
        val narrator = A11ySceneDescription.Narrator()
        val both = config()
        Assert.assertEquals(Answer("A car stops.", "POLICE"), narrator.next(both, Answer("A car stops.", "POLICE")))
        Assert.assertEquals("A car stops.", narrator.lastScene)
        // Same scene, same sign
        Assert.assertNull(narrator.next(both, Answer("a car stops", "Police.")))
        // Nothing new in the scene, new text
        Assert.assertEquals(Answer("", "One year later"), narrator.next(both, Answer("", "One year later")))
        // A sign seen a moment ago comes back into view
        Assert.assertNull(narrator.next(both, Answer("", "POLICE")))
        Assert.assertEquals(Answer("Two men talk.", ""), narrator.next(both, Answer("Two men talk.", "---")))
        // A caption with a running clock is read once
        Assert.assertNotNull(narrator.next(both, Answer("", "USS VICTORY • AWAY TEAM SENSOR LOG STARDATE 40164.7 • 17:28:43")))
        Assert.assertNull(narrator.next(both, Answer("", "USS VICTORY • AWAY TEAM SENSOR LOG\nSTARDATE 40164.7 • 17:30:02")))
        // Different words are new
        Assert.assertNotNull(narrator.next(both, Answer("", "Two years later")))
    }

    @Test
    fun `Only the part that is turned on is spoken`() {
        val answer = Answer("A car stops.", "POLICE")
        Assert.assertEquals(Answer("A car stops.", ""), A11ySceneDescription.Narrator().next(config(text = false), answer))
        Assert.assertEquals(Answer("", "POLICE"), A11ySceneDescription.Narrator().next(config(scenes = false), answer))
    }

    @Test
    fun `Default model and interval are stored as unset`() {
        val defaults = AppPreferencesSerializer().defaultValue
        val model = AccessibilityPreference.DescriptionModel.setter(defaults, "llava")
        Assert.assertEquals("llava", AccessibilityPreference.DescriptionModel.getter(model))
        val modelBack = AccessibilityPreference.DescriptionModel.setter(model, A11ySceneDescription.DEFAULT_MODEL)
        Assert.assertEquals("", modelBack.accessibilityPreferences.descriptionModel)
        val interval = AccessibilityPreference.DescriptionInterval.setter(defaults, 30)
        Assert.assertEquals(30, interval.accessibilityPreferences.descriptionIntervalS)
        val intervalBack = AccessibilityPreference.DescriptionInterval.setter(interval, 3)
        Assert.assertEquals(0, intervalBack.accessibilityPreferences.descriptionIntervalS)
    }
}
