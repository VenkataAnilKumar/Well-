package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.PhysicalActivityLevel

enum class WellnessMoodOption(
    val level: Int,
    val label: String,
    val caption: String,
    val icon: ImageVector
) {
    LOW(1, "Low", "Heavy or drained — take gentle care today.", Icons.Filled.SentimentVeryDissatisfied),
    TENSE(2, "Tense", "A bit strained or unsettled — pause for a breath.", Icons.Filled.SentimentDissatisfied),
    STEADY(3, "Steady", "Balanced baseline — moving at a calm pace.", Icons.Filled.SentimentNeutral),
    CALM(4, "Calm", "Grounded, clear, and comfortably at ease.", Icons.Filled.SentimentSatisfied),
    THRIVING(5, "Thriving", "Uplifted, energized, and feeling strong.", Icons.Filled.SentimentVerySatisfied);

    companion object {
        fun fromLevel(level: Int): WellnessMoodOption =
            entries.firstOrNull { it.level == level } ?: CALM
    }
}

enum class GeneralHealthStatus(
    val label: String,
    val icon: ImageVector
) {
    FEELING_GREAT("Feeling Great", Icons.Filled.Favorite),
    MOSTLY_WELL("Mostly Well", Icons.Filled.CheckCircle),
    MILD_DISCOMFORT("Mild Discomfort", Icons.Filled.Healing),
    FATIGUED("Fatigued", Icons.Filled.Battery2Bar),
    RECOVERING("Recovering", Icons.Filled.SelfImprovement),
    UNWELL("Unwell", Icons.Filled.Sick);

    companion object {
        fun fromLabel(label: String?): GeneralHealthStatus =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) } ?: MOSTLY_WELL
    }
}

data class ParsedWellnessNotes(
    val generalStatus: GeneralHealthStatus,
    val signals: Set<String>,
    val reflection: String
)

object WellnessNoteCodec {
    private const val HEADER_PREFIX = "[Status: "
    private const val SIGNALS_MARKER = " | Signals: "
    private const val HEADER_SUFFIX = "]"
    private const val MAX_NOTE_LENGTH = 600

    fun encode(
        status: GeneralHealthStatus,
        signals: Set<String>,
        reflection: String
    ): String {
        val cleanSignals = signals.map { it.trim() }.filter { it.isNotEmpty() }
        val header = buildString {
            append(HEADER_PREFIX)
            append(status.label)
            if (cleanSignals.isNotEmpty()) {
                append(SIGNALS_MARKER)
                append(cleanSignals.joinToString(", "))
            }
            append(HEADER_SUFFIX)
        }
        val cleanReflection = reflection.trim()
        val combined = if (cleanReflection.isEmpty()) header else "$header\n$cleanReflection"
        return combined.take(MAX_NOTE_LENGTH)
    }

    fun decode(rawNotes: String?): ParsedWellnessNotes {
        val text = rawNotes?.trim().orEmpty()
        if (!text.startsWith(HEADER_PREFIX)) {
            return ParsedWellnessNotes(
                generalStatus = GeneralHealthStatus.MOSTLY_WELL,
                signals = emptySet(),
                reflection = text
            )
        }
        val endIdx = text.indexOf(HEADER_SUFFIX)
        if (endIdx <= HEADER_PREFIX.length) {
            return ParsedWellnessNotes(
                generalStatus = GeneralHealthStatus.MOSTLY_WELL,
                signals = emptySet(),
                reflection = text
            )
        }
        val headerBody = text.substring(HEADER_PREFIX.length, endIdx)
        val reflection = text.substring(endIdx + 1).trim()
        val parts = headerBody.split(SIGNALS_MARKER, limit = 2)
        val status = GeneralHealthStatus.fromLabel(parts.firstOrNull())
        val signals = parts.getOrNull(1)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()
        return ParsedWellnessNotes(
            generalStatus = status,
            signals = signals,
            reflection = reflection
        )
    }
}

fun activityLevelIcon(level: PhysicalActivityLevel): ImageVector = when (level) {
    PhysicalActivityLevel.SEDENTARY -> Icons.Filled.Weekend
    PhysicalActivityLevel.LIGHT -> Icons.AutoMirrored.Filled.DirectionsWalk
    PhysicalActivityLevel.MODERATE -> Icons.AutoMirrored.Filled.DirectionsBike
    PhysicalActivityLevel.ACTIVE -> Icons.AutoMirrored.Filled.DirectionsRun
    PhysicalActivityLevel.VIGOROUS -> Icons.Filled.FitnessCenter
}
