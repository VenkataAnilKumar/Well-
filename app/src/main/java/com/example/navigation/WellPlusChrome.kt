package com.example.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.core.CriticalVitalAlert
import com.example.core.DoctorExportOptions
import com.example.core.HealthRegionProfile
import com.example.core.SymptomRedFlagResult
import com.example.data.model.AiFeatureArea
import com.example.data.model.AiSafetyReportReason
import com.example.data.model.Consultation
import com.example.data.model.HealthProfile
import com.example.ui.components.AiSafetyReportSheet
import com.example.ui.components.CriticalVitalAlertDialog
import com.example.ui.components.SymptomRedFlagDialog
import com.example.ui.screens.DoctorPdfExportDialog
import com.example.ui.screens.EmergencyEscalationSheet
import com.example.ui.theme.GlassFloatingBarContainer
import com.example.ui.theme.SanaTheme

@Composable
internal fun WellPlusBottomBar(
    currentRoute: String?,
    onSelectTab: (WellPlusTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        GlassFloatingBarContainer(
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (SanaTheme.isDark) 0.22f else 0.14f
                    ),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WellPlusTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.routeId
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("nav_${tab.routeId}")
                    )
                }
            }
        }
    }
}

@Composable
internal fun WellPlusNavigationRail(
    currentRoute: String?,
    onSelectTab: (WellPlusTab) -> Unit
) {
    NavigationRail(
        containerColor = SanaTheme.glass.floatingBarFill,
        modifier = Modifier.fillMaxHeight()
    ) {
        val railItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(
                alpha = if (SanaTheme.isDark) 0.22f else 0.14f
            ),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WellPlusTab.entries.forEach { tab ->
            val selected = currentRoute == tab.routeId
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                colors = railItemColors,
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.testTag("rail_${tab.routeId}")
            )
        }
    }
    VerticalDivider(
        thickness = 1.dp,
        color = SanaTheme.glass.borderMid
    )
}

@Composable
internal fun WellPlusGlobalOverlays(
    context: Context,
    isBiometricLocked: Boolean,
    currentProfile: HealthProfile?,
    activeHealthRegion: HealthRegionProfile,
    emergencyRegionOverride: String,
    emergencyAlertConsultation: Consultation?,
    activeCriticalVitalAlert: CriticalVitalAlert?,
    activeSymptomRedFlag: SymptomRedFlagResult?,
    pendingAiReport: Pair<AiFeatureArea, String>?,
    isSubmittingSafetyReport: Boolean,
    showPdfExportDialog: Boolean,
    isExportingPdf: Boolean,
    isSynthesizingSoap: Boolean = false,
    onShowMessage: (String) -> Unit,
    onDismissEmergencyAlert: () -> Unit,
    onDismissCriticalVitalAlert: () -> Unit,
    onDismissSymptomRedFlag: () -> Unit,
    onDismissAiReport: () -> Unit,
    onSubmitAiReport: (AiFeatureArea, AiSafetyReportReason, String, String, () -> Unit) -> Unit,
    onDismissPdfDialog: () -> Unit,
    onConfirmPdfExport: (DoctorExportOptions, (Intent) -> Unit) -> Unit
) {
    if (isBiometricLocked) return

    emergencyAlertConsultation?.let { emergencyTarget ->
        EmergencyEscalationSheet(
            consultation = emergencyTarget,
            profile = currentProfile,
            healthRegion = activeHealthRegion,
            emergencyRegionOverride = emergencyRegionOverride,
            onShowMessage = onShowMessage,
            onDismiss = onDismissEmergencyAlert
        )
    }

    activeCriticalVitalAlert?.let { criticalVital ->
        CriticalVitalAlertDialog(
            alert = criticalVital,
            profile = currentProfile,
            emergencyRegionOverride = emergencyRegionOverride,
            onShowMessage = onShowMessage,
            onDismiss = onDismissCriticalVitalAlert
        )
    }

    activeSymptomRedFlag?.let { symptomRedFlag ->
        SymptomRedFlagDialog(
            redFlag = symptomRedFlag,
            profile = currentProfile,
            emergencyRegionOverride = emergencyRegionOverride,
            onShowMessage = onShowMessage,
            onDismiss = onDismissSymptomRedFlag
        )
    }

    pendingAiReport?.let { reportTarget ->
        AiSafetyReportSheet(
            featureArea = reportTarget.first,
            contentSnippet = reportTarget.second,
            isSubmitting = isSubmittingSafetyReport,
            onDismiss = onDismissAiReport,
            onSubmit = { reason, note ->
                onSubmitAiReport(
                    reportTarget.first,
                    reason,
                    reportTarget.second,
                    note,
                    onDismissAiReport
                )
            }
        )
    }

    if (showPdfExportDialog) {
        DoctorPdfExportDialog(
            isExporting = isExportingPdf,
            isSynthesizingSoap = isSynthesizingSoap,
            onDismiss = onDismissPdfDialog,
            onConfirmExport = { options ->
                onConfirmPdfExport(options) { shareIntent ->
                    onDismissPdfDialog()
                    try {
                        context.startActivity(shareIntent)
                    } catch (_: ActivityNotFoundException) {
                        onShowMessage("No app available to share PDF files.")
                    }
                }
            }
        )
    }
}
