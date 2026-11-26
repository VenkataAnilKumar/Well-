package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.core.designsystem.R
import com.example.core.BiometricLockHelper
import com.example.core.HealthRegionCode
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.LegalDocumentType
import com.example.core.RegionalEmergencyDirectory
import com.example.core.SanaNotificationChannels
import com.example.core.SanctuaryDeviceSettings
import com.example.data.model.BiologicalSex
import com.example.data.model.BloodType
import com.example.data.model.HealthProfile
import com.example.data.model.WellPlusSubscriptionState
import com.example.ui.components.EditableTagSection
import com.example.ui.components.GlassProfileAvatar
import com.example.ui.components.LegalPolicyViewerSheet
import com.example.ui.components.MedicalDisclaimerNote
import com.example.ui.components.SanctuaryCard
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.LocalSanaColors
import com.example.ui.theme.SanctuaryThemeMode
import com.example.ui.viewmodel.CloudUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    accountEmail: String,
    accountPhotoUrl: String?,
    defaultDisplayName: String,
    profileState: CloudUiState<HealthProfile?>,
    healthRegion: HealthRegionProfile = HealthRegionDirectory.US,
    deviceSettings: SanctuaryDeviceSettings = SanctuaryDeviceSettings(),
    subscriptionState: WellPlusSubscriptionState = WellPlusSubscriptionState(),
    aiQuotaRemaining: Int = 25,
    aiDailyLimit: Int = 25,
    isExportingJson: Boolean = false,
    isDeletingAccount: Boolean = false,
    themeMode: SanctuaryThemeMode = SanctuaryThemeMode.SYSTEM,
    onThemeModeChange: (SanctuaryThemeMode) -> Unit = {},
    onSelectHealthRegion: (HealthRegionCode) -> Unit = {},
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleScreenPrivacy: (Boolean) -> Unit = {},
    onToggleCloudAi: (Boolean) -> Unit = {},
    onToggleAutoMaskPii: (Boolean) -> Unit = {},
    onOpenWellPlusPro: () -> Unit = {},
    onOpenEmergencyMedicalId: () -> Unit = {},
    onSelectEmergencyRegion: (String) -> Unit = {},
    onToggleCheckInReminder: (Boolean) -> Unit = {},
    onToggleMedReminders: (Boolean) -> Unit = {},
    onToggleFollowUpReminders: (Boolean) -> Unit = {},
    onOpenPdfExport: () -> Unit = {},
    onExportJsonData: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onSaveProfile: (
        displayName: String,
        age: Long,
        biologicalSex: BiologicalSex,
        bloodType: BloodType,
        allergies: List<String>,
        medications: List<String>,
        conditions: List<String>,
        wellnessGoals: List<String>,
        emergencyContactName: String,
        emergencyContactPhone: String,
        coverageProvider: String,
        coverageMemberId: String,
        primaryDoctorName: String,
        preferredPharmacy: String
    ) -> Unit,
    onDeleteAllDataAndAccount: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val existing = (profileState as? CloudUiState.Success)?.data
    val canBiometric = remember(context) { BiometricLockHelper.canUseBiometricOrDeviceLock(context) }
    val activeRegion = remember(context, deviceSettings.emergencyRegionOverride, existing?.preferredEmergencyCountry) {
        val code = deviceSettings.emergencyRegionOverride.ifBlank { existing?.preferredEmergencyCountry.orEmpty() }
        RegionalEmergencyDirectory.resolve(context, code)
    }

    var displayName by remember { mutableStateOf(existing?.displayName?.ifBlank { defaultDisplayName } ?: defaultDisplayName) }
    var ageText by remember { mutableStateOf(if ((existing?.age ?: 0L) > 0L) existing?.age.toString() else "") }
    var biologicalSex by remember { mutableStateOf(BiologicalSex.fromRaw(existing?.biologicalSex)) }
    var bloodType by remember { mutableStateOf(BloodType.fromCode(existing?.bloodType)) }
    var allergies by remember { mutableStateOf(existing?.allergies.orEmpty()) }
    var medications by remember { mutableStateOf(existing?.medications.orEmpty()) }
    var conditions by remember { mutableStateOf(existing?.conditions.orEmpty()) }
    var wellnessGoals by remember { mutableStateOf(existing?.wellnessGoals.orEmpty()) }
    var emergencyContactName by remember { mutableStateOf(existing?.emergencyContactName.orEmpty()) }
    var emergencyContactPhone by remember { mutableStateOf(existing?.emergencyContactPhone.orEmpty()) }
    var coverageProvider by remember { mutableStateOf(existing?.coverageProvider.orEmpty()) }
    var coverageMemberId by remember { mutableStateOf(existing?.coverageMemberId.orEmpty()) }
    var primaryDoctorName by remember { mutableStateOf(existing?.primaryDoctorName.orEmpty()) }
    var preferredPharmacy by remember { mutableStateOf(existing?.preferredPharmacy.orEmpty()) }

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var activeLegalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var isSigningOut by remember { mutableStateOf(false) }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingNotificationAction
        pendingNotificationAction = null
        if (granted) {
            action?.invoke()
        } else {
            onShowMessage("Notification permission is needed for local reminders.")
        }
    }

    fun requestNotificationAndRun(action: () -> Unit) {
        if (SanaNotificationChannels.hasNotificationPermission(context) ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            action()
        } else {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(existing?.updatedAt) {
        if (existing != null) {
            displayName = existing.displayName.ifBlank { defaultDisplayName }
            ageText = if (existing.age > 0L) existing.age.toString() else ""
            biologicalSex = BiologicalSex.fromRaw(existing.biologicalSex)
            bloodType = BloodType.fromCode(existing.bloodType)
            allergies = existing.allergies
            medications = existing.medications
            conditions = existing.conditions
            wellnessGoals = existing.wellnessGoals
            emergencyContactName = existing.emergencyContactName
            emergencyContactPhone = existing.emergencyContactPhone
            coverageProvider = existing.coverageProvider
            coverageMemberId = existing.coverageMemberId
            primaryDoctorName = existing.primaryDoctorName
            preferredPharmacy = existing.preferredPharmacy
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "HEALTH RECORD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Profile & Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your region, coverage details, allergies, medications, and privacy controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (profileState is CloudUiState.Error) {
            item {
                SanctuaryErrorBanner(message = profileState.message)
            }
        }

        // Google Account Card
        item {
            SanctuaryCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!accountPhotoUrl.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AsyncImage(
                                model = accountPhotoUrl,
                                contentDescription = stringResource(R.string.cd_account_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    } else {
                        val avatarInitials = displayName
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                            .ifBlank { "W" }
                        GlassProfileAvatar(
                            initials = avatarInitials,
                            contentDescriptionText = stringResource(R.string.cd_account_avatar),
                            visualSize = 48.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName.ifBlank { defaultDisplayName.ifBlank { "Signed-In User" } },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = accountEmail.ifBlank { "Google Account" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Private Cloud Sync active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Well+ Pro Plan & Emergency Medical ID Quick Access Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onOpenWellPlusPro,
                    shape = RoundedCornerShape(18.dp),
                    color = if (subscriptionState.isPro) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 92.dp)
                        .testTag("profile_open_pro_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "Well+ Pro Plan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (subscriptionState.isPro) "Well+ Pro" else "Upgrade to Pro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$aiQuotaRemaining/$aiDailyLimit AI today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onOpenEmergencyMedicalId,
                    shape = RoundedCornerShape(18.dp),
                    color = sanaColors.emergencyBg.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, sanaColors.emergencyFg.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 92.dp)
                        .testTag("profile_open_emergency_id_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalHospital,
                            contentDescription = "Emergency Medical ID",
                            tint = sanaColors.emergencyFg,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Medical ID Card",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = sanaColors.emergencyFg
                        )
                        Text(
                            text = if (deviceSettings.showEmergencyIdOnWidget) "Widget active" else "First-responder card",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Health Region Mode (United States / Canada) Card
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_health_region_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_health_region_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HealthRegionDirectory.supportedRegions.forEach { regionOption ->
                        val selected = healthRegion.code == regionOption.code
                        val tag = if (regionOption.isCanada) "profile_region_ca" else "profile_region_us"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 80.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectHealthRegion(regionOption.code) }
                                .testTag(tag),
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
                                        text = regionOption.countryName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "${regionOption.countryName} active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = regionOption.unitSummaryLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Appearance Theme Selector Card
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.theme_section_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.theme_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SanctuaryThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        val modeIcon = when (mode) {
                            SanctuaryThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            SanctuaryThemeMode.LIGHT -> Icons.Filled.LightMode
                            SanctuaryThemeMode.DARK -> Icons.Filled.DarkMode
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onThemeModeChange(mode) },
                            leadingIcon = {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("theme_mode_${mode.storageKey}")
                        )
                    }
                }
            }
        }

        // Basics Card
        item {
            SanctuaryCard {
                Text(
                    text = "Basic Details",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { if (it.length <= 80) displayName = it },
                        label = { Text("Preferred Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("profile_name_input")
                    )
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(3)
                            ageText = digits
                        },
                        label = { Text("Age") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("profile_age_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Biological Sex (optional)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BiologicalSex.entries.forEach { sex ->
                        FilterChip(
                            selected = biologicalSex == sex,
                            onClick = { biologicalSex = sex },
                            label = { Text(sex.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Blood Type (optional)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BloodType.entries.forEach { bt ->
                        FilterChip(
                            selected = bloodType == bt,
                            onClick = { bloodType = bt },
                            label = { Text(bt.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // Clinical Tags Card
        item {
            SanctuaryCard {
                EditableTagSection(
                    title = "Known Allergies",
                    placeholder = "Add allergy (e.g., Penicillin)",
                    items = allergies,
                    quickSuggestions = listOf("Penicillin", "Ibuprofen", "Peanuts", "Pollen", "Latex"),
                    testTagPrefix = "allergies",
                    onAdd = { item ->
                        if (allergies.none { it.equals(item, ignoreCase = true) } && allergies.size < 25) {
                            allergies = allergies + item
                        }
                    },
                    onRemove = { item -> allergies = allergies - item }
                )

                Spacer(modifier = Modifier.height(18.dp))
                EditableTagSection(
                    title = "Current Medications & Supplements",
                    placeholder = "Add medication (e.g., Lisinopril)",
                    items = medications,
                    quickSuggestions = listOf("Vitamin D", "Magnesium", "Antihistamine", "Inhaler"),
                    testTagPrefix = "medications",
                    onAdd = { item ->
                        if (medications.none { it.equals(item, ignoreCase = true) } && medications.size < 25) {
                            medications = medications + item
                        }
                    },
                    onRemove = { item -> medications = medications - item }
                )

                Spacer(modifier = Modifier.height(18.dp))
                EditableTagSection(
                    title = "Ongoing Health Conditions",
                    placeholder = "Add condition (e.g., Asthma)",
                    items = conditions,
                    quickSuggestions = listOf("Asthma", "Migraines", "Hypertension", "Seasonal Allergies", "GERD"),
                    testTagPrefix = "conditions",
                    onAdd = { item ->
                        if (conditions.none { it.equals(item, ignoreCase = true) } && conditions.size < 25) {
                            conditions = conditions + item
                        }
                    },
                    onRemove = { item -> conditions = conditions - item }
                )

                Spacer(modifier = Modifier.height(18.dp))
                EditableTagSection(
                    title = "Daily Wellness Goals",
                    placeholder = "Add focus area",
                    items = wellnessGoals,
                    quickSuggestions = listOf("Better Sleep", "Hydration", "Stress Relief", "Daily Walk"),
                    testTagPrefix = "goals",
                    onAdd = { item ->
                        if (wellnessGoals.none { it.equals(item, ignoreCase = true) } && wellnessGoals.size < 15) {
                            wellnessGoals = wellnessGoals + item
                        }
                    },
                    onRemove = { item -> wellnessGoals = wellnessGoals - item }
                )
            }
        }

        // Emergency Contact Card
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Emergency Contact",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shown on your Urgent / Emergency escalation sheet for quick one-tap dialing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = emergencyContactName,
                    onValueChange = { if (it.length <= 80) emergencyContactName = it },
                    label = { Text("Contact Name") },
                    placeholder = { Text("e.g., Jordan Lee (Partner)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_emergency_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = emergencyContactPhone,
                    onValueChange = { if (it.length <= 30) emergencyContactPhone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g., +1 555 234 5678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_emergency_phone_input")
                )
            }
        }

        // Coverage & Care Team Card (US Insurance or Canada Provincial Health Card)
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = healthRegion.coverageSectionTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Included on your Medical ID and Doctor Visit PDF. Member/card IDs are never sent to AI prompts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = coverageProvider,
                    onValueChange = { if (it.length <= 80) coverageProvider = it },
                    label = { Text(healthRegion.coveragePrimaryLabel) },
                    placeholder = { Text(healthRegion.coveragePrimaryPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_coverage_provider_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = coverageMemberId,
                    onValueChange = { if (it.length <= 40) coverageMemberId = it },
                    label = { Text(healthRegion.coverageIdLabel) },
                    placeholder = { Text(healthRegion.coverageIdPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_coverage_id_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = primaryDoctorName,
                    onValueChange = { if (it.length <= 80) primaryDoctorName = it },
                    label = { Text(healthRegion.primaryClinicianLabel) },
                    placeholder = { Text(healthRegion.primaryClinicianPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_primary_doctor_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = preferredPharmacy,
                    onValueChange = { if (it.length <= 100) preferredPharmacy = it },
                    label = { Text(healthRegion.pharmacyLabel) },
                    placeholder = { Text(healthRegion.pharmacyPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_preferred_pharmacy_input")
                )
            }
        }

        // Save Profile Button
        item {
            val enteredAge = ageText.toLongOrNull()
            val isUnderage = enteredAge != null && enteredAge < 18L
            Button(
                onClick = {
                    if (isUnderage) {
                        onShowMessage("Well+ is designed for adults 18 and older.")
                        return@Button
                    }
                    val parsedAge = enteredAge?.coerceIn(18L, 130L) ?: 0L
                    onSaveProfile(
                        displayName,
                        parsedAge,
                        biologicalSex,
                        bloodType,
                        allergies,
                        medications,
                        conditions,
                        wellnessGoals,
                        emergencyContactName.trim(),
                        emergencyContactPhone.trim(),
                        coverageProvider.trim(),
                        coverageMemberId.trim(),
                        primaryDoctorName.trim(),
                        preferredPharmacy.trim()
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_profile_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Health Profile", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Sanctuary Security & Smart Reminders Card
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Privacy Lock & Smart Reminders",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                ProfileToggleRow(
                    icon = Icons.Filled.Fingerprint,
                    title = "Biometric App Lock",
                    subtitle = if (canBiometric) {
                        "Require fingerprint, face, or device PIN when reopening Well+."
                    } else {
                        "Set up a screen lock or biometrics in Android Settings to enable."
                    },
                    checked = deviceSettings.biometricLockEnabled,
                    enabled = canBiometric,
                    testTag = "profile_biometric_switch",
                    onCheckedChange = { wantEnable ->
                        if (!wantEnable) {
                            onToggleBiometricLock(false)
                        } else {
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                onShowMessage("Unable to launch biometric verification.")
                            } else {
                                BiometricLockHelper.authenticate(
                                    activity = activity,
                                    title = "Enable App Lock",
                                    subtitle = "Confirm your identity to protect your health records",
                                    onAuthenticated = { onToggleBiometricLock(true) },
                                    onError = { msg -> onShowMessage(msg) }
                                )
                            }
                        }
                    }
                )

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                ProfileToggleRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.profile_screen_privacy_title),
                    subtitle = stringResource(R.string.profile_screen_privacy_subtitle),
                    checked = deviceSettings.screenPrivacyEnabled,
                    enabled = true,
                    testTag = "profile_screen_privacy_switch",
                    onCheckedChange = onToggleScreenPrivacy
                )

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                ProfileToggleRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = stringResource(R.string.profile_cloud_ai_title),
                    subtitle = stringResource(R.string.profile_cloud_ai_subtitle),
                    checked = deviceSettings.cloudAiEnabled,
                    enabled = true,
                    testTag = "profile_cloud_ai_switch",
                    onCheckedChange = onToggleCloudAi
                )

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                ProfileToggleRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Auto-Mask PII on Photo Scans",
                    subtitle = "Automatically detects and masks SSN, MRN, phone, email, and DOB on-device with preview before AI Vision scanning.",
                    checked = deviceSettings.autoMaskPiiBeforeScan,
                    enabled = true,
                    testTag = "profile_auto_mask_pii_switch",
                    onCheckedChange = onToggleAutoMaskPii
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, sanaColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_ai_quota_pill")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (deviceSettings.cloudAiEnabled) {
                                    "$aiQuotaRemaining / $aiDailyLimit free AI checks left today"
                                } else {
                                    "On-Device Free Mode Active"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (deviceSettings.cloudAiEnabled) {
                                    "Resets at local midnight • Core tracking, Smart Suggestions & safety alerts are always 100% free and work offline."
                                } else {
                                    "All tracking, Smart Suggestions, PHQ-9/GAD-7 guidance, and Doctor PDFs continue working with zero cloud AI."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Localized Emergency & Crisis Helpline Region Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showRegionPicker = true }
                        .padding(vertical = 6.dp)
                        .testTag("profile_emergency_region_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_emergency_region_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (deviceSettings.emergencyRegionOverride.isBlank() || deviceSettings.emergencyRegionOverride == "AUTO") "Auto: " else ""}${activeRegion.countryName} • ${activeRegion.emergencyNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Select emergency region",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                ProfileToggleRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Daily Wellness Check-In (8:00 PM)",
                    subtitle = "Gentle local evening reminder to log mood, sleep, and hydration.",
                    checked = deviceSettings.checkInReminderEnabled,
                    enabled = true,
                    testTag = "profile_checkin_reminder_switch",
                    onCheckedChange = { enable ->
                        if (enable) {
                            requestNotificationAndRun { onToggleCheckInReminder(true) }
                        } else {
                            onToggleCheckInReminder(false)
                        }
                    }
                )

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                ProfileToggleRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Medication Schedule Reminders",
                    subtitle = "Local alerts aligned with your active medication slots.",
                    checked = deviceSettings.medRemindersEnabled,
                    enabled = true,
                    testTag = "profile_med_reminder_switch",
                    onCheckedChange = { enable ->
                        if (enable) {
                            requestNotificationAndRun { onToggleMedReminders(true) }
                        } else {
                            onToggleMedReminders(false)
                        }
                    }
                )

                HorizontalDivider(
                    color = sanaColors.subtleDivider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                ProfileToggleRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "48-Hour Symptom Follow-Up Nudges",
                    subtitle = "Automatically nudge you 48 hours after an AI triage consultation.",
                    checked = deviceSettings.followUpRemindersEnabled,
                    enabled = true,
                    testTag = "profile_followup_reminder_switch",
                    onCheckedChange = { enable ->
                        if (enable) {
                            requestNotificationAndRun { onToggleFollowUpReminders(true) }
                        } else {
                            onToggleFollowUpReminders(false)
                        }
                    }
                )
            }
        }

        // Legal, Policies & Data Portability Hub Card
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_legal_hub_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_legal_hub_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                LegalDocumentType.entries.forEachIndexed { idx, doc ->
                    if (idx > 0) {
                        HorizontalDivider(color = sanaColors.subtleDivider)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeLegalDocument = doc }
                            .padding(vertical = 10.dp)
                            .testTag("profile_legal_${doc.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = doc.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open ${doc.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Doctor Export & Full Data Controls Card
        item {
            SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(
                    text = "Doctor Export & Data Control",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export a clean clinical PDF for appointments, download all your records as JSON (GDPR/CCPA), or permanently delete all synced cloud records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onOpenPdfExport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_export_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pdf_export_action))
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onExportJsonData,
                    enabled = !isExportingJson,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_export_json_button")
                ) {
                    if (isExportingJson) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_export_json_loading))
                    } else {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_export_json_action))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        deleteConfirmText = ""
                        showDeleteConfirm = true
                    },
                    enabled = !isDeletingAccount,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_delete_all_data_button")
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deleting cloud data…", color = MaterialTheme.colorScheme.error)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.delete_account_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                enabled = !isSigningOut,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                if (isSigningOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Signing out…",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_sign_out),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            MedicalDisclaimerNote()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.sign_out_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.sign_out_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        isSigningOut = true
                        onSignOut()
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_sign_out),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_account_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This permanently deletes your health profile, consultations, chat history, daily check-ins, medications, vitals, and care tasks from the cloud and cancels all local reminders. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it.take(16) },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_confirm_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAllDataAndAccount()
                    },
                    enabled = deleteConfirmText.trim() == "DELETE" && !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Permanently Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRegionPicker) {
        val selectedCode = deviceSettings.emergencyRegionOverride.ifBlank { "AUTO" }
        AlertDialog(
            onDismissRequest = { showRegionPicker = false },
            icon = { Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Emergency & Crisis Region") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val options = listOf("AUTO" to "Auto-Detect (SIM / Device Locale)") +
                        RegionalEmergencyDirectory.supportedRegions.map { r ->
                            r.countryCode to "${r.countryName} (${r.emergencyNumber})"
                        }
                    options.forEach { (code, label) ->
                        val isSelected = selectedCode == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectEmergencyRegion(if (code == "AUTO") "" else code)
                                    showRegionPicker = false
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("region_option_${code.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelectEmergencyRegion(if (code == "AUTO") "" else code)
                                    showRegionPicker = false
                                }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    val legalDoc = activeLegalDocument
    if (legalDoc != null) {
        LegalPolicyViewerSheet(
            document = legalDoc,
            onShowMessage = onShowMessage,
            onDismiss = { activeLegalDocument = null }
        )
    }
}

@Composable
private fun ProfileToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
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
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
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
