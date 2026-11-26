# Well+ — System Architecture & Data Model Reference

> **Document Owner:** Android & Backend Architecture  
> **Target Audience:** Android Engineers, Backend Engineers, QA Automation  
> **Build Toolchain:** AGP `9.1.1`, Kotlin `2.4.21`, Jetpack Compose BOM `2024.09.00`, Firebase BOM `35.1.0`, `minSdk 24`, `targetSdk 36`  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Verified

---

## 1. 23-Module Clean Architecture Overview

Well+ is modularized into **23 Gradle modules** (`:app`, 11 `:core:*` foundation modules, and 11 `:feature:*` modules) enforcing **strict unidirectional dependencies** and **zero cross-feature imports** (`feature/*` modules NEVER depend on another `feature/*` module).

```text
                             ┌────────────────────────────┐
                             │           :app             │
                             │ MainActivity • NavHost     │
                             │ WellPlusAppContainer (DI)  │
                             │ PlayBillingManager (v9)    │
                             └─────────────┬──────────────┘
                                           │ wires
         ┌─────────────────────────────────┴─────────────────────────────────┐
         ▼                                                                   ▼
┌───────────────────────────────────────────────────┐       ┌──────────────────────────────────┐
│           11 Independent :feature:* Modules       │       │      11 :core:* Modules          │
│  (Zero cross-feature Gradle dependencies)         │       ├──────────────────────────────────┤
├───────────────────────────────────────────────────┤       │ :core:model         (Domain DTOs)│
│ :feature:auth          :feature:today             │       │ :core:common        (Region/Util)│
│ :feature:consult       :feature:medications       │──────▶│ :core:safety        (Red Flags)  │
│ :feature:vitals        :feature:mental            │       │ :core:datastore     (Prefs/Quota)│
│ :feature:bodycycle     :feature:healthconnect     │       │ :core:firebase      (Repos/Sess) │
│ :feature:history       :feature:profile           │       │ :core:ai            (Gemini AI)  │
│ :feature:widgets       (Jetpack Glance)           │       │ :core:analytics     (PDF/Trends) │
└───────────────────────────────────────────────────┘       │ :core:healthconnect (HC SDK)     │
                                                            │ :core:notifications (WorkMgr)    │
                                                            │ :core:designsystem  (M3/MLKit/TTS│
                                                            │ :core:testing       (Test Fakes) │
                                                            └──────────────────────────────────┘
```

### Key Architectural Patterns
- **Manual User-Scoped Dependency Container (`WellPlusAppContainer`):** Instantiated per authenticated `user.uid` via `remember(user.uid)` and cleaned up via `DisposableEffect { container.close() }` (closing network monitors, billing connections, and coroutine scopes).
- **Single-Listener Hot Firestore Streams:** `WellPlusAppContainer` owns **one** `StateFlow<CloudUiState<T>>` (`SharingStarted.WhileSubscribed(5_000L)`) per Firestore collection and shares it across all 9 feature ViewModels—eliminating duplicate snapshot listeners and redundant Firestore reads.
- **Cursor-Based Pagination:** High-volume collections (`consultations`, `vitals`) stream a live head page (`DEFAULT_HISTORY_PAGE_SIZE`) while older history loads on demand via `DocumentSnapshot` cursors (`LiveHeadPage`).
- **Centralized Session Coordinator (`SessionCoordinator`):** Coordinates network connectivity (`NetworkConnectivityMonitor`), midnight-safe local date keys (`DateKeyProvider`), reactive AI quota (`combine(deviceSettings, subscriptionTierFlow)`), active US/CA health region, transient snackbars, and global clinical safety alerts.

---

## 2. Cloud Firestore Data Model (13 User-Isolated Collections)

Every collection sits under `/users/{userId}/...`, uses `FieldValue.serverTimestamp()` for `createdAt`/`updatedAt`, forbids writing `@DocumentId` (`id`) fields in document payloads, and enforces strict schema validation in `firestore.rules`.

