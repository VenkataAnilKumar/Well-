package com.example.data.repository.ai

import android.util.Log
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.ProposedChatActionValidator
import com.example.core.TrustedMedicalCitationValidator
import com.example.data.model.CareCategory
import com.example.data.model.ChatTurnExtras
import com.example.data.model.Consultation
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.ProposedChatAction
import com.example.data.model.ProposedChatActionType
import com.example.data.model.SoapPreVisitBrief
import com.example.data.model.TrustedMedicalCitation
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
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
import java.util.Locale

/**
 * Post-turn structured AI extractor (`gemini-3.5-flash-lite`) that proposes 0-3 safe,
 * user-confirmable in-app actions (Care Task, Medication, Vital, 48h Follow-Up) and
 * authoritative public-health medical references validated against an HTTPS domain allowlist.
 */
class ChatActionAndCitationAiService {

    private val extrasSchema = Schema.obj(
        properties = mapOf(
            "actions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "actionType" to Schema.enumeration(
                            values = listOf(
                                "ADD_CARE_TASK",
                                "ADD_MEDICATION",
                                "LOG_VITAL",
                                "SCHEDULE_FOLLOWUP"
                            )
                        ),
                        "title" to Schema.string(description = "Short action title (max 80 chars)"),
                        "subtitle" to Schema.string(description = "Clear 1-line summary of what will be saved"),
                        "careCategory" to Schema.enumeration(
                            values = listOf("AI_SUGGESTION", "MEDICATION", "HYDRATION", "RECOVERY", "CUSTOM"),
                            nullable = true
                        ),
                        "medicationName" to Schema.string(nullable = true),
                        "medicationDosage" to Schema.string(nullable = true),
                        "medicationSlot" to Schema.enumeration(
                            values = listOf("MORNING", "MIDDAY", "EVENING", "BEDTIME", "AS_NEEDED"),
                            nullable = true
                        ),
                        "medicationInstructions" to Schema.string(nullable = true),
                        "vitalType" to Schema.enumeration(
                            values = listOf(
                                "BLOOD_PRESSURE",
                                "HEART_RATE",
                                "TEMPERATURE",
                                "SPO2",
                                "WEIGHT",
                                "BLOOD_GLUCOSE"
                            ),
                            nullable = true
                        ),
                        "vitalPrimaryValue" to Schema.double(nullable = true),
                        "vitalSecondaryValue" to Schema.double(nullable = true),
                        "vitalNote" to Schema.string(nullable = true)
                    ),
                    optionalProperties = listOf(
                        "careCategory",
                        "medicationName",
                        "medicationDosage",
                        "medicationSlot",
                        "medicationInstructions",
                        "vitalType",
                        "vitalPrimaryValue",
                        "vitalSecondaryValue",
                        "vitalNote"
                    )
                )
            ),
            "citations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Article or public health page title"),
                        "sourceOrganization" to Schema.string(description = "Authoritative source e.g. NIH MedlinePlus, CDC, Mayo Clinic, NHS, Health Canada"),
                        "url" to Schema.string(description = "Real HTTPS URL on medlineplus.gov, nih.gov, cdc.gov, mayoclinic.org, nhs.uk, canada.ca, or heart.org"),
                        "topicTag" to Schema.string(description = "Short 1-2 word clinical topic tag")
                    )
                )
            )
        )
    )

    private val extrasModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_AI_MODEL,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = extrasSchema
            },
            systemInstruction = content {
                text(
                    """
                    You extract optional, high-confidence in-app health actions and verified public-health citations
                    from a Well+ AI Doctor reply.
                    Rules:
                    - Return 0 to 3 genuinely helpful actions (do NOT invent actions if none apply).
                    - Only propose LOG_VITAL when the user explicitly mentioned a numeric vital measurement (e.g., BP 138/88, temp 100.8, HR 92, SpO2 97).
                    - Only propose ADD_MEDICATION for non-prescription OTC/tracked items explicitly discussed with the user, never invent prescription doses.
                    - Prefer ADD_CARE_TASK or SCHEDULE_FOLLOWUP for hydration, monitoring, symptom tracking, or clinician prep.
                    - Return 1 to 3 authoritative HTTPS citations ONLY from medlineplus.gov, cdc.gov, nih.gov, mayoclinic.org, nhs.uk, canada.ca, or heart.org.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun extractChatTurnExtras(
        consultation: Consultation,
        userMessage: String,
        doctorReply: String,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): ChatTurnExtras = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("Region: ${region.countryName}")
            appendLine("Consultation: ${consultation.title} (${consultation.bodyArea}, Urgency: ${consultation.parsedUrgency.label})")
            appendLine("User message: ${userMessage.take(600)}")
            appendLine("AI Doctor reply: ${doctorReply.take(1500)}")
            appendLine("Extract 0-3 safe confirmable actions and 1-3 authoritative HTTPS citations.")
        }
        try {
            val response = withTimeout(22_000L) {
                extrasModel.generateContent(prompt)
            }
            val raw = response.text?.takeIf { it.isNotBlank() } ?: return@withContext fallbackExtras(consultation, region)
            val json = JSONObject(raw)

            val parsedActions = mutableListOf<ProposedChatAction>()
            val actionsArr = json.optJSONArray("actions")
            if (actionsArr != null) {
                for (i in 0 until actionsArr.length()) {
                    val obj = actionsArr.optJSONObject(i) ?: continue
                    val type = runCatching {
                        ProposedChatActionType.valueOf(obj.optString("actionType"))
                    }.getOrNull() ?: continue
                    val careCat = runCatching {
                        CareCategory.valueOf(obj.optString("careCategory", "RECOVERY"))
                    }.getOrDefault(CareCategory.RECOVERY)
                    val medSlot = runCatching {
                        MedicationScheduleSlot.valueOf(obj.optString("medicationSlot", "MORNING"))
                    }.getOrDefault(MedicationScheduleSlot.MORNING)
                    val vitalType = runCatching {
                        VitalType.valueOf(obj.optString("vitalType", "BLOOD_PRESSURE"))
                    }.getOrDefault(VitalType.BLOOD_PRESSURE)

                    val primaryVal = if (obj.has("vitalPrimaryValue") && !obj.isNull("vitalPrimaryValue")) {
                        obj.optDouble("vitalPrimaryValue").takeIf { !it.isNaN() }
                    } else null
                    val secondaryVal = if (obj.has("vitalSecondaryValue") && !obj.isNull("vitalSecondaryValue")) {
                        obj.optDouble("vitalSecondaryValue").takeIf { !it.isNaN() }
                    } else null

                    parsedActions.add(
                        ProposedChatAction(
                            actionType = type,
                            title = obj.optString("title"),
                            subtitle = obj.optString("subtitle"),
                            careCategory = careCat,
                            medicationName = obj.optString("medicationName"),
                            medicationDosage = obj.optString("medicationDosage"),
                            medicationSlot = medSlot,
                            medicationInstructions = obj.optString("medicationInstructions"),
                            vitalType = vitalType,
                            vitalPrimaryValue = primaryVal,
                            vitalSecondaryValue = secondaryVal,
                            vitalNote = obj.optString("vitalNote")
                        )
                    )
                }
            }

            val parsedCitations = mutableListOf<TrustedMedicalCitation>()
            val citationsArr = json.optJSONArray("citations")
            if (citationsArr != null) {
                for (i in 0 until citationsArr.length()) {
                    val obj = citationsArr.optJSONObject(i) ?: continue
                    parsedCitations.add(
                        TrustedMedicalCitation(
                            title = obj.optString("title"),
                            sourceOrganization = obj.optString("sourceOrganization"),
                            url = obj.optString("url"),
                            topicTag = obj.optString("topicTag")
                        )
                    )
                }
            }

            val validActions = ProposedChatActionValidator.sanitizeAll(parsedActions)
            val validCitations = TrustedMedicalCitationValidator.filterTrusted(parsedCitations)
                .ifEmpty {
                    TrustedMedicalCitationValidator.fallbackReferencesForTopic(
                        bodyArea = consultation.bodyArea,
                        isCanada = region.isCanada
                    )
                }
            ChatTurnExtras(
                proposedActions = validActions,
                citations = validCitations
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(AI_LOG_TAG, "Chat extras extraction fell back cleanly: ${e.message}")
            fallbackExtras(consultation, region)
        }
    }

    private fun fallbackExtras(
        consultation: Consultation,
        region: HealthRegionProfile
    ): ChatTurnExtras = ChatTurnExtras(
        proposedActions = emptyList(),
        citations = TrustedMedicalCitationValidator.fallbackReferencesForTopic(
            bodyArea = consultation.bodyArea,
            isCanada = region.isCanada
        )
    )
}

/**
 * Synthesizes a concise 1-page SOAP Pre-Visit Clinical Brief (`gemini-3.8-flash`)
 * embedded at the top of the exported Doctor Visit PDF.
 */
class SoapPreVisitAiService {

    private val soapSchema = Schema.obj(
        properties = mapOf(
            "subjectiveSummary" to Schema.string(description = "2-3 concise sentences summarizing recent patient symptoms, severity trend, and daily wellness context."),
            "objectiveHighlights" to Schema.array(
                items = Schema.string(),
                description = "Up to 5 objective bullet points citing latest recorded vitals, lab biomarkers, and adherence metrics."
            ),
            "assessmentDiscussionPoints" to Schema.array(
                items = Schema.string(),
                description = "Up to 4 non-diagnostic clinical discussion themes or flagged changes for the clinician to review."
            ),
            "planAndMedications" to Schema.array(
                items = Schema.string(),
                description = "Up to 4 concise notes summarizing active medications, allergies, and current self-care plan."
            ),
            "priorityClinicianQuestions" to Schema.array(
                items = Schema.string(),
                description = "3 focused, high-value questions the patient can ask during the appointment."
            )
        )
    )

    private val soapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_AI_MODEL,
            generationConfig = generationConfig {
                temperature = 0.25f
                responseMimeType = "application/json"
                responseSchema = soapSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are Well+, generating a crisp, neutral 1-page SOAP Pre-Visit Brief for a patient to hand to their clinician.
                    Frame all content strictly as patient-reported personal records and non-diagnostic discussion points.
                    Never fabricate numbers—only cite vitals, lab biomarkers, medications, or symptoms present in the provided records.
                    Keep every bullet concise (under 140 characters) so the brief fits cleanly on Page 1 of a medical PDF.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateSoapBrief(
        profile: HealthProfile?,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        vitals: List<VitalReading>,
        labBiomarkers: List<LabBiomarker>,
        consultations: List<Consultation>,
        checkIns: List<DailyCheckIn>,
        daysWindow: Int,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): SoapPreVisitBrief = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("Prepare a 1-page SOAP Pre-Visit Brief covering the last $daysWindow days (${region.countryName}).")
            appendLine("Profile: ${profile?.toPromptContext(medications) ?: "Not provided"}")
            if (consultations.isNotEmpty()) {
                appendLine("Recent Consultations:")
                consultations.take(5).forEach { c ->
                    appendLine("- ${c.title} (${c.bodyArea}, severity ${c.severity}/10, ${c.parsedUrgency.label}, outcome ${c.parsedFollowUpStatus.label}): ${c.symptomSummary.take(180)}")
                }
            }
            if (vitals.isNotEmpty()) {
                appendLine("Latest Vitals:")
                VitalType.entries.mapNotNull { t -> vitals.firstOrNull { it.parsedType == t } }.forEach { v ->
                    appendLine("- ${v.parsedType.label}: ${v.formattedReadingForRegion(region)} (${v.rangeStatus.label})")
                }
            }
            if (labBiomarkers.isNotEmpty()) {
                appendLine("Recent Lab Biomarkers:")
                labBiomarkers.take(10).forEach { lb ->
                    appendLine("- ${lb.markerName}: ${lb.formattedValue} (${lb.parsedStatus.label}, ref ${lb.referenceRangeText}, date ${lb.panelDateKey})")
                }
            }
            if (medicationLogs.isNotEmpty()) {
                val taken = medicationLogs.count { it.parsedStatus == MedicationLogStatus.TAKEN }
                val pct = ((taken.toDouble() / medicationLogs.size) * 100).toInt()
                appendLine("Medication Adherence: $pct% ($taken/${medicationLogs.size} logged doses taken)")
            }
            if (checkIns.isNotEmpty()) {
                val avgMood = String.format(Locale.US, "%.1f", checkIns.take(daysWindow).map { it.moodLevel }.average())
                val avgSleep = String.format(Locale.US, "%.1f", checkIns.take(daysWindow).map { it.sleepHours }.average())
                appendLine("Wellness Check-Ins (${checkIns.size}d): avg mood $avgMood/5, avg sleep $avgSleep hrs")
            }
        }

        try {
            val response = withTimeout(30_000L) {
                soapModel.generateContent(prompt)
            }
            val raw = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("AI returned an empty SOAP brief.")
            val json = JSONObject(raw)
            with(AiServiceSupport) {
                SoapPreVisitBrief(
                    subjectiveSummary = json.optString("subjectiveSummary").trim().take(480),
                    objectiveHighlights = json.optJSONArray("objectiveHighlights").toStringList().take(5),
                    assessmentDiscussionPoints = json.optJSONArray("assessmentDiscussionPoints").toStringList().take(4),
                    planAndMedications = json.optJSONArray("planAndMedications").toStringList().take(4),
                    priorityClinicianQuestions = json.optJSONArray("priorityClinicianQuestions").toStringList().take(3)
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(AI_LOG_TAG, "SOAP Pre-Visit Brief timed out", e)
            throw IllegalStateException("SOAP AI summary timed out.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_LOG_TAG, "SOAP Pre-Visit Brief failed", e)
            throw e
        }
    }
}
