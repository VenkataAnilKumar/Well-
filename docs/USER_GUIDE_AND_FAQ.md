# Well+ — User Guide & FAQ (Help Center)

> **Document Owner:** Product Management & Customer Experience  
> **Target Audience:** Well+ End Users, Customer Support Specialists  
> **Version:** 1.0  
> **Supported Markets:** United States & Canada  
> **Status:** Published Help Center Reference

---

## 1. Welcome to Well+

**Well+** is your personal health sanctuary for Android. It brings together daily check-ins, medications, vitals, lab results, mental wellness tracking, Android Health Connect metrics, and thoughtful AI-assisted health guidance in one private, calm space—so you can stay organized every day and walk into doctor appointments prepared.

> [!WARNING]
> **Important Safety Notice:** Well+ provides informational wellness guidance and health organization tools only. It does **not** diagnose medical conditions, prescribe treatments, or replace a licensed doctor. If you are experiencing a medical emergency, call **911** (US & Canada) or go to the nearest emergency room immediately. For mental health crisis support, call or text **988**.

---

## 2. How Well+ Fits Into Your Daily Health Routine

```text
  Morning                     During the Day                  Before a Doctor Visit
┌────────────────────┐      ┌───────────────────────┐       ┌────────────────────────┐
│ • 30-sec Daily     │      │ • Check symptoms via  │       │ • Open History         │
│   Check-In         │ ───▶ │   Guided Triage &     │ ───▶  │ • Tap Export Doctor    │
│ • Log Morning Meds │      │   Follow-Up Chat      │       │   Visit PDF (AI SOAP)  │
│ • Glance Home      │      │ • Scan Rx or Lab Page │       │ • Share with your      │
│   Widgets          │      │   (Auto PII Masked)   │       │   clinician            │
└────────────────────┘      └───────────────────────┘       └────────────────────────┘
```

---

## 3. Getting Started

### Signing In & 18+ Safety Setup
1. Open **Well+** and tap **Continue with Google** to securely sign in with your Google account. Your health records are synced privately to your account so you won't lose them if you switch phones.
2. Confirm that you are **18 years or older** and review the non-diagnostic wellness notice.
3. Choose your **Health Region**:
   - **United States (`US`):** Uses `°F`, `lbs`, `mg/dL`, US Insurance fields, and US emergency contacts (`911`, `988`, Poison Control `1-800-222-1222`).
   - **Canada (`CA`):** Uses `°C`, `kg`, `mmol/L`, Provincial Health Card fields, and Canadian emergency contacts (`911`, `988`, `811` Health Link/Telehealth).
4. *(Optional)* Add your primary care clinician, preferred pharmacy, allergies, and emergency contact in **Profile & Care Team** so they are ready whenever you export a Doctor Visit PDF or Emergency Medical ID.

---

## 4. Core Features Walkthrough

### 4.1 Daily Check-Ins & Care Checklist (**Today** Tab)
- **Daily Check-In:** Record how you feel in seconds—mood, energy, sleep quality, pain level, and physical activity minutes.
- **Care Checklist:** Track daily health habits and follow-up actions (like "Take morning blood pressure" or "Refill prescription"). Tap any circle to check it off and build your wellness streak.
- **Daily Insights:** View gentle, personalized wellness highlights synthesized from your recent check-ins, vitals, and Health Connect activity.

### 4.2 Guided Symptom Checks & Follow-Up Chat (**Consult** Tab)
- **Guided Triage:** Describe your symptoms in plain language (or use the microphone button for voice input). Well+ may ask 2–3 focused clarifying questions—such as duration, severity, or associated symptoms—before giving you a clear urgency summary and practical next steps.
- **Emergency Protection:** If you describe urgent warning signs (like chest pain, trouble breathing, stroke symptoms, or severe allergic reaction), Well+ immediately highlights an emergency alert with a one-tap button to open your phone dialer to **911**.
- **Follow-Up Chat:** Ask follow-up questions inside any saved consultation.
  - **Listen Aloud (TTS):** Tap **Listen** on any assistant message to have it read aloud hands-free.
  - **Verified Source Chips:** Tap citation chips beneath health explanations to open trusted public medical references (such as CDC, NIH, MedlinePlus, Mayo Clinic, or Health Canada).
  - **One-Tap Action Cards:** If the assistant suggests adding a care task, logging a vital, or adding a medication, it will show a **confirmation card**. Nothing is added to your health record unless you tap **Confirm**.
