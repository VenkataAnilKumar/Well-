# Well+ — End-to-End Feature Specification & User Flows

> **Document Owner:** Product Management & UX Design  
> **Target Audience:** PM, Design, Android Engineering, QA  
> **Product Version:** 1.0 (14 Screens + 3 Home Screen Widgets)  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Verified

---

## 1. Executive Summary

This specification documents every user-facing screen, navigation transition, interactive control, loading/empty/error state, and edge case across **Well+**. Navigation uses a single-activity Jetpack Compose `NavHost` with 5 top-level tabs (**Today**, **Meds**, **Consult**, **History**, **Profile**), 7 secondary detail screens (**Daily Check-In**, **Vitals & Labs**, **Mental Wellbeing**, **Body & Cycle**, **Health Connect**, **Well+ Pro**, **Emergency Medical ID**), full-screen **CameraX** capture with on-device **ML Kit PII Redaction Preview**, and global clinical safety overlays.

---

## 2. Master Navigation & Gate Flow

```text
App Launch -> MainActivity (App Check + Edge-to-Edge + Screen Privacy)
                   │
                   ▼
         ┌───────────────────┐  Signed Out  ┌──────────────────────────┐
         │ Firebase Auth     ├─────────────▶│ AuthScreen               │
         │ State Listener    │              │ (Google Credential Mgr)  │
         └─────────┬─────────┘              └──────────────────────────┘
                   │ Signed In
                   ▼
         ┌───────────────────┐  Locked      ┌──────────────────────────┐
         │ Biometric Lock    ├─────────────▶│ BiometricLockScreen      │
         │ Gate (Optional)   │              │ (Biometric / Device PIN) │
         └─────────┬─────────┘              └──────────────────────────┘
                   │ Unlocked
                   ▼
         ┌───────────────────┐  Incomplete  ┌──────────────────────────┐
         │ 18+ & Disclaimer  ├─────────────▶│ OnboardingScreen         │
         │ Onboarding Gate   │              │ (Profile + 18+ + Consent)│
         └─────────┬─────────┘              └──────────────────────────┘
                   │ Ready (consumes PendingDeepLink if present)
                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Primary 5-Tab Workspace                              │
│  [Today]       [Meds]        [Consult]       [History]       [Profile]  │
│     │             │              │               │               │      │
│     ├─►Check-In   └─►Rx Scan     ├─►Guided       └─►SOAP PDF     ├─►Pro │
│     ├─►Vitals &       + Inter-   │  Intake          Export       └─►Med │
│     │  Labs           actions    ├─►Report                          ID  │
│     ├─►Mental                    └─►Follow-Up                           │
│     ├─►Body/Cycle                   Chat + Actions                      │
│     └─►Health Connect                                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Deep Links Handled After Gates Clear
- `sana_open_destination = "medications"` → **Meds** tab
- `sana_open_destination = "check_in"` → **Today** → **Daily Check-In**
- `sana_open_destination = "follow_up"` (+ optional `consultationId`) → opens Consultation Report in **Consult** (or **History** if ID absent)
- `sana_open_destination = "emergency_id"` (from Emergency Medical ID Widget) → **Profile** → **Emergency Medical ID**

---

## 3. Screen-by-Screen Feature Specifications

### 3.1 Authentication & First-Run Safety Onboarding
- **AuthScreen:** Single-tap **Sign in with Google** via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`). Displays non-diagnostic positioning copy and privacy trust badges. Anonymous sign-in is disabled.
- **OnboardingScreen:** Collects preferred display name, optional age/biological sex/blood type, known allergies, current medications, conditions, emergency contact, and preferred US/Canada region.
  - **Hard Gates:** Primary CTA remains disabled until both `ageConfirmed18Plus` (`I confirm I am 18+ years old`) and `disclaimerAccepted` (`Well+ provides informational guidance, not medical diagnosis`) are explicitly checked.

