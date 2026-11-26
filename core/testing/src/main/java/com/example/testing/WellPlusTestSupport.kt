package com.example.testing

import com.example.core.AiEntitlementProvider
import com.example.core.AiQuotaCheckResult
import com.example.core.SanctuaryDeviceSettings
import com.example.data.model.Consultation
import com.example.data.model.ConsultationType
import com.example.data.model.CycleFlowLevel
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.FollowUpStatus
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.LabBiomarkerCategory
import com.example.data.model.LabBiomarkerStatus
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.UrgencyLevel
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Shared JUnit4 rule replacing `Dispatchers.Main` with a controllable [TestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/**
 * Deterministic in-memory [AiEntitlementProvider] fake for fast JVM tests across modules.
 */
class FakeAiEntitlementProvider(
    override var cloudAiGloballyEnabled: Boolean = true,
    override var dailyLimit: Int = 25,
    override var urgentSafetyNotice: String? = null,
    override var labBiomarkerAiEnabled: Boolean = true,
    override var clarifyingQuestionsEnabled: Boolean = true,
    override var proactiveReassessmentEnabled: Boolean = true
) : AiEntitlementProvider {

    var consumedCount: Int = 0
    private val cache = mutableMapOf<String, Pair<String, Any>>()

    override fun computeRemaining(settings: SanctuaryDeviceSettings): Int =
        (dailyLimit - consumedCount).coerceAtLeast(0)

    override suspend fun tryConsumeDailyQuota(): AiQuotaCheckResult {
        if (consumedCount >= dailyLimit) {
            return AiQuotaCheckResult.DailyLimitReached(dailyLimit)
        }
        consumedCount++
        return AiQuotaCheckResult.Allowed(
            remainingAfter = dailyLimit - consumedCount,
            dailyLimit = dailyLimit
        )
    }

    override fun fingerprintOf(vararg parts: Any?): String =
        parts.joinToString("|") { it?.toString().orEmpty() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getCached(cacheKey: String, fingerprint: String): T? {
        val entry = cache[cacheKey] ?: return null
        return if (entry.first == fingerprint) entry.second as? T else null
    }

    override fun <T : Any> putCached(cacheKey: String, fingerprint: String, value: T) {
        cache[cacheKey] = fingerprint to value
    }

    override fun clearCache() {
        cache.clear()
    }
}

/**
 * Reusable clinical fixtures for per-module unit tests (never used in production UI).
 */
object WellPlusTestFixtures {
    const val TEST_UID = "test-user-wellplus-001"

    fun sampleProfile(regionCode: String = "US"): HealthProfile = HealthProfile(
        userId = TEST_UID,
        displayName = "Alex Rivera",
        age = 36L,
        allergies = listOf("Penicillin"),
        medications = listOf("Lisinopril 10mg"),
        conditions = listOf("Mild Hypertension"),
        onboardingCompleted = true,
        disclaimerAccepted = true,
        ageConfirmed18Plus = true,
        preferredHealthRegion = regionCode
    )

    fun sampleConsultation(
        id: String = "consult-1",
        urgency: UrgencyLevel = UrgencyLevel.DOCTOR_SOON,
        followUp: FollowUpStatus = FollowUpStatus.PENDING
    ): Consultation = Consultation(
        id = id,
        userId = TEST_UID,
        consultationType = ConsultationType.SYMPTOM_CHECK.name,
        title = "Persistent Tension Headache",
        bodyArea = "Head & Neuro",
        symptomSummary = "Dull headache behind eyes for 2 days",
        severity = 5L,
        duration = "2–3 days",
        associatedSymptoms = listOf("Light sensitivity"),
        urgencyLevel = urgency.name,
        aiSummary = "Likely tension-related headache; monitor hydration and screen strain.",
        followUpStatus = followUp.name
    )

    fun sampleVital(
        type: VitalType = VitalType.BLOOD_PRESSURE,
        primary: Double = 148.0,
        secondary: Double? = 94.0,
        unit: String = "mmHg"
    ): VitalReading = VitalReading(
        id = "vital-1",
        userId = TEST_UID,
        vitalType = type.name,
        primaryValue = primary,
        secondaryValue = secondary,
        unit = unit
    )

    fun sampleMedication(
        id: String = "med-1",
        name: String = "Lisinopril",
        slot: MedicationScheduleSlot = MedicationScheduleSlot.MORNING
    ): MedicationItem = MedicationItem(
        id = id,
        userId = TEST_UID,
        name = name,
        dosage = "10 mg",
        scheduleSlot = slot.name,
        active = true
    )

    fun sampleCheckIn(dateKey: String = "2026-11-25", moodLevel: Long = 4L): DailyCheckIn =
        DailyCheckIn(
            id = dateKey,
            userId = TEST_UID,
            dateKey = dateKey,
            moodLevel = moodLevel,
            energyLevel = 4L,
            sleepHours = 7.5,
            hydrationGlasses = 7L
        )

    fun sampleLabBiomarker(
        id: String = "lab-1",
        markerName: String = "HbA1c",
        value: Double = 5.4,
        unit: String = "%",
        status: LabBiomarkerStatus = LabBiomarkerStatus.OPTIMAL,
        dateKey: String = "2026-11-20"
    ): LabBiomarker = LabBiomarker(
        id = id,
        userId = TEST_UID,
        markerName = markerName,
        category = LabBiomarkerCategory.METABOLIC.name,
        value = value,
        unit = unit,
        referenceRangeText = "4.0 - 5.6 %",
        status = status.name,
        panelDateKey = dateKey
    )

    fun sampleCycleLog(dateKey: String, flow: CycleFlowLevel = CycleFlowLevel.MEDIUM): CycleLog =
        CycleLog(
            id = dateKey,
            userId = TEST_UID,
            dateKey = dateKey,
            flowLevel = flow.name
        )
}