| Collection Path | Entity | Write Policy | Key Fields & Validation Bounds |
| :--- | :--- | :--- | :--- |
| `/users/{uid}/profile/{profileId}` | `HealthProfile` | Create, Update (`main`), Delete | `displayName`, `age`, `biologicalSex`, `bloodType`, `allergies[]`, `medications[]`, `conditions[]`, `wellnessGoals[]`, `onboardingCompleted`, `disclaimerAccepted`, `ageConfirmed18Plus`, `preferredHealthRegion` (`US`/`CA`), `preferredEmergencyCountry`, `emergencyContactName/Phone`, `coverageProvider`, `coverageMemberId` (`<= 40`), `primaryDoctorName`, `preferredPharmacy` |
| `/users/{uid}/consultations/{id}` | `Consultation` | Create, Follow-Up Update Only, Delete | `consultationType` (`SYMPTOM_CHECK`/`PHOTO_ANALYSIS`), `title`, `bodyArea`, `symptomSummary`, `severity` (`1..10`), `duration`, `associatedSymptoms[]`, `hadPhotoAttachment`, `urgencyLevel` (`SELF_CARE`/`DOCTOR_SOON`/`URGENT`/`EMERGENCY`), `aiSummary` *(immutable)*, `possibleCauses[]`, `careSuggestions[]`, `redFlags[]`, `doctorQuestions[]`, `followUpStatus` (`PENDING`/`IMPROVING`/`UNCHANGED`/`WORSENING`/`RESOLVED`), `followUpNote` |
| `/users/{uid}/consultations/{id}/messages/{msgId}` | `ChatMessage` | Create (parent must exist), Read, Delete | `consultationId`, `role` (`USER`/`DOCTOR`), `content`, `hadPhotoAttachment`, `createdAt` *(immutable; orphan writes denied)* |
| `/users/{uid}/checkIns/{dateKey}` | `DailyCheckIn` | Create, Update, Delete | Keyed `yyyy-MM-dd`; `moodLevel` (`1..5`), `energyLevel` (`1..5`), `sleepHours` (`0..24`), `hydrationGlasses` (`0..30`), `activityLevel` (`SEDENTARY`..`VIGOROUS`), `activityMinutes` (`0..600`), `notes` |
| `/users/{uid}/careTasks/{taskId}` | `CareTask` | Create, Update, Delete | `title`, `category` (`AI_SUGGESTION`/`MEDICATION`/`HYDRATION`/`RECOVERY`/`CUSTOM`), `completed`, optional `sourceConsultationId` |
| `/users/{uid}/medications/{medId}` | `MedicationItem` | Create, Update, Delete | `name` (`<= 100`), `dosage` (`<= 60`), `scheduleSlot` (`MORNING`/`MIDDAY`/`EVENING`/`BEDTIME`/`AS_NEEDED`), `reminderHour` (`0..23`), `reminderMinute` (`0..59`), `instructions` (`<= 240`), `active` |
| `/users/{uid}/medicationLogs/{logId}` | `MedicationLog` | Create, Update, Delete | Keyed `{dateKey}_{medicationId}`; `dateKey`, `medicationId`, `medicationName`, `status` (`TAKEN`/`SKIPPED`) |
| `/users/{uid}/vitals/{vitalId}` | `VitalReading` | Create, Delete *(Immutable)* | `vitalType` (`BLOOD_PRESSURE`/`HEART_RATE`/`TEMPERATURE`/`SPO2`/`WEIGHT`/`BLOOD_GLUCOSE`), `primaryValue` (`> 0`), optional `secondaryValue`, `unit`, `note` (`<= 300`), `recordedAt` |
| `/users/{uid}/labBiomarkers/{id}` | `LabBiomarker` | Create, Delete *(Immutable)* | `markerName`, `category` (`METABOLIC`/`LIPIDS`/`THYROID`/`VITAMINS`/`CBC`/`KIDNEY_LIVER`/`OTHER`), `value` (`>= 0`), `unit`, `referenceRangeText`, `status` (`OPTIMAL`/`BORDERLINE`/`HIGH`/`LOW`), `panelDateKey`, `labNote`, `recordedAt` |
| `/users/{uid}/mentalAssessments/{id}` | `MentalAssessment` | Create, AI Reflection Update Only, Delete | `assessmentType` (`PHQ9`/`GAD7`), `totalScore` *(immutable)*, `maxScore`, `severityBand`, `itemScores[]`, `crisisFlagged`, `aiReflection`, `aiCopingSteps[]` |
| `/users/{uid}/bodyMeasurements/{id}` | `BodyMeasurement` | Create, Delete *(Immutable)* | `weightKg` (`10..400`), `heightCm` (`0` or `50..260`), `bmi`, `waistCm`, `note`, `recordedAt` |
| `/users/{uid}/cycleLogs/{dateKey}` | `CycleLog` | Create, Update, Delete | Keyed `yyyy-MM-dd` (must match doc ID); `flowLevel` (`NONE`/`SPOTTING`/`LIGHT`/`MEDIUM`/`HEAVY`), `symptoms[]`, `moodTag`, `notes` |
| `/users/{uid}/aiSafetyReports/{id}` | `AiSafetyReport` | Create, Read, Delete *(Immutable)* | `featureArea` (12 enums), `reason` (5 enums), `contentSnippet` (`<= 400`), `userNote` (`<= 500`), `createdAt` |

