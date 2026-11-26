# Well+ — Monetization, Subscription & Go-To-Market (GTM) Brief

> **Document Owner:** Product Management & Growth  
> **Target Audience:** Leadership, PM, Growth, Engineering, Finance  
> **Product Version:** 1.0 (Google Play Billing Library v9)  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Ready for Play Console SKU Publishing

---

## 1. Executive Summary

Well+ uses a **freemium clinical-value subscription model** powered by **Google Play Billing Library v9 (`billing-ktx:9.1.0`)**. Every foundational personal health tracker, Health Connect sync, on-device ML Kit PII redaction, and life-critical safety guardrail remains **free forever**. Users who rely heavily on daily Gemini AI triage, vision scanning, follow-up conversations, and SOAP clinician PDF briefs upgrade to **Well+ Pro** for an **8× expansion in daily AI quota** (`25/day` → `200/day`).

---

## 2. Subscription Packaging & SKU Catalog

| Plan | Play Console Product ID | Fallback Display Price | Billing Cadence | Merchandising Badge | Daily AI Quota |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Well+ Free** | *(default tier)* | `$0.00` | Ongoing | Current Plan | `25 requests / day` |
| **Well+ Pro Annual** | `wellplus_pro_annual` | `$59.99 / yr` *(live Play localized price overrides)* | Annual (`P1Y`) | **Best Value** (~37% savings vs. monthly) | `200 requests / day` |
| **Well+ Pro Monthly** | `wellplus_pro_monthly` | `$7.99 / mo` *(live Play localized price overrides)* | Monthly (`P1M`) | *(none)* | `200 requests / day` |

### Ethical Health Monetization Guardrails
> [!IMPORTANT]
> Well+ follows a strict **Safety-Never-Paywalled** policy:
> 1. Emergency escalation sheets, localized `911`/`988`/`811` dialing, and `Emergency Medical ID` are always free.
> 2. Deterministic critical-vital alerts and symptom red-flag interception work offline and free of charge.
> 3. On-device ML Kit PII redaction preview is always free—privacy is never a paid upsell.
> 4. Post-turn Follow-Up Chat action card extraction (`gemini-3.5-flash-lite`) does **not** consume an extra daily AI credit.

---

## 3. Quota Enforcement & Upgrade Conversion Funnel

```text
┌───────────────────────────┐     ┌──────────────────────────────────┐
│ Daily AI Usage (Free Tier)│────▶│ Remaining Quota Shown in Profile │
│  • Symptom Triage         │     │  & Today Sanctuary (25 / day)    │
│  • Lab / Rx Vision Scans  │     └────────────────┬─────────────────┘
│  • Follow-Up Chat Turns   │                      │
│  • 30-Day / SOAP Synthesis│                      ▼
└───────────────────────────┘     ┌──────────────────────────────────┐
                                  │  Upgrade Touchpoint              │
                                  │  • Profile -> "Well+ Pro" Card   │
                                  │  • Quota-reached graceful notice │
                                  └────────────────┬─────────────────┘
                                                   │
                                                   ▼
                                  ┌──────────────────────────────────┐
                                  │  WellPlusProScreen               │
                                  │  • Compare Free vs. Pro          │
                                  │  • Live Play localized pricing   │
                                  │  • Subscribe via Google Play     │
                                  │  • Restore Purchases             │
                                  └────────────────┬─────────────────┘
                                                   │
                                                   ▼
                                  ┌──────────────────────────────────┐
                                  │  PlayBillingManager (Billing v9) │
                                  │  • launchBillingFlow()           │
                                  │  • acknowledgePurchase()         │
                                  │  • LocalAiEntitlementProvider    │
                                  │    scales immediately 25 -> 200  │
                                  └──────────────────────────────────┘
```

### Billing Edge-Case Handling
- **Store SKUs Unpublished (Pre-Production / Sideload / Emulator):** `PlayBillingManager` gracefully displays `DEFAULT_DISPLAY_CATALOG` with a transparent informational banner explaining that Google Play Billing is awaiting published SKUs on a Play Store test track while keeping all Free features active.
- **Pending Purchases (Cash / Delayed Payment Instruments):** Configured via `PendingPurchasesParams.enableOneTimeProducts().enablePrepaidPlans()`. Pending purchases show a clear status notice without prematurely granting Pro entitlements until `PURCHASE_STATE == PURCHASED`.
- **Reinstall / Device Switch:** Tapping **Restore** queries `queryPurchasesAsync(ProductType.SUBS)` and restores active Pro entitlements immediately.
- **Remote Config Override Safety:** `LocalAiEntitlementProvider` computes `maxOf(remoteBaseLimit, WellPlusTier.PRO.dailyAiQuota)` for Pro users while preserving the global `cloudAiGloballyEnabled` safety kill switch.

