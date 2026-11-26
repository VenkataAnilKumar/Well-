# Well+ — QA Test Plan & Critical User Journeys (CUJs)

> **Document Owner:** Quality Assurance & Android Engineering  
> **Target Audience:** QA Engineers, Release Managers, Android/Backend Developers, PM  
> **Version:** 1.0  
> **Last Verified Against Codebase:** November 2026  
> **Status:** Active Release Sign-Off Specification

---

## 1. Executive Summary

This document defines the automated and manual verification strategy required to sign off every **Well+** release candidate. Automated quality gates combine **13 Kotlin/JVM & Robolectric test suites** across `:app`, `:core:*`, and `:feature:*` modules with **12 Firebase Emulator Firestore Security Rules tests** in `firestore.test.js`. Manual verification enforces a **15-CUJ release acceptance matrix** covering authentication, biometric privacy, offline resilience, deterministic red-flag escalation, on-device ML Kit PII photo redaction, Health Connect sync, Doctor Visit SOAP PDFs, Play Billing v9 subscriptions, Glance widgets, and adaptive tablet layouts.

---

## 2. QA Verification Pyramid & Gate Pipeline

```text
                        ┌─────────────────────────────┐
                        │  Manual Exploratory & Store │  Physical Android 10–15
                        │  Pre-Launch Device Sign-Off │  Phones + Foldables/Tablets
                        └──────────────┬──────────────┘
                                       │
                   ┌───────────────────┴───────────────────┐
                   │   15 Critical User Journeys (CUJs)    │  Given / When / Then
                   │  Safety • PII • Billing • Offline • UI│  Release Gate Matrix
                   └───────────────────┬───────────────────┘
                                       │
          ┌────────────────────────────┴────────────────────────────┐
          │         Firestore Security Rules Emulator Suite         │  12 Node.js Tests
          │      (Per-UID Isolation, 18+ Gate, 12 Safety Areas)     │  firestore.test.js
          └────────────────────────────┬────────────────────────────┘
                                       │
  ┌────────────────────────────────────┴────────────────────────────────────┐
  │                 13 Kotlin Unit & Robolectric Test Suites                │  JVM + Robolectric
  │   Safety Engines • Billing/Quota • Models • ViewModels • Navigation     │  Zero Emulator Flake
  └─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Automated Test Inventory (Verified Codebase Suites)

### 3.1 Kotlin Unit & Robolectric Suites (13 Files)

| # | Module | Test Suite File | Primary Verification Coverage |
| :-: | :--- | :--- | :--- |
| 1 | `:app` | `app/src/test/java/com/example/EndToEndWorkflowAuditTest.kt` | End-to-end orchestration across auth, onboarding, triage, tasks, meds, vitals, and export flows. |
| 2 | `:app` | `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Robolectric Android runtime initialization and app-level component wiring. |
| 3 | `:app` | `app/src/test/java/com/example/ExampleUnitTest.kt` | Baseline JVM unit test harness verification. |
| 4 | `:app` | `app/src/test/java/com/example/FreeSmartSuggestionsEngineTest.kt` | Deterministic zero-AI smart daily suggestions and habit nudges. |
| 5 | `:app` | `app/src/test/java/com/example/HealthRegionAndSafetyTest.kt` | US vs. Canada unit conversions, emergency numbers (`911`/`988`/`811`), and region formatting. |
| 6 | `:app` | `app/src/test/java/com/example/ScalingArchitectureTest.kt` | Hot stream sharing, pagination cursors, quota metering (`25` Free / `200` Pro), and caching. |
| 7 | `:core:analytics` | `core/analytics/src/test/java/com/example/core/CycleAndHighlightsModuleTest.kt` | Menstrual cycle phase calculation, trend deltas, and longitudinal highlight generation. |
| 8 | `:core:datastore` | `core/datastore/src/test/java/com/example/core/WidgetSnapshotAndFlagsTest.kt` | Glance widget snapshot serialization, opt-in Medical ID scrubbing on sign-out, and privacy flags. |
| 9 | `:core:model` | `core/model/src/test/java/com/example/data/model/LabBiomarkerAndIntakeModelTest.kt` | Lab biomarker reference range parsing, abnormal status classification, and intake model serialization. |
| 10 | `:core:safety` | `core/safety/src/test/java/com/example/core/ClinicalSafetyEngineModuleTest.kt` | Deterministic 7-category red-flag keyword detection and US/CA critical vital threshold triggers. |
| 11 | `:core:safety` | `core/safety/src/test/java/com/example/core/Phase3SafetyEnginesTest.kt` | `CitationValidator` (10-domain HTTPS allowlist), `ProposedChatActionValidator`, and PII regex redaction engine. |
| 12 | `:feature:consult` | `feature/consult/src/test/java/com/example/feature/consult/ConsultClarifyingAndFollowUpModuleTest.kt` | Adaptive clarifying Q&A flow, follow-up chat state, one-tap action confirmation cards, and TTS triggers. |
| 13 | `:feature:vitals` | `feature/vitals/src/test/java/com/example/feature/vitals/VitalsLabAndReassessmentModuleTest.kt` | Vitals logging, lab scan biomarker extraction mapping, and automated reassessment nudges. |