- **Report an AI Issue:** Notice something inaccurate or unhelpful? Tap **Report Safety Issue** on any AI card to flag it immediately for our safety team.

### 4.3 Privacy-First Photo, Lab & Prescription Scanning
Well+ lets you scan **prescription bottles**, **lab result pages**, or **symptom photos** using your camera or Android's built-in Photo Picker (no broad gallery permission required).

**How On-Device PII Redaction Protects You:**
1. Before a photo ever leaves your phone, Well+ runs an **on-device scanner (ML Kit)** to look for personal identifiers like your **name, date of birth, phone number, email, SSN, or Medical Record Number (MRN)**.
2. Well+ automatically draws solid black redaction boxes over detected personal details and shows you a **Redaction Preview Screen**.
3. Review the preview—you can confirm the masked image before sending it for analysis, or cancel at any time.
4. Photos are analyzed transiently and are **never permanently saved** to cloud file storage.

### 4.4 Medications & Reminders (**Meds** Tab)
- Add medications manually or scan a prescription bottle label (with automatic PII masking).
- Set daily reminder times so Well+ sends helpful Android notifications when it's time to take a dose.
- Mark doses as **Taken** or **Skipped** to keep an accurate adherence history.
- Tap **Check Interactions** to review potential medication considerations to discuss with your pharmacist or doctor.

### 4.5 Vitals, Lab Biomarkers, Mental Wellness & Cycle (**Vitals** & **Wellness** Tabs)
- **Vitals:** Log Blood Pressure, Heart Rate, Temperature, Blood Oxygen (SpO2), Blood Glucose, and Weight. If a reading enters a critical emergency threshold (for example, hypertensive crisis blood pressure or dangerously low oxygen), Well+ immediately displays an urgent safety alert.
- **Lab Biomarkers:** Scan a lab report or enter biomarkers (`HbA1c`, `LDL`, `TSH`, `Vitamin D`, etc.) to track Normal, High, or Low trends over time.
- **Mental Health Check-Ins:** Complete confidential **PHQ-9** (mood) and **GAD-7** (anxiety) self-check-ins. If you ever indicate thoughts of self-harm, Well+ immediately displays the **988 Suicide & Crisis Lifeline** for free, confidential 24/7 support.
- **Body & Cycle Tracking:** Optional body measurements and menstrual cycle phase/symptom logging.

### 4.6 Syncing Android Health Connect
1. Open **Settings → Health Connect**.
2. Tap **Connect Health Connect** and choose which categories you want to share: **Steps**, **Heart Rate**, **Sleep**, and **Blood Pressure**.
3. Tap **Sync Now** anytime to bring recent wearable or fitness data into Well+. You can revoke permissions anytime inside Android's Health Connect settings.

