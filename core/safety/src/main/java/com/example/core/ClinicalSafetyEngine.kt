package com.example.core

import com.example.data.model.VitalType
import java.util.Locale

enum class RedFlagCategory {
    CARDIAC_OR_CHEST,
    SEVERE_RESPIRATORY,
    NEUROLOGICAL_STROKE,
    ANAPHYLAXIS,
    SEVERE_BLEEDING_OR_TRAUMA,
    POISONING_OR_OVERDOSE,
    MENTAL_HEALTH_CRISIS
}

data class SymptomRedFlagResult(
    val category: RedFlagCategory,
    val isMentalHealthCrisis: Boolean,
    val headline: String,
    val rationale: String,
    val immediateSteps: List<String>
)

data class CriticalVitalAlert(
    val vitalType: VitalType,
    val formattedReading: String,
    val headline: String,
    val guidance: String,
    val immediateSteps: List<String>
)

/**
 * Deterministic, offline-capable clinical safety engine that evaluates symptoms and vitals
 * independently of any LLM so life-threatening red flags can never be missed or downgraded.
 */
object ClinicalSafetyEngine {

    private val crisisPatterns = listOf(
        "suicide", "suicidal", "kill myself", "end my life", "want to die",
        "don't want to live", "dont want to live", "hurt myself", "self harm",
        "self-harm", "overdose on purpose", "cut myself"
    )

    private val cardiacPatterns = listOf(
        "chest pain", "chest pressure", "chest tightness", "crushing chest",
        "squeezing chest", "pain radiating to arm", "pain down my left arm",
        "jaw pain with chest", "heart attack"
    )

    private val respiratoryPatterns = listOf(
        "can't breathe", "cant breathe", "cannot breathe", "unable to breathe",
        "gasping for air", "turning blue", "blue lips", "cyanosis",
        "severe shortness of breath", "choking"
    )

    private val strokePatterns = listOf(
        "face drooping", "facial droop", "slurred speech", "sudden weakness on one side",
        "arm weakness one side", "cannot speak", "sudden vision loss",
        "worst headache of my life", "thunderclap headache", "stroke symptoms"
    )

    private val anaphylaxisPatterns = listOf(
        "throat swelling", "throat closing", "swollen tongue", "swollen lips and hives",
        "anaphylaxis", "severe allergic reaction"
    )

    private val severeBleedingPatterns = listOf(
        "uncontrolled bleeding", "won't stop bleeding", "wont stop bleeding",
        "coughing up blood", "vomiting blood", "heavy bleeding",
        "passed out", "lost consciousness", "unconscious", "having a seizure", "seizure right now"
    )

    private val overdosePatterns = listOf(
        "overdose", "took too many pills", "swallowed poison", "accidental poisoning"
    )