### 3.2 Today Hub (`TodayScreen`) & Daily Check-In (`DailyCheckInScreen`)
- **Today Hub:**
  - Personalized greeting with date key and subtle `OfflineStatusBanner` / Remote Config urgent safety notice when active.
  - **Quick Mood Strip:** 5 one-tap mood levels (`Low`, `Tense`, `Steady`, `Calm`, `Thriving`) that immediately upsert today's `DailyCheckIn` while preserving sleep, hydration, activity, and reflection notes.
  - **AI Daily Summary Micro-Insight:** On-demand cached Gemini card (`gemini-3.5-flash-lite`) summarizing today's mood, sleep, meds, and vitals, with dismiss and AI Safety Report footer.
  - **Quick Action Cards:** Entry points for **Symptom Check**, **Photo/Lab Analysis**, **Daily Check-In**, **Vitals & Labs**, **Mental Wellbeing**, **Body & Cycle**, **Health Connect**, and **Doctor Visit PDF Export**.
  - **Today Medications Card:** Live adherence progress bar + 1-tap `Taken` / `Skip` dose logging.
  - **Interactive Care Checklist (`CareTask`):** Add custom tasks, toggle completion, delete items, and see items promoted from AI consultation suggestions or chat action cards.
- **Daily Check-In Screen:** Full editor for mood (`1–5`), energy (`1–5`), sleep hours (`0–24`), hydration glasses (`0–30`), physical activity level (`Sedentary` → `Vigorous`), active minutes (`0–600`), health signals, and freeform reflection notes + 31-day history trend.

### 3.3 Guided Symptom & Photo Consult (`ConsultScreen`, `ConsultChatAndReport`)

```text
Step 1: Area & Symptoms ──► Step 2: Severity & Timing ──► Step 3: Photo (+ PII Mask)
                                                                      │
                                                                      ▼
Report View ◄── Gemini Triage (gemini-3.8-flash) ◄── Step 4: Clarifying Qs & Review
  │   ▲
  │   └──── BackHandler / Report Toggle
  ▼
Follow-Up Chat (Streaming) ──► Post-Turn Action Extractor (0 extra AI credits)
                                 └─► 1-Tap Confirmation Cards (Task/Med/Vital/48h Nudge)
```

- **4-Step Guided Intake:**
  - Supports **Symptom Check** or **Photo / Lab Analysis** mode.
  - Zero-permission Android system voice dictation (`RecognizerIntent`) in symptom fields.
  - **Adaptive Clarifying Questions (Step 4):** Optional AI call generating 2–3 focused clinical clarifying questions with tappable chips to sharpen triage precision.
  - **Deterministic Red-Flag Interception:** Runs offline via `ClinicalSafetyEngine.evaluateSymptoms()` *before* or alongside AI analysis. Any cardiac, respiratory, stroke, anaphylaxis, severe trauma/bleeding, poisoning/overdose, or mental-health crisis match immediately raises `SymptomRedFlagDialog`.
- **Consultation Report View:**
  - Displays urgency badge (`Self-Care`, `Doctor Soon`, `Urgent`, `Emergency`), AI summary, possible educational causes, actionable care suggestions (each with 1-tap **Add to Care Checklist**), red-flag warnings, and doctor visit questions.
  - **On-Device TTS Readout (`Listen` / `Stop` pill):** Speaks the report summary and key care steps aloud via Android `TextToSpeech` with lifecycle auto-stop on screen exit.
  - **Trusted Medical Citations:** Displays up to 4 allowlist-validated HTTPS reference cards opening safely in Android Custom Tabs / browser.
  - **Symptom Recovery Outcome Tracking:** Users mark `Improving`, `Unchanged`, `Worsening`, or `Resolved` and schedule a 48h follow-up reminder. Marking **Worsening** triggers optional **Proactive AI Reassessment**.
  - **Emergency Auto-Escalation:** Any `Urgent` or `Emergency` triage automatically surfaces `EmergencyEscalationSheet` with localized 1-tap dialing.
