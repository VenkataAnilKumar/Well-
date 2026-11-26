# Well+ — Master Product Requirements Document (PRD)

> **Document Owner:** Product Management  
> **Target Audience:** Product, Engineering, Design, QA, Legal/Compliance  
> **Product Version:** 1.0 (Phase 3 Complete — Production Baseline)  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Shipped

---

## 1. Executive Summary

**Well+** is an Android personal health sanctuary and AI clinical companion built for adults (`18+`) in the **United States** and **Canada**. It unifies daily preventive tracking (mood, sleep, hydration, physical activity, medications, vitals, lab biomarkers, mental wellbeing, body & cycle, and Android Health Connect) with context-aware **Firebase AI Logic (Gemini)** symptom triage, photo/lab/prescription scanning, follow-up chat with one-tap actions, and clinician-ready **SOAP Doctor Visit PDF** exports.

Well+ is strictly a **non-diagnostic wellness and care-preparation companion**. Every AI surface is wrapped in deterministic offline clinical safety guardrails, on-device ML Kit PII redaction before image upload, allowlist-validated medical references, and zero-trust per-user cloud isolation.

---

## 2. Problem Statement & Core Value Proposition

| User Pain Point | How Well+ Solves It |
| :--- | :--- |
| Health data is fragmented across wearables, paper lab reports, pill bottles, and symptom notes. | Consolidates daily check-ins, Health Connect metrics, medications, vitals, lab biomarkers, PHQ-9/GAD-7 screenings, and consultations into one encrypted personal health sanctuary. |
| Generic web symptom searches ignore personal context (allergies, prescriptions, baseline vitals) and trigger anxiety. | Injects the user's real Health Profile, active medications, and recent vitals into structured Gemini clinical triage with calm urgency bands (`Self-Care`, `Doctor Soon`, `Urgent`, `Emergency`) and official public-health citations. |
| Users hesitate to upload prescription labels or lab printouts to cloud AI due to exposed SSN/MRN/DOB details. | Runs **on-device ML Kit OCR PII redaction** locally before upload and shows a visual redaction preview sheet (`Auto-Mask PII`). |
| Clinician appointments are short and patients struggle to summarize weeks of symptoms, vitals, and lab trends. | Generates a **Doctor Visit PDF** featuring an optional 1-page AI **SOAP Pre-Visit Clinical Brief**, active medications, vitals summary, recent lab biomarkers, and consultation history. |
| Cross-border North American users face mismatched units, coverage fields, and emergency/crisis numbers. | Ships native **US (`US`)** and **Canada (`CA`)** health region profiles (`°F`/`mg/dL`/`lbs` vs. `°C`/`mmol/L`/`kg`, US Insurance vs. Provincial Health Card, `911`/`988` + Canada `811` Nurse Line). |

---

## 3. Target Personas

1. **Maya (34, Proactive Care & Chronic Condition Manager — US or Canada)**
   - Tracks mild asthma, daily medications, blood pressure, and annual bloodwork (HbA1c, Vitamin D, LDL).
   - Uses Well+ to scan new prescriptions for drug–allergy interactions, track daily adherence, and export a 30-day SOAP PDF before visiting her primary care clinician or family doctor.
2. **David (48, Acute Symptom Checker & Caregiver)**
   - Opens Well+ when new symptoms arise, uses voice dictation and adaptive clarifying questions during Guided Symptom Intake, listens to the triage readout via on-device TTS, and confirms one-tap care actions directly from Follow-Up Chat.
3. **Priya (29, Holistic Wellness & Mental Wellbeing Tracker)**
   - Logs daily mood/sleep/activity check-ins, syncs Health Connect steps/heart rate/sleep in the background, completes periodic PHQ-9/GAD-7 screenings, tracks optional cycle logs, and uses Home Screen widgets for frictionless daily adherence.

---

## 4. Regional Localization Matrix (US & Canada)

