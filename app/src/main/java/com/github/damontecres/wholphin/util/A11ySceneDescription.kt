package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PersonKind
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Accessibility: "Describe scenes aloud" and "Read on-screen text aloud".
 *
 * During playback the app looks at the picture every few seconds, sends the frame to an Ollama
 * server running a vision model, and speaks the answer. The two settings are independent: one
 * request asks for whichever of them is on.
 *
 * This file is the part with no Android in it: settings, the request and what to say.
 */
object A11ySceneDescription {
    const val DEFAULT_MODEL = "qwen3.5:9b"
    const val DEFAULT_INTERVAL_S = 3
    const val DEFAULT_PORT = 11434

    /** What was asked for, resolved from the stored settings; null when there is nothing to do */
    data class Config(
        val describeScenes: Boolean,
        val readText: Boolean,
        /** Base URL of the Ollama server, without a trailing slash */
        val serverUrl: String,
        val model: String,
        val intervalMs: Long,
        /** What is known about the title being played, to help name people and places; see [aboutTitle] */
        val about: String? = null,
    ) {
        /**
         * The size to send a picture of the given shape at. The time a vision model takes grows
         * faster than the number of pixels (on an RTX 3080 about 4 seconds for 768x432, 2.5 for
         * 512x288 and under 2 for 384x216), and reading text needs a sharper picture than
         * describing a scene.
         */
        fun frameSize(
            width: Int,
            height: Int,
        ): Pair<Int, Int> {
            val pixels = if (readText) 576 * 324 else 384 * 216
            val aspect = width.coerceAtLeast(1).toDouble() / height.coerceAtLeast(1)
            val w = sqrt(pixels * aspect).roundToInt().coerceAtLeast(16)
            return w to (w / aspect).roundToInt().coerceAtLeast(16)
        }
    }

    fun config(
        prefs: AccessibilityPreferences,
        item: BaseItemDto? = null,
    ): Config? {
        if (!prefs.describeScenes && !prefs.readOnScreenText) return null
        val url = serverUrl(prefs.descriptionServer) ?: return null
        return Config(
            describeScenes = prefs.describeScenes,
            readText = prefs.readOnScreenText,
            serverUrl = url,
            model = prefs.descriptionModel.trim().ifEmpty { DEFAULT_MODEL },
            // Only scene descriptions use names; reading text is quicker without the extra words
            about = item?.takeIf { prefs.describeScenes && !prefs.describeWithoutTitleInfo }?.let(::aboutTitle),
            intervalMs = (prefs.descriptionIntervalS.takeIf { it > 0 } ?: DEFAULT_INTERVAL_S).coerceIn(1, 600) * 1000L,
        )
    }

    /**
     * Turns what was typed into the server setting into a base URL. A bare address such as
     * "192.168.1.50" gets http and Ollama's port. Returns null if nothing usable was entered.
     */
    fun serverUrl(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val hasScheme = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
        val scheme = if (hasScheme) trimmed.substringBefore("://").lowercase() else "http"
        val rest = if (hasScheme) trimmed.substringAfter("://") else trimmed
        val host = rest.substringBefore('/')
        if (host.isEmpty()) return null
        val path = rest.removePrefix(host)
        // "[::1]" and "[::1]:1234" are IPv6; otherwise a colon means a port was given
        val hasPort = host.substringAfterLast(']').contains(':')
        // A full URL without a port keeps the port of its scheme (it may be behind a proxy)
        val withPort = if (hasPort || hasScheme) host else "$host:$DEFAULT_PORT"
        return "$scheme://$withPort$path"
    }