- **Follow-Up Chat View:**
  - Multi-turn streaming Gemini chat grounded in the consultation, Health Profile, active medications, and recent vitals.
  - Supports ephemeral photo attachments (routed through **ML Kit PII Redaction Preview**), voice dictation, per-reply **Listen/Stop TTS** pills, verified citations, and AI Safety Reporting.
  - **One-Tap AI Action Cards (`ProposedChatActionCard`):** After each doctor reply, `ChatActionAndCitationAiService` (`gemini-3.5-flash-lite`, zero extra daily quota) proposes 0–3 sanitized actions:
    1. `ADD_CARE_TASK` → Saves directly to `careTasks`
    2. `ADD_MEDICATION` → Saves directly to `medications`
    3. `LOG_VITAL` → Validates physiological bounds & saves to `vitals` (plus triggers critical-vital alert if out of crisis bounds)
    4. `SCHEDULE_FOLLOWUP` → Schedules 48-hour WorkManager follow-up nudge
    Each card shows explicit **Confirm** and **Dismiss** buttons; confirmed cards flip to `Added` and prevent duplicate writes.

### 3.4 Medications & Prescription Safety (`MedicationsScreen`)
- **Medication Manager:** Add/edit/pause/delete prescriptions and supplements with dosage, schedule slot (`Morning`, `Midday`, `Evening`, `Bedtime`, `As Needed`), reminder time (`HH:mm`), and intake instructions.
- **WorkManager Reminders:** Toggle daily reminders, send test reminder notification, and handle notification quick actions (`Mark Taken`, `Snooze 15m`) via `MedicationActionReceiver`.
- **Prescription Bottle Vision Scanner:** Launch CameraX or Android Photo Picker → auto-run **ML Kit PII Redaction Preview** → extract medication name, dosage, slot, and instructions into a pre-filled editable draft (`MedicationAiService`).
- **Drug–Drug & Drug–Allergy Interaction Checker:** Evaluates active medications against recorded allergies and ongoing conditions, returning `Safe`, `Caution`, or `Ask Pharmacist` badges with dose-spacing guidance and safety reporting.

### 3.5 Vitals & Lab Biomarkers (`VitalsScreen`, `VitalsLabBiomarkersSection`)
- **Segmented Hub:** Switches cleanly between **Vitals** and **Lab Biomarkers** (automatically switching to **Lab Biomarkers** when returning from Lab CameraX capture).
- **Vitals Tracking:** Logs Blood Pressure (`mmHg`), Heart Rate (`bpm`), Temperature (`°F`/`°C`), SpO₂ (`%`), Weight (`lbs`/`kg`), and Blood Glucose (`mg/dL`/`mmol/L`) with interactive touch-scrubbed trend charts and paginated history.
  - Critical values immediately raise `CriticalVitalAlertDialog` (offline deterministic check).
  - Non-critical abnormal vitals offer optional **Proactive AI Reassessment**.
- **Lab Biomarkers:** Capture or pick a lab report photo → auto-run **ML Kit PII Redaction Preview** → extract structured markers (`HbA1c`, `LDL`, `Vitamin D`, `TSH`, etc.) with category, value, unit, reference range, and status (`Optimal`, `Borderline`, `High`, `Low`). Users toggle which extracted rows to save in batch or enter markers manually.

