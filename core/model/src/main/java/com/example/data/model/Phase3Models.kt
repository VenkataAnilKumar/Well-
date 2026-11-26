package com.example.data.model

import java.util.UUID

/**
 * Action types that the AI Doctor may propose in Follow-Up Chat for explicit 1-tap user confirmation.
 */
enum class ProposedChatActionType(val label: String) {
    ADD_CARE_TASK("Add Care Task"),
    ADD_MEDICATION("Add Medication Reminder"),
    LOG_VITAL("Log Vital Sign"),
    SCHEDULE_FOLLOWUP("Schedule 48h Follow-Up")
}

enum class ProposedChatActionStatus {
    PENDING,
    SAVING,
    CONFIRMED,
    DISMISSED
}

/**
 * Validated, user-confirmable follow-up action proposed alongside an AI Doctor chat reply.
 */
data class ProposedChatAction(
    val id: String = UUID.randomUUID().toString(),
    val actionType: ProposedChatActionType,
    val title: String,
    val subtitle: String,
    val careCategory: CareCategory = CareCategory.RECOVERY,
    val medicationName: String = "",
    val medicationDosage: String = "",
    val medicationSlot: MedicationScheduleSlot = MedicationScheduleSlot.MORNING,
    val medicationInstructions: String = "",
    val vitalType: VitalType = VitalType.BLOOD_PRESSURE,
    val vitalPrimaryValue: Double? = null,
    val vitalSecondaryValue: Double? = null,
    val vitalNote: String = "",
    val status: ProposedChatActionStatus = ProposedChatActionStatus.PENDING
)

/**
 * Verifiable public-health citation validated against an authoritative HTTPS domain allowlist.
 */
data class TrustedMedicalCitation(
    val title: String,
    val sourceOrganization: String,
    val url: String,
    val topicTag: String = ""
)

/**
 * Combined post-turn structured extraction output for Follow-Up Chat.
 */
data class ChatTurnExtras(
    val proposedActions: List<ProposedChatAction> = emptyList(),
    val citations: List<TrustedMedicalCitation> = emptyList()
)

/**
 * AI-distilled 1-page SOAP Pre-Visit Brief embedded at the top of exported Doctor Visit PDFs.
 */
data class SoapPreVisitBrief(
    val subjectiveSummary: String,
    val objectiveHighlights: List<String>,
    val assessmentDiscussionPoints: List<String>,
    val planAndMedications: List<String>,
    val priorityClinicianQuestions: List<String>,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Commercial subscription tier backing [com.example.core.AiEntitlementProvider].
 */
enum class WellPlusTier(val label: String, val dailyAiQuota: Int) {
    FREE("Well+ Free", 25),
    PRO("Well+ Pro", 200)
}

data class SubscriptionPlanOffer(
    val productId: String,
    val title: String,
    val subtitle: String,
    val formattedPrice: String,
    val billingPeriodLabel: String,
    val offerToken: String = "",
    val badgeText: String = ""
)

data class WellPlusSubscriptionState(
    val tier: WellPlusTier = WellPlusTier.FREE,
    val billingConnected: Boolean = false,
    val storeAvailable: Boolean = false,
    val offers: List<SubscriptionPlanOffer> = emptyList(),
    val activeProductId: String? = null,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null
) {
    val isPro: Boolean get() = tier == WellPlusTier.PRO
}
