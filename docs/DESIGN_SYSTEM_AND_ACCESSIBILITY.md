# Well+ — Design System & Accessibility Specification

> **Document Owner:** Product Design & Android UI Engineering  
> **Target Audience:** Design, Android Engineering, QA  
> **Design System:** Material Design 3 + Well+ Frosted Sanctuary Glassmorphism  
> **Last Verified:** 2026-11-26  
> **Status:** Live / Verified

---

## 1. Aesthetic Direction & Visual Identity

Well+ is designed as a **Calm Clinical Sanctuary**—replacing clinical sterility and alarming symptom-checker visuals with soft atmospheric glass surfaces, high-contrast WCAG AA/AAA typography, and clear color-blind-safe urgency cues.

- **Light Posture (`Frosted Light`):** Airy daylight mist canvas (`#EEF4F7`) layered with subtle ambient refraction orbs (Mint `#2DD4BF`, Indigo `#818CF8`, Coral `#FB7185` at `20%` alpha) behind frosted white glass cards (`White @ 72%` fill, `22.dp` radius, specular top highlight).
- **Dark Posture (`Twilight Dark`):** Deep twilight navy-slate canvas (`#0B1120`) with translucent glass cards (`White @ 8%` fill, `1.dp` luminous top-to-bottom gradient rim) and luminous mint-teal (`#2DD4BF`) primary actions.
- **Theme Persistence:** Users choose between `System`, `Frosted Light`, and `Twilight Dark` in Profile (`SanctuaryThemeMode`), persisted immediately in `SanctuaryThemePrefs` and synced with edge-to-edge status/navigation bar contrast (`WindowCompat.getInsetsController`).

---

## 2. Color System & Semantic Tokens

### 2.1 Core Brand & Surface Tokens

| Role | Light Token (`LightSanctuaryScheme`) | Dark Token (`DarkSanctuaryScheme`) | Usage |
| :--- | :--- | :--- | :--- |
| **Primary (Clinical Teal)** | `#0F766E` (`SanctuaryTeal`) | `#2DD4BF` (`DarkSeafoam`) | Primary CTAs, active tabs, medication accents, Pro highlights |
| **Primary Container** | `#CCFBF1` (`SanctuarySeafoam`) | `#0F353A` (`DarkSeafoamContainer`) | Selected chips, active pills, subtle clinical highlight cards |
| **Secondary (Calm Indigo)** | `#4F46E5` (`BotanicalSage`) | `#818CF8` (`DarkSage`) | Sleep, mindfulness, mental wellbeing reflections |
| **Tertiary (Soft Coral)** | `#E11D48` (`TerracottaClay`) | `#FB7185` (`DarkTerracotta`) | Heart rate, vitals emphasis, Medical ID badges |
| **Canvas Background** | `#EEF4F7` (`SanctuaryCream`) | `#0B1120` (`DarkEvergreen`) | Root ambient scaffold canvas |
| **Primary Ink** | `#0F172A` (`InkCharcoal`) | `#F1F5F9` (`DarkParchment`) | High-contrast body and heading copy (WCAG AAA) |
| **Secondary Ink** | `#475569` (`InkSlate`) | `#94A3B8` (`DarkMutedText`) | Subtitles, metadata timestamps, helper captions |

### 2.2 Color-Blind-Safe Clinical Urgency Tokens (`SanaSemanticColors`)

Every clinical urgency badge pairs **distinct hue + tonal background + text label + Material icon** so severity never relies on color alone:

| Urgency Band | Light FG / BG | Dark FG / BG | Required Pairing |
| :--- | :--- | :--- | :--- |
| **`SELF_CARE`** | `#15803D` / `#DCFCE7` | `#34D399` / `#0D3326` | Check/Home care icon + `"Self-Care"` label |
| **`DOCTOR_SOON`** | `#B45309` / `#FEF3C7` | `#FBBF24` / `#3B2A09` | Calendar/Doctor icon + `"Doctor Soon"` label |
| **`URGENT`** | `#C2410C` / `#FFEDD5` | `#FB923C` / `#421C0D` | Priority warning icon + `"Urgent"` label |
| **`EMERGENCY`** | `#BE123C` / `#FFE4E6` | `#FB7185` / `#4A0E20` | Emergency hospital icon + `"Emergency"` label + auto-escalation sheet |

---

## 3. Typography & Numeric Clarity

Well+ bundles two local `.ttf` Google Fonts inside `:core:designsystem` (`res/font/`)—never relying on runtime font downloads:

- **Display & Screen Headlines (`Fraunces` — Editorial Serif):** Warm, reassuring human voice for greeting headers and primary section titles (`displayLarge` 42sp → `headlineMedium` 23sp).
- **Clinical Data, Controls & Body (`Plus Jakarta Sans` — Geometric Sans):** Crisp scannability for dosages, vitals, lab reference ranges, forms, and chat threads (`headlineSmall` 20sp → `labelSmall` 11sp).
- **OpenType Tabular Numerals (`TextStyle.tabular()` → `fontFeatureSettings = "tnum"`):** Applied across live vitals, adherence percentages, daily AI quota counters, and chart readouts so changing digits never cause horizontal jitter.

