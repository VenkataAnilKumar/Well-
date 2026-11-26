# Well+ — AI Features & Capabilities Specification

This document tracks the evolution of AI features and underlying capabilities in **Well+**, comparing the initial MVP, the current 0→1 product build, and the prioritized roadmap for next-stage clinical AI capabilities.

---

## 1. Previous AI Features (Initial MVP)

1. **Guided Symptom Triage (`JSON Schema`)**
   - Analyzed the 4-step symptom intake (body area, severity, timing, associated symptoms) alongside the basic Health Profile to produce a structured report: urgency tier, plain-language summary, possible causes, care suggestions, red-flag warnings, and doctor questions.
2. **Multimodal Photo & Lab Review**
   - Ephemeral Gemini Vision analysis of a CameraX-captured or gallery-selected symptom or lab report image without storing raw photos in cloud buckets.
3. **Streaming Follow-Up Chat**
   - Multi-turn conversational AI Doctor grounded in the active consultation report, prior messages, health profile, and optional follow-up photos.

---

## 2. AI Features Live Now (Current Build)

Everything from the initial MVP, expanded with:

1. **Full Clinical Context Injection**
   - AI triage and follow-up chat automatically incorporate **active tracked prescriptions (with dosages and schedules)** and **recent biometric vitals (with clinical range flags)** in addition to allergies, conditions, and demographics.
2. **30-Day Longitudinal Health Synthesis**
   - On-demand structured Gemini synthesis in the History tab correlating 30 days of sleep, mood, daily check-ins, vitals, medication adherence, allergies, and symptom recovery outcomes into:
     - Overall trajectory badge & headline
     - Cross-signal patterns observed
     - Medication & allergy awareness notes
     - Next-week focus recommendations
     - Clinician talking points
3. **Voice-to-AI Dictation**
   - Zero-permission Android system speech recognition integrated directly into Guided Symptom Intake, Photo Context, and AI Follow-Up Chat.
4. **Urgency-Triggered Emergency Safety Escalation**
   - AI `Urgent` and `Emergency` classifications automatically surface the high-contrast Emergency Escalation Sheet with red-flag guidance and one-tap `ACTION_DIAL` actions for 911 and the saved emergency contact.

### Underlying Capabilities Active Today
- **Structured Output Generation (`JSON Schema`)** for deterministic clinical UI rendering.
- **Multimodal Vision Processing** for ephemeral symptom and lab document interpretation.
- **Token-by-Token Streaming Chat** for low-latency follow-up reasoning.
- **Cross-Signal Longitudinal Aggregation** across Firestore collections.
- **Clinical Safety Guardrails** enforcing non-diagnostic framing and urgency escalation.

---

## 3. Next AI Features & Capabilities Roadmap

| Priority | Category | Feature / Capability | Description & User Value |
| :---: | :--- | :--- | :--- |
| **P0** | Medication Safety AI | **Drug–Drug & Drug–Allergy Interaction Checker** | Automatically evaluates newly logged prescriptions against active medications, recorded allergies, and ongoing conditions (`Safe` / `Caution` / `Ask Pharmacist`) with dose-spacing guidance. |
| **P0** | Vision → Form Extraction | **Prescription Bottle & Pill Label Scanner** | Point CameraX at a pill bottle or pharmacy printout to auto-populate prescription name, dosage strength, schedule window, and intake instructions in the **Meds** tab. |
| **P1** | Vision → Structured Data | **Lab Biomarker Extraction & Trends** | Parse photographed lab reports into structured biomarkers (e.g., HbA1c, LDL, Vitamin D, TSH) with reference-range badges tracked over time alongside vitals. |
| **P1** | Adaptive Triage AI | **Dynamic Clarifying Intake Questions** | Generate 2–3 targeted clinical clarifying questions based on Step 1 symptoms before producing the triage report to improve signal quality and reduce friction. |
| **P1** | Proactive Risk AI | **Worsening Symptom & Abnormal Vital Reassessment** | Automatically trigger contextual AI reassessment when a user marks a prior symptom **Worsening** or logs an out-of-range vital (e.g., elevated BP or fever). |
| **P2** | Agentic Tool Calling | **One-Tap AI Actions in Follow-Up Chat** | Allow the AI Doctor to propose structured in-app actions (add a medication reminder, log a vital, add a Care Checklist item, or schedule a 48h follow-up nudge) with user confirmation. |
| **P2** | Clinical Prep AI | **AI SOAP Pre-Visit Brief in PDF** | Embed an AI-distilled 1-page SOAP clinical summary (Subjective, Objective, Assessment Questions, Active Meds) at the top of the exported Doctor Visit PDF. |
| **P2** | Grounding & Voice | **Cited Medical Sources + Hands-Free Spoken AI** | Attach verifiable medical reference links (NIH, CDC, MedlinePlus), add optional spoken AI responses (`TextToSpeech` / Live Audio), and surface daily check-in micro-insights. |
| **P3** | Hybrid On-Device AI | **Local OCR Framing & PII Redaction** | Use on-device ML Kit text recognition for instant offline label framing and automatic redaction of sensitive IDs (e.g., SSN/MRN) before cloud AI requests. |