### 3.2 Firestore Security Rules Test Suite (`firestore.test.js` — 12 Tests)

| Test Group | Enforced Security Contract |
| :--- | :--- |
| **Unauthenticated Denial** | All reads and writes to `users/{uid}/**` are rejected when `request.auth == null`. |
| **Cross-UID Isolation** | Authenticated User A cannot read, create, update, or delete any document under User B's UID path. |
| **HealthProfile Validation** | Enforces 18+ age gate (`isAdultConfirmed`), valid region (`US` / `CA`), and Care Team field bounds. |
| **Consultations & Messages** | Rejects orphaned messages without a valid parent consultation; allows follow-up message appends and status updates. |
| **DailyCheckIn Bounds** | Validates mood, energy, sleep, pain, and physical activity minute bounds. |
| **CareTask & Medications** | Enforces schema integrity for care tasks, medication schedules, and adherence logs. |
| **Vitals & LabBiomarkers** | Validates vital metric types/ranges and lab biomarker structures. |
| **MentalAssessments** | Validates PHQ-9 (0–27) and GAD-7 (0–21) item counts and score ranges. |
| **BodyMeasurements & CycleLogs** | Enforces valid measurement units and menstrual flow/symptom enums. |
| **AiSafetyReports Immutability** | Permits creation across all **12 `AiFeatureArea` enum values** (including `SOAP_PREVISIT_BRIEF` and `CHAT_ACTION_SUGGESTION`); denies client-side edits or deletions after submission. |

### 3.3 Mandatory Pre-Merge & Release Commands

```bash
# 1. Run all 13 Kotlin/JVM & Robolectric test suites across all 23 modules
gradle testDebugUnitTest --continue --console=plain

# 2. Run all 12 Firestore Security Rules tests against local Firestore Emulator
firebase emulators:exec --only firestore "node --test firestore.test.js"

# 3. Verify zero cross-feature module imports (architectural boundary check)
grep -rI 'implementation(project(":feature:' feature/

# 4. Verify release AAB compilation with R8 minification and Crashlytics mapping ID
gradle :app:bundleRelease
```

---

## 4. Manual Release Acceptance CUJ Matrix (15 Critical User Journeys)

Every P0/P1 release candidate requires **100% PASS** on all 15 CUJs below across at least one **Compact phone (Android 14/15)** and one **Expanded tablet/foldable (`>= 600.dp`)**.

