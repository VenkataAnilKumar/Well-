# Well+ — Product Documentation Hub

> **Product:** Well+ (Personal Health Sanctuary & AI Clinical Companion for Android)  
> **Markets:** United States (`US`) & Canada (`CA`)  
> **Documentation Version:** 1.0 (Verified against current production build)  
> **Audience:** Product Management, Engineering, QA, Legal/Compliance, and User Support

Welcome to the **Well+ Product Documentation Suite**. This hub links all **12 living product, UX, safety, engineering, launch, and customer support documents**—each verified directly against the live Well+ Android codebase, Firestore security rules, and AI safety engines.

---

## Documentation Architecture Map

```text
                        ┌───────────────────────────────────────┐
                        │     docs/README.md (Master Hub)       │
                        │   Role-Based Tracks • 12 Live Docs    │
                        └───────────────────┬───────────────────┘
                                            │
        ┌───────────────────┬───────────────┼───────────────┬───────────────────┐
        ▼                   ▼               ▼               ▼                   ▼
┌───────────────┐   ┌───────────────┐ ┌───────────┐ ┌───────────────┐   ┌───────────────┐
│  1. Strategy  │   │ 2. UX & Specs │ │ 3. Safety │ │ 4. Tech & Data│   │  5. Release   │
├───────────────┤   ├───────────────┤ ├───────────┤ ├───────────────┤   ├───────────────┤
│ • Master PRD  │   │ • Feature &   │ │ • Clinical│ │ • Modular     │   │ • Play Store  │
│ • Monetization│   │   User Flows  │ │   Safety &│ │   Arch & Data │   │   Launch      │
│   & GTM       │   │ • Design Sys  │ │   AI Gov  │ │   Model       │   │   Checklist   │
│ • AI Roadmap  │   │   & Access.   │ │ • Privacy │ │ • Apple Health│   │ • QA Plan &   │
│               │   │               │ │   & Comp. │ │   Feature Map │   │   CUJ Matrix  │
│               │   │               │ │           │ │               │   │ • User Guide  │
└───────────────┘   └───────────────┘ └───────────┘ └───────────────┘   └───────────────┘
```

---

## Complete Document Catalog (12 Documents)

### 1. Product Strategy & Planning
| Document | Primary Audience | Scope | Status |
| :--- | :--- | :--- | :---: |
| [**Master Product Requirements (PRD)**](./PRODUCT_REQUIREMENTS_PRD.md) | PM, Leadership, Eng, QA | Vision, personas, US/CA localization, Free vs. Pro tier matrix, functional/non-functional requirements, KPIs, and acceptance criteria. | Live |
| [**Monetization & Go-To-Market (GTM)**](./MONETIZATION_AND_GTM.md) | PM, Growth, Finance, Eng | Well+ Pro Google Play Billing v9 plans, daily AI quota scaling (25 → 200), retention loops, paywall triggers, and phased rollout strategy. | Live |
| [**AI Features & Capabilities Roadmap**](./AI_FEATURES_ROADMAP.md) | PM, AI/ML Eng | Historical progression from MVP to full clinical AI capabilities across triage, vision, longitudinal synthesis, agentic actions, SOAP PDFs, and on-device OCR. | Live |

### 2. UX, Design & Feature Specifications
| Document | Primary Audience | Scope | Status |
| :--- | :--- | :--- | :---: |
| [**End-to-End Feature Spec & User Flows**](./FEATURE_SPEC_AND_USER_FLOWS.md) | PM, Design, Eng, QA | Screen-by-screen behavior, state transitions, edge cases, and flow diagrams across all 14 Well+ screens and 3 Glance Home Screen widgets. | Live |
| [**Design System & Accessibility Guide**](./DESIGN_SYSTEM_AND_ACCESSIBILITY.md) | Design, Android Eng, QA | Frosted Daylight & Twilight Dark glassmorphism tokens, Fraunces + Plus Jakarta Sans typography, semantic urgency colors, adaptive phone/tablet layouts, TalkBack, and TTS rules. | Live |

### 3. Clinical Safety, AI Governance & Privacy
| Document | Primary Audience | Scope | Status |
| :--- | :--- | :--- | :---: |
| [**Clinical Safety & AI Governance**](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md) | Clinical Safety, Legal, Eng | Non-diagnostic boundaries, deterministic red-flag & critical-vital thresholds, US/CA emergency directories, citation domain allowlist, ML Kit on-device PII masking, 12-area AI Safety Reporting, and Remote Config kill switches. | Live |
| [**Privacy, Security & Compliance**](./PRIVACY_SECURITY_AND_COMPLIANCE.md) | Legal, Security, Privacy, Eng | Zero-trust Firestore UID isolation, Firebase App Check, Biometric Lock & Screen Privacy, opt-in Emergency Medical ID widget privacy, Health Connect least-privilege permissions, JSON portability export, and full account deletion. | Live |

