package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Pressure
import com.example.data.model.HealthConnectAvailability
import com.example.data.model.HealthConnectBloodPressureReading
import com.example.data.model.HealthConnectDailyHeartRate
import com.example.data.model.HealthConnectDailySteps
import com.example.data.model.HealthConnectHeartRateSummary
import com.example.data.model.HealthConnectSleepSummary
import com.example.data.model.HealthConnectSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.round

private const val HC_TAG = "HealthConnectManager"
private const val HC_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
private const val MAX_PAGINATION_PAGES = 5

data class HealthConnectClientInitResult(
    val availability: HealthConnectAvailability,
    val isClientInitialized: Boolean,
    val frameworkSourceLabel: String
)

data class SampleHealthWriteResult(
    val stepsWritten: Long?,
    val heartRateBpmWritten: Long?,
    val recordsInsertedCount: Int
)

/**
 * Manages the Android Jetpack [HealthConnectClient] lifecycle, read/write permission checks
 * and system permission requests, and bidirectional data synchronization for Steps,
 * Heart Rate, Sleep Sessions, and Blood Pressure.
 */
class HealthConnectManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val clientLock = Any()

    @Volatile
    private var cachedClient: HealthConnectClient? = null

    val isClientInitialized: Boolean
        get() = cachedClient != null

    /**
     * Initializes and caches the [HealthConnectClient] bound to applicationContext against
     * the Android system health framework (Android 14+) or Health Connect provider APK (Android 9–13).
     */
    fun initialize(): HealthConnectClientInitResult {
        val sourceLabel = frameworkSourceLabel()
        val availability = checkAvailability()
        if (availability != HealthConnectAvailability.AVAILABLE) {
            synchronized(clientLock) {
                cachedClient = null
            }
            Log.i(HC_TAG, "Health Connect client not initialized (status=$availability)")
            return HealthConnectClientInitResult(
                availability = availability,
                isClientInitialized = false,
                frameworkSourceLabel = sourceLabel
            )
        }

        val client = getOrInitializeClient()
        return HealthConnectClientInitResult(
            availability = availability,
            isClientInitialized = client != null,
            frameworkSourceLabel = sourceLabel
        )
    }

    fun getOrInitializeClient(): HealthConnectClient? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        cachedClient?.let { return it }
        return synchronized(clientLock) {
            cachedClient?.let { return@synchronized it }
            if (checkAvailability() != HealthConnectAvailability.AVAILABLE) {
                return@synchronized null
            }
            try {
                HealthConnectClient.getOrCreate(appContext).also { created ->
                    cachedClient = created
                    Log.i(HC_TAG, "Initialized HealthConnectClient via ${frameworkSourceLabel()}")
                }
            } catch (e: Exception) {
                Log.e(HC_TAG, "Failed to initialize HealthConnectClient", e)
                null
            }
        }
    }

    fun checkAvailability(): HealthConnectAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return HealthConnectAvailability.NOT_SUPPORTED
        }
        return try {
            when (HealthConnectClient.getSdkStatus(appContext, HC_PROVIDER_PACKAGE)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    HealthConnectAvailability.UPDATE_REQUIRED
                else -> HealthConnectAvailability.NOT_SUPPORTED
            }
        } catch (e: Exception) {
            Log.w(HC_TAG, "Unable to query Health Connect SDK status", e)
            HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    /**
     * Returns the set of Health Connect permissions currently granted to this app by the Android system.
     */
    suspend fun getGrantedPermissions(): Set<String> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext emptySet()
        val client = getOrInitializeClient() ?: return@withContext emptySet()
        try {
            client.permissionController.getGrantedPermissions()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(HC_TAG, "Failed to read granted Health Connect permissions", e)
            emptySet()
        }
    }

    /**
     * Checks whether all specified [permissions] (defaulting to all read & write permissions) are granted.
     */
    suspend fun hasAllPermissions(permissions: Set<String> = allPermissions): Boolean {
        val granted = getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    /**
     * Checks whether read and write permissions for Heart Rate and Steps are all granted.
     */
    suspend fun hasHeartRateAndStepsPermissions(): Boolean {
        return hasAllPermissions(heartRateAndStepsPermissions)
    }

    /**
     * Computes which permissions from [requested] have not yet been granted, suitable for
     * passing to the system launcher created by [createPermissionRequestContract].
     */
    suspend fun getMissingPermissions(requested: Set<String> = allPermissions): Set<String> {
        val granted = getGrantedPermissions()
        return requested - granted
    }

    /**
     * Computes any missing read/write permissions specifically for Heart Rate and Steps.
     */
    suspend fun getMissingHeartRateAndStepsPermissions(): Set<String> {
        return getMissingPermissions(heartRateAndStepsPermissions)
    }

    /**
     * Launches the Android system Health Connect permission prompt for the given [permissions].
     */
    fun requestPermissions(
        launcher: ActivityResultLauncher<Set<String>>,
        permissions: Set<String> = allPermissions
    ) {
        if (permissions.isNotEmpty()) {
            launcher.launch(permissions)
        }
    }

    /**
     * Launches the Android system Health Connect permission prompt for Heart Rate & Steps read/write access.
     */
    fun requestHeartRateAndStepsPermissions(
        launcher: ActivityResultLauncher<Set<String>>
    ) {
        launcher.launch(heartRateAndStepsPermissions)
    }

    /**
     * Reads all currently authorized Health Connect data types (Steps, Heart Rate, Sleep, Blood Pressure).
     */
    suspend fun syncHealthData(): Result<HealthConnectSnapshot> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext Result.failure(
                IllegalStateException("Health Connect requires Android 8.0 or higher.")
            )
        }
        if (checkAvailability() != HealthConnectAvailability.AVAILABLE) {
            return@withContext Result.failure(
                IllegalStateException("Health Connect is not currently available on this device.")
            )
        }

        try {
            val client = getOrInitializeClient()
                ?: return@withContext Result.failure(
                    IllegalStateException("Could not initialize the Health Connect client on this device.")
                )
            val granted = client.permissionController.getGrantedPermissions()
            if (granted.intersect(readPermissions).isEmpty()) {
                return@withContext Result.success(
                    HealthConnectSnapshot(
                        syncedAtMillis = System.currentTimeMillis(),
                        grantedPermissions = granted
                    )
                )
            }
            val snapshot = readAuthorizedMetricsApi26(client, granted)
            Result.success(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Log.w(HC_TAG, "Health Connect permission denied during sync", e)
            Result.failure(IllegalStateException("Health Connect permissions were revoked. Please re-authorize."))
        } catch (e: RemoteException) {
            Log.e(HC_TAG, "Health Connect IPC failure", e)
            Result.failure(IllegalStateException("Could not reach the Health Connect service. Please try again."))
        } catch (e: IOException) {
            Log.e(HC_TAG, "Health Connect I/O error", e)
            Result.failure(IllegalStateException("Health Connect storage could not be read right now."))
        } catch (e: Exception) {
            Log.e(HC_TAG, "Health Connect sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Writes a manual Steps record to Android Health Connect when WRITE_STEPS permission is granted.
     */
    suspend fun writeSteps(
        count: Long,
        durationMinutes: Long = 15L,
        endTimeMillis: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (count <= 0L) {
            return@withContext Result.failure(IllegalArgumentException("Step count must be greater than zero."))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext Result.failure(IllegalStateException("Health Connect requires Android 8.0 or higher."))
        }
        writeStepsApi26(count = count, durationMinutes = durationMinutes.coerceIn(1L, 720L), endTimeMillis = endTimeMillis)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun writeStepsApi26(
        count: Long,
        durationMinutes: Long,
        endTimeMillis: Long
    ): Result<Unit> {
        val client = getOrInitializeClient()
            ?: return Result.failure(IllegalStateException("Health Connect client is not initialized."))
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (PERMISSION_WRITE_STEPS !in granted) {
                return Result.failure(SecurityException("Write Steps permission is not granted."))
            }
            val end = Instant.ofEpochMilli(endTimeMillis)
            val start = end.minus(Duration.ofMinutes(durationMinutes))
            val zone = ZoneId.systemDefault()
            val startOffset = zone.rules.getOffset(start)
            val endOffset = zone.rules.getOffset(end)
            val record = StepsRecord(
                count = count,
                startTime = start,
                endTime = end,
                startZoneOffset = startOffset,
                endZoneOffset = endOffset,
                metadata = Metadata.manualEntry()
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(HC_TAG, "Failed to write steps to Health Connect", e)
            Result.failure(e)
        }
    }

    /**
     * Writes a batch of sample Steps and Heart Rate records to the Android Health Connect store
     * for verifying end-to-end synchronization. Only inserts record types whose write permission
     * is currently granted.
     */
    suspend fun writeSampleHealthData(
        sampleStepsCount: Long = 1_250L,
        sampleHeartRateBpm: Long = 72L
    ): Result<SampleHealthWriteResult> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext Result.failure(
                IllegalStateException("Health Connect requires Android 8.0 or higher.")
            )
        }
        writeSampleHealthDataApi26(
            sampleStepsCount = sampleStepsCount.coerceIn(1L, 100_000L),
            sampleHeartRateBpm = sampleHeartRateBpm.coerceIn(35L, 220L)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun writeSampleHealthDataApi26(
        sampleStepsCount: Long,
        sampleHeartRateBpm: Long
    ): Result<SampleHealthWriteResult> {
        val client = getOrInitializeClient()
            ?: return Result.failure(IllegalStateException("Health Connect client is not initialized."))
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val canWriteSteps = PERMISSION_WRITE_STEPS in granted
            val canWriteHeartRate = PERMISSION_WRITE_HEART_RATE in granted
            if (!canWriteSteps && !canWriteHeartRate) {
                return Result.failure(
                    SecurityException("Grant Write Steps or Write Heart Rate permission to write sample health data.")
                )
            }

            val now = Instant.now()
            val start = now.minus(Duration.ofMinutes(15))
            val zone = ZoneId.systemDefault()
            val startOffset = zone.rules.getOffset(start)
            val endOffset = zone.rules.getOffset(now)

            val recordsToInsert = mutableListOf<androidx.health.connect.client.records.Record>()
            var writtenSteps: Long? = null
            var writtenBpm: Long? = null

            if (canWriteSteps) {
                recordsToInsert += StepsRecord(
                    count = sampleStepsCount,
                    startTime = start,
                    endTime = now,
                    startZoneOffset = startOffset,
                    endZoneOffset = endOffset,
                    metadata = Metadata.manualEntry()
                )
                writtenSteps = sampleStepsCount
            }

            if (canWriteHeartRate) {
                val samples = listOf(
                    HeartRateRecord.Sample(time = start, beatsPerMinute = (sampleHeartRateBpm - 2L).coerceAtLeast(40L)),
                    HeartRateRecord.Sample(time = start.plus(Duration.ofMinutes(7)), beatsPerMinute = sampleHeartRateBpm + 3L),
                    HeartRateRecord.Sample(time = now.minusSeconds(5), beatsPerMinute = sampleHeartRateBpm)
                )
                recordsToInsert += HeartRateRecord(
                    startTime = start,
                    startZoneOffset = startOffset,
                    endTime = now,
                    endZoneOffset = endOffset,
                    samples = samples,
                    metadata = Metadata.manualEntry()
                )
                writtenBpm = sampleHeartRateBpm
            }

            client.insertRecords(recordsToInsert)
            Result.success(
                SampleHealthWriteResult(
                    stepsWritten = writtenSteps,
                    heartRateBpmWritten = writtenBpm,
                    recordsInsertedCount = recordsToInsert.size
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(HC_TAG, "Failed to write sample health data to Health Connect", e)
            Result.failure(e)
        }
    }

    /**
     * Writes a Heart Rate reading (BPM) to Android Health Connect when WRITE_HEART_RATE permission is granted.
     */
    suspend fun writeHeartRate(
        bpm: Long,
        recordedAtMillis: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (bpm !in 25L..250L) {
            return@withContext Result.failure(IllegalArgumentException("Heart rate must be between 25 and 250 bpm."))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext Result.failure(IllegalStateException("Health Connect requires Android 8.0 or higher."))
        }
        writeHeartRateApi26(bpm = bpm, recordedAtMillis = recordedAtMillis)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun writeHeartRateApi26(
        bpm: Long,
        recordedAtMillis: Long
    ): Result<Unit> {
        val client = getOrInitializeClient()
            ?: return Result.failure(IllegalStateException("Health Connect client is not initialized."))
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (PERMISSION_WRITE_HEART_RATE !in granted) {
                return Result.failure(SecurityException("Write Heart Rate permission is not granted."))
            }
            val sampleTime = Instant.ofEpochMilli(recordedAtMillis)
            val endTime = sampleTime.plusSeconds(1)
            val offset = ZoneId.systemDefault().rules.getOffset(sampleTime)
            val record = HeartRateRecord(
                startTime = sampleTime,
                startZoneOffset = offset,
                endTime = endTime,
                endZoneOffset = offset,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = sampleTime,
                        beatsPerMinute = bpm
                    )
                ),
                metadata = Metadata.manualEntry()
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(HC_TAG, "Failed to write heart rate to Health Connect", e)
            Result.failure(e)
        }
    }

    /**
     * Writes a Blood Pressure reading (mmHg) to Android Health Connect when WRITE_BLOOD_PRESSURE is granted.
     */
    suspend fun writeBloodPressure(
        systolicMmHg: Double,
        diastolicMmHg: Double,
        recordedAtMillis: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (systolicMmHg !in 50.0..260.0 || diastolicMmHg !in 30.0..180.0) {
            return@withContext Result.failure(IllegalArgumentException("Blood pressure values are out of physiological bounds."))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext Result.failure(IllegalStateException("Health Connect requires Android 8.0 or higher."))
        }
        writeBloodPressureApi26(systolicMmHg, diastolicMmHg, recordedAtMillis)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun writeBloodPressureApi26(
        systolicMmHg: Double,
        diastolicMmHg: Double,
        recordedAtMillis: Long
    ): Result<Unit> {
        val client = getOrInitializeClient()
            ?: return Result.failure(IllegalStateException("Health Connect client is not initialized."))
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (PERMISSION_WRITE_BLOOD_PRESSURE !in granted) {
                return Result.failure(SecurityException("Write Blood Pressure permission is not granted."))
            }
            val time = Instant.ofEpochMilli(recordedAtMillis)
            val offset = ZoneId.systemDefault().rules.getOffset(time)
            val record = BloodPressureRecord(
                time = time,
                zoneOffset = offset,
                systolic = Pressure.millimetersOfMercury(systolicMmHg),
                diastolic = Pressure.millimetersOfMercury(diastolicMmHg),
                bodyPosition = BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
                metadata = Metadata.manualEntry()
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(HC_TAG, "Failed to write blood pressure to Health Connect", e)
            Result.failure(e)
        }
    }

    /**
     * Writes a Sleep session to Android Health Connect when WRITE_SLEEP is granted.
     */
    suspend fun writeSleepSession(
        durationHours: Double,
        endTimeMillis: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (durationHours !in 0.25..24.0) {
            return@withContext Result.failure(IllegalArgumentException("Sleep duration must be between 0.25 and 24 hours."))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext Result.failure(IllegalStateException("Health Connect requires Android 8.0 or higher."))
        }
        writeSleepSessionApi26(durationHours, endTimeMillis)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun writeSleepSessionApi26(
        durationHours: Double,
        endTimeMillis: Long
    ): Result<Unit> {
        val client = getOrInitializeClient()
            ?: return Result.failure(IllegalStateException("Health Connect client is not initialized."))
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (PERMISSION_WRITE_SLEEP !in granted) {
                return Result.failure(SecurityException("Write Sleep permission is not granted."))
            }
            val end = Instant.ofEpochMilli(endTimeMillis)
            val minutes = (durationHours * 60.0).toLong().coerceAtLeast(15L)
            val start = end.minus(Duration.ofMinutes(minutes))
            val zone = ZoneId.systemDefault()
            val record = SleepSessionRecord(
                startTime = start,
                startZoneOffset = zone.rules.getOffset(start),
                endTime = end,
                endZoneOffset = zone.rules.getOffset(end),
                title = "Well+ Logged Sleep",
                metadata = Metadata.manualEntry()
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(HC_TAG, "Failed to write sleep session to Health Connect", e)
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun readAuthorizedMetricsApi26(
        client: HealthConnectClient,
        granted: Set<String>
    ): HealthConnectSnapshot {
        val warnings = mutableListOf<String>()
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zone)

        var stepsToday: Long? = null
        var dailySteps = emptyList<HealthConnectDailySteps>()
        if (PERMISSION_READ_STEPS in granted) {
            try {
                val (todayTotal, sevenDayList) = readSteps7Days(client, today, now, zone)
                stepsToday = todayTotal
                dailySteps = sevenDayList
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(HC_TAG, "Steps read failed", e)
                warnings += "Could not read Steps from Health Connect."
            }
        }

        var heartRateSummary: HealthConnectHeartRateSummary? = null
        if (PERMISSION_READ_HEART_RATE in granted) {
            try {
                heartRateSummary = readHeartRate7Days(client, today, now, zone)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(HC_TAG, "Heart rate read failed", e)
                warnings += "Could not read Heart Rate from Health Connect."
            }
        }

        var sleepSummary: HealthConnectSleepSummary? = null
        if (PERMISSION_READ_SLEEP in granted) {
            try {
                sleepSummary = readSleep7Days(client, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(HC_TAG, "Sleep read failed", e)
                warnings += "Could not read Sleep sessions from Health Connect."
            }
        }

        var recentBp = emptyList<HealthConnectBloodPressureReading>()
        if (PERMISSION_READ_BLOOD_PRESSURE in granted) {
            try {
                recentBp = readBloodPressure30Days(client, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(HC_TAG, "Blood pressure read failed", e)
                warnings += "Could not read Blood Pressure from Health Connect."
            }
        }

        return HealthConnectSnapshot(
            syncedAtMillis = System.currentTimeMillis(),
            grantedPermissions = granted,
            stepsToday = stepsToday,
            dailySteps7Days = dailySteps,
            heartRate = heartRateSummary,
            sleep = sleepSummary,
            latestBloodPressure = recentBp.firstOrNull(),
            recentBloodPressures = recentBp,
            partialWarnings = warnings
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun readSteps7Days(
        client: HealthConnectClient,
        today: LocalDate,
        now: Instant,
        zone: ZoneId
    ): Pair<Long, List<HealthConnectDailySteps>> {
        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.US)
        val keyFmt = DateTimeFormatter.ISO_LOCAL_DATE
        val dailyList = mutableListOf<HealthConnectDailySteps>()

        for (offset in 6 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val startInstant = date.atStartOfDay(zone).toInstant()
            val endInstant = if (offset == 0) {
                now
            } else {
                date.plusDays(1).atStartOfDay(zone).toInstant()
            }
            val total = if (endInstant.isAfter(startInstant)) {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                    )
                )
                agg[StepsRecord.COUNT_TOTAL] ?: 0L
            } else {
                0L
            }
            dailyList += HealthConnectDailySteps(
                dayLabel = date.format(dayFmt),
                dateKey = date.format(keyFmt),
                steps = total
            )
        }

        val todaySteps = dailyList.lastOrNull()?.steps ?: 0L
        return todaySteps to dailyList
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun readHeartRate7Days(
        client: HealthConnectClient,
        today: LocalDate,
        now: Instant,
        zone: ZoneId
    ): HealthConnectHeartRateSummary? {
        val start = today.minusDays(6).atStartOfDay(zone).toInstant()
        val agg = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    HeartRateRecord.BPM_AVG,
                    HeartRateRecord.BPM_MIN,
                    HeartRateRecord.BPM_MAX
                ),
                timeRangeFilter = TimeRangeFilter.between(start, now)
            )
        )

        var latestBpm: Long? = null
        var latestTimeMillis = 0L
        var sampleCount = 0
        val dailyBuckets = mutableMapOf<LocalDate, MutableList<Long>>()
        var pageToken: String? = null
        var pages = 0

        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now),
                    pageToken = pageToken
                )
            )
            response.records.forEach { record ->
                record.samples.forEach { sample ->
                    sampleCount++
                    val millis = sample.time.toEpochMilli()
                    if (millis >= latestTimeMillis) {
                        latestTimeMillis = millis
                        latestBpm = sample.beatsPerMinute
                    }
                    val sampleDate = sample.time.atZone(zone).toLocalDate()
                    dailyBuckets.getOrPut(sampleDate) { mutableListOf() }.add(sample.beatsPerMinute)
                }
            }
            pageToken = response.pageToken
            pages++
        } while (!pageToken.isNullOrEmpty() && pages < MAX_PAGINATION_PAGES)

        val resolvedLatest = latestBpm ?: agg[HeartRateRecord.BPM_AVG] ?: return null
        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.US)
        val keyFmt = DateTimeFormatter.ISO_LOCAL_DATE
        val dailyTrend = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val bpms = dailyBuckets[date].orEmpty()
            HealthConnectDailyHeartRate(
                dayLabel = date.format(dayFmt),
                dateKey = date.format(keyFmt),
                avgBpm = if (bpms.isNotEmpty()) round(bpms.average()).toLong() else null,
                minBpm = bpms.minOrNull(),
                maxBpm = bpms.maxOrNull(),
                sampleCount = bpms.size
            )
        }

        return HealthConnectHeartRateSummary(
            latestBpm = resolvedLatest,
            latestTimestampMillis = if (latestTimeMillis > 0L) latestTimeMillis else now.toEpochMilli(),
            avgBpm = agg[HeartRateRecord.BPM_AVG],
            minBpm = agg[HeartRateRecord.BPM_MIN],
            maxBpm = agg[HeartRateRecord.BPM_MAX],
            sampleCount = sampleCount,
            dailyTrend7Days = dailyTrend
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun readSleep7Days(
        client: HealthConnectClient,
        now: Instant
    ): HealthConnectSleepSummary? {
        val start = now.minus(Duration.ofDays(7))
        val sessions = mutableListOf<SleepSessionRecord>()
        var pageToken: String? = null
        var pages = 0

        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now),
                    pageToken = pageToken
                )
            )
            sessions += response.records
            pageToken = response.pageToken
            pages++
        } while (!pageToken.isNullOrEmpty() && pages < MAX_PAGINATION_PAGES)

        if (sessions.isEmpty()) return null

        val sorted = sessions.sortedByDescending { it.endTime }
        val latest = sorted.first()
        val latestHours = round1(Duration.between(latest.startTime, latest.endTime).toMinutes() / 60.0)
        val totalMinutesAll = sorted.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0L)
        }
        val avgHours = round1((totalMinutesAll.toDouble() / sorted.size) / 60.0)

        var deepMin = 0L
        var remMin = 0L
        var lightMin = 0L
        var awakeMin = 0L
        latest.stages.forEach { stage ->
            val mins = Duration.between(stage.startTime, stage.endTime).toMinutes().coerceAtLeast(0L)
            when (stage.stage) {
                SleepSessionRecord.STAGE_TYPE_DEEP -> deepMin += mins
                SleepSessionRecord.STAGE_TYPE_REM -> remMin += mins
                SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_SLEEPING -> lightMin += mins
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awakeMin += mins
            }
        }

        return HealthConnectSleepSummary(
            lastNightHours = latestHours,
            average7DayHours = avgHours,
            sessionCount = sorted.size,
            deepMinutes = deepMin,
            remMinutes = remMin,
            lightMinutes = lightMin,
            awakeMinutes = awakeMin,
            lastSessionEndMillis = latest.endTime.toEpochMilli()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun readBloodPressure30Days(
        client: HealthConnectClient,
        now: Instant
    ): List<HealthConnectBloodPressureReading> {
        val start = now.minus(Duration.ofDays(30))
        val records = mutableListOf<BloodPressureRecord>()
        var pageToken: String? = null
        var pages = 0

        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now),
                    pageToken = pageToken
                )
            )
            records += response.records
            pageToken = response.pageToken
            pages++
        } while (!pageToken.isNullOrEmpty() && pages < MAX_PAGINATION_PAGES)

        return records
            .sortedByDescending { it.time }
            .take(15)
            .map { rec ->
                HealthConnectBloodPressureReading(
                    systolicMmHg = round1(rec.systolic.inMillimetersOfMercury),
                    diastolicMmHg = round1(rec.diastolic.inMillimetersOfMercury),
                    timestampMillis = rec.time.toEpochMilli()
                )
            }
    }

    private fun round1(value: Double): Double = round(value * 10.0) / 10.0

    companion object {
        @Volatile
        private var instance: HealthConnectManager? = null

        fun getInstance(context: Context): HealthConnectManager {
            return instance ?: synchronized(this) {
                instance ?: HealthConnectManager(context.applicationContext).also { instance = it }
            }
        }

        // Read permissions
        val PERMISSION_READ_STEPS: String =
            HealthPermission.getReadPermission(StepsRecord::class)
        val PERMISSION_READ_HEART_RATE: String =
            HealthPermission.getReadPermission(HeartRateRecord::class)
        val PERMISSION_READ_SLEEP: String =
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        val PERMISSION_READ_BLOOD_PRESSURE: String =
            HealthPermission.getReadPermission(BloodPressureRecord::class)

        // Write permissions
        val PERMISSION_WRITE_STEPS: String =
            HealthPermission.getWritePermission(StepsRecord::class)
        val PERMISSION_WRITE_HEART_RATE: String =
            HealthPermission.getWritePermission(HeartRateRecord::class)
        val PERMISSION_WRITE_SLEEP: String =
            HealthPermission.getWritePermission(SleepSessionRecord::class)
        val PERMISSION_WRITE_BLOOD_PRESSURE: String =
            HealthPermission.getWritePermission(BloodPressureRecord::class)

        val readPermissions: Set<String> = setOf(
            PERMISSION_READ_STEPS,
            PERMISSION_READ_HEART_RATE,
            PERMISSION_READ_SLEEP,
            PERMISSION_READ_BLOOD_PRESSURE
        )

        val writePermissions: Set<String> = setOf(
            PERMISSION_WRITE_STEPS,
            PERMISSION_WRITE_HEART_RATE,
            PERMISSION_WRITE_SLEEP,
            PERMISSION_WRITE_BLOOD_PRESSURE
        )

        val heartRateAndStepsPermissions: Set<String> = setOf(
            PERMISSION_READ_STEPS,
            PERMISSION_WRITE_STEPS,
            PERMISSION_READ_HEART_RATE,
            PERMISSION_WRITE_HEART_RATE
        )

        val allPermissions: Set<String> = readPermissions + writePermissions

        /** Backwards-compatible alias for all requested read & write permissions. */
        val requiredPermissions: Set<String> = allPermissions

        /**
         * Creates the official Android system ActivityResultContract used to prompt
         * the user to grant or deny Health Connect read/write permissions.
         */
        fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> {
            return PermissionController.createRequestPermissionResultContract()
        }

        fun frameworkSourceLabel(): String = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                "Android System Health Framework"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                "Health Connect Provider App"
            else ->
                "Android 8.0+ Required"
        }

        fun buildInstallOrUpdateIntent(context: Context): Intent {
            val uriString =
                "market://details?id=$HC_PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"
            return Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = Uri.parse(uriString)
                putExtra("overlay", true)
                putExtra("callerId", context.packageName)
            }
        }

        fun buildPlayStoreWebFallbackIntent(): Intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$HC_PROVIDER_PACKAGE")
        )

        fun buildHealthConnectSettingsIntent(): Intent {
            return Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        }
    }
}
