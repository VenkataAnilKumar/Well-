package com.example.data.repository.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.ChatMessage
import com.example.data.model.Consultation
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.repository.normalizeFollowUpChatTurns
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout

class FollowUpChatAiService {

    private val followUpModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                temperature = 0.45f
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+, a calm, reassuring AI Doctor health companion continuing a personalized consultation.
                    Respond clearly in concise paragraphs or short bullet points without excessive markdown clutter.
                    Always respect the user's Personal Health Profile (allergies, current medications, chronic conditions) and the initial consultation summary.
                    Never give a definitive diagnosis or prescribe controlled/prescription medications. Highlight red flags clearly if symptoms worsen.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamFollowUpReply(
        consultation: Consultation,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem> = emptyList(),
        existingMessages: List<ChatMessage>,
        userMessage: String,
        photoBitmap: Bitmap?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): Flow<String> = flow {
        val seedUserText = buildString {
            appendLine("Consultation Context:")
            appendLine(region.aiPromptContext)
            appendLine("Health Profile: ${profile?.toPromptContext(activeMedications) ?: "None provided"}")
            appendLine("Type: ${consultation.parsedType.label}")
            appendLine("Area: ${consultation.bodyArea} | Severity: ${consultation.severity}/10 | Duration: ${consultation.duration}")
            appendLine("Outcome Status: ${consultation.parsedFollowUpStatus.label}")
            appendLine("Symptoms: ${consultation.symptomSummary}")
            if (consultation.associatedSymptoms.isNotEmpty()) {
                appendLine("Associated: ${consultation.associatedSymptoms.joinToString(", ")}")
            }
        }
        val seedModelText = buildString {
            appendLine("Initial Assessment (${consultation.parsedUrgency.label}): ${consultation.aiSummary}")
            if (consultation.careSuggestions.isNotEmpty()) {
                appendLine("Suggested care: ${consultation.careSuggestions.joinToString("; ")}")
            }
        }

        val (normalizedHistoryTurns, finalOutgoingPrompt) = normalizeFollowUpChatTurns(
            seedUserText = seedUserText,
            seedModelText = seedModelText,
            existingMessages = existingMessages.takeLast(14),
            newUserMessage = userMessage.ifBlank { "Please review this follow-up photo in the context of my symptoms." }
        )

        val chatHistory = normalizedHistoryTurns.map { (role, text) ->
            content(role) { text(text) }
        }

        val chat = followUpModel.startChat(history = chatHistory)
        val outgoing = content("user") {
            text(finalOutgoingPrompt)
            if (photoBitmap != null) {
                image(photoBitmap)
            }
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(INFERENCE_TIMEOUT_MS) {
                chat.sendMessageStream(outgoing).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("The AI Doctor returned an empty reply. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_LOG_TAG, "Follow-up stream timed out", e)
            throw IllegalStateException("Follow-up reply timed out. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_LOG_TAG, "Follow-up stream failed", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)
}
