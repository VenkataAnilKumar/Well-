package com.example.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.model.MedicationLogStatus
import com.example.data.repository.FirestoreSessionHelper
import com.example.data.repository.MedicationRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val MED_ACTION_TAG = "MedNotificationAction"

/**
 * Non-exported receiver handling inline **Mark Taken** and **Snooze 30m** medication reminder
 * actions directly from the notification shade without forcing a full Activity launch.
 */
class MedicationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID).orEmpty().trim()
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME).orEmpty().trim()

        if (notifId > 0) {
            NotificationManagerCompat.from(context.applicationContext).cancel(notifId)
        }

        when (action) {
            ACTION_MARK_TAKEN -> {
                if (medicationId.isBlank()) return
                enqueueMarkTakenWork(
                    context = context.applicationContext,
                    medicationId = medicationId,
                    medicationName = medicationName.ifBlank { "Medication" }
                )
            }

            ACTION_SNOOZE_30M -> {
                if (medicationId.isBlank()) return
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                    .ifBlank { "Snoozed Reminder: ${medicationName.ifBlank { "Medication" }}" }
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                    .ifBlank { "Time to take your snoozed medication dose." }
                ReminderScheduler.scheduleMedicationSnooze(
                    context = context.applicationContext,
                    medicationId = medicationId,
                    medicationName = medicationName.ifBlank { "Medication" },
                    title = title,
                    body = body,
                    notifId = if (notifId > 0) notifId else (medicationId.hashCode() and 0x7FFF) + 4000,
                    delayMinutes = SNOOZE_MINUTES
                )
            }
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.action.MEDICATION_MARK_TAKEN"
        const val ACTION_SNOOZE_30M = "com.example.action.MEDICATION_SNOOZE_30M"

        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"

        const val SNOOZE_MINUTES = 30L
        private const val WORK_MARK_TAKEN_PREFIX = "sana_med_mark_taken_"

        fun markTakenRequestCode(notifId: Int): Int = (notifId * 3) + 1
        fun snoozeRequestCode(notifId: Int): Int = (notifId * 3) + 2

        fun createMarkTakenIntent(
            context: Context,
            medicationId: String,
            medicationName: String,
            notifId: Int
        ): Intent = Intent(context.applicationContext, MedicationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }

        fun createSnoozeIntent(
            context: Context,
            medicationId: String,
            medicationName: String,
            title: String,
            body: String,
            notifId: Int
        ): Intent = Intent(context.applicationContext, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_30M
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }

        fun enqueueMarkTakenWork(
            context: Context,
            medicationId: String,
            medicationName: String
        ) {
            val input = Data.Builder()
                .putString(MedicationDoseLogWorker.KEY_MEDICATION_ID, medicationId)
                .putString(MedicationDoseLogWorker.KEY_MEDICATION_NAME, medicationName)
                .build()

            val request = OneTimeWorkRequestBuilder<MedicationDoseLogWorker>()
                .setInputData(input)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "$WORK_MARK_TAKEN_PREFIX$medicationId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

/**
 * Authenticated background worker writing a `TAKEN` adherence entry to the named Firestore
 * database while computing `todayDateKey` dynamically at execution time and preserving `createdAt`
 * on updates to stay 100% compliant with `firestore.rules`.
 */
class MedicationDoseLogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getString(KEY_MEDICATION_ID).orEmpty().trim()
        val medicationName = inputData.getString(KEY_MEDICATION_NAME).orEmpty().trim()
            .ifBlank { "Medication" }
        if (medicationId.isBlank()) return Result.success()

        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (e: Exception) {
            Log.w(MED_ACTION_TAG, "Firebase Auth unavailable for inline dose log", e)
            null
        }
        if (uid.isNullOrBlank()) {
            Log.i(MED_ACTION_TAG, "Skipping background dose log because no user is signed in.")
            return Result.success()
        }

        val todayDateKey = DateKeyProvider.formatCurrentDateKey()
        return try {
            val databaseId = AppResourceResolver.getRequiredString(applicationContext, "firestore_database_id")
            val db = FirebaseFirestore.getInstance(databaseId)
            val repo = MedicationRepository(FirestoreSessionHelper(db, Firebase.auth))
            val result = repo.logMedicationDoseById(
                userId = uid,
                dateKey = todayDateKey,
                medicationId = medicationId,
                medicationName = medicationName,
                status = MedicationLogStatus.TAKEN
            )
            if (result.isSuccess) {
                Result.success()
            } else if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(MED_ACTION_TAG, "Background dose log failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
    }
}
