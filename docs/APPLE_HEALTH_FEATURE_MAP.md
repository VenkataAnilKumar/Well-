# Well+ — Apple Health End-to-End Feature Map & Gap Analysis

This document captures the complete end-to-end feature architecture of **Apple Health**, mapped directly against what is already implemented in **Well+** today, where **Well+** differentiates with Gemini AI, and the prioritized modules to build next on Android.

---

## 1. Summary Dashboard (Home)

| Feature | Apple Health Capability | Well+ Status |
| :--- | :--- | :--- |
| **Pinned Metrics ("Favorites")** | Customizable metric cards with bold figures & mini sparkline charts | **Implemented** (2×2 Pinned Grid: Vitals sparkline, Medications adherence dots, Sleep 7-day bars, AI Triage status) |
| **Automatic Highlights** | Plain-language callouts when metrics shift over 7/14/30 days | **Partially Implemented** (30-Day AI Longitudinal Synthesis; auto-highlighted Summary cards planned) |
| **Long-Term Trends** | Multi-week directional trend tracking & significant-change notifications | **Implemented** (30-Day Mood & Symptom chart + Vitals history) |
| **Health Checklist** | Setup verification for Medical ID, reminders, and safety locks | **Implemented** (3-step Clinical Safety & Setup Onboarding + Profile controls) |

---

## 2. Core Health Data Categories (Browse)

### 2.1 Activity & Fitness
- Steps, walking/running distance, flights climbed, active/resting energy, exercise minutes, stand hours, cardio fitness ($\text{VO}_2\text{ max}$), and workout logs.
- **Well+ Status:** Planned via **Android Health Connect** integration.

### 2.2 Heart
- Heart rate, resting heart rate, walking heart rate average, Heart Rate Variability (HRV), ECG recordings, irregular rhythm notifications, high/low heart rate alerts, AFib History, and Cardio Recovery.
- **Well+ Status:** Heart rate logging & clinical range classification implemented; wearable/HRV sync planned via Health Connect.

### 2.3 Vitals
- Blood pressure, body/wrist temperature, blood oxygen ($\text{SpO}_2$), respiratory rate, blood glucose, and overnight Vitals baseline.
- **Well+ Status:** **Implemented** (BP, Heart Rate, Temperature, $\text{SpO}_2$, Glucose logging with automatic clinical range evaluation: Normal / Elevated / Low / High).

### 2.4 Sleep
- Sleep stages (Awake, REM, Core, Deep), bedtime/wake schedules, wind-down routines, sleep consistency, and sleep apnea breathing disturbance notifications.
- **Well+ Status:** Daily sleep hours logging + 7-day bar sparkline implemented; stage sync & bedtime schedules planned.

### 2.5 Medications
- Prescription & supplement library (manual entry or camera label scan).
- Custom schedules (daily, specific days, cyclical, as-needed) and time-sensitive reminders.
- **Drug–Drug, Alcohol, Tobacco & Cannabis Interaction Alerts** (Critical / Serious / Moderate severity tiers).
- Pill shape/color visual appearance and refill reminders.
- **Well+ Status:** **Core Implemented** (dedicated `Meds` tab, per-prescription daily WorkManager push notifications, M3 TimePicker, 7-day adherence log, deep-link tap-to-log). **Planned Next:** AI Drug–Drug & Allergy Interaction Checker + Camera Prescription Bottle Scanner.

### 2.6 Symptoms
- Structured symptom entries (mild/moderate/severe, onset, duration, longitudinal log).
- **Well+ Status:** **Surpassed via AI** (4-step Guided Symptom Intake, Voice Dictation, AI Clinical Triage JSON report, Red-Flag Emergency Escalation, and 48-hour Symptom Recovery Follow-Up Loop).

### 2.7 Mental Wellbeing
- **State of Mind:** Momentary emotions and daily mood logging with contributing life factors.
- **Standardized Clinical Assessments:** Validated **PHQ-9** (depression) and **GAD-7** (anxiety) questionnaires with PDF export.
- **Mindful Minutes** & Time in Daylight tracking.
- **Well+ Status:** Daily Mood & Emotional Ease check-ins implemented; PHQ-9 / GAD-7 assessments planned.

