package com.example.core

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.model.HealthConnectAvailability
import java.util.concurrent.TimeUnit

private const val HC_BG_TAG = "HealthConnectBgSync"
private const val UNIQUE_HC_SYNC_WORK = "wellplus_periodic_health_connect_sync"

/**
 * Play-Policy-compliant periodic WorkManager sync for Android Health Connect.
 * Runs only when enabled by the user and read permissions are granted; updates the
 * local widget summary store and triggers a Glance widget refresh without requiring
 * foreground services or broad background permissions.
 */
class HealthConnectBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val manager = HealthConnectManager.getInstance(applicationContext)
        val init = manager.initialize()
        if (init.availability != HealthConnectAvailability.AVAILABLE || !init.isClientInitialized) {
            return Result.success()
        }
        val granted = manager.getGrantedPermissions()
        if (granted.intersect(HealthConnectManager.readPermissions).isEmpty()) {
            return Result.success()
        }

        return manager.syncHealthData().fold(
            onSuccess = { snapshot ->
                WidgetSnapshotStore(applicationContext).updateHealthConnectSummary(
                    stepsToday = snapshot.stepsToday,
                    heartRateBpm = snapshot.heartRate?.latestBpm,
                    syncedAtMillis = snapshot.syncedAtMillis
                )
                WidgetRefreshBridge.notifyDataChanged(applicationContext)
                Log.i(HC_BG_TAG, "Background Health Connect sync completed (steps=${snapshot.stepsToday})")
                Result.success()
            },
            onFailure = { err ->
                // SecurityException can occur if Health Connect restricts background reads without background permission;
                // do not spin retries aggressively.
                Log.w(HC_BG_TAG, "Background Health Connect sync skipped/failed: ${err.message}")
                Result.success()
            }
        )
    }
}

object HealthConnectSyncScheduler {
    fun syncPeriodicSchedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_HC_SYNC_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<HealthConnectBackgroundSyncWorker>(
            6L, TimeUnit.HOURS,
            30L, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_HC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