| CUJ ID | Journey | Severity | Given / When / Then Acceptance Check |
| :--- | :--- | :---: | :--- |
| **CUJ-01** | **Google Sign-In & 18+ Regional Onboarding** | P0 | **Given** a fresh install, **When** the user signs in via Credential Manager Google Sign-In, confirms 18+, selects `Canada (CA)` (or `US`), and saves Care Team/Profile, **Then** the user lands on Today with Metric/CA (`811`/`988`/`911`) or Imperial/US localization persisted to Firestore. |
| **CUJ-02** | **Biometric Lock & Screen Privacy (`FLAG_SECURE`)** | P0 | **Given** Biometric Lock and Screen Privacy are enabled in Settings, **When** the user backgrounds Well+, inspects Android Recents, and returns, **Then** the Recents preview is masked/blank, screenshots are blocked, and biometric/device-credential authentication is required before health content is visible. |
| **CUJ-03** | **Offline Mode & Sync Recovery** | P0 | **Given** Airplane Mode is turned on while signed in, **When** the user logs a Daily Check-In, marks a Care Task done, and enters chest pain into Triage, **Then** an offline banner appears, writes queue cleanly in Firestore offline cache, deterministic red-flag emergency guidance still fires immediately, and all queued records sync upon reconnecting. |
| **CUJ-04** | **Guided Triage + Clarifying Questions + Red-Flag Interception** | P0 | **Given** the user opens Consult → Guided Triage, **When** (a) they enter non-urgent symptoms ("mild sore throat for 2 days"), **Then** adaptive clarifying questions appear followed by structured urgency guidance; **When** (b) they enter "crushing chest pain radiating to my left arm", **Then** the deterministic `EMERGENCY` red-flag banner intercepts immediately with `ACTION_DIAL` 911 button before any AI delay. |
| **CUJ-05** | **Follow-Up Chat + One-Tap Actions + Citations + TTS** | P0 | **Given** an active consultation, **When** the user sends a follow-up question by voice or text, **Then** the AI reply renders verified HTTPS medical source chips (10-domain allowlist only), tapping **Listen** reads the reply aloud via Android `TextToSpeech`, and any proposed task/med/vital appears as a **one-tap confirmation card** that writes to Firestore **only** after explicit tap. |
| **CUJ-06** | **On-Device ML Kit PII Redaction Preview (All 4 Photo Entry Points)** | P0 | **Given** a sample photo of a lab report or prescription bottle containing visible name, DOB, MRN, phone, and SSN, **When** selected via Camera or Android Photo Picker from (1) Guided Triage, (2) Follow-Up Chat, (3) Rx Bottle Scanner, or (4) Lab Report Scanner, **Then** on-device ML Kit overlays black redaction boxes and shows the **PII Preview Sheet** prior to upload; only the masked bitmap is transmitted to Gemini and zero raw photo is saved to cloud storage. |
| **CUJ-07** | **Medications, Rx Scanner, Interactions & Reminders** | P1 | **Given** the Meds screen, **When** the user scans a prescription label (post-PII preview), confirms the extracted medication/dosage, adds a reminder time, and taps **Check Interactions**, **Then** the med persists to Firestore, WorkManager schedules the notification, adherence logging updates the streak, and interaction guidance displays with non-diagnostic framing. |
| **CUJ-08** | **Vitals, Lab Biomarkers & Critical Vital Alerts** | P0 | **Given** the Vitals screen, **When** the user logs Blood Pressure `186/122 mmHg` (hypertensive crisis threshold) or SpO2 `88%`, **Then** a deterministic critical vital alert fires immediately with urgent care / `911` guidance; **When** they scan a CBC/Metabolic lab sheet, **Then** extracted biomarkers (`HbA1c`, `LDL`, etc.) save to `labBiomarkers` with Normal/High/Low status chips. |
| **CUJ-09** | **Mental Health PHQ-9 / GAD-7 Crisis Interception** | P0 | **Given** the Mental Health check-in screen, **When** the user answers PHQ-9 Item 9 (self-harm ideation) with any score `> 0`, **Then** the `988` Suicide & Crisis Lifeline banner appears immediately with one-tap `ACTION_DIAL` access, regardless of subscription tier or network status. |
| **CUJ-10** | **Android Health Connect Sync & Write-Back** | P1 | **Given** Health Connect is installed with sample Steps, HR, Sleep, and BP records, **When** the user opens Settings → Health Connect, views the rationale, grants permissions, and taps **Sync Now**, **Then** records populate Today and Vitals without duplicates, and logging a new BP reading in Well+ writes back cleanly to Health Connect. |
| **CUJ-11** | **Doctor Visit SOAP PDF Export & Offline Fallback** | P1 | **Given** a user with logged consults, meds, vitals, and labs, **When** they tap **Export Doctor Visit PDF** (a) online and (b) offline, **Then** (a) an AI-synthesized SOAP Pre-Visit Brief PDF opens via Android Share Sheet with disclaimer footer and (b) offline gracefully generates the deterministic non-AI structured clinical summary PDF without crashing. |
| **CUJ-12** | **Well+ Pro Play Billing Purchase, Quota Upgrade & Restore** | P0 | **Given** a Free user (`25` AI requests/day) on a Play License Tester account, **When** they upgrade to `wellplus_pro_annual` or `wellplus_pro_monthly`, **Then** quota updates to `200/day`, Settings shows active Pro badge, and reinstalling + tapping **Restore Purchases** restores Pro entitlement cleanly. Also verify safety red-flag alerts still trigger if quota hits `0`. |
| **CUJ-13** | **AI Safety Reporting Across 12 Areas** | P1 | **Given** any AI output card (Triage, Follow-Up Chat, Action Card, Lab Scan, Rx Scan, SOAP Brief, etc.), **When** the user taps **Report AI Safety Issue**, selects a reason, and submits, **Then** an immutable `aiSafetyReports` document is written to Firestore with the matching `AiFeatureArea` and confirmation toast is shown. |
| **CUJ-14** | **Emergency Medical ID, Opt-In Widget & Sign-Out Scrub** | P0 | **Given** the user populates Emergency Medical ID (allergies, blood type, emergency contact), **When** they add all 3 Glance widgets (Daily Sanctuary, Med Adherence, Emergency Medical ID), **Then** the Medical ID widget shows a privacy-locked placeholder until the user explicitly opts in via Settings; **When** the user signs out, **Then** all 3 widgets scrub personal health data immediately. |
| **CUJ-15** | **JSON Export, Account Deletion & Adaptive Tablet Layout** | P1 | **Given** a tablet/foldable (`>= 600.dp` width), **When** the user navigates all tabs, **Then** `NavigationRail` replaces the bottom `NavigationBar`, content constrains cleanly to `680.dp` max width, TalkBack reads all controls with `>= 48.dp` targets, **Export All Data (JSON)** exports a valid portable bundle, and **Delete Account & All Data** wipes all 13 collections and signs out cleanly. |

