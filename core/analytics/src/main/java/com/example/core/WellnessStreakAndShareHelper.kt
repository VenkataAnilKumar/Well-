package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.DailyCheckIn
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayHabitDot(
    val dateKey: String,
    val dayLetter: String,
    val isToday: Boolean,
    val completed: Boolean
)

data class WellnessStreakSummary(
    val consecutiveDays: Int,
    val totalDaysLogged: Int,
    val last7Days: List<DayHabitDot>
)

/**
 * Computes real consecutive check-in streaks and generates privacy-safe (zero-PHI)
 * visual 1080x1080 PNG Milestone Share Cards for organic sharing via FileProvider.
 */
object WellnessStreakAndShareHelper {

    fun computeStreakSummary(
        todayDateKey: String,
        checkIns: List<DailyCheckIn>
    ): WellnessStreakSummary {
        val loggedDates = checkIns.map { it.dateKey }.filter { it.isNotBlank() }.toSet()
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US) // Universal 3-letter weekday -> take(1) for M, T, W...

        val todayCal = Calendar.getInstance().apply {
            runCatching { keyFmt.parse(todayDateKey) }.getOrNull()?.let { time = it }
        }

        // Build 7-day rolling window ending today (oldest -> today)
        val last7Days = (6 downTo 0).map { daysAgo ->
            val cal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = keyFmt.format(cal.time)
            val letter = dayFmt.format(cal.time).uppercase(Locale.US).take(1).ifBlank { "D" }
            DayHabitDot(
                dateKey = key,
                dayLetter = letter,
                isToday = daysAgo == 0,
                completed = key in loggedDates
            )
        }

