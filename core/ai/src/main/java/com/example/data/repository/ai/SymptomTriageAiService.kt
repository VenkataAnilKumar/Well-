package com.example.data.repository.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.ClarifyingQuestionItem
import com.example.data.model.ConsultationType
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.StructuredTriageResult
import com.example.data.model.UrgencyLevel
import com.example.data.model.VitalReading
import com.example.data.repository.ai.AiServiceSupport.toStringList
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class SymptomTriageAiService {

    private val triageSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(
                    description = "Concise, calm clinical title for this consultation (3-7 words)"
                ),
                "urgencyLevel" to Schema.enumeration(
                    values = listOf("SELF_CARE", "DOCTOR_SOON", "URGENT", "EMERGENCY"),
                    description = "Clinical triage urgency level. Always choose EMERGENCY for chest pain, severe breathing difficulty, stroke signs, severe bleeding, suicidal crisis, or anaphylaxis."
                ),
                "aiSummary" to Schema.string(
                    description = "Empathetic, clear 2-4 sentence personalized summary explaining what may be happening, explicitly accounting for the user's allergies, medications, or conditions when relevant. Non-diagnostic framing."
                ),
                "possibleCauses" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 5 potential causes or observations to discuss with a healthcare professional, written in plain language."
                ),
                "careSuggestions" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 6 practical, safe, actionable home-care or next-step items short enough to add to a personal care checklist."
                ),
                "redFlags" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 5 clear warning signs that require urgent or emergency in-person medical care."
                ),
                "doctorQuestions" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 thoughtful questions the user can bring to their clinician."
                )
            )
        )
    }

    private val triageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = triageSchema
                temperature = 0.3f
                maxOutputTokens = 1536
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+, a calm, empathetic, evidence-based AI clinical health companion.
                    Your role is to analyze user-reported symptoms, optional photos of visible concerns, or photos of lab results / prescriptions, and provide structured, personalized, NON-DIAGNOSTIC health guidance.
                    Safety rules (MANDATORY):
                    - Adults 18+ only: never provide pediatric dosing or clinical advice for minors.
                    - Never claim to provide a definitive medical diagnosis, prescribe medications, or recommend changing/stopping any prescribed dosage.
                    - Never provide self-harm instructions or minimize suicidal ideation; if self-harm is mentioned, immediately direct the user to local crisis and emergency services.
                    - Always cross-check suggestions against the user's listed allergies, active tracked medications, and existing conditions; warn clearly if a common OTC remedy might conflict.
                    - If symptoms suggest an emergency (e.g., crushing chest pain, sudden weakness/slurred speech, severe shortness of breath, blue lips, uncontrolled bleeding, swelling of tongue/throat, severe head injury, overdose), set urgencyLevel to EMERGENCY and put immediate emergency action first.
                    - For lab report photos, summarize key visible values in plain language, note which values appear flagged/out of range, explain what those markers generally measure, and recommend confirming with the ordering clinician.
                    - Keep tone serene, reassuring, structured, and easy to scan.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun analyzeConsultation(
        type: ConsultationType,
        bodyArea: String,
        symptomSummary: String,
        severity: Int,
        duration: String,
        associatedSymptoms: List<String>,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem> = emptyList(),
        recentVitals: List<VitalReading> = emptyList(),
        photoBitmap: Bitmap?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): StructuredTriageResult {
        val profileContext = profile?.toPromptContext(activeMedications)
            ?: if (activeMedications.isNotEmpty()) {
                "Active tracked medications: " + activeMedications.filter { it.active }
                    .joinToString(", ") { "${it.name} ${it.dosage}".trim() }
            } else {
                "No personal health profile details provided yet."
            }
        val associatedText = if (associatedSymptoms.isEmpty()) {
            "None selected"
        } else {
            associatedSymptoms.joinToString(", ")
        }
        val vitalsText = recentVitals.take(4).joinToString("; ") { v ->
            "${v.parsedType.shortLabel}: ${v.formattedReadingForRegion(region)} (${v.rangeStatus.label})"
        }
        val modePrompt = if (type == ConsultationType.PHOTO_ANALYSIS) {
            "This is a visual symptom or lab report / medical document photo analysis request. Carefully examine the attached image alongside the user's notes and health profile."
        } else {
            "This is a guided symptom check request. Evaluate all reported symptoms, severity, duration, optional photo, and personal health profile."
        }

        val fullPrompt = buildString {
            appendLine(modePrompt)
            appendLine(region.aiPromptContext)
            appendLine("Personal Health Profile: $profileContext")
            if (vitalsText.isNotBlank()) {
                appendLine("Recent Logged Vitals: $vitalsText")
            }
            appendLine("Primary Area / Category: $bodyArea")
            appendLine("Self-Reported Severity: $severity / 10")
            appendLine("Duration: $duration")
            appendLine("Associated Symptoms: $associatedText")
            appendLine("User Description: ${symptomSummary.ifBlank { "Please analyze the attached photo." }}")
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                    triageModel.generateContent(
                        content {
                            text(fullPrompt)
                            if (photoBitmap != null) {
                                image(photoBitmap)
                            }
                        }
                    )
                }
                val rawJson = response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("The AI Doctor returned an empty response. Please try again.")
                parseStructuredTriage(rawJson)
            } catch (e: TimeoutCancellationException) {
                Log.e(AI_LOG_TAG, "Structured triage timed out", e)
                throw IllegalStateException("Analysis timed out after 60 seconds. Please check your connection and try again.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_LOG_TAG, "Structured triage failed", e)
                throw e
            }
        }
    }

    private fun parseStructuredTriage(rawJson: String): StructuredTriageResult {
        val json = JSONObject(rawJson)
        val title = json.optString("title", "Health Consultation").trim().ifBlank { "Health Consultation" }
        val urgency = UrgencyLevel.fromRaw(json.optString("urgencyLevel", UrgencyLevel.DOCTOR_SOON.name))
        val summary = json.optString("aiSummary", "").trim()
            .ifBlank { "Please review the suggested guidance below and discuss persistent symptoms with a clinician." }

        return StructuredTriageResult(
            title = title,
            urgencyLevel = urgency,
            aiSummary = summary,
            possibleCauses = json.optJSONArray("possibleCauses").toStringList(),
            careSuggestions = json.optJSONArray("careSuggestions").toStringList(),
            redFlags = json.optJSONArray("redFlags").toStringList(),
            doctorQuestions = json.optJSONArray("doctorQuestions").toStringList()
        )
    }

    private val clarifyingQuestionsSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "id" to Schema.string(description = "Short unique slug like q1, q2, q3"),
                            "question" to Schema.string(description = "Concise, clear clinical clarifying question (under 18 words)"),
                            "quickOptions" to Schema.array(
                                items = Schema.string(),
                                description = "3 to 4 brief tappable answer chips (e.g., 'Yes', 'No', 'Only with movement', 'Not sure')"
                            ),
                            "clinicalReason" to Schema.string(description = "Brief 5-10 word explanation of why this question helps triage")
                        )
                    ),
                    description = "Exactly 2 or 3 high-yield clarifying questions tailored to the user's symptoms."
                )
            )
        )
    }

    private val clarifyingQuestionsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clarifyingQuestionsSchema
                temperature = 0.25f
                maxOutputTokens = 768
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+, an adaptive clinical intake assistant for adults 18+.
                    Given a user's chief complaint, body area, severity, and duration, generate 2 to 3 targeted,
                    high-yield clarifying questions that help distinguish self-care from urgent warning signs
                    and improve symptom-triage accuracy. Keep questions calm, brief, and non-diagnostic.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateClarifyingQuestions(
        bodyArea: String,
        symptomDescription: String,
        severity: Int,
        duration: String,
        profile: HealthProfile?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): List<ClarifyingQuestionItem> {
        val prompt = buildString {
            appendLine("Generate 3 targeted clinical clarifying questions for this intake:")
            appendLine(region.aiPromptContext)
            appendLine("Body Area: $bodyArea")
            appendLine("Chief Complaint: ${symptomDescription.trim().take(800)}")
            appendLine("Severity: $severity / 10 • Duration: $duration")
            profile?.let { appendLine("Health Profile: ${it.toPromptContext()}") }
        }
        val rawJson = AiServiceSupport.generateJsonResilient(
            model = clarifyingQuestionsModel,
            prompt = prompt,
            label = "Clarifying intake questions",
            emptyResponseMessage = "No clarifying questions returned.",
            timeoutMessage = "Clarifying questions timed out — you can still continue straight to AI analysis."
        )
        val json = JSONObject(rawJson)
        val arr = json.optJSONArray("questions") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val question = obj.optString("question", "").trim()
                if (question.isEmpty()) continue
                val id = obj.optString("id", "q${i + 1}").trim().ifBlank { "q${i + 1}" }
                val options = obj.optJSONArray("quickOptions").toStringList()
                    .ifEmpty { listOf("Yes", "No", "Not sure") }
                    .take(4)
                val reason = obj.optString("clinicalReason", "").trim()
                add(
                    ClarifyingQuestionItem(
                        id = id,
                        question = question,
                        quickOptions = options,
                        clinicalReason = reason
                    )
                )
            }
        }.take(3)
    }
}
