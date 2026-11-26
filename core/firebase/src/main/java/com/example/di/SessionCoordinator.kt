package com.example.di

import android.content.Context
import com.example.core.AiEntitlementProvider
import com.example.core.AiQuotaCheckResult
import com.example.core.CriticalVitalAlert
import com.example.core.DateKeyProvider
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.NetworkConnectivityMonitor
import com.example.core.SanctuaryDeviceSettings
import com.example.core.SanctuaryPreferencesStore
import com.example.core.SymptomRedFlagResult
import com.example.data.model.AiFeatureArea
import com.example.data.model.AiSafetyReportReason
import com.example.data.model.HealthProfile
import com.example.data.model.WellPlusTier
import com.example.data.repository.MentalAndBodyRepository
import com.example.ui.viewmodel.CloudUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * User-scoped session coordinator shared across all feature ViewModels.
 * Centralizes transient snackbars, connectivity, AI quota/entitlement guards, active region,
 * midnight-safe date key updates, and clinical safety alerts/reports.
 */
class SessionCoordinator(
    private val appContext: Context,
    private val sessionScope: CoroutineScope,
    val currentUserId: String,
    val defaultDisplayName: String,
    val accountEmail: String,
    val prefsStore: SanctuaryPreferencesStore,
    val entitlementProvider: AiEntitlementProvider,
    val dateKeyProvider: DateKeyProvider,
    private val mentalAndBodyRepository: MentalAndBodyRepository,
    profileState: StateFlow<CloudUiState<HealthProfile?>>,
    subscriptionTierFlow: StateFlow<WellPlusTier> = MutableStateFlow(WellPlusTier.FREE)
) {
    private val networkMonitor = NetworkConnectivityMonitor(appContext)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val aiDailyLimit: Int get() = entitlementProvider.dailyLimit
    val urgentSafetyNotice: String? get() = entitlementProvider.urgentSafetyNotice

    val todayDateKeyFlow: StateFlow<String> = dateKeyProvider.todayDateKeyFlow
    val todayDateKey: String get() = dateKeyProvider.todayKey()

    val deviceSettings: StateFlow<SanctuaryDeviceSettings> =
        prefsStore.settingsFlow.stateIn(
            scope = sessionScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SanctuaryDeviceSettings()
        )

    val aiQuotaRemaining: StateFlow<Int> =
        combine(deviceSettings, subscriptionTierFlow) { settings, _ ->
            entitlementProvider.computeRemaining(settings)
        }.stateIn(
            scope = sessionScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = aiDailyLimit
        )

    val activeHealthRegion: StateFlow<HealthRegionProfile> =
        combine(deviceSettings, profileState) { settings, profState ->
            val cloudRegion = (profState as? CloudUiState.Success)?.data?.preferredHealthRegion.orEmpty()
            val override = settings.preferredHealthRegion.ifBlank { cloudRegion }
            HealthRegionDirectory.resolve(appContext, override)
        }.stateIn(
            scope = sessionScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HealthRegionDirectory.detectSuggestedRegion(appContext)
        )

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activeCriticalVitalAlert = MutableStateFlow<CriticalVitalAlert?>(null)
    val activeCriticalVitalAlert: StateFlow<CriticalVitalAlert?> = _activeCriticalVitalAlert.asStateFlow()

    private val _activeSymptomRedFlag = MutableStateFlow<SymptomRedFlagResult?>(null)
    val activeSymptomRedFlag: StateFlow<SymptomRedFlagResult?> = _activeSymptomRedFlag.asStateFlow()

    private val _isSubmittingSafetyReport = MutableStateFlow(false)
    val isSubmittingSafetyReport: StateFlow<Boolean> = _isSubmittingSafetyReport.asStateFlow()

    init {
        networkMonitor.start()
    }

    fun refreshTodayDateKey() {
        dateKeyProvider.refresh()
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun emitCriticalVitalAlert(alert: CriticalVitalAlert) {
        _activeCriticalVitalAlert.value = alert
    }

    fun dismissCriticalVitalAlert() {
        _activeCriticalVitalAlert.value = null
    }

    fun emitSymptomRedFlag(redFlag: SymptomRedFlagResult) {
        _activeSymptomRedFlag.value = redFlag
    }

    fun dismissSymptomRedFlag() {
        _activeSymptomRedFlag.value = null
    }

    /**
     * Checks Cloud AI consent, global safety kill-switch, network availability, and daily fair-use quota.
     * Returns null when allowed, or an actionable user-facing message when blocked.
     */
    suspend fun guardCloudAiAndConsumeQuota(): String? {
        if (!entitlementProvider.cloudAiGloballyEnabled) {
            return "Cloud AI features are temporarily paused for safety maintenance."
        }
        if (!deviceSettings.value.cloudAiEnabled) {
            return "Cloud AI analysis is paused in Profile → Privacy & Security. Enable it anytime to resume AI insights."
        }
        if (!isOnline.value) {
            return "You appear to be offline. Local tracking & Health Connect stay available; Cloud AI resumes once connected."
        }
        return when (val quota = entitlementProvider.tryConsumeDailyQuota()) {
            is AiQuotaCheckResult.Allowed -> null
            is AiQuotaCheckResult.DailyLimitReached ->
                "Daily fair-use limit reached (${quota.dailyLimit} AI requests/day). Quota resets at midnight—cached insights and emergency safety checks remain available."
        }
    }

    fun submitAiSafetyReport(
        featureArea: AiFeatureArea,
        reason: AiSafetyReportReason,
        contentSnippet: String,
        userNote: String,
        onSubmitted: () -> Unit = {}
    ) {
        if (_isSubmittingSafetyReport.value) return
        sessionScope.launch {
            _isSubmittingSafetyReport.value = true
            try {
                val res = mentalAndBodyRepository.submitAiSafetyReport(
                    userId = currentUserId,
                    featureArea = featureArea,
                    reason = reason,
                    contentSnippet = contentSnippet,
                    userNote = userNote
                )
                res.onSuccess {
                    _snackbarMessage.value = "Thank you — your AI safety report was recorded for review."
                    onSubmitted()
                }.onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not submit safety report."
                }
            } finally {
                _isSubmittingSafetyReport.value = false
            }
        }
    }

    fun close() {
        networkMonitor.stop()
    }
}
