package com.github.damontecres.wholphin.ui.playback

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.core.view.children
import androidx.media3.common.Player
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AccessibilityPreferences
import com.github.damontecres.wholphin.ui.components.Speaker
import com.github.damontecres.wholphin.util.A11ySceneDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Accessibility: "Describe scenes aloud" and "Read on-screen text aloud".
 *
 * While a video plays, takes a copy of the picture every few seconds, asks the description
 * server (Ollama with a vision model) what is in it, and speaks the answer with the device's
 * text-to-speech voice. Does nothing unless one of the two settings is on and a server is set.
 *
 * See [A11ySceneDescription] for the request and for how repeats are avoided.
 */
@Composable
fun DescribeScenes(
    prefs: AccessibilityPreferences,
    player: Player,
    item: BaseItemDto?,
) {
    val config = remember(prefs, item?.id) { A11ySceneDescription.config(prefs, item) } ?: return
    val context = LocalContext.current
    val resources = LocalResources.current
    val view = LocalView.current
    val speaker = remember { Speaker(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }
    LaunchedEffect(config) {
        Timber.i("Describing the picture with %s", config)
        val narrator = A11ySceneDescription.Narrator()
        var failures = 0
        var saidUnavailable = false
        var tooSlow = 0
        var saidTooSlow = false
        // Brightness pattern of the last picture sent, to notice when the shot changes
        var lastLooked: IntArray? = null
        var lastLookAt = 0L
        var readingText = false
        var wait = POLL_MS
        while (true) {
            delay(wait)
            wait = POLL_MS
            // Text in the picture does not wait: keep looking for it while a description is spoken
            if (!player.isPlaying || (speaker.isSpeaking && !config.readText)) continue
            val sinceLook = SystemClock.elapsedRealtime() - lastLookAt
            if (sinceLook < minOf(MIN_GAP_MS, config.intervalMs)) continue
            val picture = capture(view, config)
            if (picture == null) {
                wait = NO_FRAME_RETRY_MS
                continue
            }
            // Look as soon as the picture changes, and every so often even when it does not
            if (sinceLook < config.intervalMs && !A11ySceneDescription.pictureChanged(lastLooked, picture.pattern)) {
                picture.bitmap.recycle()
                continue
            }
            lastLooked = picture.pattern
            lastLookAt = SystemClock.elapsedRealtime()
            val frame = encode(picture.bitmap)
            val started = SystemClock.elapsedRealtime()
            val position = player.currentPosition
            val answer =
                runCatching { describe(config, frame, narrator.lastScene) }
                    .onFailure { Timber.w(it, "Could not get a description from %s", config.serverUrl) }
                    .getOrNull()
            val elapsed = SystemClock.elapsedRealtime() - started
            if (answer == null) {
                // Say so once, since a blind viewer cannot see an error message
                if (++failures == FAILURES_BEFORE_SAYING_SO && !saidUnavailable) {
                    saidUnavailable = true
                    speaker.speak(resources.getString(R.string.a11y_describe_unavailable))
                }
                continue
            }
            failures = 0
            // Too late to be useful: the answer was slow, or the viewer paused or skipped
            val moved = player.currentPosition - position
            val skipped = moved < -SKIP_TOLERANCE_MS || moved > elapsed * MAX_SPEED + SKIP_TOLERANCE_MS
            if (elapsed > STALE_MS) {
                Timber.d("Dropping a description that took %s ms", elapsed)
                // The very first answer is often slow while the server loads the model
                if (++tooSlow == SLOW_BEFORE_SAYING_SO && !saidTooSlow) {
                    saidTooSlow = true
                    speaker.speak(resources.getString(R.string.a11y_describe_too_slow))
                }
                continue
            }
            tooSlow = 0
            if (!player.isPlaying || skipped) continue
            Timber.d("Answer in %s ms: %s (speaking=%s)", elapsed, answer, speaker.isSpeaking)
            // New text cuts into a scene description, because captions and signs do not stay
            // on screen for long. A new scene description waits its turn and is skipped for now.
            val speaking = speaker.isSpeaking
            val say = narrator.next(if (speaking) config.copy(describeScenes = false) else config, answer) ?: continue
            val text = say.text.takeIf { it.isNotEmpty() }?.let { resources.getString(R.string.a11y_describe_text_says, it) }
            speaker.speak(
                listOfNotNull(text, say.scene.takeIf { it.isNotEmpty() }).joinToString(" "),
                // Text is not cut off by more text
                interrupt = !(speaking && readingText),
            )
            readingText = text != null
        }
    }
}

private const val POLL_MS = 500L
private const val NO_FRAME_RETRY_MS = 1_000L

/** A change of shot is looked at this soon after the last look, without waiting for the full setting */
private const val MIN_GAP_MS = 2_500L
private const val STALE_MS = 8_000L
private const val SKIP_TOLERANCE_MS = 2_000L
private const val MAX_SPEED = 3
private const val FAILURES_BEFORE_SAYING_SO = 3
private const val SLOW_BEFORE_SAYING_SO = 4

/**
 * Not the app's signed-in client: the description server must never be sent the Jellyfin
 * token. The first answer can be slow while the server loads the model.
 */
private val httpClient by lazy {
    OkHttpClient
        .Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()
}

private class Picture(
    val bitmap: Bitmap,
    /** See [A11ySceneDescription.pictureChanged] */
    val pattern: IntArray,
)

/** The current video picture, or null if it cannot be copied or is black */
private suspend fun capture(
    view: View,
    config: A11ySceneDescription.Config,
): Picture? {
    val surface = findSurfaceView(view.rootView)?.takeIf { it.width > 0 && it.height > 0 && it.holder.surface.isValid }
    if (surface == null) {
        Timber.d("No video surface to copy")
        return null
    }
    val (width, height) = config.frameSize(surface.width, surface.height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val copied =
        suspendCancellableCoroutine { continuation ->
            runCatching {
                PixelCopy.request(
                    surface,
                    bitmap,
                    { result ->
                        if (result != PixelCopy.SUCCESS) Timber.d("Copying the video picture failed with code %s", result)
                        continuation.resume(result == PixelCopy.SUCCESS)
                    },
                    Handler(Looper.getMainLooper()),
                )
            }.onFailure {
                Timber.w(it, "Could not copy the video picture")
                continuation.resume(false)
            }
        }
    // A black picture is a fade, a loading screen or video that cannot be copied; nothing to say
    val pattern = if (copied) withContext(Dispatchers.Default) { brightnessPattern(bitmap) } else null
    if (pattern == null || pattern.all { it < 24 }) {
        bitmap.recycle()
        return null
    }
    return Picture(bitmap, pattern)
}

/** Brightness (0-255) at a grid of points across the picture */
private fun brightnessPattern(bitmap: Bitmap): IntArray {
    val columns = A11ySceneDescription.PATTERN_COLUMNS
    val rows = A11ySceneDescription.PATTERN_ROWS
    return IntArray(columns * rows) { i ->
        val pixel =
            bitmap.getPixel(
                (i % columns * 2 + 1) * bitmap.width / (columns * 2),
                (i / columns * 2 + 1) * bitmap.height / (rows * 2),
            )
        ((pixel shr 16 and 0xFF) * 3 + (pixel shr 8 and 0xFF) * 6 + (pixel and 0xFF)) / 10
    }
}

/** Turns the picture into a base64 JPEG and frees it */
private suspend fun encode(bitmap: Bitmap): String =
    withContext(Dispatchers.Default) {
        try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

private fun findSurfaceView(view: View): SurfaceView? =
    when (view) {
        is SurfaceView -> view
        is ViewGroup -> view.children.firstNotNullOfOrNull(::findSurfaceView)
        else -> null
    }

private suspend fun describe(
    config: A11ySceneDescription.Config,
    jpegBase64: String,
    previousScene: String?,
): A11ySceneDescription.Answer {
    val request =
        Request
            .Builder()
            .url("${config.serverUrl}/api/chat")
            .post(
                A11ySceneDescription
                    .request(config, jpegBase64, previousScene)
                    .toRequestBody("application/json".toMediaType()),
            ).build()
    val call = httpClient.newCall(request)
    val body =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        continuation.resumeWith(
                            runCatching {
                                response.use {
                                    if (!it.isSuccessful) throw IOException("HTTP ${it.code}: ${it.body.string().take(200)}")
                                    it.body.string()
                                }
                            },
                        )
                    }
                },
            )
        }
    return A11ySceneDescription.parseResponse(body) ?: throw IOException("Unexpected answer: ${body.take(200)}")
}