| Capability | United States (`US`) | Canada (`CA`) |
| :--- | :--- | :--- |
| **Temperature Unit** | `°F` (Fahrenheit) | `°C` (Celsius) |
| **Blood Glucose Unit** | `mg/dL` | `mmol/L` |
| **Body Weight Unit** | `lbs` (metric toggle supported) | `kg` (metric default) |
| **Primary Clinician Label** | Primary Care Doctor | Family Doctor / Clinic |
| **Coverage Field Label** | Health Insurance Plan & Member ID | Provincial Health Card (e.g., OHIP, MSP, AHS) |
| **Emergency Services** | `911` | `911` |
| **Mental Health Crisis Line** | `988` Suicide & Crisis Lifeline | `988` Suicide Crisis Helpline |
| **Non-Emergency Nurse Triage** | Regional Urgent Care / Primary Care | `811` HealthLink / Info-Santé Nurse Line |
| **Fallback Trusted Citations** | NIH MedlinePlus, CDC, AHA | NIH MedlinePlus, Health Canada (`canada.ca`), AHA |

---

## 5. Free vs. Well+ Pro Tier Matrix

> [!IMPORTANT]
> Core clinical safety features—including deterministic emergency red-flag alerts, critical vital warnings, Emergency Medical ID, and on-device ML Kit PII redaction—are **100% free forever** on every tier and are never paywalled.

| Capability | Well+ Free | Well+ Pro (Google Play Billing v9) |
| :--- | :---: | :---: |
| **Daily Cloud AI Quota** | **25 requests / day** | **200 requests / day** |
| **Guided Symptom & Photo Triage** | Included (within daily quota) | Expanded quota |
| **Follow-Up Chat + One-Tap Action Cards** | Included (action extraction uses 0 extra credits) | Expanded quota |
| **Trusted Medical Citations & On-Device TTS** | Included | Included |
| **On-Device ML Kit PII Redaction Preview** | **Always Free** | **Always Free** |
| **Drug–Drug & Drug–Allergy Interaction Check** | Included (within daily quota) | Expanded quota |
| **Prescription Bottle & Lab Report Vision Scan** | Included (within daily quota) | Expanded quota |
| **Doctor Visit PDF + AI SOAP Pre-Visit Brief** | Included (within daily quota; offline fallback free) | Priority expanded quota |
| **Health Connect Sync, Vitals, Meds, PHQ-9/GAD-7, Widgets** | **Unlimited & Free** | **Unlimited & Free** |
| **Emergency Escalation & Emergency Medical ID** | **Always Free** | **Always Free** |

---