---

## 5. Device, OS & Localization Test Matrix

| Dimension | Minimum Required Coverage per Release |
| :--- | :--- |
| **Android API Levels** | Android 10 (API 29), Android 13 (API 33 — legacy Health Connect rationale & notification runtime permission), Android 14 (API 34 — system Health Connect & `VIEW_PERMISSION_USAGE`), Android 15/16 (API 35/36 — strict edge-to-edge insets). |
| **Form Factors** | Compact Phone (`360dp–412dp`), Large Phone (`412dp+`), Foldable/Tablet (`>= 600dp` width in Portrait & Landscape). |
| **Accessibility Modes** | TalkBack screen reader enabled, System Font Scale `1.3x` and `1.5x`, Dark Mode & Light Mode, Color-blindness visual check (shape + icon + text urgency badges). |
| **Regional Modes** | `US` profile (`911`, `988`, `1-800-222-1222`, `°F`, `lbs`, `mg/dL`, US Insurance) and `CA` profile (`911`, `988`, `811`, `°C`, `kg`, `mmol/L`, Provincial Health Card). |

---

## 6. Defect Severity & Release Exit Criteria

| Severity | Definition | Release Policy |
| :--- | :--- | :--- |
| **P0 — Blocker / Safety** | Any crash on startup, missed red-flag/critical-vital alert, PII leak bypassing ML Kit preview, cross-UID data access, broken Billing acknowledgement, or Play Policy violation. | **Zero open P0s allowed.** Immediate hotfix / rollout halt. |
| **P1 — Major** | Broken core CUJ without workaround, broken Health Connect sync, PDF generation failure, or accessibility failure (`< 48.dp` target or unlabeled primary action). | **Zero open P1s allowed** for Production; may ship to Internal only while fixing. |
| **P2 — Minor** | Non-blocking layout polish, minor spacing/typography drift, or edge-case copy tweak. | May ship with tracked backlog ticket. |

**Release Sign-Off Gate:**
1. `gradle testDebugUnitTest --continue` passes **100%** across all 13 suites.
2. `firebase emulators:exec --only firestore "node --test firestore.test.js"` passes **12/12**.
3. Cross-feature `grep` returns **zero** violations.
4. All **15 manual CUJs** recorded as `PASS` in the release sign-off log.
5. Play Console Pre-Launch Report shows **zero crashes** and **zero policy warnings**.

---

## 7. Cross-References

- [Master Docs Hub (`README.md`)](./README.md)
- [End-to-End Feature Spec & User Flows](./FEATURE_SPEC_AND_USER_FLOWS.md)
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [Google Play Launch & Policy Checklist](./PLAY_STORE_LAUNCH_CHECKLIST.md)
- [Architecture & Data Model Reference](./ARCHITECTURE_AND_DATA_MODEL.md)
