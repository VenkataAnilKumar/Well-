package com.example.core

import com.example.data.model.ProposedChatAction
import com.example.data.model.ProposedChatActionType
import com.example.data.model.TrustedMedicalCitation
import com.example.data.model.VitalType
import java.net.URI
import java.util.Locale

enum class PiiCategory(val displayLabel: String) {
    SSN("SSN / SIN"),
    MRN("Medical Record / Patient ID"),
    PHONE("Phone Number"),
    EMAIL("Email Address"),
    DOB("Date of Birth")
}

data class PiiMatchSummary(
    val category: PiiCategory,
    val count: Int
)

/**
 * Pure-Kotlin deterministic PII line classifier used both on-device with ML Kit OCR bounding boxes
 * and directly in fast JVM unit tests.
 */
object PiiPatternClassifier {

    private val ssnRegex = Regex("""\b\d{3}[-.\s]\d{2}[-.\s]\d{4}\b""")
    private val canadaSinRegex = Regex("""\b\d{3}[-.\s]\d{3}[-.\s]\d{3}\b""")
    private val phoneRegex = Regex("""(?<!\d)(?:\+?1[-.\s]?)?(?:\(\d{3}\)|\d{3})[-.\s]\d{3}[-.\s]\d{4}(?!\d)""")
    private val emailRegex = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
    private val mrnKeywordRegex = Regex(
        """(?i)\b(?:mrn|medical\s*record|patient\s*id|acct|account\s*#|chart\s*#|health\s*card|ohip|phn|rx\s*#?)\s*[:#-]?\s*[A-Z0-9-]{4,}"""
    )
    private val dobKeywordRegex = Regex(
        """(?i)\b(?:dob|date\s*of\s*birth|birth\s*date|born)\s*[:#-]?\s*(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}[/-]\d{1,2}[/-]\d{1,2}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})"""
    )

    fun detectCategories(lineText: String): Set<PiiCategory> {
        val text = lineText.trim()
        if (text.length < 5) return emptySet()
        val found = linkedSetOf<PiiCategory>()
        if (ssnRegex.containsMatchIn(text) || canadaSinRegex.containsMatchIn(text)) {
            found.add(PiiCategory.SSN)
        }
        if (mrnKeywordRegex.containsMatchIn(text)) {
            found.add(PiiCategory.MRN)
        }
        if (phoneRegex.containsMatchIn(text)) {
            found.add(PiiCategory.PHONE)
        }
        if (emailRegex.containsMatchIn(text)) {
            found.add(PiiCategory.EMAIL)
        }
        if (dobKeywordRegex.containsMatchIn(text)) {
            found.add(PiiCategory.DOB)
        }
        return found
    }

    fun summarizeCategories(categoriesPerBox: List<Set<PiiCategory>>): List<PiiMatchSummary> {
        val counts = linkedMapOf<PiiCategory, Int>()
        categoriesPerBox.forEach { set ->
            set.forEach { cat ->
                counts[cat] = (counts[cat] ?: 0) + 1
            }
        }
        return counts.entries.map { (cat, count) -> PiiMatchSummary(cat, count) }
    }
}

/**
 * Enforces an HTTPS domain allowlist of authoritative public-health organizations so AI citations
 * never surface phishing, broken, or untrusted links.
 */
object TrustedMedicalCitationValidator {

    private val allowedDomains: Set<String> = setOf(
        "medlineplus.gov",
        "nih.gov",
        "cdc.gov",
        "mayoclinic.org",
        "nhs.uk",
        "canada.ca",
        "who.int",
        "clevelandclinic.org",
        "heart.org",
        "diabetes.org"
    )

