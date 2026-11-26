package com.example.feature.widgets

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.AppResourceResolver
import com.example.core.DateKeyProvider
import com.example.core.MedicationActionReceiver
import com.example.core.WidgetRefreshBridge
import com.example.core.WidgetSnapshot
import com.example.core.WidgetSnapshotStore
import com.example.data.repository.CheckInAndCareRepository
import com.example.data.repository.FirestoreSessionHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private const val WIDGET_TAG = "WellPlusWidgets"
private val PARAM_MOOD_LEVEL = ActionParameters.Key<Int>("mood_level")
private val PARAM_MOOD_LABEL = ActionParameters.Key<String>("mood_label")
private val PARAM_MED_ID = ActionParameters.Key<String>("med_id")
private val PARAM_MED_NAME = ActionParameters.Key<String>("med_name")

private data class QuickMoodPreset(
    val level: Int,
    val shortLabel: String,
    val fullLabel: String
)

private val WIDGET_MOOD_PRESETS = listOf(
    QuickMoodPreset(1, "Low", "Low"),
    QuickMoodPreset(2, "Tense", "Tense"),
    QuickMoodPreset(3, "Okay", "Steady"),
    QuickMoodPreset(4, "Calm", "Calm"),
    QuickMoodPreset(5, "Great", "Thriving")
)

/**
 * Registers the Glance widget refresh callback into `:core:common`'s [WidgetRefreshBridge]
 * so background workers and app state updates can trigger widget redraws without coupling
 * `:core:*` modules to `:feature:widgets`.
 */
object WellPlusWidgetRefresher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerBridge() {
        WidgetRefreshBridge.onRefreshRequested = { ctx ->
            refreshAll(ctx)
        }
    }

    fun refreshAll(context: Context) {
        val appCtx = context.applicationContext
        scope.launch {
            try {
                WellPlusQuickMoodWidget().updateAll(appCtx)
            } catch (e: Exception) {
                Log.w(WIDGET_TAG, "Quick Mood widget update skipped: ${e.message}")
            }
            try {
                WellPlusNextDoseWidget().updateAll(appCtx)
            } catch (e: Exception) {
                Log.w(WIDGET_TAG, "Next Dose widget update skipped: ${e.message}")
            }
            try {
                WellPlusEmergencyIdWidget().updateAll(appCtx)
            } catch (e: Exception) {
                Log.w(WIDGET_TAG, "Emergency ID widget update skipped: ${e.message}")
            }
        }
    }
}

class WellPlusQuickMoodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WellPlusQuickMoodWidget()
}

class WellPlusNextDoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WellPlusNextDoseWidget()
}

/**
 * Home Screen Glance Widget #1: 1-Tap Quick Mood Check-In + Streak & Health Connect Steps.
 */
class WellPlusQuickMoodWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).read()
        val todayKey = DateKeyProvider.formatCurrentDateKey()
        provideContent {
            GlanceTheme {
                QuickMoodWidgetContent(snapshot = snapshot, todayKey = todayKey)
            }
        }
    }
}