        // Compute true consecutive streak (starts from today if logged, otherwise yesterday)
        var streak = 0
        val cursor = todayCal.clone() as Calendar
        if (todayDateKey !in loggedDates) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (streak < 3650) {
            val key = keyFmt.format(cursor.time)
            if (key in loggedDates) {
                streak += 1
                cursor.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return WellnessStreakSummary(
            consecutiveDays = streak,
            totalDaysLogged = loggedDates.size,
            last7Days = last7Days
        )
    }

    /**
     * Renders a crisp 1080x1080 zero-PHI PNG milestone card to app cache and builds
     * a ready-to-launch Android share chooser Intent (requires zero storage permissions).
     */
    fun createMilestoneShareIntent(
        context: Context,
        chooserTitle: String,
        percentComplete: Int,
        streakSummary: WellnessStreakSummary,
        completedGoals: Int,
        totalGoals: Int
    ): Intent {
        val safeCaption = buildString {
            if (completedGoals >= totalGoals && totalGoals > 0) {
                append("Closed my Daily Wellness Ring on Well+! ")
            } else {
                append("Checking in on my daily health habits with Well+. ")
            }
            if (streakSummary.consecutiveDays > 0) {
                append("${streakSummary.consecutiveDays}-day calm streak going strong.")
            }
        }

        val imageFile = runCatching {
            val bitmap = renderMilestoneBitmap(
                percentComplete = percentComplete.coerceIn(0, 100),
                streakSummary = streakSummary,
                completedGoals = completedGoals,
                totalGoals = totalGoals
            )
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportsDir, "wellplus_milestone.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            bitmap.recycle()
            outFile
        }.getOrNull()

        if (imageFile != null && imageFile.exists()) {
            val contentUri = runCatching {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
            }.getOrNull()

            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, safeCaption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                return Intent.createChooser(shareIntent, chooserTitle)
            }
        }

        // Safe fallback to plain text sharing
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, safeCaption)
        }
        return Intent.createChooser(textIntent, chooserTitle)
    }

    private fun renderMilestoneBitmap(
        percentComplete: Int,
        streakSummary: WellnessStreakSummary,
        completedGoals: Int,
        totalGoals: Int
    ): Bitmap {
        val size = 1080
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 1. Calm modern editorial gradient background (Apple Blue -> Soft Sky -> Pure White)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(
                    Color.parseColor("#EBF4FF"),
                    Color.parseColor("#F5FAFF"),
                    Color.parseColor("#E6F7F5")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Subtle decorative soft halo circles
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(26, 0, 122, 255)
        }
        canvas.drawCircle(920f, 160f, 240f, haloPaint)
        haloPaint.color = Color.argb(24, 23, 142, 163)
        canvas.drawCircle(140f, 940f, 220f, haloPaint)

        // 2. Center elevated pure white card
        val cardRect = RectF(84f, 84f, size - 84f, size - 84f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(32f, 0f, 14f, Color.argb(22, 17, 17, 20))
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, cardPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#E5E5EA")
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, borderPaint)

        // 3. Brand Header Pill ("Well+  •  DAILY WELLNESS")
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#007AFF")
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Well+  •  DAILY WELLNESS", size / 2f, 186f, brandPaint)

        // 4. Center Glowing Progress Ring
        val ringCenterX = size / 2f
        val ringCenterY = 445f
        val ringRadius = 178f
        val ringRect = RectF(
            ringCenterX - ringRadius,
            ringCenterY - ringRadius,
            ringCenterX + ringRadius,
            ringCenterY + ringRadius
        )

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 38f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#EEF1F6")
        }
        canvas.drawArc(ringRect, -90f, 360f, false, trackPaint)

        val sweepAngle = (percentComplete / 100f * 360f).coerceAtLeast(if (percentComplete > 0) 24f else 0f)
        if (sweepAngle > 0f) {
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 38f
                strokeCap = Paint.Cap.ROUND
                shader = LinearGradient(
                    ringRect.left, ringRect.top, ringRect.right, ringRect.bottom,
                    if (percentComplete >= 100) Color.parseColor("#28A745") else Color.parseColor("#007AFF"),
                    Color.parseColor("#178EA3"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawArc(ringRect, -90f, sweepAngle, false, arcPaint)
        }

        // Percent number inside ring
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111114")
            textSize = 104f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$percentComplete%", ringCenterX, ringCenterY + 26f, pctPaint)

        val subRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#636366")
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$completedGoals of $totalGoals daily habits", ringCenterX, ringCenterY + 74f, subRingPaint)

        // 5. Streak Headline Pill
        val streakLabel = when {
            streakSummary.consecutiveDays >= 2 -> "${streakSummary.consecutiveDays}-Day Calm Wellness Streak"
            streakSummary.consecutiveDays == 1 -> "Day 1 Wellness Streak Started"
            else -> "Building Calm Daily Habits"
        }
        val pillWidth = 680f
        val pillHeight = 76f
        val pillRect = RectF(
            ringCenterX - pillWidth / 2f,
            682f,
            ringCenterX + pillWidth / 2f,
            682f + pillHeight
        )
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF4E5")
        }
        canvas.drawRoundRect(pillRect, 38f, 38f, pillBgPaint)

        val streakTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C97300")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(streakLabel, ringCenterX, pillRect.centerY() + 12f, streakTextPaint)

        // 6. 7-Day Consistency Dot Strip
        val dots = streakSummary.last7Days
        val spacing = 96f
        val startX = ringCenterX - ((dots.size - 1) * spacing) / 2f
        val dotY = 836f
        val dotRadius = 26f

        val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dotLetterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        dots.forEachIndexed { idx, dot ->
            val cx = startX + idx * spacing
            dotFillPaint.color = when {
                dot.completed -> Color.parseColor("#007AFF")
                dot.isToday -> Color.parseColor("#E5F1FF")
                else -> Color.parseColor("#F2F2F7")
            }
            canvas.drawCircle(cx, dotY, dotRadius, dotFillPaint)

            dotLetterPaint.color = when {
                dot.completed -> Color.WHITE
                dot.isToday -> Color.parseColor("#007AFF")
                else -> Color.parseColor("#636366")
            }
            canvas.drawText(dot.dayLetter, cx, dotY + 9f, dotLetterPaint)
        }

        // 7. Calm Privacy-Safe Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8E8E93")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Private  •  Calm  •  Daily Health Clarity", ringCenterX, 942f, footerPaint)

        return bmp
    }
}
