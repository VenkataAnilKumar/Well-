package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BillingEntitlementGateway
import com.example.core.HealthRegionDirectory
import com.example.core.HealthRegionProfile
import com.example.core.RegionalEmergencyDirectory
import com.example.core.VoiceAndEmergencyHelper
import com.example.core.designsystem.R
import com.example.data.model.BiologicalSex
import com.example.data.model.BloodType
import com.example.data.model.HealthProfile
import com.example.data.model.MedicationItem
import com.example.data.model.SubscriptionPlanOffer
import com.example.data.model.WellPlusSubscriptionState
import com.example.data.model.WellPlusTier
import com.example.ui.components.SanctuaryCard
import com.example.ui.theme.LocalSanaColors

/**
 * Well+ Pro subscription screen wired to Google Play Billing Library v9.
 */
@Composable
fun WellPlusProScreen(
    subscriptionState: WellPlusSubscriptionState,
    aiQuotaRemaining: Int,
    aiDailyLimit: Int,
    onBack: () -> Unit,
    onPurchasePlan: (Activity, SubscriptionPlanOffer) -> Unit,
    onRestorePurchases: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = context as? Activity
    val sanaColors = LocalSanaColors.current
    val offers = subscriptionState.offers.ifEmpty { BillingEntitlementGateway.DEFAULT_DISPLAY_CATALOG }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wellplus_pro_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pro_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
                FilledTonalButton(
                    onClick = onRestorePurchases,
                    enabled = !subscriptionState.isProcessing,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pro_restore_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore")
                }
            }
        }

        // Hero Plan Banner
        item {
            SanctuaryCard(
                containerColor = if (subscriptionState.isPro) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "Well+ Pro",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(10.dp).size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (subscriptionState.isPro) "Well+ Pro Active" else "Upgrade to Well+ Pro",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (subscriptionState.isPro) {
                                "Expanded ${WellPlusTier.PRO.dailyAiQuota}/day AI clinical quota active • $aiQuotaRemaining left today."
                            } else {
                                "Unlock ${WellPlusTier.PRO.dailyAiQuota}/day AI clinical checks, unlimited SOAP Doctor Visit PDFs, and priority lab analysis."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Plan: ${subscriptionState.tier.label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$aiQuotaRemaining / $aiDailyLimit today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Feature Comparison Card
        item {
            SanctuaryCard {
                Text(
                    text = "What's Included",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                ProFeatureRow("Daily AI Doctor & Vision Quota", "Free: 25/day  •  Pro: 200/day")
                HorizontalDivider(color = sanaColors.subtleDivider, modifier = Modifier.padding(vertical = 8.dp))
                ProFeatureRow("AI SOAP Pre-Visit PDF Briefs", "1-page clinician executive summary on Page 1")
                HorizontalDivider(color = sanaColors.subtleDivider, modifier = Modifier.padding(vertical = 8.dp))
                ProFeatureRow("Lab Biomarker & Rx Vision Scanning", "On-device ML Kit PII redaction always free")
                HorizontalDivider(color = sanaColors.subtleDivider, modifier = Modifier.padding(vertical = 8.dp))
                ProFeatureRow("Emergency Safety & Medical ID", "100% free forever on every tier")
            }
        }

        // Subscription Offers from Google Play Billing v9
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose Your Plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                offers.forEach { offer ->
                    val isCurrent = subscriptionState.activeProductId == offer.productId
                    SanctuaryCard(
                        containerColor = if (offer.badgeText.isNotBlank()) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        borderColor = if (offer.badgeText.isNotBlank() || isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            sanaColors.cardBorder
                        },
                        modifier = Modifier.testTag("pro_offer_card_${offer.productId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (offer.badgeText.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = offer.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = offer.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = offer.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = offer.formattedPrice,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = offer.billingPeriodLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (activity != null) {
                                    onPurchasePlan(activity, offer)
                                } else {
                                    onShowMessage("Unable to open Google Play purchase sheet.")
                                }
                            },
                            enabled = !isCurrent && !subscriptionState.isProcessing,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pro_subscribe_${offer.productId}")
                        ) {
                            if (subscriptionState.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Filled.Verified else Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCurrent) "Active Plan" else "Subscribe via Google Play")
                            }
                        }
                    }
                }
            }
        }

        // Transparent Play Store availability / status message
        val statusMsg = subscriptionState.statusMessage ?: if (!subscriptionState.storeAvailable) {
            "Google Play Billing is ready and awaiting published subscription SKUs on a Play Store test track. All core Well+ features remain available on Free."
        } else {
            null
        }
        if (!statusMsg.isNullOrBlank()) {
            item {
                SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text(
                        text = statusMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ProFeatureRow(title: String, detail: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp).size(18.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * High-contrast Emergency Medical ID Card for first responders & urgent care visits,
 * with 1-tap dialing, text sharing, and an opt-in Home Screen widget toggle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyMedicalIdScreen(
    profile: HealthProfile?,
    defaultDisplayName: String,
    activeMedications: List<MedicationItem>,
    healthRegion: HealthRegionProfile = HealthRegionDirectory.US,
    emergencyRegionOverride: String = "",
    showOnHomeWidget: Boolean,
    onToggleShowOnWidget: (Boolean) -> Unit,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    val emergencyProfile = RegionalEmergencyDirectory.resolve(
        context,
        emergencyRegionOverride.ifBlank { profile?.preferredEmergencyCountry.orEmpty() }
    )

    val displayName = profile?.displayName?.ifBlank { defaultDisplayName } ?: defaultDisplayName.ifBlank { "Patient" }
    val bloodType = BloodType.fromCode(profile?.bloodType)
    val biologicalSex = BiologicalSex.fromRaw(profile?.biologicalSex)
    val allergies = profile?.allergies.orEmpty()
    val conditions = profile?.conditions.orEmpty()
    val medNames = buildList {
        activeMedications.filter { it.active }.forEach { med ->
            add(if (med.dosage.isNotBlank()) "${med.name} (${med.dosage})" else med.name)
        }
        profile?.medications.orEmpty().forEach { name ->
            if (none { it.startsWith(name, ignoreCase = true) }) add(name)
        }
    }

    fun buildShareableText(): String = buildString {
        appendLine("=== EMERGENCY MEDICAL ID (Well+) ===")
        appendLine("Name: $displayName")
        if ((profile?.age ?: 0L) > 0L) appendLine("Age: ${profile?.age}")
        if (biologicalSex != BiologicalSex.UNSPECIFIED) appendLine("Sex: ${biologicalSex.label}")
        appendLine("Blood Type: ${bloodType.label}")
        appendLine("Allergies: ${allergies.ifEmpty { listOf("None reported") }.joinToString(", ")}")
        appendLine("Active Medications: ${medNames.ifEmpty { listOf("None listed") }.joinToString(", ")}")
        appendLine("Conditions: ${conditions.ifEmpty { listOf("None listed") }.joinToString(", ")}")
        if (!profile?.emergencyContactName.isNullOrBlank() || !profile?.emergencyContactPhone.isNullOrBlank()) {
            appendLine("Emergency Contact: ${profile?.emergencyContactName.orEmpty()} ${profile?.emergencyContactPhone.orEmpty()}".trim())
        }
        if (!profile?.primaryDoctorName.isNullOrBlank()) {
            appendLine("${healthRegion.primaryClinicianLabel}: ${profile?.primaryDoctorName}")
        }
        if (!profile?.coverageProvider.isNullOrBlank()) {
            appendLine("${healthRegion.coveragePrimaryLabel}: ${profile?.coverageProvider}")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emergency_medical_id_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("emergency_id_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
                FilledTonalButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Emergency Medical ID — $displayName")
                            putExtra(Intent.EXTRA_TEXT, buildShareableText())
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(sendIntent, "Share Medical ID"))
                        }.onFailure {
                            onShowMessage("No share target available on this device.")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("emergency_id_share_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Text")
                }
            }
        }

        // High-Contrast Crimson Emergency Header
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = sanaColors.emergencyFg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(
                                imageVector = Icons.Filled.LocalHospital,
                                contentDescription = "Emergency Medical ID",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "MEDICAL ID",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.onError.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Blood: ${bloodType.label}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                    val demographicParts = buildList {
                        if ((profile?.age ?: 0L) > 0L) add("Age ${profile?.age}")
                        if (biologicalSex != BiologicalSex.UNSPECIFIED) add(biologicalSex.label)
                        add(healthRegion.countryName)
                    }
                    Text(
                        text = demographicParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Allergies Alert Strip
        item {
            SanctuaryCard(
                containerColor = if (allergies.isNotEmpty()) {
                    sanaColors.emergencyBg.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                borderColor = if (allergies.isNotEmpty()) sanaColors.emergencyFg else sanaColors.cardBorder
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (allergies.isNotEmpty()) sanaColors.emergencyFg else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "KNOWN ALLERGIES",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (allergies.isNotEmpty()) sanaColors.emergencyFg else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (allergies.isEmpty()) {
                    Text(
                        text = "No known allergies recorded. Add allergies in Profile if applicable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allergies.forEach { allergy ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = sanaColors.emergencyFg
                            ) {
                                Text(
                                    text = allergy,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Medications & Conditions
        item {
            SanctuaryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Active Medications", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (medNames.isEmpty()) "No active medications listed." else medNames.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(color = sanaColors.subtleDivider, modifier = Modifier.padding(vertical = 12.dp))
                Text("Ongoing Conditions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (conditions.isEmpty()) "No conditions listed." else conditions.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!profile?.coverageProvider.isNullOrBlank() || !profile?.primaryDoctorName.isNullOrBlank()) {
                    HorizontalDivider(color = sanaColors.subtleDivider, modifier = Modifier.padding(vertical = 12.dp))
                    if (!profile?.primaryDoctorName.isNullOrBlank()) {
                        Text(
                            text = "${healthRegion.primaryClinicianLabel}: ${profile?.primaryDoctorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!profile?.coverageProvider.isNullOrBlank()) {
                        Text(
                            text = "${healthRegion.coveragePrimaryLabel}: ${profile?.coverageProvider}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 1-Tap Emergency & Contact Dialing
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val contactPhone = profile?.emergencyContactPhone.orEmpty()
                val contactName = profile?.emergencyContactName.orEmpty().ifBlank { "Emergency Contact" }
                if (contactPhone.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { VoiceAndEmergencyHelper.openDialer(context, contactPhone) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("emergency_id_dial_contact_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call $contactName ($contactPhone)")
                    }
                }
                Button(
                    onClick = { VoiceAndEmergencyHelper.openDialer(context, emergencyProfile.emergencyNumber) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sanaColors.emergencyFg,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("emergency_id_dial_911_button")
                ) {
                    Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call ${emergencyProfile.emergencyLabel} (${emergencyProfile.emergencyNumber})")
                }
            }
        }

        // Opt-In Home Screen Widget Privacy Toggle
        item {
            SanctuaryCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show on Home Screen Widget",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Opt-in: Displays blood type, allergies, and 1-tap emergency dial on the Well+ Medical ID widget. Automatically cleared on sign-out.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showOnHomeWidget,
                        onCheckedChange = onToggleShowOnWidget,
                        modifier = Modifier.testTag("emergency_id_widget_switch")
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
