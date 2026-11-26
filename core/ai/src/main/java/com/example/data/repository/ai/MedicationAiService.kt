package com.example.data.repository.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.DrugInteractionAlert
import com.example.data.model.HealthProfile
import com.example.data.model.InteractionSeverity
import com.example.data.model.MedicationInteractionReport
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.ScannedPrescriptionDraft
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

class MedicationAiService {

    private val interactionSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "overallStatus" to Schema.string(
                    description = "Concise 3-7 word safety headline summarizing the screening result."
                ),
                "highestSeverity" to Schema.enumeration(
                    values = listOf("CRITICAL", "SERIOUS", "MODERATE", "NONE"),
                    description = "Highest interaction or allergy conflict severity found across the active medication list and profile."
                ),
                "executiveSummary" to Schema.string(
                    description = "Clear 2-3 sentence pharmacist-style non-diagnostic overview of the medication regimen, highlighting any notable interactions, duplicate therapies, or allergy/condition cautions."
                ),
                "interactions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "pairLabel" to Schema.string(
                                description = "Short label of the interacting pair or conflict, e.g., 'Warfarin + Ibuprofen' or 'Amoxicillin + Penicillin Allergy'."
                            ),
                            "severity" to Schema.enumeration(
                                values = listOf("CRITICAL", "SERIOUS", "MODERATE", "NONE"),
                                description = "Severity of this specific interaction or caution."
                            ),
                            "summary" to Schema.string(
                                description = "Plain-language 1-2 sentence explanation of why this combination or caution matters."
                            ),
                            "recommendation" to Schema.string(
                                description = "Practical next step such as spacing doses, monitoring symptoms, or asking a pharmacist."
                            )
                        )
                    ),
                    description = "List of identified drug-drug, drug-allergy, duplicate-class, or drug-condition cautions. Return an empty array if no meaningful conflicts exist."
                ),
                "lifestyleAndFoodNotes" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 4 concise timing, food, alcohol, grapefruit, caffeine, or sun-sensitivity notes relevant to the listed medications."
                ),
                "pharmacistQuestions" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 focused questions the user can ask their pharmacist or prescribing clinician."
                )
            )
        )
    }

    private val prescriptionScanSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(
                    description = "Medication or supplement name read from the prescription bottle, box, or label (generic or brand name). Empty string if unreadable."
                ),
                "dosage" to Schema.string(
                    description = "Strength and form, e.g., '10 mg tablet', '500 mg capsule', '1000 IU'. Empty string if not shown."
                ),
                "scheduleSlot" to Schema.enumeration(
                    values = listOf("MORNING", "MIDDAY", "EVENING", "BEDTIME", "AS_NEEDED"),
                    description = "Most appropriate daily schedule window inferred from the label directions (default MORNING if once daily unspecified; AS_NEEDED if PRN / as needed)."
                ),
                "reminderHour" to Schema.integer(
                    description = "Suggested 24-hour reminder hour (0-23) matching the scheduleSlot, e.g., 8 for morning, 13 for midday, 19 for evening, 22 for bedtime."
                ),
                "reminderMinute" to Schema.integer(
                    description = "Suggested reminder minute (0-59), usually 0 or 30."
                ),
                "instructions" to Schema.string(
                    description = "Concise intake instructions from the label (e.g., 'Take 1 tablet by mouth once daily with food'). Max 180 characters."
                ),
                "confidenceNote" to Schema.string(
                    description = "Brief note on image legibility or any partially obscured text the user should double-check."
                )
            )
        )
    }

    private val interactionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = interactionSchema
                temperature = 0.2f
                maxOutputTokens = 1536
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Clinical Medication Safety Screener.
                    Given a user's active medications/supplements, known allergies, age, and ongoing conditions, perform a thorough, evidence-based, NON-DIAGNOSTIC safety screening for:
                    1. Drug-drug interactions and duplicate therapy classes.
                    2. Drug-allergy cross-reactivity or contraindications.
                    3. Drug-condition cautions.
                    4. Key food, alcohol, grapefruit, caffeine, and spacing/timing considerations.
                    Safety rules (MANDATORY):
                    - Informational non-exhaustive screening for adults 18+ only; never prescribe drugs or recommend dose changes.
                    - Never advise the user to abruptly stop a prescribed medication on their own; always recommend confirming changes with their prescribing doctor or pharmacist.
                    - Do not fabricate severe interactions when none exist; if the regimen has no major interactions, set highestSeverity to NONE and share helpful timing/food notes.
                    - Keep every item clear, calm, accurate, and easy to scan.
                    """.trimIndent()
                )
            }
        )
    }

    private val prescriptionScanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = prescriptionScanSchema
                temperature = 0.1f
                maxOutputTokens = 768
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+'s Prescription Label Reader.
                    Examine the provided photo of a prescription bottle, pill box, blister pack, supplement bottle, or pharmacy printout.
                    Extract the medication or supplement name, strength/dosage, best-fit scheduleSlot, suggested reminder time, and concise intake instructions.
                    Never guess or invent a medication name if the image does not show one — leave name blank and explain in confidenceNote what was unclear.
                    Do not include personal patient names, doctor names, pharmacy phone numbers, or Rx account numbers in the extracted instructions.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun analyzeMedicationInteractions(
        medications: List<MedicationItem>,
        profile: HealthProfile?,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): MedicationInteractionReport {
        val activeTracked = medications.filter { it.active }
        val trackedLines = activeTracked.map { med ->
            buildString {
                append(med.name.trim())
                if (med.dosage.isNotBlank()) append(" (${med.dosage.trim()})")
                append(" — ${med.parsedSlot.label}")
                if (med.parsedSlot != MedicationScheduleSlot.AS_NEEDED) {
                    append(" at ${med.formattedReminderTime()}")
                }
                if (med.instructions.isNotBlank()) append(" [${med.instructions.trim()}]")
            }
        }
        val profileOnlyMeds = profile?.medications.orEmpty()
            .filter { pm ->
                activeTracked.none { it.name.equals(pm.trim(), ignoreCase = true) }
            }
        val totalCount = activeTracked.size + profileOnlyMeds.size

        val prompt = buildString {
            appendLine("Screen the following active medication and supplement regimen for drug-drug interactions, drug-allergy conflicts, drug-condition cautions, and food/alcohol/timing factors.")
            appendLine(region.aiPromptContext)
            appendLine("Tracked Active Medications (${activeTracked.size}):")
            if (trackedLines.isEmpty()) {
                appendLine("  - None")
            } else {
                trackedLines.forEach { appendLine("  - $it") }
            }
            if (profileOnlyMeds.isNotEmpty()) {
                appendLine("Additional Profile Medications: ${profileOnlyMeds.joinToString(", ")}")
            }
            appendLine("Known Allergies: ${profile?.allergies.orEmpty().joinToString(", ").ifBlank { "None listed" }}")
            appendLine("Ongoing Conditions: ${profile?.conditions.orEmpty().joinToString(", ").ifBlank { "None listed" }}")
            if (profile != null && profile.age > 0L) {
                appendLine("Age: ${profile.age}")
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                    interactionModel.generateContent(prompt)
                }
                val rawJson = response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Interaction screener returned an empty response. Please try again.")
                parseInteractionReport(rawJson, totalCount)
            } catch (e: TimeoutCancellationException) {
                Log.e(AI_LOG_TAG, "Medication interaction check timed out", e)
                throw IllegalStateException("Interaction check timed out after 60 seconds. Please try again.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_LOG_TAG, "Medication interaction check failed", e)
                throw e
            }
        }
    }

    suspend fun scanPrescriptionLabel(photoBitmap: Bitmap): ScannedPrescriptionDraft {
        return withContext(Dispatchers.IO) {
            try {
                val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                    prescriptionScanModel.generateContent(
                        content {
                            text("Extract the medication or supplement name, strength/dosage, schedule window, suggested reminder time, and concise intake instructions from this photo.")
                            image(photoBitmap)
                        }
                    )
                }
                val rawJson = response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Prescription scanner returned an empty response. Please try again.")
                parseScannedPrescription(rawJson)
            } catch (e: TimeoutCancellationException) {
                Log.e(AI_LOG_TAG, "Prescription scan timed out", e)
                throw IllegalStateException("Label scan timed out after 60 seconds. Please try again with a clearer photo.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_LOG_TAG, "Prescription scan failed", e)
                throw e
            }
        }
    }

    private fun parseInteractionReport(
        rawJson: String,
        checkedCount: Int
    ): MedicationInteractionReport {
        val json = JSONObject(rawJson)
        val highestSeverity = InteractionSeverity.fromRaw(
            json.optString("highestSeverity", InteractionSeverity.NONE.name)
        )
        val overallStatus = json.optString("overallStatus", highestSeverity.label)
            .trim()
            .ifBlank { highestSeverity.label }
        val executiveSummary = json.optString("executiveSummary", "")
            .trim()
            .ifBlank { "Review the notes below and confirm any medication adjustments with your pharmacist or doctor." }

        val interactionsArray = json.optJSONArray("interactions")
        val alerts = buildList {
            if (interactionsArray != null) {
                for (i in 0 until interactionsArray.length()) {
                    val obj = interactionsArray.optJSONObject(i) ?: continue
                    val pair = obj.optString("pairLabel", "").trim()
                    val summary = obj.optString("summary", "").trim()
                    if (pair.isNotEmpty() || summary.isNotEmpty()) {
                        add(
                            DrugInteractionAlert(
                                pairLabel = pair.ifBlank { "Medication Safety Note" },
                                severity = InteractionSeverity.fromRaw(
                                    obj.optString("severity", InteractionSeverity.MODERATE.name)
                                ),
                                summary = summary,
                                recommendation = obj.optString("recommendation", "").trim()
                            )
                        )
                    }
                }
            }
        }

        return MedicationInteractionReport(
            overallStatus = overallStatus,
            highestSeverity = highestSeverity,
            executiveSummary = executiveSummary,
            interactions = alerts,
            lifestyleAndFoodNotes = json.optJSONArray("lifestyleAndFoodNotes").toStringList(),
            pharmacistQuestions = json.optJSONArray("pharmacistQuestions").toStringList(),
            checkedMedicationCount = checkedCount
        )
    }

    private fun parseScannedPrescription(rawJson: String): ScannedPrescriptionDraft {
        val json = JSONObject(rawJson)
        val slot = MedicationScheduleSlot.fromRaw(
            json.optString("scheduleSlot", MedicationScheduleSlot.MORNING.name)
        )
        val rawHour = json.optInt("reminderHour", slot.defaultHour).coerceIn(0, 23)
        val rawMinute = json.optInt("reminderMinute", slot.defaultMinute).coerceIn(0, 59)
        return ScannedPrescriptionDraft(
            name = json.optString("name", "").trim().take(100),
            dosage = json.optString("dosage", "").trim().take(60),
            scheduleSlot = slot,
            reminderHour = rawHour,
            reminderMinute = rawMinute,
            instructions = json.optString("instructions", "").trim().take(240),
            confidenceNote = json.optString("confidenceNote", "").trim()
        )
    }
}