    fun evaluateSymptoms(
        symptomText: String,
        associatedSymptoms: List<String> = emptyList(),
        severity: Int = 5
    ): SymptomRedFlagResult? {
        val combined = buildString {
            append(symptomText.lowercase(Locale.US))
            if (associatedSymptoms.isNotEmpty()) {
                append(' ')
                append(associatedSymptoms.joinToString(" ").lowercase(Locale.US))
            }
        }

        if (crisisPatterns.any { combined.contains(it) }) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.MENTAL_HEALTH_CRISIS,
                isMentalHealthCrisis = true,
                headline = "Immediate Crisis Support Available",
                rationale = "Your message mentions thoughts of self-harm or severe distress. Free, confidential human support is available right now.",
                immediateSteps = listOf(
                    "Call or text your local crisis helpline immediately.",
                    "If you are in immediate physical danger, call local emergency services.",
                    "Stay with a trusted friend, family member, or healthcare professional."
                )
            )
        }

        if (anaphylaxisPatterns.any { combined.contains(it) }) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.ANAPHYLAXIS,
                isMentalHealthCrisis = false,
                headline = "Possible Severe Allergic Reaction",
                rationale = "Airway, tongue, or throat swelling can worsen rapidly and requires emergency medical evaluation.",
                immediateSteps = listOf(
                    "Call emergency services immediately.",
                    "If you have a prescribed epinephrine auto-injector, use it as directed by your doctor.",
                    "Do not drive yourself to the hospital."
                )
            )
        }

        if (cardiacPatterns.any { combined.contains(it) }) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.CARDIAC_OR_CHEST,
                isMentalHealthCrisis = false,
                headline = "Urgent Chest / Cardiac Warning Sign",
                rationale = "Chest pain, tightness, or radiating discomfort should be evaluated immediately by emergency responders.",
                immediateSteps = listOf(
                    "Call local emergency services right away rather than waiting.",
                    "Sit or lie down in a comfortable position and stay calm.",
                    "Do not drive yourself to urgent care or the ER."
                )
            )
        }

        if (strokePatterns.any { combined.contains(it) }) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.NEUROLOGICAL_STROKE,
                isMentalHealthCrisis = false,
                headline = "Urgent Neurological Warning Sign",
                rationale = "Sudden facial drooping, one-sided weakness, speech trouble, vision loss, or thunderclap headache requires immediate emergency care.",
                immediateSteps = listOf(
                    "Call local emergency services immediately.",
                    "Note the exact time symptoms began for emergency clinicians.",
                    "Do not take food, drink, or unprescribed medication while awaiting help."
                )
            )
        }

        if (respiratoryPatterns.any { combined.contains(it) }) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.SEVERE_RESPIRATORY,
                isMentalHealthCrisis = false,
                headline = "Severe Breathing Difficulty Detected",
                rationale = "Severe shortness of breath, gasping, or bluish lips requires immediate emergency medical attention.",
                immediateSteps = listOf(
                    "Call local emergency services immediately.",
                    "Sit upright to ease breathing and loosen tight clothing.",
                    "Use your prescribed rescue inhaler only if already prescribed for asthma."
                )
            )
        }

        if (overdosePatterns.any { combined.contains(it) }) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.POISONING_OR_OVERDOSE,
                isMentalHealthCrisis = false,
                headline = "Possible Poisoning or Overdose",
                rationale = "Suspected medication overdose or poisoning requires immediate emergency or poison-control intervention.",
                immediateSteps = listOf(
                    "Call local emergency services or poison control right away.",
                    "Keep medication bottles or substance containers nearby for responders.",
                    "Do not induce vomiting unless explicitly instructed by emergency personnel."
                )
            )
        }

        if (severeBleedingPatterns.any { combined.contains(it) } ||
            (severity >= 9 && (combined.contains("chest") || combined.contains("breath") || combined.contains("faint")))
        ) {
            return SymptomRedFlagResult(
                category = RedFlagCategory.SEVERE_BLEEDING_OR_TRAUMA,
                isMentalHealthCrisis = false,
                headline = "High-Severity Emergency Warning Sign",
                rationale = "Severe bleeding, fainting/loss of consciousness, seizures, or extreme acute distress requires immediate in-person evaluation.",
                immediateSteps = listOf(
                    "Call local emergency services immediately.",
                    "Alert someone nearby or call your saved emergency contact.",
                    "Follow dispatcher instructions until help arrives."
                )
            )
        }

        return null
    }

    /**
     * Deterministic vital-sign safety check for critical physiological thresholds.
     */
    fun evaluateVital(
        vitalType: VitalType,
        primaryValue: Double,
        secondaryValue: Double? = null,
        unit: String = vitalType.defaultUnit,
        region: HealthRegionProfile = HealthRegionDirectory.US
    ): CriticalVitalAlert? {
        return when (vitalType) {
            VitalType.BLOOD_PRESSURE -> {
                val sys = primaryValue
                val dia = secondaryValue ?: 0.0
                when {
                    sys >= 180.0 || dia >= 120.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = "${sys.toInt()}/${dia.toInt()} mmHg",
                        headline = "Hypertensive Crisis Range Logged",
                        guidance = "A blood pressure reading of 180/120 mmHg or higher can be a medical emergency—especially if paired with chest pain, shortness of breath, vision changes, or numbness.",
                        immediateSteps = listOf(
                            "Rest quietly for 5 minutes and re-check once if you feel completely well.",
                            "If you have chest pain, shortness of breath, headache, or numbness, call emergency services immediately.",
                            "Contact your doctor or urgent care promptly if a repeat reading remains ≥ 180/120."
                        )
                    )
                    sys in 1.0..80.0 || (dia > 0.0 && dia <= 50.0) -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = "${sys.toInt()}/${dia.toInt()} mmHg",
                        headline = "Very Low Blood Pressure Logged",
                        guidance = "Very low blood pressure can cause fainting or inadequate organ perfusion, particularly with dizziness, confusion, or cold clammy skin.",
                        immediateSteps = listOf(
                            "Sit or lie down immediately to prevent a fall.",
                            "Call emergency services if you feel faint, confused, or short of breath."
                        )
                    )
                    else -> null
                }
            }
            VitalType.SPO2 -> {
                if (primaryValue < 90.0) {
                    CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = "${primaryValue.toInt()}% SpO₂",
                        headline = "Critically Low Blood Oxygen (SpO₂)",
                        guidance = "Blood oxygen below 90% can indicate severe hypoxemia. Re-check on a warm, still finger and seek immediate medical care if confirmed.",
                        immediateSteps = listOf(
                            "Confirm sensor placement on a warm finger without nail polish.",
                            "If the reading stays under 90% or you feel short of breath, call emergency services immediately."
                        )
                    )
                } else null
            }
            VitalType.HEART_RATE -> {
                when {
                    primaryValue >= 160.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = "${primaryValue.toInt()} bpm",
                        headline = "Very High Heart Rate Logged",
                        guidance = "A resting heart rate above 160 bpm warrants prompt medical attention—especially with chest pain, dizziness, or faintness.",
                        immediateSteps = listOf(
                            "Stop physical activity and sit down calmly.",
                            "Call emergency services immediately if accompanied by chest pain, lightheadedness, or breathing difficulty."
                        )
                    )
                    primaryValue in 1.0..39.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = "${primaryValue.toInt()} bpm",
                        headline = "Very Low Heart Rate Logged",
                        guidance = "A heart rate below 40 bpm can be dangerous unless known to your physician (e.g., elite athletes), especially with dizziness or fatigue.",
                        immediateSteps = listOf(
                            "Re-check your pulse and sit down safely.",
                            "Seek urgent or emergency care if you feel faint, dizzy, or short of breath."
                        )
                    )
                    else -> null
                }
            }
            VitalType.TEMPERATURE -> {
                val isCelsiusInput = HealthRegionDirectory.isCelsiusUnit(unit, primaryValue)
                val tempF = if (isCelsiusInput) {
                    HealthRegionDirectory.celsiusToFahrenheit(primaryValue)
                } else {
                    primaryValue
                }
                val tempC = if (isCelsiusInput) {
                    primaryValue
                } else {
                    HealthRegionDirectory.fahrenheitToCelsius(primaryValue)
                }
                val formattedTemp = if (region.usesCelsiusTemp || isCelsiusInput) {
                    String.format(Locale.US, "%.1f °C", tempC)
                } else {
                    String.format(Locale.US, "%.1f °F", tempF)
                }
                val highThresholdText = if (region.usesCelsiusTemp) "40.0°C (104°F)" else "104°F (40°C)"
                val lowThresholdText = if (region.usesCelsiusTemp) "35.0°C (95°F)" else "95°F (35°C)"
                when {
                    tempF >= 104.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = formattedTemp,
                        headline = "Dangerously High Fever Logged",
                        guidance = "A fever of $highThresholdText or higher requires prompt medical evaluation—especially with confusion, stiff neck, rash, or difficulty breathing.",
                        immediateSteps = listOf(
                            "Seek immediate emergency care if accompanied by confusion, seizure, stiff neck, or breathing trouble.",
                            "Move to a cool environment and contact urgent medical care right away."
                        )
                    )
                    tempF <= 95.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = formattedTemp,
                        headline = "Possible Hypothermia Range",
                        guidance = "A core body temperature of $lowThresholdText or lower requires prompt medical evaluation.",
                        immediateSteps = listOf(
                            "Move to a warm, dry area and verify the thermometer reading.",
                            "Call emergency services if experiencing shivering cessation, slurred speech, or confusion."
                        )
                    )
                    else -> null
                }
            }
            VitalType.BLOOD_GLUCOSE -> {
                val isMmolInput = HealthRegionDirectory.isMmolGlucoseUnit(unit, primaryValue)
                val mgDl = if (isMmolInput) {
                    HealthRegionDirectory.mmolLToMgDl(primaryValue)
                } else {
                    primaryValue
                }
                val mmolL = if (isMmolInput) {
                    primaryValue
                } else {
                    HealthRegionDirectory.mgDlToMmolL(primaryValue)
                }
                val formattedGlucose = if (region.usesMmolGlucose || isMmolInput) {
                    String.format(Locale.US, "%.1f mmol/L", mmolL)
                } else {
                    "${mgDl.toInt()} mg/dL"
                }
                val lowThresholdText = if (region.usesMmolGlucose) "3.0 mmol/L (54 mg/dL)" else "54 mg/dL"
                val highThresholdText = if (region.usesMmolGlucose) "22.2 mmol/L (400 mg/dL)" else "400 mg/dL"
                when {
                    mgDl < 54.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = formattedGlucose,
                        headline = "Critically Low Blood Sugar Logged",
                        guidance = "Blood glucose below $lowThresholdText is severe hypoglycemia and requires immediate treatment.",
                        immediateSteps = listOf(
                            "If awake and able to swallow safely, consume 15g of fast-acting carbohydrates and re-check in 15 minutes.",
                            "Call emergency services immediately if experiencing confusion, inability to swallow, or fainting."
                        )
                    )
                    mgDl >= 400.0 -> CriticalVitalAlert(
                        vitalType = vitalType,
                        formattedReading = formattedGlucose,
                        headline = "Critically High Blood Sugar Logged",
                        guidance = "Blood glucose at or above $highThresholdText carries risk of diabetic ketoacidosis (DKA) or hyperosmolar crisis.",
                        immediateSteps = listOf(
                            "Seek urgent or emergency care immediately—especially with nausea, vomiting, fruity breath, or rapid breathing.",
                            "Follow your clinician's sick-day action plan if you have one."
                        )
                    )
                    else -> null
                }
            }
            VitalType.WEIGHT -> null
        }
    }
}
