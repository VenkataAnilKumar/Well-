package com.example.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class LegalDocumentType(
    val title: String,
    val subtitle: String,
    val publicHttpsUrl: String,
    val lastUpdated: String,
    val sections: List<Pair<String, String>>
) {
    PRIVACY_POLICY(
        title = "Privacy Policy",
        subtitle = "How Well+ protects, stores, and isolates your health records",
        publicHttpsUrl = LegalPolicyConfig.PRIVACY_POLICY_URL,
        lastUpdated = "November 2026",
        sections = listOf(
            "1. Zero-Trust Ownership & Encryption" to
                "Your personal health profile, consultations, check-ins, medications, vitals, mental wellbeing screenings, and optional cycle logs are encrypted in transit over HTTPS/TLS and isolated to your authenticated Google Account UID. No other user can access your records.",
            "2. Ephemeral Photo Analysis" to
                "When you optionally submit a symptom photo, lab document, or prescription bottle label for AI analysis, the image is sent directly for real-time inference and is never persisted to cloud file storage.",
            "3. Zero Advertising or Data Brokering" to
                "Well+ never sells, rents, or shares your health or wellness data with advertisers, data brokers, or third-party marketing platforms.",
            "4. Your Controls & Data Rights" to
                "You can pause Cloud AI analysis at any time in Profile, export all your personal records as JSON or PDF, or permanently delete your account and cloud records either inside the app or via our web deletion portal."
        )
    ),
    TERMS_OF_SERVICE(
        title = "Terms of Service & Medical Disclaimer",
        subtitle = "18+ adult eligibility, non-diagnostic scope, and safe usage rules",
        publicHttpsUrl = LegalPolicyConfig.TERMS_OF_SERVICE_URL,
        lastUpdated = "November 2026",
        sections = listOf(
            "1. Informational Wellness Tool Only (Not a Medical Device)" to
                "Well+ provides AI-assisted wellness information and personal record organization. It does not provide medical diagnoses, prescriptions, dosage adjustments, or emergency clinical care, and is not a substitute for a licensed healthcare professional.",
            "2. Adults 18+ Only" to
                "Well+ is intended strictly for adults aged 18 and older. It must not be used for pediatric dosing or minor medical decision-making.",
            "3. Emergency Situations" to
                "If you experience chest pain, difficulty breathing, stroke symptoms, severe allergic reactions, uncontrolled bleeding, or thoughts of self-harm, call local emergency services or a crisis helpline immediately.",
            "4. Fair-Use Limits & AI Reporting" to
                "To maintain reliability and safety for all users, Cloud AI requests are subject to daily fair-use limits (25/day with smart caching). Please use the 'Report AI Response' control if any AI summary appears inaccurate or unsafe."
        )
    ),
    HEALTH_CONNECT_LIMITED_USE(
        title = "Health Connect Data & Limited Use",
        subtitle = "Strict adherence to Google Play Health Connect Permissions Policy",
        publicHttpsUrl = LegalPolicyConfig.HEALTH_CONNECT_POLICY_URL,
        lastUpdated = "November 2026",
        sections = listOf(
            "1. Scoped Least-Privilege Access" to
                "Well+ requests only the Android Health Connect permissions you explicitly authorize (Steps, Heart Rate, Sleep, and Blood Pressure). You may grant, deny, or revoke any individual type at any time in Android Health Connect settings.",
            "2. Limited Use Commitment" to
                "Information received from Android Health Connect adheres strictly to the Health Connect Permissions Policy, including the Limited Use requirements. Synced wearable data is used solely to display your personal health trends and, when Cloud AI is enabled, generate your requested wellness briefs.",
            "3. No Advertising or Secondary Transfer" to
                "Health Connect records are never used for advertising, credit evaluation, insurance underwriting, or shared with third-party data brokers."
        )
    ),
    WEB_ACCOUNT_DELETION(
        title = "Account & Cloud Data Deletion",
        subtitle = "In-app instant wipe and external web deletion instructions",
        publicHttpsUrl = LegalPolicyConfig.WEB_ACCOUNT_DELETION_URL,
        lastUpdated = "November 2026",
        sections = listOf(
            "1. Instant In-App Deletion" to
                "Open Profile → Delete All Cloud Data & Account to immediately and permanently erase your profile, consultations, chat threads, daily check-ins, medications, vitals, mental screenings, body measurements, cycle logs, and safety reports from Firestore and unlink your account.",
            "2. Web Deletion Request (Without Reinstalling)" to
                "In compliance with Google Play Account Deletion policy, you can also submit a verified account and data deletion request from any browser using our official web deletion page below. Verified web requests are purged within 7 days.",
            "3. Android Health Connect Records" to
                "Deleting your Well+ account clears cloud records stored by Well+. Records stored locally inside Android's system Health Connect store can be managed or deleted directly inside Android Settings → Health Connect."
        )
    )
}

object LegalPolicyConfig {
    // Update these public HTTPS URLs to your verified Play Console production domain before publishing
    const val PRIVACY_POLICY_URL = "https://wellplus.health/privacy"
    const val TERMS_OF_SERVICE_URL = "https://wellplus.health/terms"
    const val HEALTH_CONNECT_POLICY_URL = "https://wellplus.health/health-connect-privacy"
    const val WEB_ACCOUNT_DELETION_URL = "https://wellplus.health/delete-account"

    /**
     * Validates that the target URL strictly uses HTTPS before launching an external browser intent.
     */
    fun openValidatedHttpsUrl(context: Context, rawUrl: String): Boolean {
        val uri = try {
            Uri.parse(rawUrl.trim())
        } catch (_: Exception) {
            return false
        }
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
