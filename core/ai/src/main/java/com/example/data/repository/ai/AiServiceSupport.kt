package com.example.data.repository.ai

import android.util.Log
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray

internal const val AI_LOG_TAG = "WellPlusAi"
internal const val PRIMARY_AI_MODEL = "gemini-3.8-flash"
internal const val FAST_AI_MODEL = "gemini-3.5-flash-lite"
internal const val INFERENCE_TIMEOUT_MS = 60_000L
internal const val FAST_FIRST_ATTEMPT_TIMEOUT_MS = 28_000L
internal const val FAST_RETRY_TIMEOUT_MS = 30_000L

/**
 * Shared resilience and JSON parsing helpers for Firebase AI Logic (`Firebase.ai`) domain services.
 * All requests run on `Dispatchers.IO` with bounded timeouts.
 */
internal object AiServiceSupport {

    suspend fun generateJsonResilient(
        model: GenerativeModel,
        prompt: String,
        label: String,
        emptyResponseMessage: String,
        timeoutMessage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = try {
                withTimeout(FAST_FIRST_ATTEMPT_TIMEOUT_MS) {
                    model.generateContent(prompt)
                }
            } catch (_: TimeoutCancellationException) {
                Log.w(AI_LOG_TAG, "$label timed out on first attempt (${FAST_FIRST_ATTEMPT_TIMEOUT_MS}ms); retrying once")
                withTimeout(FAST_RETRY_TIMEOUT_MS) {
                    model.generateContent(prompt)
                }
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(emptyResponseMessage)
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_LOG_TAG, "$label timed out after retry", e)
            throw IllegalStateException(timeoutMessage, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_LOG_TAG, "$label failed", e)
            throw e
        }
    }

    fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { idx -> optString(idx)?.trim()?.takeIf { it.isNotEmpty() } }
    }
}
