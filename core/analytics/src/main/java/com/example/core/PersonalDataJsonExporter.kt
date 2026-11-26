package com.example.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.BodyMeasurement
import com.example.data.model.CareTask
import com.example.data.model.Consultation
import com.example.data.model.CycleLog
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MentalAssessment
import com.example.data.model.VitalReading
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GDPR / CCPA personal data portability exporter.
 * Serializes all authenticated user health records into a readable JSON bundle stored in
 * the app's private cache/exports directory and shared via FileProvider.
 */
object PersonalDataJsonExporter {

    fun exportAndShareJson(
        context: Context,
        accountEmail: String,
        profile: HealthProfile?,
        consultations: List<Consultation>,
        checkIns: List<DailyCheckIn>,
        careTasks: List<CareTask>,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        vitals: List<VitalReading>,
        mentalAssessments: List<MentalAssessment>,
        bodyMeasurements: List<BodyMeasurement>,
        cycleLogs: List<CycleLog>,
        labBiomarkers: List<LabBiomarker> = emptyList()
    ): Result<File> {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val root = JSONObject().apply {
                put("exportVersion", "1.0")
                put("application", "Well+")
                put("exportedAtUtc", isoFormat.format(Date()))
                put("accountEmail", accountEmail)
                put("medicalDisclaimer", "Informational personal health records exported from Well+. Not a clinical medical record.")

                put("profile", JSONObject().apply {
                    if (profile != null) {
                        put("displayName", profile.displayName)
                        put("age", profile.age)
                        put("ageConfirmed18Plus", profile.ageConfirmed18Plus)
                        put("biologicalSex", profile.biologicalSex)
                        put("bloodType", profile.bloodType)
                        put("allergies", JSONArray(profile.allergies))
                        put("medications", JSONArray(profile.medications))
                        put("conditions", JSONArray(profile.conditions))
                        put("wellnessGoals", JSONArray(profile.wellnessGoals))
                        put("emergencyContactName", profile.emergencyContactName)
                        put("emergencyContactPhone", profile.emergencyContactPhone)
                        put("preferredHealthRegion", profile.preferredHealthRegion)
                        put("preferredEmergencyCountry", profile.preferredEmergencyCountry)
                        put("coverageProvider", profile.coverageProvider)
                        put("coverageMemberId", profile.coverageMemberId)
                        put("primaryDoctorName", profile.primaryDoctorName)
                        put("preferredPharmacy", profile.preferredPharmacy)
                    }
                })

                put("consultations", JSONArray().apply {
                    consultations.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("type", c.consultationType)
                            put("title", c.title)
                            put("bodyArea", c.bodyArea)
                            put("symptomSummary", c.symptomSummary)
                            put("severity", c.severity)
                            put("duration", c.duration)
                            put("urgencyLevel", c.urgencyLevel)
                            put("aiSummary", c.aiSummary)
                            put("followUpStatus", c.followUpStatus)
                            put("followUpNote", c.followUpNote)
                            put("createdAt", c.createdAt?.toDate()?.let { isoFormat.format(it) } ?: "")
                        })
                    }
                })

                put("dailyCheckIns", JSONArray().apply {
                    checkIns.forEach { ci ->
                        put(JSONObject().apply {
                            put("dateKey", ci.dateKey)
                            put("moodLevel", ci.moodLevel)
                            put("energyLevel", ci.energyLevel)
                            put("sleepHours", ci.sleepHours)
                            put("hydrationGlasses", ci.hydrationGlasses)
                            put("activityLevel", ci.parsedActivityLevel.name)
                            put("activityMinutes", ci.activityMinutes)
                            put("notes", ci.notes)
                        })
                    }
                })

                put("careTasks", JSONArray().apply {
                    careTasks.forEach { task ->
                        put(JSONObject().apply {
                            put("title", task.title)
                            put("category", task.category)
                            put("completed", task.completed)
                        })
                    }
                })

                put("medications", JSONArray().apply {
                    medications.forEach { med ->
                        put(JSONObject().apply {
                            put("name", med.name)
                            put("dosage", med.dosage)
                            put("scheduleSlot", med.scheduleSlot)
                            put("instructions", med.instructions)
                            put("active", med.active)
                        })
                    }
                })

                put("medicationLogs", JSONArray().apply {
                    medicationLogs.forEach { log ->
                        put(JSONObject().apply {
                            put("dateKey", log.dateKey)
                            put("medicationName", log.medicationName)
                            put("status", log.status)
                        })
                    }
                })

                put("vitals", JSONArray().apply {
                    vitals.forEach { v ->
                        put(JSONObject().apply {
                            put("vitalType", v.vitalType)
                            put("primaryValue", v.primaryValue)
                            if (v.secondaryValue != null) put("secondaryValue", v.secondaryValue)
                            put("unit", v.unit)
                            put("note", v.note)
                            put("recordedAt", v.recordedAt?.toDate()?.let { isoFormat.format(it) } ?: "")
                        })
                    }
                })

                put("mentalWellbeingScreenings", JSONArray().apply {
                    mentalAssessments.forEach { m ->
                        put(JSONObject().apply {
                            put("assessmentType", m.assessmentType)
                            put("totalScore", m.totalScore)
                            put("maxScore", m.maxScore)
                            put("severityBand", m.severityBand)
                            put("crisisFlagged", m.crisisFlagged)
                            put("recordedAt", m.recordedAt?.toDate()?.let { isoFormat.format(it) } ?: "")
                        })
                    }
                })

                put("bodyMeasurements", JSONArray().apply {
                    bodyMeasurements.forEach { b ->
                        put(JSONObject().apply {
                            put("weightKg", b.weightKg)
                            put("heightCm", b.heightCm)
                            put("bmi", b.bmi)
                            put("waistCm", b.waistCm)
                            put("note", b.note)
                            put("recordedAt", b.recordedAt?.toDate()?.let { isoFormat.format(it) } ?: "")
                        })
                    }
                })

                put("cycleLogs", JSONArray().apply {
                    cycleLogs.forEach { cl ->
                        put(JSONObject().apply {
                            put("dateKey", cl.dateKey)
                            put("flowLevel", cl.flowLevel)
                            put("symptoms", JSONArray(cl.symptoms))
                            put("moodTag", cl.moodTag)
                            put("notes", cl.notes)
                        })
                    }
                })

                put("labBiomarkers", JSONArray().apply {
                    labBiomarkers.forEach { lb ->
                        put(JSONObject().apply {
                            put("markerName", lb.markerName)
                            put("category", lb.category)
                            put("value", lb.value)
                            put("unit", lb.unit)
                            put("referenceRangeText", lb.referenceRangeText)
                            put("status", lb.status)
                            put("panelDateKey", lb.panelDateKey)
                            put("labNote", lb.labNote)
                        })
                    }
                })
            }

            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(exportsDir, "wellplus_my_data_$dateStamp.json")
            outFile.writeText(root.toString(2), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Well+ Personal Health Data Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Export Well+ Personal Data (JSON)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