### 4. Engineering & Data Architecture
| Document | Primary Audience | Scope | Status |
| :--- | :--- | :--- | :---: |
| [**Architecture & Data Model Reference**](./ARCHITECTURE_AND_DATA_MODEL.md) | Android Eng, Backend Eng | 23-module Clean/MVVM architecture (zero cross-feature coupling), shared hot Firestore streams, complete contract tables for all 13 Firestore collections, DataStore/Widget schemas, AI quota/caching, Play Billing v9, and WorkManager. | Live |
| [**Apple Health Feature Parity Map**](./APPLE_HEALTH_FEATURE_MAP.md) | PM, Android Eng | Feature parity mapping between Apple Health capabilities and Well+ Android (Health Connect + Jetpack Compose + Firestore). | Live |

### 5. Launch Readiness, QA & Customer Support
| Document | Primary Audience | Scope | Status |
| :--- | :--- | :--- | :---: |
| [**Google Play Launch & Policy Checklist**](./PLAY_STORE_LAUNCH_CHECKLIST.md) | Release Eng, PM, Legal | Play Health Apps declaration, Generative AI safety compliance, Data Safety table, Health Connect & Photo Picker review justifications, Play Billing v9 SKU checklist, and Crashlytics readiness. | Live |
| [**QA Test Plan & CUJ Matrix**](./QA_TEST_PLAN_AND_CUJS.md) | QA, Release Eng, Eng | Automated test inventory (13 Kotlin/Robolectric test suites + 12 Firestore rules emulator tests) and manual release sign-off acceptance matrix. | Live |
| [**User Guide & FAQ (Help Center)**](./USER_GUIDE_AND_FAQ.md) | End Users, Support, CX | Plain-language onboarding guide, PII-safe photo/lab scanning, Health Connect setup, Doctor Visit SOAP PDF export, Emergency Medical ID widget opt-in, subscriptions, and privacy FAQ. | Live |

---

## Role-Based Reading Tracks

- **Product Manager / Executive:** Start with [`PRODUCT_REQUIREMENTS_PRD.md`](./PRODUCT_REQUIREMENTS_PRD.md) → [`MONETIZATION_AND_GTM.md`](./MONETIZATION_AND_GTM.md) → [`FEATURE_SPEC_AND_USER_FLOWS.md`](./FEATURE_SPEC_AND_USER_FLOWS.md).
- **Android / Backend Engineer:** Start with [`ARCHITECTURE_AND_DATA_MODEL.md`](./ARCHITECTURE_AND_DATA_MODEL.md) → [`DESIGN_SYSTEM_AND_ACCESSIBILITY.md`](./DESIGN_SYSTEM_AND_ACCESSIBILITY.md) → [`QA_TEST_PLAN_AND_CUJS.md`](./QA_TEST_PLAN_AND_CUJS.md).
- **Clinical Safety / Legal / Privacy Reviewer:** Start with [`CLINICAL_SAFETY_AND_AI_GOVERNANCE.md`](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md) → [`PRIVACY_SECURITY_AND_COMPLIANCE.md`](./PRIVACY_SECURITY_AND_COMPLIANCE.md) → [`PLAY_STORE_LAUNCH_CHECKLIST.md`](./PLAY_STORE_LAUNCH_CHECKLIST.md).
- **QA & Release Manager:** Start with [`QA_TEST_PLAN_AND_CUJS.md`](./QA_TEST_PLAN_AND_CUJS.md) → [`PLAY_STORE_LAUNCH_CHECKLIST.md`](./PLAY_STORE_LAUNCH_CHECKLIST.md) → [`FEATURE_SPEC_AND_USER_FLOWS.md`](./FEATURE_SPEC_AND_USER_FLOWS.md).
- **Customer Support / End User:** Start with [`USER_GUIDE_AND_FAQ.md`](./USER_GUIDE_AND_FAQ.md) → [`PRIVACY_SECURITY_AND_COMPLIANCE.md`](./PRIVACY_SECURITY_AND_COMPLIANCE.md).

---

## Product Snapshot (Verified Baseline)

| Dimension | Current Verified Production Baseline |
| :--- | :--- |
| **Application Name & Package** | `Well+` (`com.aistudio.sanacare.qvxkmt`, `minSdk 24`, `targetSdk 36`, `compileSdk 36.1`) |
| **Architecture** | 23 Gradle modules (`:app`, 11 `:core:*`, 11 `:feature:*`), zero cross-feature dependencies |
| **Authentication & Cloud Storage** | Google Sign-In via Jetpack Credential Manager + Firebase Auth + per-UID Cloud Firestore (13 collections) |
| **AI Platform** | Firebase AI Logic (`gemini-3.8-flash` primary clinical/vision/SOAP, `gemini-3.5-flash-lite` fast insights/actions) + Firebase App Check |
| **On-Device Intelligence** | ML Kit Text Recognition v2 (automatic PII redaction preview), Android `TextToSpeech`, Android Speech Recognition, deterministic offline `ClinicalSafetyEngine` |
| **Monetization** | Google Play Billing Library v9 (`wellplus_pro_annual`, `wellplus_pro_monthly`) — Free `25` AI requests/day vs. Pro `200` AI requests/day |
| **Home Screen & Ecosystem** | Android Health Connect (Steps, Heart Rate, Sleep, Blood Pressure) + 3 Jetpack Glance Home Screen widgets |
