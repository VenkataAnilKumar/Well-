package com.example.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.notifications.R
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationScheduleSlot
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object SanaNotificationChannels {
    const val CHECK_INS = "sana_checkins"
    const val MEDICATIONS = "sana_medications"
    const val FOLLOW_UPS = "sana_followups"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channels = listOf(
            NotificationChannel(
                CHECK_INS,
                "Daily Wellness Check-Ins",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle daily reminders to log mood, sleep, and wellness status."
            },
            NotificationChannel(
                MEDICATIONS,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily intake reminders for your scheduled prescriptions and supplements."
            },
            NotificationChannel(
                FOLLOW_UPS,
                "Symptom Follow-Up Nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "48-hour check-ins to track how your symptoms are recovering."
            }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class SanaReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!SanaNotificationChannels.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }
        SanaNotificationChannels.ensureCreated(applicationContext)

        val channelId = inputData.getString(KEY_CHANNEL_ID) ?: SanaNotificationChannels.CHECK_INS
        val title = inputData.getString(KEY_TITLE) ?: "Well+ Reminder"
        val body = inputData.getString(KEY_BODY) ?: "Take a moment to check in with your health sanctuary."
        val notifId = inputData.getInt(KEY_NOTIF_ID, 1001)
        val destination = inputData.getString(KEY_DESTINATION).orEmpty()
        val consultationId = inputData.getString(KEY_CONSULTATION_ID).orEmpty()
        val medicationId = inputData.getString(KEY_MEDICATION_ID).orEmpty()
        val medicationName = inputData.getString(KEY_MEDICATION_NAME).orEmpty()

        val openIntent = Intent().setClassName(
            applicationContext.packageName,
            "com.example.MainActivity"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destination.isNotBlank()) {
                putExtra(ReminderScheduler.EXTRA_OPEN_DESTINATION, destination)
            }
            if (consultationId.isNotBlank()) {
                putExtra(ReminderScheduler.EXTRA_CONSULTATION_ID, consultationId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = applicationContext.applicationInfo.icon
            .takeIf { it != 0 } ?: android.R.drawable.ic_popup_reminder

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(
                if (channelId == SanaNotificationChannels.MEDICATIONS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (channelId == SanaNotificationChannels.MEDICATIONS && medicationId.isNotBlank()) {
            val resolvedName = medicationName.ifBlank { title }
            val markTakenIntent = MedicationActionReceiver.createMarkTakenIntent(
                context = applicationContext,
                medicationId = medicationId,
                medicationName = resolvedName,
                notifId = notifId
            )
            val markTakenPending = PendingIntent.getBroadcast(
                applicationContext,
                MedicationActionReceiver.markTakenRequestCode(notifId),
                markTakenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val snoozeIntent = MedicationActionReceiver.createSnoozeIntent(
                context = applicationContext,
                medicationId = medicationId,
                medicationName = resolvedName,
                title = title,
                body = body,
                notifId = notifId
            )
            val snoozePending = PendingIntent.getBroadcast(
                applicationContext,
                MedicationActionReceiver.snoozeRequestCode(notifId),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder
                .addAction(
                    0,
                    applicationContext.getString(R.string.notif_action_mark_taken),
                    markTakenPending
                )
                .addAction(
                    0,
                    applicationContext.getString(R.string.notif_action_snooze_30m),
                    snoozePending
                )
        }

        NotificationManagerCompat.from(applicationContext).notify(notifId, builder.build())
        return Result.success()
    }

    companion object {
        const val KEY_CHANNEL_ID = "channel_id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_NOTIF_ID = "notif_id"
        const val KEY_DESTINATION = "destination"
        const val KEY_CONSULTATION_ID = "consultation_id"
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
    }
}

object ReminderScheduler {
    const val EXTRA_OPEN_DESTINATION = "sana_open_destination"
    const val EXTRA_CONSULTATION_ID = "sana_consultation_id"
    const val DEST_MEDICATIONS = "medications"
    const val DEST_CHECK_IN = "check_in"
    const val DEST_FOLLOW_UP = "follow_up"

    private const val WORK_DAILY_CHECKIN = "sana_daily_checkin_reminder"
    private const val WORK_DAILY_MEDS_LEGACY = "sana_daily_meds_reminder"
    private const val TAG_MED_REMINDER = "sana_med_daily_reminder"
    private const val TAG_MED_SNOOZE = "sana_med_snooze_reminder"
    private const val WORK_MED_PREFIX = "sana_med_reminder_"
    private const val WORK_MED_SNOOZE_PREFIX = "sana_med_snooze_"
    private const val WORK_MED_TEST = "sana_med_test_reminder"
    private const val TAG_FOLLOWUP_PREFIX = "sana_followup_"

    fun syncDailyCheckInReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_DAILY_CHECKIN)
            return
        }
        SanaNotificationChannels.ensureCreated(context)
        val delayMs = computeInitialDelayMillis(hour, minute)
        val input = Data.Builder()
            .putString(SanaReminderWorker.KEY_CHANNEL_ID, SanaNotificationChannels.CHECK_INS)
            .putString(SanaReminderWorker.KEY_TITLE, "Time for your Daily Wellness Check-In")
            .putString(
                SanaReminderWorker.KEY_BODY,
                "Log your mood, rest, hydration, and body signals in Well+."
            )
            .putInt(SanaReminderWorker.KEY_NOTIF_ID, 2001)
            .putString(SanaReminderWorker.KEY_DESTINATION, DEST_CHECK_IN)
            .build()

        val request = PeriodicWorkRequestBuilder<SanaReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        wm.enqueueUniquePeriodicWork(
            WORK_DAILY_CHECKIN,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Reconciles individual 24-hour WorkManager reminders for every active scheduled prescription
     * (excluding AS_NEEDED medications) at each medication's exact configured time.
     */
    fun syncMedicationReminders(
        context: Context,
        enabled: Boolean,
        medications: List<MedicationItem>
    ) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelUniqueWork(WORK_DAILY_MEDS_LEGACY)
        wm.cancelAllWorkByTag(TAG_MED_REMINDER)

        val scheduledMeds = medications.filter {
            it.active && it.parsedSlot != MedicationScheduleSlot.AS_NEEDED && it.id.isNotBlank()
        }
        if (!enabled || scheduledMeds.isEmpty()) {
            return
        }

        SanaNotificationChannels.ensureCreated(context)

        scheduledMeds.forEach { med ->
            val delayMs = computeInitialDelayMillis(
                med.reminderHour.toInt(),
                med.reminderMinute.toInt()
            )
            val dosageSuffix = if (med.dosage.isNotBlank()) " (${med.dosage})" else ""
            val title = "Time for ${med.name}$dosageSuffix"
            val body = buildString {
                append("${med.parsedSlot.label} dose scheduled for ${med.formattedReminderTime()}.")
                if (med.instructions.isNotBlank()) {
                    append(" ${med.instructions}")
                } else {
                    append(" Tap Mark Taken or open Well+.")
                }
            }
            val notifId = (med.id.hashCode() and 0x7FFF) + 4000

            val input = Data.Builder()
                .putString(SanaReminderWorker.KEY_CHANNEL_ID, SanaNotificationChannels.MEDICATIONS)
                .putString(SanaReminderWorker.KEY_TITLE, title)
                .putString(SanaReminderWorker.KEY_BODY, body)
                .putInt(SanaReminderWorker.KEY_NOTIF_ID, notifId)
                .putString(SanaReminderWorker.KEY_DESTINATION, DEST_MEDICATIONS)
                .putString(SanaReminderWorker.KEY_MEDICATION_ID, med.id)
                .putString(SanaReminderWorker.KEY_MEDICATION_NAME, med.name)
                .build()

            val request = PeriodicWorkRequestBuilder<SanaReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag(TAG_MED_REMINDER)
                .build()

            wm.enqueueUniquePeriodicWork(
                "$WORK_MED_PREFIX${med.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    /**
     * Schedules a one-time snoozed medication reminder (default 30 minutes) preserving both
     * Mark Taken and Snooze 30m inline actions.
     */
    fun scheduleMedicationSnooze(
        context: Context,
        medicationId: String,
        medicationName: String,
        title: String,
        body: String,
        notifId: Int,
        delayMinutes: Long = MedicationActionReceiver.SNOOZE_MINUTES
    ) {
        SanaNotificationChannels.ensureCreated(context)
        val wm = WorkManager.getInstance(context.applicationContext)
        val input = Data.Builder()
            .putString(SanaReminderWorker.KEY_CHANNEL_ID, SanaNotificationChannels.MEDICATIONS)
            .putString(SanaReminderWorker.KEY_TITLE, title)
            .putString(SanaReminderWorker.KEY_BODY, body)
            .putInt(SanaReminderWorker.KEY_NOTIF_ID, notifId)
            .putString(SanaReminderWorker.KEY_DESTINATION, DEST_MEDICATIONS)
            .putString(SanaReminderWorker.KEY_MEDICATION_ID, medicationId)
            .putString(SanaReminderWorker.KEY_MEDICATION_NAME, medicationName)
            .build()

        val request = OneTimeWorkRequestBuilder<SanaReminderWorker>()
            .setInitialDelay(delayMinutes.coerceAtLeast(1L), TimeUnit.MINUTES)
            .setInputData(input)
            .addTag(TAG_MED_SNOOZE)
            .build()

        wm.enqueueUniqueWork(
            "$WORK_MED_SNOOZE_PREFIX$medicationId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Queues a near-immediate WorkManager notification so the user can verify
     * local medication push alerts directly on their device.
     */
    fun sendTestMedicationNotification(
        context: Context,
        sampleMedication: MedicationItem?
    ) {
        SanaNotificationChannels.ensureCreated(context)
        val wm = WorkManager.getInstance(context.applicationContext)
        val title = if (sampleMedication != null) {
            val dosage = if (sampleMedication.dosage.isNotBlank()) " (${sampleMedication.dosage})" else ""
            "Reminder Preview: ${sampleMedication.name}$dosage"
        } else {
            "Well+ Medication Reminder Ready"
        }
        val body = if (sampleMedication != null) {
            "Scheduled daily at ${sampleMedication.formattedReminderTime()} via WorkManager. Use Mark Taken or tap to log."
        } else {
            "Daily prescription notifications are active and scheduled through WorkManager."
        }

        val inputBuilder = Data.Builder()
            .putString(SanaReminderWorker.KEY_CHANNEL_ID, SanaNotificationChannels.MEDICATIONS)
            .putString(SanaReminderWorker.KEY_TITLE, title)
            .putString(SanaReminderWorker.KEY_BODY, body)
            .putInt(SanaReminderWorker.KEY_NOTIF_ID, 4999)
            .putString(SanaReminderWorker.KEY_DESTINATION, DEST_MEDICATIONS)

        if (sampleMedication != null && sampleMedication.id.isNotBlank()) {
            inputBuilder
                .putString(SanaReminderWorker.KEY_MEDICATION_ID, sampleMedication.id)
                .putString(SanaReminderWorker.KEY_MEDICATION_NAME, sampleMedication.name)
        }

        val request = OneTimeWorkRequestBuilder<SanaReminderWorker>()
            .setInitialDelay(1, TimeUnit.SECONDS)
            .setInputData(inputBuilder.build())
            .build()

        wm.enqueueUniqueWork(WORK_MED_TEST, ExistingWorkPolicy.REPLACE, request)
    }

    fun nextScheduledMedicationSummary(medications: List<MedicationItem>): String? {
        val scheduled = medications.filter {
            it.active && it.parsedSlot != MedicationScheduleSlot.AS_NEEDED
        }
        if (scheduled.isEmpty()) return null

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val sorted = scheduled.sortedBy { it.reminderHour * 60 + it.reminderMinute }
        val nextToday = sorted.firstOrNull { (it.reminderHour * 60 + it.reminderMinute) > nowMinutes }
        val chosen = nextToday ?: sorted.first()
        val dayPrefix = if (nextToday != null) "Today" else "Tomorrow"
        return String.format(
            Locale.US,
            "Next: %s • %s at %s",
            chosen.name,
            dayPrefix,
            chosen.formattedReminderTime()
        )
    }

    fun scheduleConsultationFollowUp(
        context: Context,
        consultationId: String,
        consultationTitle: String,
        delayHours: Long = 48L
    ) {
        SanaNotificationChannels.ensureCreated(context)
        val wm = WorkManager.getInstance(context.applicationContext)
        val workName = "$TAG_FOLLOWUP_PREFIX$consultationId"
        val input = Data.Builder()
            .putString(SanaReminderWorker.KEY_CHANNEL_ID, SanaNotificationChannels.FOLLOW_UPS)
            .putString(SanaReminderWorker.KEY_TITLE, "Symptom Check-In: $consultationTitle")
            .putString(
                SanaReminderWorker.KEY_BODY,
                "How are you feeling now? Update your recovery status in Well+ so your timeline stays accurate."
            )
            .putInt(SanaReminderWorker.KEY_NOTIF_ID, (consultationId.hashCode() and 0x7FFF) + 3000)
            .putString(SanaReminderWorker.KEY_DESTINATION, DEST_FOLLOW_UP)
            .putString(SanaReminderWorker.KEY_CONSULTATION_ID, consultationId)
            .build()

        val request = OneTimeWorkRequestBuilder<SanaReminderWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .setInputData(input)
            .addTag(TAG_FOLLOWUP_PREFIX)
            .build()

        wm.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAllReminders(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelUniqueWork(WORK_DAILY_CHECKIN)
        wm.cancelUniqueWork(WORK_DAILY_MEDS_LEGACY)
        wm.cancelAllWorkByTag(TAG_MED_REMINDER)
        wm.cancelAllWorkByTag(TAG_MED_SNOOZE)
        wm.cancelAllWorkByTag(TAG_FOLLOWUP_PREFIX)
    }

    private fun computeInitialDelayMillis(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }
}
