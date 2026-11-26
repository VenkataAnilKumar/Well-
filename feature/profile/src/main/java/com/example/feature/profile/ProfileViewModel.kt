package com.example.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.HealthRegionCode
import com.example.core.HealthRegionDirectory
import com.example.core.PersonalDataJsonExporter
import com.example.core.ReminderScheduler
import com.example.data.model.BiologicalSex
import com.example.data.model.BloodType
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareTask
import com.example.data.model.Consultation
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationScheduleSlot
import com.example.data.model.MentalAssessment
import com.example.data.model.VitalReading
import com.example.data.repository.AccountDataPortabilityRepository
import com.example.data.repository.LiveHeadPage
import com.example.data.repository.ProfileRepository
import com.example.di.SessionCoordinator
import com.example.ui.viewmodel.CloudUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val appContext: Context,
    private val session: SessionCoordinator,
    private val profileRepository: ProfileRepository,
    private val portabilityRepository: AccountDataPortabilityRepository,
    private val profileState: StateFlow<CloudUiState<HealthProfile?>>,
    private val consultationsHeadState: StateFlow<CloudUiState<LiveHeadPage<Consultation>>>,
    private val checkInsState: StateFlow<CloudUiState<List<DailyCheckIn>>>,
    private val careTasksState: StateFlow<CloudUiState<List<CareTask>>>,
    private val medicationsState: StateFlow<CloudUiState<List<MedicationItem>>>,
    private val medicationLogsState: StateFlow<CloudUiState<List<MedicationLog>>>,
    private val vitalsHeadState: StateFlow<CloudUiState<LiveHeadPage<VitalReading>>>,
    private val mentalAssessmentsState: StateFlow<CloudUiState<List<MentalAssessment>>>,
    private val bodyMeasurementsState: StateFlow<CloudUiState<List<BodyMeasurement>>>,
    private val cycleLogsState: StateFlow<CloudUiState<List<CycleLog>>>
) : ViewModel() {

    private val _isSavingOnboarding = MutableStateFlow(false)
    val isSavingOnboarding: StateFlow<Boolean> = _isSavingOnboarding.asStateFlow()

    private val _isExportingJson = MutableStateFlow(false)
    val isExportingJson: StateFlow<Boolean> = _isExportingJson.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    fun completeOnboarding(
        displayName: String,
        age: Long,
        allergies: List<String>,
        emergencyContactName: String,
        emergencyContactPhone: String,
        selectedRegion: HealthRegionCode = HealthRegionDirectory.detectSuggestedRegion(appContext).code,
        coverageProvider: String = "",
        coverageMemberId: String = "",
        primaryDoctorName: String = "",
        preferredPharmacy: String = "",
        enableCheckInReminder: Boolean,
        enableMedReminders: Boolean,
        enableBiometricLock: Boolean
    ) {
        if (_isSavingOnboarding.value) return
        val existing = (profileState.value as? CloudUiState.Success)?.data
        val regionCode = selectedRegion.code
        viewModelScope.launch {
            _isSavingOnboarding.value = true
            try {
                val res = profileRepository.saveProfile(
                    userId = session.currentUserId,
                    displayName = displayName.ifBlank {
                        existing?.displayName?.ifBlank { session.defaultDisplayName } ?: session.defaultDisplayName
                    },
                    age = age,
                    biologicalSex = BiologicalSex.fromRaw(existing?.biologicalSex),
                    bloodType = BloodType.fromCode(existing?.bloodType),
                    allergies = allergies,
                    medications = existing?.medications.orEmpty(),
                    conditions = existing?.conditions.orEmpty(),
                    wellnessGoals = existing?.wellnessGoals.orEmpty(),
                    emergencyContactName = emergencyContactName,
                    emergencyContactPhone = emergencyContactPhone,
                    onboardingCompleted = true,
                    disclaimerAccepted = true,
                    ageConfirmed18Plus = true,
                    preferredHealthRegion = regionCode,
                    preferredEmergencyCountry = existing?.preferredEmergencyCountry?.ifBlank { regionCode } ?: regionCode,
                    coverageProvider = coverageProvider.ifBlank { existing?.coverageProvider.orEmpty() },
                    coverageMemberId = coverageMemberId.ifBlank { existing?.coverageMemberId.orEmpty() },
                    primaryDoctorName = primaryDoctorName.ifBlank { existing?.primaryDoctorName.orEmpty() },
                    preferredPharmacy = preferredPharmacy.ifBlank { existing?.preferredPharmacy.orEmpty() }
                )
                res.onSuccess {
                    session.prefsStore.setPreferredHealthRegion(regionCode)
                    session.prefsStore.setUseMetricBodyUnits(selectedRegion == HealthRegionCode.CA)
                    if (session.deviceSettings.value.emergencyRegionOverride.isBlank()) {
                        session.prefsStore.setEmergencyRegionOverride(regionCode)
                    }
                    session.prefsStore.setBiometricLockEnabled(enableBiometricLock)
                    session.prefsStore.setCheckInReminder(enableCheckInReminder, 20, 0)
                    session.prefsStore.setMedicationRemindersEnabled(enableMedReminders)
                    ReminderScheduler.syncDailyCheckInReminder(appContext, enableCheckInReminder, 20, 0)
                    val meds = (medicationsState.value as? CloudUiState.Success)?.data.orEmpty()
                    ReminderScheduler.syncMedicationReminders(appContext, enableMedReminders, meds)
                    val regionName = HealthRegionDirectory.fromCode(regionCode)?.countryName ?: regionCode
                    session.showMessage("Welcome to Well+ ($regionName mode ready).")
                }.onFailure { err ->
                    session.showMessage(err.localizedMessage ?: "Could not complete onboarding.")
                }
            } finally {
                _isSavingOnboarding.value = false
            }
        }
    }

    fun saveHealthProfile(
        displayName: String,
        age: Long,
        biologicalSex: BiologicalSex,
        bloodType: BloodType,
        allergies: List<String>,
        medications: List<String>,
        conditions: List<String>,
        wellnessGoals: List<String>,
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        coverageProvider: String = "",
        coverageMemberId: String = "",
        primaryDoctorName: String = "",
        preferredPharmacy: String = ""
    ) {
        val existing = (profileState.value as? CloudUiState.Success)?.data
        viewModelScope.launch {
            val result = profileRepository.saveProfile(
                userId = session.currentUserId,
                displayName = displayName.ifBlank { session.defaultDisplayName },
                age = age,
                biologicalSex = biologicalSex,
                bloodType = bloodType,
                allergies = allergies,
                medications = medications,
                conditions = conditions,
                wellnessGoals = wellnessGoals,
                emergencyContactName = emergencyContactName,
                emergencyContactPhone = emergencyContactPhone,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                disclaimerAccepted = existing?.disclaimerAccepted ?: true,
                ageConfirmed18Plus = existing?.ageConfirmed18Plus ?: true,
                preferredHealthRegion = existing?.preferredHealthRegion?.ifBlank { session.activeHealthRegion.value.code.code }
                    ?: session.activeHealthRegion.value.code.code,
                preferredEmergencyCountry = existing?.preferredEmergencyCountry.orEmpty(),
                coverageProvider = coverageProvider,
                coverageMemberId = coverageMemberId,
                primaryDoctorName = primaryDoctorName,
                preferredPharmacy = preferredPharmacy
            )
            result.onSuccess {
                session.showMessage("Personal health profile synced.")
            }.onFailure { err ->
                session.showMessage(err.localizedMessage ?: "Could not save health profile.")
            }
        }
    }

    fun setHealthRegion(regionCode: HealthRegionCode) {
        viewModelScope.launch {
            val code = regionCode.code
            val profile = HealthRegionDirectory.fromCode(code) ?: HealthRegionDirectory.US
            session.prefsStore.setPreferredHealthRegion(code)
            session.prefsStore.setUseMetricBodyUnits(profile.usesMetricBody)
            val currentEmergencyOverride = session.deviceSettings.value.emergencyRegionOverride
            if (currentEmergencyOverride.isBlank() || currentEmergencyOverride == "US" || currentEmergencyOverride == "CA") {
                session.prefsStore.setEmergencyRegionOverride(code)
            }
            session.entitlementProvider.clearCache()

            val existing = (profileState.value as? CloudUiState.Success)?.data
            if (existing != null) {
                val updatedEmergencyCountry = if (
                    existing.preferredEmergencyCountry.isBlank() ||
                    existing.preferredEmergencyCountry == "US" ||
                    existing.preferredEmergencyCountry == "CA"
                ) {
                    code
                } else {
                    existing.preferredEmergencyCountry
                }
                profileRepository.saveProfile(
                    userId = session.currentUserId,
                    displayName = existing.displayName,
                    age = existing.age,
                    biologicalSex = BiologicalSex.fromRaw(existing.biologicalSex),
                    bloodType = BloodType.fromCode(existing.bloodType),
                    allergies = existing.allergies,
                    medications = existing.medications,
                    conditions = existing.conditions,
                    wellnessGoals = existing.wellnessGoals,
                    emergencyContactName = existing.emergencyContactName,
                    emergencyContactPhone = existing.emergencyContactPhone,
                    onboardingCompleted = existing.onboardingCompleted,
                    disclaimerAccepted = existing.disclaimerAccepted,
                    ageConfirmed18Plus = existing.ageConfirmed18Plus,
                    preferredHealthRegion = code,
                    preferredEmergencyCountry = updatedEmergencyCountry,
                    coverageProvider = existing.coverageProvider,
                    coverageMemberId = existing.coverageMemberId,
                    primaryDoctorName = existing.primaryDoctorName,
                    preferredPharmacy = existing.preferredPharmacy
                )
            }
            session.showMessage("Health Region switched to ${profile.countryName} (${profile.unitSummaryLine}).")
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setBiometricLockEnabled(enabled)
            session.showMessage(
                if (enabled) "Biometric Sanctuary Lock enabled." else "Biometric Sanctuary Lock turned off."
            )
        }
    }

    fun setScreenPrivacyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setScreenPrivacyEnabled(enabled)
            session.showMessage(
                if (enabled) {
                    "Screen privacy enabled — screenshots & app-switcher previews hidden."
                } else {
                    "Screen privacy turned off — screenshots are now allowed."
                }
            )
        }
    }

    fun setCloudAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setCloudAiEnabled(enabled)
            if (!enabled) {
                session.entitlementProvider.clearCache()
            }
            session.showMessage(
                if (enabled) {
                    "Cloud AI analysis enabled."
                } else {
                    "Cloud AI analysis paused. Tracking, vitals, and Health Connect remain active."
                }
            )
        }
    }

    fun setAutoMaskPiiBeforeScan(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setAutoMaskPiiBeforeScan(enabled)
            session.showMessage(
                if (enabled) {
                    "On-device PII redaction preview enabled before photo scans."
                } else {
                    "Auto-mask PII preview turned off — photos upload directly."
                }
            )
        }
    }

    fun setShowEmergencyIdOnWidget(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setShowEmergencyIdOnWidget(enabled)
            session.showMessage(
                if (enabled) {
                    "Emergency Medical ID widget enabled for your Home Screen."
                } else {
                    "Emergency Medical ID hidden from Home Screen widget."
                }
            )
        }
    }

    fun setEmergencyRegionOverride(countryCode: String) {
        viewModelScope.launch {
            session.prefsStore.setEmergencyRegionOverride(countryCode)
            val existing = (profileState.value as? CloudUiState.Success)?.data
            if (existing != null) {
                profileRepository.saveProfile(
                    userId = session.currentUserId,
                    displayName = existing.displayName,
                    age = existing.age,
                    biologicalSex = BiologicalSex.fromRaw(existing.biologicalSex),
                    bloodType = BloodType.fromCode(existing.bloodType),
                    allergies = existing.allergies,
                    medications = existing.medications,
                    conditions = existing.conditions,
                    wellnessGoals = existing.wellnessGoals,
                    emergencyContactName = existing.emergencyContactName,
                    emergencyContactPhone = existing.emergencyContactPhone,
                    onboardingCompleted = existing.onboardingCompleted,
                    disclaimerAccepted = existing.disclaimerAccepted,
                    ageConfirmed18Plus = existing.ageConfirmed18Plus,
                    preferredHealthRegion = existing.preferredHealthRegion,
                    preferredEmergencyCountry = countryCode,
                    coverageProvider = existing.coverageProvider,
                    coverageMemberId = existing.coverageMemberId,
                    primaryDoctorName = existing.primaryDoctorName,
                    preferredPharmacy = existing.preferredPharmacy
                )
            }
            session.showMessage(
                if (countryCode.isBlank() || countryCode == "AUTO") {
                    "Emergency numbers set to automatic region detection."
                } else {
                    "Emergency & crisis helpline region set to $countryCode."
                }
            )
        }
    }

    fun setCheckInReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        viewModelScope.launch {
            session.prefsStore.setCheckInReminder(enabled, hour, minute)
            ReminderScheduler.syncDailyCheckInReminder(appContext, enabled, hour, minute)
            session.showMessage(
                if (enabled) "Daily wellness check-in reminder scheduled." else "Daily wellness reminder turned off."
            )
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

    fun setFollowUpRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            session.prefsStore.setFollowUpRemindersEnabled(enabled)
            session.showMessage(
                if (enabled) "48-hour symptom follow-up nudges enabled." else "Symptom follow-up nudges turned off."
            )
        }
    }

    /**
     * Fetches the user's complete Firestore archive (including records older than the 30-item
     * live-head UI window) for full GDPR/HIPAA JSON data portability, falling back to cached
     * in-memory streams when offline.
     */
    fun exportAllPersonalDataJson() {
        if (_isExportingJson.value) return
        viewModelScope.launch {
            _isExportingJson.value = true
            try {
                val archive = portabilityRepository.fetchCompleteArchive(session.currentUserId).getOrNull()
                val res = PersonalDataJsonExporter.exportAndShareJson(
                    context = appContext,
                    accountEmail = session.accountEmail,
                    profile = archive?.profile ?: (profileState.value as? CloudUiState.Success)?.data,
                    consultations = archive?.consultations
                        ?: (consultationsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty(),
                    checkIns = archive?.checkIns
                        ?: (checkInsState.value as? CloudUiState.Success)?.data.orEmpty(),
                    careTasks = archive?.careTasks
                        ?: (careTasksState.value as? CloudUiState.Success)?.data.orEmpty(),
                    medications = archive?.medications
                        ?: (medicationsState.value as? CloudUiState.Success)?.data.orEmpty(),
                    medicationLogs = archive?.medicationLogs
                        ?: (medicationLogsState.value as? CloudUiState.Success)?.data.orEmpty(),
                    vitals = archive?.vitals
                        ?: (vitalsHeadState.value as? CloudUiState.Success)?.data?.items.orEmpty(),
                    mentalAssessments = archive?.mentalAssessments
                        ?: (mentalAssessmentsState.value as? CloudUiState.Success)?.data.orEmpty(),
                    bodyMeasurements = archive?.bodyMeasurements
                        ?: (bodyMeasurementsState.value as? CloudUiState.Success)?.data.orEmpty(),
                    cycleLogs = archive?.cycleLogs
                        ?: (cycleLogsState.value as? CloudUiState.Success)?.data.orEmpty(),
                    labBiomarkers = archive?.labBiomarkers.orEmpty()
                )
                res.onSuccess {
                    session.showMessage("Personal health data JSON export ready to share.")
                }.onFailure { err ->
                    session.showMessage(err.localizedMessage ?: "Could not export personal data.")
                }
            } finally {
                _isExportingJson.value = false
            }
        }
    }

    fun deleteAllDataAndPrepareSignOut(onDataWiped: () -> Unit) {
        if (_isDeletingAccount.value) return
        viewModelScope.launch {
            _isDeletingAccount.value = true
            try {
                val res = portabilityRepository.deleteAllUserData(session.currentUserId)
                res.onSuccess {
                    ReminderScheduler.cancelAllReminders(appContext)
                    session.prefsStore.clearAll()
                    onDataWiped()
                }.onFailure { err ->
                    session.showMessage(
                        err.localizedMessage
                            ?: "Could not complete full cloud data deletion. Please check your connection."
                    )
                }
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }
}
