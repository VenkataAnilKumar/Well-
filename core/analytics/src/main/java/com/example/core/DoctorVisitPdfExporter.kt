package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BiologicalSex
import com.example.data.model.BloodType
import com.example.data.model.Consultation
import com.example.data.model.DailyCheckIn
import com.example.data.model.HealthProfile
import com.example.data.model.LabBiomarker
import com.example.data.model.MedicationItem
import com.example.data.model.MedicationLog
import com.example.data.model.MedicationLogStatus
import com.example.data.model.MentalAssessment
import com.example.data.model.MentalAssessmentType
import com.example.data.model.SoapPreVisitBrief
import com.example.data.model.VitalReading
import com.example.data.model.VitalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DoctorExportOptions(
    val daysWindow: Int = 30,
    val includeProfile: Boolean = true,
    val includeMedications: Boolean = true,
    val includeVitals: Boolean = true,
    val includeLabBiomarkers: Boolean = true,
    val includeConsultations: Boolean = true,
    val includeWellnessSummary: Boolean = true,
    val includeMentalWellbeing: Boolean = true,
    val includeSoapAiBrief: Boolean = true
)

object DoctorVisitPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 width in PostScript points
    private const val PAGE_HEIGHT = 842 // A4 height in PostScript points
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    suspend fun generateAndCreateShareIntent(
        context: Context,
        accountEmail: String,
        profile: HealthProfile?,
        medications: List<MedicationItem>,
        medicationLogs: List<MedicationLog>,
        vitals: List<VitalReading>,
        consultations: List<Consultation>,
        checkIns: List<DailyCheckIn>,
        mentalAssessments: List<MentalAssessment> = emptyList(),
        labBiomarkers: List<LabBiomarker> = emptyList(),
        soapBrief: SoapPreVisitBrief? = null,
        options: DoctorExportOptions,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(exportDir, "WellPlus_Doctor_Brief_$dateStamp.pdf")

            val cutoffMillis = System.currentTimeMillis() - options.daysWindow.toLong() * 24L * 60L * 60L * 1000L
            val windowConsults = consultations.filter { c ->
                val ts = (c.updatedAt ?: c.createdAt)?.toDate()?.time ?: System.currentTimeMillis()
                ts >= cutoffMillis
            }
            val windowVitals = vitals.filter { v ->
                val ts = (v.recordedAt ?: v.createdAt)?.toDate()?.time ?: System.currentTimeMillis()
                ts >= cutoffMillis
            }
            val windowCheckIns = checkIns.take(options.daysWindow)

            val doc = PdfDocument()
            try {
            val writer = PdfPageWriter(doc)

            writer.drawHeaderBanner(
                patientName = profile?.displayName?.ifBlank { "Personal Health Summary" }
                    ?: "Personal Health Summary",
                windowDays = options.daysWindow,
                accountEmail = accountEmail,
                regionLabel = "${region.countryName} (${if (region.isCanada) "Metric" else "US Units"})"
            )

            if (soapBrief != null && options.includeSoapAiBrief) {
                writer.drawSoapCalloutHeader()
                if (soapBrief.subjectiveSummary.isNotBlank()) {
                    writer.drawSubheading("S — Subjective (Patient-Reported Context)")
                    writer.drawBodyLine(soapBrief.subjectiveSummary)
                }
                if (soapBrief.objectiveHighlights.isNotEmpty()) {
                    writer.drawSubheading("O — Objective (Logged Vitals, Labs & Adherence)")
                    soapBrief.objectiveHighlights.forEach { writer.drawBullet(it) }
                }
                if (soapBrief.assessmentDiscussionPoints.isNotEmpty()) {
                    writer.drawSubheading("A — Assessment Discussion Themes (Non-Diagnostic)")
                    soapBrief.assessmentDiscussionPoints.forEach { writer.drawBullet(it) }
                }
                if (soapBrief.planAndMedications.isNotEmpty()) {
                    writer.drawSubheading("P — Active Plan & Medications")
                    soapBrief.planAndMedications.forEach { writer.drawBullet(it) }
                }
                if (soapBrief.priorityClinicianQuestions.isNotEmpty()) {
                    writer.drawSubheading("Priority Questions for Clinician")
                    soapBrief.priorityClinicianQuestions.forEach { writer.drawBullet(it) }
                }
                writer.addSpacer(10f)
            }

            if (options.includeProfile) {
                writer.drawSectionTitle("1. Patient Clinical Profile & Care Team")
                val sexLabel = BiologicalSex.fromRaw(profile?.biologicalSex).label
                val btLabel = BloodType.fromCode(profile?.bloodType).label
                val ageText = if ((profile?.age ?: 0L) > 0L) "${profile?.age} yrs" else "Not specified"
                writer.drawBodyLine("Age: $ageText   •   Biological Sex: $sexLabel   •   Blood Type: $btLabel")
                writer.drawBodyLine(
                    "Known Allergies: ${profile?.allergies?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None listed"}"
                )
                writer.drawBodyLine(
                    "Ongoing Conditions: ${profile?.conditions?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None listed"}"
                )
                if (profile?.coverageProvider?.isNotBlank() == true || profile?.coverageMemberId?.isNotBlank() == true) {
                    val coverageText = buildString {
                        if (profile.coverageProvider.isNotBlank()) append(profile.coverageProvider)
                        if (profile.coverageMemberId.isNotBlank()) {
                            if (isNotEmpty()) append("  •  ")
                            append("ID: ${profile.coverageMemberId}")
                        }
                    }
                    val coverageLabel = if (region.isCanada) "Provincial Health Card" else "Health Insurance"
                    writer.drawBodyLine("$coverageLabel: $coverageText")
                }
                if (profile?.primaryDoctorName?.isNotBlank() == true) {
                    writer.drawBodyLine("${region.primaryClinicianLabel}: ${profile.primaryDoctorName}")
                }
                if (profile?.preferredPharmacy?.isNotBlank() == true) {
                    writer.drawBodyLine("${region.pharmacyLabel}: ${profile.preferredPharmacy}")
                }
                if (profile?.hasEmergencyContact == true) {
                    val contact = buildString {
                        if (profile.emergencyContactName.isNotBlank()) append("${profile.emergencyContactName} — ")
                        append(profile.emergencyContactPhone)
                    }
                    writer.drawBodyLine("Emergency Contact: $contact")
                }
                writer.addSpacer(8f)
            }

            if (options.includeMedications) {
                writer.drawSectionTitle("2. Active Medications & Adherence")
                val activeMeds = medications.filter { it.active }
                val profileOnlyMeds = profile?.medications.orEmpty().filter { pm ->
                    activeMeds.none { it.name.equals(pm, ignoreCase = true) }
                }
                if (activeMeds.isEmpty() && profileOnlyMeds.isEmpty()) {
                    writer.drawBodyLine("No active medications or supplements recorded.")
                } else {
                    val takenCount = medicationLogs.count { it.parsedStatus == MedicationLogStatus.TAKEN }
                    val totalLogs = medicationLogs.size
                    if (totalLogs > 0) {
                        val pct = ((takenCount.toDouble() / totalLogs) * 100).toInt()
                        writer.drawBodyLine("Logged Adherence Rate: $pct% ($takenCount of $totalLogs recorded doses taken)")
                    }
                    activeMeds.forEach { med ->
                        val dosePart = if (med.dosage.isNotBlank()) " — ${med.dosage}" else ""
                        val instrPart = if (med.instructions.isNotBlank()) " (${med.instructions})" else ""
                        writer.drawBullet("${med.name}$dosePart [${med.parsedSlot.label}]$instrPart")
                    }
                    profileOnlyMeds.forEach { medName ->
                        writer.drawBullet("$medName (Profile list)")
                    }
                }
                writer.addSpacer(8f)
            }

            if (options.includeVitals) {
                writer.drawSectionTitle("3. Recent Vitals & Biometrics (${options.daysWindow}d)")
                if (windowVitals.isEmpty()) {
                    writer.drawBodyLine("No vitals recorded during the last ${options.daysWindow} days.")
                } else {
                    val latestByType = VitalType.entries.mapNotNull { type ->
                        windowVitals.firstOrNull { it.parsedType == type }
                    }
                    val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
                    latestByType.forEach { reading ->
                        val tsStr = reading.recordedAt?.toDate()?.let { dateFmt.format(it) } ?: "Recent"
                        val notePart = if (reading.note.isNotBlank()) " — ${reading.note}" else ""
                        writer.drawBullet(
                            "${reading.parsedType.label}: ${reading.formattedReadingForRegion(region)} (${reading.rangeStatus.label}, $tsStr)$notePart"
                        )
                    }
                }
                writer.addSpacer(8f)
            }

            if (options.includeLabBiomarkers && labBiomarkers.isNotEmpty()) {
                writer.drawSectionTitle("3b. Recent Lab Biomarkers (${labBiomarkers.size} Logged)")
                labBiomarkers.take(15).forEach { marker ->
                    val refPart = if (marker.referenceRangeText.isNotBlank()) " · Ref: ${marker.referenceRangeText}" else ""
                    val notePart = if (marker.labNote.isNotBlank()) " — ${marker.labNote.take(100)}" else ""
                    writer.drawBullet(
                        "${marker.markerName}: ${marker.formattedValue} [${marker.parsedStatus.label}]$refPart (${marker.panelDateKey})$notePart"
                    )
                }
                writer.addSpacer(8f)
            }

            if (options.includeWellnessSummary) {
                writer.drawSectionTitle("4. Daily Wellness Trends (${windowCheckIns.size} Check-Ins)")
                if (windowCheckIns.isEmpty()) {
                    writer.drawBodyLine("No daily wellness check-ins recorded yet.")
                } else {
                    val avgMood = windowCheckIns.map { it.moodLevel }.average()
                    val avgEnergy = windowCheckIns.map { it.energyLevel }.average()
                    val avgSleep = windowCheckIns.map { it.sleepHours }.average()
                    val avgWater = windowCheckIns.map { it.hydrationGlasses }.average()
                    val avgActivityMin = windowCheckIns.map { it.activityMinutes }.average()
                    writer.drawBodyLine(
                        String.format(
                            Locale.US,
                            "Average Mood: %.1f / 5   •   Energy: %.1f / 5   •   Sleep: %.1f hrs   •   Hydration: %.1f cups/day   •   Activity: %.0f min/day",
                            avgMood,
                            avgEnergy,
                            avgSleep,
                            avgWater,
                            avgActivityMin
                        )
                    )
                }
                writer.addSpacer(8f)
            }

            if (options.includeMentalWellbeing) {
                val windowMental = mentalAssessments.filter { m ->
                    val ts = (m.recordedAt ?: m.createdAt)?.toDate()?.time ?: System.currentTimeMillis()
                    ts >= cutoffMillis
                }
                writer.drawSectionTitle("5. Mental Wellbeing Screenings (PHQ-9 & GAD-7)")
                if (windowMental.isEmpty()) {
                    writer.drawBodyLine("No PHQ-9 or GAD-7 screenings recorded during the last ${options.daysWindow} days.")
                } else {
                    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
                    MentalAssessmentType.entries.forEach { type ->
                        val latest = windowMental.firstOrNull { it.parsedType == type }
                        if (latest != null) {
                            val dateStr = (latest.recordedAt ?: latest.createdAt)?.toDate()?.let { dateFmt.format(it) } ?: "Recent"
                            val crisisSuffix = if (latest.crisisFlagged) "  [Elevated Safety Flag]" else ""
                            writer.drawBullet(
                                "${type.title}: ${latest.totalScore}/${latest.maxScore} (${latest.parsedBand.label}, $dateStr)$crisisSuffix"
                            )
                        }
                    }
                }
                writer.addSpacer(8f)
            }

            if (options.includeConsultations) {
                writer.drawSectionTitle("6. Symptom History & Questions for Clinician (${windowConsults.size} Entries)")
                if (windowConsults.isEmpty()) {
                    writer.drawBodyLine("No symptom consultations recorded during the last ${options.daysWindow} days.")
                } else {
                    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
                    windowConsults.take(10).forEach { c ->
                        val dateText = (c.createdAt ?: c.updatedAt)?.toDate()?.let { dateFmt.format(it) } ?: "Recent"
                        writer.drawSubheading("${c.title}  [$dateText]")
                        writer.drawBodyLine(
                            "Area: ${c.bodyArea}  •  Severity: ${c.severity}/10  •  Duration: ${c.duration}  •  Triage: ${c.parsedUrgency.label}  •  Outcome: ${c.parsedFollowUpStatus.label}"
                        )
                        writer.drawBodyLine("Summary: ${c.aiSummary}")
                        if (c.followUpNote.isNotBlank()) {
                            writer.drawBodyLine("Follow-Up Note: ${c.followUpNote}")
                        }
                        if (c.doctorQuestions.isNotEmpty()) {
                            writer.drawBodyLine("Suggested Questions:")
                            c.doctorQuestions.take(3).forEach { q ->
                                writer.drawBullet(q)
                            }
                        }
                        writer.addSpacer(6f)
                    }
                }
            }

            writer.drawDisclaimerFooter()
            writer.finish()

            FileOutputStream(outFile).use { stream ->
                doc.writeTo(stream)
            }
            } finally {
                doc.close()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Well+ — Doctor Visit Summary")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is my personal health and symptom brief exported from Well+."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(shareIntent, "Share Doctor Visit Brief (PDF)"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class PdfPageWriter(private val document: PdfDocument) {
        private var pageNumber = 0
        private var currentPage: PdfDocument.Page? = null
        private var canvas: Canvas = Canvas()
        private var cursorY = MARGIN

        private val tealColor = Color.rgb(0, 122, 255)
        private val seafoamColor = Color.rgb(229, 241, 255)
        private val creamColor = Color.rgb(255, 255, 255)
        private val charcoalColor = Color.rgb(17, 17, 20)
        private val slateColor = Color.rgb(99, 99, 102)
        private val dividerColor = Color.rgb(229, 229, 234)

        private val bannerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        private val bannerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = seafoamColor
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tealColor
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        private val subheadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = charcoalColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = charcoalColor
            textSize = 9.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = slateColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            strokeWidth = 1f
        }

        init {
            startNewPage()
        }

        private fun startNewPage() {
            currentPage?.let { document.finishPage(it) }
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(info)
            currentPage = page
            canvas = page.canvas
            fillPaint.color = creamColor
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), fillPaint)
            cursorY = MARGIN
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (cursorY + requiredHeight > PAGE_HEIGHT - MARGIN - 24f) {
                startNewPage()
            }
        }

        fun drawHeaderBanner(
            patientName: String,
            windowDays: Int,
            accountEmail: String,
            regionLabel: String
        ) {
            val bannerHeight = 76f
            fillPaint.color = tealColor
            canvas.drawRoundRect(
                RectF(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY + bannerHeight),
                12f,
                12f,
                fillPaint
            )
            val generatedDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
            canvas.drawText("WELL+  •  DOCTOR VISIT BRIEF  •  $regionLabel", MARGIN + 16f, cursorY + 24f, bannerSubPaint)
            canvas.drawText(patientName, MARGIN + 16f, cursorY + 46f, bannerTitlePaint)
            val meta = "Last $windowDays days  •  Generated $generatedDate" +
                if (accountEmail.isNotBlank()) "  •  $accountEmail" else ""
            canvas.drawText(meta, MARGIN + 16f, cursorY + 63f, bannerSubPaint)
            cursorY += bannerHeight + 18f
        }

        fun drawSoapCalloutHeader() {
            ensureSpace(34f)
            fillPaint.color = seafoamColor
            canvas.drawRoundRect(
                RectF(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY + 24f),
                8f,
                8f,
                fillPaint
            )
            canvas.drawText(
                "AI-ASSISTED SOAP PRE-VISIT BRIEF  •  FOR CLINICIAN DISCUSSION",
                MARGIN + 10f,
                cursorY + 16f,
                sectionPaint
            )
            cursorY += 32f
        }

        fun drawSectionTitle(title: String) {
            ensureSpace(28f)
            canvas.drawText(title, MARGIN, cursorY + 13f, sectionPaint)
            cursorY += 18f
            canvas.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, linePaint)
            cursorY += 10f
        }

        fun drawSubheading(text: String) {
            drawWrappedText(text, subheadingPaint, indent = 0f, lineHeight = 14f)
        }

        fun drawBodyLine(text: String) {
            drawWrappedText(text, bodyPaint, indent = 0f, lineHeight = 13.5f)
        }

        fun drawBullet(text: String) {
            drawWrappedText("•  $text", bodyPaint, indent = 8f, lineHeight = 13.5f)
        }

        fun addSpacer(dp: Float) {
            cursorY += dp
        }

        fun drawDisclaimerFooter() {
            ensureSpace(46f)
            cursorY += 6f
            canvas.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, linePaint)
            cursorY += 10f
            drawWrappedText(
                "Medical Notice: Prepared by Well+ for personal health organization and clinician discussion. AI-assisted summaries are informational only and do not constitute a medical diagnosis or prescription.",
                mutedPaint,
                indent = 0f,
                lineHeight = 11.5f
            )
        }

        fun finish() {
            currentPage?.let {
                document.finishPage(it)
                currentPage = null
            }
        }

        private fun drawWrappedText(
            text: String,
            paint: Paint,
            indent: Float,
            lineHeight: Float
        ) {
            val maxW = CONTENT_WIDTH - indent
            val words = text.replace("\n", " ").split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return
            val lineBuilder = StringBuilder()
            for (word in words) {
                val candidate = if (lineBuilder.isEmpty()) word else "$lineBuilder $word"
                if (paint.measureText(candidate) <= maxW) {
                    lineBuilder.clear()
                    lineBuilder.append(candidate)
                } else {
                    if (lineBuilder.isNotEmpty()) {
                        ensureSpace(lineHeight)
                        canvas.drawText(lineBuilder.toString(), MARGIN + indent, cursorY + 10f, paint)
                        cursorY += lineHeight
                    }
                    lineBuilder.clear()
                    lineBuilder.append(word)
                }
            }
            if (lineBuilder.isNotEmpty()) {
                ensureSpace(lineHeight)
                canvas.drawText(lineBuilder.toString(), MARGIN + indent, cursorY + 10f, paint)
                cursorY += lineHeight
            }
        }
    }
}
