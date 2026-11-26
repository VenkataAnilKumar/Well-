package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MentalAssessmentType(
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val maxScore: Int,
    val questions: List<String>
) {
    PHQ9(
        title = "Depression Risk (PHQ-9)",
        shortLabel = "PHQ-9",
        subtitle = "9 questions • ~2 min • Past 2 weeks",
        maxScore = 27,
        questions = listOf(
            "Little interest or pleasure in doing things",
            "Feeling down, depressed, or hopeless",
            "Trouble falling or staying asleep, or sleeping too much",
            "Feeling tired or having little energy",
            "Poor appetite or overeating",
            "Feeling bad about yourself — or that you are a failure or have let yourself or your family down",
            "Trouble concentrating on things, such as reading or watching television",
            "Moving or speaking so slowly that other people could have noticed, or being so fidgety or restless that you have been moving around a lot more than usual",
            "Thoughts that you would be better off dead, or of hurting yourself in some way"
        )
    ),
    GAD7(
        title = "Anxiety Risk (GAD-7)",
        shortLabel = "GAD-7",
        subtitle = "7 questions • ~1 min • Past 2 weeks",
        maxScore = 21,
        questions = listOf(
            "Feeling nervous, anxious, or on edge",
            "Not being able to stop or control worrying",
            "Worrying too much about different things",
            "Trouble relaxing",
            "Being so restless that it is hard to sit still",
            "Becoming easily annoyed or irritable",
            "Feeling afraid, as if something awful might happen"
        )
    );

    fun evaluateBand(score: Int): MentalSeverityBand = when (this) {
        PHQ9 -> when {
            score >= 20 -> MentalSeverityBand.SEVERE
            score >= 15 -> MentalSeverityBand.MODERATELY_SEVERE
            score >= 10 -> MentalSeverityBand.MODERATE
            score >= 5 -> MentalSeverityBand.MILD
            else -> MentalSeverityBand.MINIMAL
        }
        GAD7 -> when {
            score >= 15 -> MentalSeverityBand.SEVERE
            score >= 10 -> MentalSeverityBand.MODERATE
            score >= 5 -> MentalSeverityBand.MILD
            else -> MentalSeverityBand.MINIMAL
        }
    }

    fun isCrisisTriggered(answers: List<Int>, totalScore: Int): Boolean = when (this) {
        PHQ9 -> (answers.getOrNull(8) ?: 0) > 0 || totalScore >= 20
        GAD7 -> false
    }

    companion object {
        val RESPONSE_OPTIONS: List<Pair<Int, String>> = listOf(
            0 to "Not at all",
            1 to "Several days",
            2 to "More than half the days",
            3 to "Nearly every day"
        )

        fun fromRaw(raw: String?): MentalAssessmentType =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: PHQ9
    }
}

enum class MentalSeverityBand(val label: String, val guidance: String) {
    MINIMAL("Minimal", "Scores are in the minimal range. Keep supporting your daily balance and rest."),
    MILD("Mild", "Mild symptoms noted. Gentle routines, rest, and check-ins can help track patterns."),
    MODERATE("Moderate", "Moderate symptoms noted. Consider sharing this summary with a clinician or therapist."),
    MODERATELY_SEVERE("Moderately Severe", "Elevated symptoms noted. Reaching out to a healthcare professional is recommended."),
    SEVERE("Severe", "Significant symptoms noted. Please connect with a doctor or mental health professional promptly.");

    companion object {
        fun fromRaw(raw: String?): MentalSeverityBand =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: MINIMAL
    }
}

@Keep
@IgnoreExtraProperties
data class MentalAssessment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val assessmentType: String = MentalAssessmentType.PHQ9.name,
    val totalScore: Long = 0L,
    val maxScore: Long = 27L,
    val severityBand: String = MentalSeverityBand.MINIMAL.name,
    val itemScores: List<Long> = emptyList(),
    val crisisFlagged: Boolean = false,
    val aiReflection: String = "",
    val aiCopingSteps: List<String> = emptyList(),
    @ServerTimestamp val recordedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: MentalAssessmentType
        get() = MentalAssessmentType.fromRaw(assessmentType)

    val parsedBand: MentalSeverityBand
        get() = MentalSeverityBand.fromRaw(severityBand)
}

data class MentalWellbeingReflection(
    val empatheticSummary: String,
    val gentleCopingSteps: List<String>,
    val clinicianTalkingPoints: List<String>,
    val encourageProfessionalSupport: Boolean
)
