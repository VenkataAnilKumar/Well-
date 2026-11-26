# Well+ — Google Play Launch & Policy Checklist

> **Document Owner:** Release Engineering, Product Management & Legal/Compliance  
> **Target Audience:** Release Managers, Legal Reviewers, Android Engineers, PM  
> **Version:** 1.0  
> **Last Verified Against Codebase:** November 2026  
> **Status:** Production Launch Gate

---

## 1. Executive Summary

This document defines the mandatory pre-submission and store-review gates for launching **Well+** (`com.aistudio.sanacare.qvxkmt`) on Google Play across the **United States (`US`)** and **Canada (`CA`)**. Because Well+ operates at the intersection of **Google Play Health Apps**, **Android Health Connect**, **Generative AI (Firebase AI Logic)**, and **Google Play Billing v9 subscriptions**, every Play Console declaration, permission rationale, Data Safety entry, and release-build artifact must trace verbatim to verified runtime behavior. Passing every checklist item below is a hard ship blocker for production promotion.

---

## 2. Release & Compliance Gate Flow

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                     WELL+ GOOGLE PLAY RELEASE PIPELINE                     │
└────────────────────────────────────┬───────────────────────────────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         ▼                           ▼                           ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌────────────────────────┐
│ 1. Policy & Console │   │ 2. Build & Security │   │ 3. Billing & Backend   │
├─────────────────────┤   ├─────────────────────┤   ├────────────────────────┤
│ • Health Apps Form  │   │ • AAB Release Build │   │ • Play Billing v9 SKUs │
│ • GenAI Safety Form │   │ • R8 + ShrinkRes    │   │ • License Testers Pass │
│ • Data Safety Table │   │ • Crashlytics 3.0.8 │   │ • Firestore Rules Live │
│ • Health Connect    │   │   Mapping Uploaded  │   │ • App Check Play       │
│   Video & Rationale │   │ • Zero DCL / Zero   │   │   Integrity Registered │
│ • 18+ Rating & Store│   │   Broad Storage     │   │ • Remote Config Flags  │
└──────────┬──────────┘   └──────────┬──────────┘   └───────────┬────────────┘
           │                         │                          │
           └─────────────────────────┴──────────────────────────┘
                                     │
                                     ▼
          ┌─────────────────────────────────────────────────────┐
          │   Internal Testing ─▶ Closed Beta (US/CA) ─▶ 10%    │
          │   Staged Rollout ─▶ 50% ─▶ 100% Production Release  │
          └─────────────────────────────────────────────────────┘
