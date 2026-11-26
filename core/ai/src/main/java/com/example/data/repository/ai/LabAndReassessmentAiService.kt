package com.example.data.repository.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.data.model.Consultation
import com.example.data.model.ExtractedBiomarkerDraft
import com.example.data.model.ExtractedLabPanelResult
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarkerCategory
import com.example.data.model.LabBiomarkerStatus
import com.example.data.model.MedicationItem
import com.example.data.model.ProactiveReassessmentResult
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gemini Vision lab biomarker extraction (HbA1c, LDL, HDL, Vitamin D, TSH, Glucose, etc.)
 * and Proactive Clinical Reassessment for worsening symptoms or abnormal vitals.
 */
class LabAndReassessmentAiService {

    private val labExtractionSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "panelSummary" to Schema.string(
                    description = "Calm 2-3 sentence plain-language summary of the visible lab panel, noting any flagged markers. Non-diagnostic."
                ),
                "panelDateKey" to Schema.string(
                    description = "Lab collection date in yyyy-MM-dd format if visible on the sheet, otherwise empty string."
                ),
                "biomarkers" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "markerName" to Schema.string(description = "Standard marker name e.g., HbA1c, LDL Cholesterol, Vitamin D, TSH, Fasting Glucose"),
                            "category" to Schema.enumeration(
                                values = listOf("METABOLIC", "LIPIDS", "THYROID", "VITAMINS", "CBC", "KIDNEY_LIVER", "OTHER"),
                                description = "Lab category"
                            ),
                            "value" to Schema.double(description = "Numeric lab value"),
                            "unit" to Schema.string(description = "Unit e.g., %, mg/dL, mmol/L, ng/mL, mIU/L"),
                            "referenceRangeText" to Schema.string(description = "Reference range shown on the lab sheet or standard adult range, e.g. '4.0 - 5.6 %'"),
                            "status" to Schema.enumeration(
                                values = listOf("OPTIMAL", "BORDERLINE", "HIGH", "LOW"),
                                description = "Status relative to the reference range"
                            ),
                            "labNote" to Schema.string(description = "1-sentence plain-language explanation of what this biomarker measures")
                        )
                    ),
                    description = "Up to 15 numeric biomarkers visible in the lab report photo."
                ),
                "clinicianDiscussionPoints" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 4 practical questions the user can ask their ordering doctor about these results."
                )
            )
        )
    }

    private val labExtractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = labExtractionSchema
                temperature = 0.2f
                maxOutputTokens = 1536
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+, an informational clinical lab-report assistant for adults 18+.
                    Extract only clearly visible numeric lab biomarkers from the attached laboratory report image.
                    Rules:
                    - Never invent or hallucinate values that are unreadable or absent.
                    - Preserve the exact numeric values and units shown on the lab report.
                    - Use non-diagnostic, calm educational phrasing and advise confirming results with the ordering clinician.
                    """.trimIndent()
                )
            }
        )
    }

    private val reassessmentSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "urgencyLevel" to Schema.enumeration(
                    values = listOf("SELF_CARE", "DOCTOR_SOON", "URGENT", "EMERGENCY"),
                    description = "Reassessed urgency level. Escalate to EMERGENCY for hypertensive crisis, severe hypoxia, severe fever with red flags, chest pain, or rapid deterioration."
                ),
                "headline" to Schema.string(
                    description = "Clear 5-10 word reassessment headline."
                ),
                "clinicalReasoning" to Schema.string(
                    description = "Calm 2-3 sentence explanation of why this change or abnormal reading deserves attention, incorporating the user's medications/conditions."
                ),
                "immediateNextSteps" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 safe, concrete next steps the user should take right now (never instruct stopping prescribed medication)."
                ),
                "redFlagsToWatch" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 specific warning signs requiring immediate emergency evaluation."
                ),
                "doctorPrepQuestion" to Schema.string(
                    description = "One sharp question to ask a doctor, pharmacist, or nurse advice line."
                )
            )
        )
    }

    private val reassessmentModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reassessmentSchema
                temperature = 0.25f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+, a proactive clinical safety companion for adults 18+.
                    A user has either marked a prior symptom as WORSENING or logged an out-of-range vital sign.
                    Provide structured, non-diagnostic reassessment guidance that prioritizes patient safety,
                    checks for red-flag escalation, and respects regional clinical conventions (US or Canada).
                    Never tell a user to stop or alter prescribed medication dosages without their clinician.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun extractLabBiomarkers(
        photoBitmap: Bitmap,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): ExtractedLabPanelResult {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val prompt = buildString {
            appendLine("Extract all clearly readable numeric lab biomarkers from this lab report image.")
            appendLine(region.aiPromptContext)
            appendLine("If no collection date is visible, leave panelDateKey blank (client will default to $today).")
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                    labExtractionModel.generateContent(
                        content {
                            text(prompt)
                            image(photoBitmap)
                        }
                    )
                }
                val rawJson = response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Could not read any text from this lab photo. Try a sharper, brighter photo.")
                parseLabExtraction(rawJson, today)
            } catch (e: TimeoutCancellationException) {
                Log.e(AI_LOG_TAG, "Lab biomarker extraction timed out", e)
                throw IllegalStateException("Lab scan timed out after 60 seconds. Please try again.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_LOG_TAG, "Lab biomarker extraction failed", e)
                throw e
            }
        }
    }

    suspend fun reassessWorseningConsultation(
        consultation: Consultation,
        followUpNote: String,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem>,
        recentVitals: List<VitalReading>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): ProactiveReassessmentResult {
        val prompt = buildString {
            appendLine("PROACTIVE REASSESSMENT: User marked a prior consultation as WORSENING.")
            appendLine(region.aiPromptContext)
            appendLine("Original Consultation: ${consultation.title} (${consultation.bodyArea})")
            appendLine("Original Severity: ${consultation.severity}/10 • Duration: ${consultation.duration}")
            appendLine("Original Urgency: ${consultation.urgencyLevel}")
            appendLine("Original Summary: ${consultation.symptomSummary}")
            if (followUpNote.isNotBlank()) {
                appendLine("User Worsening Update Note: $followUpNote")
            }
            profile?.let { appendLine("Health Profile: ${it.toPromptContext(activeMedications)}") }
            if (recentVitals.isNotEmpty()) {
                val vitalsText = recentVitals.take(4).joinToString("; ") { v ->
                    "${v.parsedType.shortLabel}: ${v.formattedReadingForRegion(region)} (${v.rangeStatus.label})"
                }
                appendLine("Recent Vitals: $vitalsText")
            }
        }
        val rawJson = AiServiceSupport.generateJsonResilient(
            model = reassessmentModel,
            prompt = prompt,
            label = "Worsening symptom reassessment",
            emptyResponseMessage = "AI reassessment returned an empty response.",
            timeoutMessage = "Reassessment timed out. If symptoms feel severe or rapidly worsening, seek in-person care promptly."
        )
        return parseReassessment(rawJson)
    }

    suspend fun reassessAbnormalVital(
        vital: VitalReading,
        recentVitals: List<VitalReading>,
        profile: HealthProfile?,
        activeMedications: List<MedicationItem>,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): ProactiveReassessmentResult {
        val prompt = buildString {
            appendLine("PROACTIVE REASSESSMENT: User just logged an out-of-range vital reading.")
            appendLine(region.aiPromptContext)
            appendLine(
                "Flagged Vital: ${vital.parsedType.label} = ${vital.formattedReadingForRegion(region)} " +
                    "(Status: ${vital.rangeStatus.label}; Normal ref: ${vital.parsedType.normalRangeLabelFor(region)})"
            )
            if (vital.note.isNotBlank()) {
                appendLine("Measurement Context Note: ${vital.note}")
            }
            val priorSameType = recentVitals
                .filter { it.parsedType == vital.parsedType && it.id != vital.id }
                .take(4)
            if (priorSameType.isNotEmpty()) {
                appendLine(
                    "Prior ${vital.parsedType.shortLabel} readings: " +
                        priorSameType.joinToString(", ") { it.formattedReadingForRegion(region) }
                )
            }
            profile?.let { appendLine("Health Profile: ${it.toPromptContext(activeMedications)}") }
        }
        val rawJson = AiServiceSupport.generateJsonResilient(
            model = reassessmentModel,
            prompt = prompt,
            label = "Abnormal vital reassessment",
            emptyResponseMessage = "AI reassessment returned an empty response.",
            timeoutMessage = "Vital reassessment timed out. Re-check after 5 minutes of rest or contact a clinician if you feel unwell."
        )
        return parseReassessment(rawJson)
    }

    private fun parseLabExtraction(rawJson: String, fallbackDate: String): ExtractedLabPanelResult {
        val json = JSONObject(rawJson)
        val summary = json.optString("panelSummary", "").trim()
            .ifBlank { "Extracted visible lab markers below. Verify each number against your printed lab sheet." }
        val dateKey = json.optString("panelDateKey", "").trim()
            .takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }
            ?: fallbackDate
        val arr = json.optJSONArray("biomarkers")
        val markers = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("markerName", "").trim().take(80)
                    val value = obj.optDouble("value", Double.NaN)
                    if (name.isEmpty() || value.isNaN() || value < 0.0 || value > 100000.0) continue
                    add(
                        ExtractedBiomarkerDraft(
                            markerName = name,
                            category = LabBiomarkerCategory.fromRaw(obj.optString("category")),
                            value = value,
                            unit = obj.optString("unit", "units").trim().ifBlank { "units" }.take(24),
                            referenceRangeText = obj.optString("referenceRangeText", "").trim().take(80),
                            status = LabBiomarkerStatus.fromRaw(obj.optString("status")),
                            labNote = obj.optString("labNote", "").trim().take(400),
                            selected = true
                        )
                    )
                }
            }
        }
        return ExtractedLabPanelResult(
            panelSummary = summary,
            panelDateKey = dateKey,
            biomarkers = markers,
            clinicianDiscussionPoints = json.optJSONArray("clinicianDiscussionPoints").toStringList()
        )
    }

    private fun parseReassessment(rawJson: String): ProactiveReassessmentResult {
        val json = JSONObject(rawJson)
        return ProactiveReassessmentResult(
            urgencyLevel = UrgencyLevel.fromRaw(json.optString("urgencyLevel", UrgencyLevel.DOCTOR_SOON.name)),
            headline = json.optString("headline", "Clinical Follow-Up Recommended").trim()
                .ifBlank { "Clinical Follow-Up Recommended" },
            clinicalReasoning = json.optString("clinicalReasoning", "").trim()
                .ifBlank { "A worsening trend or abnormal reading warrants careful re-checking and clinician review." },
            immediateNextSteps = json.optJSONArray("immediateNextSteps").toStringList(),
            redFlagsToWatch = json.optJSONArray("redFlagsToWatch").toStringList(),
            doctorPrepQuestion = json.optString("doctorPrepQuestion", "").trim()
                .ifBlank { "Given this recent change, when should I be evaluated in person?" }
        )
    }
}