### 4.7 Exporting Your Doctor Visit PDF (SOAP Brief)
Heading to an appointment?
1. Open the **History** tab and tap **Export Doctor Visit PDF**.
2. Well+ compiles your recent consultations, medications, vitals, and lab biomarkers into a clean, clinician-friendly **SOAP Pre-Visit Brief** PDF (and still generates a clean structured summary even when you're offline).
3. Use the Android Share Sheet to print it, save it to Files, or send it to your doctor's patient portal.

### 4.8 Emergency Medical ID & Home Screen Widgets
- **Emergency Medical ID:** Store critical info (blood type, allergies, medical conditions, medications, emergency contact) in **Profile → Emergency Medical ID**.
- **3 Home Screen Widgets:** Long-press your Android home screen → **Widgets → Well+** to add:
  1. **Daily Sanctuary Widget** (check-in status & tasks)
  2. **Medication Adherence Widget** (upcoming doses)
  3. **Emergency Medical ID Widget** (locked by default for privacy — enable display explicitly under **Settings → Privacy & Security → Show Medical ID on Widget**)

---

## 5. Well+ Free vs. Well+ Pro Subscriptions

| Feature | Well+ Free | Well+ Pro |
| :--- | :---: | :---: |
| Daily Check-Ins, Care Tasks, Meds, Vitals, Labs, Mental & Cycle Logs | Unlimited | Unlimited |
| Emergency Red-Flag & Critical Vital Safety Alerts | **Always Free** | **Always Free** |
| Android Health Connect Sync & Home Screen Widgets | Included | Included |
| Doctor Visit PDF Export & JSON Data Backup | Included | Included |
| Daily AI Companion Requests (Triage, Chat, Photo/Lab/Rx Scans, Insights, AI SOAP) | **25 / day** | **200 / day** |

- **Plans:** Available as **Well+ Pro Monthly** (`$7.99 / mo`) or **Well+ Pro Annual** (`$59.99 / yr` — Best Value), billed securely through **Google Play**.
- **Manage or Cancel Anytime:** Open the **Google Play Store app → Profile icon → Payments & subscriptions → Subscriptions → Well+**.
- **Switching Phones / Reinstalling?** Sign in with the same Google account and tap **Settings → Subscription → Restore Purchases**.

---

## 6. Frequently Asked Questions (FAQ)

### Privacy & Security
- **Who can see my health data?**  
  Only you. Your records are stored under your private user account with strict per-user access controls and encrypted in transit. Well+ has **zero advertisements** and never sells your health data.
- **How do I lock Well+ on a shared phone?**  
  Go to **Settings → Privacy & Security** and enable **Biometric App Lock** (fingerprint/face/PIN) and **Screen Privacy** (which blocks screenshots and hides Well+ in the Android Recent Apps switcher).
- **What happens to my widgets if I sign out?**  
  Signing out immediately clears your personal health information and Emergency Medical ID from all Home Screen widgets.
- **Can I download or permanently delete all my data?**  
  Yes. In **Settings → Privacy & Data**, tap **Export All Data (JSON)** to download a complete portable copy, or tap **Delete Account & All Data** to permanently erase all your records and account.

### Using Well+ Offline & Troubleshooting
- **Does Well+ work without internet?**  
  Yes! You can log check-ins, medications, vitals, and tasks offline—they automatically sync once you're back online. Deterministic emergency red-flag alerts and non-AI Doctor Visit PDFs also work offline. AI chat and photo scans require an internet connection.
- **Why didn't my medication reminder ring?**  
  Make sure notifications are allowed for Well+ in **Android Settings → Apps → Well+ → Notifications** and that Battery Optimization isn't restricting background reminders.
- **Why did I reach my daily AI limit?**  
  Free accounts include **25 AI requests per day** (resetting daily), and Well+ Pro includes **200 AI requests per day**. Even if you hit your daily limit, all manual tracking, offline PDFs, Health Connect sync, and emergency safety alerts remain 100% available.

---

## 7. Quick-Reference Support & Verification Links

- [Master Docs Hub (`README.md`)](./README.md)
- [Privacy, Security & Compliance](./PRIVACY_SECURITY_AND_COMPLIANCE.md)
- [Clinical Safety & AI Governance](./CLINICAL_SAFETY_AND_AI_GOVERNANCE.md)
- [End-to-End Feature Spec & User Flows](./FEATURE_SPEC_AND_USER_FLOWS.md)
- [Monetization & Go-To-Market](./MONETIZATION_AND_GTM.md)