```

---

## 3. Store Listing, Metadata & Content Rating Audit

| Policy Check | Play Requirement | Verified Well+ Implementation | Gate Status |
| :--- | :--- | :--- | :---: |
| **App Title** | <= 30 characters; no emojis, ALL CAPS, or promotional buzzwords ("Free", "Best", "#1"). | `app_name = "Well+"` (5 characters). Matches `metadata.json`, `strings.xml`, and `settings.gradle.kts`. | Verified |
| **Application ID** | Globally unique immutable package identifier. | `com.aistudio.sanacare.qvxkmt` (namespace `com.example`). | Verified |
| **Custom Launcher Icon** | High-resolution adaptive launcher icon (foreground + background + monochrome); never ship default Android icon. | Custom Well+ adaptive launcher icon bundled in `mipmap-*` & `mipmap-anydpi-v26`. | Verified |
| **Target Audience & Age Gate** | Health apps with adult clinical workflows and 18+ safety checks must not target children under 13. | Play Console Target Audience set to **18 and over**. In-app mandatory 18+ onboarding confirmation persisted in `healthProfile.isAdultConfirmed = true` and enforced in `firestore.rules`. | Verified |
| **Store Listing Framing** | Zero claims of medical diagnosis, cure, FDA/Health Canada medical device clearance, or doctor replacement. | Copy consistently positions Well+ as a **personal health companion & informational wellness triage tool** that prepares users for licensed clinicians. | Required at Upload |
| **Regional Distribution** | Markets must align with localized emergency numbers, units, and insurance labels. | Initial release countries strictly scoped to **United States** (`911` / `988` / `1-800-222-1222`, Imperial/US units) and **Canada** (`911` / `988` / `811`, Metric/CA units). | Required in Console |

---

## 4. Google Play Health Apps Declaration

> [!IMPORTANT]
> Google Play requires every health and wellness app to complete the **Health Apps Declaration** in Play Console (`App content > Health apps`). Selecting inaccurate categories triggers immediate rejection.

### 4.1 Declared Health App Categories
Select **only** the categories corresponding to shipped features:
- [x] **Activity and Fitness** — Daily physical activity check-in logging and Android Health Connect Steps sync.
- [x] **Health Tracking & Medical Reference (Non-Diagnostic)** — Personal vitals (BP, HR, Temp, SpO2, Glucose, Weight), lab biomarker tracking, symptom timeline, and informational guidance.
- [x] **Medication & Pain Management** — Personal medication list, reminder schedules, dose adherence logs, and Rx bottle label scanning.
- [x] **Mental and Behavioral Health** — Standard self-assessment check-ins (PHQ-9, GAD-7) with immediate crisis helpline banners (`988` / `911`).
- [x] **Reproductive & Sexual Health** — Optional menstrual cycle phase, flow, and symptom tracking.
- [x] **Sleep Management** — Android Health Connect Sleep duration sync and daily sleep quality check-in.

### 4.2 Explicitly Excluded Declarations (Do NOT Select)
- [ ] **Medical Device / Clinical Decision Support (SaMD)** — Well+ is **not** a regulated medical device and does not diagnose, prescribe, or treat.
- [ ] **Human Subjects Research / Clinical Trials** — No research enrollment or IRB data collection.
- [ ] **Emergency Dispatch Service** — Well+ uses `Intent.ACTION_DIAL` to open the system dialer; it does not place automated emergency calls.

### 4.3 Required In-App Disclosures Verified
- Prominent non-diagnostic disclaimer displayed during 18+ Onboarding, Guided Triage, Follow-Up Chat, Lab/Photo Scans, and Doctor Visit SOAP PDFs.
- Emergency red-flag guidance and `911` / `988` / Poison Control / `811` links are **never paywalled** and work offline via `ClinicalSafetyEngine`.

---

## 5. Google Play Generative AI Policy Compliance

Google Play's **AI-Generated Content Policy** mandates user-facing reporting controls, content safety safeguards, and clear AI attribution.

| Requirement | Verified Well+ Control | Codebase / Schema Grounding |
| :--- | :--- | :--- |
| **In-App Flagging / Reporting** | Users can flag unsafe, inaccurate, or concerning AI outputs across every AI surface without leaving the app. | Immutable `users/{uid}/aiSafetyReports` collection covering all **12 `AiFeatureArea` values** (including `SOAP_PREVISIT_BRIEF` and `CHAT_ACTION_SUGGESTION`). |
| **Deterministic Safety Guardrails** | Life-threatening symptoms and critical vitals trigger deterministic offline escalation independent of LLM output. | `ClinicalSafetyEngine` (7 red-flag categories + US/CA critical vital thresholds) and `Phase3SafetyEngines`. |
| **Citation Verification** | Medical reference links returned by Gemini are strictly filtered before display. | `CitationValidator` enforces HTTPS-only URLs against an explicit **10-domain clinical allowlist** (CDC, NIH, MedlinePlus, Mayo Clinic, WHO, Health Canada, etc.). |
| **Human-in-the-Loop Actions** | AI never silently modifies user health records (tasks, meds, vitals). | `ProposedChatActionValidator` renders a one-tap confirmation card per action; user must explicitly confirm before Firestore write. |
| **Privacy-Preserving Vision AI** | Lab reports and prescription photos are scrubbed of visible PII on-device prior to cloud upload. | On-device **ML Kit Text Recognition v2** detects SSN, MRN, DOB, phone, email, and names and renders a mandatory black-box redaction preview before upload. |
| **Instant Kill Switch** | Operations can disable AI features immediately in an incident without shipping an APK update. | Firebase Remote Config safety kill-switch flags (`ai_features_enabled`, per-feature toggles) gracefully fall back to deterministic offline triage and non-AI SOAP PDFs. |
| **Clear AI Attribution** | AI-generated briefs, chat turns, lab summaries, and insights are visually labeled as AI-assisted informational output. | Material 3 badges and disclaimers rendered across Consult, Insights, Vitals Labs, Meds, and SOAP PDF footer. |

---

## 6. Permissions, Photo Picker & Health Connect Audit

### 6.1 Manifest Permissions Justification Table

| Permission | Protection Level | Why Required | Play Policy Compliance Note |
| :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal | Firebase Auth, Firestore sync, Firebase AI Logic, Play Billing. | Standard install-time permission. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Detect offline state for banner, sync queue, and deterministic fallback. | Standard install-time permission. |
| `android.permission.CAMERA` | Dangerous (Runtime) | Optional capture of symptom photos, prescription bottles, or lab sheets. | `<uses-feature android:name="android.hardware.camera" android:required="false" />` ensures Chromebooks/tablets without cameras remain eligible. Requested just-in-time via `ActivityResultContracts.RequestPermission`. |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (API 33+) | Medication reminders, daily wellness nudges, and follow-up check-ins via WorkManager. | Requested only when user enables reminders; app functions cleanly if denied. |
| `android.permission.health.READ_STEPS` / `WRITE_STEPS` | Health Connect | Sync daily step counts into Today & Trends; optional write-back. | Exact scope declared in Health Connect Console form. |
| `android.permission.health.READ_HEART_RATE` / `WRITE_HEART_RATE` | Health Connect | Sync resting/active HR into Vitals; write logged HR to Health Connect. | Exact scope declared in Health Connect Console form. |
| `android.permission.health.READ_SLEEP` / `WRITE_SLEEP` | Health Connect | Sync sleep duration into Daily Check-In & longitudinal trends. | Exact scope declared in Health Connect Console form. |
| `android.permission.health.READ_BLOOD_PRESSURE` / `WRITE_BLOOD_PRESSURE` | Health Connect | Sync BP readings into Vitals and write logged BP back to Health Connect. | Exact scope declared in Health Connect Console form. |

### 6.2 Prohibited Patterns Verified Absent
- **Zero Broad Storage Permissions:** `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, and `READ_MEDIA_VIDEO` are **not declared**. Gallery selection uses the zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- **Zero `CALL_PHONE` Permission:** Emergency actions invoke `Intent.ACTION_DIAL`, giving the user final confirmation in the system dialer.
- **Zero Dynamic Code Loading (DCL):** No `.dex`, `.jar`, or `.so` downloads at runtime.
- **Zero Cloud Backup Leakage:** `android:allowBackup="false"` prevents unencrypted ADB/cloud backups of sensitive health data.