---

## 4. Core Retention & Habit Loops

| Loop | Trigger | User Action | Value Reinforcement |
| :--- | :--- | :--- | :--- |
| **Daily Check-In Loop** | Evening reminder notification or Quick Mood Glance Widget | 1-tap mood check-in + sleep/hydration/activity log | Streak counter + cached Today Micro-Insight card |
| **Medication Adherence Loop** | Scheduled WorkManager alarm per dose slot (`Morning`, `Midday`, `Evening`, `Bedtime`) | Notification quick-action (`Mark Taken` / `Snooze`) or Next Dose Widget | Real-time adherence percentage + Drug–Allergy safety confidence |
| **Symptom Recovery Loop** | 48-hour follow-up nudge notification (manual or via Chat Action Card) | Update outcome (`Improving` / `Unchanged` / `Worsening` / `Resolved`) | If `Worsening`, proactive AI reassessment triggers immediately |
| **Clinician Visit Loop** | Upcoming doctor appointment | Export 30-day **Doctor Visit PDF** with 1-page AI SOAP Brief & Lab Biomarkers | Tangible real-world clinical utility driving word-of-mouth referrals |
| **Wearable Sync Loop** | 6-hour periodic Health Connect WorkManager sync | Passive steps, heart rate, sleep, and blood pressure ingestion | Zero-effort longitudinal dashboard freshness |

---

## 5. Phased Go-To-Market (GTM) Rollout Plan

| Phase | Target Audience & Scope | Distribution Track | Key Exit Gates |
| :--- | :--- | :--- | :--- |
| **Phase 1: Internal Dogfood** | Core product, engineering, QA, clinical reviewers (15–30 testers) | Google Play Internal Testing | Zero P0 crashes, Play Billing license testers validate Monthly & Annual purchase/acknowledge/restore, Crashlytics symbols verified. |
| **Phase 2: Closed Clinical Beta (US & CA)** | 250–500 opted-in adult beta testers across US and Canada | Google Play Closed Testing | Play Health Apps & Health Connect production permissions approved; PII redaction adoption >= 85%; AI safety report rate < 2%. |
| **Phase 3: Open Regional Launch (US & CA)** | General adult consumers on Android (`minSdk 24+`) | Google Play Production (staged 10% → 25% → 50% → 100%) | Crash-free sessions >= 99.5%, Firestore security rules verified, Play Store listing assets & privacy policy live. |

---

## 6. Acceptance Criteria (Given / When / Then)

- **AC-M1 Live Catalog Query:** *Given* a Play Store build with published `wellplus_pro_annual` and `wellplus_pro_monthly` subscriptions, *when* `WellPlusProScreen` opens, *then* localized Play Store prices and billing periods render dynamically with `offerToken`s ready for checkout.
- **AC-M2 Purchase & Acknowledgment:** *Given* a Free user completes a test subscription purchase via Google Play, *when* `onPurchasesUpdated` returns `OK` with `PURCHASED`, *then* `PlayBillingManager` acknowledges the purchase, updates `WellPlusSubscriptionState.tier = PRO`, and `SessionCoordinator.aiQuotaRemaining` immediately reflects the `200/day` ceiling.
- **AC-M3 User Cancellation Safety:** *Given* a user dismisses the Play purchase sheet, *when* `USER_CANCELED` returns, *then* the processing spinner clears, the user remains on Free, and a calm non-error notice confirms no charge occurred.
- **AC-M4 Restore Purchases:** *Given* a Pro subscriber reinstalls Well+ on a new device with the same Google Play account, *when* they tap `Restore` on `WellPlusProScreen`, *then* active `SUBS` entitlements are restored automatically.

---

## Cross-References
- [Master PRD](./PRODUCT_REQUIREMENTS_PRD.md)
- [Google Play Launch & Policy Checklist](./PLAY_STORE_LAUNCH_CHECKLIST.md)
- [Architecture & Data Model](./ARCHITECTURE_AND_DATA_MODEL.md)
