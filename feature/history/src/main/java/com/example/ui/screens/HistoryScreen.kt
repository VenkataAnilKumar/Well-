package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.R
import com.example.data.model.AiFeatureArea
import com.example.data.model.Consultation
import com.example.data.model.ConsultationType
import com.example.data.model.DailyCheckIn
import com.example.data.model.MedicationItem
import com.example.data.model.UrgencyLevel
import com.example.data.model.VitalReading
import com.example.ui.components.EmptySanctuaryState
import com.example.ui.components.HealthEmptyIllustrationKind
import com.example.ui.components.PaginatedListFooter
import com.example.ui.components.SanctuaryErrorBanner
import com.example.ui.theme.GlassShimmerCard
import com.example.ui.theme.LocalSanaColors
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.InsightsUiState
import com.example.ui.viewmodel.PaginationUiState

private enum class JourneySectionTab {
    CONSULTATIONS,
    WELLNESS_LOG
}

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    SYMPTOMS("Symptoms"),
    PHOTO_LAB("Photo & Lab"),
    SELF_CARE("Self-Care"),
    HIGH_PRIORITY("Doctor / Urgent")
}

private enum class HistorySortOrder(val label: String) {
    NEWEST("Newest"),
    HIGHEST_SEVERITY("Highest Severity")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    todayDateKey: String = "",
    consultationsState: CloudUiState<List<Consultation>>,
    checkInsState: CloudUiState<List<DailyCheckIn>>,
    medications: List<MedicationItem> = emptyList(),
    vitals: List<VitalReading> = emptyList(),
    insightsState: InsightsUiState = InsightsUiState.Idle,
    onGenerateInsights: () -> Unit = {},
    onOpenPdfExport: () -> Unit = {},
    onOpenConsultation: (String, Boolean) -> Unit,
    onDeleteConsultation: (String) -> Unit,
    onStartNewConsult: () -> Unit,
    onOpenDailyCheckIn: () -> Unit = {},
    onReportAiResponse: (AiFeatureArea, String) -> Unit = { _, _ -> },
    paginationState: PaginationUiState = PaginationUiState(),
    onLoadMore: () -> Unit = {}
) {
    val sanaColors = LocalSanaColors.current
    var activeTab by remember { mutableStateOf(JourneySectionTab.CONSULTATIONS) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var sortOrder by remember { mutableStateOf(HistorySortOrder.NEWEST) }
    var pendingDelete by remember { mutableStateOf<Consultation?>(null) }

    val consultations = (consultationsState as? CloudUiState.Success)?.data.orEmpty()
    val checkIns = (checkInsState as? CloudUiState.Success)?.data.orEmpty()

    val filteredConsultations = remember(consultations, searchQuery, activeFilter, sortOrder) {
        val matching = consultations.filter { item ->
            val matchesFilter = when (activeFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.SYMPTOMS -> item.parsedType == ConsultationType.SYMPTOM_CHECK
                HistoryFilter.PHOTO_LAB -> item.parsedType == ConsultationType.PHOTO_ANALYSIS || item.hadPhotoAttachment
                HistoryFilter.SELF_CARE -> item.parsedUrgency == UrgencyLevel.SELF_CARE
                HistoryFilter.HIGH_PRIORITY -> item.parsedUrgency != UrgencyLevel.SELF_CARE
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.bodyArea.contains(searchQuery, ignoreCase = true) ||
                item.symptomSummary.contains(searchQuery, ignoreCase = true) ||
                item.aiSummary.contains(searchQuery, ignoreCase = true) ||
                item.associatedSymptoms.any { it.contains(searchQuery, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
        when (sortOrder) {
            HistorySortOrder.NEWEST -> matching
            HistorySortOrder.HIGHEST_SEVERITY -> matching.sortedByDescending { it.severity }
        }
    }

    val groupedConsultations = remember(filteredConsultations, sortOrder) {
        if (sortOrder == HistorySortOrder.HIGHEST_SEVERITY) {
            mapOf("Ranked by Severity" to filteredConsultations)
        } else {
            filteredConsultations.groupBy { historyTimeBucket(it.updatedAt ?: it.createdAt) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Cloud Synced pill
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(R.string.history_cloud_synced),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onOpenPdfExport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("history_export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pdf_export_action))
                }
            }
        }

        // Health Journey Pulse & Severity Sparkline Card
        item {
            HealthJourneyOverviewCard(
                consultations = consultations,
                checkIns = checkIns
            )
        }

        // 30-Day Mood & Symptom Frequency Visualization
        item {
            ThirtyDayWellnessAnalyticsCard(
                consultations = consultations,
                checkIns = checkIns
            )
        }

        // 30-Day AI Longitudinal Health Synthesis
        item {
            LongitudinalInsightsCard(
                insightsState = insightsState,
                hasAnyHealthData = consultations.isNotEmpty() ||
                    checkIns.isNotEmpty() ||
                    vitals.isNotEmpty() ||
                    medications.isNotEmpty(),
                onGenerateInsights = onGenerateInsights,
                onReportAiResponse = onReportAiResponse
            )
        }

        // Journey Section Switcher (Consultations vs Wellness Log)
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeTab == JourneySectionTab.CONSULTATIONS,
                    onClick = { activeTab = JourneySectionTab.CONSULTATIONS },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("history_tab_consultations")
                ) {
                    Text("${stringResource(R.string.history_tab_consultations)} (${consultations.size})")
                }
                SegmentedButton(
                    selected = activeTab == JourneySectionTab.WELLNESS_LOG,
                    onClick = { activeTab = JourneySectionTab.WELLNESS_LOG },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("history_tab_wellness")
                ) {
                    Text("${stringResource(R.string.history_tab_wellness)} (${checkIns.size})")
                }
            }
        }

        if (activeTab == JourneySectionTab.CONSULTATIONS) {
            // Search, Filter & Sort Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("history_clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search)
                                    )
                                }
                            }
                        },
                        placeholder = { Text("Search symptoms, body area, or AI notes...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HistoryFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("history_filter_${filter.name}")
                            )
                        }
                    }

                    if (consultations.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${filteredConsultations.size} of ${consultations.size} entries",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    sortOrder = if (sortOrder == HistorySortOrder.NEWEST) {
                                        HistorySortOrder.HIGHEST_SEVERITY
                                    } else {
                                        HistorySortOrder.NEWEST
                                    }
                                },
                                modifier = Modifier.testTag("history_sort_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sort: ${sortOrder.label}")
                            }
                        }
                    }
                }
            }

            when (consultationsState) {
                is CloudUiState.Loading -> {
                    items(3) {
                        GlassShimmerCard(lines = 3)
                    }
                }

                is CloudUiState.Error -> {
                    item {
                        SanctuaryErrorBanner(message = consultationsState.message)
                    }
                }

                is CloudUiState.Success -> {
                    if (filteredConsultations.isEmpty()) {
                        val hasFilters = searchQuery.isNotBlank() || activeFilter != HistoryFilter.ALL
                        item {
                            EmptySanctuaryState(
                                illustrationKind = HealthEmptyIllustrationKind.HISTORY,
                                title = if (consultations.isEmpty()) {
                                    "Your health journey starts here"
                                } else {
                                    "No matching symptom entries"
                                },
                                subtitle = if (consultations.isEmpty()) {
                                    "Complete a guided symptom intake or photo analysis and Well+ will build your synced clinical timeline here."
                                } else {
                                    "Try clearing your search terms or switching back to all consultations."
                                },
                                actionLabel = when {
                                    consultations.isEmpty() -> "Start Symptom Check"
                                    hasFilters -> "Reset Filters"
                                    else -> null
                                },
                                actionTestTag = "history_empty_start_button",
                                onAction = when {
                                    consultations.isEmpty() -> onStartNewConsult
                                    hasFilters -> {
                                        {
                                            searchQuery = ""
                                            activeFilter = HistoryFilter.ALL
                                        }
                                    }
                                    else -> null
                                }
                            )
                        }
                    } else {
                        groupedConsultations.forEach { (bucketTitle, entries) ->
                            item(key = "section_$bucketTitle") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Text(
                                        text = bucketTitle.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• ${entries.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(entries, key = { it.id }) { consultation ->
                                ConsultationTimelineCard(
                                    consultation = consultation,
                                    onOpenReport = { onOpenConsultation(consultation.id, false) },
                                    onOpenChat = { onOpenConsultation(consultation.id, true) },
                                    onRequestDelete = { pendingDelete = consultation }
                                )
                            }
                        }
                    }

                    if (paginationState.hasMore || paginationState.isLoadingMore || paginationState.errorMessage != null) {
                        item(key = "history_pagination_footer") {
                            PaginatedListFooter(
                                paginationState = paginationState,
                                onLoadMore = onLoadMore,
                                buttonTestTag = "history_load_more_button"
                            )
                        }
                    }
                }
            }
        } else {
            // Wellness Check-In Log Over Time
            when (checkInsState) {
                is CloudUiState.Loading -> {
                    items(3) {
                        GlassShimmerCard(lines = 2)
                    }
                }

                is CloudUiState.Error -> {
                    item {
                        SanctuaryErrorBanner(message = checkInsState.message)
                    }
                }

                is CloudUiState.Success -> {
                    if (checkIns.isEmpty()) {
                        item {
                            EmptySanctuaryState(
                                illustrationKind = HealthEmptyIllustrationKind.CHECK_IN,
                                title = "No daily check-ins recorded yet",
                                subtitle = "Log your daily mood, general health status, sleep, and hydration to track how you feel across the week.",
                                actionLabel = "Record Today's Check-In",
                                actionTestTag = "history_empty_checkin_button",
                                onAction = onOpenDailyCheckIn
                            )
                        }
                    } else {
                        items(checkIns, key = { it.dateKey.ifBlank { it.id } }) { entry ->
                            WellnessCheckInHistoryCard(
                                entry = entry,
                                isToday = todayDateKey.isNotBlank() && entry.dateKey == todayDateKey
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Consultation?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Remove \"${target.title}\" and its follow-up messages from your cloud history? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConsultation(target.id)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_consultation")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