    /**
     * What the media server knows about a title, in a few sentences for the model: its name,
     * the plot summary and who plays whom. Null if there is nothing useful.
     */
    fun aboutTitle(item: BaseItemDto): String? {
        val name = item.name?.takeIf { it.isNotBlank() }
        val series = item.seriesName?.takeIf { it.isNotBlank() }
        val year = item.productionYear?.let { " ($it)" }.orEmpty()
        val title =
            when {
                series != null -> {
                    val number =
                        listOfNotNull(
                            item.parentIndexNumber?.let { "season $it" },
                            item.indexNumber?.let { "episode $it" },
                        ).joinToString(" ")
                    listOfNotNull("\"$series\"", number.takeIf { it.isNotEmpty() }, name?.let { "\"$it\"" }).joinToString(", ") + year
                }

                name != null -> {
                    "\"$name\"$year"
                }

                else -> {
                    null
                }
            }
        val overview =
            item.overview
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { if (it.length > MAX_OVERVIEW) it.take(MAX_OVERVIEW).substringBeforeLast(' ') + "…" else it }
        val characters =
            item.people
                .orEmpty()
                .filter { it.type == PersonKind.ACTOR || it.type == PersonKind.GUEST_STAR }
                .mapNotNull { person ->
                    val role = person.role?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    person.name?.takeIf { it.isNotBlank() }?.let { "$role (played by $it)" } ?: role
                }.take(MAX_CHARACTERS)
        if (title == null && overview == null && characters.isEmpty()) return null
        return buildString {
            title?.let { append("The video is $it.") }
            overview?.let { append(" Summary: $it") }
            if (characters.isNotEmpty()) append(" Characters: ${characters.joinToString("; ")}.")
        }.trim()
    }

    private const val MAX_OVERVIEW = 600
    private const val MAX_CHARACTERS = 20

    const val PATTERN_COLUMNS = 16
    const val PATTERN_ROWS = 9

    /**
     * Whether the picture is a different shot from the last one looked at, so it is worth
     * looking again straight away. The patterns are brightness (0-255) at a grid of points; a
     * cut or a pan changes many of them, a person talking changes few.
     */
    fun pictureChanged(
        old: IntArray?,
        new: IntArray,
    ): Boolean {
        if (old == null || old.size != new.size) return true
        val changed = new.indices.count { abs(new[it] - old[it]) > POINT_CHANGE }
        return changed * 100 >= new.size * CHANGED_PERCENT
    }

    private const val POINT_CHANGE = 40
    private const val CHANGED_PERCENT = 25

    private val json = Json { ignoreUnknownKeys = true }