---

## 3. Local Storage, Caching & Entitlements Architecture

### 3.1 Jetpack DataStore (`SanctuaryPreferencesStore`)
Stores device-local user preferences (`SanctuaryDeviceSettings`):
- Privacy toggles: `biometricLockEnabled`, `screenPrivacyEnabled`, `cloudAiEnabled`, `autoMaskPiiBeforeScan` (default `true`), `showEmergencyIdOnWidget` (default `false`)
- Clinical & regional settings: `preferredHealthRegion`, `emergencyRegionOverride`, `useMetricBodyUnits`, `cycleTrackingEnabled`, `typicalCycleLengthDays`
- Reminders & Sync: `medRemindersEnabled`, `checkInReminderEnabled`, `followUpRemindersEnabled`, `backgroundHcSyncEnabled`
- Quota counters: `aiQuotaDateKey`, `aiQuotaUsedToday`

### 3.2 AI Quota, Caching & Play Billing v9 Seam
- **`BillingEntitlementGateway`** (`:core:datastore`) decouples Play Billing Library v9 (`PlayBillingManager` in `:app`) from feature ViewModels and JVM tests.
- **`PlayBillingManager`** configures `BillingClient` with `PendingPurchasesParams.enableOneTimeProducts().enablePrepaidPlans()` and `.enableAutoServiceReconnection()`, queries `ProductType.SUBS` via `queryProductDetailsAsync` and `queryPurchasesAsync`, and auto-acknowledges `PURCHASED` subscriptions.
- **`LocalAiEntitlementProvider`** combines `AiQuotaAndCacheManager` (in-memory fingerprint TTL cache + daily counter), `RemoteConfigSafetyFlagProvider`, and `subscriptionTierProvider` so `dailyLimit` scales dynamically from `25` (Free) to `200` (Pro).

### 3.3 Glance Widgets & WorkManager Background Pipelines
- **`WidgetSnapshotStore`** persists compact, non-sensitive widget snapshots (and opt-in Medical ID summary when enabled). `WellPlusAppContainer` observes Firestore + Health Connect + device settings and calls `WidgetRefreshBridge.notifyDataChanged(appContext)` → `WellPlusWidgetRefresher.refreshAll()`.
- **WorkManager Pipelines (`:core:notifications` & `:core:healthconnect`):**
  - Daily medication dose reminders (`MedicationReminderWorker` + `MedicationActionReceiver` for `Mark Taken` / `Snooze`)
  - Evening daily check-in reminder & 48-hour symptom follow-up nudges
  - 6-hour periodic Health Connect background sync (`HealthConnectSyncScheduler`)

---

## 4. Verification Invariants

- **Zero Cross-Feature Coupling Check:** `grep -rn 'project(":feature:' feature/*/build.gradle.kts` must return empty.
- **Schema Parity Check:** Every enum written by Kotlin repositories (`CareCategory`, `MedicationScheduleSlot`, `AiFeatureArea`, `LabBiomarkerCategory`, `LabBiomarkerStatus`) must match `firebase-blueprint.json`, `firestore.rules`, and `firestore.test.js` 1:1.

---

## Cross-References
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [Privacy, Security & Compliance](./PRIVACY_SECURITY_AND_COMPLIANCE.md)
- [QA Test Plan & CUJ Matrix](./QA_TEST_PLAN_AND_CUJS.md)