### 6.3 Health Connect Play Console Review Requirements
Before submitting to Production, complete the **Health Connect permissions declaration** in Play Console:
- [ ] Confirm `PermissionsRationaleActivity` handles `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 13 and lower) and `ViewPermissionUsageActivity` alias handles `android.intent.action.VIEW_PERMISSION_USAGE` with `android.intent.category.HEALTH_PERMISSIONS` (Android 14+). Both are declared in `app/src/main/AndroidManifest.xml`.
- [ ] Record and attach a **screen-capture review video** demonstrating:
  1. Navigating to **Settings → Health Connect**.
  2. Viewing the in-app permission rationale screen.
  3. Granting Steps, Heart Rate, Sleep, and Blood Pressure permissions in the Android Health Connect system dialog.
  4. Observing synced values rendered on the **Today** and **Vitals** screens and verifying write-back behavior.
- [ ] Publish a public **Privacy Policy URL** explicitly detailing how Health Connect data is used, stored per-UID in Firestore, never sold, and never used for advertising.

---

## 7. Google Play Data Safety Form Mapping

Use the verified matrix below when completing **Play Console → App content → Data safety**:

| Data Category | Data Types Collected | Collected? | Shared w/ 3rd Parties? | Purpose | Ephemeral? | User Can Request Deletion? |
| :--- | :--- | :---: | :---: | :--- | :---: | :---: |
| **Personal Info** | Name, Email Address, User ID (Firebase Auth / Google Sign-In) | Yes | No | Account management, App functionality | No | Yes (In-app + Web) |
| **Health & Fitness** | Health info (symptoms, vitals, labs, medications, PHQ-9/GAD-7, cycle, consultations), Fitness info (steps, sleep, activity) | Yes | No *(processed via Firebase AI Logic under Google Cloud terms; zero ad sharing)* | App functionality, Personalization | No | Yes (In-app + Web) |
| **Photos & Videos** | User-selected symptom, Rx, or lab photos | Yes (Transient) | No | App functionality (multimodal analysis) | **Yes** *(masked on-device via ML Kit; sent transiently to Gemini; zero permanent photo storage in Cloud Storage/Firestore)* | N/A (Not retained) |
| **Financial Info** | Purchase history (Well+ Pro subscription entitlement state) | Yes | No (Managed by Google Play Billing) | App functionality, Account management | No | Yes |
| **App Info & Performance** | Crash logs, Diagnostics (Firebase Crashlytics) | Yes | No | Analytics, Fraud prevention & security | No | Yes |
| **Device or Other IDs** | Firebase App Check / Play Integrity attestation tokens | Yes | No | Fraud prevention, security, and compliance | Yes | N/A |

### Mandatory Data Safety Declarations
- **Is all user data encrypted in transit?** → **Yes** (HTTPS/TLS enforced via `network_security_config.xml`).
- **Do you provide a way for users to request that their data be deleted?** → **Yes** — In-app **Settings → Privacy & Data → Delete Account & All Data** wipes all 13 user-scoped Firestore collections, local DataStore/widget caches, and Firebase Auth account; also provide a matching **Web Deletion Request URL** in Play Console.
- **Third-Party Advertising SDKs:** **None** (`Contains Ads = No`).

---

## 8. Google Play Billing v9 Console Setup

| Billing Item | Exact Console Configuration | Verification Check |
| :--- | :--- | :--- |
| **Billing Library Version** | `com.android.billingclient:billing-ktx:9.1.0` | Confirmed in `gradle/libs.versions.toml` & `core:billing`. |
| **Annual Subscription Product ID** | `wellplus_pro_annual` | Base plan `P1Y` (Yearly auto-renewing), fallback display price `$59.99 / yr`, tagged as **Best Value**. |
| **Monthly Subscription Product ID** | `wellplus_pro_monthly` | Base plan `P1M` (Monthly auto-renewing), fallback display price `$7.99 / mo`. |
| **Pending & Prepaid Purchases** | `PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build()` | Verified in `PlayBillingManager`. |
| **Auto Service Reconnection** | `.enableAutoServiceReconnection()` enabled on `BillingClient.Builder`. | Handles Billing service disconnects gracefully without silent crashes. |
| **Purchase Acknowledgement** | `acknowledgePurchase` called immediately on `PURCHASED` state when `!isAcknowledged`. | Prevents 3-day automatic Google Play refund of unacknowledged purchases. |
| **Restore Purchases** | `queryPurchasesAsync(ProductType.SUBS)` invoked on launch and via Settings **Restore Purchases** button. | Re-hydrates Pro entitlement (`200` AI requests/day) across reinstalls/devices. |
| **License Testers** | Add QA Google accounts to Play Console → Setup → License testing (`RESPOND_NORMALLY`, `SLOW_TEST_CARD`, `DECLINED`). | Validate purchase, pending, cancel, expiry, and restore flows before release. |

---

## 9. Release Engineering & Infrastructure Readiness

- [ ] **Signed Release AAB:** Build via `gradle :app:bundleRelease`; verify `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` rules preserve `@Serializable` models, Firestore POJOs, Play Billing classes, and ML Kit Text Recognition classes.
- [ ] **Dependency Metadata:** `dependenciesInfo { includeInApk = false; includeInBundle = true }` verified in `app/build.gradle.kts`.
- [ ] **Firebase Crashlytics Gradle Plugin (`3.0.8`):** Confirm `com.google.firebase.crashlytics` plugin generates `com.google.firebase.crashlytics.mapping_file_id` during release builds and uploads ProGuard/R8 mapping files automatically (eliminating `The Crashlytics build ID is missing` crash).
- [ ] **Firebase App Check (Play Integrity):** Register the Play App Signing SHA-256 certificate fingerprint in Firebase Console → App Check → Play Integrity, and switch enforcement to **Enforced** for Firebase AI Logic and Firestore prior to 100% rollout.
- [ ] **Firestore Security Rules Deployed:** Confirm `firestore.rules` is deployed to production and passes `12/12` tests in `firestore.test.js`.
- [ ] **Remote Config Defaults Published:** Publish initial Remote Config parameters (`ai_features_enabled = true`, quota thresholds `25` / `200`, feature kill switches) in Firebase Console.

---

## 10. Testable Acceptance Criteria

- **AC-L1 (Zero-Policy-Violation Manifest):**
  - *Given* the release AAB generated from `app/build.gradle.kts`,
  - *When* the merged `AndroidManifest.xml` is inspected via Android Studio APK Analyzer or Play Console Pre-Launch Report,
  - *Then* `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, and `CALL_PHONE` are completely absent, `android:allowBackup="false"` is set, and both Health Connect rationale activities are exported/declared cleanly.