### 3.6 Mental Wellbeing (`MentalWellbeingScreen`), Body & Cycle (`BodyAndCycleScreen`), Health Connect (`HealthConnectScreen`)
- **Mental Wellbeing:** Standardized **PHQ-9** (0–27) and **GAD-7** (0–21) screenings with automatic severity banding (`Minimal` → `Severe`). Any positive self-harm item (PHQ-9 #9 > 0) or severe threshold immediately surfaces a high-contrast localized Crisis Helpline banner (`988` / `911`). Includes optional empathetic AI coping reflection with 1-tap `Add Coping Step` to Care Checklist.
- **Body & Cycle:** Weight/height/waist logging with automatic BMI calculation (`Metric` / `Imperial` toggle), opt-in Menstrual Cycle daily log (`None` → `Heavy`, symptoms, mood, typical cycle length), and optional AI Body & Cycle insight.
- **Health Connect:** Displays Health Connect SDK availability, manages least-privilege Read/Write permissions for Steps, Heart Rate, Sleep, and Blood Pressure, runs manual or 6-hour periodic background sync, supports importing Heart Rate / BP directly into Vitals, writes user-triggered measurements back to Health Connect, and generates a Health Connect AI summary brief.

### 3.7 History & Doctor Visit SOAP PDF (`HistoryScreen`, `DoctorPdfExportDialog`)
- **Paginated Consultation History:** Search/browse consultations with urgency pills, follow-up badges, delete confirmation, and `Load More` pagination.
- **30-Day Longitudinal Health Synthesis:** Cross-correlates check-ins, vitals, medications, adherence, and symptom outcomes into trajectory headline, observed patterns, medication/allergy notes, next-week focus, and clinician talking points.
- **Doctor Visit PDF Export Dialog:** Allows selecting time window (`14`, `30`, or `90` days) and toggling sections:
  - **AI SOAP Pre-Visit Brief** (Page 1 executive summary synthesized via `gemini-3.8-flash` with 30s timeout and cache; silently falls back to deterministic export if offline/quota-limited so export never fails)
  - Health Profile & Care Team
  - Active Medications & Adherence
  - Recent Vitals Summary
  - **Recent Lab Biomarkers Table**
  - Consultation History
  Shares generated PDF securely via `FileProvider` (`Intent.ACTION_SEND`).

### 3.8 Profile, Well+ Pro, Emergency Medical ID & Home Screen Widgets
- **ProfileScreen:** Displays account header, **Well+ Pro** quick-access banner (with active tier & daily AI remaining badge), **Medical ID Card** quick-access button, US/Canada Health Region selector, Theme selector (`System`, `Frosted Light`, `Twilight Dark`), Privacy & Security controls (**Biometric Lock**, **Screen Privacy**, **Cloud AI Consent**, **Auto-Mask PII on Photo Scans** default ON), Notification Reminder toggles, editable Health Profile & Care Team form, **Export All Data (JSON)**, **Delete All Data & Account**, and **Sign Out**.
- **WellPlusProScreen:** Compares Free (`25/day`) vs. Pro (`200/day`), displays live Play Billing v9 subscription offers (`Annual` & `Monthly`), launches Google Play checkout, and supports `Restore`.
- **EmergencyMedicalIdScreen:** High-contrast crimson **MEDICAL ID** header with blood type pill, prominent **KNOWN ALLERGIES** strip, active medications, conditions, Care Team info, 1-tap `ACTION_DIAL` for Emergency Contact and local emergency services (`911`), **Share Text**, and **Show on Home Screen Widget** opt-in toggle (default OFF).
- **3 Jetpack Glance Home Screen Widgets:**
  1. `WellPlusMoodWidget`: Displays today's mood + streak with 1-tap mood check-in deep link.
  2. `WellPlusNextMedWidget`: Displays next scheduled dose and today's taken/total progress.
  3. `WellPlusEmergencyIdWidget`: Privacy-first opt-in widget; renders a locked setup prompt until enabled, displays blood type/allergies/meds + 1-tap `Call ICE` / `Call 911` (`ACTION_DIAL`) when enabled, and wipes immediately on opt-out or sign-out.

---

## 4. Acceptance Criteria (Given / When / Then)

- **AC-F1 PII Preview Coverage:** *Given* `Auto-Mask PII on Photo Scans` is enabled, *when* a user captures or picks a photo in Symptom Intake, Follow-Up Chat, Rx Scanner, or Lab Scanner, *then* `PiiRedactionPreviewSheet` always intercepts the bitmap before any network call.
- **AC-F2 Camera Lab Auto-Tab:** *Given* a user is on the Vitals tab and launches Lab Camera capture, *when* they confirm the photo, *then* `VitalsScreen` automatically switches to the **Lab Biomarkers** tab and opens the PII redaction preview.
- **AC-F3 TTS Lifecycle Safety:** *Given* TTS readout is speaking a Consultation Report or Follow-Up Chat message, *when* the user navigates away or closes the screen, *then* `DisposableEffect` immediately stops and shuts down the `TextToSpeech` engine with zero audio leak.
- **AC-F4 PDF Export Resilience:** *Given* a user exports a Doctor Visit PDF with `AI SOAP Pre-Visit Brief` checked while offline, *when* export runs, *then* SOAP synthesis silently skips without throwing an error and the deterministic PDF still opens in Android's share sheet.

---

## Cross-References
- [Master PRD](./PRODUCT_REQUIREMENTS_PRD.md)
- [Design System & Accessibility](./DESIGN_SYSTEM_AND_ACCESSIBILITY.md)
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [QA Test Plan & CUJ Matrix](./QA_TEST_PLAN_AND_CUJS.md)
