# Well+ — Clinical Safety & AI Governance Specification

> **Document Owner:** Clinical Safety, AI Engineering & Legal  
> **Target Audience:** Legal/Compliance, Clinical Advisors, AI/Android Engineers, QA  
> **Product Version:** 1.0 (Defense-in-Depth Clinical AI Architecture)  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Enforced by Unit Tests & Firestore Rules

---

## 1. Non-Diagnostic Medical Boundary

> [!WARNING]
> **Well+ is NOT a medical device, does NOT provide medical diagnoses, and does NOT replace licensed clinicians or emergency services.** Every AI output is framed as structured educational guidance and clinician-visit preparation.

- **Explicit Onboarding Consent:** First-run onboarding hard-blocks access until the user confirms `ageConfirmed18Plus == true` and `disclaimerAccepted == true`.
- **Persistent Disclaimers:** Non-diagnostic medical disclaimers appear across Onboarding, Guided Intake, Consultation Reports, Follow-Up Chat, Medication Interactions, Lab Scans, Mental Wellbeing, and Exported Doctor Visit PDFs.
- **No Prescriptive Commands:** System prompts forbid diagnosing diseases, prescribing dosages, or telling users to stop prescribed medications; drug interactions explicitly instruct users to consult a pharmacist or physician.

---

## 2. Defense-in-Depth Clinical Safety Architecture

```text
User Input (Symptoms / Vitals / Photo / PHQ-9)
       │
       ├──► [Layer 1: Deterministic Offline Safety Engine — Zero LLM Dependency]
       │       • ClinicalSafetyEngine.evaluateSymptoms() -> 7 emergency categories
       │       • ClinicalSafetyEngine.evaluateVital()    -> Crisis vital bounds (US/CA)
       │       • PHQ-9 Item 9 / Severe score check       -> Immediate 988/911 banner
       │
       ├──► [Layer 2: On-Device ML Kit PII Redaction — Before Cloud Upload]
       │       • ML Kit Text Recognition v2 (local device only)
       │       • PiiPatternClassifier (SSN/SIN, MRN/OHIP/Rx#, Phone, Email, DOB)
       │       • Software ARGB_8888 bitmap black-box masking + user Preview Sheet
       │
       ├──► [Layer 3: Guarded Cloud AI Execution — Firebase AI Logic + App Check]
       │       • Remote Config global kill switch (`cloudAiGloballyEnabled`)
       │       • User Cloud AI Consent toggle (`cloudAiEnabled`)
       │       • Daily fair-use quota (`25` Free / `200` Pro) + response cache
       │       • Strict JSON Schema outputs + 30s timeout + silent fallbacks
       │
       └──► [Layer 4: Post-AI Validation, Citations & User Safety Reporting]
               • ProposedChatActionValidator (physiological & Firestore rules bounds)
               • TrustedMedicalCitationValidator (strict HTTPS 10-domain allowlist)
               • AiSafetyFooter on every AI surface -> immutable `aiSafetyReports`
```

---

## 3. Layer 1 — Deterministic Offline Clinical Guardrails

`ClinicalSafetyEngine` (`:core:safety`) evaluates inputs **synchronously on-device without network or LLM calls** so life-threatening signals can never be missed, delayed, or hallucinated away.

### 3.1 Symptom Red-Flag Categories (7 Hard-Coded Emergency Triggers)

| `RedFlagCategory` | Representative Signals | Automated Action |
| :--- | :--- | :--- |
| `MENTAL_HEALTH_CRISIS` | Self-harm or suicidal ideation phrases | Immediate **Crisis Support Dialog** (`isMentalHealthCrisis = true`) with 1-tap `988` Crisis Lifeline + `911` |
| `ANAPHYLAXIS` | Throat/tongue swelling, closing airway, severe allergic reaction | Immediate Emergency Dialog + epinephrine/911 instructions |
| `CARDIAC_OR_CHEST` | Chest pain/pressure/tightness, pain radiating to left arm/jaw, heart attack | Immediate Emergency Dialog + do-not-drive guidance |
| `NEUROLOGICAL_STROKE` | Facial droop, slurred speech, one-sided weakness, vision loss, thunderclap headache | Immediate Emergency Dialog + note exact onset time |
| `SEVERE_RESPIRATORY` | Unable to breathe, gasping, blue lips/cyanosis, choking | Immediate Emergency Dialog + upright posture guidance |
| `POISONING_OR_OVERDOSE` | Medication overdose, accidental poisoning | Immediate Emergency Dialog + poison control/bottle retention guidance |
| `SEVERE_BLEEDING_OR_TRAUMA` | Uncontrolled bleeding, vomiting/coughing blood, fainting/unconscious, active seizure, or severity `>= 9` with chest/breathing/fainting | Immediate Emergency Dialog + alert ICE contact / 911 |

