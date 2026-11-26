package com.example.feature.medications

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ReminderScheduler
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationInteractionReport
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MedicationScheduleSlot
import com.example.data.repository.MedicationRepository
import com.example.data.repository.ai.MedicationAiService
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.InteractionCheckUiState
import com.example.ui.viewmodel.PrescriptionScanUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "MedicationsVM"

class MedicationsViewModel(
    private val appContext: Context,
    private val session: SessionCoordinator,
    private val medicationRepository: MedicationRepository,
    private val medicationAiService: MedicationAiService,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>
) : ViewModel() {

    private val _interactionState = MutableStateFlow<InteractionCheckUiState>(InteractionCheckUiState.Idle)
    val interactionState: StateFlow<InteractionCheckUiState> = _interactionState.asStateFlow()

    private val _prescriptionScanState = MutableStateFlow<PrescriptionScanUiState>(PrescriptionScanUiState.Idle)
    val prescriptionScanState: StateFlow<PrescriptionScanUiState> = _prescriptionScanState.asStateFlow()

    fun saveMedication(
        existingId: String? = null,
        name: String,
        dosage: String,
        scheduleSlot: MedicationScheduleSlot,
        reminderHour: Int,
        reminderMinute: Int,
        instructions: String,
        active: Boolean = true,
        enableRemindersIfRequested: Boolean = false
    ) {
        viewModelScope.launch {
            val res = medicationRepository.saveMedication(
                userId = session.currentUserId,
                existingId = existingId,
                name = name,
                dosage = dosage,
                scheduleSlot = scheduleSlot,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                instructions = instructions,
                active = active
            )
            res.onSuccess {
                if (enableRemindersIfRequested && !session.deviceSettings.value.medRemindersEnabled) {
                    session.prefsStore.setMedicationRemindersEnabled(true)
                }
                val formattedTime = MedicationItem(
                    reminderHour = reminderHour.toLong(),
                    reminderMinute = reminderMinute.toLong()
                ).formattedReminderTime()
                session.showMessage(
                    when {
                        existingId == null && (enableRemindersIfRequested || session.deviceSettings.value.medRemindersEnabled) &&
                            scheduleSlot != MedicationScheduleSlot.AS_NEEDED ->
                            "Saved $name with daily $formattedTime reminder."
                        existingId == null -> "Added $name to your prescription tracker."
                        else -> "Updated $name."
                    }
                )
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save medication.")
            }
        }
    }

    fun toggleMedicationActive(item: MedicationItem) {
        viewModelScope.launch {
            val res = medicationRepository.setMedicationActive(session.currentUserId, item.id, !item.active)
            res.onSuccess {
                session.showMessage(if (!item.active) "${item.name} resumed." else "${item.name} paused.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not update medication.")
            }
        }
    }

    fun deleteMedication(medicationId: String) {
        viewModelScope.launch {
            val res = medicationRepository.deleteMedication(session.currentUserId, medicationId)
            res.onSuccess {
                session.showMessage("Medication removed.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not remove medication.")
            }
        }
    }

    fun logMedicationDose(medication: MedicationItem, status: MedicationLogStatus) {
        viewModelScope.launch {
            val res = medicationRepository.logMedicationDose(
                userId = session.currentUserId,
                dateKey = session.todayDateKey,
                medication = medication,
                status = status
            )
            res.onSuccess {
                session.showMessage("${medication.name} marked ${status.label.lowercase(Locale.US)}.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not log dose.")
            }
        }
    }

    fun setMedicationRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setMedicationRemindersEnabled(enabled)
            val meds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
            ReminderScheduler.syncMedicationReminders(appContext, enabled, meds)
            val scheduledCount = meds.count {
                it.active && it.parsedSlot != MedicationScheduleSlot.AS_NEEDED
            }
            session.showMessage(
                if (enabled) {
                    if (scheduledCount > 0) {
                        "Scheduled $scheduledCount daily medication reminder(s) via WorkManager."
                    } else {
                        "Daily medication reminders enabled. Add a prescription to schedule alerts."
                    }
                } else {
                    "Daily medication reminders turned off."
                }
            )
        }
    }

    fun sendTestMedicationReminder() {
        val meds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val sample = meds.firstOrNull { it.active && it.parsedSlot != MedicationScheduleSlot.AS_NEEDED }
            ?: meds.firstOrNull { it.active }
        ReminderScheduler.sendTestMedicationNotification(appContext, sample)
        session.showMessage("Test medication push notification queued via WorkManager.")
    }

    fun runMedicationInteractionCheck() {
        if (_interactionState.value is InteractionCheckUiState.Loading) return
        val medications = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
        val profile = (profileState.value as? CloudUiState.Success)?.data
        val activeTrackedCount = medications.count { it.active }
        val profileMedCount = profile?.medications.orEmpty().size

        if (activeTrackedCount + profileMedCount == 0) {
            _interactionState.value = InteractionCheckUiState.Error(
                "Add at least one active medication or supplement to run an AI drug & allergy safety check."
            )
            return
        }

        val activeRegion = session.activeHealthRegion.value
        val entitlements = session.entitlementProvider
        val fingerprint = entitlements.fingerprintOf(
            "meds_interaction",
            activeRegion.code.code,
            medications.filter { it.active }.map { "${it.name}:${it.dosage}:${it.scheduleSlot}" },
            profile?.allergies,
            profile?.medications,
            profile?.conditions,
            profile?.age
        )
        entitlements.getCached<MedicationInteractionReport>("meds_interaction", fingerprint)?.let { cached ->
            _interactionState.value = InteractionCheckUiState.Success(cached)
            session.showMessage("Loaded cached safety check (0 daily AI credits used).")
            return
        }

        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _interactionState.value = InteractionCheckUiState.Error(blockReason)
                return@launch
            }
            _interactionState.value = InteractionCheckUiState.Loading
            try {
                val report = medicationAiService.analyzeMedicationInteractions(
                    medications = medications,
                    profile = profile,
                    region = activeRegion
                )
                entitlements.putCached("meds_interaction", fingerprint, report)
                _interactionState.value = InteractionCheckUiState.Success(report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Medication interaction check failed", e)
                _interactionState.value = InteractionCheckUiState.Error(
                    e.localizedMessage ?: "Unable to complete interaction safety check right now."
                )
            }
        }
    }

    fun clearInteractionReport() {
        _interactionState.value = InteractionCheckUiState.Idle
    }

    fun scanPrescriptionBottle(bitmap: Bitmap) {
        if (_prescriptionScanState.value is PrescriptionScanUiState.Scanning) return
        viewModelScope.launch {
            val blockReason = session.guardCloudAiAndConsumeQuota()
            if (blockReason != null) {
                _prescriptionScanState.value = PrescriptionScanUiState.Error(blockReason)
                return@launch
            }
            _prescriptionScanState.value = PrescriptionScanUiState.Scanning
            try {
                val draft = medicationAiService.scanPrescriptionLabel(bitmap)
                _prescriptionScanState.value = PrescriptionScanUiState.Ready(draft)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Prescription label scan failed", e)
                _prescriptionScanState.value = PrescriptionScanUiState.Error(
                    e.localizedMessage ?: "Could not read prescription label. Try a brighter, closer photo."
                )
            }
        }
    }

    fun clearPrescriptionScan() {
        _prescriptionScanState.value = PrescriptionScanUiState.Idle
    }
}