- **AC-L2 (Crashlytics & R8 Release Smoke):**
  - *Given* a release-minified APK/AAB installed on a physical Android 14+ test device,
  - *When* the app cold-starts through Google Sign-In, Today, Guided Triage, ML Kit PII scan preview, and Doctor Visit SOAP PDF export,
  - *Then* zero `FirebaseInitProvider` Crashlytics build ID crashes, zero `ClassNotFoundException`/`NoSuchFieldException` serialization crashes, and zero `MissingPluginException` errors occur.
- **AC-L3 (Billing SKU Parity & Restore):**
  - *Given* a Google Play License Tester account on the Internal Testing track,
  - *When* the tester purchases `wellplus_pro_annual`, reinstalls the app, and signs back in,
  - *Then* the subscription acknowledges within 5 seconds, daily AI quota upgrades from `25` to `200`, and reinstall automatically restores Pro entitlement via `queryPurchasesAsync`.
- **AC-L4 (Data Safety & Deletion Parity):**
  - *Given* an active user with records across all 13 Firestore collections and an enabled Emergency Medical ID widget,
  - *When* the user executes **Delete Account & All Data**,
  - *Then* all cloud documents are deleted, local DataStore and widget snapshots are scrubbed to empty, and Firebase Auth signs the user out cleanly.

---

## 11. Cross-References

- [Master Docs Hub (`README.md`)](./README.md)
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [Privacy, Security & Compliance](./PRIVACY_SECURITY_AND_COMPLIANCE.md)
- [Monetization & Go-To-Market](./MONETIZATION_AND_GTM.md)
- [QA Test Plan & CUJ Matrix](./QA_TEST_PLAN_AND_CUJS.md)
