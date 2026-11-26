# Well+ — Privacy, Security & Data Governance Specification

> **Document Owner:** Security Engineering & Privacy/Legal  
> **Target Audience:** Legal, Privacy Reviewers, Security Engineers, Release Managers  
> **Product Version:** 1.0 (Zero-Trust Per-User Vault)  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Verified by Automated Security Rules Tests

---

## 1. Privacy-First Design Principles

Well+ processes highly sensitive personal health information (symptoms, medications, vitals, lab biomarkers, PHQ-9/GAD-7 scores, cycle logs, and emergency contacts). Its security architecture enforces five non-negotiable guarantees:

1. **Strict Zero-Trust User Isolation:** Every cloud document lives under `/users/{uid}/...` and is guarded server-side by `firestore.rules` verifying `request.auth != null && request.auth.uid == userId`. Cross-user reads, writes, and deletes are categorically denied.
2. **No Anonymous Auth:** All cloud access requires authenticated Google Sign-In via Jetpack `CredentialManager`. Anonymous authentication is disabled.
3. **Ephemeral Photo Processing:** Photos captured or picked for Symptom Intake, Follow-Up Chat, Prescription Scanning, or Lab Reports are downscaled to `<= 1024px`, optionally black-box redacted on-device via ML Kit, sent ephemerally to Firebase AI Logic, and **never uploaded to Firebase Storage or persisted in cloud buckets**.
4. **Device-Level Privacy Protection:** Cloud backups are disabled (`android:allowBackup="false"`), sensitive screens support Biometric Lock and `FLAG_SECURE`/recents screenshot masking, and the Emergency Medical ID widget is strictly opt-in (`default OFF`) and wiped on sign-out.
5. **User Ownership & Right to Erasure:** Users can pause Cloud AI anytime, export their entire health vault as JSON, or permanently delete all cloud health collections and their Firebase Auth account from Profile.

---

## 2. Security Controls Matrix

| Control Layer | Mechanism | Scope & Behavior |
| :--- | :--- | :--- |
| **Authentication** | Firebase Auth + Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) | Single-account session isolation; `WellPlusAppContainer` is keyed to `user.uid` and closed via `DisposableEffect` on sign-out. |
| **Backend Integrity** | Firebase App Check (Play Integrity / reCAPTCHA in release, Debug provider in debug) | Configured at the top of `MainActivity.onCreate()` before any Firebase SDK or ViewModel initializes. |
| **Database Authorization** | `firestore.rules` (default deny-all + schema/enum/type/bounds/timestamp enforcement) | Validates every field whitelist, string length, numeric range, immutable `createdAt`/`userId`, and server timestamp sanity (`<= request.time + 5m`). Verified by 12 automated emulator test suites. |
| **Network Security** | `android:networkSecurityConfig="@xml/network_security_config"` + HTTPS-only citations | Enforces TLS transport security; rejects cleartext external medical links. |
| **Backup Hardening** | `android:allowBackup="false"` + `data_extraction_rules.xml` + `backup_rules.xml` | Prevents unencrypted ADB or automatic cloud backup extraction of local DataStore/widget preferences. |
| **Biometric App Lock** | `androidx.biometric:biometric:1.1.0` (`BiometricLockHelper` + `BiometricLockScreen`) | Optional toggle in Profile; automatically re-locks when the app backgrounds (`Lifecycle.Event.ON_STOP`). |
| **Screen Privacy** | `setRecentsScreenshotEnabled(false)` (API 33+) & `FLAG_SECURE` on production physical devices | Protects health screens in Android Recents app-switcher and blocks screenshots when enabled in Profile. |
| **On-Device PII Masking** | ML Kit Text Recognition v2 + `PiiPatternClassifier` | Auto-masks SSN/SIN, MRN/OHIP/Rx#, phone, email, and DOB locally before Vision AI calls (default ON). |
| **Crash Telemetry Sanitization** | `FirebaseCrashlyticsTelemetry` (`AppTelemetryRegistry`) | Truncates log messages and custom keys (`<= 240` / `120` chars) and never intentionally logs raw symptom notes, lab photos, or personal identifiers. |

---

## 3. Permissions Minimization Audit

Well+ strictly adheres to Android least-privilege and Google Play policy rules:

