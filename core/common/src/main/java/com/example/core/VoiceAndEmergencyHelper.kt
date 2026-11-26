package com.example.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import java.util.Locale

object VoiceAndEmergencyHelper {

    /**
     * Zero-permission system speech recognition intent (Google Play policy compliant).
     */
    fun buildVoiceDictationIntent(prompt: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }

    fun extractSpokenText(data: Intent?): String? {
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return matches?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Zero-permission emergency dialer intent using ACTION_DIAL (never requests CALL_PHONE).
     */
    fun openDialer(context: Context, rawNumber: String): Boolean {
        val sanitized = rawNumber.filter { it.isDigit() || it == '+' }
        if (sanitized.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
