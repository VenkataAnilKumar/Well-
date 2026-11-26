package com.example.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Lifecycle-safe Android `TextToSpeech` controller for hands-free spoken AI readout
 * of Consultation Reports and Follow-Up Chat replies.
 */
@Stable
class WellPlusTtsController(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized by mutableStateOf(false)
    private var initFailed by mutableStateOf(false)

    var speakingId by mutableStateOf<String?>(null)
        private set

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                engine?.language = Locale.getDefault()
                engine?.setSpeechRate(0.98f)
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        speakingId = utteranceId
                    }

                    override fun onDone(utteranceId: String?) {
                        if (speakingId == utteranceId) {
                            speakingId = null
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (speakingId == utteranceId) {
                            speakingId = null
                        }
                    }
                })
                isInitialized = true
            } else {
                initFailed = true
            }
        }
    }

    fun toggleSpeak(
        id: String,
        rawText: String,
        onUnavailable: (String) -> Unit = {}
    ) {
        if (speakingId == id) {
            stop()
            return
        }
        if (initFailed || !isInitialized) {
            onUnavailable("Spoken voice engine is not ready on this device.")
            return
        }
        val clean = sanitizeForSpeech(rawText)
        if (clean.isBlank()) return
        speakingId = id
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        runCatching { tts?.stop() }
        speakingId = null
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        speakingId = null
    }

    companion object {
        fun sanitizeForSpeech(raw: String): String =
            raw.replace(Regex("""[*_`#>]+"""), " ")
                .replace("•", ". ")
                .replace("—", ", ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(3_500)
    }
}

@Composable
fun rememberWellPlusTtsController(): WellPlusTtsController {
    val context = LocalContext.current
    val controller = remember(context) { WellPlusTtsController(context) }
    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
    return controller
}