### 3.2 Critical Vital-Sign Thresholds (US & Canada Unit-Aware)

| Vital Sign | Critical High Trigger | Critical Low Trigger | Automated Response |
| :--- | :--- | :--- | :--- |
| **Blood Pressure** | Systolic `>= 180 mmHg` OR Diastolic `>= 120 mmHg` (Hypertensive Crisis) | Systolic `<= 80 mmHg` OR Diastolic `<= 50 mmHg` | `CriticalVitalAlertDialog` + localized `ACTION_DIAL` |
| **Blood Oxygen (`SpO₂`)** | *(N/A)* | `< 90%` (Severe Hypoxemia) | `CriticalVitalAlertDialog` + warm-finger recheck & `911` |
| **Heart Rate** | `>= 160 bpm` | `<= 39 bpm` | `CriticalVitalAlertDialog` |
| **Temperature** | `>= 104.0°F` (`40.0°C`) | `<= 95.0°F` (`35.0°C`) | `CriticalVitalAlertDialog` (formatted in user's `°F` or `°C`) |
| **Blood Glucose** | `>= 400 mg/dL` (`22.2 mmol/L`) | `< 54 mg/dL` (`3.0 mmol/L`) | `CriticalVitalAlertDialog` (formatted in user's `mg/dL` or `mmol/L`) |

---

## 4. Layer 2 — On-Device ML Kit OCR & Automatic PII Redaction

Before any photo from **Symptom Intake**, **Follow-Up Chat**, **Prescription Bottle Scanner**, or **Lab Report Scanner** leaves the device for Gemini Vision:

1. **Local Software Copy:** Creates a mutable `Bitmap.Config.ARGB_8888` copy in memory (handling Android hardware bitmaps safely).
2. **On-Device ML Kit Recognition:** Runs `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` (`com.google.mlkit:text-recognition:16.0.1`) completely offline.
3. **Deterministic PII Classification (`PiiPatternClassifier`):** Inspects every OCR text line for 5 sensitive PII categories while preserving clinical values (`HbA1c 5.4%`, `LDL 112 mg/dL`):
   - `SSN` — US SSN (`XXX-XX-XXXX`) and Canadian SIN (`XXX-XXX-XXX`)
   - `MRN` — `MRN`, `Medical Record`, `Patient ID`, `Account #`, `Chart #`, `Health Card`, `OHIP`, `PHN`, `Rx #`
   - `PHONE` — North American telephone numbers
   - `EMAIL` — Email addresses
   - `DOB` — Labeled birth dates (`DOB`, `Date of Birth`, `Born`)
4. **Interactive Redaction Preview (`PiiRedactionPreviewSheet`):** Draws opaque black boxes over detected bounding rectangles, lists matched PII categories/counts, and lets the user choose **Use Redacted Image** (primary CTA), **Use Original**, or **Cancel**—with zero cloud upload until confirmed. Controlled by **Profile → Auto-Mask PII on Photo Scans** (default `ON`).

---

## 5. Layer 3 — Gemini Model Routing & AI Controls

All cloud AI executes exclusively through **Firebase AI Logic (`Firebase.ai`)** protected by **Firebase App Check** (zero hardcoded API keys in source or APK):

| AI Capability | Model ID | Quota Cost | Timeout / Caching |
| :--- | :--- | :---: | :--- |
| **Guided Symptom & Photo Triage** | `gemini-3.8-flash` | 1 credit | Strict JSON Schema; ephemeral image only |
| **Follow-Up Chat (Streaming)** | `gemini-3.8-flash` | 1 credit / turn | Token streaming; full clinical context |
| **Follow-Up Chat Action & Citation Extractor** | `gemini-3.5-flash-lite` | **0 extra credits** | Post-turn JSON Schema; strict validator filter |
| **Dynamic Clarifying Questions** | `gemini-3.8-flash` | 1 credit | Optional step before triage |
| **Prescription Bottle Scan & Drug Interactions** | `gemini-3.8-flash` | 1 credit | Draft review required before saving |
| **Lab Biomarker Extraction & Reassessment** | `gemini-3.8-flash` | 1 credit | Batch checkbox confirmation required before saving |
| **30-Day Longitudinal Health Synthesis** | `gemini-3.8-flash` | 1 credit | Fingerprint-cached locally |
| **Doctor Visit PDF — AI SOAP Pre-Visit Brief** | `gemini-3.8-flash` | 1 credit | **30s timeout**, fingerprint-cached, silent deterministic fallback |
| **Today Micro-Insight / Mental / Body / HC Brief** | `gemini-3.5-flash-lite` | 1 credit | Fingerprint-cached locally |

---

## 6. Layer 4 — Post-AI Validation, Trusted Citations & Safety Reporting

### 6.1 Allowlist-Validated Medical Citations (`TrustedMedicalCitationValidator`)
- Only `https://` URLs with zero `userInfo` (`@` spoofing rejected) matching exact domains or subdomains of **10 authoritative public-health organizations** are permitted:
  - `medlineplus.gov`, `nih.gov`, `cdc.gov`, `mayoclinic.org`, `nhs.uk`, `canada.ca`, `who.int`, `clevelandclinic.org`, `heart.org`, `diabetes.org`
- Unapproved, HTTP, or spoofed domains (e.g., `medlineplus.gov.evil.com`, `cdc.gov@attacker.test`) are stripped deterministically.
- Region-aware deterministic fallbacks (`NIH MedlinePlus`, `CDC`, `AHA`, or `Health Canada` for `CA`) ensure every consultation always includes trustworthy references even if AI returns none.

### 6.2 Proposed Chat Action Validation (`ProposedChatActionValidator`)
- Caps suggestions to `<= 3` per turn and clamps every string field to exact `firestore.rules` length ceilings (`medicationName <= 100`, `title <= 120`, `vitalNote <= 300`).
- Rejects physiologically impossible vitals (e.g., inverted blood pressure `70/120`, `SpO₂ > 100%`, `HR < 25` or `> 240`, `Temp < 90°F` or `> 112°F`, `NaN`/`Infinite`).
- **Explicit Human-in-the-Loop:** AI never auto-writes medications, vitals, tasks, or reminders; every proposed action requires an explicit 1-tap user confirmation.

### 6.3 Generative AI Safety Reporting (`AiSafetyReports`)
- Every AI card renders an `AiSafetyFooter` (**Report Issue**) opening `AiSafetyReportSheet`.
- Submissions persist as immutable records in `/users/{uid}/aiSafetyReports/{reportId}` covering all **12 `AiFeatureArea` values** (`SYMPTOM_TRIAGE`, `FOLLOW_UP_CHAT`, `DRUG_INTERACTION`, `MENTAL_REFLECTION`, `HIGHLIGHTS_INSIGHT`, `LONGITUDINAL_SYNTHESIS`, `BODY_CYCLE_INSIGHT`, `HEALTH_CONNECT_BRIEF`, `LAB_BIOMARKER_SCAN`, `PROACTIVE_REASSESSMENT`, `SOAP_PREVISIT_BRIEF`, `CHAT_ACTION_SUGGESTION`) and 5 reasons (`INACCURATE`, `UNSAFE_ADVICE`, `MISSED_URGENCY`, `OFFENSIVE`, `OTHER`).

### 6.4 Firebase Remote Config Emergency Kill Switches
`RemoteConfigSafetyFlagProvider` fetches remote operational safety controls dynamically:
- `cloudAiGloballyEnabled` — instant global circuit-breaker pausing all Cloud AI during safety incidents
- `dailyAiFairUseLimit` — server-tunable Free tier quota baseline (default `25`)
- `labBiomarkerAiEnabled`, `clarifyingQuestionsEnabled`, `proactiveReassessmentEnabled` — granular feature kill switches
- `urgentSafetyNotice` — top-of-app safety banner broadcast

---

## 7. Acceptance Criteria (Given / When / Then)

- **AC-S1 Citation Spoofing Rejection:** *Given* an AI response contains `http://medlineplus.gov` or `https://cdc.gov.phish.com`, *when* processed by `TrustedMedicalCitationValidator`, *then* both links are stripped before reaching the UI.
- **AC-S2 Kill Switch Effectiveness:** *Given* Remote Config sets `cloudAiGloballyEnabled = false`, *when* a user attempts any Cloud AI action, *then* `SessionCoordinator.guardCloudAiAndConsumeQuota()` blocks the call with a maintenance notice while deterministic trackers and emergency guardrails keep working.
- **AC-S3 Safety Report Immutability:** *Given* a user submits an `AiSafetyReport` for `SOAP_PREVISIT_BRIEF` or `CHAT_ACTION_SUGGESTION`, *when* saved to Firestore, *then* create succeeds and any subsequent `update` attempt is denied by `firestore.rules`.

---

## Cross-References
- [Privacy, Security & Compliance](./PRIVACY_SECURITY_AND_COMPLIANCE.md)
- [QA Test Plan & CUJ Matrix](./QA_TEST_PLAN_AND_CUJS.md)
- [Google Play Launch & Policy Checklist](./PLAY_STORE_LAUNCH_CHECKLIST.md)