## 6. Functional Requirements Overview

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                         WELL+ CORE PRODUCT PILLARS                         │
├────────────────────┬─────────────────────────┬─────────────────────────────┤
│ 1. Daily Sanctuary │ 2. Clinical AI Doctor   │ 3. Longitudinal Care &      │
│    & Tracking      │    & Vision Safety      │    Emergency Readiness      │
├────────────────────┼─────────────────────────┼─────────────────────────────┤
│ • Today Hub & Mood │ • 4-Step Guided Intake  │ • 30-Day Longitudinal AI    │
│ • Daily Check-In   │ • Clarifying Questions  │   Synthesis                 │
│ • Care Checklist   │ • Multimodal Photo/Lab  │ • Doctor Visit PDF + 1-Page │
│ • Meds & Reminders │ • Streaming Chat + TTS  │   AI SOAP Brief + Labs      │
│ • Vitals & Trends  │ • One-Tap Action Cards  │ • Emergency Escalation Sheet│
│ • Lab Biomarkers   │ • Verified Citations    │ • High-Contrast Medical ID  │
│ • PHQ-9 & GAD-7    │ • Rx Bottle Scanner     │ • Opt-In Medical ID Widget  │
│ • Body & Cycle     │ • Drug–Allergy Checker  │ • Biometric & Screen Lock   │
│ • Health Connect   │ • On-Device ML Kit PII  │ • JSON Export & Full Wipe   │
│ • 3 Glance Widgets │   Redaction Preview     │ • Well+ Pro Play Billing v9 │
└────────────────────┴─────────────────────────┴─────────────────────────────┘
```

### Key Functional Requirements
- **FR-01 Authentication & 18+ Onboarding:** Require Google Sign-In via Jetpack Credential Manager (anonymous sign-in prohibited). Gate first-time users behind explicit `18+` age confirmation and non-diagnostic medical disclaimer acceptance before entering the workspace.
- **FR-02 Guided Triage & Clarifying Questions:** Capture body area, severity (`1–10`), duration, associated symptoms, optional voice dictation, optional PII-redacted photo, and 2–3 dynamic clarifying questions prior to generating a structured triage report.
- **FR-03 Context-Aware Follow-Up Chat & Agentic Actions:** Stream doctor responses grounded in the consultation, Health Profile, active medications, and recent vitals. After each turn, propose 0–3 pre-validated one-tap confirmation cards (`Add Care Task`, `Add Medication`, `Log Vital`, `Schedule 48h Follow-Up`) without consuming a second daily AI credit.
- **FR-04 Trusted Medical Citations & Voice Readout:** Display up to 4 allowlist-validated HTTPS medical citations (`medlineplus.gov`, `nih.gov`, `cdc.gov`, `mayoclinic.org`, `nhs.uk`, `canada.ca`, `who.int`, `clevelandclinic.org`, `heart.org`, `diabetes.org`) and provide lifecycle-safe on-device `TextToSpeech` playback (`Listen` / `Stop`).
- **FR-05 On-Device ML Kit PII Redaction:** Before uploading any photo from Symptom Intake, Follow-Up Chat, Prescription Scanner, or Lab Report Scanner, scan locally with ML Kit Text Recognition v2, black-box detected SSN/SIN, MRN/Patient ID, phone, email, and DOB on a mutable bitmap copy, and present an interactive preview sheet (`Use Redacted` / `Use Original` / `Cancel`).
- **FR-06 Medications, Vitals & Lab Biomarkers:** Support medication schedules, dose adherence logs, WorkManager daily reminders with notification quick actions (`Mark Taken` / `Snooze`), Rx bottle vision scanning, drug–drug/allergy checks, 6 vital types with interactive charts, abnormal vital proactive reassessment, and structured lab biomarker extraction & history.
- **FR-07 Mental Wellbeing, Body & Cycle, Health Connect:** Support validated PHQ-9 & GAD-7 screenings (with immediate crisis hotline banner when item 9 or severe score triggers), BMI/body tracking, optional menstrual cycle logging, and bidirectional Android Health Connect sync (manual + 6-hour periodic background sync).
- **FR-08 Clinician Visit Export:** Export a multi-page `Doctor Visit PDF` with selectable sections (1-page AI SOAP Pre-Visit Brief, Profile/Care Team, Active Medications, Vitals, Recent Lab Biomarkers, Consultation History) that falls back silently to deterministic export if offline or quota-limited.
- **FR-09 Emergency Medical ID & Home Screen Widgets:** Provide a high-contrast `Emergency Medical ID` screen (blood type, allergies, active meds, conditions, 1-tap `ACTION_DIAL` for ICE contact and local emergency services, text share) plus 3 Glance widgets (Quick Mood, Next Med Dose, and privacy-first opt-in Medical ID defaulting to OFF and wiped on sign-out).
- **FR-10 Subscriptions & Entitlements:** Integrate Google Play Billing Library v9 for Well+ Pro Monthly and Annual subscriptions, scaling daily AI quota immediately from 25/day to 200/day with purchase acknowledgment and subscription restoration.

---

## 7. Non-Functional Requirements (NFRs)

| Category | Requirement |
| :--- | :--- |
| **Offline Resilience** | All deterministic trackers, cached Firestore reads, Emergency Medical ID, and deterministic clinical red-flag checks must operate fully offline; clear offline banner surfaces when disconnected. |
| **Performance & Startup** | Offload NotificationChannel setup and Health Connect binding off the Main thread; reuse 1 hot Firestore listener per collection via `WellPlusAppContainer`; paginate Consultations and Vitals history. |
| **Security & Privacy** | Enforce zero-trust per-UID Firestore security rules, Firebase App Check, `android:allowBackup="false"`, optional Biometric Lock, `FLAG_SECURE`/recents screenshot masking, and ephemeral photo processing (zero raw photo bucket storage). |
| **Accessibility** | Meet Material 3 `48.dp` minimum touch targets, semantic `contentDescription` labels, tabular numerals (`tnum`) for live metrics, WCAG AA/AAA text contrast, and system font scaling. |
| **Adaptive Layout** | Support Compact phones (floating Bottom Bar) and Medium/Expanded tablets & foldables (`>= 600.dp` Navigation Rail, centered `680.dp` max content width). |
| **Modularity** | Maintain strict unidirectional dependency graph across 23 modules with zero cross-feature module imports. |

---

## 8. Product KPIs & North Star Metrics

| Metric Tier | KPI | Definition / Target Signal |
| :--- | :--- | :--- |
| **North Star** | **Weekly Active Care Actions (WACA)** | Unique signed-in users completing >= 3 meaningful health actions/week (check-in, med dose logged, vital/lab logged, consultation, or PDF export). |
| **Activation** | **Onboarding & First Value Rate** | % of authenticated users completing 18+ safety onboarding and logging their first check-in, medication, or symptom check within 24 hours. |
| **Clinical Engagement** | **Follow-Up Action Conversion** | % of AI-proposed one-tap chat action cards confirmed and saved by users (target >= 35%). |
| **Privacy & Trust** | **PII Redaction Adoption** | % of Lab/Rx scans uploaded using the auto-masked redacted bitmap when PII is detected (target >= 90%). |
| **Monetization** | **Free-to-Pro Conversion & D30 Retention** | % of active users hitting >= 60% daily AI quota who convert to Well+ Pro Annual/Monthly; D30 Pro retention. |
| **Safety Reliability** | **Zero Missed Critical Alerts** | 100% pass rate on deterministic `ClinicalSafetyEngine` red-flag/vital threshold unit tests and Firestore rules isolation tests. |

---

## 9. Release Acceptance Criteria (Given / When / Then)

- **AC-01 Safety Gate:** *Given* a newly authenticated user, *when* they sign in for the first time, *then* they cannot access Today/Consult/Meds until confirming `18+` age and accepting the non-diagnostic disclaimer.
- **AC-02 Deterministic Red Flag:** *Given* a user enters "crushing chest pain radiating to left arm" or logs BP `185/122 mmHg` (even offline), *when* submitted, *then* an immediate high-contrast emergency modal opens with localized `ACTION_DIAL` emergency actions.
- **AC-03 On-Device PII Masking:** *Given* `Auto-Mask PII on Photo Scans` is ON (default) and a user picks or captures a lab report containing an SSN or DOB, *when* previewed, *then* the redaction sheet displays black-masked overlays locally before any Gemini Vision call occurs.
- **AC-04 Action Card Validation:** *Given* Follow-Up Chat proposes a vital or medication action, *when* the user taps confirm once, *then* the record persists to Firestore passing all schema bounds and updates the card state to `Added`.
- **AC-05 Pro Quota Scaling:** *Given* a user has an active acknowledged Well+ Pro Play subscription, *when* entitlement state refreshes, *then* daily AI quota scales immediately from `25/day` to `200/day` without requiring an app restart.
- **AC-06 Medical ID Widget Privacy:** *Given* a user opts in to the Home Screen Medical ID widget and later signs out, *when* sign-out completes, *then* all Medical ID fields in local widget storage are immediately scrubbed and the widget reverts to its locked off state.

---

## Cross-References
- [Monetization & GTM](./MONETIZATION_AND_GTM.md)
- [End-to-End Feature Spec & User Flows](./FEATURE_SPEC_AND_USER_FLOWS.md)
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [Architecture & Data Model](./ARCHITECTURE_AND_DATA_MODEL.md)