| Permission | Classification | Justification & Guardrail |
| :--- | :--- | :--- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Normal (Install-time) | Required for Firebase Auth, Cloud Firestore sync, and Firebase AI Logic. |
| `CAMERA` | Dangerous (Runtime) | Requested only when user opens in-app CameraX capture for symptom/lab/Rx photos; optional (`camera required="false"`). |
| `POST_NOTIFICATIONS` | Dangerous (Runtime, API 33+) | Requested only when user enables medication, daily check-in, or follow-up reminders. |
| `health.READ/WRITE_STEPS`, `_HEART_RATE`, `_SLEEP`, `_BLOOD_PRESSURE` | Health Connect Runtime | Requested strictly on user action inside `HealthConnectScreen` with dedicated `PermissionsRationaleActivity` and Android 14+ `ViewPermissionUsageActivity` alias. |
| **Zero Broad Storage Permissions** | **Not Requested** | Gallery photo selection uses Android's zero-permission Photo Picker (`PickVisualMedia`). No `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`. |
| **Zero `CALL_PHONE` Permission** | **Not Requested** | Emergency and ICE contact buttons use `Intent.ACTION_DIAL`, opening Android's native dialer without silent calling permissions. |

---

## 4. Emergency Medical ID Widget Privacy Contract

Because Home Screen widgets are visible without unlocking Well+'s in-app Biometric Lock:

```text
Default State (OFF) ──► Widget displays ONLY "Medical ID Widget Off" setup card
       │                (Zero PHI written to WidgetSnapshotStore)
       ▼ (User explicitly toggles "Show on Home Screen Widget" ON)
Opted-In State (ON) ──► Writes compact emergency subset ONLY:
                        Display Name, Blood Type, Allergies, Top 4 Meds, ICE Phone, 911
       │
       ├──► User toggles OFF ──► Immediately scrubs all emergency keys from SharedPreferences
       └──► User Signs Out / Deletes Account ──► WidgetSnapshotStore.clear() + Glance refresh
```

---

## 5. User Data Portability & Deletion Flows

- **Full JSON Data Export (`AccountDataPortabilityRepository`):**
  - Available in **Profile → Export All Data (JSON)**.
  - Aggregates the user's profile, consultations (+ messages), check-ins, care tasks, medications, adherence logs, vitals, lab biomarkers, mental assessments, body measurements, and cycle logs into a structured JSON file shared via `FileProvider` (`Intent.ACTION_SEND`).
- **Complete Account & Data Deletion (`deleteAllDataAndPrepareSignOut`):**
  - Requires explicit confirmation dialog in Profile.
  - Batch-deletes all user subcollections in Firestore under `/users/{uid}/...`, clears local DataStore caches and `WidgetSnapshotStore`, refreshes Glance widgets to blank state, deletes the Firebase Auth user record (handling recent-login errors gracefully after cloud wipe), and clears Jetpack Credential Manager state.
- **Pause Cloud AI Consent (`cloudAiEnabled`):**
  - Users can disable all Cloud AI processing with a single switch in Profile; local tracking, Health Connect, PDF deterministic export, and deterministic safety checks continue working seamlessly.

---

## 6. Acceptance Criteria (Given / When / Then)

- **AC-P1 Cross-User Isolation:** *Given* User B (`bob_456`) is authenticated, *when* User B attempts to read, write, or delete any document under `/users/alice_123/...`, *then* Firestore rejects 100% of requests with `PERMISSION_DENIED`.
- **AC-P2 Sign-Out Widget Scrub:** *Given* User A enabled the Emergency Medical ID widget, *when* User A taps **Sign Out**, *then* `WidgetSnapshotStore.clear()` wipes all stored health fields and triggers `WellPlusWidgetRefresher.refreshAll()`.
- **AC-P3 Biometric Re-Lock:** *Given* Biometric Lock is enabled in Profile, *when* the user backgrounds the app (`ON_STOP`) and returns, *then* `BiometricLockScreen` obscures all health data and global overlays until re-authenticated.
- **AC-P4 Zero Storage Permissions:** *Given* the compiled `AndroidManifest.xml`, *when* audited for Play compliance, *then* zero `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, or `CALL_PHONE` permissions exist.

---

## Cross-References
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [Architecture & Data Model](./ARCHITECTURE_AND_DATA_MODEL.md)
- [Google Play Launch & Policy Checklist](./PLAY_STORE_LAUNCH_CHECKLIST.md)