    fun isAllowedHttpsUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false
        return try {
            val uri = URI(trimmed)
            val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return false
            if (uri.userInfo != null) return false
            allowedDomains.any { domain -> host == domain || host.endsWith(".$domain") }
        } catch (_: Exception) {
            false
        }
    }

    fun filterTrusted(citations: List<TrustedMedicalCitation>): List<TrustedMedicalCitation> {
        val seenUrls = linkedSetOf<String>()
        return citations.mapNotNull { item ->
            val cleanTitle = item.title.trim().take(120)
            val cleanOrg = item.sourceOrganization.trim().take(60)
            val cleanUrl = item.url.trim()
            if (cleanTitle.isEmpty() || cleanOrg.isEmpty() || !isAllowedHttpsUrl(cleanUrl)) {
                null
            } else if (!seenUrls.add(cleanUrl.lowercase(Locale.US))) {
                null
            } else {
                item.copy(
                    title = cleanTitle,
                    sourceOrganization = cleanOrg,
                    url = cleanUrl,
                    topicTag = item.topicTag.trim().take(32)
                )
            }
        }.take(4)
    }

    /**
     * Deterministic fallback public-health references when AI citations are empty or unavailable.
     */
    fun fallbackReferencesForTopic(bodyArea: String, isCanada: Boolean): List<TrustedMedicalCitation> {
        val normalized = bodyArea.lowercase(Locale.US)
        val list = mutableListOf<TrustedMedicalCitation>()
        when {
            normalized.contains("chest") || normalized.contains("heart") -> {
                list.add(
                    TrustedMedicalCitation(
                        title = "Warning Signs of a Heart Attack & Chest Pain",
                        sourceOrganization = "American Heart Association",
                        url = "https://www.heart.org/en/health-topics/heart-attack/warning-signs-of-a-heart-attack",
                        topicTag = "Cardiovascular"
                    )
                )
            }
            normalized.contains("head") || normalized.contains("neuro") -> {
                list.add(
                    TrustedMedicalCitation(
                        title = "Headache Overview & When to Seek Care",
                        sourceOrganization = "NIH MedlinePlus",
                        url = "https://medlineplus.gov/headache.html",
                        topicTag = "Neurology"
                    )
                )
            }
            normalized.contains("respiratory") || normalized.contains("throat") || normalized.contains("ent") -> {
                list.add(
                    TrustedMedicalCitation(
                        title = "Respiratory Illnesses & Symptom Guidance",
                        sourceOrganization = "CDC",
                        url = "https://www.cdc.gov/respiratory-viruses/index.html",
                        topicTag = "Respiratory"
                    )
                )
            }
            normalized.contains("digest") || normalized.contains("stomach") || normalized.contains("abdom") -> {
                list.add(
                    TrustedMedicalCitation(
                        title = "Digestive Diseases & Abdominal Symptoms",
                        sourceOrganization = "NIH MedlinePlus",
                        url = "https://medlineplus.gov/digestivediseases.html",
                        topicTag = "Digestive"
                    )
                )
            }
            else -> {
                list.add(
                    TrustedMedicalCitation(
                        title = "Health Topics & Symptom Encyclopedia",
                        sourceOrganization = "NIH MedlinePlus",
                        url = "https://medlineplus.gov/healthtopics.html",
                        topicTag = "General Health"
                    )
                )
            }
        }
        if (isCanada) {
            list.add(
                TrustedMedicalCitation(
                    title = "Health Services & Public Health Guidance",
                    sourceOrganization = "Health Canada",
                    url = "https://www.canada.ca/en/health-canada.html",
                    topicTag = "Canada Health"
                )
            )
        } else {
            list.add(
                TrustedMedicalCitation(
                    title = "Symptoms & Self-Care Reference Library",
                    sourceOrganization = "CDC",
                    url = "https://www.cdc.gov/index.html",
                    topicTag = "Public Health"
                )
            )
        }
        return filterTrusted(list)
    }
}

/**
 * Validates and clamps AI-proposed chat actions against exact `firestore.rules` bounds
 * before rendering confirmation cards so invalid suggestions never produce broken saves.
 */
object ProposedChatActionValidator {

    fun sanitizeAll(raw: List<ProposedChatAction>): List<ProposedChatAction> =
        raw.mapNotNull(::sanitize).take(3)

    fun sanitize(action: ProposedChatAction): ProposedChatAction? {
        val cleanTitle = action.title.trim().take(120)
        if (cleanTitle.isBlank()) return null
        val cleanSubtitle = action.subtitle.trim().take(200)

        return when (action.actionType) {
            ProposedChatActionType.ADD_CARE_TASK -> action.copy(
                title = cleanTitle.take(200),
                subtitle = cleanSubtitle.ifBlank { "Add to your Today Care Checklist" }
            )

            ProposedChatActionType.ADD_MEDICATION -> {
                val cleanName = action.medicationName.trim().ifBlank { cleanTitle }.take(100)
                if (cleanName.isBlank()) return null
                action.copy(
                    title = cleanTitle,
                    subtitle = cleanSubtitle.ifBlank {
                        listOf(action.medicationDosage.trim(), action.medicationSlot.label)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                            .ifBlank { "Track in Medications" }
                    },
                    medicationName = cleanName,
                    medicationDosage = action.medicationDosage.trim().take(60),
                    medicationInstructions = action.medicationInstructions.trim().take(240)
                )
            }

            ProposedChatActionType.LOG_VITAL -> {
                val primary = action.vitalPrimaryValue ?: return null
                if (!isPhysiologicallyValid(action.vitalType, primary, action.vitalSecondaryValue)) {
                    return null
                }
                val secondary = if (action.vitalType == VitalType.BLOOD_PRESSURE) {
                    action.vitalSecondaryValue
                } else {
                    null
                }
                action.copy(
                    title = cleanTitle,
                    subtitle = cleanSubtitle.ifBlank { "Save to Vitals history" },
                    vitalPrimaryValue = primary,
                    vitalSecondaryValue = secondary,
                    vitalNote = action.vitalNote.trim().take(300)
                )
            }

            ProposedChatActionType.SCHEDULE_FOLLOWUP -> action.copy(
                title = cleanTitle,
                subtitle = cleanSubtitle.ifBlank { "Reminder in 48 hours to review symptom progress" }
            )
        }
    }

    private fun isPhysiologicallyValid(
        type: VitalType,
        primary: Double,
        secondary: Double?
    ): Boolean {
        if (primary.isNaN() || primary.isInfinite() || primary <= 0.0 || primary > 1000.0) return false
        return when (type) {
            VitalType.BLOOD_PRESSURE -> {
                val dia = secondary ?: return false
                primary in 60.0..260.0 && dia in 35.0..180.0 && primary > dia
            }
            VitalType.HEART_RATE -> primary in 25.0..240.0
            VitalType.TEMPERATURE -> primary in 90.0..112.0
            VitalType.SPO2 -> primary in 50.0..100.0
            VitalType.WEIGHT -> primary in 30.0..900.0
            VitalType.BLOOD_GLUCOSE -> primary in 20.0..700.0
        }
    }
}
