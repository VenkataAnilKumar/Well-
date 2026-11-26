package com.example.core

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

data class RegionalEmergencyProfile(
    val countryCode: String,
    val countryName: String,
    val emergencyNumber: String,
    val emergencyLabel: String,
    val crisisNumber: String,
    val crisisLabel: String,
    val nurseAdviceNumber: String? = null,
    val nurseAdviceLabel: String? = null,
    val internationalCrisisDirectoryUrl: String = "https://findahelpline.com/"
)

/**
 * Global emergency and crisis helpline directory supporting automatic SIM/locale detection
 * plus user-configurable country overrides for international launches.
 */
object RegionalEmergencyDirectory {

    val supportedRegions: List<RegionalEmergencyProfile> = listOf(
        RegionalEmergencyProfile("US", "United States", "911", "Emergency Services (911)", "988", "988 Suicide & Crisis Lifeline"),
        RegionalEmergencyProfile("CA", "Canada", "911", "Emergency Services (911)", "988", "988 Suicide Crisis Helpline", "811", "811 Free 24/7 Nurse Advice Line"),
        RegionalEmergencyProfile("GB", "United Kingdom", "999", "Emergency Services (999)", "111", "NHS 111 Mental Health / 116 123 Samaritans"),
        RegionalEmergencyProfile("IE", "Ireland", "112", "Emergency Services (112 / 999)", "116123", "Samaritans Ireland (116 123)"),
        RegionalEmergencyProfile("IN", "India", "112", "Emergency Services (112 / 108)", "14416", "Tele-MANAS Crisis Line (14416)"),
        RegionalEmergencyProfile("AU", "Australia", "000", "Emergency Services (000)", "131114", "Lifeline Australia (13 11 14)"),
        RegionalEmergencyProfile("NZ", "New Zealand", "111", "Emergency Services (111)", "1737", "Need to Talk? (1737)"),
        RegionalEmergencyProfile("DE", "Germany", "112", "Notruf (112)", "08001110111", "TelefonSeelsorge (0800 111 0 111)"),
        RegionalEmergencyProfile("FR", "France", "112", "SAMU / Urgences (112 / 15)", "3114", "Numéro National Prévention Suicide (3114)"),
        RegionalEmergencyProfile("ES", "Spain", "112", "Emergencias (112)", "024", "Línea 024 Atención Conducta Suicida"),
        RegionalEmergencyProfile("IT", "Italy", "112", "Emergenze (112)", "0223272327", "Telefono Amico (02 2327 2327)"),
        RegionalEmergencyProfile("NL", "Netherlands", "112", "Alarmnummer (112)", "113", "113 Zelfmoordpreventie"),
        RegionalEmergencyProfile("SE", "Sweden", "112", "SOS Alarm (112)", "90101", "Mind Självmordslinjen (90101)"),
        RegionalEmergencyProfile("CH", "Switzerland", "112", "Emergency Services (112 / 144)", "143", "Die Dargebotene Hand (143)"),
        RegionalEmergencyProfile("JP", "Japan", "119", "Ambulance & Fire (119)", "0120783556", "Yorisoi Hotline (0120-279-338)"),
        RegionalEmergencyProfile("KR", "South Korea", "119", "Emergency Services (119)", "109", "Suicide Prevention Hotline (109)"),
        RegionalEmergencyProfile("SG", "Singapore", "995", "SCDF Ambulance (995)", "1767", "Samaritans of Singapore (1767)"),
        RegionalEmergencyProfile("BR", "Brazil", "192", "SAMU (192)", "188", "CVV Crisis Support (188)"),
        RegionalEmergencyProfile("MX", "Mexico", "911", "Emergencias (911)", "8009112000", "Línea de la Vida (800 911 2000)"),
        RegionalEmergencyProfile("ZA", "South Africa", "10177", "Ambulance (10177 / 112)", "0800567567", "SADAG Crisis Line (0800 567 567)"),
        RegionalEmergencyProfile("AE", "United Arab Emirates", "998", "Ambulance (998)", "8004673", "National Support Line (800-HOPE)")
    )

    private val defaultInternational = RegionalEmergencyProfile(
        countryCode = "INTL",
        countryName = "International (Default)",
        emergencyNumber = "112",
        emergencyLabel = "Local Emergency Services (112)",
        crisisNumber = "",
        crisisLabel = "Find Local Crisis Helpline (findahelpline.com)"
    )

    fun detectCountryCode(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val networkIso = tm?.networkCountryIso?.trim()?.uppercase(Locale.US).orEmpty()
        if (networkIso.length == 2) return networkIso
        val simIso = tm?.simCountryIso?.trim()?.uppercase(Locale.US).orEmpty()
        if (simIso.length == 2) return simIso
        val localeIso = Locale.getDefault().country.trim().uppercase(Locale.US)
        if (localeIso.length == 2) return localeIso
        return "INTL"
    }

    fun resolve(context: Context, overrideCode: String = ""): RegionalEmergencyProfile {
        val normalizedOverride = overrideCode.trim().uppercase(Locale.US)
        val targetCode = if (normalizedOverride.isNotEmpty() && normalizedOverride != "AUTO") {
            normalizedOverride
        } else {
            detectCountryCode(context)
        }
        return supportedRegions.firstOrNull { it.countryCode == targetCode } ?: defaultInternational
    }
}