### 2.8 Cycle Tracking & Reproductive Health
- Period logging, cycle & fertile window predictions, symptom & basal body temperature logging, cycle deviation alerts, and pregnancy tracking.
- **Well+ Status:** Planned optional module.

### 2.9 Body Measurements
- Height, weight, BMI, body fat percentage, lean body mass, and waist circumference.
- **Well+ Status:** Planned under Vitals & Biometrics expansion.

### 2.10 Mobility, Respiratory & Hearing
- **Mobility:** Walking steadiness, gait asymmetry, double-support time, walking speed, step length, six-minute walk test.
- **Respiratory:** $\text{SpO}_2$, peak expiratory flow, inhaler usage.
- **Hearing:** Headphone audio levels, environmental sound exposure, audiograms.
- **Well+ Status:** $\text{SpO}_2$ implemented; sensor/wearable metrics planned via Health Connect.

### 2.11 Nutrition
- Calories, macronutrients, water/hydration, caffeine, vitamins & minerals.
- **Well+ Status:** Daily hydration tracking implemented; full nutrition/macros optional future module.

---

## 3. Clinical & Medical Records

| Feature | Apple Health Capability | Well+ Status |
| :--- | :--- | :--- |
| **Medical ID** | Instant emergency card with blood type, allergies, conditions, meds & ICE contacts | **Implemented** (Summary Medical ID card + full dialog + 1-tap `ACTION_DIAL` Emergency Contact & 911) |
| **Health Records (FHIR)** | Direct hospital/clinic EHR sync for labs, immunizations & notes | **Future Integration** |
| **Lab Result Insights** | Out-of-range marker callouts & plain-language explanations | **Implemented via AI Vision** (Ephemeral CameraX / Photo Picker lab report analysis) |
| **Doctor Visit Summary (PDF)** | Printable/shareable clinician brief | **Implemented** (Multi-page FileProvider Doctor Visit PDF export) |

---

## 4. Safety, Sharing & Privacy

- **Emergency Escalation & One-Tap Dialing:** **Implemented** in Well+ (zero-permission `ACTION_DIAL` for 911 and personal emergency contact).
- **Biometric Protection & Cloud Isolation:** **Implemented** in Well+ (on-device `BiometricPrompt` Sanctuary Lock + Google Sign-In via Credential Manager + strict per-user Firestore security rules).
- **Full Data & Account Deletion:** **Implemented** in Well+ (2-step verified deletion of all Firestore subcollections + Firebase Auth account).
- **Family / Caregiver Live Sharing:** Planned future module (selective metric sharing and missed-dose/alert notifications for family caregivers).
- **Wearable & Ecosystem Sync:** Planned via **Android Health Connect**.

---

## 5. Unique AI Advantages Already in Well+ (Beyond Apple Health)

1. **Guided AI Symptom Triage (`Gemini` Structured JSON)** — Personalized urgency tier, differential possibilities, home care suggestions, red-flag warnings, and doctor questions cross-checked against allergies & active prescriptions.
2. **Multimodal Photo & Lab Document Analysis** — On-demand ephemeral AI review of visible symptoms and printed/screen lab reports without storing raw photos in cloud storage.
3. **Streaming Follow-Up AI Clinician Chat** — Context-aware multi-turn Q&A grounded in the user's profile and active consultation report.
4. **30-Day AI Longitudinal Synthesis** — Cross-domain pattern discovery across mood, sleep, hydration, vitals, medication adherence, and symptom outcomes.
5. **Zero-Permission Voice Dictation** — Hands-free symptom intake and chat input.

---

## 6. Recommended Next Modules to Build in Well+

1. **AI Drug–Drug & Allergy Interaction Checker** — Automatically flag Critical/Serious/Moderate interactions against active medications and listed allergies inside the `Meds` tab.
2. **Camera Prescription Bottle Scanner** — Use Gemini Vision to auto-fill medication name, dosage, schedule slot, and instructions from a pill bottle photo.
3. **Mental Wellbeing Assessments (PHQ-9 & GAD-7)** — Clinically standard self-check questionnaires with longitudinal scores included in Doctor PDF exports.
4. **Automatic Summary "Highlights & Trends" Cards** — Proactive 7/14-day shift callouts surfaced automatically on the `Summary` screen.
5. **Android Health Connect Sync** — Two-way sync for steps, heart rate, sleep stages, and blood pressure from Android wearables and fitness apps.
