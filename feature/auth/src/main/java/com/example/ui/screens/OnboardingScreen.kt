package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.core.designsystem.R
import com.example.core.BiometricLockHelper
import com.example.core.HealthRegionCode
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.SanaNotificationChannels
import com.example.ui.components.EditableTagSection
import com.example.ui.components.GlassOnboardingStepIllustration
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanaTheme

@Composable
fun OnboardingScreen(
    defaultDisplayName: String,
    isSaving: Boolean,
    onShowMessage: (String) -> Unit,
    onComplete: (
        displayName: String,
        age: Long,
        allergies: List<String>,
        emergencyContactName: String,
        emergencyContactPhone: String,
        selectedRegion: HealthRegionCode,
        coverageProvider: String,
        coverageMemberId: String,
        primaryDoctorName: String,
        preferredPharmacy: String,
        enableCheckInReminder: Boolean,
        enableMedReminders: Boolean,
        enableBiometricLock: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val canBiometric = remember(context) { BiometricLockHelper.canUseBiometricOrDeviceLock(context) }

    var step by remember { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0 && !isSaving) {
        step -= 1
    }
    var selectedRegion by remember(context) {
        mutableStateOf(HealthRegionDirectory.detectSuggestedRegion(context).code)
    }
    val activeRegionProfile: HealthRegionProfile = remember(selectedRegion) {
        HealthRegionDirectory.fromCode(selectedRegion.code) ?: HealthRegionDirectory.US
    }

    var acceptedDisclaimer by remember { mutableStateOf(false) }
    var confirmed18Plus by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf(defaultDisplayName) }
    var ageText by remember { mutableStateOf("") }
    val parsedEnteredAge = ageText.toLongOrNull()
    val isUnderageBlocked = parsedEnteredAge != null && parsedEnteredAge < 18L
    var allergies by remember { mutableStateOf<List<String>>(emptyList()) }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var coverageProvider by remember { mutableStateOf("") }
    var coverageMemberId by remember { mutableStateOf("") }
    var primaryDoctorName by remember { mutableStateOf("") }
    var preferredPharmacy by remember { mutableStateOf("") }

    var enableCheckInReminder by remember { mutableStateOf(false) }
    var enableMedReminders by remember { mutableStateOf(false) }
    var enableBiometricLock by remember { mutableStateOf(false) }
    var pendingReminderTarget by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingReminderTarget) {
                "checkin" -> enableCheckInReminder = true
                "meds" -> enableMedReminders = true
            }
        } else {
            onShowMessage("Notification permission was not granted. You can enable reminders later in Profile.")
        }
        pendingReminderTarget = null
    }

    fun toggleReminder(target: String, desired: Boolean) {
        if (!desired) {
            if (target == "checkin") enableCheckInReminder = false else enableMedReminders = false
            return
        }
        if (SanaNotificationChannels.hasNotificationPermission(context) ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            if (target == "checkin") enableCheckInReminder = true else enableMedReminders = true
        } else {
            pendingReminderTarget = target
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Step ${step + 1} of 3",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (step + 1) / 3f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            item {
                AnimatedContent(targetState = step, label = "onboarding_step") { activeStep ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GlassOnboardingStepIllustration(step = activeStep)
                        when (activeStep) {
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.onboarding_step1_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Choose your healthcare region to set clinical units, nurse advice lines, and coverage fields.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // US / Canada Region Selector
                            SanctuaryCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Healthcare Region",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HealthRegionDirectory.supportedRegions.forEach { region ->
                                        val isSelected = selectedRegion == region.code
                                        val tag = if (region.isCanada) "onboarding_region_ca" else "onboarding_region_us"
                                        RegionOptionCard(
                                            region = region,
                                            selected = isSelected,
                                            testTag = tag,
                                            onClick = { selectedRegion = region.code },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (activeRegionProfile.isCanada) {
                                        "Canada mode uses °C, mmol/L, kg, Provincial Health Card fields, and quick access to 811."
                                    } else {
                                        "United States mode uses °F, mg/dL, lbs, Health Insurance fields, and 911 / 988."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            SanctuaryCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SafetyPointRow(
                                        icon = Icons.Filled.Shield,
                                        title = "Informational Tool, Not a Diagnosis",
                                        body = "Symptom summaries and lab reviews help you prepare for doctor visits—they do not replace licensed medical care."
                                    )
                                    SafetyPointRow(
                                        icon = Icons.Filled.WarningAmber,
                                        title = "Call 911 for Emergencies",
                                        body = "For chest pain, severe breathing trouble, stroke signs, or heavy bleeding, call emergency services right away."
                                    )
                                    SafetyPointRow(
                                        icon = Icons.Filled.CheckCircle,
                                        title = "Private Account Sync",
                                        body = "Your profile, medications, and vitals stay tied to your Google account. Export or delete anytime."
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { acceptedDisclaimer = !acceptedDisclaimer }
                                    .testTag("onboarding_consent_card"),
                                color = if (acceptedDisclaimer) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (acceptedDisclaimer) MaterialTheme.colorScheme.primary else sanaColors.cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = acceptedDisclaimer,
                                        onCheckedChange = { acceptedDisclaimer = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("onboarding_consent_checkbox")
                                    )
                                    Text(
                                        text = stringResource(R.string.onboarding_consent_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { confirmed18Plus = !confirmed18Plus }
                                    .testTag("onboarding_age_18_card"),
                                color = if (confirmed18Plus) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (confirmed18Plus) MaterialTheme.colorScheme.primary else sanaColors.cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = confirmed18Plus,
                                        onCheckedChange = { confirmed18Plus = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("onboarding_age_18_checkbox")
                                    )
                                    Text(
                                        text = stringResource(R.string.onboarding_age_18_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        1 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_step2_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(enabled = !isUnderageBlocked) { step = 2 }
                                        .testTag("onboarding_skip_step2_button"),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .height(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.onboarding_skip_for_now),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "All fields below are optional. They populate your Medical ID and Doctor Visit PDF.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            SanctuaryCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = { if (it.length <= 80) displayName = it },
                                        label = { Text("Preferred Name") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .testTag("onboarding_name_input")
                                    )
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                                        label = { Text("Age (18+)") },
                                        isError = isUnderageBlocked,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .testTag("onboarding_age_input")
                                    )
                                }
                                if (isUnderageBlocked) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = sanaColors.emergencyBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, sanaColors.emergencyFg.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("onboarding_under_18_warning")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.WarningAmber,
                                                contentDescription = null,
                                                tint = sanaColors.emergencyFg,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(R.string.onboarding_under_18_title),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = sanaColors.emergencyFg
                                                )
                                                Text(
                                                    text = stringResource(R.string.onboarding_under_18_body),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                EditableTagSection(
                                    title = "Known Allergies",
                                    placeholder = "e.g., Penicillin, Ibuprofen",
                                    items = allergies,
                                    quickSuggestions = listOf("Penicillin", "Ibuprofen", "Peanuts", "Latex", "Pollen"),
                                    testTagPrefix = "onboarding_allergies",
                                    onAdd = { item ->
                                        if (allergies.none { it.equals(item, ignoreCase = true) } && allergies.size < 25) {
                                            allergies = allergies + item
                                        }
                                    },
                                    onRemove = { item -> allergies = allergies - item }
                                )
                            }

                            // Region-Adaptive Care Team & Coverage Card
                            SanctuaryCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${activeRegionProfile.coverageSectionTitle} (optional)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = coverageProvider,
                                    onValueChange = { if (it.length <= 80) coverageProvider = it },
                                    label = { Text(activeRegionProfile.coveragePrimaryLabel) },
                                    placeholder = { Text(activeRegionProfile.coveragePrimaryPlaceholder) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_coverage_provider")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = coverageMemberId,
                                    onValueChange = { if (it.length <= 40) coverageMemberId = it },
                                    label = { Text(activeRegionProfile.coverageIdLabel) },
                                    placeholder = { Text(activeRegionProfile.coverageIdPlaceholder) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_coverage_id")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = primaryDoctorName,
                                    onValueChange = { if (it.length <= 80) primaryDoctorName = it },
                                    label = { Text(activeRegionProfile.primaryClinicianLabel) },
                                    placeholder = { Text(activeRegionProfile.primaryClinicianPlaceholder) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_primary_doctor")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = preferredPharmacy,
                                    onValueChange = { if (it.length <= 100) preferredPharmacy = it },
                                    label = { Text(activeRegionProfile.pharmacyLabel) },
                                    placeholder = { Text(activeRegionProfile.pharmacyPlaceholder) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_preferred_pharmacy")
                                )
                            }

                            SanctuaryCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContactPhone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Emergency Contact (optional)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = emergencyName,
                                    onValueChange = { if (it.length <= 80) emergencyName = it },
                                    label = { Text("Contact Name") },
                                    placeholder = { Text("Partner, family member, or care partner") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_emergency_name")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = emergencyPhone,
                                    onValueChange = { if (it.length <= 30) emergencyPhone = it },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("+1 555-0199") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_emergency_phone")
                                )
                            }
                        }

                        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.onboarding_step3_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Choose optional local reminders and on-device lock protection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Region: ${activeRegionProfile.countryName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = activeRegionProfile.unitSummaryLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            SanctuaryCard {
                                OnboardingToggleRow(
                                    icon = Icons.Filled.NotificationsActive,
                                    title = "Daily Check-In Reminder (8:00 PM)",
                                    subtitle = "Local reminder to log daily mood, sleep, and hydration.",
                                    checked = enableCheckInReminder,
                                    testTag = "onboarding_toggle_checkin_reminder",
                                    onCheckedChange = { toggleReminder("checkin", it) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OnboardingToggleRow(
                                    icon = Icons.Filled.NotificationsActive,
                                    title = "Medication Reminders",
                                    subtitle = "Local notifications for scheduled medications.",
                                    checked = enableMedReminders,
                                    testTag = "onboarding_toggle_med_reminder",
                                    onCheckedChange = { toggleReminder("meds", it) }
                                )
                            }

                            SanctuaryCard {
                                OnboardingToggleRow(
                                    icon = Icons.Filled.Fingerprint,
                                    title = "Biometric App Lock",
                                    subtitle = if (canBiometric) {
                                        "Require fingerprint, face, or device PIN when reopening Well+."
                                    } else {
                                        "Set up a screen lock in Android Settings to enable."
                                    },
                                    checked = enableBiometricLock,
                                    enabled = canBiometric,
                                    testTag = "onboarding_toggle_biometric",
                                    onCheckedChange = { desired ->
                                        if (!desired) {
                                            enableBiometricLock = false
                                        } else {
                                            val activity = context as? FragmentActivity
                                            if (activity != null) {
                                                BiometricLockHelper.authenticate(
                                                    activity = activity,
                                                    title = "Enable App Lock",
                                                    subtitle = "Confirm biometrics or device PIN to activate lock",
                                                    onAuthenticated = { enableBiometricLock = true },
                                                    onError = { msg -> onShowMessage(msg) }
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            Text(
                                text = stringResource(R.string.onboarding_step3_ready_badge),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        }
                    }
                }
            }
        }

        // Docked frosted glass footer
        Surface(
            color = SanaTheme.glass.floatingBarFill,
            border = BorderStroke(1.dp, SanaTheme.glass.borderMid)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < 2) {
                            step += 1
                        } else {
                            val parsedAge = ageText.toLongOrNull()?.coerceIn(0L, 130L) ?: 0L
                            onComplete(
                                displayName,
                                parsedAge,
                                allergies,
                                emergencyName,
                                emergencyPhone,
                                selectedRegion,
                                coverageProvider.trim(),
                                coverageMemberId.trim(),
                                primaryDoctorName.trim(),
                                preferredPharmacy.trim(),
                                enableCheckInReminder,
                                enableMedReminders,
                                enableBiometricLock
                            )
                        }
                    },
                    enabled = !isSaving &&
                        when (step) {
                            0 -> acceptedDisclaimer && confirmed18Plus
                            1 -> !isUnderageBlocked
                            else -> acceptedDisclaimer && confirmed18Plus && !isUnderageBlocked
                        },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("onboarding_continue_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving…")
                    } else {
                        Text(
                            text = when (step) {
                                0 -> "Agree & Continue"
                                1 -> "Next: Reminders"
                                else -> "Finish Setup"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionOptionCard(
    region: HealthRegionProfile,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else sanaColors.cardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = region.code.shortCode,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "${region.countryName} selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = region.countryName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = region.unitSummaryLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SafetyPointRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(7.dp)
                    .size(18.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
