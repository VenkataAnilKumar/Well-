package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.PermissionsRationaleActivity
import com.example.core.designsystem.R
import com.example.core.HealthConnectManager
import com.example.data.model.AiFeatureArea
import com.example.data.model.CareCategory
import com.example.data.model.HealthConnectAiBrief
import com.example.data.model.HealthConnectAvailability
import com.example.data.model.HealthConnectBloodPressureReading
import com.example.data.model.HealthConnectDailySteps
import com.example.data.model.HealthConnectHeartRateSummary
import com.example.data.model.HealthConnectSleepSummary
import com.example.data.model.VitalRangeStatus
import com.example.ui.components.AiSafetyFooter
import com.example.ui.components.MedicalDisclaimerNote
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.components.WeeklyHeartRateTrendChart
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.HealthConnectAiUiState
import com.example.ui.viewmodel.HealthConnectUiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Apple Health-style Summary Card for Android Health Connect Sync.
 */
@Composable
fun TodayHealthConnectCard(
    hcState: HealthConnectUiState,
    onOpenHealthConnect: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val snapshot = hcState.snapshot
    val numberFmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    val statusText = when {
        hcState.availability == HealthConnectAvailability.NOT_SUPPORTED -> "Unavailable on this Android version"
        hcState.availability == HealthConnectAvailability.UPDATE_REQUIRED -> "Install Health Connect to sync wearables"
        hcState.isSyncing -> "Syncing wearables & health apps…"
        hcState.hasAnyGrantedPermission && snapshot != null && snapshot.hasAnyMetricData ->
            "Connected • Synced ${formatSyncTime(snapshot.syncedAtMillis)}"
        hcState.hasAnyGrantedPermission -> "Connected • Tap to sync latest records"
        else -> stringResource(R.string.hc_card_subtitle_connect)
    }

    val badgeLabel = when {
        hcState.hasAnyGrantedPermission -> "Connected"
        hcState.availability == HealthConnectAvailability.UPDATE_REQUIRED -> "Setup"
        hcState.availability == HealthConnectAvailability.NOT_SUPPORTED -> "Info"
        else -> "Connect"
    }

    SanctuaryCard(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenHealthConnect)
            .testTag("home_health_connect_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = sanaColors.activityOrange.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Watch,
                        contentDescription = null,
                        tint = sanaColors.activityOrange,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hc_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = if (hcState.hasAnyGrantedPermission) {
                                sanaColors.healthyGreen.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hcState.hasAnyGrantedPermission) {
                                    sanaColors.healthyGreen
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open Health Connect",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        if (snapshot != null && snapshot.hasAnyMetricData) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                snapshot.stepsToday?.let { steps ->
                    HealthConnectMiniChip(
                        label = "Steps",
                        value = numberFmt.format(steps),
                        accent = sanaColors.activityOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                snapshot.heartRate?.let { hr ->
                    HealthConnectMiniChip(
                        label = "Heart Rate",
                        value = "${hr.latestBpm} bpm",
                        accent = sanaColors.heartPink,
                        modifier = Modifier.weight(1f)
                    )
                }
                snapshot.sleep?.let { sl ->
                    HealthConnectMiniChip(
                        label = "Sleep",
                        value = String.format(Locale.US, "%.1fh", sl.lastNightHours),
                        accent = sanaColors.sleepIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthConnectMiniChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, LocalSanaColors.current.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthConnectScreen(
    hcState: HealthConnectUiState,
    aiState: HealthConnectAiUiState,
    backgroundSyncEnabled: Boolean = false,
    onToggleBackgroundSync: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onRefreshStatus: () -> Unit,
    onPermissionsResult: (Set<String>) -> Unit,
    onSyncNow: () -> Unit,
    onImportHeartRateToVitals: () -> Unit,
    onImportBloodPressureToVitals: () -> Unit,
    onWriteSteps: (Long, Long) -> Unit = { _, _ -> },
    onWriteHeartRate: (Long) -> Unit = {},
    onWriteSampleData: () -> Unit = {},
    onGenerateAiBrief: () -> Unit,
    onAddTipToChecklist: (String, CareCategory) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val snapshot = hcState.snapshot

    var showWriteStepsDialog by remember { mutableStateOf(false) }
    var showWriteHeartRateDialog by remember { mutableStateOf(false) }

    val permissionContract = remember { HealthConnectManager.createPermissionRequestContract() }
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = permissionContract
    ) { granted ->
        onPermissionsResult(granted)
    }

    LaunchedEffect(Unit) {
        onRefreshStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hc_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.hc_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = sanaColors.activityOrange
                        )
                        Text(
                            text = stringResource(R.string.hc_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                if (hcState.availability == HealthConnectAvailability.AVAILABLE && hcState.hasAnyGrantedPermission) {
                    IconButton(
                        onClick = onSyncNow,
                        enabled = !hcState.isSyncing,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hc_header_sync_button")
                    ) {
                        if (hcState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = stringResource(R.string.hc_sync_now),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Connection Status & Authorization Card
        item {
            val hcManager = remember(context) { HealthConnectManager.getInstance(context) }
            HealthConnectStatusCard(
                hcState = hcState,
                onRequestPermissions = {
                    val toRequest = hcState.missingPermissions.ifEmpty { HealthConnectManager.allPermissions }
                    try {
                        hcManager.requestPermissions(permissionsLauncher, toRequest)
                    } catch (_: Exception) {
                        onShowMessage("Could not open Health Connect permissions screen on this device.")
                    }
                },
                onSyncNow = onSyncNow,
                onInstallHealthConnect = {
                    try {
                        context.startActivity(HealthConnectManager.buildInstallOrUpdateIntent(context))
                    } catch (_: ActivityNotFoundException) {
                        try {
                            context.startActivity(HealthConnectManager.buildPlayStoreWebFallbackIntent())
                        } catch (_: ActivityNotFoundException) {
                            onShowMessage("Google Play Store is unavailable on this device.")
                        }
                    }
                },
                onOpenSystemSettings = {
                    try {
                        context.startActivity(HealthConnectManager.buildHealthConnectSettingsIntent())
                    } catch (_: ActivityNotFoundException) {
                        onShowMessage("Health Connect settings are unavailable on this device.")
                    }
                },
                onViewPrivacyPolicy = {
                    context.startActivity(Intent(context, PermissionsRationaleActivity::class.java))
                }
            )
        }

        // Periodic 6-Hour Background Auto-Sync Card
        if (hcState.availability == HealthConnectAvailability.AVAILABLE) {
            item {
                SanctuaryCard(modifier = Modifier.testTag("hc_background_sync_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_bg_sync_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.hc_bg_sync_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = backgroundSyncEnabled,
                            onCheckedChange = onToggleBackgroundSync,
                            modifier = Modifier.testTag("hc_background_sync_switch")
                        )
                    }
                }
            }
        }

        // Quick Write to Health Connect (Steps & Heart Rate)
        if (hcState.availability == HealthConnectAvailability.AVAILABLE) {
            item {
                val canWriteSteps = HealthConnectManager.PERMISSION_WRITE_STEPS in hcState.grantedPermissions
                val canWriteHr = HealthConnectManager.PERMISSION_WRITE_HEART_RATE in hcState.grantedPermissions
                SanctuaryCard(modifier = Modifier.testTag("hc_quick_write_card")) {
                    Text(
                        text = "Write to Health Connect",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (canWriteSteps || canWriteHr) {
                            "Record manual steps or heart rate readings straight into the Android Health Connect datastore."
                        } else {
                            "Grant write permissions above to save manual steps or heart rate entries back to Health Connect."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (canWriteSteps) {
                                    showWriteStepsDialog = true
                                } else {
                                    try {
                                        permissionsLauncher.launch(
                                            setOf(
                                                HealthConnectManager.PERMISSION_READ_STEPS,
                                                HealthConnectManager.PERMISSION_WRITE_STEPS
                                            )
                                        )
                                    } catch (_: Exception) {
                                        onShowMessage("Could not open Health Connect permissions.")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hc_write_steps_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (canWriteSteps) "Log Steps" else "Allow Steps", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                if (canWriteHr) {
                                    showWriteHeartRateDialog = true
                                } else {
                                    try {
                                        HealthConnectManager.getInstance(context)
                                            .requestHeartRateAndStepsPermissions(permissionsLauncher)
                                    } catch (_: Exception) {
                                        onShowMessage("Could not open Health Connect permissions.")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hc_write_hr_button")
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (canWriteHr) "Log Heart Rate" else "Allow HR", maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            if (canWriteSteps || canWriteHr) {
                                onWriteSampleData()
                            } else {
                                try {
                                    HealthConnectManager.getInstance(context)
                                        .requestHeartRateAndStepsPermissions(permissionsLauncher)
                                } catch (_: Exception) {
                                    onShowMessage("Could not open Health Connect permissions.")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hc_write_sample_data_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (canWriteSteps || canWriteHr) {
                                "Write Sample Steps & Heart Rate"
                            } else {
                                "Request Heart Rate & Steps Permissions"
                            }
                        )
                    }
                }
            }
        }

        // Error Banner
        hcState.errorMessage?.let { err ->
            item {
                SanctuaryErrorBanner(message = err)
            }
        }

        // Synced Metrics Section (Steps, Heart Rate, Sleep, Blood Pressure)
        if (hcState.availability == HealthConnectAvailability.AVAILABLE && hcState.hasAnyReadPermission) {
            if (snapshot?.partialWarnings?.isNotEmpty() == true) {
                item {
                    SanctuaryErrorBanner(message = snapshot.partialWarnings.joinToString(" "))
                }
            }

            // Steps Card + 7-Day Activity Bar Chart
            if (HealthConnectManager.PERMISSION_READ_STEPS in hcState.grantedPermissions) {
                item {
                    StepsHealthConnectCard(
                        stepsToday = snapshot?.stepsToday,
                        avg7DaySteps = snapshot?.average7DaySteps,
                        dailySteps = snapshot?.dailySteps7Days.orEmpty(),
                        isSyncing = hcState.isSyncing
                    )
                }
            }

            // Heart Rate Card + One-Tap Import to Well+ Vitals
            if (HealthConnectManager.PERMISSION_READ_HEART_RATE in hcState.grantedPermissions) {
                item {
                    HeartRateHealthConnectCard(
                        heartRate = snapshot?.heartRate,
                        isSyncing = hcState.isSyncing,
                        onImportToVitals = onImportHeartRateToVitals
                    )
                }
            }

            // Sleep Sessions Card + Stage Breakdown
            if (HealthConnectManager.PERMISSION_READ_SLEEP in hcState.grantedPermissions) {
                item {
                    SleepHealthConnectCard(
                        sleep = snapshot?.sleep,
                        isSyncing = hcState.isSyncing
                    )
                }
            }

            // Blood Pressure Card + Clinical Range + One-Tap Import to Well+ Vitals
            if (HealthConnectManager.PERMISSION_READ_BLOOD_PRESSURE in hcState.grantedPermissions) {
                item {
                    BloodPressureHealthConnectCard(
                        latest = snapshot?.latestBloodPressure,
                        recentCount = snapshot?.recentBloodPressures?.size ?: 0,
                        isSyncing = hcState.isSyncing,
                        onImportToVitals = onImportBloodPressureToVitals
                    )
                }
            }

            // Gemini AI Wearable & Recovery Brief
            item {
                HealthConnectAiBriefCard(
                    hasData = snapshot?.hasAnyMetricData == true,
                    aiState = aiState,
                    onGenerate = onGenerateAiBrief,
                    onAddTipToChecklist = onAddTipToChecklist,
                    onReportAiResponse = onReportAiResponse
                )
            }
        }

        item {
            MedicalDisclaimerNote()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showWriteStepsDialog) {
        WriteStepsHealthConnectDialog(
            onDismiss = { showWriteStepsDialog = false },
            onConfirm = { steps, durationMin ->
                onWriteSteps(steps, durationMin)
                showWriteStepsDialog = false
            }
        )
    }

    if (showWriteHeartRateDialog) {
        WriteHeartRateHealthConnectDialog(
            onDismiss = { showWriteHeartRateDialog = false },
            onConfirm = { bpm ->
                onWriteHeartRate(bpm)
                showWriteHeartRateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthConnectStatusCard(
    hcState: HealthConnectUiState,
    onRequestPermissions: () -> Unit,
    onSyncNow: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onViewPrivacyPolicy: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val granted = hcState.grantedPermissions

    SanctuaryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = when {
                        hcState.hasAnyGrantedPermission -> sanaColors.healthyGreen.copy(alpha = 0.14f)
                        hcState.availability == HealthConnectAvailability.AVAILABLE ->
                            MaterialTheme.colorScheme.primaryContainer
                        else -> sanaColors.activityOrange.copy(alpha = 0.14f)
                    },
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (hcState.hasAnyGrantedPermission) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.Watch
                        },
                        contentDescription = null,
                        tint = when {
                            hcState.hasAnyGrantedPermission -> sanaColors.healthyGreen
                            hcState.availability == HealthConnectAvailability.AVAILABLE ->
                                MaterialTheme.colorScheme.primary
                            else -> sanaColors.activityOrange
                        },
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when {
                            hcState.availability == HealthConnectAvailability.NOT_SUPPORTED ->
                                "Health Connect Unavailable"
                            hcState.availability == HealthConnectAvailability.UPDATE_REQUIRED ->
                                "Health Connect Update Needed"
                            hcState.hasAllRequiredPermissions ->
                                "All Read & Write Permissions Granted"
                            hcState.hasAnyGrantedPermission ->
                                "${granted.intersect(HealthConnectManager.allPermissions).size} of ${HealthConnectManager.allPermissions.size} Permissions Granted"
                            else -> "Connect Wearables & Fitness Apps"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val syncedSnapshot = hcState.snapshot
                    Text(
                        text = when {
                            hcState.availability == HealthConnectAvailability.NOT_SUPPORTED ->
                                "Android Health Connect requires Android 8.0+ with the Health Connect service."
                            hcState.availability == HealthConnectAvailability.UPDATE_REQUIRED ->
                                "Install or update the official Health Connect app from Google Play to sync data."
                            syncedSnapshot != null ->
                                "Last synced ${formatSyncTime(syncedSnapshot.syncedAtMillis)} • ${hcState.frameworkSourceLabel.ifBlank { "Health Connect" }}"
                            hcState.isClientInitialized ->
                                "Client initialized via ${hcState.frameworkSourceLabel.ifBlank { "Android Health Connect" }} • Supports Read & Write sync for Steps, Heart Rate, Sleep & Blood Pressure."
                            else ->
                                "Read and write Steps, Heart Rate, Sleep, and Blood Pressure with Fitbit, Pixel Watch, Samsung Health & more."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (hcState.availability == HealthConnectAvailability.AVAILABLE) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PermissionStatusPill(
                    label = "Steps",
                    readGranted = HealthConnectManager.PERMISSION_READ_STEPS in granted,
                    writeGranted = HealthConnectManager.PERMISSION_WRITE_STEPS in granted
                )
                PermissionStatusPill(
                    label = "Heart Rate",
                    readGranted = HealthConnectManager.PERMISSION_READ_HEART_RATE in granted,
                    writeGranted = HealthConnectManager.PERMISSION_WRITE_HEART_RATE in granted
                )
                PermissionStatusPill(
                    label = "Sleep",
                    readGranted = HealthConnectManager.PERMISSION_READ_SLEEP in granted,
                    writeGranted = HealthConnectManager.PERMISSION_WRITE_SLEEP in granted
                )
                PermissionStatusPill(
                    label = "Blood Pressure",
                    readGranted = HealthConnectManager.PERMISSION_READ_BLOOD_PRESSURE in granted,
                    writeGranted = HealthConnectManager.PERMISSION_WRITE_BLOOD_PRESSURE in granted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (hcState.availability) {
            HealthConnectAvailability.NOT_SUPPORTED -> {
                OutlinedButton(
                    onClick = onViewPrivacyPolicy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("hc_privacy_policy_button")
                ) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Health Connect Privacy Policy")
                }
            }

            HealthConnectAvailability.UPDATE_REQUIRED -> {
                Button(
                    onClick = onInstallHealthConnect,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("hc_install_play_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.hc_install_cta))
                }
            }

            HealthConnectAvailability.AVAILABLE -> {
                if (!hcState.hasAllRequiredPermissions) {
                    Button(
                        onClick = onRequestPermissions,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hc_request_permissions_button")
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (hcState.hasAnyGrantedPermission) {
                                "Grant Remaining Data Types"
                            } else {
                                stringResource(R.string.hc_connect_cta)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hcState.hasAnyGrantedPermission) {
                        FilledTonalButton(
                            onClick = onSyncNow,
                            enabled = !hcState.isSyncing,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hc_sync_now_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (hcState.isSyncing) "Syncing…" else stringResource(R.string.hc_sync_now))
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenSystemSettings,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hc_open_settings_button")
                    ) {
                        Text(stringResource(R.string.hc_manage_permissions), maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onViewPrivacyPolicy)
                        .padding(horizontal = 6.dp)
                        .testTag("hc_privacy_link"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PrivacyTip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "How Well+ protects Health Connect data",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusPill(
    label: String,
    readGranted: Boolean,
    writeGranted: Boolean
) {
    val sanaColors = LocalSanaColors.current
    val anyGranted = readGranted || writeGranted
    val accessSuffix = when {
        readGranted && writeGranted -> "R/W"
        readGranted -> "Read"
        writeGranted -> "Write"
        else -> "Off"
    }
    val tint = when {
        readGranted && writeGranted -> sanaColors.healthyGreen
        anyGranted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = if (anyGranted) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            0.5.dp,
            if (anyGranted) tint.copy(alpha = 0.35f) else sanaColors.cardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (anyGranted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = "$label · $accessSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}

@Composable
private fun StepsHealthConnectCard(
    stepsToday: Long?,
    avg7DaySteps: Long?,
    dailySteps: List<HealthConnectDailySteps>,
    isSyncing: Boolean
) {
    val sanaColors = LocalSanaColors.current
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    SanctuaryCard(modifier = Modifier.testTag("hc_steps_card")) {
        MetricHeaderRow(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            accent = sanaColors.activityOrange,
            title = "Steps",
            trailing = avg7DaySteps?.let { "7d avg ${fmt.format(it)}" }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (stepsToday == null && dailySteps.isEmpty()) {
            Text(
                text = if (isSyncing) "Reading steps from Health Connect…" else "No step records synced in the last 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = fmt.format(stepsToday ?: 0L),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "steps today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (dailySteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Steps7DayBarChart(dailySteps = dailySteps, barColor = sanaColors.activityOrange)
            }
        }
    }
}

@Composable
private fun Steps7DayBarChart(
    dailySteps: List<HealthConnectDailySteps>,
    barColor: Color
) {
    val maxSteps = (dailySteps.maxOfOrNull { it.steps } ?: 0L).coerceAtLeast(1000L)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerLow

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val count = dailySteps.size.coerceAtLeast(1)
            val gap = 10.dp.toPx()
            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(6.dp.toPx())
            val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

            dailySteps.forEachIndexed { idx, item ->
                val x = idx * (barWidth + gap)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = corner
                )
                val fraction = (item.steps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
                val fillH = (size.height * fraction).coerceAtLeast(if (item.steps > 0L) 6.dp.toPx() else 0f)
                if (fillH > 0f) {
                    drawRoundRect(
                        color = if (idx == dailySteps.lastIndex) barColor else barColor.copy(alpha = 0.65f),
                        topLeft = Offset(x, size.height - fillH),
                        size = Size(barWidth, fillH),
                        cornerRadius = corner
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailySteps.forEachIndexed { idx, item ->
                Text(
                    text = item.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (idx == dailySteps.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == dailySteps.lastIndex) barColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeartRateHealthConnectCard(
    heartRate: HealthConnectHeartRateSummary?,
    isSyncing: Boolean,
    onImportToVitals: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var imported by remember(heartRate?.latestTimestampMillis) { mutableStateOf(false) }

    SanctuaryCard(modifier = Modifier.testTag("hc_heart_rate_card")) {
        MetricHeaderRow(
            icon = Icons.Filled.Favorite,
            accent = sanaColors.heartPink,
            title = "Heart Rate",
            trailing = heartRate?.let { "${it.sampleCount} samples (7d)" }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (heartRate == null) {
            Text(
                text = if (isSyncing) "Reading heart rate samples…" else "No heart rate records found in the last 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${heartRate.latestBpm}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "bpm latest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = formatSyncTime(heartRate.latestTimestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (heartRate.avgBpm != null || (heartRate.minBpm != null && heartRate.maxBpm != null)) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    heartRate.avgBpm?.let {
                        HealthConnectMiniChip("7d Avg", "$it bpm", sanaColors.heartPink, Modifier.weight(1f))
                    }
                    heartRate.minBpm?.let {
                        HealthConnectMiniChip("Min", "$it bpm", sanaColors.healthyGreen, Modifier.weight(1f))
                    }
                    heartRate.maxBpm?.let {
                        HealthConnectMiniChip("Max", "$it bpm", sanaColors.activityOrange, Modifier.weight(1f))
                    }
                }
            }

            if (heartRate.dailyTrend7Days.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyHeartRateTrendChart(
                    dailyTrend = heartRate.dailyTrend7Days
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    imported = true
                    onImportToVitals()
                },
                enabled = !imported,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("hc_import_hr_button")
            ) {
                Icon(
                    imageVector = if (imported) Icons.Filled.Check else Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (imported) "Saved to Well+ Vitals" else "Import Latest HR to Well+ Vitals")
            }
        }
    }
}

@Composable
private fun SleepHealthConnectCard(
    sleep: HealthConnectSleepSummary?,
    isSyncing: Boolean
) {
    val sanaColors = LocalSanaColors.current

    SanctuaryCard(modifier = Modifier.testTag("hc_sleep_card")) {
        MetricHeaderRow(
            icon = Icons.Filled.Bedtime,
            accent = sanaColors.sleepIndigo,
            title = "Sleep",
            trailing = sleep?.let {
                String.format(Locale.US, "7d avg %.1fh", it.average7DayHours)
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (sleep == null) {
            Text(
                text = if (isSyncing) "Reading sleep sessions…" else "No sleep sessions found in the last 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", sleep.lastNightHours),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "hrs last session • ${sleep.sessionCount} night(s) synced",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (sleep.hasStageBreakdown) {
                Spacer(modifier = Modifier.height(10.dp))
                SleepStagesBreakdownBar(sleep = sleep)
            }
        }
    }
}

@Composable
private fun SleepStagesBreakdownBar(sleep: HealthConnectSleepSummary) {
    val sanaColors = LocalSanaColors.current
    val total = (sleep.deepMinutes + sleep.remMinutes + sleep.lightMinutes + sleep.awakeMinutes)
        .coerceAtLeast(1L).toFloat()
    val deepColor = sanaColors.sleepIndigo
    val remColor = MaterialTheme.colorScheme.primary
    val lightColor = sanaColors.medsTeal
    val awakeColor = sanaColors.activityOrange

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            var x = 0f
            listOf(
                sleep.deepMinutes to deepColor,
                sleep.remMinutes to remColor,
                sleep.lightMinutes to lightColor,
                sleep.awakeMinutes to awakeColor
            ).forEach { (mins, color) ->
                if (mins > 0L) {
                    val w = size.width * (mins.toFloat() / total)
                    drawRect(color = color, topLeft = Offset(x, 0f), size = Size(w, size.height))
                    x += w
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Deep ${sleep.deepMinutes}m", style = MaterialTheme.typography.labelSmall, color = deepColor)
            Text("REM ${sleep.remMinutes}m", style = MaterialTheme.typography.labelSmall, color = remColor)
            Text("Light ${sleep.lightMinutes}m", style = MaterialTheme.typography.labelSmall, color = lightColor)
            Text("Awake ${sleep.awakeMinutes}m", style = MaterialTheme.typography.labelSmall, color = awakeColor)
        }
    }
}

@Composable
private fun BloodPressureHealthConnectCard(
    latest: HealthConnectBloodPressureReading?,
    recentCount: Int,
    isSyncing: Boolean,
    onImportToVitals: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var imported by remember(latest?.timestampMillis) { mutableStateOf(false) }

    SanctuaryCard(modifier = Modifier.testTag("hc_bp_card")) {
        MetricHeaderRow(
            icon = Icons.Filled.MonitorHeart,
            accent = MaterialTheme.colorScheme.primary,
            title = "Blood Pressure",
            trailing = if (recentCount > 0) "$recentCount reading(s) (30d)" else null
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (latest == null) {
            Text(
                text = if (isSyncing) "Reading blood pressure records…" else "No blood pressure records found in the last 30 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val rangeColor = when (latest.clinicalRange) {
                VitalRangeStatus.NORMAL -> sanaColors.healthyGreen
                VitalRangeStatus.ELEVATED, VitalRangeStatus.LOW -> sanaColors.activityOrange
                VitalRangeStatus.HIGH -> sanaColors.heartPink
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = latest.formattedValue,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "mmHg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Surface(
                    color = rangeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = latest.clinicalRange.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = rangeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Recorded ${formatSyncTime(latest.timestampMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    imported = true
                    onImportToVitals()
                },
                enabled = !imported,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("hc_import_bp_button")
            ) {
                Icon(
                    imageVector = if (imported) Icons.Filled.Check else Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (imported) "Saved to Well+ Vitals" else "Import Latest BP to Well+ Vitals")
            }
        }
    }
}

@Composable
private fun MetricHeaderRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    trailing: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthConnectAiBriefCard(
    hasData: Boolean,
    aiState: HealthConnectAiUiState,
    onGenerate: () -> Unit,
    onAddTipToChecklist: (String, CareCategory) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> }
) {
    val sanaColors = LocalSanaColors.current

    SanctuaryCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.hc_ai_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (aiState is HealthConnectAiUiState.Success) {
                IconButton(
                    onClick = onGenerate,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("hc_ai_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh wearable brief",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (aiState) {
            HealthConnectAiUiState.Idle -> {
                Text(
                    text = stringResource(R.string.hc_ai_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGenerate,
                    enabled = hasData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("hc_ai_generate_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.hc_ai_cta))
                }
            }

            HealthConnectAiUiState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Text(
                        text = stringResource(R.string.hc_ai_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is HealthConnectAiUiState.Error -> {
                SanctuaryErrorBanner(message = aiState.message)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Try Again")
                }
            }

            is HealthConnectAiUiState.Success -> {
                HealthConnectAiBriefDetails(
                    brief = aiState.brief,
                    onAddTipToChecklist = onAddTipToChecklist,
                    onReportAiResponse = onReportAiResponse
                )
            }
        }
    }
}

@Composable
private fun HealthConnectAiBriefDetails(
    brief: HealthConnectAiBrief,
    onAddTipToChecklist: (String, CareCategory) -> Unit,
    onReportAiResponse: (AiFeatureArea, String) -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var addedSet by remember(brief.generatedAtMillis) { mutableStateOf(emptySet<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = brief.headline,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (brief.activityAndRecoverySummary.isNotBlank()) {
            Text(
                text = brief.activityAndRecoverySummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (brief.cardioAndSleepNote.isNotBlank()) {
            Text(
                text = brief.cardioAndSleepNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (brief.clinicianNote.isNotBlank()) {
            Surface(
                color = sanaColors.activityOrange.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = brief.clinicianNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        if (brief.actionableTips.isNotEmpty()) {
            HorizontalDivider(color = sanaColors.subtleDivider)
            Text(
                text = "Suggested Habits",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            brief.actionableTips.forEach { tip ->
                val added = tip in addedSet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !added) {
                                addedSet = addedSet + tip
                                onAddTipToChecklist(tip, CareCategory.AI_SUGGESTION)
                            },
                        color = if (added) sanaColors.selfCareBg else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (added) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (added) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (added) "Added" else "Checklist",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (added) sanaColors.selfCareFg else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        AiSafetyFooter(
            featureArea = AiFeatureArea.HEALTH_CONNECT_BRIEF,
            contentSnippet = "${brief.headline}: ${brief.activityAndRecoverySummary}",
            generatedAtMillis = brief.generatedAtMillis,
            onReportClick = onReportAiResponse
        )
    }
}

@Composable
private fun WriteStepsHealthConnectDialog(
    onDismiss: () -> Unit,
    onConfirm: (steps: Long, durationMinutes: Long) -> Unit
) {
    var stepsText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("20") }
    val parsedSteps = stepsText.trim().toLongOrNull()
    val parsedDuration = durationText.trim().toLongOrNull()?.coerceIn(1L, 720L) ?: 15L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Steps to Health Connect", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { if (it.length <= 6) stepsText = it.filter(Char::isDigit) },
                    label = { Text("Step count") },
                    placeholder = { Text("e.g. 2500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hc_dialog_steps_input")
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { if (it.length <= 3) durationText = it.filter(Char::isDigit) },
                    label = { Text("Activity duration (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hc_dialog_steps_duration_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsedSteps?.let { onConfirm(it, parsedDuration) } },
                enabled = parsedSteps != null && parsedSteps in 1L..200_000L,
                modifier = Modifier.testTag("hc_dialog_steps_save_button")
            ) {
                Text("Save to Health Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun WriteHeartRateHealthConnectDialog(
    onDismiss: () -> Unit,
    onConfirm: (bpm: Long) -> Unit
) {
    var bpmText by remember { mutableStateOf("") }
    val parsedBpm = bpmText.trim().toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write Heart Rate to Health Connect", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = bpmText,
                onValueChange = { if (it.length <= 3) bpmText = it.filter(Char::isDigit) },
                label = { Text("Heart rate (bpm)") },
                placeholder = { Text("e.g. 72") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hc_dialog_hr_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { parsedBpm?.let(onConfirm) },
                enabled = parsedBpm != null && parsedBpm in 25L..250L,
                modifier = Modifier.testTag("hc_dialog_hr_save_button")
            ) {
                Text("Save to Health Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatSyncTime(millis: Long): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(millis))
}