---

## 4. Shape, Spacing & Adaptive Layout Rules

```text
Compact Phone (< 600.dp width)            Medium / Expanded Tablet & Foldable (>= 600.dp)
┌────────────────────────────┐            ┌──────┬──────────────────────────────────────┐
│  Offline / Safety Banner   │            │ Nav  │       Offline / Safety Banner        │
├────────────────────────────┤            │ Rail ├──────────────────────────────────────┤
│                            │            │      │         Centered Content Pane        │
│    Scrollable Content      │            │Today │        (Modifier.widthIn(            │
│    (20.dp horiz padding,   │            │Meds  │           max = 680.dp))             │
│     8.dp vertical grid)    │            │Cons. │                                      │
│                            │            │Hist. │     Glass Cards (22.dp radius)       │
├────────────────────────────┤            │Prof. │     1.dp specular gradient rim       │
│  Floating Glass Bottom Bar │            │      │                                      │
└────────────────────────────┘            └──────┴──────────────────────────────────────┘
```

- **Edge-to-Edge Mandatory:** `MainActivity` calls `enableEdgeToEdge()`, and the root `Scaffold` applies `WindowInsets.safeDrawing` so content never collides with camera cutouts, gesture bars, or IME keyboards.
- **Shape Scale (`SanaShapes`):** `extraSmall` (`10.dp`), `small` (`14.dp` buttons/inputs), `medium` (`20.dp` dialogs/cards), `large` (`22.dp` Sanctuary glass cards), `extraLarge` (`26.dp` bottom sheets & floating containers).
- **Adaptive Breakpoint:** `BoxWithConstraints` switches from `WellPlusBottomBar` to `WellPlusNavigationRail` at `maxWidth >= 600.dp`, keeping main content comfortably centered at `max = 680.dp`.
- **Canvas & Chart Sizing:** All vital trend sparklines/charts use `DrawScope.size` dynamically—never hardcoded pixel constants or `LocalConfiguration` dimensions—supporting split-screen and foldable posture changes smoothly.

---

## 5. Accessibility & Inclusive UX Standards

| Standard | Implementation in Well+ |
| :--- | :--- |
| **Minimum Touch Targets** | All buttons, icon buttons, action pills, and back controls enforce `>= 48.dp × 48.dp` via Material 3 `minimumInteractiveComponentSize()` or explicit `height(48.dp..54.dp)`. |
| **TalkBack Content Descriptions** | Every functional `Icon`, `IconButton`, and `Image` defines localized `contentDescription`; decorative icons explicitly pass `null` to prevent screen-reader clutter. |
| **Dynamic Font Scaling** | All typography uses `sp` units; card headers use `weight(1f)` and `FlowRow` chips wrap gracefully at `200%` system font scale without clipping buttons. |
| **Spoken Clinical Readout (TTS)** | `WellPlusTtsController` provides 1-tap `Listen` / `Stop` pills on Consultation Reports and every Follow-Up Chat Doctor bubble, giving hands-free audio access for low-vision or fatigued patients. |
| **Voice Input Dictation** | Zero-permission Android system speech recognition (`VoiceAndEmergencyHelper`) is embedded in Symptom Intake and Follow-Up Chat for patients who struggle to type while unwell. |
| **Automated UI Semantics (`testTag`)** | All primary controls carry deterministic `snake_case` `Modifier.testTag(...)` identifiers (e.g., `nav_today`, `pro_subscribe_*`, `emergency_id_dial_911_button`, `pii_use_redacted_button`) supporting Compose UI & Robolectric verification. |
| **High-Contrast Emergency Mode** | Emergency Escalation Sheets, Red-Flag Dialogs, Crisis Banners, and the **Emergency Medical ID** header use high-contrast crimson (`#BE123C` / `#C62828`) with pure white text (`> 7:1` contrast ratio) for immediate legibility by first responders. |

---

## 6. Acceptance Criteria (Given / When / Then)

- **AC-D1 Theme Switching:** *Given* a user switches from `Frosted Light` to `Twilight Dark` in Profile, *when* selected, *then* all screens, glass cards, status bar icons, and semantic urgency pills immediately re-theme and persist across app restarts.
- **AC-D2 Tablet/Foldable Adaptation:** *Given* the app opens on a screen width `>= 600.dp`, *when* on any top-level tab, *then* the floating Bottom Bar is replaced by a left `WellPlusNavigationRail` and content stays bounded at `<= 680.dp`.
- **AC-D3 Accessibility Audit:** *Given* TalkBack is active and system font size is set to Largest, *when* navigating Today, Consult Report, Medical ID, and Pro screens, *then* no interactive touch target falls below `48.dp`, no text clips off primary buttons, and every icon button announces its purpose clearly.

---

## Cross-References
- [End-to-End Feature Spec & User Flows](./FEATURE_SPEC_AND_USER_FLOWS.md)
- [QA Test Plan & CUJ Matrix](./QA_TEST_PLAN_AND_CUJS.md)