@Composable
private fun QuickMoodWidgetContent(
    snapshot: WidgetSnapshot,
    todayKey: String
) {
    val isTodayLogged = snapshot.todayDateKey == todayKey && snapshot.todayMoodLevel in 1..5
    val numberFmt = NumberFormat.getIntegerInstance(Locale.US)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionRunCallback<OpenWellPlusAppCallback>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "WELL+ CHECK-IN",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (isTodayLogged) {
                        "Mood: ${snapshot.todayMoodLabel.ifBlank { "${snapshot.todayMoodLevel}/5" }}"
                    } else {
                        "How are you feeling?"
                    },
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            if (snapshot.streakDays > 0) {
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${snapshot.streakDays}d streak",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            WIDGET_MOOD_PRESETS.forEachIndexed { idx, preset ->
                val selected = isTodayLogged && snapshot.todayMoodLevel == preset.level
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(
                            if (selected) GlanceTheme.colors.primary
                            else GlanceTheme.colors.secondaryContainer
                        )
                        .cornerRadius(10.dp)
                        .padding(vertical = 8.dp, horizontal = 2.dp)
                        .clickable(
                            actionRunCallback<QuickMoodWidgetActionCallback>(
                                actionParametersOf(
                                    PARAM_MOOD_LEVEL to preset.level,
                                    PARAM_MOOD_LABEL to preset.fullLabel
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.shortLabel,
                        style = TextStyle(
                            color = if (selected) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
                if (idx < WIDGET_MOOD_PRESETS.lastIndex) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
            }
        }

        if (snapshot.lastStepsToday >= 0L || snapshot.lastHeartRateBpm > 0L) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            val parts = buildList {
                if (snapshot.lastStepsToday >= 0L) add("${numberFmt.format(snapshot.lastStepsToday)} steps")
                if (snapshot.lastHeartRateBpm > 0L) add("${snapshot.lastHeartRateBpm} bpm")
            }
            Text(
                text = "Health Connect · ${parts.joinToString(" • ")}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Home Screen Glance Widget #2: Next Scheduled Medication Dose with 1-Tap "Mark Taken".
 */
class WellPlusNextDoseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).read()
        provideContent {
            GlanceTheme {
                NextDoseWidgetContent(snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun NextDoseWidgetContent(snapshot: WidgetSnapshot) {
    val hasNextMed = snapshot.nextMedId.isNotBlank() && snapshot.nextMedName.isNotBlank()
    val allTaken = snapshot.activeMedsCount > 0 && snapshot.takenTodayCount >= snapshot.activeMedsCount

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionRunCallback<OpenWellPlusAppCallback>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "WELL+ MEDICATIONS",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when {
                        allTaken -> "All doses taken today"
                        hasNextMed -> snapshot.nextMedName
                        else -> "No doses scheduled"
                    },
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            if (snapshot.activeMedsCount > 0) {
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${snapshot.takenTodayCount}/${snapshot.activeMedsCount}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Text(
            text = when {
                allTaken -> "Great adherence — tap to open medication log."
                hasNextMed -> listOf(snapshot.nextMedDosage, snapshot.nextMedTimeLabel)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { "Scheduled dose" }
                else -> "Open Well+ to add prescriptions or scan a bottle."
            },
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp
            ),
            maxLines = 1
        )

        if (hasNextMed && !allTaken) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(12.dp)
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .clickable(
                        actionRunCallback<MarkNextDoseTakenCallback>(
                            actionParametersOf(
                                PARAM_MED_ID to snapshot.nextMedId,
                                PARAM_MED_NAME to snapshot.nextMedName
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mark ${snapshot.nextMedName} Taken",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

class OpenWellPlusAppCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }
}

class QuickMoodWidgetActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val level = (parameters[PARAM_MOOD_LEVEL] ?: 3).coerceIn(1, 5)
        val label = parameters[PARAM_MOOD_LABEL].orEmpty().ifBlank { "Steady" }
        val todayKey = DateKeyProvider.formatCurrentDateKey()
        val store = WidgetSnapshotStore(context)
        val existing = store.read()
        val nextStreak = if (existing.todayDateKey == todayKey && existing.todayMoodLevel in 1..5) {
            existing.streakDays
        } else {
            (existing.streakDays + 1).coerceAtLeast(1)
        }
        store.updateMood(
            dateKey = todayKey,
            moodLevel = level,
            moodLabel = label,
            streakDays = nextStreak
        )
        WellPlusQuickMoodWidget().update(context, glanceId)

        val workData = Data.Builder()
            .putInt(WidgetMoodSyncWorker.KEY_MOOD_LEVEL, level)
            .putString(WidgetMoodSyncWorker.KEY_DATE_KEY, todayKey)
            .build()
        val request = OneTimeWorkRequestBuilder<WidgetMoodSyncWorker>()
            .setInputData(workData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "wellplus_widget_mood_$todayKey",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class MarkNextDoseTakenCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val medId = parameters[PARAM_MED_ID].orEmpty().trim()
        val medName = parameters[PARAM_MED_NAME].orEmpty().trim().ifBlank { "Medication" }
        if (medId.isBlank()) return

        val store = WidgetSnapshotStore(context)
        val current = store.read()
        val newTaken = (current.takenTodayCount + 1).coerceAtMost(current.activeMedsCount.coerceAtLeast(1))
        store.updateMedicationSummary(
            nextMedId = if (newTaken >= current.activeMedsCount) "" else current.nextMedId,
            nextMedName = if (newTaken >= current.activeMedsCount) "" else current.nextMedName,
            nextMedDosage = if (newTaken >= current.activeMedsCount) "" else current.nextMedDosage,
            nextMedTimeLabel = if (newTaken >= current.activeMedsCount) "" else current.nextMedTimeLabel,
            takenTodayCount = newTaken,
            activeMedsCount = current.activeMedsCount
        )
        WellPlusNextDoseWidget().update(context, glanceId)

        MedicationActionReceiver.enqueueMarkTakenWork(
            context = context.applicationContext,
            medicationId = medId,
            medicationName = medName
        )
    }
}

/**
 * Authenticated background worker syncing a 1-tap widget mood check-in to the user's
 * named Firestore database while honoring all `firestore.rules` invariants.
 */
class WidgetMoodSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val moodLevel = inputData.getInt(KEY_MOOD_LEVEL, 3).coerceIn(1, 5).toLong()
        val dateKey = inputData.getString(KEY_DATE_KEY)
            ?.takeIf { it.length in 8..16 }
            ?: DateKeyProvider.formatCurrentDateKey()

        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (e: Exception) {
            Log.w(WIDGET_TAG, "Firebase Auth unavailable for widget mood sync", e)
            null
        }
        if (uid.isNullOrBlank()) return Result.success()

        return try {
            val databaseId = AppResourceResolver.getRequiredString(applicationContext, "firestore_database_id")
            val db = FirebaseFirestore.getInstance(databaseId)
            val repo = CheckInAndCareRepository(FirestoreSessionHelper(db, Firebase.auth))
            val res = repo.saveDailyCheckIn(
                userId = uid,
                dateKey = dateKey,
                moodLevel = moodLevel,
                energyLevel = moodLevel,
                sleepHours = 7.5,
                hydrationGlasses = 6L,
                notes = "Logged from Home Screen widget"
            )
            if (res.isSuccess) Result.success()
            else if (runAttemptCount < 2) Result.retry()
            else Result.failure()
        } catch (e: Exception) {
            Log.e(WIDGET_TAG, "Widget mood sync failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_MOOD_LEVEL = "mood_level"
        const val KEY_DATE_KEY = "date_key"
    }
}