    /** The body of an Ollama /api/chat request for one frame */
    fun request(
        config: Config,
        jpegBase64: String,
        previousScene: String?,
    ): String =
        buildJsonObject {
            put("model", config.model)
            put("stream", false)
            // Thinking models would otherwise spend seconds reasoning before answering
            put("think", false)
            put("keep_alive", "10m")
            putJsonObject("options") {
                put("temperature", 0.2)
                put("num_predict", 160)
            }
            putJsonObject("format") {
                put("type", "object")
                putJsonObject("properties") {
                    if (config.describeScenes) putJsonObject(SCENE) { put("type", "string") }
                    if (config.readText) putJsonObject(TEXT) { put("type", "string") }
                }
                putJsonArray("required") {
                    if (config.describeScenes) add(SCENE)
                    if (config.readText) add(TEXT)
                }
            }
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt(config))
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", userPrompt(config, previousScene))
                        putJsonArray("images") { add(jpegBase64) }
                    },
                )
            }
        }.toString()

    fun systemPrompt(config: Config): String =
        buildString {
            append("You are an audio describer for a blind person watching a film or TV show. ")
            append("You are shown one frame from the video. Reply with JSON only.")
            if (config.describeScenes) {
                append(
                    " \"$SCENE\": one short sentence, at most 15 words, in the present tense, saying what is " +
                        "seen: who is there, what they are doing and where. Describe only what is visible. " +
                        "Do not start with \"The image\", \"The frame\" or \"The scene\". Do not mention " +
                        "subtitles, and do not repeat written words here.",
                )
            }
            if (config.readText) {
                append(
                    " \"$TEXT\": the words written in the picture that a viewer is meant to read, such as " +
                        "titles, captions, signs, letters, phone messages and subtitles, copied exactly and in " +
                        "reading order. Leave out logos, watermarks and channel names. Use an empty string " +
                        "when there is nothing to read.",
                )
            }
            config.about?.let {
                append(
                    "\n\nBackground on what is being watched, from the media library: $it\nUse a character's or " +
                        "place's name when you can tell who or what is shown; otherwise describe them plainly. " +
                        "Never guess a name, and do not retell the summary: say only what this frame shows.",
                )
            }
        }

    fun userPrompt(
        config: Config,
        previousScene: String?,
    ): String =
        if (config.describeScenes && !previousScene.isNullOrBlank()) {
            "The last description spoken was: \"$previousScene\". If the picture still shows the same thing, " +
                "use an empty string for \"$SCENE\"; otherwise describe what is new."
        } else {
            "Here is the current frame."
        }

    /** What the model said about one frame; either part may be empty */
    data class Answer(
        val scene: String = "",
        val text: String = "",
    )

    /** Reads an Ollama /api/chat response. Returns null if it is not in the expected form. */
    fun parseResponse(body: String): Answer? =
        runCatching {
            val content =
                json
                    .parseToJsonElement(body)
                    .jsonObject["message"]
                    ?.jsonObject
                    ?.get("content")
                    ?.let { (it as? JsonPrimitive)?.content }
                    ?: return null
            // Some models wrap the JSON in a code fence
            val start = content.indexOf('{')
            val end = content.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val answer = json.parseToJsonElement(content.substring(start, end + 1)).jsonObject
            Answer(scene = answer.string(SCENE), text = answer.string(TEXT))
        }.getOrNull()

    /** The value as text. Models sometimes give several lines of on-screen text as a list. */
    private fun JsonObject.string(key: String): String =
        when (val value = this[key]) {
            is JsonPrimitive -> value.takeIf { it.isString }?.content
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.joinToString(" ")
            else -> null
        }?.trim()
            ?.takeUnless { it.lowercase().trim('.', '"', '\'') in NOTHING }
            .orEmpty()

    /** Ways a model says "nothing" despite being asked for an empty string */
    private val NOTHING = setOf("", "none", "n/a", "null", "no text", "nothing", "empty", "same")

    /**
     * Decides what to say, so the same thing is not said twice. Remembers the last scene
     * description and the text read recently for one title.
     */
    class Narrator {
        var lastScene: String? = null
            private set
        private val recentText = ArrayDeque<Set<String>>()

        /** Returns the parts of this answer that are new and wanted, or null to stay silent */
        fun next(
            config: Config,
            answer: Answer,
        ): Answer? {
            val scene =
                answer.scene
                    .takeIf { config.describeScenes && it.isNotBlank() }
                    ?.takeUnless { key(it) == lastScene?.let(::key) }
            val text =
                answer.text
                    .takeIf { config.readText && it.any { c -> c.isLetterOrDigit() } }
                    ?.takeUnless { new -> words(new).let { w -> recentText.any { mostlyIn(w, it) } } }
            if (scene != null) lastScene = scene
            if (text != null) {
                recentText.addLast(words(text))
                while (recentText.size > REMEMBERED_TEXTS) recentText.removeFirst()
            }
            if (scene == null && text == null) return null
            return Answer(scene = scene.orEmpty(), text = text.orEmpty())
        }

        private fun words(s: String): Set<String> =
            s
                .lowercase()
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.isNotEmpty() }
                .toSet()

        /**
         * Whether text was already read: at least half its words were in an earlier reading. A
         * caption with a running clock, or one misread slightly, is then not read again.
         */
        private fun mostlyIn(
            new: Set<String>,
            old: Set<String>,
        ): Boolean = new.isEmpty() || new.count { it in old } * 2 >= new.size

        /** Letters and digits only, so punctuation and case differences do not count as new */
        private fun key(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }
    }

    private const val SCENE = "scene"
    private const val TEXT = "text"
    private const val REMEMBERED_TEXTS = 4
}
